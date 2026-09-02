package io.github.liuran001.mmliquidglass;

import java.util.Arrays;

/** Validated, copyable settings. No Android dependency; drafts never change live UI. */
final class DockOptions {
    enum Key {
        ENABLED("启用 QQ 悬浮底栏",0,1,1,""),
        SPLIT("替换原生底栏",0,1,1,""),
        MODE("标签显示",0,2,0,""),
        THEME_ICONS("底栏图标跟随 QQ 主题",0,1,0,""),
        AVATAR("独立账号头像",0,1,1,""),
        BADGES("显示未读数字",0,1,1,""),
        HIDE_NATIVE("隐藏原生底部背景",0,1,1,""),
        HEIGHT("栏体高度",44,88,56,"dp"),
        WIDTH("整组宽度",60,100,90,"%"),
        SCALE("整体缩放",75,125,100,"%"),
        OFFSET("底部悬浮距离",0,100,12,"dp"),
        SHIFT("水平偏移",-40,40,0,"dp"),
        AVATAR_SIZE("头像圆直径",40,80,56,"dp"),
        AVATAR_INSET("头像内边距",0,10,2,"dp"),
        GAP("头像与底栏间距",4,32,16,"dp"),
        TEXT("标签字号",10,24,17,"sp"),
        ICON("图标大小",16,32,22,"dp"),
        BOLD("加粗文字",0,1,1,""),
        OPACITY("玻璃蒙层不透明度",10,100,40,"%"),
        BLUR("背景模糊",0,16,4,"dp"),
        REFRACTION("边缘折射强度",0,40,24,"dp"),
        SATURATION("背景饱和度",50,200,150,"%"),
        TINT("玻璃色调",0,3,0,""),
        LIGHT("按压高光强度",0,100,35,"%"),
        BORDER("边框亮度",0,100,12,"%"),
        CORNER("圆角程度",0,50,50,"%"),
        SHADOW("阴影高度",0,16,5,"dp"),
        HIGHLIGHT("选中背景强度",0,60,16,"%"),
        ACCENT("强调色",0,4,0,""),
        CUSTOM_ACCENT("自定义强调色",0,0xffffff,0x4b93ff,""),
        INACTIVE_ALPHA("未选中标签不透明度",20,100,100,"%"),
        TINT_SELECTION("选中底色跟随强调色",0,1,0,""),
        PRESS_STRENGTH("按压放大强度",0,125,100,"%"),
        ANIMATION("切换动画时长",0,400,180,"ms"),
        LOGGING("记录诊断日志",0,1,1,"");
        final String label, unit;
        final int min,max,initial;
        Key(String label,int min,int max,int initial,String unit) {
            this.label=label; this.min=min; this.max=max; this.initial=initial; this.unit=unit;
        }
    }
    private final int[] values = new int[Key.values().length];
    DockOptions() { for(Key k:Key.values()) values[k.ordinal()]=k.initial; }
    DockOptions(DockOptions other) { System.arraycopy(other.values,0,values,0,values.length); }
    int get(Key k) { return values[k.ordinal()]; }
    boolean on(Key k) { return get(k)!=0; }
    void set(Key k,int value) { values[k.ordinal()]=Math.max(k.min,Math.min(k.max,value)); }
    float scale() { return get(Key.SCALE)/100f; }
    int signature() { return Arrays.hashCode(values); }
    int accent() { return get(Key.ACCENT)==4?0xff000000|get(Key.CUSTOM_ACCENT)
            :new int[]{0xff4b93ff,0xff8d79ed,0xff19ae91,0xffea9b42}[get(Key.ACCENT)]; }
    void preset(int preset) {
        set(Key.OPACITY,preset==0?24:preset==1?40:100);
        set(Key.BLUR,preset==0?2:preset==1?4:0);
        set(Key.REFRACTION,preset==2?0:24);
        set(Key.SATURATION,150);
        set(Key.TINT,0);
        set(Key.LIGHT,preset==0?65:preset==1?35:0);
        set(Key.BORDER,preset==0?20:12);
        set(Key.SHADOW,preset==0?8:preset==1?5:0);
    }
}
