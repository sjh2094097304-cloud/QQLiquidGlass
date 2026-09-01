package android.view;
public abstract class Window {
protected Window() {}
public Window(android.content.Context p0) {}
public void addFlags(int p0) { throw new RuntimeException("API stub"); }
public <T extends android.view.View> T findViewById(int p0) { throw new RuntimeException("API stub"); }
public final android.view.WindowManager.LayoutParams getAttributes() { throw new RuntimeException("API stub"); }
 public final android.content.Context getContext() { throw new RuntimeException("API stub"); }
 public android.view.View getDecorView() { throw new RuntimeException("API stub"); }
public void setAttributes(android.view.WindowManager.LayoutParams p0) { throw new RuntimeException("API stub"); }
public void setBackgroundBlurRadius(int p0) { throw new RuntimeException("API stub"); }
public void setBackgroundDrawable(android.graphics.drawable.Drawable p0) { throw new RuntimeException("API stub"); }
public void setClipToOutline(boolean p0) { throw new RuntimeException("API stub"); }
public void setContentView(int p0) { throw new RuntimeException("API stub"); }
public void setContentView(android.view.View p0) { throw new RuntimeException("API stub"); }
public void setContentView(android.view.View p0, android.view.ViewGroup.LayoutParams p1) { throw new RuntimeException("API stub"); }
public void setElevation(float p0) { throw new RuntimeException("API stub"); }
public void setGravity(int p0) { throw new RuntimeException("API stub"); }
public static interface Callback {
public boolean dispatchGenericMotionEvent(android.view.MotionEvent p0);
public boolean dispatchKeyEvent(android.view.KeyEvent p0);
public boolean dispatchKeyShortcutEvent(android.view.KeyEvent p0);
public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent p0);
public boolean dispatchTouchEvent(android.view.MotionEvent p0);
public boolean dispatchTrackballEvent(android.view.MotionEvent p0);
public void onActionModeFinished(android.view.ActionMode p0);
public void onActionModeStarted(android.view.ActionMode p0);
public void onAttachedToWindow();
public void onContentChanged();
public boolean onCreatePanelMenu(int p0, android.view.Menu p1);
 public android.view.View onCreatePanelView(int p0);
public void onDetachedFromWindow();
public boolean onMenuItemSelected(int p0, android.view.MenuItem p1);
public boolean onMenuOpened(int p0, android.view.Menu p1);
public void onPanelClosed(int p0, android.view.Menu p1);
public default void onPointerCaptureChanged(boolean p0) { throw new RuntimeException("API stub"); }
public boolean onPreparePanel(int p0, android.view.View p1, android.view.Menu p2);
public default void onProvideKeyboardShortcuts(java.util.List<android.view.KeyboardShortcutGroup> p0, android.view.Menu p1, int p2) { throw new RuntimeException("API stub"); }
public boolean onSearchRequested();
public boolean onSearchRequested(android.view.SearchEvent p0);
public void onWindowAttributesChanged(android.view.WindowManager.LayoutParams p0);
public void onWindowFocusChanged(boolean p0);
 public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback p0);
 public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback p0, int p1);
}
}
