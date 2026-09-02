package android.view;
import android.content.Context;
public final class ViewConfiguration {
    public static ViewConfiguration get(Context c){return new ViewConfiguration();}
    public int getScaledTouchSlop(){return 8;}
}
