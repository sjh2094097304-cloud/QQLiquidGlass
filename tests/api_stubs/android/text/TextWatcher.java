package android.text;
public interface TextWatcher extends android.text.NoCopySpan {
public void afterTextChanged(android.text.Editable p0);
public void beforeTextChanged(CharSequence p0, int p1, int p2, int p3);
public void onTextChanged(CharSequence p0, int p1, int p2, int p3);
}
