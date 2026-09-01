package com.qiutian.bianpaobubble.hook;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

final class BubbleDialog {
    private static final int PINK = Color.rgb(242, 93, 156);
    private static final int BLUE = Color.rgb(62, 122, 247);
    private static final int TEXT = Color.rgb(36, 31, 39);
    private static final int MUTED = Color.rgb(126, 116, 124);

    private BubbleDialog() {}

    static void showMessage(Activity activity, Object msg) {
        if (!valid(activity)) return;
        int id = Reflector.bubbleId(msg);
        if (!isValidBubbleId(id)) return;
        activity.runOnUiThread(() -> {
            Bundle config = HostConfig.get(activity);
            int[] pool = config.getIntArray("pool");
            boolean contained = contains(pool, id);
            Dialog dialog = baseDialog(activity, "MESSAGE BUBBLE", "气泡 ID", String.valueOf(id),
                    contained ? "已在随机气泡池" : "尚未添加到随机气泡池");
            LinearLayout body = (LinearLayout) dialog.findViewById(0x425501);
            Button copy = button(activity, "获取并复制气泡 ID", BLUE);
            copy.setOnClickListener(v -> {
                copy(activity, id);
                dialog.dismiss();
            });
            body.addView(copy, matchTop(activity, 16));
            LinearLayout actions = row(activity, 10);
            Button add = button(activity, "添加气泡 ID", Color.rgb(255, 218, 234));
            add.setTextColor(PINK);
            add.setEnabled(!contained);
            add.setAlpha(contained ? 0.45f : 1f);
            add.setOnClickListener(v -> {
                Bundle latest = HostConfig.get(activity);
                int[] updated = addId(latest.getIntArray("pool"), id);
                if (!checkPoolLimit(activity, updated)) return;
                HostConfig.stageSettings(latest.getBoolean("masterEnabled", false),
                        latest.getBoolean("randomEnabled", false), latest.getBoolean("lockedEnabled", false),
                        latest.getInt("lockedId", 0), updated);
                ConfigUi.write(activity, () -> HostConfig.call(activity, "addBubble", id), "已加入随机气泡池", null);
                dialog.dismiss();
            });
            Button remove = button(activity, "取消气泡 ID", Color.rgb(246, 240, 244));
            remove.setTextColor(contained ? TEXT : MUTED);
            remove.setEnabled(contained);
            remove.setAlpha(contained ? 1f : 0.45f);
            remove.setOnClickListener(v -> {
                Bundle latest = HostConfig.get(activity);
                int[] updated = removeId(latest.getIntArray("pool"), id);
                boolean random = latest.getBoolean("randomEnabled", false) && updated.length > 0;
                int lockedId = latest.getInt("lockedId", 0);
                if (lockedId == id || updated.length == 0) lockedId = 0;
                boolean locked = latest.getBoolean("lockedEnabled", false) && lockedId > 0;
                HostConfig.stageSettings(random || locked, random, locked, lockedId, updated);
                ConfigUi.write(activity, () -> HostConfig.call(activity, "removeBubble", id), "已从随机气泡池移除", null);
                dialog.dismiss();
            });
            actions.addView(add, weighted());
            actions.addView(remove, weighted());
            body.addView(actions, matchTop(activity, 10));
            Button close = button(activity, "关闭", Color.rgb(246, 240, 244));
            close.setTextColor(TEXT);
            close.setOnClickListener(v -> dialog.dismiss());
            body.addView(close, matchTop(activity, 10));
            finish(dialog, activity);
        });
    }

    /** QQ bubble IDs accepted by the module UI are 4-10 decimal digits. */
    static boolean isValidBubbleId(int id) {
        return id >= 1000;
    }

