package com.qiutian.bianpaobubble.hook;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** QQNT recall interceptor; ordinary and unknown packets always follow QQ's own path. */
final class AntiRevokeHook {
    private static final String MSG_PUSH = "trpc.msg.olpush.OlPushService.MsgPush";
    private static final String SYNC_PUSH = "trpc.msg.register_proxy.RegisterProxy.InfoSyncPush";
    private static final String NOTICE = "已拦截对方撤回，消息已保留";
    private static final AtomicBoolean FIRST_INTERCEPT_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean PENDING = new AtomicBoolean(false);
    private static final AtomicBoolean NOTICE_QUEUED = new AtomicBoolean(false);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static WeakReference<TextView> visibleNotice = new WeakReference<>(null);
    private static Runnable dismissNotice;
    private AntiRevokeHook() {}

    static void install(ClassLoader loader, Context context) throws Exception {
        Class<?> proxy = loader.loadClass("com.tencent.qqnt.kernel.nativeinterface.IQQNTWrapperSession$CppProxy");
        int installed = 0;
        for (Method method : proxy.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (!"onMsfPush".equals(method.getName()) || method.getReturnType() != void.class
                    || parameters.length < 2 || parameters[0] != String.class || parameters[1] != byte[].class) continue;
            method.setAccessible(true);
            XposedBridge.hookMethod(method, new XC_MethodHook(95) {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (param.args == null || param.args.length < 2) return;
                        String command = param.args[0] instanceof String ? (String) param.args[0] : "";
                        if (!MSG_PUSH.equals(command) && !SYNC_PUSH.equals(command)) return;
                        HookStatus.seen("防撤回");
                        if (!HostConfig.get(context).getBoolean("antiRevokeEnabled", false)) return;
                        byte[] payload = param.args[1] instanceof byte[] ? (byte[]) param.args[1] : null;
                        if (payload == null || payload.length == 0) return;
                        boolean changed = false;
                        if (MSG_PUSH.equals(command)) {
                            if (ProtoLite.isRecallMsgPush(payload) && !ProtoLite.isSelfMsgPush(payload)) {
                                param.setResult(null); // Skip the known void callback; never send an invalid empty protobuf to QQ.
                                changed = true;
                            }
                        } else {
                            ProtoLite.RewriteResult result = ProtoLite.stripSyncRecall(payload);
                            if (result.changed) { param.args[1] = result.bytes; changed = true; }
                        }
                        if (!changed) return;
                        if (FIRST_INTERCEPT_LOGGED.compareAndSet(false, true)) HostConfig.appendLog(context, "防撤回已首次拦截撤回指令");
                        queueNotice();
                    } catch (Throwable error) { HookLog.error(context, "防撤回处理失败", error); }
                }
            });
            installed++;
        }
        if (installed == 0) throw new NoSuchMethodException(proxy.getName() + ".onMsfPush(String, byte[]) void");
    }

    private static void queueNotice() {
        PENDING.set(true);
        if (!NOTICE_QUEUED.compareAndSet(false, true)) return;
        MAIN.postDelayed(() -> {
            NOTICE_QUEUED.set(false);
            onActivityResumed(BubbleXposedInit.currentActivity());
        }, 180L);
    }
    static void onActivityResumed(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || !PENDING.get()) return;
        if (Looper.myLooper() != Looper.getMainLooper()) { MAIN.post(() -> onActivityResumed(activity)); return; }
        if (!PENDING.compareAndSet(true, false)) return;
        if (!showInQq(activity)) {
            try { Toast.makeText(activity, NOTICE, Toast.LENGTH_LONG).show(); }
            catch (Throwable ignored) { PENDING.set(true); }
        }
    }
    private static boolean showInQq(Activity activity) {
        try {
            if (activity.getWindow() == null) return false;
            View decor = activity.getWindow().getDecorView();
            if (!(decor instanceof ViewGroup)) return false;
            // The activity content may be a ConstraintLayout or a framework wrapper.
            // Attach our own small overlay to the decor instead of assuming content is a FrameLayout.
            ViewGroup root = (ViewGroup) decor;
            TextView old = visibleNotice.get();
            if (dismissNotice != null) MAIN.removeCallbacks(dismissNotice);
            if (old != null && old.getParent() instanceof ViewGroup) ((ViewGroup) old.getParent()).removeView(old);
            TextView notice = new TextView(activity);
            notice.setText(NOTICE);
            notice.setTextSize(12f);
            notice.setTextColor(Color.rgb(232, 61, 139));
            notice.setGravity(Gravity.CENTER);
            notice.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 14), dp(activity, 8));
            android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
            background.setColor(Color.argb(248, 255, 242, 249));
            background.setCornerRadius(dp(activity, 18));
            background.setStroke(dp(activity, 1), Color.argb(185, 255, 255, 255));
            notice.setBackground(background);
            notice.setElevation(dp(activity, 12));
            notice.setMaxWidth(Math.max(dp(activity, 180), root.getWidth() - dp(activity, 32)));
            Rect visible = new Rect();
            decor.getWindowVisibleDisplayFrame(visible);
            int[] location = new int[2];
            decor.getLocationOnScreen(location);
            int obscuredBottom = Math.max(0, location[1] + decor.getHeight() - visible.bottom);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            params.bottomMargin = obscuredBottom + dp(activity, 80);
            root.addView(notice, params);
            visibleNotice = new WeakReference<>(notice);
            dismissNotice = () -> {
                if (notice.getParent() instanceof ViewGroup) ((ViewGroup) notice.getParent()).removeView(notice);
                if (visibleNotice.get() == notice) { visibleNotice.clear(); dismissNotice = null; }
            };
            MAIN.postDelayed(dismissNotice, 3_200L);
            return true;
        } catch (Throwable ignored) { return false; }
    }
    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
