package android.widget;
public class TextView extends android.view.View {
protected TextView() {}
public TextView(android.content.Context p0) {}
public TextView(android.content.Context p0, android.util.AttributeSet p1) {}
public TextView(android.content.Context p0, android.util.AttributeSet p1, int p2) {}
public TextView(android.content.Context p0, android.util.AttributeSet p1, int p2, int p3) {}
public void addTextChangedListener(android.text.TextWatcher p0) { throw new RuntimeException("API stub"); }
public final void append(CharSequence p0) { throw new RuntimeException("API stub"); }
public void append(CharSequence p0, int p1, int p2) { throw new RuntimeException("API stub"); }
public CharSequence getText() { throw new RuntimeException("API stub"); }
public int length() { throw new RuntimeException("API stub"); }
protected void onTextChanged(CharSequence p0, int p1, int p2, int p3) { throw new RuntimeException("API stub"); }
public void setAllCaps(boolean p0) { throw new RuntimeException("API stub"); }
public void setGravity(int p0) { throw new RuntimeException("API stub"); }
public final void setHint(CharSequence p0) { throw new RuntimeException("API stub"); }
public final void setHint(int p0) { throw new RuntimeException("API stub"); }
public final void setHintTextColor(int p0) { throw new RuntimeException("API stub"); }
public final void setHintTextColor(android.content.res.ColorStateList p0) { throw new RuntimeException("API stub"); }
public void setHorizontallyScrolling(boolean p0) { throw new RuntimeException("API stub"); }
public void setInputType(int p0) { throw new RuntimeException("API stub"); }
public void setLineSpacing(float p0, float p1) { throw new RuntimeException("API stub"); }
public void setMaxLines(int p0) { throw new RuntimeException("API stub"); }
public void setMaxWidth(int p0) { throw new RuntimeException("API stub"); }
public void setMinHeight(int p0) { throw new RuntimeException("API stub"); }
public void setMinLines(int p0) { throw new RuntimeException("API stub"); }
public void setSingleLine() { throw new RuntimeException("API stub"); }
public void setSingleLine(boolean p0) { throw new RuntimeException("API stub"); }
public final void setText(CharSequence p0) { throw new RuntimeException("API stub"); }
public void setText(CharSequence p0, android.widget.TextView.BufferType p1) { throw new RuntimeException("API stub"); }
public final void setText(char[] p0, int p1, int p2) { throw new RuntimeException("API stub"); }
public final void setText(int p0) { throw new RuntimeException("API stub"); }
public final void setText(int p0, android.widget.TextView.BufferType p1) { throw new RuntimeException("API stub"); }
public void setTextColor(int p0) { throw new RuntimeException("API stub"); }
public void setTextColor(android.content.res.ColorStateList p0) { throw new RuntimeException("API stub"); }
public void setTextIsSelectable(boolean p0) { throw new RuntimeException("API stub"); }
public void setTextSize(float p0) { throw new RuntimeException("API stub"); }
public void setTextSize(int p0, float p1) { throw new RuntimeException("API stub"); }
public void setTypeface(android.graphics.Typeface p0, int p1) { throw new RuntimeException("API stub"); }
public void setTypeface(android.graphics.Typeface p0) { throw new RuntimeException("API stub"); }
public enum BufferType {
EDITABLE, NORMAL, SPANNABLE;
}
}
