package android.content;
import android.os.Bundle; import android.net.Uri;
public abstract class ContentResolver { public abstract Bundle call(Uri uri,String method,String arg,Bundle extras); }
