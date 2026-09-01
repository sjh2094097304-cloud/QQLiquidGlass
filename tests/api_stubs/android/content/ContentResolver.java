package android.content;
public abstract class ContentResolver {
protected ContentResolver() {}
public ContentResolver(android.content.Context p0) {}
 public final android.os.Bundle call(android.net.Uri p0, String p1, String p2, android.os.Bundle p3) { throw new RuntimeException("API stub"); }
 public final android.os.Bundle call(String p0, String p1, String p2, android.os.Bundle p3) { throw new RuntimeException("API stub"); }
public final int delete(android.net.Uri p0, String p1, String[] p2) { throw new RuntimeException("API stub"); }
public final int delete(android.net.Uri p0, android.os.Bundle p1) { throw new RuntimeException("API stub"); }
 public final String getType(android.net.Uri p0) { throw new RuntimeException("API stub"); }
 public final android.net.Uri insert(android.net.Uri p0, android.content.ContentValues p1) { throw new RuntimeException("API stub"); }
 public final android.net.Uri insert(android.net.Uri p0, android.content.ContentValues p1, android.os.Bundle p2) { throw new RuntimeException("API stub"); }
 public final android.database.Cursor query(android.net.Uri p0, String[] p1, String p2, String[] p3, String p4) { throw new RuntimeException("API stub"); }
 public final android.database.Cursor query(android.net.Uri p0, String[] p1, String p2, String[] p3, String p4, android.os.CancellationSignal p5) { throw new RuntimeException("API stub"); }
 public final android.database.Cursor query(android.net.Uri p0, String[] p1, android.os.Bundle p2, android.os.CancellationSignal p3) { throw new RuntimeException("API stub"); }
public final boolean refresh(android.net.Uri p0, android.os.Bundle p1, android.os.CancellationSignal p2) { throw new RuntimeException("API stub"); }
public final int update(android.net.Uri p0, android.content.ContentValues p1, String p2, String[] p3) { throw new RuntimeException("API stub"); }
public final int update(android.net.Uri p0, android.content.ContentValues p1, android.os.Bundle p2) { throw new RuntimeException("API stub"); }
 public static android.content.ContentResolver wrap(android.content.ContentProvider p0) { throw new RuntimeException("API stub"); }
 public static android.content.ContentResolver wrap(android.content.ContentProviderClient p0) { throw new RuntimeException("API stub"); }
}
