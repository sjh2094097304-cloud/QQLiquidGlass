package android.view;
public interface Menu {
public android.view.MenuItem add(CharSequence p0);
public android.view.MenuItem add(int p0);
public android.view.MenuItem add(int p0, int p1, int p2, CharSequence p3);
public android.view.MenuItem add(int p0, int p1, int p2, int p3);
public int addIntentOptions(int p0, int p1, int p2, android.content.ComponentName p3, android.content.Intent[] p4, android.content.Intent p5, int p6, android.view.MenuItem[] p7);
public android.view.SubMenu addSubMenu(CharSequence p0);
public android.view.SubMenu addSubMenu(int p0);
public android.view.SubMenu addSubMenu(int p0, int p1, int p2, CharSequence p3);
public android.view.SubMenu addSubMenu(int p0, int p1, int p2, int p3);
public void clear();
public void close();
public android.view.MenuItem findItem(int p0);
public android.view.MenuItem getItem(int p0);
public boolean hasVisibleItems();
public boolean isShortcutKey(int p0, android.view.KeyEvent p1);
public boolean performIdentifierAction(int p0, int p1);
public boolean performShortcut(int p0, android.view.KeyEvent p1, int p2);
public void removeGroup(int p0);
public void removeItem(int p0);
public void setGroupCheckable(int p0, boolean p1, boolean p2);
public default void setGroupDividerEnabled(boolean p0) { throw new RuntimeException("API stub"); }
public void setGroupEnabled(int p0, boolean p1);
public void setGroupVisible(int p0, boolean p1);
public void setQwertyMode(boolean p0);
public int size();
}
