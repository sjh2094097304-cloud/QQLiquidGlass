package android.content;
public class Intent {
public Intent() {}
public Intent(android.content.Intent p0) {}
public Intent(String p0) {}
public Intent(String p0, android.net.Uri p1) {}
public Intent(android.content.Context p0, Class<?> p1) {}
public Intent(String p0, android.net.Uri p1, android.content.Context p2, Class<?> p3) {}
 public android.content.Intent addFlags(int p0) { throw new RuntimeException("API stub"); }
public Object clone() { throw new RuntimeException("API stub"); }
public static android.content.Intent createChooser(android.content.Intent p0, CharSequence p1) { throw new RuntimeException("API stub"); }
public static android.content.Intent createChooser(android.content.Intent p0, CharSequence p1, android.content.IntentSender p2) { throw new RuntimeException("API stub"); }
 public String getAction() { throw new RuntimeException("API stub"); }
 public android.net.Uri getData() { throw new RuntimeException("API stub"); }
 public String getDataString() { throw new RuntimeException("API stub"); }
 public android.os.Bundle getExtras() { throw new RuntimeException("API stub"); }
 public String getIdentifier() { throw new RuntimeException("API stub"); }
 public static android.content.Intent getIntent(String p0) throws java.net.URISyntaxException { throw new RuntimeException("API stub"); }
 public String getType() { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, boolean p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, byte p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, char p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, short p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, int p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, long p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, float p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, double p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, String p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, CharSequence p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, android.os.Parcelable p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, android.os.Parcelable[] p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, java.io.Serializable p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, boolean[] p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, byte[] p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, short[] p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, char[] p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, int[] p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, long[] p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, float[] p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, double[] p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, String[] p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, CharSequence[] p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent putExtra(String p0, android.os.Bundle p1) { throw new RuntimeException("API stub"); }
public android.content.ComponentName resolveActivity(android.content.pm.PackageManager p0) { throw new RuntimeException("API stub"); }
 public android.content.Intent setClassName(android.content.Context p0, String p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent setClassName(String p0, String p1) { throw new RuntimeException("API stub"); }
 public android.content.Intent setPackage(String p0) { throw new RuntimeException("API stub"); }
public static final String ACTION_SENDTO = "android.intent.action.SENDTO";
public static final String ACTION_VIEW = "android.intent.action.VIEW";
public static final String EXTRA_EMAIL = "android.intent.extra.EMAIL";
public static final String EXTRA_SUBJECT = "android.intent.extra.SUBJECT";
public static final String EXTRA_TEXT = "android.intent.extra.TEXT";
public static final int FLAG_ACTIVITY_NEW_TASK = 268435456;
}
