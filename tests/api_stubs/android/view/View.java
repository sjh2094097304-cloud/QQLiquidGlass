package android.view;
public class View {
protected View() {}
public View(android.content.Context p0) {}
public View(android.content.Context p0, android.util.AttributeSet p1) {}
public View(android.content.Context p0, android.util.AttributeSet p1, int p2) {}
public View(android.content.Context p0, android.util.AttributeSet p1, int p2, int p3) {}
public android.view.ViewPropertyAnimator animate() { throw new RuntimeException("API stub"); }
public void clearFocus() { throw new RuntimeException("API stub"); }
public final <T extends android.view.View> T findViewById(int p0) { throw new RuntimeException("API stub"); }
public CharSequence getContentDescription() { throw new RuntimeException("API stub"); }
 public final android.content.Context getContext() { throw new RuntimeException("API stub"); }
public final int getHeight() { throw new RuntimeException("API stub"); }
public android.view.ViewGroup.LayoutParams getLayoutParams() { throw new RuntimeException("API stub"); }
public void getLocationOnScreen(int[] p0) { throw new RuntimeException("API stub"); }
public final android.view.ViewParent getParent() { throw new RuntimeException("API stub"); }
public android.content.res.Resources getResources() { throw new RuntimeException("API stub"); }
public int getVisibility() { throw new RuntimeException("API stub"); }
public final int getWidth() { throw new RuntimeException("API stub"); }
public void getWindowVisibleDisplayFrame(android.graphics.Rect p0) { throw new RuntimeException("API stub"); }
public boolean hasOnClickListeners() { throw new RuntimeException("API stub"); }
 public void invalidate(android.graphics.Rect p0) { throw new RuntimeException("API stub"); }
 public void invalidate(int p0, int p1, int p2, int p3) { throw new RuntimeException("API stub"); }
public void invalidate() { throw new RuntimeException("API stub"); }
public boolean isClickable() { throw new RuntimeException("API stub"); }
public boolean isLaidOut() { throw new RuntimeException("API stub"); }
public void layout(int p0, int p1, int p2, int p3) { throw new RuntimeException("API stub"); }
protected void onDraw(android.graphics.Canvas p0) { throw new RuntimeException("API stub"); }
public boolean performClick() { throw new RuntimeException("API stub"); }
public boolean post(Runnable p0) { throw new RuntimeException("API stub"); }
public boolean postDelayed(Runnable p0, long p1) { throw new RuntimeException("API stub"); }
public boolean removeCallbacks(Runnable p0) { throw new RuntimeException("API stub"); }
public final boolean requestFocus() { throw new RuntimeException("API stub"); }
public final boolean requestFocus(int p0) { throw new RuntimeException("API stub"); }
public boolean requestFocus(int p0, android.graphics.Rect p1) { throw new RuntimeException("API stub"); }
public void setAlpha(float p0) { throw new RuntimeException("API stub"); }
public void setBackground(android.graphics.drawable.Drawable p0) { throw new RuntimeException("API stub"); }
public void setBackgroundColor(int p0) { throw new RuntimeException("API stub"); }
 public void setBackgroundDrawable(android.graphics.drawable.Drawable p0) { throw new RuntimeException("API stub"); }
public void setClickable(boolean p0) { throw new RuntimeException("API stub"); }
public void setClipToOutline(boolean p0) { throw new RuntimeException("API stub"); }
public void setContentDescription(CharSequence p0) { throw new RuntimeException("API stub"); }
public void setElevation(float p0) { throw new RuntimeException("API stub"); }
public void setEnabled(boolean p0) { throw new RuntimeException("API stub"); }
public void setFocusable(boolean p0) { throw new RuntimeException("API stub"); }
public void setFocusable(int p0) { throw new RuntimeException("API stub"); }
public void setId(int p0) { throw new RuntimeException("API stub"); }
public void setImportantForAccessibility(int p0) { throw new RuntimeException("API stub"); }
public void setLayoutParams(android.view.ViewGroup.LayoutParams p0) { throw new RuntimeException("API stub"); }
public void setMinimumHeight(int p0) { throw new RuntimeException("API stub"); }
public void setMinimumWidth(int p0) { throw new RuntimeException("API stub"); }
public void setOnClickListener(android.view.View.OnClickListener p0) { throw new RuntimeException("API stub"); }
public void setOnTouchListener(android.view.View.OnTouchListener p0) { throw new RuntimeException("API stub"); }
public void setPadding(int p0, int p1, int p2, int p3) { throw new RuntimeException("API stub"); }
public void setRenderEffect(android.graphics.RenderEffect p0) { throw new RuntimeException("API stub"); }
public void setSelected(boolean p0) { throw new RuntimeException("API stub"); }
public void setTranslationX(float p0) { throw new RuntimeException("API stub"); }
public void setTranslationY(float p0) { throw new RuntimeException("API stub"); }
public static final int IMPORTANT_FOR_ACCESSIBILITY_NO = 2;
public static final int VISIBLE = 0;
public static interface OnCreateContextMenuListener {
public void onCreateContextMenu(android.view.ContextMenu p0, android.view.View p1, android.view.ContextMenu.ContextMenuInfo p2);
}
public static interface OnClickListener {
public void onClick(android.view.View p0);
}
public static interface OnTouchListener {
public boolean onTouch(android.view.View p0, android.view.MotionEvent p1);
}
}
