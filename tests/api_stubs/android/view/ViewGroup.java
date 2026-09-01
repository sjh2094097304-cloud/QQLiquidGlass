package android.view;
public abstract class ViewGroup extends android.view.View {
protected ViewGroup() {}
public ViewGroup(android.content.Context p0) {}
public ViewGroup(android.content.Context p0, android.util.AttributeSet p1) {}
public ViewGroup(android.content.Context p0, android.util.AttributeSet p1, int p2) {}
public ViewGroup(android.content.Context p0, android.util.AttributeSet p1, int p2, int p3) {}
public void addView(android.view.View p0) { throw new RuntimeException("API stub"); }
public void addView(android.view.View p0, int p1) { throw new RuntimeException("API stub"); }
public void addView(android.view.View p0, int p1, int p2) { throw new RuntimeException("API stub"); }
public void addView(android.view.View p0, android.view.ViewGroup.LayoutParams p1) { throw new RuntimeException("API stub"); }
public void addView(android.view.View p0, int p1, android.view.ViewGroup.LayoutParams p2) { throw new RuntimeException("API stub"); }
public android.view.View getChildAt(int p0) { throw new RuntimeException("API stub"); }
public int getChildCount() { throw new RuntimeException("API stub"); }
public final void layout(int p0, int p1, int p2, int p3) { throw new RuntimeException("API stub"); }
public void removeAllViews() { throw new RuntimeException("API stub"); }
public void removeView(android.view.View p0) { throw new RuntimeException("API stub"); }
public static class LayoutParams {
protected LayoutParams() {}
public LayoutParams(android.content.Context p0, android.util.AttributeSet p1) {}
public LayoutParams(int p0, int p1) {}
public LayoutParams(android.view.ViewGroup.LayoutParams p0) {}
public static final int MATCH_PARENT = -1;
public static final int WRAP_CONTENT = -2;
public int height;
public int width;
}
public static class MarginLayoutParams extends android.view.ViewGroup.LayoutParams {
protected MarginLayoutParams() {}
public MarginLayoutParams(android.content.Context p0, android.util.AttributeSet p1) {}
public MarginLayoutParams(int p0, int p1) {}
public MarginLayoutParams(android.view.ViewGroup.MarginLayoutParams p0) {}
public MarginLayoutParams(android.view.ViewGroup.LayoutParams p0) {}
public int bottomMargin;
public int leftMargin;
public int rightMargin;
public int topMargin;
}
}
