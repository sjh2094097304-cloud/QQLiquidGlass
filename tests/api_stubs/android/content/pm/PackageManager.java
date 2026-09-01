package android.content.pm;
public abstract class PackageManager {
 public PackageManager() {}
 public android.content.pm.ApplicationInfo getApplicationInfo(String p0, int p1) throws android.content.pm.PackageManager.NameNotFoundException { throw new RuntimeException("API stub"); }
 public android.content.pm.ApplicationInfo getApplicationInfo(String p0, android.content.pm.PackageManager.ApplicationInfoFlags p1) throws android.content.pm.PackageManager.NameNotFoundException { throw new RuntimeException("API stub"); }
 public android.graphics.drawable.Drawable getDrawable(String p0, int p1, android.content.pm.ApplicationInfo p2) { throw new RuntimeException("API stub"); }
 public android.content.pm.PackageInfo getPackageArchiveInfo(String p0, int p1) { throw new RuntimeException("API stub"); }
 public android.content.pm.PackageInfo getPackageArchiveInfo(String p0, android.content.pm.PackageManager.PackageInfoFlags p1) { throw new RuntimeException("API stub"); }
public android.content.pm.PackageInfo getPackageInfo(String p0, int p1) throws android.content.pm.PackageManager.NameNotFoundException { throw new RuntimeException("API stub"); }
 public android.content.pm.PackageInfo getPackageInfo(String p0, android.content.pm.PackageManager.PackageInfoFlags p1) throws android.content.pm.PackageManager.NameNotFoundException { throw new RuntimeException("API stub"); }
public android.content.pm.PackageInfo getPackageInfo(android.content.pm.VersionedPackage p0, int p1) throws android.content.pm.PackageManager.NameNotFoundException { throw new RuntimeException("API stub"); }
 public android.content.pm.PackageInfo getPackageInfo(android.content.pm.VersionedPackage p0, android.content.pm.PackageManager.PackageInfoFlags p1) throws android.content.pm.PackageManager.NameNotFoundException { throw new RuntimeException("API stub"); }
 public String[] getPackagesForUid(int p0) { throw new RuntimeException("API stub"); }
 public CharSequence getText(String p0, int p1, android.content.pm.ApplicationInfo p2) { throw new RuntimeException("API stub"); }
 public android.content.pm.ResolveInfo resolveActivity(android.content.Intent p0, int p1) { throw new RuntimeException("API stub"); }
 public android.content.pm.ResolveInfo resolveActivity(android.content.Intent p0, android.content.pm.PackageManager.ResolveInfoFlags p1) { throw new RuntimeException("API stub"); }
 public static final int GET_SIGNATURES = 64;
public static final int GET_SIGNING_CERTIFICATES = 134217728;
public static class NameNotFoundException extends android.util.AndroidException {
public NameNotFoundException() {}
public NameNotFoundException(String p0) {}
}
public static class ApplicationInfoFlags {
protected ApplicationInfoFlags() {}
public long getValue() { throw new RuntimeException("API stub"); }
 public static android.content.pm.PackageManager.ApplicationInfoFlags of(long p0) { throw new RuntimeException("API stub"); }
}
public static class PackageInfoFlags {
protected PackageInfoFlags() {}
public long getValue() { throw new RuntimeException("API stub"); }
 public static android.content.pm.PackageManager.PackageInfoFlags of(long p0) { throw new RuntimeException("API stub"); }
}
public static class ResolveInfoFlags {
protected ResolveInfoFlags() {}
public long getValue() { throw new RuntimeException("API stub"); }
 public static android.content.pm.PackageManager.ResolveInfoFlags of(long p0) { throw new RuntimeException("API stub"); }
}
}
