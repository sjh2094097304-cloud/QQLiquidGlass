package com.qiutian.bianpaobubble.hook;

import android.content.Context;

import java.util.Map;
import java.util.LinkedHashMap;
import android.os.SystemClock;

import de.robv.android.xposed.XposedBridge;

final class HookLog {
    private static final Map<String, Long> RECENT = new LinkedHashMap<>();

    private HookLog() {}

    static void info(String message) {
        XposedBridge.log("百变气泡 | " + message);
    }

    static void error(Context context, String where, Throwable error) {
        String type = error == null ? "未知异常" : error.getClass().getSimpleName();
        String detail = error == null || error.getMessage() == null ? "" : error.getMessage();
        String code = errorCode(where, type);
        String trace = "";
        if (error != null && error.getStackTrace() != null && error.getStackTrace().length > 0) {
            StackTraceElement top = error.getStackTrace()[0];
            trace = " @ " + top.getClassName() + "." + top.getMethodName() + ":" + top.getLineNumber();
        }
        String message = "[" + code + "] " + where + " | " + type
                + (detail.isEmpty() ? "" : ": " + detail) + trace;
        if (message.length() > 1200) message = message.substring(0, 1200);
        long now = SystemClock.elapsedRealtime();
        synchronized (RECENT) {
            Long previous = RECENT.get(message);
            if (previous != null && now - previous < 60_000L) return;
            RECENT.put(message, now);
            while (RECENT.size() > 128) RECENT.remove(RECENT.keySet().iterator().next());
        }
        XposedBridge.log("百变气泡 | " + message);
        if (context != null) HostConfig.appendLog(context, message);
    }

    private static String errorCode(String where, String type) {
        int value = (String.valueOf(where) + '|' + type).hashCode() & 0x7fffffff;
        String hex = Integer.toHexString(value).toUpperCase();
        while (hex.length() < 6) hex = "0" + hex;
        return "BB-" + hex.substring(Math.max(0, hex.length() - 6));
    }
}
