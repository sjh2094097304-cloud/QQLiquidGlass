package com.qiutian.bianpaobubble.hook;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qiutian.bianpaobubble.R;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Reuses one of QQ's concrete menu-item classes. This avoids runtime dex generation,
 * keeps the APK small, and is safer on FPA and traditional Xposed loaders.
 */
final class NtMenuFactory {
    private static final WeakIdentityMap<Runnable> ACTIONS = new WeakIdentityMap<>(128);
    private static final Set<String> HOOKED = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<String> VIEW_SCANNED = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private NtMenuFactory() {}

    static boolean containsOwned(List<?> items) {
        if (items == null) return false;
        synchronized (ACTIONS) {
            for (Object item : items) if (ACTIONS.containsKey(item)) return true;
        }
        return false;
    }

    static Object create(ClassLoader hostLoader, Object msg, List<?> existingItems, Context hostContext,
                         int menuIcon, Runnable action) throws Exception {
        Resources hostResources = hostContext == null ? null : hostContext.getResources();
        Class<?> msgClass = hostLoader.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem");
        Class<?> base = resolveMenuBase(hostLoader);
        Method clickMethod = null;
        for (Method method : menuMethods(base)) {
            if (method.getParameterTypes().length != 0) continue;
            if (method.getReturnType() == void.class && clickMethod == null) clickMethod = method;
        }
        if (clickMethod == null) throw new NoSuchMethodException("QQ custom menu click method");

        Object item = null;
        Object template = null;
        Class<?> itemClass = null;
        for (Object existing : existingItems) {
            if (existing == null || !base.isInstance(existing)) continue;
            Class<?> candidate = existing.getClass();
            try {
                item = instantiate(candidate, msgClass, msg, hostContext);
                template = existing;
                itemClass = candidate;
                break;
            } catch (Throwable ignored) {
                try {
                    item = cloneWithoutConstructor(existing, msgClass, msg);
                    template = existing;
                    itemClass = candidate;
                    break;
                } catch (Throwable ignoredAgain) {
                }
            }
        }
        if (item == null || itemClass == null) throw new NoSuchMethodException("No reusable QQ menu item instance");
        installOverrides(base, itemClass, template, hostResources, clickMethod.getName(), menuIcon);
        ACTIONS.put(item, action);
        return item;
    }

    /** Uses the same final menu-view interception point as QFun, so the module avatar does not
     * depend on QQ accepting an injected resource id. */
    static void installCustomView(Class<?> layoutClass, Class<?> itemClass, Context context) {
        if (layoutClass == null || itemClass == null || context == null) return;
        String scanKey = layoutClass.getName() + ':' + itemClass.getName();
        if (!VIEW_SCANNED.add(scanKey)) return;
        Class<?> current = layoutClass;
        while (current != null && current != Object.class) {
            // QFun's QQ 9.2.75 path: View method(int, CopyMenuItem, boolean, float[]).
            for (Method method : current.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (!View.class.isAssignableFrom(method.getReturnType()) || params.length != 4
                        || params[0] != int.class || params[2] != boolean.class
                        || params[3] != float[].class
                        || !(params[1].isAssignableFrom(itemClass) || itemClass.isAssignableFrom(params[1]))) continue;
                hookViewMethod(current, method, itemClass, context, "exact");
            }
            // Conservative fallback for minor QQ signature changes.
            for (Method method : current.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (!View.class.isAssignableFrom(method.getReturnType()) || params.length < 2) continue;
                hookViewMethod(current, method, itemClass, context, "fallback");
            }
            current = current.getSuperclass();
        }
    }

