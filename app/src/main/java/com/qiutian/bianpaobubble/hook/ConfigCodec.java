package com.qiutian.bianpaobubble.hook;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.regex.Pattern;

/** Stable, permission-free text format used by the in-QQ import/export surface. */
final class ConfigCodec {
    static final int MAX_IDS = 300;
    private static final Pattern ID = Pattern.compile("[0-9]{4,10}");

    private ConfigCodec() {}

    static String encode(String name, boolean randomEnabled, boolean lockedEnabled,
                         boolean antiRevokeEnabled, int lockedId, List<Integer> ids) {
        String mode = lockedEnabled ? "independent" : randomEnabled ? "random" : "off";
        StringBuilder output = new StringBuilder(256);
        output.append("{\n")
                .append("  \"format\": \"bianbian-bubble-config\",\n")
                .append("  \"version\": \"3.7\",\n")
                .append("  \"name\": \"").append(escape(safeName(name))).append("\",\n")
                .append("  \"mode\": \"").append(mode).append("\",\n")
                .append("  \"randomEnabled\": ").append(randomEnabled).append(",\n")
                .append("  \"lockedEnabled\": ").append(lockedEnabled).append(",\n")
                .append("  \"antiRevokeEnabled\": ").append(antiRevokeEnabled).append(",\n")
                .append("  \"lockedId\": ").append(Math.max(0, lockedId)).append(",\n")
                .append("  \"pool\": [");
        List<Integer> unique = uniqueIds(ids);
        for (int i = 0; i < unique.size(); i++) {
            if (i > 0) output.append(", ");
            output.append(unique.get(i));
        }
        return output.append("]\n}").toString();
    }

    static DecodedConfig decode(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("配置内容不能为空");
        if (text.length() > 65_536) throw new IllegalArgumentException("配置内容过大");
        if (text.startsWith("{")) return decodeObject(text);
        if (text.startsWith("[")) {
            if (!text.endsWith("]")) throw new IllegalArgumentException("气泡 ID 数组没有正确结束");
            List<Integer> ids = parseArray(text.substring(1, text.length() - 1));
            if (ids.isEmpty()) throw new IllegalArgumentException("没有找到有效气泡 ID");
            return new DecodedConfig("导入配置", true, false, false, 0, ids);
        }
        List<Integer> ids = parseLooseList(text);
        if (ids.isEmpty()) throw new IllegalArgumentException("没有找到有效气泡 ID");
        return new DecodedConfig("导入配置", true, false, false, 0, ids);
    }

    private static DecodedConfig decodeObject(String text) {
        try {
            checkNesting(text);
            JSONTokener tokenizer = new JSONTokener(text);
            Object parsed = tokenizer.nextValue();
            if (!(parsed instanceof JSONObject) || tokenizer.nextClean() != 0) throw new IllegalArgumentException("配置 JSON 格式错误");
            JSONObject object = (JSONObject) parsed;
            String format = jsonString(object, "format");
            if (format != null && !"bianbian-bubble-config".equals(format)) throw new IllegalArgumentException("不是百变气泡配置");
            if (!object.has("mode") && !object.has("randomEnabled") && !object.has("lockedEnabled")) {
                throw new IllegalArgumentException("配置缺少模式字段；仅导入 ID 请使用数组或 ID 列表");
            }
            String name = safeName(jsonString(object, "name"));
            String mode = jsonString(object, "mode");
            boolean random = jsonBoolean(object, "randomEnabled");
            boolean locked = jsonBoolean(object, "lockedEnabled");
            boolean anti = jsonBoolean(object, "antiRevokeEnabled");
            if (mode != null) {
                switch (mode.trim().toLowerCase(Locale.ROOT)) {
                    case "random": random = true; locked = false; break;
                    case "independent": case "fixed": random = false; locked = true; break;
                    case "off": random = false; locked = false; break;
                    default: throw new IllegalArgumentException("不支持的模式：" + mode);
                }
            }
            if (random && locked) throw new IllegalArgumentException("随机模式和独立模式不能同时开启");
            int lockedId = 0;
            if (object.has("lockedId")) {
                Object raw = object.get("lockedId");
                if (!(raw instanceof Number) || raw instanceof Double || raw instanceof Float) throw new IllegalArgumentException("lockedId 必须是整数");
                long number = ((Number) raw).longValue();
                if (number != 0) lockedId = checkedId(String.valueOf(number));
            }
            if (!object.has("pool") && !locked) throw new IllegalArgumentException("配置缺少气泡池");
            List<Integer> ids = new ArrayList<>();
            if (object.has("pool")) {
                Object raw = object.get("pool");
                if (!(raw instanceof JSONArray)) throw new IllegalArgumentException("pool 必须是气泡 ID 数组");
                JSONArray pool = (JSONArray) raw;
                for (int i = 0; i < pool.length(); i++) {
                    Object id = pool.get(i);
                    if (!(id instanceof String) && !(id instanceof Number) || id instanceof Double || id instanceof Float) {
                        throw new IllegalArgumentException("气泡 ID 必须是整数");
                    }
                    addUnique(ids, checkedId(String.valueOf(id)));
                }
            }
            if (locked) {
                if (lockedId <= 0) throw new IllegalArgumentException("独立模式缺少有效的 lockedId");
                if (!ids.contains(lockedId)) ids.add(0, lockedId);
            }
            if (random && ids.isEmpty()) throw new IllegalArgumentException("随机模式的气泡池不能为空");
            if (ids.size() > MAX_IDS) throw new IllegalArgumentException("气泡 ID 不能超过 " + MAX_IDS + " 个");
            return new DecodedConfig(name, random, locked, anti, lockedId, ids, object.has("antiRevokeEnabled"));
        } catch (IllegalArgumentException error) { throw error; }
        catch (Exception error) { throw new IllegalArgumentException("配置 JSON 格式错误", error); }
    }

