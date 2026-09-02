package io.github.liuran001.mmliquidglass;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Locale;

/** Bounded avatar snapshots. Never recursively draws a QQ View or the page. */
final class QqAvatarBridge {
    static volatile String status = "等待识别原生账号头像";
    static volatile String diagnostics = "尚未扫描头像";
    private final WeakReference<Activity> activity;
    private final WeakReference<ViewGroup> nativeBar;
    private WeakReference<View> source = new WeakReference<>(null);
    private WeakReference<View> clickTarget = new WeakReference<>(null);
    private final UiWorkGate action = new UiWorkGate();
    private final QqFaceLoader faceLoader = new QqFaceLoader();
    private final int[] loc = new int[2], rootLoc = new int[2];
    private Bitmap bitmap;
    Bitmap copyForPreview() {
        try { return bitmap==null || bitmap.isRecycled()?null:bitmap.copy(Bitmap.Config.ARGB_8888,false); }
        catch(Throwable ignored) { return null; }
    }
    private long nextProbe, captureGeneration;
    private String account;
    private Field runtimeField;
    private int remaining;

    QqAvatarBridge(Activity owner, ViewGroup bar) {
        activity = new WeakReference<>(owner);
        nativeBar = new WeakReference<>(bar);
    }

    void invalidateAccount() { nextProbe = 0; captureGeneration++; }
    void cancel() { action.cancel(); captureGeneration++; faceLoader.cancel(); }
    void dispose() { cancel(); faceLoader.dispose(); bitmap=null; }

    void refresh(AvatarButton button) {
        long now = SystemClock.uptimeMillis();
        if (now < nextProbe) return;
        nextProbe = now + 2000;
        Activity a = activity.get();
        if (a == null || a.isDestroyed() || !button.isAttachedToWindow() || !a.hasWindowFocus()) return;
        String current = currentAccount(a);
        if (current != null && !current.equals(account)) {
            account = current;
            bitmap = null;
            source.clear();
            clickTarget.clear();
            captureGeneration++;
            faceLoader.cancel();
            button.invalidate();
            FeedbackLog.event("AVATAR_ACCOUNT", "account changed; previous image discarded");
        }
        if (account!=null && account.isEmpty()) { status="当前账号未就绪"; return; }
        if (!messageSelected()) return; // Preserve the same account's last valid image.
        locate(a);
        View view = source.get();
        if (view == null || !view.isShown() || excluded(view)) { requestFace(a,button); return; }
        Bitmap result = null;
        // QQ 9.2.75 may use a plain View whose background holds the face image.
        // Read only the recognized header subtree, not pixels from the Window.
        View candidate = view;
        float density = a.getResources().getDisplayMetrics().density;
        for (int i = 0; i < 3 && candidate != null; i++) {
            if (candidate.getWidth() > 120 * density || candidate.getHeight() > 112 * density) break;
            result = new NativeImageReader().read(candidate);
            if (result != null) break;
            candidate = candidate.getParent() instanceof View ? (View) candidate.getParent() : null;
        }
        if (result != null) {
            if (bitmap == null || !bitmap.sameAs(result)) {
                bitmap = result;
                button.invalidate();
            }
            status = "已读取原生头像图片（无屏幕取图）";
            FeedbackLog.event("AVATAR_READY", view.getClass().getName());
        } else {
            status = bitmap == null ? "原生头像图片暂未识别；未启用屏幕截图回退" : "保留当前账号最近的有效头像";
            FeedbackLog.event("AVATAR_WAIT", view.getClass().getName());
            requestFace(a,button);
        }
    }

    private void requestFace(Activity a,AvatarButton button) {
        long generation=captureGeneration;
        faceLoader.load(currentRuntime(a),a.getClassLoader(),account,decoded->{
            if(generation!=captureGeneration || !button.isAttachedToWindow()) return;
            bitmap=decoded; button.invalidate();
            status="已读取 QQ 当前账号头像解码结果（无屏幕取图）";
            FeedbackLog.event("AVATAR_DECODER_READY","current account image");
        });
    }

