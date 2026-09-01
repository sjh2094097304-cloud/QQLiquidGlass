package com.qiutian.bianpaobubble.hook;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;
import android.widget.ImageView;

import com.qiutian.bianpaobubble.AppConfig;
import com.qiutian.bianpaobubble.SignatureGuard;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.AndroidAppHelper;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class BubbleXposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final String QQ_PACKAGE = "com.tencent.mobileqq";
    private static final Set<String> MENU_METHODS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final AtomicBoolean MODERN_MENU_SEEN = new AtomicBoolean(false);
    private static final AtomicBoolean MODERN_MENU_INSERTED = new AtomicBoolean(false);
    private static final Map<Activity, Long> SETTINGS_REBIND_AT =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile WeakReference<Activity> currentActivity = new WeakReference<>(null);
    private static volatile Context qqContext;
    private static volatile ClassLoader qqLoader;
    private static volatile boolean initialized;
    private static final BubbleRandomBag RANDOM_BAG = new BubbleRandomBag();
    private static final ThreadLocal<Integer> SEND_DEPTH = new ThreadLocal<>();
    private static final AtomicBoolean SETTINGS_ENTRY_LOGGED = new AtomicBoolean(false);
    private static volatile int menuIconRes;
    private static volatile String modulePath;

    static Activity currentActivity() {
        Activity activity = currentActivity.get();
        return activity != null && !activity.isFinishing() && !activity.isDestroyed() ? activity : null;
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        modulePath = startupParam == null ? null : startupParam.modulePath;
        ModuleIcon.setModuleApkPath(modulePath);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!QQ_PACKAGE.equals(lpparam.packageName)) return;
        // Isolated loaders can rename the primary process. Still skip QQ's known subprocesses.
        if (lpparam.processName != null && lpparam.processName.startsWith(QQ_PACKAGE + ":")) return;
        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook(100) {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    Context context = param.thisObject instanceof Context ? (Context) param.thisObject : (Context) param.args[0];
                    initialize(context, lpparam.classLoader);
                }
            });
        } catch (Throwable error) { HookLog.info("Application.attach 挂载失败：" + error.getClass().getSimpleName()); }
        // Rootless/late-injection frameworks may invoke handleLoadPackage after attach.
        try {
            Application running = AndroidAppHelper.currentApplication();
            if (running != null && QQ_PACKAGE.equals(running.getPackageName())) initialize(running, lpparam.classLoader);
        } catch (Throwable error) { HookLog.info("延迟注入检查：" + error.getClass().getSimpleName()); }
    }

    private static synchronized void initialize(Context context, ClassLoader fallbackLoader) {
        if (initialized || context == null) return;
        String version = qqVersion(context);
        if (AppConfig.compareVersion(version, AppConfig.MIN_QQ_VERSION) < 0) {
            HookLog.info("QQ " + version + " 低于最低支持版本 " + AppConfig.MIN_QQ_VERSION + "，已停止 Hook");
            return;
        }
        Context app = context.getApplicationContext();
        qqContext = app == null ? context : app;
        qqLoader = context.getClassLoader();
        if (qqLoader == null) qqLoader = fallbackLoader;
        try { qqLoader.loadClass("com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService$CppProxy"); }
        catch (Throwable ignored) { if (fallbackLoader != null) qqLoader = fallbackLoader; }
        try { SignatureGuard.enforceArchiveIfReadable(qqContext, modulePath); }
        catch (Throwable error) { HookLog.error(qqContext, "框架模块路径签名检测已跳过", error); }
        menuIconRes = context.getResources().getIdentifier("qui_tuning", "drawable", QQ_PACKAGE);
        if (menuIconRes <= 0) menuIconRes = android.R.drawable.sym_def_app_icon;
        initialized = true;
        HookStatus.started();
        installAll(qqLoader, qqContext);
        HostConfig.reportHook(qqContext, version);
        HookLog.info("3.7 已加载，QQ " + version);
    }

    private static void installAll(ClassLoader loader, Context context) {
        installActivityTracker(context);
        safeInstall(context, "设置点击接管", () -> installSettingsClickInterceptor(context));
        safeInstall(context, "发送气泡", () -> installSendHook(loader, context));
        safeInstall(context, "防撤回", () -> AntiRevokeHook.install(loader, context));
        safeInstall(context, "长按菜单", () -> installMessageMenu(loader, context));
        safeInstall(context, "商城识别", () -> installMallHook(loader, context));
        safeInstall(context, "设置入口", () -> installSettingsEntry(loader, context));
    }

    private static void safeInstall(Context context, String name, ThrowingRunnable action) {
        try {
            action.run();
            HookStatus.installed(name);
        } catch (Throwable e) {
            HookStatus.failed(name, e);
            HookLog.error(context, name + "初始化失败", e);
        }
    }

    private static void installActivityTracker(Context context) {
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook(-100) {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    currentActivity = new WeakReference<>(activity);
                    HostConfig.reportHook(context, qqVersion(context));
                    scheduleSettingsEntryRebind(activity);
                    AntiRevokeHook.onActivityResumed(activity);
                }
            });
            XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook(-100) {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (currentActivity.get() == param.thisObject) currentActivity = new WeakReference<>(null);
                }
            });
        } catch (Throwable e) {
            HookLog.error(context, "界面跟踪初始化失败", e);
        }
    }

    /**
     * FPA may publish the module launcher as a QQ settings row. Its original click listener starts
     * our exported Activity, which makes QQ show the "leave QQ" confirmation. Intercept the click
     * before the original listener runs and render the settings surface in the current QQ Activity.
     */
    private static void installSettingsClickInterceptor(Context context) {
        XposedHelpers.findAndHookMethod(View.class, "performClick", new XC_MethodHook(10000) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (InQqSettingsDialog.isShowing() || !(param.thisObject instanceof View)) return;
                    Activity tracked = currentActivity.get();
                    if (!isLikelySettingsActivity(tracked)) return;
                    View clicked = (View) param.thisObject;
                    if (!containsExactText(clicked, "气泡", 0, 5)) return;
                    Activity activity = activityFrom(clicked.getContext());
                    if (activity == null) activity = currentActivity.get();
                    if (!isQqSettingsPage(activity)) return;
                    applySettingsEntryAvatar(clicked, findExactText(clicked, "气泡", 0, 6));
                    InQqSettingsDialog.showEntry(activity);
                    param.setResult(true);
                } catch (Throwable e) {
                    HookLog.error(context, "设置点击接管失败", e);
                }
            }
        });
    }

    private static void scheduleSettingsEntryRebind(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.getWindow() == null) return;
        View decor = activity.getWindow().getDecorView();
        if (decor == null) return;
        if (!isLikelySettingsActivity(activity)) return;
        long now = System.currentTimeMillis();
        synchronized (SETTINGS_REBIND_AT) {
            Long previous = SETTINGS_REBIND_AT.get(activity);
            if (previous != null && now - previous < 2_500L) return;
            SETTINGS_REBIND_AT.put(activity, now);
        }
        int[] delays = {60, 650, 1300};
        for (int delay : delays) decor.postDelayed(() -> rebindSettingsEntry(activity), delay);
    }

    private static void rebindSettingsEntry(Activity activity) {
        try {
            if (!isQqSettingsPage(activity) || activity.getWindow() == null) return;
            View target = findExactText(activity.getWindow().getDecorView(), "气泡", 0, 18);
            if (target == null) return;
            View row = clickableAncestor(target, 6);
            if (row == null) row = target;
            applySettingsEntryAvatar(row, target);
            row.setClickable(true);
            row.setOnClickListener(v -> InQqSettingsDialog.showEntry(activity));
            if (SETTINGS_ENTRY_LOGGED.compareAndSet(false, true)) {
                HookLog.info("已接管 QQ 设置中的气泡入口");
            }
        } catch (Throwable e) {
            HookLog.error(qqContext, "设置入口重绑定失败", e);
        }
    }

    private static boolean isQqSettingsPage(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.getWindow() == null) return false;
        View root = activity.getWindow().getDecorView();
        if (root == null) return false;
        if (!isLikelySettingsActivity(activity)) return false;
        int markers = 0;
        if (containsExactText(root, "账户与安全", 0, 16)) markers++;
        if (containsExactText(root, "消息通知", 0, 16)) markers++;
        if (containsExactText(root, "模式选择", 0, 16)) markers++;
        if (containsExactText(root, "个性装扮与特权外显", 0, 16)) markers++;
        return markers >= 2;
    }

    private static boolean isLikelySettingsActivity(Activity activity) {
        if (activity == null || activity.isFinishing()) return false;
        String name = activity.getClass().getName();
        return name.contains("Setting") || name.contains("QPublicFragmentActivity")
                || name.contains("QQFragmentActivity");
    }

    private static View clickableAncestor(View target, int maxDepth) {
        View current = target;
        for (int i = 0; i <= maxDepth && current != null; i++) {
            if (current.isClickable() || current.hasOnClickListeners()) return current;
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        ViewParent parent = target.getParent();
        return parent instanceof View ? (View) parent : target;
    }

    private static View findExactText(View view, String expected, int depth, int maxDepth) {
        if (view == null || depth > maxDepth) return null;
        if (hasExactText(view, expected)) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findExactText(group.getChildAt(i), expected, depth + 1, maxDepth);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean containsExactText(View view, String expected, int depth, int maxDepth) {
        return findExactText(view, expected, depth, maxDepth) != null;
    }

    private static boolean hasExactText(View view, String expected) {
        if (view instanceof TextView && expected.contentEquals(((TextView) view).getText())) return true;
        CharSequence description = view.getContentDescription();
        return description != null && expected.contentEquals(description);
    }

    /** Replaces FPA/QQ's placeholder information icon after the settings row is rendered. */
    private static void applySettingsEntryAvatar(View row, View label) {
        if (row == null) return;
        Drawable avatar = ModuleIcon.load(row.getContext());
        if (avatar == null) return;
        ImageView icon = findEntryIcon(row, label);
        if (icon == null) {
            ViewParent parent = row.getParent();
            if (parent instanceof View) icon = findEntryIcon((View) parent, label);
        }
        if (icon == null) return;
        icon.setImageDrawable(avatar);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setContentDescription("百变气泡应用头像");
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            android.graphics.drawable.GradientDrawable outline = new android.graphics.drawable.GradientDrawable();
            outline.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            outline.setColor(android.graphics.Color.TRANSPARENT);
            icon.setBackground(outline);
            icon.setClipToOutline(true);
        }
        icon.invalidate();
    }

    private static ImageView findEntryIcon(View root, View label) {
        List<ImageView> images = new ArrayList<>();
        collectImages(root, images, 0, 8);
        if (images.isEmpty()) return null;
        if (label == null) return images.get(0);
        int[] labelPosition = new int[2];
        label.getLocationOnScreen(labelPosition);
        ImageView best = null;
        int bestX = Integer.MIN_VALUE;
        for (ImageView image : images) {
            int[] position = new int[2];
            image.getLocationOnScreen(position);
            int centerX = position[0] + image.getWidth() / 2;
            if (centerX < labelPosition[0] && centerX > bestX) {
                best = image;
                bestX = centerX;
            }
        }
        return best != null ? best : images.get(0);
    }

    private static void collectImages(View view, List<ImageView> result, int depth, int maxDepth) {
        if (view == null || depth > maxDepth) return;
        if (view instanceof ImageView && view.getVisibility() == View.VISIBLE) {
            result.add((ImageView) view);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectImages(group.getChildAt(i), result, depth + 1, maxDepth);
            }
        }
    }

    private static Activity activityFrom(Context context) {
        Context current = context;
        for (int i = 0; i < 8 && current != null; i++) {
            if (current instanceof Activity) return (Activity) current;
            current = current instanceof ContextWrapper ? ((ContextWrapper) current).getBaseContext() : null;
        }
        return null;
    }

    private static void installSendHook(ClassLoader loader, Context context) throws Exception {
        Class<?> proxy = loader.loadClass("com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService$CppProxy");
        int installed = 0;
        for (Method method : proxy.getDeclaredMethods()) {
            if (!"sendMsg".equals(method.getName())) continue;
            boolean hasAttributes = false;
            for (Class<?> type : method.getParameterTypes()) if (Map.class.isAssignableFrom(type)) hasAttributes = true;
            if (!hasAttributes) continue;
            method.setAccessible(true);
            XposedBridge.hookMethod(method, new XC_MethodHook(80) {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    Integer previous = SEND_DEPTH.get();
                    int depth = previous == null ? 0 : previous;
                    SEND_DEPTH.set(depth + 1);
                    param.setObjectExtra("bbSendEntered", Boolean.TRUE);
                    if (depth > 0) return; // Delegating overloads must consume only one random ID.
                    try {
                        HookStatus.seen("发送气泡");
                        Bundle config = HostConfig.getForSend(context);
                        if (!config.getBoolean("masterEnabled", false)) return;
                        Object bubble = Reflector.bubbleInfoFromArgs(param.args);
                        if (bubble == null) return;
                        int targetId = config.getBoolean("lockedEnabled", false) ? config.getInt("lockedId", 0) : 0;
                        if (!AppConfig.validId(targetId) && config.getBoolean("randomEnabled", false)) {
                            targetId = RANDOM_BAG.next(config.getIntArray("pool"), config.getInt("lastAppliedId", 0));
                        }
                        if (!AppConfig.validId(targetId)) return;
                        boolean changed = Reflector.setNumber(bubble, "subBubbleId", targetId);
                        changed = Reflector.setNumber(bubble, "bubbleId", targetId) || changed;
                        if (!changed) throw new NoSuchFieldException("bubbleInfo.subBubbleId/bubbleId");
                        RANDOM_BAG.noteApplied(targetId);
                        HostConfig.noteApplied(context, targetId);
                    } catch (Throwable error) { HookLog.error(context, "发送气泡处理失败", error); }
                }
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (!Boolean.TRUE.equals(param.getObjectExtra("bbSendEntered"))) return;
                    Integer depth = SEND_DEPTH.get();
                    if (depth == null || depth <= 1) SEND_DEPTH.remove(); else SEND_DEPTH.set(depth - 1);
                }
            });
            installed++;
        }
        if (installed == 0) throw new NoSuchMethodException("IKernelMsgService.CppProxy.sendMsg(..., Map, ...)");
    }

    private static void installMessageMenu(ClassLoader loader, Context context) throws Exception {
        int modernHooks = 0;
        boolean legacyHooked = false;
        Throwable modernError = null;
        Throwable legacyError = null;
        try {
            modernHooks = installExpandableMessageMenu(loader, context);
        } catch (Throwable error) {
            modernError = error;
            HookLog.error(context, "新版长按菜单入口初始化失败", error);
        }
        try {
            installLegacyMessageMenu(loader, context);
            legacyHooked = true;
        } catch (Throwable error) {
            legacyError = error;
            if (modernHooks == 0) HookLog.error(context, "旧版长按菜单入口初始化失败", error);
        }
        HostConfig.appendLog(context, "长按菜单入口：新版=" + modernHooks
                + "，旧版兜底=" + (legacyHooked ? "已挂载" : "未挂载")
                + "，QQ=" + qqVersion(context));
        if (modernHooks == 0 && !legacyHooked) {
            Throwable cause = modernError != null ? modernError : legacyError;
            throw new IllegalStateException("新版和旧版长按菜单入口均不可用", cause);
        }
    }

    /**
     * QQ 9.2.30+ builds the final message menu in QQCustomMenuExpandableLayout.setMenu.
     * Hooking this final surface follows QFun's current public implementation and avoids relying
     * on the old BaseContentComponent menu-list methods, which are no longer called on 9.2.75.
     */
    private static int installExpandableMessageMenu(ClassLoader loader, Context context) throws Exception {
        Class<?> layoutClass = loader.loadClass("com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout");
        Class<?> msgClass = loader.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem");
        Set<?> hooks = XposedBridge.hookAllMethods(layoutClass, "setMenu", new XC_MethodHook(10000) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (param.args == null || param.args.length == 0 || param.args[0] == null) return;
                    MenuListRef menu = findMenuList(param.args[0], msgClass);
                    if (menu == null || menu.items.isEmpty()) {
                        HookLog.error(context, "新版长按菜单没有找到菜单列表",
                                new IllegalStateException(param.args[0].getClass().getName()));
                        return;
                    }
                    Object msg = findMenuMessage(menu.items, msgClass);
                    if (msg == null) {
                        HookLog.error(context, "新版长按菜单没有找到消息对象",
                                new IllegalStateException(menu.items.get(0).getClass().getName()));
                        return;
                    }
                    HookStatus.seen("长按菜单");
                    if (MODERN_MENU_SEEN.compareAndSet(false, true)) {
                        HostConfig.appendLog(context, "新版长按菜单已触发：menu="
                                + param.args[0].getClass().getName() + "，items=" + menu.items.size());
                    }
                    if (!BubbleDialog.isValidBubbleId(Reflector.bubbleId(msg))) return;
                    if (NtMenuFactory.containsOwned(menu.items)) return;
                    Context itemContext = currentActivity.get();
                    if (itemContext == null) itemContext = context;
                    Object item = NtMenuFactory.create(loader, msg, menu.items, itemContext,
                            menuIconRes, () -> openBubbleFromMenu(context, msg));
                    NtMenuFactory.installCustomView(param.thisObject.getClass(), item.getClass(), context);
                    try {
                        menu.items.add(0, item);
                    } catch (Throwable immutable) {
                        List<Object> mutable = new ArrayList<>(menu.items);
                        mutable.add(0, item);
                        menu.field.set(menu.owner, mutable);
                        menu.items = mutable;
                    }
                    if (MODERN_MENU_INSERTED.compareAndSet(false, true)) {
                        HostConfig.appendLog(context, "新版长按菜单已成功加入“秋天”：item="
                                + item.getClass().getName());
                    }
                } catch (Throwable error) {
                    HookLog.error(context, "新版长按菜单注入失败", error);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!(param.thisObject instanceof View)) return;
                View menuView = (View) param.thisObject;
                menuView.post(() -> {
                    try {
                        NtMenuFactory.applyAvatarToRenderedMenu(menuView, context);
                    } catch (Throwable avatarError) {
                        HookLog.error(context, "长按菜单头像渲染失败", avatarError);
                    }
                });
            }
        });
        if (hooks == null || hooks.isEmpty()) throw new NoSuchMethodException(layoutClass.getName() + ".setMenu");
        return hooks.size();
    }

    private static void openBubbleFromMenu(Context context, Object msg) {
        Activity activity = currentActivity.get();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            int id = Reflector.bubbleId(msg);
            if (!BubbleDialog.isValidBubbleId(id)) {
                HostConfig.appendLog(context, "消息没有有效气泡，已阻止 ID 弹窗 | raw=" + id);
                return;
            }
            BubbleDialog.showMessage(activity, msg);
        } else {
            HookLog.error(context, "长按菜单没有可用界面",
                    new IllegalStateException(msg == null ? "msg=null" : msg.getClass().getName()));
        }
    }

    @SuppressWarnings("unchecked")
    private static MenuListRef findMenuList(Object menuObject, Class<?> msgClass) {
        Class<?> current = menuObject.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !List.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object raw = field.get(menuObject);
                    if (!(raw instanceof List)) continue;
                    List<Object> items = (List<Object>) raw;
                    if (items.isEmpty() || findMenuMessage(items, msgClass) != null) {
                        return new MenuListRef(menuObject, field, items);
                    }
                } catch (Throwable ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Object findMenuMessage(List<?> items, Class<?> msgClass) {
        if (items == null) return null;
        int limit = Math.min(items.size(), 8);
        for (int i = 0; i < limit; i++) {
            Object item = items.get(i);
            if (item == null) continue;
            try {
                Field field = Reflector.firstInstanceField(item.getClass(), msgClass);
                if (field != null) {
                    Object value = field.get(item);
                    if (value != null) return value;
                }
            } catch (Throwable ignored) {
            }
            Class<?> current = item.getClass();
            while (current != null && current != Object.class) {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getParameterTypes().length != 0 || !msgClass.isAssignableFrom(method.getReturnType())) continue;
                    try {
                        method.setAccessible(true);
                        Object value = method.invoke(item);
                        if (value != null) return value;
                    } catch (Throwable ignored) {
                    }
                }
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static final class MenuListRef {
        final Object owner;
        final Field field;
        List<Object> items;

        MenuListRef(Object owner, Field field, List<Object> items) {
            this.owner = owner;
            this.field = field;
            this.items = items;
        }
    }

    private static void installLegacyMessageMenu(ClassLoader loader, Context context) throws Exception {
        Class<?> msgClass = loader.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem");
        Class<?> base = loader.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.BaseContentComponent");
        Method getMsg = null;
        List<String> menuMethodNames = new ArrayList<>();
        for (Method method : base.getDeclaredMethods()) {
            if (method.getParameterTypes().length != 0) continue;
            if (method.getReturnType() == msgClass) getMsg = method;
            if (Modifier.isAbstract(method.getModifiers()) && List.class.isAssignableFrom(method.getReturnType())
                    && !menuMethodNames.contains(method.getName())) menuMethodNames.add(method.getName());
        }
        if (getMsg == null || menuMethodNames.isEmpty()) {
            throw new NoSuchMethodException("BaseContentComponent message/menu methods");
        }
        getMsg.setAccessible(true);
        final Method getMsgFinal = getMsg;

        int directHooks = 0;
        for (String name : new String[]{
                "com.tencent.mobileqq.aio.msglist.holder.component.text.AIOTextContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.pic.AIOPicContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.reply.AIOReplyComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.ptt.AIOPttContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.video.AIOVideoContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.file.AIOFileContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.file.AIOOnlineFileContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.mix.AIOMixContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.multipci.AIOMultiPicContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.longmsg.AIOLongMsgContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.markdown.AIORichContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.markdown.AIOMarkdownContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.marketface.AIOMarketFaceComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.template.AIOTemplateMsgComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.ark.AIOArkContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.anisticker.AIOAniStickerContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.facebubble.AIOFaceBubbleContentComponent",
                "com.tencent.mobileqq.aio.qwallet.AIOQWalletComponent"}) {
            try {
                Class<?> component = loader.loadClass(name);
                for (String menuMethodName : menuMethodNames) {
                    try {
                        hookMenuComponent(component, menuMethodName, getMsgFinal, loader, context);
                        directHooks++;
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        HookLog.info("长按菜单已直接接入 " + directHooks + " 个消息组件");
        HostConfig.appendLog(context, "长按菜单初始化：候选方法=" + menuMethodNames.size()
                + "，直接接入=" + directHooks);
        if (directHooks == 0) {
            HookLog.error(context, "长按菜单没有直接接入任何消息组件",
                    new IllegalStateException("QQ=" + qqVersion(context)));
        }

        XposedBridge.hookAllConstructors(base, new XC_MethodHook(50) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                for (String menuMethodName : menuMethodNames) {
                    try {
                        hookMenuComponent(param.thisObject.getClass(), menuMethodName,
                                getMsgFinal, loader, context);
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    private static void hookMenuComponent(Class<?> component, String menuMethodName, Method getMsg,
                                          ClassLoader loader, Context context) throws Exception {
        Method menu = findNoArgMethod(component, menuMethodName);
        if (menu == null || Modifier.isAbstract(menu.getModifiers()) || !List.class.isAssignableFrom(menu.getReturnType())) {
            throw new NoSuchMethodException(component.getName() + "." + menuMethodName);
        }
        String hookKey = menu.toGenericString();
        if (!MENU_METHODS.add(hookKey)) return;
        menu.setAccessible(true);
        try {
        XposedBridge.hookMethod(menu, new XC_MethodHook(48) {
            @Override
            @SuppressWarnings("unchecked")
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    Object result = param.getResult();
                    if (!(result instanceof List)) {
                        HookLog.error(context, "长按菜单返回值不可用",
                                new IllegalStateException(component.getName() + '#' + menuMethodName));
                        return;
                    }
                    HookStatus.seen("长按菜单");
                    Object msg = getMsg.invoke(param.thisObject);
                    if (msg == null || !BubbleDialog.isValidBubbleId(Reflector.bubbleId(msg))) return;
                    List<Object> items = (List<Object>) result;
                    if (NtMenuFactory.containsOwned(items)) return;
                    Context itemContext = currentActivity.get();
                    if (itemContext == null) itemContext = context;
                    Object item = NtMenuFactory.create(loader, msg, items, itemContext,
                            menuIconRes, () -> openBubbleFromMenu(context, msg));
                    try {
                        Class<?> layout = loader.loadClass(
                                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout");
                        NtMenuFactory.installCustomView(layout, item.getClass(), context);
                    } catch (Throwable iconHookError) {
                        HookLog.error(context, "长按菜单头像视图初始化失败", iconHookError);
                    }
                    try {
                        items.add(0, item);
                    } catch (Throwable immutable) {
                        List<Object> mutable = new ArrayList<>(items);
                        mutable.add(0, item);
                        param.setResult(mutable);
                    }
                } catch (Throwable e) {
                    HookLog.error(context, "长按气泡菜单构建失败", e);
                }
            }
        });
        } catch (RuntimeException error) {
            MENU_METHODS.remove(hookKey);
            throw error;
        }
    }

    private static void installMallHook(ClassLoader loader, Context context) throws Exception {
        Class<?> host = loader.loadClass("com.tencent.mobileqq.activity.QPublicFragmentActivity");
        Method onCreate = host.getDeclaredMethod("doOnCreate", Bundle.class);
        onCreate.setAccessible(true);
        XposedBridge.hookMethod(onCreate, new XC_MethodHook(45) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
                if (decor == null) return;
                AtomicBoolean shown = new AtomicBoolean(false);
                int[] delays = {320, 850, 1600};
                for (int delay : delays) decor.postDelayed(() -> {
                    if (shown.get() || activity.isFinishing() || activity.isDestroyed()) return;
                    try {
                        HookStatus.seen("商城识别");
                        int id = mallBubbleId(activity);
                        if (id > 0 && shown.compareAndSet(false, true)) {
                            HostConfig.appendLog(context, "商城气泡识别成功 | id=" + id);
                            BubbleDialog.showMall(activity, id);
                        }
                    } catch (Throwable e) {
                        HookLog.error(context, "商城气泡识别失败", e);
                    }
                }, delay);
            }
        });
    }

    private static void installSettingsEntry(ClassLoader loader, Context context) throws Exception {
        int hookCount = 0;
        for (String name : new String[]{
                "com.tencent.mobileqq.setting.main.MainSettingConfigProvider",
                "com.tencent.mobileqq.setting.main.NewSettingConfigProvider",
                "com.tencent.mobileqq.setting.main.b"}) {
            Class<?> provider;
            try {
                provider = loader.loadClass(name);
            } catch (Throwable ignored) {
                continue;
            }
            for (Method method : provider.getDeclaredMethods()) {
                Class<?>[] args = method.getParameterTypes();
                if (!List.class.isAssignableFrom(method.getReturnType()) || args.length != 1 || !Context.class.isAssignableFrom(args[0])) continue;
                method.setAccessible(true);
                XposedBridge.hookMethod(method, new XC_MethodHook(50) {
                    @Override
                    @SuppressWarnings("unchecked")
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!(param.getResult() instanceof List) || !(param.args[0] instanceof Context)) return;
                        List<Object> groups = (List<Object>) param.getResult();
                        try {
                            synchronized (groups) {
                                if (!containsSettingsItem(groups, "气泡")) {
                                    injectSettingsItem(groups, (Context) param.args[0], loader);
                                }
                            }
                        } catch (Throwable e) {
                            HookLog.error(context, "设置入口生成失败", e);
                        }
                    }
                });
                hookCount++;
            }
        }
        if (hookCount == 0) throw new NoSuchMethodException("QQ MainSettingConfigProvider List(Context)");
    }

    private static void injectSettingsItem(List<Object> groups, Context context, ClassLoader loader) throws Exception {
        ItemTemplate template = findItemTemplate(groups);
        if (template == null) throw new IllegalStateException("未找到设置 SimpleItemProcessor");
        int viewId = 0x425550;
        int icon = menuIconRes;
        if (icon <= 0) icon = context.getResources().getIdentifier("qui_tuning", "drawable", QQ_PACKAGE);
        if (icon == 0) icon = android.R.drawable.ic_menu_manage;
        Object[] args = new Object[template.constructor.getParameterTypes().length];
        args[0] = context;
        args[1] = viewId;
        args[2] = "气泡";
        args[3] = icon;
        if (args.length == 5) args[4] = null;
        Object item = template.constructor.newInstance(args);

        Class<?> function0 = template.clickSetter.getParameterTypes()[0];
        Class<?> unitClass = loader.loadClass("kotlin.Unit");
        Object unit = unitClass.getField("INSTANCE").get(null);
        Object click = Proxy.newProxyInstance(loader, new Class<?>[]{function0}, (proxy, method, invokeArgs) -> {
            if ("invoke".equals(method.getName())) {
                openModuleSettings(context);
                return unit;
            }
            if ("toString".equals(method.getName())) return "百变气泡设置入口";
            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
            if ("equals".equals(method.getName())) return proxy == (invokeArgs == null ? null : invokeArgs[0]);
            return null;
        });
        template.clickSetter.invoke(item, click);

        List<Object> one = new ArrayList<>();
        one.add(item);
        Object group = newSettingsGroup(template.groupClass, one, loader);
        groups.add(Math.min(2, groups.size()), group);
    }

    @SuppressWarnings("unchecked")
    private static ItemTemplate findItemTemplate(List<Object> groups) {
        for (Object group : groups) {
            if (group == null) continue;
            for (Field field : allFields(group.getClass())) {
                if (!List.class.isAssignableFrom(field.getType()) || Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(group);
                    if (!(value instanceof List)) continue;
                    for (Object existing : (List<Object>) value) {
                        if (existing == null) continue;
                        Constructor<?> constructor = simpleItemConstructor(existing.getClass());
                        Method setter = function0Setter(existing.getClass());
                        if (constructor != null && setter != null) return new ItemTemplate(group.getClass(), constructor, setter);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static boolean containsSettingsItem(List<Object> groups, String title) {
        for (Object group : groups) {
            if (group == null) continue;
            for (Field field : allFields(group.getClass())) {
                if (!List.class.isAssignableFrom(field.getType()) || Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(group);
                    if (!(value instanceof List)) continue;
                    for (Object item : (List<?>) value) {
                        if (item == null) continue;
                        for (Field itemField : allFields(item.getClass())) {
                            if (Modifier.isStatic(itemField.getModifiers())) continue;
                            if (!CharSequence.class.isAssignableFrom(itemField.getType())) continue;
                            itemField.setAccessible(true);
                            Object label = itemField.get(item);
                            if (title.contentEquals(label instanceof CharSequence ? (CharSequence) label : "")) return true;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }

    private static Constructor<?> simpleItemConstructor(Class<?> type) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            Class<?>[] p = constructor.getParameterTypes();
            if ((p.length == 4 || p.length == 5) && Context.class.isAssignableFrom(p[0])
                    && p[1] == int.class && CharSequence.class.isAssignableFrom(p[2]) && p[3] == int.class) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        return null;
    }

    private static Method function0Setter(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if (method.getReturnType() == void.class && p.length == 1 && "kotlin.jvm.functions.Function0".equals(p[0].getName())) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static Object newSettingsGroup(Class<?> groupClass, List<Object> items, ClassLoader loader) throws Exception {
        for (Constructor<?> constructor : groupClass.getDeclaredConstructors()) {
            Class<?>[] p = constructor.getParameterTypes();
            if (p.length >= 3 && List.class.isAssignableFrom(p[0]) && CharSequence.class.isAssignableFrom(p[1])
                    && CharSequence.class.isAssignableFrom(p[2])) {
                constructor.setAccessible(true);
                Object[] args = new Object[p.length];
                args[0] = items;
                args[1] = "";
                args[2] = "";
                for (int i = 3; i < p.length; i++) {
                    if (p[i] == int.class) args[i] = 6;
                    else if (p[i] == boolean.class) args[i] = false;
                    else args[i] = null;
                }
                return constructor.newInstance(args);
            }
        }
        throw new NoSuchMethodException(groupClass.getName() + " group constructor");
    }

    private static void openModuleSettings(Context context) {
        try {
            Activity activity = context instanceof Activity ? (Activity) context : currentActivity.get();
            if (activity == null) throw new IllegalStateException("当前 QQ Activity 不可用");
            InQqSettingsDialog.showEntry(activity);
        } catch (Throwable e) {
            HookLog.error(qqContext, "打开气泡设置失败", e);
        }
    }

    private static int mallBubbleId(Activity activity) {
        MallIdParser.Selection selection = new MallIdParser.Selection();
        Intent intent = activity.getIntent();
        if (intent != null) {
            collectMallBundle(intent.getExtras(), selection, 0);
            MallIdParser.collect(intent.getDataString(), selection);
        }
        Object fragment = Reflector.field(activity, "mFrag");
        Object arguments = Reflector.invokeNoArgs(fragment, "getArguments");
        if (arguments instanceof Bundle) collectMallBundle((Bundle) arguments, selection, 0);
        Object description = Reflector.invokeNoArgs(fragment, "getBusinessDescription");
        if (description instanceof CharSequence) MallIdParser.collect(description.toString(), selection);
        return selection.value();
    }

    private static void collectMallBundle(Bundle bundle, MallIdParser.Selection selection, int depth) {
        if (bundle == null || depth > 3) return;
        bundle.setClassLoader(qqLoader);
        int scanned = 0;
        for (String key : bundle.keySet()) {
            if (++scanned > 64) break;
            Object value;
            try { value = bundle.get(key); } catch (Throwable ignored) { continue; }
            if (value instanceof Bundle) collectMallBundle((Bundle) value, selection, depth + 1);
            else if (value instanceof CharSequence || value instanceof Uri) {
                if (MallIdParser.isItemKey(key)) {
                    MallIdParser.collectValue(key, value.toString(), selection);
                } else MallIdParser.collect(value.toString(), selection);
            }
        }
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name);
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> result = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Collections.addAll(result, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private static String qqVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(QQ_PACKAGE, 0);
            return info.versionName == null ? "0" : info.versionName;
        } catch (Throwable ignored) {
            return "0";
        }
    }

    private interface ThrowingRunnable { void run() throws Throwable; }

    private static final class ItemTemplate {
        final Class<?> groupClass;
        final Constructor<?> constructor;
        final Method clickSetter;

        ItemTemplate(Class<?> groupClass, Constructor<?> constructor, Method clickSetter) {
            this.groupClass = groupClass;
            this.constructor = constructor;
            this.clickSetter = clickSetter;
        }
    }
}
