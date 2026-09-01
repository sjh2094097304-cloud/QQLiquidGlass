package android.view;
public abstract class ActionMode {
public ActionMode() {}
public void finish() { throw new RuntimeException("API stub"); }
public int getType() { throw new RuntimeException("API stub"); }
public void invalidate() { throw new RuntimeException("API stub"); }
public static interface Callback {
public boolean onActionItemClicked(android.view.ActionMode p0, android.view.MenuItem p1);
public boolean onCreateActionMode(android.view.ActionMode p0, android.view.Menu p1);
public void onDestroyActionMode(android.view.ActionMode p0);
public boolean onPrepareActionMode(android.view.ActionMode p0, android.view.Menu p1);
}
}
