package android.content;
public interface DialogInterface {
public void cancel();
public void dismiss();
public static interface OnCancelListener {
public void onCancel(android.content.DialogInterface p0);
}
public static interface OnDismissListener {
public void onDismiss(android.content.DialogInterface p0);
}
public static interface OnShowListener {
public void onShow(android.content.DialogInterface p0);
}
}