    private Object currentRuntime(Activity a) {
        try {
            Object app=a.getApplication();
            try {
                Class<?> mobile=a.getClassLoader().loadClass("mqq.app.MobileQQ");
                Object actual=mobile.getField("sMobileQQ").get(null);
                if(actual!=null) app=actual;
            } catch(ReflectiveOperationException ignored) { }
            if(runtimeField!=null && !runtimeField.getDeclaringClass().isInstance(app)) runtimeField=null;
            if(runtimeField==null) for(Class<?> c=app.getClass();c!=null;c=c.getSuperclass()) {
                try { runtimeField=c.getDeclaredField("mAppRuntime");runtimeField.setAccessible(true);break; }
                catch(NoSuchFieldException ignored) { }
            }
            return runtimeField==null?null:runtimeField.get(app);
        } catch(Throwable ignored) { return null; }
    }

    private String currentAccount(Activity a) {
        Object runtime=currentRuntime(a);
        if(runtime==null) return null;
        for(String name:new String[]{"getCurrentAccountUin","getAccount"}) {
            try {
                java.lang.reflect.Method getter=runtime.getClass().getMethod(name);
                getter.setAccessible(true);
                Object value=getter.invoke(runtime);
                if(value instanceof String) return (String)value;
            } catch(ReflectiveOperationException ignored) { }
        }
        return null;
    }

    private boolean messageSelected() {
        ViewGroup bar = nativeBar.get();
        return bar != null && TabBarBridge.selectedIndex(TabBarBridge.findTabRow(bar)) == 0;
    }

    private void locate(Activity a) {
        View cached = source.get(), click = clickTarget.get();
        if (cached != null && cached.isShown() && !excluded(cached) && click != null
                && click.isShown() && !excluded(click)) return;
        View root = a.getWindow().getDecorView();
        root.getLocationOnScreen(rootLoc);
        Candidate best = new Candidate();
        remaining = 600;
        find(root, 0, best, a.getResources().getDisplayMetrics().density);
        diagnostics = "扫描节点=" + (600 - Math.max(0, remaining)) + "；分数=" + best.score
                + "；头像类=" + (best.avatar == null ? "未命中" : best.avatar.getClass().getName())
                + "；点击类=" + (best.click == null ? "未命中" : best.click.getClass().getName());
        if (best.avatar != null && best.score >= 8) {
            source = new WeakReference<>(best.avatar);
            clickTarget = new WeakReference<>(best.click);
            FeedbackLog.event("AVATAR_BIND", diagnostics);
        }
    }

    private static final class Candidate { int score; View avatar, click; }

    private void find(View v, int depth, Candidate best, float density) {
        if (--remaining < 0 || depth > 24 || v.getVisibility() != View.VISIBLE
                || isOwned(v)) return;
        String name = v.getClass().getName().toLowerCase(Locale.ROOT);
        if (name.contains("qqsettingme") || name.contains("settingmeview")) return;
        int w = v.getWidth(), h = v.getHeight();
        if (w >= 18 * density && w <= 84 * density && h >= 18 * density && h <= 84 * density
                && w >= h * .7f && w <= h * 1.45f) {
            v.getLocationOnScreen(loc);
            float x = loc[0] - rootLoc[0], y = loc[1] - rootLoc[1];
            if (x >= 0 && x <= 56 * density && y >= 0 && y <= 112 * density) {
                String words = semantics(v);
                int score = 0;
                if (words.contains("头像") || words.contains("avatar") || words.contains("head")) score += 8;
                if (words.contains("账户") || words.contains("帐户") || words.contains("侧边栏")
                        || words.contains("侧栏") || words.contains("v9sideiconview")) score += 10;
                if (name.contains("vasavatar") || name.contains("face")) score += 8;
                if (v instanceof ImageView) score += 2;
                if (score > best.score) {
                    best.score = score;
                    best.avatar = avatarLeaf(v, 0);
                    best.click = clickableAncestor(v, density);
                }
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) find(g.getChildAt(i), depth + 1, best, density);
        }
    }

