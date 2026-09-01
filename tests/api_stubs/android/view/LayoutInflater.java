package android.view;
public abstract class LayoutInflater {
protected LayoutInflater() {}
protected LayoutInflater(android.content.Context p0) {}
protected LayoutInflater(android.view.LayoutInflater p0, android.content.Context p1) {}
public static android.view.LayoutInflater from(android.content.Context p0) { throw new RuntimeException("API stub"); }
public android.content.Context getContext() { throw new RuntimeException("API stub"); }
public static interface Factory2 extends android.view.LayoutInflater.Factory {
 public android.view.View onCreateView(android.view.View p0, String p1, android.content.Context p2, android.util.AttributeSet p3);
}
public static interface Factory {
 public android.view.View onCreateView(String p0, android.content.Context p1, android.util.AttributeSet p2);
}
}
