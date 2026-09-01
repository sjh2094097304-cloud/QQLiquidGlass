package com.qiutian.bianpaobubble.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded lookups of QQ's message model; never searches message text for IDs. */
final class Reflector {
    private static final int MAX_TYPES = 64;
    private static final Map<Class<?>, Accessors> ACCESS = new LinkedHashMap<Class<?>, Accessors>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Class<?>, Accessors> eldest) { return size() > MAX_TYPES; }
    };
    private static final class Accessors {
        final Map<String, Field> fields = new LinkedHashMap<>();
        final Map<String, Method> methods = new LinkedHashMap<>();
    }
    private Reflector() {}

    private static Field findField(Class<?> type, String name) {
        synchronized (ACCESS) {
            Accessors access = ACCESS.get(type);
            if (access == null) { access = new Accessors(); ACCESS.put(type, access); }
            if (access.fields.containsKey(name)) return access.fields.get(name);
            Field found = null;
            for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
                try {
                    found = current.getDeclaredField(name);
                    if (Modifier.isStatic(found.getModifiers())) { found = null; break; }
                    found.setAccessible(true); break;
                } catch (NoSuchFieldException ignored) {
                } catch (Throwable ignored) { found = null; break; }
            }
            access.fields.put(name, found);
            return found;
        }
    }
    static Object field(Object object, String... names) {
        if (object == null) return null;
        for (String name : names) {
            try {
                Field found = findField(object.getClass(), name);
                if (found == null) continue;
                Object value = found.get(object);
                if (value != null) return value;
            } catch (Throwable ignored) {}
        }
        return null;
    }
    static boolean setNumber(Object object, String name, int value) {
        if (object == null) return false;
        try {
            Field found = findField(object.getClass(), name);
            if (found == null) return false;
            Class<?> type = found.getType();
            if (type == int.class || type == Integer.class) found.set(object, value);
            else if (type == long.class || type == Long.class) found.set(object, (long) value);
            else if (type == String.class) found.set(object, String.valueOf(value));
            else return false; // A short would silently truncate a bubble ID.
            return true;
        } catch (Throwable ignored) { return false; }
    }
    static int positiveInt(Object value) {
        if (!(value instanceof Number) && !(value instanceof CharSequence)) return 0;
        if (value instanceof Float || value instanceof Double) return 0;
        try {
            int number = Integer.parseInt(value.toString().trim());
            return number > 0 ? number : 0;
        } catch (NumberFormatException ignored) { return 0; }
    }
    private static Object property(Object object, String field, String getter) {
        Object value = field(object, field);
        return value == null ? invokeNoArgs(object, getter) : value;
    }
    static Object bubbleInfoFromArgs(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) if (arg instanceof Map) {
            Object found = bubbleInfoFromAttrs(arg);
            if (found != null) return found;
        }
        return null;
    }
    /** sendMsg supplies Map<Integer, MsgAttributeInfo>; inspect only VAS attributes. */
    static Object bubbleInfoFromAttrs(Object attrs) {
        if (!(attrs instanceof Map)) return null;
        int count = 0;
        for (Object attribute : ((Map<?, ?>) attrs).values()) {
            if (++count > 32) break;
            Object vas = property(attribute, "vasMsgInfo", "getVasMsgInfo");
            Object bubble = property(vas, "bubbleInfo", "getBubbleInfo");
            if (bubble != null) return bubble;
        }
        return null;
    }
    static Object bubbleInfo(Object object) {
        Object record = messageRecord(object, 0, new IdentityHashMap<>());
        return record == null ? null : bubbleInfoFromAttrs(property(record, "msgAttrs", "getMsgAttrs"));
    }
    static int bubbleId(Object object) {
        Object record = messageRecord(object, 0, new IdentityHashMap<>());
        if (record == null || !hasBubbleContent(record)) return 0;
        Object bubble = bubbleInfoFromAttrs(property(record, "msgAttrs", "getMsgAttrs"));
        if (bubble == null) return 0;
        Object mainValue = property(bubble, "bubbleId", "getBubbleId");
        int main = positiveInt(mainValue);
        int sub = positiveInt(property(bubble, "subBubbleId", "getSubBubbleId"));
        // QQ can retain an old sub-ID while the main ID explicitly selects its default skin.
        if (mainValue != null && main < 1000) return 0;
        return sub >= 1000 ? sub : main >= 1000 ? main : 0;
    }
    private static Object messageRecord(Object object, int depth, IdentityHashMap<Object, Boolean> seen) {
        if (object == null || depth > 3 || seen.put(object, Boolean.TRUE) != null) return null;
        if (object instanceof CharSequence || object instanceof Number || object instanceof Collection
                || object instanceof Map || object.getClass().isArray()) return null;
        Object elements = property(object, "elements", "getElements");
        if (elements instanceof Collection && property(object, "msgAttrs", "getMsgAttrs") instanceof Map) return object;
        for (String method : new String[]{"getMsgRecord", "getRecord", "getData"}) {
            Object found = messageRecord(invokeNoArgs(object, method), depth + 1, seen);
            if (found != null) return found;
        }
        for (String name : new String[]{"msgRecord", "record", "data"}) {
            Object found = messageRecord(field(object, name), depth + 1, seen);
            if (found != null) return found;
        }
        return null;
    }
    private static boolean hasBubbleContent(Object record) {
        Object raw = property(record, "elements", "getElements");
        if (!(raw instanceof Collection)) return false;
        Collection<?> elements = (Collection<?>) raw;
        if (elements.isEmpty() || elements.size() > 256) return false;
        boolean text = false;
        for (Object element : elements) {
            // A standalone sticker/face may carry stale VAS metadata without rendering a bubble.
            if (property(element, "marketFaceElement", "getMarketFaceElement") != null
                    || property(element, "faceBubbleElement", "getFaceBubbleElement") != null
                    || property(element, "giphyElement", "getGiphyElement") != null) return false;
            Object textElement = property(element, "textElement", "getTextElement");
            Object content = property(textElement, "content", "getContent");
            if (content instanceof CharSequence && content.toString().trim().length() > 0) text = true;
        }
        return text;
    }
    static Object invokeNoArgs(Object object, String name) {
        if (object == null) return null;
        Method found;
        synchronized (ACCESS) {
            Class<?> type = object.getClass();
            Accessors access = ACCESS.get(type);
            if (access == null) { access = new Accessors(); ACCESS.put(type, access); }
            if (access.methods.containsKey(name)) found = access.methods.get(name);
            else {
                found = null;
                for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
                    try {
                        found = current.getDeclaredMethod(name);
                        if (Modifier.isStatic(found.getModifiers())) { found = null; break; }
                        found.setAccessible(true); break;
                    } catch (NoSuchMethodException ignored) {
                    } catch (Throwable ignored) { found = null; break; }
                }
                access.methods.put(name, found);
            }
        }
        try { return found == null ? null : found.invoke(object); }
        catch (Throwable ignored) { return null; }
    }
    static Field firstInstanceField(Class<?> type, Class<?> wanted) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && wanted.isAssignableFrom(field.getType())) {
                    field.setAccessible(true); return field;
                }
            }
        }
        return null;
    }
}
