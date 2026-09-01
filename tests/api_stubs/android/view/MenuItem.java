package android.view;
public interface MenuItem {
public boolean collapseActionView();
public boolean expandActionView();
 public android.view.ActionProvider getActionProvider();
 public android.view.View getActionView();
public default int getAlphabeticModifiers() { throw new RuntimeException("API stub"); }
public char getAlphabeticShortcut();
 public default CharSequence getContentDescription() { throw new RuntimeException("API stub"); }
public int getGroupId();
 public android.graphics.drawable.Drawable getIcon();
 public default android.graphics.BlendMode getIconTintBlendMode() { throw new RuntimeException("API stub"); }
 public default android.content.res.ColorStateList getIconTintList() { throw new RuntimeException("API stub"); }
 public default android.graphics.PorterDuff.Mode getIconTintMode() { throw new RuntimeException("API stub"); }
 public android.content.Intent getIntent();
public int getItemId();
 public android.view.ContextMenu.ContextMenuInfo getMenuInfo();
public default int getNumericModifiers() { throw new RuntimeException("API stub"); }
public char getNumericShortcut();
public int getOrder();
 public android.view.SubMenu getSubMenu();
 public CharSequence getTitle();
 public CharSequence getTitleCondensed();
 public default CharSequence getTooltipText() { throw new RuntimeException("API stub"); }
public boolean hasSubMenu();
public boolean isActionViewExpanded();
public boolean isCheckable();
public boolean isChecked();
public boolean isEnabled();
public boolean isVisible();
 public android.view.MenuItem setActionProvider(android.view.ActionProvider p0);
 public android.view.MenuItem setActionView(android.view.View p0);
 public android.view.MenuItem setActionView(int p0);
 public android.view.MenuItem setAlphabeticShortcut(char p0);
 public default android.view.MenuItem setAlphabeticShortcut(char p0, int p1) { throw new RuntimeException("API stub"); }
 public android.view.MenuItem setCheckable(boolean p0);
 public android.view.MenuItem setChecked(boolean p0);
 public default android.view.MenuItem setContentDescription(CharSequence p0) { throw new RuntimeException("API stub"); }
 public android.view.MenuItem setEnabled(boolean p0);
 public android.view.MenuItem setIcon(android.graphics.drawable.Drawable p0);
 public android.view.MenuItem setIcon(int p0);
 public default android.view.MenuItem setIconTintBlendMode(android.graphics.BlendMode p0) { throw new RuntimeException("API stub"); }
 public default android.view.MenuItem setIconTintList(android.content.res.ColorStateList p0) { throw new RuntimeException("API stub"); }
 public default android.view.MenuItem setIconTintMode(android.graphics.PorterDuff.Mode p0) { throw new RuntimeException("API stub"); }
 public android.view.MenuItem setIntent(android.content.Intent p0);
 public android.view.MenuItem setNumericShortcut(char p0);
 public default android.view.MenuItem setNumericShortcut(char p0, int p1) { throw new RuntimeException("API stub"); }
 public android.view.MenuItem setOnActionExpandListener(android.view.MenuItem.OnActionExpandListener p0);
 public android.view.MenuItem setOnMenuItemClickListener(android.view.MenuItem.OnMenuItemClickListener p0);
 public android.view.MenuItem setShortcut(char p0, char p1);
 public default android.view.MenuItem setShortcut(char p0, char p1, int p2, int p3) { throw new RuntimeException("API stub"); }
public void setShowAsAction(int p0);
 public android.view.MenuItem setShowAsActionFlags(int p0);
 public android.view.MenuItem setTitle(CharSequence p0);
 public android.view.MenuItem setTitle(int p0);
 public android.view.MenuItem setTitleCondensed(CharSequence p0);
 public default android.view.MenuItem setTooltipText(CharSequence p0) { throw new RuntimeException("API stub"); }
 public android.view.MenuItem setVisible(boolean p0);
public static interface OnActionExpandListener {
public boolean onMenuItemActionCollapse(android.view.MenuItem p0);
public boolean onMenuItemActionExpand(android.view.MenuItem p0);
}
public static interface OnMenuItemClickListener {
public boolean onMenuItemClick(android.view.MenuItem p0);
}
}
