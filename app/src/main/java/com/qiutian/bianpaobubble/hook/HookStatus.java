package com.qiutian.bianpaobubble.hook;

import java.util.LinkedHashMap;
import java.util.Map;

/** In-process hook evidence, separate from a possibly delayed provider heartbeat. */
final class HookStatus {
    private static final Map<String, String> FEATURES = new LinkedHashMap<>();
    private static volatile boolean initialized;
    private HookStatus() {}
    static void started() { initialized = true; }
    static boolean active() { return initialized; }
    static synchronized void installed(String name) { FEATURES.put(name, "已挂载（待触发验证）"); }
    static synchronized void seen(String name) { FEATURES.put(name, "已触发"); }
    static synchronized void failed(String name, Throwable error) {
        FEATURES.put(name, "未挂载：" + error.getClass().getSimpleName());
    }
    static synchronized boolean available(String name) {
        String state = FEATURES.get(name);
        return state != null && state.startsWith("已");
    }
    static boolean requiredReady(boolean anti) {
        return active() && available("发送气泡") && available("长按菜单") && available("商城识别")
                && (!anti || available("防撤回"));
    }
    static synchronized String describe() {
        StringBuilder result = new StringBuilder();
        for (String name : new String[]{"发送气泡", "长按菜单", "商城识别", "防撤回"}) {
            result.append(name).append(" Hook：").append(FEATURES.containsKey(name) ? FEATURES.get(name) : "尚未初始化").append('\n');
        }
        return result.toString();
    }
}
