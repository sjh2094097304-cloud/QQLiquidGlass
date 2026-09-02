package io.github.liuran001.mmliquidglass;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/** QQ-only dock. Native layout stays intact; only a sibling content region is sampled. */
final class QqSplitDock {
    static final Object OWNED = new Object();
    static volatile String status = "等待 QQ 首页";
    private static QqSplitDock instance;
    private static long installGeneration;
    private static WeakReference<Activity> lastActivity = new WeakReference<>(null);
    private final Activity activity;
    private final ViewGroup nativeBar;
    private final FrameLayout parent;
    private final Capsule pill;
    private final LinearLayout labels;
    private final FrameLayout avatarHost;
    private final QqAvatarBridge avatarBridge;
    private final QqAvatarBridge.AvatarButton avatar;
    private final QqGlassBackdrop backdrop;
    private final LiquidGlassPanel glass, avatarGlass;
    private final DropletPanel droplet;
    private final DropletDragController drag;
    private final ArrayList<View> nativeTabs = new ArrayList<>();
    private final UiWorkGate work = new UiWorkGate();
    private final Rect emptyClip = new Rect();
    private final Rect oldClip;
    private final int oldAccessibility;
    private final float density;
    private boolean disposed, paused, dark;
    private int styleSignature;
    private final java.util.IdentityHashMap<View,Rect> chromeClips=new java.util.IdentityHashMap<>();
    private ViewTreeObserver observer;

    private final Runnable syncTask = () -> {
        work.complete();
        if (!disposed && !paused) {
            try { sync(); }
            catch (Throwable t) { failOpen("QQ dock sync", t); }
        }
    };
    private final Runnable poll = new Runnable() {
        public void run() {
            if (disposed || paused) return;
            requestSync();
            pill.postDelayed(this, 500);
        }
    };
    private final ViewTreeObserver.OnPreDrawListener preDraw = () -> {
        // No capture or layout here. Only an independently dirty content sibling
        // can invalidate the glass; the glass's own redraw never dirties that sibling.
        if (!disposed && !paused) {
            try {
                followNative();
                refreshBackdrop();
            }
            catch (Throwable t) { requestSync(); }
        }
        return true;
    };
    private final View.OnLayoutChangeListener layoutChanged = (v,l,t,r,b,ol,ot,or,ob) -> requestSync();

    private void refreshBackdrop() {
        if(glass.isSupported() && pill.isShown() && activity.hasWindowFocus() && backdrop.prepare()) {
            glass.invalidate(); avatarGlass.invalidate(); droplet.refresh();
        }
    }

    static void scheduleInstall(Activity a) {
        lastActivity = new WeakReference<>(a);
        long generation = ++installGeneration;
        GlassConfig.load(a);
        if (!GlassConfig.qqEnabled || !GlassConfig.qqSplitDock) {
            if (instance != null) { instance.dispose(); instance = null; }
            return;
        }
        if (instance != null && instance.activity == a && !instance.disposed
                && instance.nativeBar.isAttachedToWindow()) {
            instance.resume();
            return;
        }
        if (instance != null) { instance.dispose(); instance = null; }
        a.getWindow().getDecorView().post(() -> attempt(a, generation, 0));
    }

    private static void attempt(Activity a, long generation, int tries) {
        if (generation != installGeneration || a.isFinishing() || a.isDestroyed()) return;
        if(Build.VERSION.SDK_INT<33) {
            status="此 QQ 定制版液态底栏需要 Android 13 以上，已保留原生底栏";
            return;
        }
        View root = a.getWindow().getDecorView();
        ViewGroup bar = TabBarBridge.findTabView(root);
        ViewGroup row = bar == null ? null : TabBarBridge.findTabRow(bar);
        if (bar == null || !(bar.getParent() instanceof FrameLayout) || row == null
                || TabBarBridge.tabCount(row) < 3 || bar.getWidth() <= 0) {
            if (tries < 24) root.postDelayed(() -> attempt(a, generation, tries + 1), 250);
            else status = "未识别此 QQ 版本的底栏，已保留原生底栏";
            return;
        }
        try {
            instance = new QqSplitDock(a, bar);
            instance.resume();
            status = "QQ 可调节悬浮底栏已接入";
            FeedbackLog.event("DOCK_READY", "native geometry preserved");
        } catch (Throwable t) {
            status = "底栏接入失败，保留原生底栏";
            LiquidGlassModule.logErr(status, t);
        }
    }

