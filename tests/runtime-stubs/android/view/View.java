package android.view;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
public class View implements ViewParent {
    public static final int VISIBLE=0, INVISIBLE=4, GONE=8;
    public int left,top,width=80,height=56,visibility=VISIBLE,clicks,draws;
    public boolean selected,dirty,shown=true;
    public Object tag;
    public ViewGroup parent;
    public Runnable drawHook,clickHook;
    private float tx,ty,sx=1,sy=1;
    private ViewGroup.LayoutParams lp=new ViewGroup.LayoutParams(80,56);
    public Context getContext(){return new Context();}
    public int getVisibility(){return visibility;}
    public int getWidth(){return width;}
    public int getHeight(){return height;}
    public int getLeft(){return left;}
    public int getTop(){return top;}
    public ViewGroup.LayoutParams getLayoutParams(){return lp;}
    public void setTranslationX(float x){tx=x;}
    public float getTranslationX(){return tx;}
    public void setScaleX(float x){sx=x;}
    public float getScaleX(){return sx;}
    public void setScaleY(float x){sy=x;}
    public float getScaleY(){return sy;}
    public boolean isSelected(){return selected;}
    public boolean performClick(){clicks++;if(clickHook!=null)clickHook.run();return true;}
    public ViewParent getParent(){return parent;}
    public Object getTag(){return tag;}
    public boolean isShown(){return shown && visibility==VISIBLE;}
    public boolean isDirty(){return dirty;}
    public void getLocationOnScreen(int[] p){p[0]=left;p[1]=top;}
    public boolean getGlobalVisibleRect(Rect r){r.set(left,top,left+width,top+height);return isShown();}
    public void draw(Canvas c){draws++;dirty=false;if(drawHook!=null)drawHook.run();}
}
