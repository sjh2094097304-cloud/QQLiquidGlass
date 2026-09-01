package android.os;
public class Handler {
 public Handler() {}
 public Handler(android.os.Handler.Callback p0) {}
public Handler(android.os.Looper p0) {}
public Handler(android.os.Looper p0, android.os.Handler.Callback p1) {}
public final boolean post(Runnable p0) { throw new RuntimeException("API stub"); }
public final boolean postDelayed(Runnable p0, long p1) { throw new RuntimeException("API stub"); }
public final boolean postDelayed(Runnable p0, Object p1, long p2) { throw new RuntimeException("API stub"); }
public final void removeCallbacks(Runnable p0) { throw new RuntimeException("API stub"); }
public final void removeCallbacks(Runnable p0, Object p1) { throw new RuntimeException("API stub"); }
public static interface Callback {
public boolean handleMessage(android.os.Message p0);
}
}
