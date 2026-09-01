package android.content;
public interface SharedPreferences {
    java.util.Map<String,?> getAll();
    String getString(String k,String d); boolean getBoolean(String k,boolean d);
    int getInt(String k,int d); long getLong(String k,long d); Editor edit();
    interface Editor {
        Editor putString(String k,String v); Editor putBoolean(String k,boolean v);
        Editor putInt(String k,int v); Editor putLong(String k,long v);
        Editor putFloat(String k,float v); Editor putStringSet(String k,java.util.Set<String> v);
        Editor clear(); Editor remove(String k); boolean commit(); void apply();
    }
}
