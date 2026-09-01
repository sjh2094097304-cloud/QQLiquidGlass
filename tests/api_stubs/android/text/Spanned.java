package android.text;
public interface Spanned extends java.lang.CharSequence {
public int getSpanEnd(Object p0);
public int getSpanFlags(Object p0);
public int getSpanStart(Object p0);
public <T> T[] getSpans(int p0, int p1, Class<T> p2);
public int nextSpanTransition(int p0, int p1, Class p2);
}