    private static View avatarLeaf(View v, int depth) {
        if (v instanceof ImageView || depth >= 3) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View c = g.getChildAt(i);
                if (c.getVisibility() != View.VISIBLE || c.getWidth() < v.getWidth() / 2) continue;
                View leaf = avatarLeaf(c, depth + 1);
                if (leaf instanceof ImageView) return leaf;
            }
        }
        return v;
    }

    private static String semantics(View v) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 3 && v != null; i++) {
            text.append(v.getClass().getName()).append(' ');
            if (v.getContentDescription() != null) text.append(v.getContentDescription());
            v = v.getParent() instanceof View ? (View) v.getParent() : null;
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }

    private static View clickableAncestor(View v, float density) {
        for (int i = 0; i < 5 && v != null && !excluded(v); i++) {
            if (v.getHeight() > 120 * density || v.getWidth() > 180 * density) return null;
            if (v.hasOnClickListeners() && v.isEnabled()) return v;
            v = v.getParent() instanceof View ? (View) v.getParent() : null;
        }
        return null;
    }

    private static boolean isOwned(View v) {
        return v.getTag() == QqSplitDock.OWNED || v.getTag() == LiquidGlassHostLayout.GLASS_TAG
                || v.getClass().getName().startsWith("io.github.liuran001.mmliquidglass.");
    }
    private static boolean excluded(View v) {
        for (int depth = 0; v != null && depth < 40; depth++) {
            if (isOwned(v)) return true;
            v = v.getParent() instanceof View ? (View) v.getParent() : null;
        }
        return false;
    }

    void openDrawer(View anchor) {
        long token = action.beginAction(SystemClock.uptimeMillis());
        if (token < 0) return;
        Activity a = activity.get();
        if (a == null || a.isDestroyed()) return;
        ViewGroup bar = nativeBar.get();
        View message = TabBarBridge.tabAt(bar == null ? null : TabBarBridge.findTabRow(bar), 0);
        if (message == null || excluded(message)) { fail(anchor); return; }
        // One native tab switch, and at most one native avatar click per gesture.
        try {
            if (!message.isSelected()) message.performClick();
        } catch (Throwable t) { fail(anchor); return; }
        anchor.postDelayed(() -> openReady(anchor, token, 0), 180);
    }

    private void openReady(View anchor, long token, int attempt) {
        Activity a = activity.get();
        if (!action.isCurrent(token) || !anchor.isAttachedToWindow() || a == null
                || a.isDestroyed() || !a.hasWindowFocus()) return;
        if (messageSelected()) locate(a);
        View target = clickTarget.get();
        if (messageSelected() && target != null && target.isShown() && target.isEnabled()
                && !excluded(target) && target.hasOnClickListeners()) {
            if (!action.claimAction(token)) return;
            try { target.performClick(); FeedbackLog.event("DRAWER_CLICK", "native avatar click dispatched once; opening not yet verified"); }
            catch (Throwable t) { fail(anchor); }
            return; // Never retry after executing a native action, even on throw.
        }
        if (attempt < 2) anchor.postDelayed(() -> openReady(anchor, token, attempt + 1), 250);
        else { action.claimAction(token); fail(anchor); }
    }

    private void fail(View anchor) {
        status = "未匹配此版本原生头像入口，请复制诊断日志";
        FeedbackLog.event("DRAWER_UNMATCHED", "use native header entry");
        Toast.makeText(anchor.getContext(), "暂未识别头像入口，请用消息页左上角原生头像", Toast.LENGTH_LONG).show();
    }

    static final class AvatarButton extends View {
        private final QqAvatarBridge bridge;
        private final Path clip = new Path();
        private final RectF dest = new RectF();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        AvatarButton(Activity a, QqAvatarBridge bridge) {
            super(a);
            this.bridge = bridge;
            setContentDescription("当前登录账号头像，打开 QQ 原生侧边栏");
            setFocusable(true);
            setOnClickListener(v -> bridge.openDrawer(this));
            setOnLongClickListener(v -> { QqSettingsEntry.show(a); return true; });
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = Math.max(0, Math.min(cx, cy) - QqSettingsEntry.dp(getContext(), GlassConfig.options.get(DockOptions.Key.AVATAR_INSET)));
            Bitmap image = bridge.bitmap;
            if (image != null && !image.isRecycled()) {
                int save = canvas.save();
                try {
                    clip.reset(); clip.addCircle(cx, cy, radius, Path.Direction.CW);
                    canvas.clipPath(clip);
                    float scale = radius * 2 / Math.min(image.getWidth(), image.getHeight());
                    float w = image.getWidth() * scale, h = image.getHeight() * scale;
                    dest.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
                    canvas.drawBitmap(image, null, dest, paint);
                } catch (RuntimeException ignored) {
                    bridge.bitmap = null;
                } finally { canvas.restoreToCount(save); }
            } else {
                paint.setColor(0xff929ba8);
                canvas.drawCircle(cx, cy - radius * .3f, radius * .3f, paint);
                canvas.drawRoundRect(cx - radius * .58f, cy + radius * .05f,
                        cx + radius * .58f, cy + radius * .6f, radius * .3f, radius * .3f, paint);
            }
        }
    }
}
