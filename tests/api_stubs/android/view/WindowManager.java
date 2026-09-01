package android.view;
public interface WindowManager extends android.view.ViewManager {
public default void addCrossWindowBlurEnabledListener(java.util.function.Consumer<java.lang.Boolean> p0) { throw new RuntimeException("API stub"); }
public default void addCrossWindowBlurEnabledListener(java.util.concurrent.Executor p0, java.util.function.Consumer<java.lang.Boolean> p1) { throw new RuntimeException("API stub"); }
public default void addProposedRotationListener(java.util.concurrent.Executor p0, java.util.function.IntConsumer p1) { throw new RuntimeException("API stub"); }
  public default int addScreenRecordingCallback(java.util.concurrent.Executor p0, java.util.function.Consumer<java.lang.Integer> p1) { throw new RuntimeException("API stub"); }
 public default android.view.WindowMetrics getCurrentWindowMetrics() { throw new RuntimeException("API stub"); }
 public android.view.Display getDefaultDisplay();
 public default android.view.WindowMetrics getMaximumWindowMetrics() { throw new RuntimeException("API stub"); }
public default boolean isCrossWindowBlurEnabled() { throw new RuntimeException("API stub"); }
  public default android.window.InputTransferToken registerBatchedSurfaceControlInputReceiver(android.window.InputTransferToken p0, android.view.SurfaceControl p1, android.view.Choreographer p2, android.view.SurfaceControlInputReceiver p3) { throw new RuntimeException("API stub"); }
 public default void registerTrustedPresentationListener(android.os.IBinder p0, android.window.TrustedPresentationThresholds p1, java.util.concurrent.Executor p2, java.util.function.Consumer<java.lang.Boolean> p3) { throw new RuntimeException("API stub"); }
  public default android.window.InputTransferToken registerUnbatchedSurfaceControlInputReceiver(android.window.InputTransferToken p0, android.view.SurfaceControl p1, android.os.Looper p2, android.view.SurfaceControlInputReceiver p3) { throw new RuntimeException("API stub"); }
public default void removeCrossWindowBlurEnabledListener(java.util.function.Consumer<java.lang.Boolean> p0) { throw new RuntimeException("API stub"); }
public default void removeProposedRotationListener(java.util.function.IntConsumer p0) { throw new RuntimeException("API stub"); }
  public default void removeScreenRecordingCallback(java.util.function.Consumer<java.lang.Integer> p0) { throw new RuntimeException("API stub"); }
public void removeViewImmediate(android.view.View p0);
 public default boolean transferTouchGesture(android.window.InputTransferToken p0, android.window.InputTransferToken p1) { throw new RuntimeException("API stub"); }
 public default void unregisterSurfaceControlInputReceiver(android.view.SurfaceControl p0) { throw new RuntimeException("API stub"); }
 public default void unregisterTrustedPresentationListener(java.util.function.Consumer<java.lang.Boolean> p0) { throw new RuntimeException("API stub"); }
public static class LayoutParams extends android.view.ViewGroup.LayoutParams {
public LayoutParams() {}
public LayoutParams(int p0) {}
public LayoutParams(int p0, int p1) {}
public LayoutParams(int p0, int p1, int p2) {}
public LayoutParams(int p0, int p1, int p2, int p3, int p4) {}
public LayoutParams(int p0, int p1, int p2, int p3, int p4, int p5, int p6) {}
public LayoutParams(android.os.Parcel p0) {}
public static final int FLAG_DIM_BEHIND = 2;
public float alpha;
public float dimAmount;
public int flags;
public int format;
public int gravity;
public String packageName;
public android.os.IBinder token;
public int type;
public int x;
public int y;
}
}
