package android.widget;
public class LinearLayout extends android.view.ViewGroup {
protected LinearLayout() {}
public LinearLayout(android.content.Context p0) {}
public LinearLayout(android.content.Context p0, android.util.AttributeSet p1) {}
public LinearLayout(android.content.Context p0, android.util.AttributeSet p1, int p2) {}
public LinearLayout(android.content.Context p0, android.util.AttributeSet p1, int p2, int p3) {}
public void setDividerDrawable(android.graphics.drawable.Drawable p0) { throw new RuntimeException("API stub"); }
public void setGravity(int p0) { throw new RuntimeException("API stub"); }
public void setOrientation(int p0) { throw new RuntimeException("API stub"); }
public void setShowDividers(int p0) { throw new RuntimeException("API stub"); }
public static final int HORIZONTAL = 0;
public static final int SHOW_DIVIDER_MIDDLE = 2;
public static final int VERTICAL = 1;
public static class LayoutParams extends android.view.ViewGroup.MarginLayoutParams {
protected LayoutParams() {}
public LayoutParams(android.content.Context p0, android.util.AttributeSet p1) {}
public LayoutParams(int p0, int p1) {}
public LayoutParams(int p0, int p1, float p2) {}
public LayoutParams(android.view.ViewGroup.LayoutParams p0) {}
public LayoutParams(android.view.ViewGroup.MarginLayoutParams p0) {}
public LayoutParams(android.widget.LinearLayout.LayoutParams p0) {}
public int gravity;
public float weight;
}
}
