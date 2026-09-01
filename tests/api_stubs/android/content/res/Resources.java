package android.content.res;
public class Resources {
protected Resources() {}
 public Resources(android.content.res.AssetManager p0, android.util.DisplayMetrics p1, android.content.res.Configuration p2) {}
public boolean getBoolean(int p0) throws android.content.res.Resources.NotFoundException { throw new RuntimeException("API stub"); }
public android.content.res.Configuration getConfiguration() { throw new RuntimeException("API stub"); }
public android.util.DisplayMetrics getDisplayMetrics() { throw new RuntimeException("API stub"); }
 public android.graphics.drawable.Drawable getDrawable(int p0) throws android.content.res.Resources.NotFoundException { throw new RuntimeException("API stub"); }
public android.graphics.drawable.Drawable getDrawable(int p0, android.content.res.Resources.Theme p1) throws android.content.res.Resources.NotFoundException { throw new RuntimeException("API stub"); }
 public int getIdentifier(String p0, String p1, String p2) { throw new RuntimeException("API stub"); }
 public int[] getIntArray(int p0) throws android.content.res.Resources.NotFoundException { throw new RuntimeException("API stub"); }
public String getResourceTypeName(int p0) throws android.content.res.Resources.NotFoundException { throw new RuntimeException("API stub"); }
 public String getString(int p0) throws android.content.res.Resources.NotFoundException { throw new RuntimeException("API stub"); }
 public String getString(int p0, java.lang.Object... p1) throws android.content.res.Resources.NotFoundException { throw new RuntimeException("API stub"); }
 public CharSequence getText(int p0) throws android.content.res.Resources.NotFoundException { throw new RuntimeException("API stub"); }
public CharSequence getText(int p0, CharSequence p1) { throw new RuntimeException("API stub"); }
public void getValue(int p0, android.util.TypedValue p1, boolean p2) throws android.content.res.Resources.NotFoundException { throw new RuntimeException("API stub"); }
 public void getValue(String p0, android.util.TypedValue p1, boolean p2) throws android.content.res.Resources.NotFoundException { throw new RuntimeException("API stub"); }
public class Theme {
protected Theme() {}
public android.graphics.drawable.Drawable getDrawable(int p0) throws android.content.res.Resources.NotFoundException { throw new RuntimeException("API stub"); }
public android.content.res.Resources getResources() { throw new RuntimeException("API stub"); }
}
public static class NotFoundException extends java.lang.RuntimeException {
public NotFoundException() {}
public NotFoundException(String p0) {}
public NotFoundException(String p0, Exception p1) {}
}
}
