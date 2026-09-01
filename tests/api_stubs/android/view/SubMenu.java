package android.view;
public interface SubMenu extends android.view.Menu {
public void clearHeader();
public android.view.MenuItem getItem();
public android.view.SubMenu setHeaderIcon(int p0);
public android.view.SubMenu setHeaderIcon(android.graphics.drawable.Drawable p0);
public android.view.SubMenu setHeaderTitle(int p0);
public android.view.SubMenu setHeaderTitle(CharSequence p0);
public android.view.SubMenu setHeaderView(android.view.View p0);
public android.view.SubMenu setIcon(int p0);
public android.view.SubMenu setIcon(android.graphics.drawable.Drawable p0);
}