    static boolean isDark() { return instance!=null && instance.dark; }
    static android.graphics.Bitmap previewAvatar() { return instance==null?null:instance.avatarBridge.copyForPreview(); }
    static String[] previewTitles() {
        if(instance==null || instance.labels.getChildCount()<3) return new String[]{"消息","联系人","动态"};
        String[] result=new String[instance.labels.getChildCount()];
        for(int i=0;i<result.length;i++) result[i]=((Label)instance.labels.getChildAt(i)).value;
        return result;
    }
    static Bitmap[] previewThemeIcons() {
        if(instance==null || instance.labels.getChildCount()<3) return null;
        Bitmap[] result=new Bitmap[instance.labels.getChildCount()];
        for(int i=0;i<result.length;i++) result[i]=((Label)instance.labels.getChildAt(i)).copyThemeIcon();
        return result;
    }
    static void onTabChanged() { if (instance != null) instance.requestSync(); }
    static void onPause(Activity a) {
        if (lastActivity.get() == a) installGeneration++;
        if (instance != null && instance.activity == a) instance.pause();
    }
    static void onDestroy(Activity a) {
        if (lastActivity.get() == a) { lastActivity.clear(); installGeneration++; }
        if (instance != null && instance.activity == a) { instance.dispose(); instance = null; }
    }
    static void applyPreferences() {
        Activity a = lastActivity.get();
        if (instance != null && instance.paused) {
            if (!GlassConfig.qqEnabled || !GlassConfig.qqSplitDock) { instance.dispose(); instance = null; }
            return; // Do not restart work in a paused home Activity from Settings.
        }
        if (a != null && !a.isDestroyed() && a.hasWindowFocus()) scheduleInstall(a);
    }

