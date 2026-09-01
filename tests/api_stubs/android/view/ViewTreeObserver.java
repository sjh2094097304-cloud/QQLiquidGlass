package android.view;
public class ViewTreeObserver {
protected ViewTreeObserver() {}
public static interface OnPreDrawListener {
public boolean onPreDraw();
}
}
