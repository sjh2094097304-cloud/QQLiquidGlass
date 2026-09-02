package io.github.liuran001.mmliquidglass;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.util.HashSet;

/** Adds an actual row inside QQ's settings content, not a floating overlay. */
final class QqSettingsEntry {
    private static final Object ENTRY = new Object();
    private static final Object PENDING = new Object();
    private static final int PENDING_KEY = 0x7f5a0010;
    private static final String[] FRAGMENTS = {
        "com.tencent.mobileqq.setting.main.MainSettingFragment",
        "com.tencent.mobileqq.activity.QQSettingSettingFragment",
        "com.tencent.mobileqq.fragment.QQSettingSettingFragment"
    };
    private static final HashSet<Method> HOOKS = new HashSet<>();
    static volatile String status = "等待打开 QQ 设置";

    static void install(ClassLoader loader) {
        NativeSettingsBridge.install(loader);
        for (String name : FRAGMENTS) {
            try {
                Class<?> type = loader.loadClass(name);
                for (Class<?> base = type; base != null && base != Object.class;
                        base = base.getSuperclass()) {
                    for (Method method : base.getDeclaredMethods()) {
                        String n = method.getName();
                        if (!(n.equals("onResume") || n.equals("onViewCreated")
                                || n.equals("doOnCreateView") || n.equals("onCreateView"))) continue;
                        if (!HOOKS.add(method)) continue;
                        LiquidGlassModule.hookAfter(method, chain -> {
                            Object fragment = chain.getThisObject();
                            if (fragment == null || !isSettingsFragment(fragment.getClass().getName())) return;
                            Object activity = invoke(fragment, "getActivity");
                            Object root = invoke(fragment, "getView");
                            if (activity instanceof Activity) {
                                Activity a = (Activity) activity;
                                schedule(a, root instanceof View ? (View) root
                                        : a.getWindow().getDecorView());
                            }
                        });
                    }
                }
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                LiquidGlassModule.logErr("QQ settings lifecycle hook", t);
            }
        }
    }

    private static boolean isSettingsFragment(String name) {
        for (String candidate : FRAGMENTS) if (candidate.equals(name)) return true;
        return false;
    }

    static void onActivityResumed(Activity activity) {
        String name = activity.getClass().getName();
        String fragment = activity.getIntent() == null ? null
                : activity.getIntent().getStringExtra("public_fragment_class");
        if (name.equals("com.tencent.mobileqq.activity.QQSettingSettingActivity")
                || isSettingsFragment(fragment)) {
            schedule(activity, activity.getWindow().getDecorView());
        }
    }

    private static Object invoke(Object object, String name) {
        try { return object.getClass().getMethod(name).invoke(object); }
        catch (Throwable ignored) { return null; }
    }

    private static void schedule(Activity activity, View root) {
        if (root.getTag(PENDING_KEY) == PENDING) return;
        root.setTag(PENDING_KEY, PENDING);
        root.post(() -> attempt(activity, root, 0));
    }

