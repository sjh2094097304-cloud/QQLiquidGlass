package com.qiutian.bianpaobubble;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Lightweight companion page. All functional settings live inside QQ -> Settings -> Bubble. */
public final class MainActivity extends Activity {
    private static final int PINK = Color.rgb(242, 93, 156);
    private static final int PINK_DARK = Color.rgb(207, 48, 115);
    private static final int TEXT = Color.rgb(36, 31, 39);
    private static final int MUTED = Color.rgb(126, 116, 124);
    private static final String AUTHOR_QQ = AppConfig.AUTHOR_QQ;
    private static final String GROUP_QQ = AppConfig.GROUP_QQ;
    private static final String FEEDBACK_EMAIL = AUTHOR_QQ + "@qq.com";
    private static final String SOURCE_URL = "https://github.com/oneQAQone/QFun";
    private static final String GROUP_LINK = "https://qun.qq.com/universal-share/share?ac=1&authKey=YDJxU5ttLuVx4JTgrQ4bRIhqmGreuxwo8eU%2FPJM0Z4L%2Brn4CvJ6l6JEPg%2BZA%2FBir&busi_data=eyJncm91cENvZGUiOiI4NTMyNTA1NjciLCJ0b2tlbiI6InNJRXdyRVd5MFhXdXdMSUc5MzdSRkhNRXVpWkptMkIwRXFsOVEyVzIwQUl3bURUZmcrRHdUeXZHcTZXQmM3Z3EiLCJ1aW4iOiIzNjk0NDc2NjAyIn0%3D&data=FlyhCV-ykg_kBW7Ht40g2R0qr6xceyAMqH6MRo1-WiITUpONJseblPC6IFhjsjTeA37l8d6Lv_JJK6TSB2bRRg&svctype=4&tempid=h5_group_info";