    private static void hookViewMethod(Class<?> owner, Method method, Class<?> itemClass,
                                       Context context, String kind) {
        if (!method.getReturnType().isAssignableFrom(LinearLayout.class)) return;
        String key = "view:" + owner.getName() + '#' + method.toGenericString() + ':' + itemClass.getName();
        if (!HOOKED.add(key)) return;
        try {
            method.setAccessible(true);
            XposedBridge.hookMethod(method, new XC_MethodHook(20000) {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object owned = null;
                    if (param.args != null) {
                        for (Object arg : param.args) {
                            if (arg != null && itemClass.isInstance(arg) && ACTIONS.containsKey(arg)) {
                                owned = arg;
                                break;
                            }
                        }
                    }
                    if (owned == null) return;
                    Runnable action = ACTIONS.get(owned);
                    Context viewContext = param.thisObject instanceof View
                            ? ((View) param.thisObject).getContext() : context;
                    param.setResult(createMenuView(viewContext, param.thisObject, action));
                }
            });
        } catch (Throwable ignored) {
            HOOKED.remove(key);
        }
    }

    private static View createMenuView(Context context, Object expandable, Runnable action) {
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(context, 13), dp(context, 8), dp(context, 13), dp(context, 8));
        cell.setMinimumWidth(dp(context, 70));
        cell.setMinimumHeight(dp(context, 66));

        ImageView icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        android.graphics.drawable.GradientDrawable iconShape = new android.graphics.drawable.GradientDrawable();
        iconShape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        iconShape.setColor(Color.TRANSPARENT);
        icon.setBackground(iconShape);
        icon.setClipToOutline(true);
        Drawable drawable = loadModuleIcon(context);
        if (drawable != null) icon.setImageDrawable(drawable);
        cell.addView(icon, new LinearLayout.LayoutParams(dp(context, 24), dp(context, 24)));

        TextView title = new TextView(context);
        title.setText("秋天");
        title.setTextSize(13);
        boolean night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        title.setTextColor(night ? Color.WHITE : Color.rgb(29, 29, 31));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(context, 4);
        cell.addView(title, titleParams);
        cell.setOnClickListener(view -> {
            try {
                if (action != null) action.run();
            } finally {
                Reflector.invokeNoArgs(expandable, "dismiss");
            }
        });
        return cell;
    }

    /** Replaces QQ's stock icon after the exact "秋天" menu cell is rendered. */
    static void applyAvatarToRenderedMenu(View root, Context fallbackContext) {
        if (root == null) return;
        Context context = root.getContext() == null ? fallbackContext : root.getContext();
        Drawable avatar = loadModuleIcon(context);
        if (avatar != null) applyAvatarRecursive(root, avatar, 0);
    }

    private static boolean applyAvatarRecursive(View view, Drawable avatar, int depth) {
        if (view == null || depth > 12) return false;
        if (view instanceof TextView && "秋天".contentEquals(((TextView) view).getText())) {
            View parent = view;
            for (int level = 0; level < 3 && parent != null; level++) {
                if (parent instanceof ViewGroup && replaceFirstImage((ViewGroup) parent, avatar, 0)) return true;
                android.view.ViewParent next = parent.getParent();
                parent = next instanceof View ? (View) next : null;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (applyAvatarRecursive(group.getChildAt(i), avatar, depth + 1)) return true;
            }
        }
        return false;
    }

    private static boolean replaceFirstImage(ViewGroup group, Drawable avatar, int depth) {
        if (group == null || depth > 4) return false;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ImageView) {
                ImageView image = (ImageView) child;
                ViewGroup.LayoutParams params = image.getLayoutParams();
                if (params != null) {
                    params.width = dp(image.getContext(), 24);
                    params.height = dp(image.getContext(), 24);
                    image.setLayoutParams(params);
                }
                image.setImageDrawable(avatar);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setAdjustViewBounds(false);
                image.setClipToOutline(true);
                return true;
            }
            if (child instanceof ViewGroup && replaceFirstImage((ViewGroup) child, avatar, depth + 1)) return true;
        }
        return false;
    }

    private static Drawable loadModuleIcon(Context context) {
        Drawable drawable = ModuleIcon.load(context);
        if (drawable != null) return drawable;
        try {
            return context.getResources().getDrawable(android.R.drawable.sym_def_app_icon,
                    context.getTheme());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static Class<?> resolveMenuBase(ClassLoader loader) throws Exception {
        Throwable last = null;
        for (String name : new String[]{
                "com.tencent.qqnt.aio.menu.ui.e",
                "com.tencent.qqnt.aio.menu.ui.f",
                "com.tencent.qqnt.aio.menu.ui.d",
                "com.tencent.qqnt.aio.menu.ui.AbstractQQCustomMenuItem"}) {
            try {
                Class<?> candidate = loader.loadClass(name);
                int strings = 0;
                int ints = 0;
                int clicks = 0;
                for (Method method : menuMethods(candidate)) {
                    if (method.getParameterTypes().length != 0) continue;
                    if (method.getReturnType() == String.class) strings++;
                    else if (method.getReturnType() == int.class) ints++;
                    else if (method.getReturnType() == void.class) clicks++;
                }
                if (strings >= 1 && ints >= 1 && clicks >= 1) return candidate;
            } catch (Throwable e) {
                last = e;
            }
        }
        throw new ClassNotFoundException("QQ NT menu base e/f/d", last);
    }

    private static void installOverrides(Class<?> base, Class<?> itemClass, Object template,
                                         Resources hostResources, String clickName, int menuIcon) throws Exception {
        int titleHooks = 0;
        for (Method method : menuMethods(base)) {
            if (method.getParameterTypes().length == 0 && method.getReturnType() == String.class) {
                try {
                    hookImplementation(itemClass, method.getName(), String.class, new XC_MethodHook(100) {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (ACTIONS.containsKey(param.thisObject)) param.setResult("秋天");
                        }
                    });
                    titleHooks++;
                } catch (Throwable ignored) {
                }
            }
        }
        if (titleHooks == 0) throw new NoSuchMethodException(itemClass.getName() + " menu title");
        List<Method> intMethods = new ArrayList<>();
        for (Method method : menuMethods(base)) {
            if (method.getParameterTypes().length == 0 && method.getReturnType() == int.class) intMethods.add(method);
        }
        Method iconMethod = detectIconMethod(intMethods, template, hostResources);
        Method idMethod = null;
        for (Method method : intMethods) {
            if ("c".equals(method.getName())) idMethod = method;
        }
        if (iconMethod == null) {
            for (Method method : intMethods) {
                if ("b".equals(method.getName())) {
                    iconMethod = method;
                    break;
                }
            }
        }
        if (iconMethod == null && !intMethods.isEmpty()) iconMethod = intMethods.get(0);
        if (idMethod == iconMethod) idMethod = null;
        if (idMethod == null) {
            for (Method method : intMethods) {
                if (method != iconMethod) {
                    idMethod = method;
                    break;
                }
            }
        }
        if (iconMethod != null) {
            try {
                hookInt(itemClass, iconMethod.getName(),
                        menuIcon > 0 ? menuIcon : android.R.drawable.sym_def_app_icon);
            } catch (Throwable ignored) {
            }
        }
        if (idMethod != null) {
            try {
                hookInt(itemClass, idMethod.getName(), 0x425542);
            } catch (Throwable ignored) {
            }
        }
        hookImplementation(itemClass, clickName, void.class, new XC_MethodHook(100) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Runnable action = ACTIONS.get(param.thisObject);
                if (action == null) return;
                param.setResult(null);
                action.run();
            }
        });
    }

    private static Method detectIconMethod(List<Method> methods, Object template, Resources resources) {
        if (template == null || resources == null) return null;
        for (Method method : methods) {
            try {
                method.setAccessible(true);
                Object raw = method.invoke(template);
                if (!(raw instanceof Number)) continue;
                int value = ((Number) raw).intValue();
                if (value <= 0) continue;
                String type = resources.getResourceTypeName(value);
                if ("drawable".equals(type) || "mipmap".equals(type)) return method;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void hookInt(Class<?> itemClass, String name, int value) throws Exception {
        hookImplementation(itemClass, name, int.class, new XC_MethodHook(100) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (ACTIONS.containsKey(param.thisObject)) param.setResult(value);
            }
        });
    }

    private static List<Method> menuMethods(Class<?> type) {
        List<Method> result = new ArrayList<>();
        Set<String> signatures = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                String key = method.getName() + ':' + method.getReturnType().getName();
                if (method.getParameterTypes().length == 0 && signatures.add(key)) result.add(method);
            }
            current = current.getSuperclass();
        }
        return result;
    }

    private static void hookImplementation(Class<?> itemClass, String name, Class<?> returnType,
                                           XC_MethodHook callback) throws Exception {
        Class<?> current = itemClass;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterTypes().length != 0
                        || method.getReturnType() != returnType || Modifier.isAbstract(method.getModifiers())) continue;
                String key = current.getName() + '#' + name + ':' + returnType.getName();
                if (HOOKED.add(key)) {
                    method.setAccessible(true);
                    XposedBridge.hookMethod(method, callback);
                }
                return;
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(itemClass.getName() + '.' + name);
    }

    private static Object instantiate(Class<?> itemClass, Class<?> msgClass, Object msg,
                                      Context context) throws Exception {
        for (Constructor<?> constructor : itemClass.getDeclaredConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            boolean acceptsMessage = false;
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                if (!acceptsMessage && (types[i] == msgClass || types[i].isInstance(msg))) {
                    args[i] = msg;
                    acceptsMessage = true;
                } else if (context != null && Context.class.isAssignableFrom(types[i])) {
                    args[i] = context;
                } else {
                    args[i] = defaultValue(types[i]);
                }
            }
            if (!acceptsMessage) continue;
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            } catch (Throwable ignored) {
            }
        }
        throw new NoSuchMethodException(itemClass.getName() + " message constructor");
    }

    private static Object cloneWithoutConstructor(Object source, Class<?> msgClass, Object msg) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeClass.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        Object unsafe = singleton.get(null);
        Method allocate = unsafeClass.getDeclaredMethod("allocateInstance", Class.class);
        Object target = allocate.invoke(unsafe, source.getClass());
        Class<?> current = source.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    field.set(target, field.get(source));
                } catch (Throwable ignored) {
                }
            }
            current = current.getSuperclass();
        }
        Field message = Reflector.firstInstanceField(target.getClass(), msgClass);
        if (message != null) message.set(target, msg);
        return target;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }
}