    private static void attempt(Activity activity, View root, int attempt) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            root.setTag(PENDING_KEY, null);
            return;
        }
        try {
            if (NativeSettingsBridge.hasEntry(activity)) { root.setTag(PENDING_KEY, null); return; }
            if (root.findViewWithTag(ENTRY) != null || insert(activity, root)) {
                root.setTag(PENDING_KEY, null);
                status = "QQ 原生设置入口已插入";
                return;
            }
        } catch (Throwable t) {
            LiquidGlassModule.logErr("QQ settings row insertion", t);
        }
        if (attempt < 16) {
            root.postDelayed(() -> attempt(activity, root, attempt + 1), 200);
        } else {
            root.setTag(PENDING_KEY, null);
            status = "未识别此版本 QQ 的设置列表";
            LiquidGlassModule.log(android.util.Log.WARN, status);
        }
    }

    private static View findAnchor(View root,int depth) {
        if(depth>22 || root.getVisibility()!=View.VISIBLE || root.getTag()==ENTRY) return null;
        if(root instanceof TextView) {
            String value=((TextView)root).getText().toString();
            if(value.equals("账号与安全") || value.equals("帐号与安全")) return root;
        }
        if(root instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)root;
            for(int i=0;i<g.getChildCount();i++) {
                View result=findAnchor(g.getChildAt(i),depth+1);
                if(result!=null) return result;
            }
        }
        return null;
    }

    private static boolean insert(Activity activity,View root) {
        // Legacy scroll layouts only. RecyclerView is exclusively integrated
        // through NativeSettingsBridge, never wrapped or given unmanaged rows.
        View anchor=findAnchor(root,0);
        if(anchor==null) return false;
        View scan=anchor;
        for(int i=0;i<24 && scan!=null;i++) {
            String n=scan.getClass().getName();
            if(n.contains("RecyclerView") || scan instanceof android.widget.ListView) return false;
            scan=scan.getParent() instanceof View?(View)scan.getParent():null;
        }
        View item=anchor;
        for(int i=0;i<8 && item.getParent() instanceof ViewGroup;i++) {
            ViewGroup parent=(ViewGroup)item.getParent();
            if(parent instanceof LinearLayout && ((LinearLayout)parent).getOrientation()==LinearLayout.VERTICAL
                    && parent.getChildCount()>1 && item.getHeight()>=dp(activity,40)
                    && item.getHeight()<=dp(activity,120)) {
                View row=createRow(activity);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
                p.topMargin=dp(activity,12);
                parent.addView(row,parent.indexOfChild(item)+1,p);
                FeedbackLog.event("SETTINGS_LEGACY_READY","anchored after account/security row");
                return true;
            }
            item=parent;
        }
        return false;
    }

    private static View createRow(Activity activity) {
        // Prefer QQ's own form component when its stable public setters exist.
        for (String name : new String[]{"com.tencent.mobileqq.widget.FormSimpleItem",
                "com.tencent.mobileqq.widget.FormCommonSingleLineItem"}) {
            try {
                Class<?> type = activity.getClassLoader().loadClass(name);
                View row = (View) type.getConstructor(Context.class).newInstance(activity);
                type.getMethod("setLeftText", CharSequence.class).invoke(row, "液态玻璃底栏");
                try { type.getMethod("setRightText", CharSequence.class).invoke(row, "设置"); }
                catch (ReflectiveOperationException ignored) { }
                row.setTag(ENTRY);
                row.setMinimumHeight(dp(activity, 56));
                row.setOnClickListener(v -> show(activity));
                return row;
            } catch (Throwable ignored) { }
        }
        LinearLayout row = new LinearLayout(activity);
        row.setTag(ENTRY);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity, 20), dp(activity, 16), dp(activity, 16), dp(activity, 16));
        row.setMinimumHeight(dp(activity, 56));
        TextView title = new TextView(activity);
        title.setText("液态玻璃底栏");
        title.setTextSize(17);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = new TextView(activity);
        arrow.setText("›");
        arrow.setTextSize(24);
        row.addView(arrow, new LinearLayout.LayoutParams(-2, -2));
        row.setContentDescription("液态玻璃底栏，设置");
        View decor = activity.getWindow().getDecorView();
        Boolean dark = decor instanceof ViewGroup ? LiquidGlassHostLayout.detectDarkFromText((ViewGroup) decor) : null;
        if (dark != null) {
            int color = dark ? 0xffeeeeee : 0xff222222;
            title.setTextColor(color);
            arrow.setTextColor(color);
        }
        android.util.TypedValue value = new android.util.TypedValue();
        if (activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true))
            row.setBackgroundResource(value.resourceId);
        row.setOnClickListener(v -> show(activity));
        return row;
    }

    static void show(Activity activity) {
        if(activity==null || activity.isFinishing() || activity.isDestroyed()) return;
        QqSettingsPanel.show(activity);
    }

    static int dp(Context ctx, float value) {
        return Math.round(ctx.getResources().getDisplayMetrics().density * value);
    }
}