    static void showMall(Activity activity, int id) {
        if (!valid(activity) || !isValidBubbleId(id)) return;
        activity.runOnUiThread(() -> {
            Dialog dialog = baseDialog(activity, "BUBBLE MALL", "发现气泡资源", "气泡 ID：" + id,
                    "请选择使用方式，不会再默认切换模式");
            LinearLayout body = (LinearLayout) dialog.findViewById(0x425501);
            Button locked = button(activity, "独立气泡使用", BLUE);
            locked.setOnClickListener(v -> {
                Bundle config = HostConfig.get(activity);
                int[] updated = addId(config.getIntArray("pool"), id);
                if (!checkPoolLimit(activity, updated)) return;
                HostConfig.stageSettings(true, false, true, id, updated);
                ConfigUi.write(activity, () -> HostConfig.call(activity, "applyBubble", id), "已设为独立气泡", null);
                dialog.dismiss();
            });
            body.addView(locked, matchTop(activity, 16));
            Button random = button(activity, "加入随机气泡", Color.rgb(255, 218, 234));
            random.setTextColor(PINK);
            random.setOnClickListener(v -> {
                Bundle config = HostConfig.get(activity);
                int[] updated = addId(config.getIntArray("pool"), id);
                if (!checkPoolLimit(activity, updated)) return;
                HostConfig.stageSettings(true, true, false, 0, updated);
                ConfigUi.write(activity, () -> HostConfig.saveSettings(activity,
                        true, true, false, 0, updated), "已加入随机气泡并启用随机模式", null);
                dialog.dismiss();
            });
            body.addView(random, matchTop(activity, 10));
            Button copy = button(activity, "复制 ID", Color.rgb(242, 243, 247));
            copy.setTextColor(TEXT);
            copy.setOnClickListener(v -> {
                copy(activity, id);
                dialog.dismiss();
            });
            body.addView(copy, matchTop(activity, 10));
            finish(dialog, activity);
        });
    }

