package io.github.liuran001.mmliquidglass;
import android.view.*;
public final class DropletInteractionTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static MotionEvent e(int action,float x,float y){return new MotionEvent(action,x,y);}
    public static void main(String[] args) {
        ViewGroup row=new ViewGroup();row.width=240;
        for(int i=0;i<3;i++){View v=new View();v.left=i*80;row.add(v);v.clickHook=()->{for(int j=0;j<3;j++)row.getChildAt(j).selected=row.getChildAt(j)==v;};}
        row.getChildAt(1).selected=true;
        DropletPanel lens=new DropletPanel();LiquidGlassPanel glass=new LiquidGlassPanel();glass.width=240;
        DropletDragController drag=new DropletDragController(lens,row,1,false);drag.setPill(glass);
        Choreographer clock=Choreographer.getInstance();drag.animateToIndex(1,true);
        check(!drag.onIntercept(e(0,120,20)),"tap remains with label");clock.frames(15);
        check(lens.getScaleX()>1.2f && lens.progress>.8f,"held selected tab grows using upstream springs");
        check(drag.onIntercept(e(2,210,20)),"horizontal drag intercepted");drag.onTouch(e(2,210,20));drag.onTouch(e(1,210,20));clock.frames(240);
        check(row.getChildAt(2).clicks==1 && clock.pending()==0,"release switches exactly once and settles");
        drag.onIntercept(e(0,210,20));drag.onIntercept(e(2,20,20));drag.onTouch(e(2,20,20));drag.onTouch(e(3,20,20));clock.frames(240);
        check(row.getChildAt(0).clicks==0 && clock.pending()==0,"cancel never switches tab");
        drag.onIntercept(e(0,210,20));check(!drag.onIntercept(e(2,212,100)),"vertical gesture not intercepted");drag.onIntercept(e(3,212,100));clock.frames(240);
        for(int duration:new int[]{0,1,180,400}) {drag.setAnimationDuration(duration);for(int i=0;i<100;i++){drag.animateToIndex(i%3,false);clock.frames(3);check(Float.isFinite(lens.getScaleX()),"finite fast selection");}clock.frames(500);check(clock.pending()==0,"duration settles");}
        drag.animateToIndex(1,false);drag.stop();check(clock.pending()==0,"pause removes frame callback");
        drag.setAnimationDuration(180);drag.setPressStrength(0);drag.animateToIndex(1,true);
        drag.onIntercept(e(0,120,20));clock.frames(60);
        check(Math.abs(lens.getScaleX()-1)<.001f && Math.abs(glass.getScaleX()-1)<.001f,"zero press strength disables held growth");
        drag.stop();drag.setPressStrength(125);drag.onIntercept(e(0,120,20));clock.frames(60);
        check(lens.getScaleX()>1.45f && lens.getScaleX()<1.52f,"custom press strength applies");drag.stop();
        System.out.println("PASS: actual drag controller hold/drag/release/cancel/vertical/duration/pause with host doubles");
    }
}
