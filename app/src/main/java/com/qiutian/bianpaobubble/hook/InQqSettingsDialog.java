package com.qiutian.bianpaobubble.hook;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.qiutian.bianpaobubble.AppConfig;
import com.qiutian.bianpaobubble.ReleaseNotes;

import java.util.ArrayList;
import java.util.List;

/** Settings surface rendered directly inside the QQ process. */
final class InQqSettingsDialog {
    private static final int PINK = Color.rgb(232, 71, 142);
    private static final int PINK_DARK = Color.rgb(207, 48, 115);
    private static final int BLUE = Color.rgb(98, 105, 238);
    private static final int TEXT = Color.rgb(29, 29, 31);
    private static final int MUTED = Color.rgb(134, 134, 139);
    // QFun liquid surface: roughly 60% white with a bright hairline highlight.
    private static final int CARD = Color.argb(158, 255, 255, 255);
    private static Dialog showing;

    private InQqSettingsDialog() {}

    static boolean isShowing() {
        return showing != null && showing.isShowing();
    }

    static void showEntry(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        activity.runOnUiThread(() -> {
            if (isShowing()) {
                Window window = showing.getWindow();
                if (window != null) window.getDecorView().requestFocus();
                return;
            }
            if (HostConfig.isNoticeAcceptedV135(activity)) show(activity);
            else showEntryNotice(activity);
        });
    }

