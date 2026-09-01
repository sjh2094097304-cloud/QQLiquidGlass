package android.content;
public class ContentProviderClient {
protected ContentProviderClient() {}
 public android.os.Bundle call(String p0, String p1, android.os.Bundle p2) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
 public android.os.Bundle call(String p0, String p1, String p2, android.os.Bundle p3) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
public void close() { throw new RuntimeException("API stub"); }
public int delete(android.net.Uri p0, String p1, String[] p2) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
public int delete(android.net.Uri p0, android.os.Bundle p1) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
 public String getType(android.net.Uri p0) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
 public android.net.Uri insert(android.net.Uri p0, android.content.ContentValues p1) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
 public android.net.Uri insert(android.net.Uri p0, android.content.ContentValues p1, android.os.Bundle p2) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
 public android.database.Cursor query(android.net.Uri p0, String[] p1, String p2, String[] p3, String p4) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
 public android.database.Cursor query(android.net.Uri p0, String[] p1, String p2, String[] p3, String p4, android.os.CancellationSignal p5) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
 public android.database.Cursor query(android.net.Uri p0, String[] p1, android.os.Bundle p2, android.os.CancellationSignal p3) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
public boolean refresh(android.net.Uri p0, android.os.Bundle p1, android.os.CancellationSignal p2) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
 public boolean release() { throw new RuntimeException("API stub"); }
public int update(android.net.Uri p0, android.content.ContentValues p1, String p2, String[] p3) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
public int update(android.net.Uri p0, android.content.ContentValues p1, android.os.Bundle p2) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
}
