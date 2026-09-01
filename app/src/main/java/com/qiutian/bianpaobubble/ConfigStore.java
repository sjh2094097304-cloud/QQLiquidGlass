package com.qiutian.bianpaobubble;

import android.content.SharedPreferences;
import android.os.Bundle;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Transaction rules shared by the exported provider and an isolated QQ host. */
public final class ConfigStore {
    private ConfigStore() {}

    public static Bundle call(SharedPreferences prefs, String method, Bundle extras, boolean local) {
        synchronized (prefs) {
            try {
                if (local && prefs.getInt("_schemaVersion", 0) < 37) {
                    Map<String, ?> old = prefs.getAll();
                    if (old.containsKey("pool") || old.containsKey("masterEnabled") || old.containsKey("antiRevokeEnabled")) {
                        // 3.6 did not record a dirty flag for every offline edit. Preserve
                        // the QQ copy once during migration before contacting any provider.
                        commit(prefs, prefs.edit().putInt("_schemaVersion", 37).putBoolean("_pendingSync", true), old);
                    }
                }
                if ("getConfig".equals(method)) return AppConfig.toBundle(prefs);
                if ("healthCheck".equals(method)) {
                    long token = System.nanoTime();
                    Map<String, ?> before = prefs.getAll();
                    commit(prefs, prefs.edit().putLong("_healthProbe", token), before);
                    boolean valid = prefs.getLong("_healthProbe", Long.MIN_VALUE) == token;
                    commit(prefs, prefs.edit().remove("_healthProbe"), before);
                    Bundle value = AppConfig.toBundle(prefs);
                    value.putBoolean("_healthOk", valid);
                    return value;
                }
                Map<String, ?> before = prefs.getAll();
                SharedPreferences.Editor edit = prefs.edit();
                boolean setting = false;
                if ("saveSettings".equals(method)) {
                    if (extras == null || extras.getIntArray("pool") == null) throw new IllegalArgumentException("缺少气泡配置");
                    for (int id : extras.getIntArray("pool")) if (!AppConfig.validId(id)) throw new IllegalArgumentException("无效气泡 ID：" + id);
                    int requested = extras.getInt("lockedId", 0);
                    if (requested != 0 && !AppConfig.validId(requested)) throw new IllegalArgumentException("无效固定 ID");
                    Bundle normalized = AppConfig.normalize(extras);
                    writeSettings(edit, normalized);
                    if (extras.containsKey("antiRevokeEnabled")) edit.putBoolean("antiRevokeEnabled", extras.getBoolean("antiRevokeEnabled", false));
                    if (extras.containsKey(AppConfig.QQ_NOTICE_V135)) edit.putBoolean(AppConfig.QQ_NOTICE_V135, extras.getBoolean(AppConfig.QQ_NOTICE_V135, false));
                    if (!normalized.getBoolean("masterEnabled", false)) edit.putInt("lastAppliedId", 0);
                    setting = true;
                } else if ("addBubble".equals(method) || "applyBubble".equals(method) || "removeBubble".equals(method)) {
                    int id = extras == null ? 0 : extras.getInt("id", 0);
                    if (!AppConfig.validId(id)) throw new IllegalArgumentException("无效气泡 ID：" + id);
                    List<Integer> pool = AppConfig.readPool(prefs);
                    if ("removeBubble".equals(method)) {
                        pool.remove(Integer.valueOf(id));
                        if (pool.isEmpty() || prefs.getInt("lockedId", 0) == id) {
                            edit.putBoolean("lockedEnabled", false).putInt("lockedId", 0).putInt("lastAppliedId", 0);
                            boolean random = !pool.isEmpty() && prefs.getBoolean("masterEnabled", true)
                                    && prefs.getBoolean("randomEnabled", false);
                            edit.putBoolean("masterEnabled", random).putBoolean("randomEnabled", random);
                        }
                    } else {
                        if (!pool.contains(id)) pool.add(id);
                        if (pool.size() > AppConfig.MAX_IDS) throw new IllegalArgumentException("气泡 ID 不能超过 " + AppConfig.MAX_IDS + " 个");
                        if ("applyBubble".equals(method)) edit.putBoolean("masterEnabled", true).putBoolean("randomEnabled", false)
                                .putBoolean("lockedEnabled", true).putInt("lockedId", id).putInt("lastAppliedId", id);
                    }
                    edit.putString("pool", AppConfig.poolJson(pool));
                    setting = true;
                } else if ("restoreDefault".equals(method)) {
                    edit.putBoolean("masterEnabled", false).putBoolean("randomEnabled", false).putBoolean("lockedEnabled", false)
                            .putInt("lockedId", 0).putInt("lastAppliedId", 0);
                    setting = true;
                } else if ("setAntiRevoke".equals(method)) {
                    edit.putBoolean("antiRevokeEnabled", extras != null && extras.getBoolean("enabled", false));
                    setting = true;
                } else if ("acceptNoticeV36Final1".equals(method)) {
                    edit.putBoolean(AppConfig.QQ_NOTICE_V135, true);
                    setting = true;
                } else if ("reportHook".equals(method)) {
                    edit.putLong("lastHookPing", System.currentTimeMillis()).putString("lastHookVersion", extras == null ? "" : extras.getString("version", ""));
                } else if ("recordApplied".equals(method)) {
                    int id = extras == null ? 0 : extras.getInt("id", 0);
                    if (AppConfig.validId(id)) edit.putInt("lastAppliedId", id);
                } else if ("appendLog".equals(method)) {
                    String message = extras == null ? "" : extras.getString("message", "");
                    appendLog(prefs, edit, message);
                } else if ("clearLog".equals(method) || "clearRuntimeCache".equals(method)) {
                    edit.remove("log").remove("lastLogMessage").remove("lastLogTime");
                } else {
                    throw new IllegalArgumentException("不支持的配置操作：" + method);
                }
                if (setting) {
                    long previous = prefs.getLong("settingsRevision", 0L);
                    long incoming = extras == null ? 0L : extras.getLong("settingsRevision", 0L);
                    long revision = Math.max(previous, incoming);
                    edit.putLong("settingsRevision", Math.max(System.currentTimeMillis(), revision == Long.MAX_VALUE ? revision : revision + 1L));
                    if (local) edit.putBoolean("_pendingSync", true).putInt("_schemaVersion", 37);
                }
                commit(prefs, edit, before);
                return AppConfig.toBundle(prefs);
            } catch (Throwable error) {
                Bundle failed = new Bundle();
                failed.putBoolean("_ok", false);
                failed.putBoolean("_healthOk", false);
                failed.putString("_error", error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage()));
                return failed;
            }
        }
    }

    public static void writeSettings(SharedPreferences.Editor edit, Bundle config) {
        List<Integer> pool = new ArrayList<>();
        int[] ids = config.getIntArray("pool");
        if (ids != null) for (int id : ids) pool.add(id);
        edit.putString("pool", AppConfig.poolJson(pool)).putBoolean("masterEnabled", config.getBoolean("masterEnabled", false))
                .putBoolean("randomEnabled", config.getBoolean("randomEnabled", false))
                .putBoolean("lockedEnabled", config.getBoolean("lockedEnabled", false)).putInt("lockedId", config.getInt("lockedId", 0));
    }

    public static boolean mirror(SharedPreferences prefs, Bundle config, boolean acknowledge, long expectedRevision) {
        synchronized (prefs) {
            if (prefs.getLong("settingsRevision", 0L) != expectedRevision) return false;
            if (!acknowledge && prefs.getBoolean("_pendingSync", false)) return false;
            Bundle normalized = AppConfig.normalize(config);
            Bundle current = AppConfig.toBundle(prefs);
            boolean same = java.util.Arrays.equals(current.getIntArray("pool"), normalized.getIntArray("pool"))
                    && current.getInt("lockedId", 0) == normalized.getInt("lockedId", 0)
                    && current.getLong("settingsRevision", 0L) == normalized.getLong("settingsRevision", 0L);
            for (String key : new String[]{"masterEnabled", "randomEnabled", "lockedEnabled", "antiRevokeEnabled", AppConfig.QQ_NOTICE_V135}) {
                same &= current.getBoolean(key, false) == normalized.getBoolean(key, false);
            }
            if (same && (!acknowledge || !current.getBoolean("_pendingSync", false))) return true;
            Map<String, ?> before = prefs.getAll();
            SharedPreferences.Editor edit = prefs.edit();
            writeSettings(edit, normalized);
            edit.putBoolean("antiRevokeEnabled", config.getBoolean("antiRevokeEnabled", false))
                    .putBoolean(AppConfig.QQ_NOTICE_V135, config.getBoolean(AppConfig.QQ_NOTICE_V135, false))
                    .putLong("settingsRevision", config.getLong("settingsRevision", 0L)).putInt("_schemaVersion", 37);
            if (acknowledge) edit.putBoolean("_pendingSync", false);
            try { commit(prefs, edit, before); return true; }
            catch (Throwable error) { return false; }
        }
    }

    private static void commit(SharedPreferences prefs, SharedPreferences.Editor edit, Map<String, ?> before) {
        if (edit.commit()) return;
        // A failed Android commit has already changed the process-local map. Restore it
        // so a later read cannot mistake unsaved values for a successful transaction.
        SharedPreferences.Editor rollback = prefs.edit().clear();
        for (Map.Entry<String, ?> entry : before.entrySet()) {
            Object v = entry.getValue(); String k = entry.getKey();
            if (v instanceof String) rollback.putString(k, (String) v);
            else if (v instanceof Integer) rollback.putInt(k, (Integer) v);
            else if (v instanceof Long) rollback.putLong(k, (Long) v);
            else if (v instanceof Boolean) rollback.putBoolean(k, (Boolean) v);
            else if (v instanceof Float) rollback.putFloat(k, (Float) v);
            else if (v instanceof Set) {
                @SuppressWarnings("unchecked") Set<String> set = (Set<String>) v;
                rollback.putStringSet(k, set);
            }
        }
        rollback.commit();
        throw new IllegalStateException("配置未能写入存储，请重试");
    }

    private static void appendLog(SharedPreferences prefs, SharedPreferences.Editor edit, String message) {
        if (message == null || message.trim().isEmpty()) return;
        long now = System.currentTimeMillis();
        String compact = message.trim().replace('\n', ' ');
        if (compact.length() > 1200) compact = compact.substring(0, 1200);
        if (compact.equals(prefs.getString("lastLogMessage", "")) && now - prefs.getLong("lastLogTime", 0L) < 60_000L) return;
        String[] lines = prefs.getString("log", "").split("\n");
        StringBuilder out = new StringBuilder();
        for (int i = Math.max(0, lines.length - 23); i < lines.length; i++) if (!lines[i].isEmpty()) out.append(lines[i]).append('\n');
        out.append('[').append(new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(new Date(now)))
                .append("] ").append(compact).append('\n');
        edit.putString("log", out.toString()).putString("lastLogMessage", compact).putLong("lastLogTime", now);
    }
}