    private static void showEntryNotice(Activity activity) {
        Dialog dialog = new Dialog(activity);
        showing = dialog;
        LinearLayout body = compactBody(activity, "使用须知", "确认后记住，沿用已保存的公告状态。");

        TextView guide = text(activity,
                "1. 仅支持 QQ 9.2.75 及以上。\n"
                        + "2. 独立模式与随机模式只能启用一个。\n"
                        + "3. 长按消息点“秋天”可添加气泡 ID。\n"
                        + "4. 禁止倒卖、二改倒卖、冒充作者或删除作者信息。\n"
                        + "5. 唯一反馈群：" + AppConfig.GROUP_QQ + "。\n"
                        + "6. 防撤回不要和其他模块的同类功能同时开启。",
                14, TEXT, false);
        guide.setLineSpacing(dp(activity, 4), 1.08f);
        guide.setPadding(dp(activity, 15), dp(activity, 15), dp(activity, 15), dp(activity, 15));
        guide.setBackground(glass(activity, Color.argb(170, 255, 238, 247), 20));
        body.addView(guide, top(activity, 14));

        Button confirm = button(activity, "请阅读公告（5 秒）", PINK);
        confirm.setEnabled(false);
        confirm.setAlpha(0.70f);
        body.addView(confirm, top(activity, 14));
        Button back = button(activity, "返回 QQ 设置", Color.argb(215, 255, 255, 255));
        back.setTextColor(TEXT);
        back.setOnClickListener(v -> dialog.dismiss());
        body.addView(back, top(activity, 8));

        confirm.setOnClickListener(v -> {
            confirm.setEnabled(false);
            HostConfig.runAsync(() -> {
                boolean saved = HostConfig.acceptNoticeV135(activity);
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || activity.isDestroyed() || !dialog.isShowing()) return;
                    if (!saved) {
                        confirm.setEnabled(true);
                        toast(activity, "公告状态保存失败，请重试");
                        return;
                    }
                    dialog.dismiss();
                    show(activity);
                });
            });
        });
        Handler countdownHandler = new Handler(Looper.getMainLooper());
        int[] seconds = {5};
        Runnable countdown = new Runnable() {
            @Override
            public void run() {
                if (!dialog.isShowing()) return;
                if (seconds[0] <= 0) {
                    confirm.setText("确认并进入设置");
                    confirm.setEnabled(true);
                    confirm.setAlpha(1f);
                    return;
                }
                confirm.setText("请阅读公告（" + seconds[0] + " 秒）");
                seconds[0]--;
                countdownHandler.postDelayed(this, 1000L);
            }
        };
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(ignored -> {
            countdownHandler.removeCallbacks(countdown);
            if (showing == dialog) showing = null;
        });
        showCompact(dialog, body, activity);
        countdownHandler.post(countdown);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
    }

    static void show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        activity.runOnUiThread(() -> {
            if (showing != null && showing.isShowing()) {
                showing.getWindow().getDecorView().requestFocus();
                return;
            }
            Bundle config = HostConfig.get(activity);
            Dialog dialog = new Dialog(activity);
            showing = dialog;

            FrameLayout surface = new FrameLayout(activity);
            surface.setBackground(liquidBackground(activity, 30));
            surface.setClipToOutline(true);
            addLiquidOrbs(activity, surface);

            ScrollView scroll = new ScrollView(activity);
            scroll.setFillViewport(true);
            scroll.setClipToOutline(true);
            scroll.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout page = column(activity);
            int padding = dp(activity, 18);
            page.setPadding(padding, dp(activity, 20), padding, dp(activity, 22));
            page.setBackgroundColor(Color.TRANSPARENT);
            scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

            LinearLayout hero = new LinearLayout(activity);
            hero.setOrientation(LinearLayout.HORIZONTAL);
            hero.setGravity(Gravity.CENTER_VERTICAL);
            hero.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));
            hero.setBackground(glass(activity, Color.argb(172, 255, 255, 255), 24));
            hero.setElevation(dp(activity, 3));

            ImageView avatar = authorAvatar(activity);
            hero.addView(avatar, new LinearLayout.LayoutParams(dp(activity, 64), dp(activity, 64)));
            LinearLayout heroText = column(activity);
            LinearLayout.LayoutParams heroTextParams = new LinearLayout.LayoutParams(0, -2, 1f);
            heroTextParams.leftMargin = dp(activity, 14);
            hero.addView(heroText, heroTextParams);

            TextView badge = text(activity, "修复版  ·  3.7", 11, PINK_DARK, true);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 12), dp(activity, 6));
            badge.setBackground(round(activity, Color.rgb(255, 221, 236), 18));
            LinearLayout badgeRow = new LinearLayout(activity);
            badgeRow.setGravity(Gravity.START);
            badgeRow.addView(badge, new LinearLayout.LayoutParams(-2, -2));
            heroText.addView(badgeRow, match());

            TextView title = text(activity, "气泡设置", 29, TEXT, true);
            title.setGravity(Gravity.START);
            heroText.addView(title, top(activity, 7));
            TextView note = text(activity, "作者秋天  ·  QQ 9.2.75+", 13, MUTED, false);
            note.setGravity(Gravity.START);
            heroText.addView(note, top(activity, 3));
            page.addView(hero, match());

            List<Integer> poolIds = poolList(config.getIntArray("pool"));
            int initialFixedId = config.getInt("lockedId", 0);
            boolean initialMaster = config.getBoolean("masterEnabled", false);
            boolean initialIndependent = initialMaster
                    && config.getBoolean("lockedEnabled", false) && initialFixedId >= 1000;
            boolean initialRandom = initialMaster && !initialIndependent
                    && config.getBoolean("randomEnabled", false) && !poolIds.isEmpty();
            EditText fixedEditor = editor(activity,
                    initialFixedId >= 1000 ? String.valueOf(initialFixedId) : "", "输入固定气泡 ID");
            fixedEditor.setInputType(InputType.TYPE_CLASS_NUMBER);

            ModeSwitch independentMode = new ModeSwitch(activity, "独立模式", "固定使用一个指定气泡 ID",
                    initialIndependent);
            ModeSwitch randomMode = new ModeSwitch(activity, "随机模式", "每轮先随机洗牌，整轮不重复",
                    initialRandom);
            independentMode.setListener(enabled -> {
                if (enabled) {
                    int fixedId = parseBubbleId(fixedEditor.getText().toString());
                    if (fixedId <= 0) {
                        independentMode.setChecked(false);
                        toast(activity, "请先输入 4—10 位独立气泡 ID");
                        return;
                    }
                    randomMode.setChecked(false);
                }
                persistModes(activity, poolIds, randomMode, independentMode, fixedEditor);
            });
            randomMode.setListener(enabled -> {
                if (enabled) {
                    if (poolIds.isEmpty()) {
                        randomMode.setChecked(false);
                        toast(activity, "请先添加至少一个气泡 ID");
                        return;
                    }
                    independentMode.setChecked(false);
                }
                persistModes(activity, poolIds, randomMode, independentMode, fixedEditor);
            });
            LinearLayout modeCard = card(activity, "模式选择", "点击整行或开关即可立即生效；两种模式只会启用一种。");
            modeCard.addView(independentMode.view, top(activity, 11));
            modeCard.addView(randomMode.view, top(activity, 8));
            page.addView(modeCard, top(activity, 13));

            ModeSwitch antiRevokeMode = new ModeSwitch(activity, "防撤回",
                    "保留对方撤回的消息，并显示“已拦截撤回”文字提醒",
                    config.getBoolean("antiRevokeEnabled", false));
            antiRevokeMode.setListener(enabled -> {
                HostConfig.stageAntiRevoke(enabled);
                ConfigUi.write(activity, () -> HostConfig.saveAntiRevoke(activity, enabled),
                        enabled ? "防撤回已开启" : "防撤回已关闭",
                        state -> antiRevokeMode.setChecked(state.getBoolean("antiRevokeEnabled", false)));
            });
            LinearLayout protectionCard = card(activity, "消息保护",
                    "功能默认关闭；拦截成功会显示文字提醒，请勿与其他防撤回同时开启。");
            protectionCard.addView(antiRevokeMode.view, top(activity, 11));
            page.addView(protectionCard, top(activity, 12));

            LinearLayout poolCard = card(activity, "气泡 ID", "气泡池放在独立管理页面中，每次只添加一个有效 ID。");
            Button manageIds = button(activity, "管理气泡 ID（" + poolIds.size() + " 个）", Color.rgb(255, 225, 238));
            manageIds.setTextColor(PINK);
            Runnable updateManageButton = () -> manageIds.setText("管理气泡 ID（" + poolIds.size() + " 个）");
            manageIds.setOnClickListener(v -> showPoolManager(activity, poolIds, randomMode,
                    independentMode, fixedEditor, updateManageButton));
            poolCard.addView(manageIds, top(activity, 11));
            page.addView(poolCard, top(activity, 12));

            LinearLayout fixedCard = card(activity, "固定气泡", "输入一个 ID 后开启独立模式；点 × 可清除并恢复 QQ 默认气泡。");
            LinearLayout fixedRow = row(activity, 9);
            fixedRow.setGravity(Gravity.CENTER_VERTICAL);
            fixedRow.addView(fixedEditor, weight());
            Button clearFixed = button(activity, "×", Color.rgb(242, 237, 247));
            clearFixed.setTextColor(Color.rgb(105, 93, 110));
            clearFixed.setTextSize(22);
            clearFixed.setContentDescription("清除固定气泡 ID");
            clearFixed.setPadding(0, 0, 0, 0);
            clearFixed.setOnClickListener(v -> {
                fixedEditor.setText("");
                fixedEditor.clearFocus();
                independentMode.setChecked(false);
                randomMode.setChecked(false);
                int[] pool = poolArray(poolIds);
                HostConfig.stageSettings(false, false, false, 0, pool);
                ConfigUi.write(activity, () -> HostConfig.saveSettings(activity, false, false, false, 0, pool),
                        "已清除固定 ID，并恢复 QQ 默认气泡",
                        state -> restoreControls(state, poolIds, randomMode, independentMode, fixedEditor, updateManageButton));
            });
            fixedRow.addView(clearFixed, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 52)));
            fixedCard.addView(fixedRow, top(activity, 10));
            page.addView(fixedCard, top(activity, 12));

            LinearLayout migrationCard = card(activity, "配置迁移",
                    "使用可编辑文本导入或导出，不申请存储权限；导入会先校验再一次保存。");
            LinearLayout migrationActions = row(activity, 9);
            Button importConfig = button(activity, "导入配置", Color.rgb(255, 226, 239));
            importConfig.setTextColor(PINK_DARK);
            importConfig.setOnClickListener(v -> showImportConfig(activity, poolIds, randomMode,
                    independentMode, antiRevokeMode, fixedEditor, updateManageButton));
            Button exportConfig = button(activity, "导出配置", Color.rgb(238, 230, 255));
            exportConfig.setTextColor(Color.rgb(112, 78, 163));
            exportConfig.setOnClickListener(v -> showExportConfig(activity, poolIds, randomMode,
                    independentMode, antiRevokeMode, fixedEditor));
            migrationActions.addView(importConfig, weight());
            migrationActions.addView(exportConfig, weight());
            migrationCard.addView(migrationActions, top(activity, 11));
            page.addView(migrationCard, top(activity, 12));

            Button save = button(activity, "保存设置", BLUE);
            save.setBackground(ripple(activity, gradient(activity, Color.rgb(243, 89, 157),
                    PINK_DARK, 18)));
            save.setElevation(dp(activity, 3));
            save.setOnClickListener(v -> {
                if (randomMode.isChecked() && poolIds.isEmpty()) {
                    toast(activity, "随机模式至少需要一个有效气泡 ID");
                    return;
                }
                int fixedId = parseBubbleId(fixedEditor.getText().toString());
                if (independentMode.isChecked() && fixedId <= 0) {
                    toast(activity, "独立模式需要一个有效气泡 ID");
                    return;
                }
                boolean randomEnabled = randomMode.isChecked();
                boolean independentEnabled = independentMode.isChecked();
                int[] pool = poolArray(poolIds);
                boolean masterEnabled = randomEnabled || independentEnabled;
                HostConfig.stageSettings(masterEnabled, randomEnabled, independentEnabled, fixedId, pool);
                ConfigUi.write(activity, () -> HostConfig.saveSettings(activity, masterEnabled, randomEnabled,
                        independentEnabled, fixedId, pool), "设置已保存并立即生效",
                        state -> restoreControls(state, poolIds, randomMode, independentMode, fixedEditor, updateManageButton));
            });
            page.addView(save, top(activity, 14));

            LinearLayout toolsCard = card(activity, "维护", "清理只移除运行缓存；不会删除气泡池和固定设置。");
            LinearLayout actions = row(activity, 10);
            Button clear = button(activity, "清理缓存", Color.rgb(246, 238, 243));
            clear.setTextColor(TEXT);
            clear.setOnClickListener(v -> {
                ConfigUi.write(activity, () -> HostConfig.call(activity, "clearRuntimeCache"),
                        "缓存已清理，当前设置已保留", null);
            });
            Button restore = button(activity, "恢复 QQ 默认", Color.rgb(255, 222, 235));
            restore.setTextColor(PINK);
            restore.setOnClickListener(v -> {
                fixedEditor.setText("");
                randomMode.setChecked(false);
                independentMode.setChecked(false);
                int[] pool = poolArray(poolIds);
                HostConfig.stageSettings(false, false, false, 0, pool);
                ConfigUi.write(activity, () -> HostConfig.saveSettings(activity, false, false, false, 0, pool),
                        "已停止改写，后续使用 QQ 原气泡",
                        state -> restoreControls(state, poolIds, randomMode, independentMode, fixedEditor, updateManageButton));
            });
            actions.addView(clear, weight());
            actions.addView(restore, weight());
            toolsCard.addView(actions, top(activity, 10));
            Button diagnostics = button(activity, "诊断日志", Color.rgb(255, 232, 242));
            diagnostics.setTextColor(PINK);
            diagnostics.setOnClickListener(v -> showDiagnosticLog(activity));
            toolsCard.addView(diagnostics, top(activity, 9));
            Button changelog = button(activity, "3.7 修复日志", Color.rgb(255, 232, 242));
            changelog.setTextColor(PINK);
            changelog.setOnClickListener(v -> {
                Dialog changes = new Dialog(activity);
                LinearLayout content = compactBody(activity, "更新日志", "百变气泡 3.7");
                TextView notes = text(activity, ReleaseNotes.V37, 14, TEXT, false);
                content.addView(notes, top(activity, 12));
                Button done = button(activity, "关闭", PINK);
                done.setOnClickListener(view -> changes.dismiss());
                content.addView(done, top(activity, 12));
                showTall(changes, content, activity);
            });
            toolsCard.addView(changelog, top(activity, 9));
            page.addView(toolsCard, top(activity, 12));

            Button close = button(activity, "关闭", Color.rgb(255, 255, 255));
            close.setTextColor(TEXT);
            close.setOnClickListener(v -> dialog.dismiss());
            page.addView(close, top(activity, 10));

            surface.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
            dialog.setContentView(surface);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setOnDismissListener(ignored -> {
                if (showing == dialog) showing = null;
            });
            dialog.show();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.93f);
                lp.height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.89f);
                lp.dimAmount = 0.58f;
                window.setAttributes(lp);
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                enableWindowGlass(window, activity);
            }
            reveal(activity, hero, 0);
            reveal(activity, modeCard, 45);
            reveal(activity, protectionCard, 75);
            reveal(activity, poolCard, 105);
            reveal(activity, fixedCard, 135);
            reveal(activity, migrationCard, 165);
            reveal(activity, save, 195);
            reveal(activity, toolsCard, 225);
            reveal(activity, close, 255);
        });
    }

    private static LinearLayout card(Activity activity, String title, String subtitle) {
        LinearLayout card = column(activity);
        card.setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 18));
        card.setBackground(glass(activity, CARD, 24));
        card.setElevation(dp(activity, 4));
        TextView titleView = text(activity, title, 18, TEXT, true);
        card.addView(titleView, match());
        TextView note = text(activity, subtitle, 13, MUTED, false);
        note.setLineSpacing(dp(activity, 2), 1.05f);
        card.addView(note, top(activity, 5));
        return card;
    }

    private static void showPoolManager(Activity activity, List<Integer> ids, ModeSwitch random,
                                        ModeSwitch independent, EditText fixedEditor, Runnable onChanged) {
        Dialog dialog = new Dialog(activity);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout body = compactBody(activity, "气泡 ID 管理", "点击任意 ID 可复制、固定或移除。");
        LinearLayout container = column(activity);
        renderPool(activity, container, ids, random, independent, fixedEditor, onChanged);
        body.addView(container, top(activity, 14));

        Button add = button(activity, "＋ 添加一个气泡 ID", PINK);
        add.setOnClickListener(v -> showAddBubble(activity, ids, container, random, independent,
                fixedEditor, onChanged));
        body.addView(add, top(activity, 12));
        Button close = button(activity, "完成", Color.rgb(246, 240, 244));
        close.setTextColor(TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        body.addView(close, top(activity, 8));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        showTall(dialog, scroll, activity);
    }

    private static void renderPool(Activity activity, LinearLayout container, List<Integer> ids,
                                   ModeSwitch random, ModeSwitch independent, EditText fixedEditor,
                                   Runnable onChanged) {
        container.removeAllViews();
        if (ids.isEmpty()) {
            TextView empty = text(activity, "还没有气泡 ID，点击下方按钮添加", 13, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(activity, 10), dp(activity, 16), dp(activity, 10), dp(activity, 16));
            empty.setBackground(round(activity, Color.rgb(249, 246, 248), 14));
            container.addView(empty, match());
            return;
        }
        for (int i = 0; i < ids.size(); i += 2) {
            LinearLayout line = row(activity, 8);
            addBubbleCard(activity, line, ids.get(i), ids, container, random, independent, fixedEditor, onChanged);
            if (i + 1 < ids.size()) {
                addBubbleCard(activity, line, ids.get(i + 1), ids, container, random, independent, fixedEditor, onChanged);
            } else {
                line.addView(new TextView(activity), weight());
            }
            container.addView(line, i == 0 ? match() : top(activity, 8));
        }
    }

    private static void addBubbleCard(Activity activity, LinearLayout line, int id, List<Integer> ids,
                                      LinearLayout container, ModeSwitch random, ModeSwitch independent,
                                      EditText fixedEditor, Runnable onChanged) {
        TextView card = text(activity, String.valueOf(id), 16, PINK, true);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(activity, 8), dp(activity, 15), dp(activity, 8), dp(activity, 15));
        card.setBackground(ripple(activity, round(activity, Color.rgb(255, 235, 244), 15)));
        pressBounce(card);
        card.setOnClickListener(v -> showBubbleSettings(activity, id, ids, container, random,
                independent, fixedEditor, onChanged));
        line.addView(card, weight());
    }

    private static void showAddBubble(Activity activity, List<Integer> ids, LinearLayout container,
                                      ModeSwitch random, ModeSwitch independent, EditText fixedEditor,
                                      Runnable onChanged) {
        Dialog dialog = new Dialog(activity);
        LinearLayout body = compactBody(activity, "添加气泡 ID", "每次添加一个 ID，添加后会显示为独立卡片。");
        EditText input = editor(activity, "", "输入气泡 ID");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        body.addView(input, top(activity, 14));
        Button add = button(activity, "添加到气泡池", PINK);
        add.setOnClickListener(v -> {
            int id = parseBubbleId(input.getText().toString());
            if (id <= 0) {
                toast(activity, "请输入 4—10 位有效气泡 ID");
                return;
            }
            if (ids.contains(id)) {
                toast(activity, "这个气泡 ID 已经存在");
                return;
            }
            if (ids.size() >= AppConfig.MAX_IDS) {
                toast(activity, "气泡池最多保存 300 个 ID");
                return;
            }
            ids.add(id);
            boolean masterEnabled = random.isChecked() || independent.isChecked();
            HostConfig.stageSettings(masterEnabled, random.isChecked(), independent.isChecked(),
                    parseBubbleId(fixedEditor.getText().toString()), poolArray(ids));
            ConfigUi.write(activity, () -> HostConfig.call(activity, "addBubble", id), "已添加气泡 ID：" + id,
                    state -> {
                        restoreControls(state, ids, random, independent, fixedEditor, onChanged);
                        renderPool(activity, container, ids, random, independent, fixedEditor, onChanged);
                    });
            renderPool(activity, container, ids, random, independent, fixedEditor, onChanged);
            onChanged.run();
            dialog.dismiss();
        });
        body.addView(add, top(activity, 12));
        Button close = button(activity, "取消", Color.rgb(246, 240, 244));
        close.setTextColor(TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        body.addView(close, top(activity, 8));
        showCompact(dialog, body, activity);
    }

    private static void showBubbleSettings(Activity activity, int id, List<Integer> ids,
                                           LinearLayout container, ModeSwitch random, ModeSwitch independent,
                                           EditText fixedEditor, Runnable onChanged) {
        Dialog dialog = new Dialog(activity);
        LinearLayout body = compactBody(activity, "气泡 ID", String.valueOf(id));

        Button fixed = button(activity, "设为固定气泡", BLUE);
        fixed.setOnClickListener(v -> {
            fixedEditor.setText(String.valueOf(id));
            independent.setChecked(true);
            random.setChecked(false);
            HostConfig.stageSettings(true, false, true, id, poolArray(ids));
            ConfigUi.write(activity, () -> HostConfig.call(activity, "applyBubble", id), "已固定使用气泡：" + id,
                    state -> restoreControls(state, ids, random, independent, fixedEditor, onChanged));
            dialog.dismiss();
        });
        body.addView(fixed, top(activity, 14));

        Button copy = button(activity, "复制气泡 ID", Color.rgb(246, 240, 244));
        copy.setTextColor(TEXT);
        copy.setOnClickListener(v -> {
            ClipboardManager manager = (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("bubble_id", String.valueOf(id)));
            toast(activity, "已复制气泡 ID：" + id);
            dialog.dismiss();
        });
        body.addView(copy, top(activity, 8));

        Button remove = button(activity, "从气泡池移除", Color.rgb(255, 225, 238));
        remove.setTextColor(PINK);
        remove.setOnClickListener(v -> {
            ids.remove(Integer.valueOf(id));
            boolean clearedAll = ids.isEmpty();
            boolean removedIndependent = parseBubbleId(fixedEditor.getText().toString()) == id;
            if (clearedAll || removedIndependent) {
                independent.setChecked(false);
                fixedEditor.setText("");
            }
            if (clearedAll) random.setChecked(false);
            boolean masterEnabled = random.isChecked() || independent.isChecked();
            HostConfig.stageSettings(masterEnabled, random.isChecked(), independent.isChecked(),
                    parseBubbleId(fixedEditor.getText().toString()), poolArray(ids));
            ConfigUi.write(activity, () -> HostConfig.call(activity, "removeBubble", id),
                    clearedAll ? "已清除气泡；所有模式全部关闭，请手动开启" : "已移除气泡 ID：" + id,
                    state -> {
                        restoreControls(state, ids, random, independent, fixedEditor, onChanged);
                        renderPool(activity, container, ids, random, independent, fixedEditor, onChanged);
                    });
            renderPool(activity, container, ids, random, independent, fixedEditor, onChanged);
            onChanged.run();
            dialog.dismiss();
        });
        body.addView(remove, top(activity, 8));

        Button close = button(activity, "关闭", Color.rgb(246, 240, 244));
        close.setTextColor(TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        body.addView(close, top(activity, 8));
        showCompact(dialog, body, activity);
    }

    private static void showImportConfig(Activity activity, List<Integer> ids, ModeSwitch random,
                                         ModeSwitch independent, ModeSwitch antiRevoke,
                                         EditText fixedEditor, Runnable onChanged) {
        Dialog dialog = new Dialog(activity);
        LinearLayout body = compactBody(activity, "导入气泡配置",
                "支持完整 JSON、JSON 数组，或逗号/空格/换行分隔的气泡 ID。最多 300 个。");
        EditText input = multiEditor(activity, "粘贴气泡配置或气泡 ID 列表");
        body.addView(input, top(activity, 14));

        Button paste = button(activity, "从剪贴板粘贴", Color.rgb(244, 236, 247));
        paste.setTextColor(TEXT);
        paste.setOnClickListener(v -> {
            try {
                ClipboardManager manager = (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
                if (manager == null || !manager.hasPrimaryClip() || manager.getPrimaryClip() == null
                        || manager.getPrimaryClip().getItemCount() == 0) {
                    toast(activity, "剪贴板中没有可导入的内容");
                    return;
                }
                CharSequence value = manager.getPrimaryClip().getItemAt(0).coerceToText(activity);
                input.setText(value == null ? "" : value.toString());
                input.setSelection(input.length());
            } catch (Throwable error) {
                toast(activity, "读取剪贴板失败");
            }
        });
        body.addView(paste, top(activity, 10));

        Button apply = button(activity, "校验并导入", PINK);
        apply.setOnClickListener(v -> {
            final ConfigCodec.DecodedConfig decoded;
            try {
                decoded = ConfigCodec.decode(input.getText().toString());
            } catch (IllegalArgumentException error) {
                HookLog.error(activity, "导入配置解析失败", error);
                toast(activity, "导入失败：" + error.getMessage());
                return;
            }
            int[] pool = poolArray(decoded.ids);
            boolean master = decoded.randomEnabled || decoded.lockedEnabled;
            apply.setEnabled(false);
            apply.setText("正在保存…");
            long importGeneration = HostConfig.reserveWrite();
            HostConfig.runAsync(() -> {
                Bundle result = HostConfig.importSettings(activity, master, decoded.randomEnabled,
                        decoded.lockedEnabled, decoded.hasAntiRevokeSetting ? decoded.antiRevokeEnabled : null, decoded.lockedId, pool);
                boolean success = HostConfig.success(result);
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    if (importGeneration != HostConfig.generation()) { dialog.dismiss(); return; }
                    if (!success) {
                        HookLog.error(activity, "导入配置写入失败",
                                new IllegalStateException(result == null ? "result=null" : result.toString()));
                        apply.setEnabled(true);
                        apply.setText("校验并导入");
                        toast(activity, "配置保存失败，请查看诊断日志");
                        return;
                    }
                    ids.clear();
                    ids.addAll(poolList(result.getIntArray("pool")));
                    random.setChecked(result.getBoolean("randomEnabled", false));
                    independent.setChecked(result.getBoolean("lockedEnabled", false));
                    antiRevoke.setChecked(result.getBoolean("antiRevokeEnabled", false));
                    int fixedId = result.getInt("lockedId", 0);
                    fixedEditor.setText(fixedId > 0 ? String.valueOf(fixedId) : "");
                    onChanged.run();
                    toast(activity, "已导入“" + decoded.name + "”，共 " + ids.size() + " 个 ID");
                    dialog.dismiss();
                });
            });
        });
        body.addView(apply, top(activity, 9));
        Button close = button(activity, "取消", Color.argb(220, 255, 255, 255));
        close.setTextColor(TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        body.addView(close, top(activity, 8));
        showTall(dialog, body, activity);
    }

    private static void showExportConfig(Activity activity, List<Integer> ids, ModeSwitch random,
                                         ModeSwitch independent, ModeSwitch antiRevoke,
                                         EditText fixedEditor) {
        Dialog dialog = new Dialog(activity);
        LinearLayout body = compactBody(activity, "导出气泡配置",
                "配置名称和 JSON 均可编辑；复制前会再次校验，避免导出损坏内容。");
        EditText name = editor(activity, "我的气泡配置", "自定义配置名称");
        body.addView(name, top(activity, 14));
        EditText preview = multiEditor(activity, "配置 JSON");
        Runnable regenerate = () -> {
            String value = ConfigCodec.encode(name.getText().toString(), random.isChecked(),
                    independent.isChecked(), antiRevoke.isChecked(),
                    parseBubbleId(fixedEditor.getText().toString()), ids);
            preview.setText(value);
            preview.setSelection(0);
        };
        name.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { regenerate.run(); }
        });
        regenerate.run();
        body.addView(preview, top(activity, 10));

        Button copy = button(activity, "复制导出配置", PINK);
        copy.setOnClickListener(v -> {
            String value = preview.getText().toString().trim();
            try {
                ConfigCodec.decode(value);
            } catch (IllegalArgumentException error) {
                toast(activity, "配置已被改坏：" + error.getMessage());
                return;
            }
            try {
                ClipboardManager manager = (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
                if (manager == null) throw new IllegalStateException("clipboard unavailable");
                manager.setPrimaryClip(ClipData.newPlainText("百变气泡配置", value));
                toast(activity, "气泡配置已复制");
                dialog.dismiss();
            } catch (Throwable error) {
                toast(activity, "复制配置失败");
            }
        });
        body.addView(copy, top(activity, 10));
        Button close = button(activity, "取消", Color.argb(220, 255, 255, 255));
        close.setTextColor(TEXT);
        close.setOnClickListener(v -> dialog.dismiss());
        body.addView(close, top(activity, 8));
        showTall(dialog, body, activity);
    }

    private static void showDiagnosticLog(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        toast(activity, "正在读取诊断日志");
        HostConfig.runAsync(() -> {
            Bundle fresh = HostConfig.refresh(activity);
            String stored = fresh.getString("log", "");
            String log = stored == null ? "" : stored.trim();
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                Dialog dialog = new Dialog(activity);
                LinearLayout body = compactBody(activity, "诊断日志",
                        "仅记录关键异常并自动去重；复现问题后复制给作者。最后仅保留约 24 条。");

                TextView logView = text(activity, diagnosticText(log), 12,
                        log.isEmpty() ? MUTED : TEXT, false);
                logView.setTypeface(Typeface.MONOSPACE);
                logView.setTextIsSelectable(true);
                logView.setGravity(Gravity.START | Gravity.TOP);
                logView.setPadding(dp(activity, 13), dp(activity, 13), dp(activity, 13), dp(activity, 13));
                logView.setBackground(round(activity, Color.rgb(250, 244, 248), 16));
                ScrollView logScroll = new ScrollView(activity);
                logScroll.setFillViewport(true);
                logScroll.addView(logView, new ScrollView.LayoutParams(-1, -2));
                LinearLayout.LayoutParams logParams = new LinearLayout.LayoutParams(-1, dp(activity, 310));
                logParams.topMargin = dp(activity, 14);
                body.addView(logScroll, logParams);

                Button detect = button(activity, "检测模块状态", Color.rgb(255, 224, 239));
                detect.setTextColor(PINK_DARK);
                detect.setOnClickListener(v -> {
                    detect.setEnabled(false);
                    detect.setText("正在检测…");
                    HostConfig.runAsync(() -> {
                        Bundle health = HostConfig.healthCheck(activity);
                        String report = moduleHealthText(activity, health) + "\n" + ModuleIcon.diagnostic();
                        if (!health.getBoolean("_healthOk", false)) {
                            HostConfig.appendLog(activity, "模块自检未通过 | "
                                    + report.replace('\n', ' '));
                        }
                        activity.runOnUiThread(() -> {
                            if (activity.isFinishing() || activity.isDestroyed()) return;
                            String currentLog = log == null ? "" : log.trim();
                            logView.setText(report + (currentLog.isEmpty() ? "" : "\n\n异常日志\n" + currentLog));
                            logView.setTextColor(TEXT);
                            detect.setEnabled(true);
                            detect.setText("重新检测模块状态");
                            toast(activity, "检测完成，请查看各项结果及功能触发状态");
                        });
                    });
                });
                body.addView(detect, top(activity, 10));

                LinearLayout actions = row(activity, 9);
                Button copy = button(activity, "复制日志", PINK);
                copy.setOnClickListener(v -> {
                    String current = freshLogText(logView);
                    if (current.isEmpty()) {
                        toast(activity, "暂无可复制的诊断日志");
                        return;
                    }
                    try {
                        ClipboardManager manager = (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
                        if (manager == null) throw new IllegalStateException("clipboard unavailable");
                        manager.setPrimaryClip(ClipData.newPlainText("百变气泡诊断日志", current));
                        toast(activity, "诊断日志已复制");
                    } catch (Throwable error) {
                        toast(activity, "复制失败，请稍后重试");
                    }
                });
                Button clear = button(activity, "清空日志", Color.rgb(246, 240, 244));
                clear.setTextColor(TEXT);
                clear.setOnClickListener(v -> {
                    ConfigUi.write(activity, () -> {
                        Bundle result = HostConfig.call(activity, "clearLog");
                        if (HostConfig.success(result)) activity.runOnUiThread(() -> {
                            if (activity.isFinishing() || activity.isDestroyed()) return;
                            logView.setText(diagnosticText(""));
                            logView.setTextColor(MUTED);
                        });
                        return result;
                    }, "诊断日志已清空", null);
                });
                actions.addView(copy, weight());
                actions.addView(clear, weight());
                body.addView(actions, top(activity, 10));

                Button close = button(activity, "关闭", Color.rgb(255, 232, 242));
                close.setTextColor(PINK);
                close.setOnClickListener(v -> dialog.dismiss());
                body.addView(close, top(activity, 8));
                showCompact(dialog, body, activity);
            });
        });
    }

    private static String diagnosticText(String log) {
        if (log == null || log.trim().isEmpty()) {
            return "暂无异常日志。\n\n请先复现长按菜单、头像、气泡 ID 或模式不生效的问题，再回到这里查看。";
        }
        return log.trim();
    }

    private static String moduleHealthText(Activity activity, Bundle health) {
        String qqVersion = installedVersion(activity, "com.tencent.mobileqq");
        String fpaVersion = installedVersion(activity, "fun.fpa");
        boolean channel = health != null && health.getBoolean("_healthOk", false);
        boolean denied = health != null && health.getBoolean("_authDenied", false);
        long hookPing = health == null ? 0L : health.getLong("lastHookPing", 0L);
        String hookVersion = health == null ? "" : health.getString("lastHookVersion", "");
        boolean recentHeartbeat = hookPing > 0L
                && System.currentTimeMillis() - hookPing < 10 * 60_000L;
        boolean hookAlive = HookStatus.active();
        boolean random = health != null && health.getBoolean("randomEnabled", false);
        boolean locked = health != null && health.getBoolean("lockedEnabled", false);
        int lockedId = health == null ? 0 : health.getInt("lockedId", 0);
        int[] pool = health == null ? null : health.getIntArray("pool");
        boolean modeValid = !(random && locked) && (!locked || lockedId > 0)
                && (!random || pool != null && pool.length > 0);
        boolean fallback = health != null && health.getBoolean("_configFallback", false);
        String configChannel = health == null ? "" : health.getString("_configChannel", "");
        String providerError = health == null ? "" : health.getString("_providerError", "");
        boolean antiEnabled = health != null && health.getBoolean("antiRevokeEnabled", false);
        boolean featuresReady = HookStatus.requiredReady(antiEnabled);
        boolean qqSupported = !"未检测到".equals(qqVersion) && AppConfig.compareVersion(qqVersion, AppConfig.MIN_QQ_VERSION) >= 0;
        boolean allOk = channel && hookAlive && featuresReady && modeValid && qqSupported;
        StringBuilder report = new StringBuilder();
        report.append(allOk ? "✓ 静态自检通过（功能触发情况见下方）" : "! 模块自检发现异常或待确认项").append('\n');
        report.append("QQ：").append(qqVersion).append("（最低 9.2.75）\n");
        report.append("Android：").append(Build.VERSION.RELEASE).append(" / API ")
                .append(Build.VERSION.SDK_INT).append('\n');
        report.append("FPA 安装信息：").append(fpaVersion).append("（不代表当前注入框架）\n");
        report.append("QQ 内设置入口：正常\n");
        report.append("配置读取与写入：").append(channel
                ? fallback ? "正常（QQ 本地兼容通道）" : "正常"
                : denied ? "权限被拒绝" : "失败").append('\n');
        if (channel && !configChannel.isEmpty()) {
            report.append("配置通道：").append(configChannel).append('\n');
        }
        if (channel && fallback && !providerError.isEmpty()) {
            report.append("Provider 状态：暂不可用，使用本地通道（")
                    .append(providerError).append("）\n");
        }
        report.append("Hook 注入：").append(hookAlive ? "当前进程已初始化" : "未记录初始化完成").append('\n');
        report.append(HookStatus.describe());
        report.append("Hook 心跳：").append(recentHeartbeat ? "已记录" : "记录待同步");
        if (!hookVersion.isEmpty()) report.append("（QQ ").append(hookVersion).append("）");
        report.append('\n');
        report.append("模式配置：").append(modeValid ? "正常" : "冲突或缺少 ID").append('\n');
        report.append("气泡池：").append(pool == null ? 0 : pool.length).append(" 个 ID\n");
        report.append("防撤回：").append(health != null && health.getBoolean("antiRevokeEnabled", false)
                ? "已开启" : "已关闭");
        if (!allOk) {
            report.append("\n错误代码：");
            if (!channel) report.append(denied ? " CFG-AUTH-001" : " CFG-IO-001");
            if (!modeValid) report.append(" MODE-STATE-001");
            if (!hookAlive || !featuresReady) report.append(" HOOK-PARTIAL-001");
            if (!qqSupported) report.append(" QQ-VERSION-001");
        } else if (!recentHeartbeat) {
            report.append("\n状态代码：HOOK-HB-DEFERRED（后台心跳待同步）");
        }
        if (!channel && health != null) {
            report.append("\n存储错误：").append(health.getString("_error", "探针写入或读取未通过"));
            report.append("\n调用 UID：").append(health.getInt("_callingUid", -1));
            report.append("\n调用包：").append(health.getString("_callerPackages", "未知"));
        }
        return report.toString();
    }

    private static String installedVersion(Context context, String packageName) {
        try {
            android.content.pm.PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            return info.versionName == null || info.versionName.trim().isEmpty()
                    ? "已安装" : info.versionName;
        } catch (Throwable ignored) {
            return "未检测到";
        }
    }

    private static String freshLogText(TextView logView) {
        if (logView == null || logView.getText() == null) return "";
        String value = logView.getText().toString().trim();
        return value.startsWith("暂无异常日志。") ? "" : value;
    }

    private static LinearLayout compactBody(Activity activity, String title, String subtitle) {
        LinearLayout body = column(activity);
        body.setPadding(dp(activity, 22), dp(activity, 24), dp(activity, 22), dp(activity, 22));
        body.setBackground(liquidBackground(activity, 28));
        TextView titleView = text(activity, title, 22, TEXT, true);
        titleView.setGravity(Gravity.CENTER);
        body.addView(titleView, match());
        TextView subtitleView = text(activity, subtitle, 14, MUTED, false);
        subtitleView.setGravity(Gravity.CENTER);
        body.addView(subtitleView, top(activity, 7));
        return body;
    }

    private static void showCompact(Dialog dialog, LinearLayout body, Activity activity) {
        dialog.setContentView(body);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.86f);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.dimAmount = 0.50f;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        enableWindowGlass(window, activity);
    }

    private static void showTall(Dialog dialog, View content, Activity activity) {
        dialog.setContentView(content);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.88f);
        lp.height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.72f);
        lp.dimAmount = 0.52f;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        enableWindowGlass(window, activity);
    }

    private static EditText editor(Activity activity, String value, String hint) {
        EditText input = new EditText(activity);
        input.setText(value);
        input.setHint(hint);
        input.setTextSize(16);
        input.setTextColor(TEXT);
        input.setHintTextColor(Color.rgb(180, 168, 176));
        input.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        input.setMinHeight(dp(activity, 52));
        input.setSingleLine(true);
        input.setBackground(glass(activity, Color.argb(174, 255, 242, 248), 16));
        return input;
    }

    private static EditText multiEditor(Activity activity, String hint) {
        EditText input = editor(activity, "", hint);
        input.setSingleLine(false);
        input.setGravity(Gravity.START | Gravity.TOP);
        input.setMinLines(7);
        input.setMaxLines(12);
        input.setHorizontallyScrolling(false);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setTypeface(Typeface.MONOSPACE);
        input.setTextSize(13);
        return input;
    }

    private static ImageView authorAvatar(Activity activity) {
        ImageView avatar = new ImageView(activity);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setPadding(dp(activity, 2), dp(activity, 2), dp(activity, 2), dp(activity, 2));
        avatar.setBackground(gradient(activity, Color.rgb(255, 217, 236), Color.rgb(228, 210, 255), 22));
        avatar.setClipToOutline(true);
        Drawable drawable = ModuleIcon.load(activity);
        if (drawable != null) avatar.setImageDrawable(drawable);
        else avatar.setImageResource(android.R.drawable.sym_def_app_icon);
        return avatar;
    }

    private static Button button(Activity activity, String label, int color) {
        Button button = new Button(activity);
        button.setText(label);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setMinHeight(dp(activity, 50));
        button.setBackground(ripple(activity, liquidButton(activity, color, 18)));
        button.setElevation(dp(activity, 3));
        pressBounce(button);
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

    private interface ModeListener { void onChanged(boolean enabled); }

    private static final class ModeSwitch {
        private static final int ACTIVE = Color.rgb(220, 55, 124);
        final Activity activity;
        final LinearLayout view;
        final FrameLayout track;
        final View thumb;
        private boolean checked;
        private ModeListener listener;

        ModeSwitch(Activity activity, String title, String subtitle, boolean checked) {
            this.activity = activity;
            view = new LinearLayout(activity);
            view.setOrientation(LinearLayout.HORIZONTAL);
            view.setGravity(Gravity.CENTER_VERTICAL);
            view.setPadding(dp(activity, 14), dp(activity, 13), dp(activity, 14), dp(activity, 13));
            view.setBackground(ripple(activity,
                    glass(activity, Color.argb(150, 255, 247, 251), 18)));

            LinearLayout labels = column(activity);
            labels.addView(text(activity, title, 16, TEXT, true), match());
            labels.addView(text(activity, subtitle, 12, MUTED, false), top(activity, 2));
            view.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            track = new FrameLayout(activity);
            thumb = new View(activity);
            thumb.setBackground(round(activity, Color.WHITE, 13));
            FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(dp(activity, 24), dp(activity, 24));
            thumbParams.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
            thumbParams.leftMargin = dp(activity, 3);
            track.addView(thumb, thumbParams);
            view.addView(track, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 30)));
            setChecked(checked);
            view.setClickable(true);
            view.setFocusable(true);
            view.setOnClickListener(v -> {
                setChecked(!this.checked);
                if (listener != null) listener.onChanged(this.checked);
            });
            track.setClickable(true);
            track.setOnClickListener(v -> view.performClick());
        }

        void setListener(ModeListener listener) { this.listener = listener; }
        boolean isChecked() { return checked; }

        void setChecked(boolean checked) {
            this.checked = checked;
            view.setSelected(checked);
            track.setBackground(glass(activity,
                    checked ? Color.argb(230, 220, 55, 124)
                            : Color.argb(155, 200, 194, 199), 15));
            float target = checked ? dp(activity, 22) : 0f;
            if (thumb.isLaidOut()) {
                thumb.animate().translationX(target).setDuration(240)
                        .setInterpolator(new OvershootInterpolator(0.8f)).start();
            } else {
                thumb.setTranslationX(target);
            }
        }
    }

    private static List<Integer> poolList(int[] pool) {
        List<Integer> result = new ArrayList<>();
        if (pool == null) return result;
        for (int id : pool) if (id >= 1000 && !result.contains(id)) result.add(id);
        return result;
    }

    private static int[] poolArray(List<Integer> ids) {
        int[] result = new int[ids == null ? 0 : ids.size()];
        for (int i = 0; i < result.length; i++) result[i] = ids.get(i);
        return result;
    }

    private static void persistModes(Activity activity, List<Integer> ids, ModeSwitch random,
                                     ModeSwitch independent, EditText fixedEditor) {
        int fixedId = parseBubbleId(fixedEditor.getText().toString());
        boolean randomEnabled = random.isChecked();
        boolean independentEnabled = independent.isChecked();
        int[] pool = poolArray(ids);
        boolean masterEnabled = randomEnabled || independentEnabled;
        HostConfig.stageSettings(masterEnabled, randomEnabled, independentEnabled, fixedId, pool);
        ConfigUi.write(activity, () -> HostConfig.saveSettings(activity, masterEnabled, randomEnabled,
                independentEnabled, fixedId, pool), independentEnabled ? "独立模式已生效"
                : randomEnabled ? "随机模式已生效" : "气泡模式已关闭",
                state -> restoreControls(state, ids, random, independent, fixedEditor, null));
    }

    private static void restoreControls(Bundle state, List<Integer> ids, ModeSwitch random,
                                         ModeSwitch independent, EditText fixedEditor, Runnable onChanged) {
        ids.clear();
        ids.addAll(poolList(state.getIntArray("pool")));
        random.setChecked(state.getBoolean("randomEnabled", false));
        independent.setChecked(state.getBoolean("lockedEnabled", false));
        int id = state.getInt("lockedId", 0);
        fixedEditor.setText(id >= 1000 ? String.valueOf(id) : "");
        if (onChanged != null) onChanged.run();
    }

    private static int parseBubbleId(String input) {
        try {
            String text = input == null ? "" : input.trim();
            if (!text.matches("[0-9]{4,10}")) return 0;
            int value = Integer.parseInt(text);
            return value >= 1000 ? value : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static void toast(Activity activity, String text) { Toast.makeText(activity, text, Toast.LENGTH_SHORT).show(); }
    private static LinearLayout column(Activity activity) { LinearLayout view = new LinearLayout(activity); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private static LinearLayout row(Activity activity, int gap) { LinearLayout view = new LinearLayout(activity); view.setOrientation(LinearLayout.HORIZONTAL); GradientDrawable d = new GradientDrawable(); d.setColor(Color.TRANSPARENT); d.setSize(dp(activity, gap), 1); view.setDividerDrawable(d); view.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE); return view; }
    private static GradientDrawable round(Activity activity, int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(activity, radius)); return d; }
    private static GradientDrawable glass(Activity activity, int color, int radius) {
        int alpha = Color.alpha(color);
        if (alpha == 255) alpha = 178;
        int top = Color.argb(Math.min(220, alpha + 28), 255, 255, 255);
        int middle = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        int bottom = Color.argb(Math.max(105, alpha - 24), 255, 222, 239);
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{top, middle, bottom});
        d.setCornerRadius(dp(activity, radius));
        d.setStroke(dp(activity, 1), Color.argb(220, 255, 255, 255));
        return d;
    }
    private static GradientDrawable gradient(Activity activity, int start, int end, int radius) { GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end}); d.setCornerRadius(dp(activity, radius)); return d; }
    private static GradientDrawable liquidBackground(Activity activity, int radius) { GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(255, 238, 248), Color.rgb(255, 207, 231), Color.rgb(234, 216, 255), Color.rgb(255, 244, 250)}); d.setCornerRadius(dp(activity, radius)); return d; }
    private static GradientDrawable liquidButton(Activity activity, int color, int radius) {
        int alpha = Math.min(232, Color.alpha(color) == 255 ? 232 : Color.alpha(color));
        int base = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        int highlight = withAlpha(mix(color, Color.WHITE, 0.20f), Math.min(245, alpha + 10));
        int shadow = withAlpha(mix(color, PINK_DARK, 0.22f), alpha);
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{highlight, base, shadow});
        d.setCornerRadius(dp(activity, radius));
        d.setStroke(dp(activity, 1), Color.argb(185, 255, 255, 255));
        return d;
    }
    private static int withAlpha(int color, int alpha) { return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)); }
    private static int mix(int from, int to, float amount) { float keep = 1f - amount; return Color.rgb(Math.round(Color.red(from) * keep + Color.red(to) * amount), Math.round(Color.green(from) * keep + Color.green(to) * amount), Math.round(Color.blue(from) * keep + Color.blue(to) * amount)); }
    private static void addLiquidOrbs(Activity activity, FrameLayout surface) {
        addOrb(activity, surface, dp(activity, 210), Color.argb(85, 255, 90, 168), Gravity.TOP | Gravity.RIGHT, -54, -42);
        addOrb(activity, surface, dp(activity, 170), Color.argb(72, 158, 113, 255), Gravity.CENTER_VERTICAL | Gravity.LEFT, -68, 0);
        addOrb(activity, surface, dp(activity, 230), Color.argb(68, 255, 116, 190), Gravity.BOTTOM | Gravity.RIGHT, -74, -48);
    }
    private static void addOrb(Activity activity, FrameLayout surface, int size, int color, int gravity, int marginX, int marginY) {
        View orb = new View(activity);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        shape.setStroke(dp(activity, 1), Color.argb(105, 255, 255, 255));
        orb.setBackground(shape);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GlassApi31.blur(orb, dp(activity, 30));
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size, gravity);
        params.leftMargin = dp(activity, marginX);
        params.rightMargin = dp(activity, marginX);
        params.topMargin = dp(activity, marginY);
        params.bottomMargin = dp(activity, marginY);
        surface.addView(orb, params);
    }
    private static void enableWindowGlass(Window window, Activity activity) { if (window == null) return; try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) window.setBackgroundBlurRadius(dp(activity, 34)); } catch (Throwable ignored) {} }
    @android.annotation.TargetApi(31)
    private static final class GlassApi31 {
        private static void blur(View view, int radius) {
            try {
                view.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                        radius, radius, android.graphics.Shader.TileMode.CLAMP));
            } catch (Throwable ignored) {
            }
        }
    }
    private static RippleDrawable ripple(Activity activity, GradientDrawable content) { return new RippleDrawable(ColorStateList.valueOf(Color.argb(25, 74, 58, 68)), content, null); }
    private static void pressBounce(View view) { view.setOnTouchListener((target, event) -> { if (event.getAction() == MotionEvent.ACTION_DOWN) target.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start(); else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) target.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(new OvershootInterpolator(1.15f)).start(); return false; }); }
    private static void reveal(Activity activity, View view, long delay) { view.setAlpha(0f); view.setTranslationY(dp(activity, 12)); view.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(280).setInterpolator(new DecelerateInterpolator(1.35f)).start(); }
    private static int dp(Activity activity, int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private static LinearLayout.LayoutParams match() { return new LinearLayout.LayoutParams(-1, -2); }
    private static LinearLayout.LayoutParams top(Activity activity, int top) { LinearLayout.LayoutParams p = match(); p.topMargin = dp(activity, top); return p; }
    private static LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); }
}
