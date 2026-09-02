package io.github.liuran001.mmliquidglass;
import android.view.*;
import android.graphics.Canvas;
public final class SharedBackdropTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args) {
        ViewGroup parent=new ViewGroup();parent.width=400;parent.height=900;
        ViewGroup page=new ViewGroup();page.width=400;page.height=900;parent.add(page);
        View bar=new View();parent.add(bar);
        ViewGroup pill=new ViewGroup();pill.width=280;pill.height=56;pill.top=800;pill.tag=QqSplitDock.OWNED;parent.add(pill);
        View avatar=new View();avatar.left=300;avatar.top=800;parent.add(avatar);
        QqGlassBackdrop b=new QqGlassBackdrop(parent,bar,pill,avatar,1);b.bindSource();
        Canvas c=new Canvas();int[] pos={0,800};
        page.drawHook=()->check(!b.draw(c,pos,24,328,104),"reentrant request blocked");
        check(b.prepare() && b.draw(c,pos,24,328,104),"initial frame recorded");
        for(int i=0;i<100;i++)check(b.draw(c,pos,24,328,104),"cached consumer");
        check(page.draws==1 && !b.prepare() && pill.draws==0 && parent.draws==0,"one capture shared; no ancestor/self feedback");
        page.dirty=true;check(b.prepare(),"native refresh dirties capture");b.draw(c,pos,24,328,104);check(page.draws==2,"refresh records once");
        b.setPaused(true);b.changed();b.draw(c,pos,24,328,104);check(page.draws==2,"pause freezes display list");
        b.setPaused(false);b.draw(c,pos,24,328,104);check(page.draws==3,"resume refreshes");
        page.drawHook=()->{throw new IllegalStateException();};b.changed();check(!b.draw(c,pos,24,328,104),"capture exception falls back");
        check(!QqGlassBackdrop.isCapturing(),"finally releases capture guard");b.dispose();
        String hostStatus=QqGlassBackdrop.status,hostLog=FeedbackLog.text();
        QqGlassBackdrop preview=new QqGlassBackdrop(parent,bar,pill,avatar,1,false);preview.bindSource();
        preview.reportFailure("preview shader unavailable");preview.draw(c,pos,24,328,104);
        check(hostStatus.equals(QqGlassBackdrop.status) && hostLog.equals(FeedbackLog.text()),"preview source/errors cannot overwrite actual QQ diagnostics");preview.dispose();
        page.drawHook=null;parent.remove(page);parent.add(page);
        QqGlassBackdrop afterBar=new QqGlassBackdrop(parent,bar,pill,avatar,1);afterBar.bindSource();
        check(!afterBar.draw(c,pos,24,328,104),"overlay after dock cannot become source");
        System.out.println("PASS: actual shared capture cache/reentry/pause/error/source isolation with host doubles (no GPU)");
    }
}
