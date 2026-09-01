package android.graphics.drawable;
public abstract class Drawable {
public Drawable() {}
 public android.graphics.drawable.Drawable mutate() { throw new RuntimeException("API stub"); }
public void setAlpha(int p0) { throw new RuntimeException("API stub"); }
public static interface Callback {
public void invalidateDrawable(android.graphics.drawable.Drawable p0);
public void scheduleDrawable(android.graphics.drawable.Drawable p0, Runnable p1, long p2);
public void unscheduleDrawable(android.graphics.drawable.Drawable p0, Runnable p1);
}
}
