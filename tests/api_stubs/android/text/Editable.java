package android.text;
public interface Editable extends java.lang.CharSequence, java.lang.Appendable, android.text.GetChars, android.text.Spannable {
public android.text.Editable append(CharSequence p0);
public android.text.Editable append(CharSequence p0, int p1, int p2);
public android.text.Editable append(char p0);
public void clear();
public void clearSpans();
public android.text.Editable delete(int p0, int p1);
public android.text.InputFilter[] getFilters();
public android.text.Editable insert(int p0, CharSequence p1, int p2, int p3);
public android.text.Editable insert(int p0, CharSequence p1);
public android.text.Editable replace(int p0, int p1, CharSequence p2, int p3, int p4);
public android.text.Editable replace(int p0, int p1, CharSequence p2);
public void setFilters(android.text.InputFilter[] p0);
}
