package android.animation;
public abstract class Animator {
public Animator() {}
public void cancel() { throw new RuntimeException("API stub"); }
public android.animation.Animator clone() { throw new RuntimeException("API stub"); }
public void end() { throw new RuntimeException("API stub"); }
public android.animation.Animator setDuration(long p0) { throw new RuntimeException("API stub"); }
public void setInterpolator(android.animation.TimeInterpolator p0) { throw new RuntimeException("API stub"); }
public void setStartDelay(long p0) { throw new RuntimeException("API stub"); }
public void start() { throw new RuntimeException("API stub"); }
public static interface AnimatorListener {
public void onAnimationCancel(android.animation.Animator p0);
public default void onAnimationEnd(android.animation.Animator p0, boolean p1) { throw new RuntimeException("API stub"); }
public void onAnimationEnd(android.animation.Animator p0);
public void onAnimationRepeat(android.animation.Animator p0);
public default void onAnimationStart(android.animation.Animator p0, boolean p1) { throw new RuntimeException("API stub"); }
public void onAnimationStart(android.animation.Animator p0);
}
}
