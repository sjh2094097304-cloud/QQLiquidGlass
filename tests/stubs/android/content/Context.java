package android.content;
import android.content.pm.PackageManager;
public abstract class Context {
    public static final int MODE_PRIVATE=0;
    public static final String ACTIVITY_SERVICE="activity";
    public Context getApplicationContext(){return this;}
    public abstract SharedPreferences getSharedPreferences(String name,int mode);
    public abstract ContentResolver getContentResolver();
    public PackageManager getPackageManager(){return new PackageManager();}
    public Object getSystemService(String name){return null;}
    public String getPackageName(){return "com.qiutian.bianpaobubble.v36";}
}
