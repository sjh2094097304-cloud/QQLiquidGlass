package io.github.liuran001.mmliquidglass;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.Map;
import java.util.WeakHashMap;
import static io.github.liuran001.mmliquidglass.DockOptions.Key.*;

/** Explicitly themed, self-contained settings sheet. Never changes QQ's theme. */
final class QqSettingsPanel {
    private static final Map<Activity,Dialog> open=new WeakHashMap<>();
    private final Activity activity;
    private final Context context;
    private final boolean dark;
    private final int bg,card,text,muted,accent=0xff609aff;
    private DockOptions draft;
    private final Dialog dialog;
    private final LinearLayout root,body,pages;
    private final DockPreview preview;
    private DockColorPicker customColorPicker;
    private boolean keyboardVisible;
    private final ScrollView scroll;
    private int page;
    private final Map<DockOptions.Key,SeekBar> sliders=new java.util.EnumMap<>(DockOptions.Key.class);
    private final Map<DockOptions.Key,LinearLayout> choiceRows=new java.util.EnumMap<>(DockOptions.Key.class);
    private final int[] scrollPositions=new int[3];
    private TextView diagnosticOutput;

    static void show(Activity a) {
        Dialog existing=open.get(a);
        if(existing!=null && existing.isShowing()) return;
        new QqSettingsPanel(a).display();
    }
    static void close(Activity a) { Dialog d=open.remove(a);if(d!=null && d.isShowing())d.dismiss(); }
    private QqSettingsPanel(Activity a) {
        activity=a; GlassConfig.load(a); draft=new DockOptions(GlassConfig.options);
        dark=QqSplitDock.isDark() || (a.getResources().getConfiguration().uiMode&0x30)==0x20;
        context=new ContextThemeWrapper(a,dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);
        bg=dark?0xff111722:0xfff3f6fc; card=dark?0xff1c2533:0xffffffff;
        text=dark?0xfff2f5fa:0xff162338; muted=dark?0xff99a8bd:0xff6c7e96;
        dialog=new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if(dialog.getWindow()!=null)dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        root=column(); root.setBackground(shape(bg,26)); root.setPadding(dp(16),dp(14),dp(16),dp(10));
        LinearLayout header=row(); header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heading=column();
        heading.addView(label("液态玻璃",25,text,true));
        heading.addView(label("QQ 悬浮底栏 · "+FeedbackLog.VERSION,11,muted,false));
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(button("关闭",()->dialog.dismiss(),false)); root.addView(header);
        preview=new DockPreview(context,draft,dark,QqSplitDock.previewAvatar(),QqSplitDock.previewTitles(),QqSplitDock.previewThemeIcons());
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2); pp.topMargin=dp(10); root.addView(preview,pp);
        if(android.os.Build.VERSION.SDK_INT>=30) root.setOnApplyWindowInsetsListener((v,insets)->{
            keyboardVisible=insets.isVisible(android.view.WindowInsets.Type.ime());
            preview.setVisibility(page==2 || keyboardVisible?View.GONE:View.VISIBLE);
            return insets;
        });
        pages=row(); pages.setPadding(0,dp(6),0,dp(8)); root.addView(pages);
        scroll=new ScrollView(context); scroll.setFillViewport(false); scroll.setClipToPadding(true); scroll.setClipChildren(true); scroll.setVerticalScrollBarEnabled(false);
        body=column(); scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=row(); footer.setPadding(0,dp(10),0,0);
        TextView reset=button("恢复默认",()->confirmReset(),false);
        footer.addView(reset,new LinearLayout.LayoutParams(0,dp(46),1));
        TextView save=button("保存并应用",()->{
            GlassConfig.save(activity,draft); LiquidGlassInstaller.applyPreferences();
            dialog.dismiss(); android.widget.Toast.makeText(activity,"已保存，返回 QQ 首页应用",0).show();
        },true);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(46),2); sp.leftMargin=dp(10); footer.addView(save,sp);
        root.addView(footer); dialog.setContentView(root);
        dialog.setOnDismissListener(d->{preview.dispose();open.remove(activity);});
        select(0);
    }
    private void display() {
        if(activity.isFinishing() || activity.isDestroyed()) return;
        open.put(activity,dialog); dialog.show();
        Window w=dialog.getWindow();
        if(w!=null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams p=w.getAttributes(); p.dimAmount=.5f; p.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;
            android.util.DisplayMetrics metrics=activity.getResources().getDisplayMetrics();
            p.width=Math.min(metrics.widthPixels-dp(16),dp(560));
            p.height=(int)(metrics.heightPixels*.89f); w.setAttributes(p);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        FeedbackLog.event("SETTINGS_OPEN","themed sheet");
    }
    private void select(int selected) {
        scrollPositions[page]=scroll.getScrollY();
        page=selected; pages.removeAllViews();
        preview.setVisibility(page==2 || keyboardVisible?View.GONE:View.VISIBLE);
        String[] names={"外观","布局","反馈"};
        for(int i=0;i<names.length;i++) {
            final int index=i; TextView t=button(names[i],()->select(index),i==page);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(38),1); if(i>0)p.leftMargin=dp(8); pages.addView(t,p);
        }
        body.removeAllViews(); sliders.clear(); choiceRows.clear(); diagnosticOutput=null; customColorPicker=null;
        if(page==0) appearance(); else if(page==1) layout(); else feedback();
        final int position=scrollPositions[page];
        scroll.post(()->scroll.scrollTo(0,position));
    }
    private void appearance() {
        LinearLayout c=section("显示内容","默认使用模块线条图标；开启“底栏图标跟随 QQ 主题”后，直接读取当前主题/个性装扮的底栏图标。仅在“仅图标”或“图标＋文字”模式生效。");
        choices(c,MODE,new String[]{"仅文字","仅图标","图标＋文字"});
        toggle(c,THEME_ICONS); toggle(c,AVATAR); toggle(c,BADGES); toggle(c,BOLD);
        slider(c,TEXT); slider(c,ICON); slider(c,INACTIVE_ALPHA);
        c=section("液态玻璃","实时模糊与边缘折射。原版预设使用 4dp 模糊、24dp 折射与 1.5 倍饱和度。");
        LinearLayout presets=row();
        String[] names={"通透","原版","实体"};
        for(int i=0;i<3;i++) { final int index=i; TextView b=button(names[i],()->{draft.preset(index); refreshControls();},false);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(38),1); if(i>0)lp.leftMargin=dp(6);presets.addView(b,lp); }
        c.addView(presets);
        slider(c,BLUR); slider(c,REFRACTION); slider(c,SATURATION);
        slider(c,OPACITY); choices(c,TINT,new String[]{"中性","冰蓝","淡紫","暖色"});
        slider(c,LIGHT); slider(c,BORDER); slider(c,CORNER); slider(c,SHADOW);
        c=section("选中效果",null);
        choices(c,ACCENT,new String[]{"蓝","紫","绿","橙","自定义"});
        customColorPicker=new DockColorPicker(context,draft.get(CUSTOM_ACCENT),dark,rgb->{
            draft.set(CUSTOM_ACCENT,rgb);draft.set(ACCENT,4);updatePreview();
        });
        c.addView(customColorPicker,new LinearLayout.LayoutParams(-1,-2));updateColorVisibility();
        toggle(c,TINT_SELECTION);slider(c,HIGHLIGHT);slider(c,PRESS_STRENGTH);slider(c,ANIMATION);
    }
    private void layout() {
        LinearLayout c=section("启用与入口","左侧按压与拖动使用原版液滴交互，长按交由 QQ 处理。长按右侧头像或从 QQ 原生设置进入本页。");
        toggle(c,ENABLED); toggle(c,SPLIT); toggle(c,HIDE_NATIVE);
        c=section("尺寸与位置","高度是栏体厚度；悬浮距离是距离底部安全区域的间隔。过大尺寸会自动限制在屏幕内。");
        slider(c,HEIGHT); slider(c,WIDTH); slider(c,SCALE); slider(c,OFFSET); slider(c,SHIFT);
        c=section("独立头像","只读取原生头像图片，不再截取屏幕区域。无法识别时保留有效缓存或显示占位。");
        toggle(c,AVATAR); slider(c,AVATAR_SIZE); slider(c,AVATAR_INSET); slider(c,GAP);
    }
    private void feedback() {
        LinearLayout c=section("日志与隐私","仅记录模块状态、控件类名、配置及异常类型。无 QQ 号、昵称、聊天内容或截图；不会自动上传。");
        toggle(c,LOGGING);
        TextView note=label("关闭记录后不再新增事件。已有日志可手动清空；进程重启后日志不保留。导出的是已保存的配置。",12,muted,false); note.setPadding(0,dp(8),0,dp(8)); c.addView(note);
        LinearLayout actions=column();
        TextView copy=button("复制日志",()->FeedbackExport.copy(activity),true);
        addAction(actions,copy);
        TextView export=button("导出 .txt",()->FeedbackExport.export(activity),false);
        addAction(actions,export);
        addAction(actions,button("清空本次日志",()->{FeedbackLog.clear(); refreshDiagnostic();},false));
        c.addView(actions,new LinearLayout.LayoutParams(-1,-2));
        c=section("当前诊断","点击刷新可重新读取状态；成功发送原生点击不等于已确认侧边栏打开。");
        addAction(c,button("刷新诊断",this::refreshDiagnostic,false));
        diagnosticOutput=label(FeedbackExport.report(activity),11,muted,false);
        diagnosticOutput.setTypeface(Typeface.MONOSPACE);
        diagnosticOutput.setLineSpacing(dp(3),1f);
        diagnosticOutput.setPadding(dp(10),dp(12),dp(10),dp(12));
        diagnosticOutput.setBackground(shape(bg,10));
        LinearLayout.LayoutParams outputParams=new LinearLayout.LayoutParams(-1,-2); outputParams.topMargin=dp(12);
        c.addView(diagnosticOutput,outputParams);
    }
    private void addAction(LinearLayout parent,TextView button) {
        button.setMinHeight(dp(48));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(8);
        parent.addView(button,p);
    }
    private void refreshDiagnostic() { if(diagnosticOutput!=null) diagnosticOutput.setText(FeedbackExport.report(activity)); }
    private void refreshControls() {
        // Presets update existing controls. Replacing the body loses focus and scroll.
        for(Map.Entry<DockOptions.Key,SeekBar> e:sliders.entrySet()) e.getValue().setProgress(draft.get(e.getKey())-e.getKey().min);
        for(Map.Entry<DockOptions.Key,LinearLayout> e:choiceRows.entrySet())
            for(int i=0;i<e.getValue().getChildCount();i++) styleButton((TextView)e.getValue().getChildAt(i),draft.get(e.getKey())==i);
        updatePreview();
    }
    private void confirmReset() {
        new android.app.AlertDialog.Builder(context).setTitle("恢复默认外观？")
                .setMessage("仅重置当前草稿，点击“保存并应用”后生效。")
                .setNegativeButton("取消",null).setPositiveButton("恢复",(d,w)->{draft=new DockOptions();select(page);updatePreview();}).show();
    }
    private LinearLayout section(String title,String description) {
        LinearLayout c=column(); c.setPadding(dp(14),dp(14),dp(14),dp(14)); c.setBackground(shape(card,18));
        c.addView(label(title,16,text,true));
        if(description!=null) { TextView hint=label(description,12,muted,false);hint.setPadding(0,dp(5),0,dp(10));c.addView(hint); }
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);body.addView(c,p);return c;
    }
    private void slider(LinearLayout parent,DockOptions.Key key) {
        LinearLayout head=row(); head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(0,dp(13),0,0);
        head.addView(label(key.label,13,text,false),new LinearLayout.LayoutParams(0,-2,1));
        TextView value=label(draft.get(key)+key.unit,12,accent,true);head.addView(value);parent.addView(head);
        SeekBar seek=new SeekBar(context); seek.setMax(key.max-key.min);seek.setProgress(draft.get(key)-key.min);
        sliders.put(key,seek);
        seek.setProgressTintList(ColorStateList.valueOf(accent));seek.setProgressBackgroundTintList(ColorStateList.valueOf(dark?0xff3a4659:0xffdbe3ef));
        seek.setThumbTintList(ColorStateList.valueOf(accent)); seek.setSplitTrack(false);
        seek.setContentDescription(key.label);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean user){draft.set(key,p+key.min);value.setText(draft.get(key)+key.unit);updatePreview();}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        });
        parent.addView(seek,new LinearLayout.LayoutParams(-1,dp(42)));
    }
    private void toggle(LinearLayout parent,DockOptions.Key key) {
        LinearLayout line=row();line.setGravity(Gravity.CENTER_VERTICAL);line.setMinimumHeight(dp(52));
        TextView label=label(key.label,14,text,false);line.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Toggle toggle=new Toggle(context,key);line.addView(toggle,new LinearLayout.LayoutParams(dp(46),dp(30)));
        line.setBackground(new RippleDrawable(ColorStateList.valueOf(0x226099ff),null,shape(Color.WHITE,8)));
        line.setOnClickListener(v->toggle.performClick());parent.addView(line);
    }
    private void choices(LinearLayout parent,DockOptions.Key key,String[] choices) {
        TextView caption=label(key.label,13,muted,false);caption.setPadding(0,dp(10),0,dp(8));parent.addView(caption);
        LinearLayout options=row();parent.addView(options);choiceRows.put(key,options);
        for(int i=0;i<choices.length;i++) {
            final int index=i;TextView b=button(choices[i],()->{
                draft.set(key,index);
                if(key==ACCENT)updateColorVisibility();
                for(int j=0;j<options.getChildCount();j++) styleButton((TextView)options.getChildAt(j),j==index);
                updatePreview();
            },draft.get(key)==i);
            b.setTextSize(12); b.setPadding(dp(2),0,dp(2),0);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(38),1);if(i>0)p.leftMargin=dp(5);options.addView(b,p);
        }
    }
    private LinearLayout column(){LinearLayout v=new LinearLayout(context);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(context);v.setOrientation(LinearLayout.HORIZONTAL);return v;}
    private TextView label(String s,int size,int color,boolean bold){TextView v=new TextView(context);v.setText(s);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT_BOLD);return v;}
    private TextView button(String s,Runnable action,boolean selected){TextView v=label(s,13,text,true);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(40));v.setPadding(dp(12),dp(6),dp(12),dp(6));v.setFocusable(true);v.setOnClickListener(w->action.run());styleButton(v,selected);return v;}
    private void styleButton(TextView v,boolean selected){v.setTextColor(selected?Color.WHITE:text);v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x336099ff),shape(selected?accent:dark?0xff2a374a:0xffe6edf8,12),null));v.setSelected(selected);}
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private int dp(float value){return Math.round(value*context.getResources().getDisplayMetrics().density);}

    private final class Toggle extends View {
        final DockOptions.Key key;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Toggle(Context c,DockOptions.Key key){super(c);this.key=key;setFocusable(true);setClickable(true);describe();setOnClickListener(v->{draft.set(key,draft.on(key)?0:1);describe();invalidate();updatePreview();});}
        void describe(){setContentDescription(key.label+(draft.on(key)?"，已开启":"，已关闭"));}
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Switch");info.setCheckable(true);info.setChecked(draft.on(key));}
        @Override protected void onDraw(Canvas c){float h=getHeight()*.78f,y=(getHeight()-h)/2; p.setColor(draft.on(key)?accent:dark?0xff46546a:0xffb9c5d7);c.drawRoundRect(0,y,getWidth(),y+h,h/2,h/2,p);p.setColor(Color.WHITE);float r=h/2-dp(3),x=draft.on(key)?getWidth()-h/2:h/2;c.drawCircle(x,getHeight()/2f,r,p);}
    }
    private void updatePreview(){preview.update(draft);}
    private void updateColorVisibility(){if(customColorPicker!=null)customColorPicker.setVisibility(draft.get(ACCENT)==4?View.VISIBLE:View.GONE);}
}
