package android.graphics;
public class Canvas {
    public boolean isHardwareAccelerated(){return true;}
    public int save(){return 1;}
    public void restoreToCount(int n){}
    public void clipRect(int l,int t,int r,int b){}
    public void translate(float x,float y){}
    public void drawRenderNode(RenderNode n){}
    public void drawColor(int color){}
}
