package android.content;
public abstract class ContentProvider {
public ContentProvider() {}
 public android.os.Bundle call(String p0, String p1, String p2, android.os.Bundle p3) { throw new RuntimeException("API stub"); }
 public android.os.Bundle call(String p0, String p1, android.os.Bundle p2) { throw new RuntimeException("API stub"); }
public int delete(android.net.Uri p0, String p1, String[] p2) { throw new RuntimeException("API stub"); }
public int delete(android.net.Uri p0, android.os.Bundle p1) { throw new RuntimeException("API stub"); }
 public final android.content.Context getContext() { throw new RuntimeException("API stub"); }
 public String getType(android.net.Uri p0) { throw new RuntimeException("API stub"); }
 public android.net.Uri insert(android.net.Uri p0, android.content.ContentValues p1) { throw new RuntimeException("API stub"); }
 public android.net.Uri insert(android.net.Uri p0, android.content.ContentValues p1, android.os.Bundle p2) { throw new RuntimeException("API stub"); }
public boolean onCreate() { throw new RuntimeException("API stub"); }
 public android.database.Cursor query(android.net.Uri p0, String[] p1, String p2, String[] p3, String p4) { throw new RuntimeException("API stub"); }
 public android.database.Cursor query(android.net.Uri p0, String[] p1, String p2, String[] p3, String p4, android.os.CancellationSignal p5) { throw new RuntimeException("API stub"); }
 public android.database.Cursor query(android.net.Uri p0, String[] p1, android.os.Bundle p2, android.os.CancellationSignal p3) { throw new RuntimeException("API stub"); }
public boolean refresh(android.net.Uri p0, android.os.Bundle p1, android.os.CancellationSignal p2) { throw new RuntimeException("API stub"); }
public int update(android.net.Uri p0, android.content.ContentValues p1, String p2, String[] p3) { throw new RuntimeException("API stub"); }
public int update(android.net.Uri p0, android.content.ContentValues p1, android.os.Bundle p2) { throw new RuntimeException("API stub"); }
}
