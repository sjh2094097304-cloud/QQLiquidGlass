package android.view;
public class ViewGroup extends View {
    private final java.util.List<View> children=new java.util.ArrayList<>();
    public static class LayoutParams { public int width,height;public LayoutParams(int w,int h){width=w;height=h;} }
    public void add(View v){children.add(v);v.parent=this;}
    public void remove(View v){children.remove(v);v.parent=null;}
    public int getChildCount(){return children.size();}
    public View getChildAt(int i){return children.get(i);}
    public int indexOfChild(View v){return children.indexOf(v);}
}