    private SharedPreferences prefs;
    private boolean noticeShowing;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        SignatureGuard.enforceInstalled(this);
        prefs = AppConfig.prefs(this);
        rememberFrameworkVersion();
        setContentView(buildContent());
        if (!prefs.getBoolean(AppConfig.EXTERNAL_NOTICE_V135, false)) {
            getWindow().getDecorView().post(this::showFirstNotice);
        }
    }

    private View buildContent() {
        FrameLayout root = new FrameLayout(this);
        root.setBackground(liquidBackground(0));
        WatermarkView watermark = new WatermarkView();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout page = column();
        page.setPadding(dp(18), dp(22), dp(18), dp(36));
        page.setBackgroundColor(Color.TRANSPARENT);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        LinearLayout hero = column();
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(20), dp(22), dp(20), dp(22));
        hero.setBackground(glass(Color.argb(218, 255, 255, 255), 26));
        hero.setElevation(dp(4));
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.app_icon);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.addView(icon, new LinearLayout.LayoutParams(dp(78), dp(78)));
        TextView title = label("百变气泡", 28, TEXT, true);
        title.setGravity(Gravity.CENTER);
        hero.addView(title, top(10));
        TextView author = label("作者：秋天  ·  QQ：" + AUTHOR_QQ, 14, MUTED, false);
        author.setGravity(Gravity.CENTER);
        author.setOnClickListener(v -> openAuthorProfile());
        hero.addView(author, top(5));
        TextView status = label(moduleStatus(), 14, statusColor(), true);
        status.setGravity(Gravity.CENTER);
        hero.addView(status, top(7));
        page.addView(hero, match());

        Button changelog = button("查看更新日志  ·  v3.7", Color.rgb(230, 72, 145));
        changelog.setOnClickListener(v -> showChangelog());
        page.addView(changelog, spaced());

        LinearLayout contact = card("作者与反馈", "作者按钮打开 QQ 主页；Bug 反馈使用邮箱并自动复制收件地址。", PINK);
        Button profile = button("打开作者秋天主页", PINK);
        profile.setOnClickListener(v -> openAuthorProfile());
        contact.addView(profile, top(16));

        Button feedback = button("反馈 Bug", Color.rgb(220, 55, 124));
        feedback.setOnClickListener(v -> openFeedbackEmail());
        contact.addView(feedback, top(10));

        Button copyQq = button("复制作者 QQ：" + AUTHOR_QQ, Color.rgb(255, 225, 238));
        copyQq.setTextColor(PINK);
        copyQq.setOnClickListener(v -> copy(AUTHOR_QQ, "作者 QQ 已复制"));
        contact.addView(copyQq, top(10));

        Button group = button("反馈交流群", Color.rgb(255, 225, 238));
        group.setTextColor(PINK);
        group.setOnClickListener(v -> openGroup());
        contact.addView(group, top(10));

        Button source = button("本模块基于 QFun 开源源码集成修改", Color.rgb(108, 91, 159));
        source.setOnClickListener(v -> openSource());
        contact.addView(source, top(10));
        page.addView(contact, spaced());

        LinearLayout warning = card("严禁倒卖",
                "本模块仅供个人学习与自用测试。禁止倒卖、二改倒卖、冒充作者或删除作者信息。唯一反馈群："
                        + GROUP_QQ + "。", PINK_DARK);
        page.addView(warning, spaced());

        LinearLayout note = card("说明", "基于疑是天上星的气泡脚本，由秋天移植为模块。支持 QQ 9.2.75 及以上，采用传统 Xposed 接口，支持框架隔离时的本地配置保存。", Color.rgb(156, 120, 218));
        page.addView(note, spaced());

        TextView watermarkLabel = label("请勿倒卖  ·  唯一反馈群 " + GROUP_QQ, 12,
                Color.rgb(151, 51, 105), true);
        watermarkLabel.setGravity(Gravity.CENTER);
        watermarkLabel.setAlpha(0.94f);
        page.addView(watermarkLabel, top(18));

        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Keep the anti-resale watermark above every card and button. It never consumes touches.
        root.addView(watermark, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    private void showFirstNotice() {
        if (noticeShowing || isFinishing() || isDestroyed()
                || prefs.getBoolean(AppConfig.EXTERNAL_NOTICE_V135, false)) return;
        noticeShowing = true;
        Dialog dialog = new Dialog(this);
        LinearLayout body = column();
        body.setPadding(dp(22), dp(22), dp(22), dp(22));
        body.setBackground(liquidBackground(26));

        TextView title = label("百变气泡公告", 24, TEXT, true);
        title.setGravity(Gravity.CENTER);
        body.addView(title, match());
        TextView warning = label("请勿倒卖 · 请勿冒充作者", 14, PINK_DARK, true);
        warning.setGravity(Gravity.CENTER);
        warning.setPadding(dp(10), dp(8), dp(10), dp(8));
        warning.setBackground(round(Color.rgb(255, 226, 239), 16));
        body.addView(warning, top(10));

        TextView guide = label("1. 本模块仅支持 QQ 9.2.75 及以上。\n"
                + "2. 功能入口：QQ→设置→气泡。\n"
                + "3. 长按消息点击“秋天”可添加气泡 ID。\n"
                + "4. 禁止倒卖、二改倒卖、冒充作者或删除作者信息。\n"
                + "5. 本模块不申请联网权限，不收集 QQ 账号数据。\n"
                + "6. Bug 反馈请联系作者 QQ " + AUTHOR_QQ + "，或加入唯一反馈群 "
                + GROUP_QQ + "。\n"
                + "7. 点击确认后，本公告不会再次弹出。", 14, TEXT, false);
        guide.setLineSpacing(dp(4), 1.08f);
        guide.setPadding(dp(15), dp(15), dp(15), dp(15));
        guide.setBackground(glass(Color.argb(220, 255, 240, 248), 18));
        body.addView(guide, top(14));

        Button feedback = button("反馈 Bug", Color.rgb(220, 55, 124));
        feedback.setOnClickListener(v -> openFeedbackEmail());
        body.addView(feedback, top(14));
        Button confirm = button("我已阅读并确认关闭", PINK);
        confirm.setEnabled(false);
        confirm.setAlpha(0.70f);
        confirm.setOnClickListener(v -> {
            if (!prefs.edit().putBoolean(AppConfig.EXTERNAL_NOTICE_V135, true).commit()) {
                toast("公告状态保存失败，请重试");
                return;
            }
            dialog.dismiss();
        });
        body.addView(confirm, top(10));

        Handler countdownHandler = new Handler(Looper.getMainLooper());
        int[] seconds = {5};
        Runnable countdown = new Runnable() {
            @Override
            public void run() {
                if (!dialog.isShowing()) return;
                if (seconds[0] <= 0) {
                    confirm.setText("我已阅读并确认关闭");
                    confirm.setEnabled(true);
                    confirm.setAlpha(1f);
                    return;
                }
                confirm.setText("请阅读公告（" + seconds[0] + " 秒）");
                seconds[0]--;
                countdownHandler.postDelayed(this, 1000L);
            }
        };

        dialog.setContentView(body);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(ignored -> {
            countdownHandler.removeCallbacks(countdown);
            noticeShowing = false;
        });
        dialog.show();
        countdownHandler.post(countdown);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.dimAmount = 0.56f;
            window.setAttributes(lp);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    private void showChangelog() {
        if (isFinishing() || isDestroyed()) return;
        Dialog dialog = new Dialog(this);
        LinearLayout body = column();
        body.setPadding(dp(22), dp(22), dp(22), dp(20));
        body.setBackground(liquidBackground(26));

        TextView title = label("百变气泡更新日志", 23, TEXT, true);
        title.setGravity(Gravity.CENTER);
        body.addView(title, match());
        TextView subtitle = label("当前版本 v3.7 · 点击关闭后可随时再次查看", 13, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        body.addView(subtitle, top(6));

        TextView content = label(releaseHistory(), 14, TEXT, false);
        content.setLineSpacing(dp(4), 1.08f);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        content.setBackground(glass(Color.argb(221, 255, 247, 252), 19));
        ScrollView history = new ScrollView(this);
        history.setFillViewport(true);
        history.addView(content, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(-1, dp(470));
        historyParams.topMargin = dp(14);
        body.addView(history, historyParams);

        Button close = button("关闭更新日志", PINK);
        close.setOnClickListener(v -> dialog.dismiss());
        body.addView(close, top(12));
        dialog.setContentView(body);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.91f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.dimAmount = 0.52f;
            window.setAttributes(lp);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    private String releaseHistory() {
        return ReleaseNotes.V37 + "\n\n历史版本记录\nv3.6\n"
                + "• 修复长时间运行后气泡模式状态与发送缓存不同步。\n"
                + "• 发送前自动刷新并校正开关状态，无需重新开关即可恢复。\n"
                + "• 修复独立安装包清除数据后无法定位模块头像。\n"
                + "• 长按“秋天”菜单增加渲染后头像替换，不再显示滑杆占位图标。\n"
                + "• 修复 FPA 环境配置读写、自检误报与导入配置失败。\n"
                + "• 自检在 Provider 受限时自动使用 QQ 本地通道，不再误报 CFG-IO-001。\n"
                + "• 商城只识别 2_ 类型气泡 ID，挂件、头像与主题不再弹窗。\n"
                + "• 长按头像固定为 24dp 正方形比例，避免横向拉伸。\n"
                + "• 修复清空气泡后的模式关闭状态与文字提示。\n"
                + "• Bug 反馈改为复制作者邮箱并打开邮件应用。\n"
                + "• 诊断日志增加智能错误代码、关键异常位置与兼容通道状态。\n"
                + "• 消息气泡 ID 严格校验为 4—10 位，普通消息不再误弹 115。\n\n"
                + "v1.3.5\n"
                + "• 优化 QQ 9.3.50 与 FPA 3.6 的配置同步稳定性。\n"
                + "• 修复首次公告状态保存与重复显示问题。\n"
                + "• 修复长按菜单头像加载及部分消息气泡 ID 识别。\n"
                + "• 优化随机气泡算法，全部 ID 每轮随机打乱且不重复。\n"
                + "• 优化防撤回文字提醒、模块状态检测及运行流畅性。\n"
                + "• 减少设置扫描与跨进程写入，降低多模块资源冲突。\n"
                + "• 升级 Liquid Glass 界面并增强 Android 16/17 兼容性。\n\n"
                + "v1.3.3\n"
                + "• 加入公告记忆、全屏防倒卖水印与签名完整性保护。\n"
                + "• 优化应用头像、长按菜单图标和粉色玻璃质感。\n\n"
                + "v1.3.2\n"
                + "• 重构独立模式、随机模式与气泡 ID 管理。\n"
                + "• 设置修改支持即时保存并在 QQ 内生效。\n\n"
                + "v1.2.3\n"
                + "• 独立模式与随机模式改为互斥开关。\n"
                + "• 增加独立气泡 ID 管理入口、格式校验与重复拦截。\n"
                + "• 长按消息菜单增加“秋天”入口并优化显示位置。\n\n"
                + "v1.2.2\n"
                + "• 每个气泡 ID 改为独立卡片，可复制、固定或移除。\n"
                + "• 新增 ID 改为逐个添加，保存逻辑更加清晰。\n\n"
                + "v1.2.1\n"
                + "• 修复 QQ 主题导致模式开关样式异常的问题。\n"
                + "• 增强 QQNT 长按菜单基类与消息类型兼容。\n\n"
                + "v1.2.0\n"
                + "• 将气泡功能真正接入 QQ 设置页面。\n"
                + "• 增加设置入口接管、商城 ID 深层识别与长按消息解析。\n"
                + "• 外部应用精简为作者、反馈、公告及模块说明。";
    }

    private LinearLayout card(String title, String subtitle, int accent) {
        LinearLayout card = column();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(glass(Color.argb(216, 255, 255, 255), 22));
        card.setElevation(dp(3));
        card.addView(label(title, 19, TEXT, true), match());
        TextView subtitleView = label(subtitle, 13, MUTED, false);
        subtitleView.setLineSpacing(dp(2), 1.08f);
        card.addView(subtitleView, top(6));
        View bar = new View(this);
        bar.setBackground(round(accent, 3));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(42), dp(4));
        barParams.topMargin = dp(13);
        card.addView(bar, barParams);
        return card;
    }

    private void openAuthorProfile() {
        openProfile(AUTHOR_QQ, "作者");
    }

    private void openAuthorChat() {
        String primary = "mqqapi://im/chat?chat_type=wpa&uin=" + AUTHOR_QQ
                + "&version=1&src_type=internal";
        String fallback = "mqqwpa://im/chat?chat_type=wpa&uin=" + AUTHOR_QQ
                + "&version=1&src_type=web";
        if (!openQqJump(primary) && !openQqUri(primary)
                && !openQqJump(fallback) && !openQqUri(fallback)) {
            copy(AUTHOR_QQ, "反馈跳转失败，作者 QQ 已复制");
        }
    }

    private void openFeedbackEmail() {
        copy(FEEDBACK_EMAIL, "反馈邮箱已复制，正在打开邮箱");
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO,
                    Uri.parse("mailto:" + FEEDBACK_EMAIL));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{FEEDBACK_EMAIL});
            intent.putExtra(Intent.EXTRA_SUBJECT, "百变气泡 3.7 Bug 反馈");
            intent.putExtra(Intent.EXTRA_TEXT,
                    "请描述出现问题的操作步骤，并粘贴“QQ 设置 → 气泡 → 诊断日志”中的检测结果：\n\n");
            startActivity(Intent.createChooser(intent, "选择邮箱应用发送 Bug 反馈"));
        } catch (Throwable ignored) {
            toast("未找到邮箱应用，反馈邮箱已复制：" + FEEDBACK_EMAIL);
        }
    }

    private void openProfile(String qq, String label) {
        String primary = "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + qq
                + "&card_type=person&source=qrcode";
        String fallback = "mqqwpa://im/chat?chat_type=wpa&uin=" + qq + "&version=1&src_type=web";
        if (!openQqJump(primary) && !openQqUri(primary) && !openQqJump(fallback) && !openQqUri(fallback)) {
            copy(qq, label + "主页跳转失败，QQ 已复制");
        }
    }

    private void openSource() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) {
            copy(SOURCE_URL, "源码链接已复制");
        }
    }

    private void openGroup() {
        String nativeUri = "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + GROUP_QQ
                + "&card_type=group&source=qrcode";
        if (openQqUri(nativeUri)) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GROUP_LINK)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) {
            copy(GROUP_QQ, "群聊跳转失败，群号已复制");
        }
    }

    private boolean openQqUri(String value) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(value));
            intent.setPackage("com.tencent.mobileqq");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(getPackageManager()) == null) return false;
            startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean openQqJump(String value) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(value));
            intent.setClassName("com.tencent.mobileqq", "com.tencent.mobileqq.activity.JumpActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private String moduleStatus() {
        String qq = installedQqVersion();
        if (qq == null) return "未检测到 QQ";
        if (AppConfig.compareVersion(qq, AppConfig.MIN_QQ_VERSION) < 0) return "QQ " + qq + " · 版本不适配";
        long ping = prefs.getLong("lastHookPing", 0L);
        return System.currentTimeMillis() - ping < 300_000L ? "已在 QQ " + qq + " 中运行" : "QQ " + qq + " · 等待框架激活";
    }

    private void rememberFrameworkVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo("fun.fpa", 0);
            String version = info.versionName == null ? "已安装" : info.versionName.trim();
            if (!version.isEmpty()) prefs.edit().putString("detectedFpaVersion", version).apply();
        } catch (Throwable ignored) {
        }
    }

    private int statusColor() {
        String qq = installedQqVersion();
        if (qq == null || AppConfig.compareVersion(qq, AppConfig.MIN_QQ_VERSION) < 0) return Color.rgb(210, 75, 85);
        return Color.rgb(58, 164, 119);
    }

    @SuppressWarnings("deprecation")
    private String installedQqVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.tencent.mobileqq", 0);
            return info.versionName;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void copy(String value, String message) {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        manager.setPrimaryClip(ClipData.newPlainText("百变气泡", value));
        toast(message);
    }

    private Button button(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setMinHeight(dp(50));
        button.setBackground(liquidButton(color, 17));
        button.setElevation(dp(2));
        return button;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private GradientDrawable glass(int color, int radius) { GradientDrawable d = round(color, radius); d.setStroke(dp(1), Color.argb(210, 255, 255, 255)); return d; }
    private GradientDrawable liquidButton(int color, int radius) { GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{mix(color, Color.WHITE, 0.18f), color, mix(color, PINK_DARK, 0.22f)}); d.setCornerRadius(dp(radius)); d.setStroke(dp(1), Color.argb(145, 255, 255, 255)); return d; }
    private GradientDrawable liquidBackground(int radius) { GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(255, 238, 248), Color.rgb(255, 208, 232), Color.rgb(235, 218, 255), Color.rgb(255, 243, 249)}); d.setCornerRadius(dp(radius)); return d; }
    private int mix(int from, int to, float amount) { float keep = 1f - amount; return Color.rgb(Math.round(Color.red(from) * keep + Color.red(to) * amount), Math.round(Color.green(from) * keep + Color.green(to) * amount), Math.round(Color.blue(from) * keep + Color.blue(to) * amount)); }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout.LayoutParams match() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams top(int top) { LinearLayout.LayoutParams p = match(); p.topMargin = dp(top); return p; }
    private LinearLayout.LayoutParams spaced() { return top(14); }

    private final class WatermarkView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        WatermarkView() {
            super(MainActivity.this);
            paint.setColor(Color.argb(86, 164, 35, 104));
            paint.setTextSize(dp(14));
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            setClickable(false);
            setFocusable(false);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;
            String value = "请勿倒卖  ·  反馈群 " + GROUP_QQ;
            canvas.save();
            canvas.rotate(-24f, width / 2f, height / 2f);
            int stepX = dp(250);
            int stepY = dp(105);
            for (int y = -height; y < height * 2; y += stepY) {
                int offset = ((y / stepY) & 1) == 0 ? 0 : stepX / 2;
                for (int x = -width - stepX; x < width * 2; x += stepX) {
                    canvas.drawText(value, x + offset, y, paint);
                }
            }
            canvas.restore();
        }
    }
}