    private QqSplitDock(Activity a, ViewGroup bar) {
        activity = a;
        nativeBar = bar;
        parent = (FrameLayout) bar.getParent();
        density = a.getResources().getDisplayMetrics().density;
        oldClip = bar.getClipBounds();
        oldAccessibility = bar.getImportantForAccessibility();
        pill = new Capsule(a);
        pill.setTag(OWNED);
        labels = new LinearLayout(a);
        labels.setOrientation(LinearLayout.HORIZONTAL);
        labels.setPadding(dp(4), 0, dp(4), 0);
        labels.setClipChildren(false);
        avatarHost = new FrameLayout(a) {
            @Override protected void dispatchDraw(Canvas c) { if(!QqGlassBackdrop.isCapturing()) super.dispatchDraw(c); }
        };
        avatarHost.setTag(OWNED);
        avatarHost.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View v,android.graphics.Outline outline) { outline.setOval(0,0,v.getWidth(),v.getHeight()); }
        });
        backdrop=new QqGlassBackdrop(parent,nativeBar,pill,avatarHost,density);
        glass=new LiquidGlassPanel(a,null,density,false);glass.setQqBackdrop(backdrop);
        avatarGlass=new LiquidGlassPanel(a,null,density,false);avatarGlass.setQqBackdrop(backdrop);
        droplet=new DropletPanel(a,null,labels,density,false);droplet.setQqBackdrop(backdrop);droplet.setPill(glass);
        drag=new DropletDragController(droplet,labels,density,false);drag.setPill(glass);drag.setHost(pill);
        for(View v:new View[]{glass,avatarGlass,droplet}) v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams inner=new FrameLayout.LayoutParams(-1,-1);
        inner.setMargins(pill.bleed,pill.bleed,pill.bleed,pill.bleed);
        pill.addView(glass,inner);
        pill.addView(labels,new FrameLayout.LayoutParams(inner));
        droplet.setVisibility(View.INVISIBLE);
        pill.addView(droplet,new FrameLayout.LayoutParams(0,0,Gravity.TOP|Gravity.LEFT));
        avatarHost.addView(avatarGlass,new FrameLayout.LayoutParams(-1,-1));
        avatarBridge = new QqAvatarBridge(a, bar);
        avatar = new QqAvatarBridge.AvatarButton(a, avatarBridge);
        avatarHost.addView(avatar, new FrameLayout.LayoutParams(-1, -1));
        try {
            int at = parent.indexOfChild(bar) + 1;
            parent.addView(pill, at, new FrameLayout.LayoutParams(dp(224), dp(56), Gravity.BOTTOM | Gravity.LEFT));
            parent.addView(avatarHost, at + 1, new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM | Gravity.LEFT));
            parent.addOnLayoutChangeListener(layoutChanged);
            bar.addOnLayoutChangeListener(layoutChanged);
            sync();
        } catch (Throwable t) { dispose(); throw t; }
    }

    private void resume() {
        paused = false;
        backdrop.setPaused(false);
        if (observer == null || !observer.isAlive()) {
            observer = parent.getViewTreeObserver();
            observer.addOnPreDrawListener(preDraw);
        }
        pill.removeCallbacks(poll);
        pill.post(poll);
        requestSync();
        avatarBridge.invalidateAccount();
    }

    private void pause() {
        paused = true;
        pill.removeCallbacks(poll);
        pill.removeCallbacks(syncTask);
        work.cancel();
        avatarBridge.cancel();
        if (observer != null && observer.isAlive()) observer.removeOnPreDrawListener(preDraw);
        observer = null;
        // QQ's outgoing Activity is still visible during a chat transition.
        // Freeze that frame. Restoring native pixels here caused the one-frame flash.
        backdrop.setPaused(true);
        pill.stopAnimation();
    }

    private void dispose() {
        if (disposed) return;
        disposed = true;
        pause();
        nativeBar.setClipBounds(oldClip);
        nativeBar.setImportantForAccessibility(oldAccessibility);
        restoreChrome();
        disposeLabels();
        backdrop.dispose();
        avatarBridge.dispose();
        parent.removeOnLayoutChangeListener(layoutChanged);
        nativeBar.removeOnLayoutChangeListener(layoutChanged);
        if (pill.getParent() == parent) parent.removeView(pill);
        if (avatarHost.getParent() == parent) parent.removeView(avatarHost);
    }

    private void failOpen(String message, Throwable error) {
        LiquidGlassModule.logErr(message, error);
        FeedbackLog.error("DOCK_FAIL_OPEN",error);
        status = "异常后已恢复 QQ 原生底栏";
        dispose();
    }

    private void requestSync() {
        if (!disposed && !paused && work.request()) pill.postDelayed(syncTask, 32);
    }

    private void followNative() {
        WindowInsets insets = nativeBar.getRootWindowInsets();
        boolean keyboard = Build.VERSION.SDK_INT >= 30 && insets != null
                && insets.isVisible(WindowInsets.Type.ime());
        boolean show = !keyboard && nativeBar.isShown() && nativeBar.getAlpha() > .01f
                && nativeBar.getParent() == parent && pill.isAttachedToWindow();
        int visibility = show ? View.VISIBLE : View.INVISIBLE;
        if (pill.getVisibility() != visibility) pill.setVisibility(visibility);
        int avatarVisibility=GlassConfig.options.on(DockOptions.Key.AVATAR)?visibility:View.GONE;
        if (avatarHost.getVisibility() != avatarVisibility) avatarHost.setVisibility(avatarVisibility);
        if (show) {
            // Paint clip only. Never alter QQ's parent, dimensions, padding,
            // alpha, visibility, scroll insets, listeners or gesture dispatch.
            Rect current = nativeBar.getClipBounds();
            if (current == null || !current.isEmpty()) nativeBar.setClipBounds(emptyClip);
            if (nativeBar.getImportantForAccessibility() != View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
                nativeBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
        for(View chrome:chromeClips.keySet()) {
            Rect clip=chrome.getClipBounds();
            if(clip==null || !clip.isEmpty()) chrome.setClipBounds(emptyClip);
        }
        mirror(pill);
        mirror(avatarHost);
    }

    private void mirror(View owned) {
        if (owned.getAlpha() != nativeBar.getAlpha()) owned.setAlpha(nativeBar.getAlpha());
        if (owned.getTranslationY() != nativeBar.getTranslationY()) owned.setTranslationY(nativeBar.getTranslationY());
        if (owned.getTranslationX() != nativeBar.getTranslationX()) owned.setTranslationX(nativeBar.getTranslationX());
    }

    private void sync() {
        if (activity.isDestroyed() || nativeBar.getParent() != parent || !nativeBar.isAttachedToWindow()) {
            dispose();
            if (!activity.isDestroyed() && !activity.isFinishing() && activity.hasWindowFocus()) {
                activity.getWindow().getDecorView().post(() -> scheduleInstall(activity));
            }
            return;
        }
        ViewGroup row = TabBarBridge.findTabRow(nativeBar);
        int n = TabBarBridge.tabCount(row);
        if (n < 3 || n > 5) return;
        boolean changed = nativeTabs.size() != n;
        for (int i = 0; !changed && i < n; i++) changed = nativeTabs.get(i) != TabBarBridge.tabAt(row, i);
        if (changed) {
            drag.stop();
            nativeTabs.clear();
            disposeLabels();
            labels.removeAllViews();
            for (int i = 0; i < n; i++) {
                View target = TabBarBridge.tabAt(row, i);
                nativeTabs.add(target);
                labels.addView(new Label(target, DockGeometry.fallbackTitle(i, n)), new LinearLayout.LayoutParams(0, -1, 1));
            }
            drag.setTabRow(labels);droplet.setTabRow(labels);
        }
        Boolean detected = LiquidGlassHostLayout.detectDarkFromText(nativeBar);
        boolean night = detected != null ? detected : (activity.getResources().getConfiguration().uiMode & 0x30) == 0x20;
        DockOptions o=GlassConfig.options;
        if (styleSignature==0 || night != dark || styleSignature!=o.signature()) {
            dark = night;
            styleSignature=o.signature();
            glass.configureQq(o,dark);droplet.configureQq(o,dark);
            DockOptions circle=new DockOptions(o);circle.set(DockOptions.Key.CORNER,50);
            avatarGlass.configureQq(circle,dark);
            backdrop.setTheme(dark);backdrop.changed();
            drag.setAnimationDuration(o.get(DockOptions.Key.ANIMATION));
            drag.setPressStrength(o.get(DockOptions.Key.PRESS_STRENGTH));
            pill.setElevation(dp(o.get(DockOptions.Key.SHADOW)));
            avatarHost.setElevation(dp(o.get(DockOptions.Key.SHADOW)));
            pill.invalidate(); avatar.invalidate();
            for(int i=0;i<labels.getChildCount();i++) labels.getChildAt(i).invalidate();
        }
        updateChrome(o.on(DockOptions.Key.HIDE_NATIVE));
        backdrop.bindSource();
        for (int i = 0; i < labels.getChildCount(); i++) ((Label) labels.getChildAt(i)).sync();
        DockGeometry geometry = new DockGeometry(Math.max(1, parent.getWidth()), density, n, o);
        WindowInsets insets = nativeBar.getRootWindowInsets();
        int nav = insets == null ? 0 : Build.VERSION.SDK_INT >= 30
                ? insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom : insets.getSystemWindowInsetBottom();
        int[] parentPos = new int[2], decorPos = new int[2];
        parent.getLocationOnScreen(parentPos);
        View decor = activity.getWindow().getDecorView();
        decor.getLocationOnScreen(decorPos);
        int below = Math.max(0, decorPos[1] + decor.getHeight() - parentPos[1] - parent.getHeight());
        int height=dp(o.get(DockOptions.Key.HEIGHT)*o.scale());
        int commonHeight=Math.max(height,geometry.avatarSize);
        int bottom = Math.min(Math.max(0,parent.getHeight()-commonHeight),Math.max(0, nav - below) + dp(o.get(DockOptions.Key.OFFSET)));
        position(pill, geometry.left-pill.bleed, geometry.barWidth+pill.bleed*2, height+pill.bleed*2, bottom+(commonHeight-height)/2-pill.bleed);
        position(avatarHost, geometry.avatarLeft, geometry.avatarSize, geometry.avatarSize, bottom+(commonHeight-geometry.avatarSize)/2);
        pill.select(TabBarBridge.selectedIndex(row), n);
        followNative();
        if (avatarHost.isShown() && activity.hasWindowFocus()) avatarBridge.refresh(avatar);
    }

    private void position(View v, int x, int w, int h, int bottom) {
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) v.getLayoutParams();
        if (p.leftMargin == x && p.width == w && p.height == h && p.bottomMargin == bottom) return;
        p.leftMargin = x; p.width = w; p.height = h; p.bottomMargin = bottom;
        v.setLayoutParams(p); // Module-owned views only, outside pre-draw.
        backdrop.changed();
    }

    private void restoreChrome() {
        for(java.util.Map.Entry<View,Rect> entry:chromeClips.entrySet()) entry.getKey().setClipBounds(entry.getValue());
        chromeClips.clear();
    }
    private void updateChrome(boolean hide) {
        if(!hide) { if(!chromeClips.isEmpty()) restoreChrome(); return; }
        for(int i=0;i<parent.getChildCount();i++) {
            View child=parent.getChildAt(i);
            boolean known=child.getClass().getName().equals("com.tencent.qui.quiblurview.QQBlurViewWrapper");
            boolean line=child.getClass()==View.class && child.getHeight()>0 && child.getHeight()<=dp(1.5f)
                    && child.getWidth()>=parent.getWidth()*.9f && child.getTop()>=nativeBar.getTop()-dp(3);
            if(child!=nativeBar && child!=pill && child!=avatarHost && (known||line) && !chromeClips.containsKey(child)) {
                chromeClips.put(child,child.getClipBounds());
                FeedbackLog.event("CHROME_BOUND",child.getClass().getName());
            }
        }
    }
    private int dp(float v) { return Math.round(v * density); }

    private static TextView findTitle(View v, int depth) {
        if (depth > 6) return null;
        if (v instanceof TextView) {
            String s = ((TextView) v).getText().toString();
            if (!s.isEmpty() && s.length() <= 16 && !s.matches("[0-9+ .]+")) return (TextView) v;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                TextView t = findTitle(g.getChildAt(i), depth + 1);
                if (t != null) return t;
            }
        }
        return null;
    }

    private void disposeLabels() {
        for(int i=0;i<labels.getChildCount();i++) {
            View v=labels.getChildAt(i);
            if(v instanceof Label) ((Label)v).dispose();
        }
    }

    private static View findThemeIcon(View v,int depth) {
        if(v==null || depth>7) return null;
        HostApp app=LiquidGlassModule.app();
        if(app!=null && app.isTabIconClass(v.getClass().getName())) return v;
        if(v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++) {
                View found=findThemeIcon(g.getChildAt(i),depth+1);
                if(found!=null) return found;
            }
        }
        return null;
    }

    private final class Label extends View implements DropletPanel.TabContent {
        final View target;
        View themeIcon;
        final TextView title;
        final String fallback;
        String value="",badge="";
        final Paint badgePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint themePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        final Paint metricsPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF themeDst=new RectF();
        Bitmap themeBitmap; Canvas themeCanvas;
        Label(View target,String fallback) {
            super(activity);
            this.target=target; this.fallback=fallback; title=findTitle(target,0); themeIcon=findThemeIcon(target,0);
            setFocusable(true); setClickable(true);
            setOnClickListener(v->{
                if(disposed || paused || !target.isAttachedToWindow()) return;
                try { target.performClick(); requestSync(); }
                catch(Throwable t) { failOpen("QQ tab click",t); }
            });
            setOnLongClickListener(v->{
                if(disposed || paused || !target.isAttachedToWindow()) return false;
                try { return target.performLongClick(); }
                catch(Throwable t) { FeedbackLog.error("NATIVE_TAB_LONG_PRESS",t);return false; }
            });
        }
        void sync() {
            DockOptions options=GlassConfig.options;
            String text=title==null || title.getText().length()==0?fallback:title.getText().toString();
            String unread=options.on(DockOptions.Key.BADGES)?unreadText(target,0):"";
            boolean stateChanged=!value.equals(text) || !badge.equals(unread) || isSelected()!=target.isSelected();
            if(stateChanged) {
                value=text; badge=unread; setSelected(target.isSelected());
                setContentDescription(text+(unread.isEmpty()?"":"，"+unread+"条未读"));
            }
            boolean iconChanged=options.on(DockOptions.Key.THEME_ICONS) && refreshThemeBitmap();
            if(stateChanged || iconChanged) invalidate();
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawContent(canvas,isSelected());
        }
        @Override public void drawForLens(Canvas canvas) { drawContent(canvas,true); }
        private void drawContent(Canvas canvas,boolean selected) {
            DockOptions options=GlassConfig.options;
            boolean useTheme=options.on(DockOptions.Key.THEME_ICONS) && themeBitmap!=null
                    && !themeBitmap.isRecycled() && options.get(DockOptions.Key.MODE)!=0;
            DockPainter.tab(canvas,getWidth(),getHeight(),value,labels.indexOfChild(this),labels.getChildCount(),
                    selected,options,dark,density,getResources().getDisplayMetrics().scaledDensity,!useTheme);
            if(useTheme) drawThemeIcon(canvas,options);
            if(badge.isEmpty()) return;
            float r=dp(7),cy=dp(9);
            badgePaint.setTextSize(dp(9)); badgePaint.setTypeface(Typeface.DEFAULT_BOLD);
            float w=Math.max(r*2,badgePaint.measureText(badge)+dp(6));
            float right=getWidth()-dp(2),left=right-w;
            badgePaint.setColor(0xffed4653);
            canvas.drawRoundRect(left,cy-r,right,cy+r,r,r,badgePaint);
            badgePaint.setColor(0xffffffff); badgePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(badge,(left+right)/2,cy-(badgePaint.ascent()+badgePaint.descent())/2,badgePaint);
        }
        private void drawThemeIcon(Canvas canvas,DockOptions options) {
            int mode=options.get(DockOptions.Key.MODE);
            float scaledDensity=getResources().getDisplayMetrics().scaledDensity;
            metricsPaint.setTypeface(options.on(DockOptions.Key.BOLD)?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
            metricsPaint.setTextSize(options.get(DockOptions.Key.TEXT)*scaledDensity*options.scale());
            float textHeight=metricsPaint.descent()-metricsPaint.ascent();
            float iconSize=DockPainter.iconSize(getHeight(),options,density);
            float top=DockPainter.iconTop(getHeight(),iconSize,textHeight,mode,density);
            float scale=Math.min(iconSize/themeBitmap.getWidth(),iconSize/themeBitmap.getHeight());
            float drawW=themeBitmap.getWidth()*scale,drawH=themeBitmap.getHeight()*scale;
            themeDst.set((getWidth()-drawW)/2f,top+(iconSize-drawH)/2f,
                    (getWidth()+drawW)/2f,top+(iconSize+drawH)/2f);
            canvas.drawBitmap(themeBitmap,null,themeDst,themePaint);
        }
        private boolean refreshThemeBitmap() {
            View current=findThemeIcon(target,0);
            if(current!=themeIcon) {
                themeIcon=current;
                if(themeBitmap!=null && !themeBitmap.isRecycled()) themeBitmap.recycle();
                themeBitmap=null;themeCanvas=null;
            }
            if(themeIcon==null || themeIcon.getWidth()<=0 || themeIcon.getHeight()<=0) return false;
            try {
                int w=themeIcon.getWidth(),h=themeIcon.getHeight();
                if(themeBitmap==null || themeBitmap.isRecycled() || themeBitmap.getWidth()!=w || themeBitmap.getHeight()!=h) {
                    if(themeBitmap!=null && !themeBitmap.isRecycled()) themeBitmap.recycle();
                    themeBitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
                    themeCanvas=new Canvas(themeBitmap);
                } else themeBitmap.eraseColor(0x00000000);
                themeIcon.draw(themeCanvas);
                return true;
            } catch(Throwable t) {
                FeedbackLog.error("THEME_ICON_CAPTURE",t);
                return false;
            }
        }
        Bitmap copyThemeIcon() {
            if(themeBitmap==null || themeBitmap.isRecycled()) refreshThemeBitmap();
            if(themeBitmap==null || themeBitmap.isRecycled()) return null;
            try { return themeBitmap.copy(Bitmap.Config.ARGB_8888,false); }
            catch(Throwable t) { FeedbackLog.error("THEME_ICON_PREVIEW",t); return null; }
        }
        void dispose() {
            if(themeBitmap!=null && !themeBitmap.isRecycled()) themeBitmap.recycle();
            themeBitmap=null;themeCanvas=null;
        }
    }

    private static String unreadText(View v, int depth) {
        if (depth > 6 || v.getVisibility() != View.VISIBLE) return "";
        if (v instanceof TextView) {
            String text = ((TextView) v).getText().toString().trim();
            if (text.matches("[1-9][0-9]{0,3}\\+?")) return text.length() > 3 ? "99+" : text;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                String value = unreadText(g.getChildAt(i), depth + 1);
                if (!value.isEmpty()) return value;
            }
        }
        return "";
    }

    private final class Capsule extends FrameLayout {
        final int bleed=dp(32);
        int index = -1, count;
        float downX,downY;
        boolean blocked;
        Capsule(Activity a) {
            super(a);setClipChildren(false);setClipToPadding(false);
            setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View v,android.graphics.Outline outline) {
                    float radius=(v.getHeight()-bleed*2)*GlassConfig.options.get(DockOptions.Key.CORNER)/100f;
                    outline.setRoundRect(bleed,bleed,Math.max(bleed,v.getWidth()-bleed),Math.max(bleed,v.getHeight()-bleed),radius);
                }
            });
        }
        void stopAnimation() { drag.stop();releaseParent(); }
        void select(int target, int n) {
            if (target < 0 || target >= n) return;
            for(int i=0;i<labels.getChildCount();i++) labels.getChildAt(i).setSelected(i==target);
            int w=Math.max(1,(getLayoutParams().width-bleed*2-dp(8))/n);
            int h=Math.max(1,getLayoutParams().height-bleed*2-dp(8));
            FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)droplet.getLayoutParams();
            boolean resized=lp.width!=w || lp.height!=h;
            if(resized) {lp.width=w;lp.height=h;lp.topMargin=bleed+dp(4);droplet.setLayoutParams(lp);}
            droplet.setVisibility(View.VISIBLE);
            if(!resized && target==index && n==count) return;
            boolean first = index < 0 || count != n;
            index=target;count=n;
            drag.animateToIndex(target,first || resized);
            backdrop.changed();
        }
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b) {
            super.onLayout(changed,l,t,r,b);
            if(changed && index>=0) drag.animateToIndex(index,true);
        }
        @Override protected void dispatchDraw(Canvas c) { if(!QqGlassBackdrop.isCapturing()) super.dispatchDraw(c); }
        private void releaseParent() { if(blocked && getParent()!=null) getParent().requestDisallowInterceptTouchEvent(false);blocked=false; }
        @Override public boolean dispatchTouchEvent(MotionEvent e) {
            if(disposed || paused) return false;
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN) {
                // Transparent lens-overflow padding must not steal message-list touches.
                if(e.getX()<bleed || e.getX()>getWidth()-bleed || e.getY()<bleed || e.getY()>getHeight()-bleed) return false;
                downX=e.getX();downY=e.getY();blocked=true;
                if(getParent()!=null) getParent().requestDisallowInterceptTouchEvent(true);
            } else if(action==MotionEvent.ACTION_MOVE && Math.abs(e.getY()-downY)>Math.max(dp(20),50)
                    && Math.abs(e.getY()-downY)>Math.abs(e.getX()-downX)) releaseParent();
            try { return super.dispatchTouchEvent(e); }
            finally { if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) releaseParent(); }
        }
        @Override public boolean onInterceptTouchEvent(MotionEvent e) { return drag.onIntercept(e) || super.onInterceptTouchEvent(e); }
        @Override public boolean onTouchEvent(MotionEvent e) { return drag.onTouch(e) || super.onTouchEvent(e); }
    }
}
