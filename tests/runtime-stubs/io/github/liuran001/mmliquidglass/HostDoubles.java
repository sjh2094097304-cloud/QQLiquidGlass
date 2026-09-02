package io.github.liuran001.mmliquidglass;
import android.view.*;
class LiquidGlassHostLayout {interface DragHandler {boolean onIntercept(MotionEvent e);boolean onTouch(MotionEvent e);}}
class LiquidGlassPanel extends View {void setInteraction(float p,float x){}}
class DropletPanel extends View {float progress;void setProgress(float p){progress=p;}void refresh(){}}
class TabBarBridge {
    static int tabCount(ViewGroup row){return row==null?0:row.getChildCount();}
    static View tabAt(ViewGroup row,int i){return row==null||i<0||i>=row.getChildCount()?null:row.getChildAt(i);}
    static int selectedIndex(ViewGroup row){for(int i=0;i<row.getChildCount();i++)if(row.getChildAt(i).isSelected())return i;return -1;}
}
class HostApp {static final HostApp QQ=new HostApp();boolean isHiddenSibling(String s){return s.contains("QQBlurViewWrapper");}}
class QqSplitDock {static final Object OWNED=new Object();}
