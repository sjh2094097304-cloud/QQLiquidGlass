package io.github.liuran001.mmliquidglass;

import android.content.Context;
import android.content.SharedPreferences;

/** Host-private preferences; QQ changes never alter WeChat's preferences. */
final class GlassConfig {

    /** Named before QQ was a target; kept so existing WeChat setups still read. */
    private static final String PREFS = "wx_liquid_glass_cfg";

    /** Distance between the bottom of the glass pill and the screen edge, dp. */
    static volatile int barOffsetDp = 12;
    static volatile boolean qqEnabled = true;
    static volatile boolean qqSplitDock = true;
    static volatile int qqTextSizeSp = 17;
    static volatile DockOptions options = new DockOptions();

    private GlassConfig() {
    }

    static void load(Context ctx) {
        try {
            SharedPreferences p = ctx.getSharedPreferences(PREFS, 0);
            barOffsetDp = Math.max(0, Math.min(48, p.getInt("barOffsetDp", 12)));
            qqEnabled = p.getBoolean("qqEnabled", true);
            qqSplitDock = p.getBoolean("qqSplitDock", true);
            qqTextSizeSp = Math.max(12, Math.min(22, p.getInt("qqTextSizeSp", 17)));
            if (LiquidGlassModule.app() == HostApp.QQ) {
                DockOptions next=new DockOptions();
                next.set(DockOptions.Key.ENABLED,qqEnabled?1:0);
                next.set(DockOptions.Key.SPLIT,qqSplitDock?1:0);
                next.set(DockOptions.Key.OFFSET,barOffsetDp);
                next.set(DockOptions.Key.TEXT,qqTextSizeSp);
                for(DockOptions.Key k:DockOptions.Key.values())
                    if(p.contains("dock4_"+k.name())) next.set(k,p.getInt("dock4_"+k.name(),k.initial));
                // Map the old solid tint to a surface wash when enabling real optics.
                // Do this only while reading old settings; save writes the new keys.
                if(!p.contains("dock4_BLUR") && p.contains("dock4_OPACITY"))
                    next.set(DockOptions.Key.OPACITY,Math.round(next.get(DockOptions.Key.OPACITY)*40f/86f));
                options=next;
                qqEnabled=next.on(DockOptions.Key.ENABLED);
                qqSplitDock=next.on(DockOptions.Key.SPLIT);
                barOffsetDp=next.get(DockOptions.Key.OFFSET);
                qqTextSizeSp=next.get(DockOptions.Key.TEXT);
                FeedbackLog.enabled=next.on(DockOptions.Key.LOGGING);
            }
        } catch (Throwable t) {
            LiquidGlassModule.logErr("config load failed", t);
        }
    }

    static void save(Context ctx, boolean enabled, boolean split, int offset, int textSize) {
        ctx.getSharedPreferences(PREFS, 0).edit()
                .putBoolean("qqEnabled", enabled).putBoolean("qqSplitDock", split)
                .putInt("barOffsetDp", Math.max(0, Math.min(48, offset)))
                .putInt("qqTextSizeSp", Math.max(12, Math.min(22, textSize)))
                .apply();
        load(ctx);
    }

    static void save(Context ctx,DockOptions draft) {
        SharedPreferences.Editor e=ctx.getSharedPreferences(PREFS,0).edit();
        for(DockOptions.Key k:DockOptions.Key.values()) e.putInt("dock4_"+k.name(),draft.get(k));
        e.apply();
        load(ctx);
        FeedbackLog.event("CONFIG_SAVED","layout/style/display options updated");
    }
}
