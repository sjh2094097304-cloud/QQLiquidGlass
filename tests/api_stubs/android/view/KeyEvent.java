package android.view;
public class KeyEvent extends android.view.InputEvent {
protected KeyEvent() {}
public KeyEvent(int p0, int p1) {}
public KeyEvent(long p0, long p1, int p2, int p3, int p4) {}
public KeyEvent(long p0, long p1, int p2, int p3, int p4, int p5) {}
public KeyEvent(long p0, long p1, int p2, int p3, int p4, int p5, int p6, int p7) {}
public KeyEvent(long p0, long p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {}
public KeyEvent(long p0, long p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9) {}
public KeyEvent(long p0, String p1, int p2, int p3) {}
public KeyEvent(android.view.KeyEvent p0) {}
 public KeyEvent(android.view.KeyEvent p0, long p1, int p2) {}
public final int getAction() { throw new RuntimeException("API stub"); }
public final int getModifiers() { throw new RuntimeException("API stub"); }
public static final int ACTION_DOWN = 0;
public static final int ACTION_UP = 1;
public static interface Callback {
public boolean onKeyDown(int p0, android.view.KeyEvent p1);
public boolean onKeyLongPress(int p0, android.view.KeyEvent p1);
public boolean onKeyMultiple(int p0, int p1, android.view.KeyEvent p2);
public boolean onKeyUp(int p0, android.view.KeyEvent p1);
}
}
