package com.qiutian.bianpaobubble.hook;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import java.util.concurrent.Callable;

/** Success feedback follows a confirmed disk write; a failure restores the visible state. */
final class ConfigUi {
    interface Refresh { void update(Bundle state); }
    private ConfigUi() {}
    static void write(Activity activity, Callable<Bundle> operation, String message, Refresh refresh) {
        long generation = HostConfig.generation();
        HostConfig.runAsync(() -> {
            Bundle result;
            try { result = operation.call(); }
            catch (Exception error) {
                result = new Bundle();
                result.putString("_error", String.valueOf(error.getMessage()));
                HookLog.error(activity, "配置保存失败", error);
            }
            boolean saved = HostConfig.success(result);
            String error = result == null ? "无返回结果" : result.getString("_error", "存储不可用");
            if (!saved) HookLog.error(activity, "配置保存失败", new IllegalStateException(error));
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (generation != HostConfig.generation()) {
                    if (!saved) Toast.makeText(activity, "一项设置保存失败，请重新打开设置确认", Toast.LENGTH_LONG).show();
                    return;
                }
                if (!saved && refresh != null) refresh.update(HostConfig.get(activity));
                String notice = saved ? message : "保存失败，已恢复已保存配置：" + error;
                if (notice != null && !notice.isEmpty()) Toast.makeText(activity, notice, Toast.LENGTH_SHORT).show();
            });
        });
    }
}
