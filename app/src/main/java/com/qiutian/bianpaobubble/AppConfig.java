package com.qiutian.bianpaobubble;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AppConfig {
    public static final int MAX_IDS = 300;
    public static final String PREFS = "bubble_config";
    public static final String AUTHORITY = "com.qiutian.bianpaobubble.v36.config";
    public static final String MIN_QQ_VERSION = "9.2.75";
    public static final String AUTHOR_QQ = "3694476602";
    public static final String GROUP_QQ = "853250567";
    /** Retained to preserve already accepted notices across the 3.7 upgrade. */
    public static final String EXTERNAL_NOTICE_V135 = "externalNoticeAcceptedV36Final1";
    public static final String QQ_NOTICE_V135 = "qqNoticeAcceptedV36Final1";

    private AppConfig() {}

    public static boolean validId(int id) { return id >= 1000; }

    public static int[] cleanPool(int[] source) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (source != null) for (int id : source) {
            if (validId(id)) ids.add(id);
            if (ids.size() > MAX_IDS) throw new IllegalArgumentException("气泡 ID 不能超过 " + MAX_IDS + " 个");
        }
        int[] result = new int[ids.size()];
        int i = 0;
        for (int id : ids) result[i++] = id;
        return result;
    }

    /** One normalization rule for the UI, provider and isolated host process. */
    public static Bundle normalize(Bundle source) {
        Bundle result = new Bundle(source);
        int[] pool = cleanPool(source.getIntArray("pool"));
        int id = source.getInt("lockedId", 0);
        if (!validId(id)) id = 0;
        boolean master = source.getBoolean("masterEnabled", false);
        boolean locked = master && source.getBoolean("lockedEnabled", false) && id > 0;
        boolean random = master && !locked && source.getBoolean("randomEnabled", false) && pool.length > 0;
        result.putIntArray("pool", pool);
        result.putInt("lockedId", id);
        result.putBoolean("lockedEnabled", locked);
        result.putBoolean("randomEnabled", random);
        result.putBoolean("masterEnabled", locked || random);
        return result;
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<Integer> readPool(SharedPreferences prefs) {
        String raw = prefs.getString("pool", "[2119335,2119336,2119337,2119338,2119339,2119340,2119341,2119342,2119343,2119344]");
        Set<Integer> unique = new LinkedHashSet<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                int id = array.optInt(i, 0);
                if (validId(id) && unique.size() < MAX_IDS) unique.add(id);
            }
        } catch (Throwable ignored) {
            String[] values = raw == null ? new String[0] : raw.split("[^0-9]+");
            for (String value : values) {
                try {
                    int id = Integer.parseInt(value);
                    if (validId(id) && unique.size() < MAX_IDS) unique.add(id);
                } catch (Throwable ignoredAgain) {
                }
            }
        }
        return new ArrayList<>(unique);
    }

    public static boolean writePool(SharedPreferences prefs, List<Integer> ids) {
        return prefs.edit().putString("pool", poolJson(ids)).commit();
    }

    public static String poolJson(List<Integer> ids) {
        JSONArray array = new JSONArray();
        Set<Integer> unique = new LinkedHashSet<>(ids == null ? new ArrayList<>() : ids);
        for (Integer id : unique) if (id != null && validId(id)) array.put(id);
        return array.toString();
    }

    public static Bundle toBundle(SharedPreferences prefs) {
        List<Integer> pool = readPool(prefs);
        int[] values = new int[pool.size()];
        for (int i = 0; i < pool.size(); i++) values[i] = pool.get(i);
        Bundle result = new Bundle();
        result.putBoolean("masterEnabled", prefs.getBoolean("masterEnabled", true));
        result.putBoolean("randomEnabled", prefs.getBoolean("randomEnabled", true));
        result.putBoolean("lockedEnabled", prefs.getBoolean("lockedEnabled", false));
        result.putInt("lockedId", prefs.getInt("lockedId", 0));
        result.putInt("lastAppliedId", prefs.getInt("lastAppliedId", 0));
        result.putBoolean("antiRevokeEnabled", prefs.getBoolean("antiRevokeEnabled", false));
        result.putBoolean(QQ_NOTICE_V135, prefs.getBoolean(QQ_NOTICE_V135, false));
        result.putIntArray("pool", values);
        result.putLong("lastHookPing", prefs.getLong("lastHookPing", 0L));
        result.putString("lastHookVersion", prefs.getString("lastHookVersion", ""));
        result.putString("detectedFpaVersion", prefs.getString("detectedFpaVersion", ""));
        result.putString("log", prefs.getString("log", ""));
        result.putLong("settingsRevision", prefs.getLong("settingsRevision", 0L));
        result.putBoolean("_pendingSync", prefs.getBoolean("_pendingSync", false));
        result.putBoolean("_ok", true);
        return normalize(result);
    }

    public static int compareVersion(String left, String right) {
        String[] a = left == null ? new String[0] : left.replaceAll("[^0-9.]", "").split("\\.");
        String[] b = right == null ? new String[0] : right.replaceAll("[^0-9.]", "").split("\\.");
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int ai = parsePart(a, i);
            int bi = parsePart(b, i);
            if (ai != bi) return ai < bi ? -1 : 1;
        }
        return 0;
    }

    private static int parsePart(String[] parts, int index) {
        if (index >= parts.length || parts[index].isEmpty()) return 0;
        try {
            return Integer.parseInt(parts[index]);
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
