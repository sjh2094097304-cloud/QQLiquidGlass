package io.github.liuran001.mmliquidglass;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;

/** One cropped, shared page display list for the pill, avatar ring and held lens. */
final class QqGlassBackdrop {
    static volatile String status="等待液态玻璃背景";
    private static boolean capturing; // All callers run on the UI thread.
    private final ViewGroup parent;
    private final View nativeBar, pill, avatar;
    private ViewGroup source;
    private final RenderNode node=new RenderNode("qqGlassSharedBackdrop");
    private final Rect region=new Rect(), visible=new Rect();
    private final int[] point=new int[2];
    private boolean dirty=true, valid, paused, disabled, dark;
    private int slowFrames;
    private final int padding;
    private final boolean diagnostics;

    QqGlassBackdrop(ViewGroup parent,View nativeBar,View pill,View avatar,float density) {
        this(parent,nativeBar,pill,avatar,density,true);
    }
    QqGlassBackdrop(ViewGroup parent,View nativeBar,View pill,View avatar,float density,boolean diagnostics) {
        this.parent=parent;this.nativeBar=nativeBar;this.pill=pill;this.avatar=avatar;
        this.diagnostics=diagnostics;
        // Includes the 40dp maximum refraction and the original 78/56 held lens.
        padding=Math.round(100*density);
    }
    static boolean isCapturing() { return capturing; }
    void reportFailure(String message) { if(diagnostics) status=message; }
    void setPaused(boolean value) { paused=value; if(!value) dirty=true; }
    void setTheme(boolean value) { if(dark!=value) {dark=value;dirty=true;} }
    void changed() { dirty=true; }

    /** Called by the coalesced sync, never by capture or a drawing callback. */
    void bindSource() {
        ViewGroup best=null;
        long area=0;
        int before=parent.indexOfChild(nativeBar);
        for(int i=0;i<before;i++) {
            View v=parent.getChildAt(i);
            if(!(v instanceof ViewGroup) || v.getTag()==QqSplitDock.OWNED || !v.isShown()
                    || HostApp.QQ.isHiddenSibling(v.getClass().getName())
                    || v.getWidth()<parent.getWidth()/2 || v.getHeight()<parent.getHeight()/3) continue;
            long size=(long)v.getWidth()*v.getHeight();
            if(size>area) {area=size;best=(ViewGroup)v;}
        }
        if(source!=best) {
            source=best;dirty=true;valid=false;
            if(!disabled) reportFailure(best==null?"未识别独立内容区，使用玻璃蒙层":"原版液态玻璃 · 共享局部背景");
            if(best!=null && diagnostics) FeedbackLog.event("GLASS_SOURCE",best.getClass().getName());
        }
    }

    /** Only a dirty sibling requests a refresh; our own redraw cannot feed itself. */
    boolean prepare() {
        if(paused || disabled || source==null || source.getParent()!=parent || !source.isShown() || capturing) return false;
        if(source.isDirty()) dirty=true;
        return dirty;
    }

    boolean draw(Canvas canvas,int[] self,int pad,int width,int height) {
        if(!canvas.isHardwareAccelerated() || capturing || disabled) return false;
        if(dirty && !paused) record();
        if(!valid) return false;
        int save=canvas.save();
        canvas.clipRect(0,0,width,height);
        canvas.translate(region.left-self[0]+pad,region.top-self[1]+pad);
        canvas.drawRenderNode(node);
        canvas.restoreToCount(save);
        return true;
    }

    private void record() {
        // A source must be a preceding sibling, never the window, dock or an ancestor.
        if(source==null || source.getParent()!=parent || parent.indexOfChild(source)>=parent.indexOfChild(nativeBar)
                || source==pill || source==avatar || !source.isShown() || capturing) {
            dirty=false;valid=false;return;
        }
        pill.getLocationOnScreen(point);
        region.set(point[0],point[1],point[0]+pill.getWidth(),point[1]+pill.getHeight());
        if(avatar.getVisibility()==View.VISIBLE) {
            avatar.getLocationOnScreen(point);
            region.union(point[0],point[1],point[0]+avatar.getWidth(),point[1]+avatar.getHeight());
        }
        region.inset(-padding,-padding);
        parent.getLocationOnScreen(point);
        if(!region.intersect(point[0],point[1],point[0]+parent.getWidth(),point[1]+parent.getHeight())) return;
        if(region.width()<=0 || region.height()<=0) return;
        capturing=true;
        long started=SystemClock.uptimeMillis();
        try {
            node.setPosition(0,0,region.width(),region.height());
            RecordingCanvas c=node.beginRecording(region.width(),region.height());
            try {
                c.drawColor(dark?0xff111111:0xfff7f7f7);
                // Upstream's pager handling preserves both pages during a swipe.
                String name=source.getClass().getName();
                if(name.contains("ViewPager") && !name.contains("ViewPager2")) {
                    for(int i=0;i<source.getChildCount();i++) paintPage(c,source.getChildAt(i));
                } else paintPage(c,source);
            } finally { node.endRecording(); }
            valid=true;
            long elapsed=SystemClock.uptimeMillis()-started;
            slowFrames=elapsed>48?slowFrames+1:0;
            if(slowFrames>=3) {
                disabled=true;
                reportFailure("背景绘制持续超时，已降级玻璃蒙层");
                if(diagnostics) FeedbackLog.event("GLASS_BUDGET","capture circuit opened");
            }
        } catch(Throwable t) {
            valid=false;disabled=true;reportFailure("背景绘制异常，已降级玻璃蒙层");
            if(diagnostics) FeedbackLog.error("GLASS_CAPTURE",t);
        } finally { capturing=false;dirty=false; }
    }
    private void paintPage(Canvas c,View page) {
        if(page.getVisibility()!=View.VISIBLE || page.getTag()==QqSplitDock.OWNED
                || !page.getGlobalVisibleRect(visible) || !Rect.intersects(region,visible)) return;
        page.getLocationOnScreen(point);
        int save=c.save();
        c.translate(point[0]-region.left,point[1]-region.top);
        c.clipRect(region.left-point[0],region.top-point[1],region.right-point[0],region.bottom-point[1]);
        try { page.draw(c); } finally { c.restoreToCount(save); }
    }
    void dispose() { paused=true;source=null;valid=false;node.discardDisplayList(); }
}