    private static Dialog baseDialog(Activity activity, String badge, String title, String value, String subtitle) {
        Dialog dialog = new Dialog(activity);
        LinearLayout body = column(activity, 7);
        body.setId(0x425501);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(activity, 24);
        body.setPadding(pad, pad, pad, pad);
        body.setBackground(liquidBackground(activity, 28));

        ImageView avatar = new ImageView(activity);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setBackground(glass(activity, Color.argb(170, 255, 255, 255), 22));
        avatar.setPadding(dp(activity, 2), dp(activity, 2), dp(activity, 2), dp(activity, 2));
        avatar.setClipToOutline(true);
        Drawable icon = ModuleIcon.load(activity);
        if (icon != null) avatar.setImageDrawable(icon);
        body.addView(avatar, new LinearLayout.LayoutParams(dp(activity, 58), dp(activity, 58)));

        TextView badgeView = text(activity, badge, 11, PINK, true);
        badgeView.setGravity(Gravity.CENTER);
        badgeView.setPadding(dp(activity, 13), dp(activity, 7), dp(activity, 13), dp(activity, 7));
        badgeView.setBackground(round(activity, Color.rgb(255, 220, 235), 18));
        body.addView(badgeView, matchTop(activity, 10));
        TextView titleView = text(activity, title, 23, TEXT, true);
        titleView.setGravity(Gravity.CENTER);
        body.addView(titleView, matchTop(activity, 15));
        TextView valueView = text(activity, value, 25, BLUE, false);
        valueView.setGravity(Gravity.CENTER);
        valueView.setPadding(dp(activity, 12), dp(activity, 13), dp(activity, 12), dp(activity, 13));
        valueView.setBackground(glass(activity, Color.argb(172, 255, 239, 246), 18));
        body.addView(valueView, matchTop(activity, 14));
        TextView subView = text(activity, subtitle, 14, MUTED, true);
        subView.setGravity(Gravity.CENTER);
        body.addView(subView, matchTop(activity, 10));
        dialog.setContentView(body);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    private static void finish(Dialog dialog, Activity activity) {
        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window == null) return;
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.dimAmount = 0.58f;
            window.setAttributes(lp);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try { window.setBackgroundBlurRadius(dp(activity, 34)); } catch (Throwable ignoredBlur) {}
            }
        });
        dialog.show();
    }

    private static void copy(Activity activity, int id) {
        ClipboardManager manager = (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
        if (manager == null) return;
        manager.setPrimaryClip(ClipData.newPlainText("bubble_id", String.valueOf(id)));
        Toast.makeText(activity, "已复制气泡 ID：" + id, Toast.LENGTH_SHORT).show();
    }

    private static boolean checkPoolLimit(Activity activity, int[] pool) {
        if (pool.length <= com.qiutian.bianpaobubble.AppConfig.MAX_IDS) return true;
        Toast.makeText(activity, "气泡池最多保存 300 个 ID", Toast.LENGTH_SHORT).show();
        return false;
    }

    private static boolean contains(int[] values, int target) {
        if (values == null) return false;
        for (int value : values) if (value == target) return true;
        return false;
    }

    private static int[] addId(int[] values, int id) {
        if (contains(values, id)) return values == null ? new int[]{id} : values.clone();
        int length = values == null ? 0 : values.length;
        int[] result = new int[length + 1];
        if (length > 0) System.arraycopy(values, 0, result, 0, length);
        result[length] = id;
        return result;
    }

    private static int[] removeId(int[] values, int id) {
        if (values == null || values.length == 0) return new int[0];
        int count = 0;
        for (int value : values) if (value > 0 && value != id) count++;
        int[] result = new int[count];
        int index = 0;
        for (int value : values) if (value > 0 && value != id) result[index++] = value;
        return result;
    }

    private static boolean valid(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private static Button button(Activity activity, String value, int color) {
        Button button = new Button(activity);
        button.setText(value);
        button.setTextSize(15);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(dp(activity, 52));
        button.setBackground(liquidButton(activity, color, 17));
        button.setElevation(dp(activity, 2));
        return button;
    }

    private static TextView text(Activity activity, String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static LinearLayout column(Activity activity, int gap) {
        LinearLayout view = new LinearLayout(activity);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private static LinearLayout row(Activity activity, int gap) {
        LinearLayout view = new LinearLayout(activity);
        view.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable divider = new GradientDrawable();
        divider.setColor(Color.TRANSPARENT);
        divider.setSize(dp(activity, gap), 1);
        view.setDividerDrawable(divider);
        view.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        return view;
    }

    private static GradientDrawable round(Activity activity, int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(activity, radius));
        return drawable;
    }

    private static GradientDrawable glass(Activity activity, int color, int radius) {
        int alpha = Color.alpha(color) == 255 ? 178 : Color.alpha(color);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(Math.min(220, alpha + 28), 255, 255, 255),
                        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                        Color.argb(Math.max(105, alpha - 24), 255, 222, 239)});
        drawable.setCornerRadius(dp(activity, radius));
        drawable.setStroke(dp(activity, 1), Color.argb(220, 255, 255, 255));
        return drawable;
    }

    private static GradientDrawable liquidBackground(Activity activity, int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(255, 239, 248), Color.rgb(255, 213, 234),
                        Color.rgb(238, 224, 255), Color.rgb(255, 247, 251)});
        drawable.setCornerRadius(dp(activity, radius));
        drawable.setStroke(dp(activity, 1), Color.argb(220, 255, 255, 255));
        return drawable;
    }

    private static GradientDrawable liquidButton(Activity activity, int color, int radius) {
        int alpha = Math.min(232, Color.alpha(color) == 255 ? 232 : Color.alpha(color));
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{withAlpha(mix(color, Color.WHITE, 0.20f), Math.min(245, alpha + 10)),
                        withAlpha(color, alpha), withAlpha(mix(color, PINK, 0.20f), alpha)});
        drawable.setCornerRadius(dp(activity, radius));
        drawable.setStroke(dp(activity, 1), Color.argb(145, 255, 255, 255));
        return drawable;
    }

    private static int mix(int from, int to, float amount) {
        float keep = 1f - amount;
        return Color.rgb(Math.round(Color.red(from) * keep + Color.red(to) * amount),
                Math.round(Color.green(from) * keep + Color.green(to) * amount),
                Math.round(Color.blue(from) * keep + Color.blue(to) * amount));
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-2, -2); }
    private static LinearLayout.LayoutParams matchTop(Activity activity, int top) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(activity, top); return p; }
    private static LinearLayout.LayoutParams weighted() { return new LinearLayout.LayoutParams(0, -2, 1f); }
}
