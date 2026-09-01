package android.os;
public interface IBinder {
 public default void addFrozenStateChangeCallback(java.util.concurrent.Executor p0, android.os.IBinder.FrozenStateChangeCallback p1) throws android.os.RemoteException { throw new RuntimeException("API stub"); }
public void dump(java.io.FileDescriptor p0, String[] p1) throws android.os.RemoteException;
public void dumpAsync(java.io.FileDescriptor p0, String[] p1) throws android.os.RemoteException;
 public String getInterfaceDescriptor() throws android.os.RemoteException;
public static int getSuggestedMaxIpcSizeBytes() { throw new RuntimeException("API stub"); }
public boolean isBinderAlive();
public void linkToDeath(android.os.IBinder.DeathRecipient p0, int p1) throws android.os.RemoteException;
public boolean pingBinder();
 public android.os.IInterface queryLocalInterface(String p0);
 public default boolean removeFrozenStateChangeCallback(android.os.IBinder.FrozenStateChangeCallback p0) { throw new RuntimeException("API stub"); }
public boolean transact(int p0, android.os.Parcel p1, android.os.Parcel p2, int p3) throws android.os.RemoteException;
public boolean unlinkToDeath(android.os.IBinder.DeathRecipient p0, int p1);
public static interface FrozenStateChangeCallback {
public void onFrozenStateChanged(android.os.IBinder p0, int p1);
}
public static interface DeathRecipient {
public void binderDied();
public default void binderDied(android.os.IBinder p0) { throw new RuntimeException("API stub"); }
}
}
