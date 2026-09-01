package android.content;
public abstract class Context {
public Context() {}
public android.content.Context createPackageContext(String p0, int p1) throws android.content.pm.PackageManager.NameNotFoundException { throw new RuntimeException("API stub"); }
public android.content.Context getApplicationContext() { throw new RuntimeException("API stub"); }
public android.content.pm.ApplicationInfo getApplicationInfo() { throw new RuntimeException("API stub"); }
public ClassLoader getClassLoader() { throw new RuntimeException("API stub"); }
public android.content.ContentResolver getContentResolver() { throw new RuntimeException("API stub"); }
 public final android.graphics.drawable.Drawable getDrawable(int p0) { throw new RuntimeException("API stub"); }
public android.os.Looper getMainLooper() { throw new RuntimeException("API stub"); }
public android.content.pm.PackageManager getPackageManager() { throw new RuntimeException("API stub"); }
public String getPackageName() { throw new RuntimeException("API stub"); }
public android.content.res.Resources getResources() { throw new RuntimeException("API stub"); }
public android.content.SharedPreferences getSharedPreferences(String p0, int p1) { throw new RuntimeException("API stub"); }
 public final String getString(int p0) { throw new RuntimeException("API stub"); }
 public final String getString(int p0, java.lang.Object... p1) { throw new RuntimeException("API stub"); }
public Object getSystemService(String p0) { throw new RuntimeException("API stub"); }
public final <T> T getSystemService(Class<T> p0) { throw new RuntimeException("API stub"); }
 public final CharSequence getText(int p0) { throw new RuntimeException("API stub"); }
public android.content.res.Resources.Theme getTheme() { throw new RuntimeException("API stub"); }
public void startActivity(android.content.Intent p0) { throw new RuntimeException("API stub"); }
public void startActivity(android.content.Intent p0, android.os.Bundle p1) { throw new RuntimeException("API stub"); }
public static final String CLIPBOARD_SERVICE = "clipboard";
public static final int CONTEXT_IGNORE_SECURITY = 2;
public static final int CONTEXT_INCLUDE_CODE = 1;
public static final int MODE_PRIVATE = 0;
}
