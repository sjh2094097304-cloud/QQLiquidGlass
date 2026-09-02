package android.graphics;
public final class Rect {
    public int left,top,right,bottom;
    public void set(int l,int t,int r,int b){left=l;top=t;right=r;bottom=b;}
    public int width(){return right-left;}
    public int height(){return bottom-top;}
    public void union(int l,int t,int r,int b){set(Math.min(left,l),Math.min(top,t),Math.max(right,r),Math.max(bottom,b));}
    public void inset(int x,int y){left+=x;right-=x;top+=y;bottom-=y;}
    public boolean intersect(int l,int t,int r,int b){if(left>=r||right<=l||top>=b||bottom<=t)return false;set(Math.max(left,l),Math.max(top,t),Math.min(right,r),Math.min(bottom,b));return true;}
    public static boolean intersects(Rect a,Rect b){return a.left<b.right && a.right>b.left && a.top<b.bottom && a.bottom>b.top;}
}