    private static void checkNesting(String text) {
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (value == '\\') escaped = true;
                else if (value == '"') quoted = false;
                continue;
            }
            if (value == '"') quoted = true;
            else if (value == '{' || value == '[') {
                if (++depth > 16) throw new IllegalArgumentException("配置嵌套过深");
            } else if (value == '}' || value == ']') {
                if (--depth < 0) throw new IllegalArgumentException("配置 JSON 格式错误");
            } else if (value == '\'' || value == '#' || value == '/') {
                throw new IllegalArgumentException("配置 JSON 不接受单引号或注释");
            }
        }
        if (quoted || depth != 0) throw new IllegalArgumentException("配置 JSON 没有正确结束");
    }

    private static String jsonString(JSONObject object, String key) throws Exception {
        if (!object.has(key)) return null;
        Object value = object.get(key);
        if (!(value instanceof String)) throw new IllegalArgumentException(key + " 必须是文本");
        return (String) value;
    }

    private static boolean jsonBoolean(JSONObject object, String key) throws Exception {
        if (!object.has(key)) return false;
        Object value = object.get(key);
        if (!(value instanceof Boolean)) throw new IllegalArgumentException(key + " 必须是 true 或 false");
        return (Boolean) value;
    }

    private static List<Integer> parseArray(String body) {
        List<Integer> result = new ArrayList<>();
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty()) return result;
        String[] parts = trimmed.split(",", -1);
        for (String part : parts) {
            String token = part.trim();
            if (token.length() >= 2 && token.startsWith("\"") && token.endsWith("\"")) {
                token = token.substring(1, token.length() - 1).trim();
            }
            if (token.isEmpty()) throw new IllegalArgumentException("气泡 ID 数组中存在空项");
            addUnique(result, checkedId(token));
        }
        return result;
    }

    private static List<Integer> parseLooseList(String text) {
        List<Integer> result = new ArrayList<>();
        if (!text.matches("[0-9,，;；\\s]+")) throw new IllegalArgumentException("ID 列表含无效字符");
        String[] parts = text.split("[,，;；\\s]+");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            addUnique(result, checkedId(part));
        }
        return result;
    }

    private static int checkedId(String value) {
        String token = value == null ? "" : value.trim();
        if (!ID.matcher(token).matches()) {
            throw new IllegalArgumentException("无效气泡 ID：" + token);
        }
        try {
            int id = Integer.parseInt(token);
            if (id < 1000) throw new NumberFormatException("below minimum bubble id");
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("气泡 ID 超出范围：" + token);
        }
    }

    private static void addUnique(List<Integer> result, int id) {
        if (!result.contains(id)) result.add(id);
        if (result.size() > MAX_IDS) throw new IllegalArgumentException("气泡 ID 不能超过 " + MAX_IDS + " 个");
    }

    private static List<Integer> uniqueIds(List<Integer> source) {
        Set<Integer> unique = new LinkedHashSet<>();
        if (source != null) {
            for (Integer id : source) if (id != null && id >= 1000) unique.add(id);
        }
        if (unique.size() > MAX_IDS) throw new IllegalArgumentException("气泡 ID 不能超过 " + MAX_IDS + " 个");
        return new ArrayList<>(unique);
    }

    private static String safeName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) value = "我的气泡配置";
        return value.length() > 32 ? value.substring(0, 32) : value;
    }

    private static String escape(String value) {
        String quoted = JSONObject.quote(value);
        return quoted.substring(1, quoted.length() - 1);
    }

    static final class DecodedConfig {
        final String name;
        final boolean randomEnabled;
        final boolean lockedEnabled;
        final boolean antiRevokeEnabled;
        final boolean hasAntiRevokeSetting;
        final int lockedId;
        final List<Integer> ids;

        DecodedConfig(String name, boolean randomEnabled, boolean lockedEnabled,
                      boolean antiRevokeEnabled, int lockedId, List<Integer> ids) {
            this(name, randomEnabled, lockedEnabled, antiRevokeEnabled, lockedId, ids, false);
        }

        DecodedConfig(String name, boolean randomEnabled, boolean lockedEnabled,
                      boolean antiRevokeEnabled, int lockedId, List<Integer> ids, boolean hasAntiRevokeSetting) {
            this.name = safeName(name);
            this.randomEnabled = randomEnabled;
            this.lockedEnabled = lockedEnabled;
            this.antiRevokeEnabled = antiRevokeEnabled;
            this.hasAntiRevokeSetting = hasAntiRevokeSetting;
            this.lockedId = lockedId;
            this.ids = uniqueIds(ids);
        }
    }
}
