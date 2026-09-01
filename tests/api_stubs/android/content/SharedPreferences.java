package android.content;
public interface SharedPreferences {
public boolean contains(String p0);
public android.content.SharedPreferences.Editor edit();
public java.util.Map<java.lang.String,?> getAll();
public boolean getBoolean(String p0, boolean p1);
public float getFloat(String p0, float p1);
public int getInt(String p0, int p1);
public long getLong(String p0, long p1);
 public String getString(String p0, String p1);
 public java.util.Set<java.lang.String> getStringSet(String p0, java.util.Set<java.lang.String> p1);
public void registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener p0);
public void unregisterOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener p0);
public static interface Editor {
public void apply();
public android.content.SharedPreferences.Editor clear();
public boolean commit();
public android.content.SharedPreferences.Editor putBoolean(String p0, boolean p1);
public android.content.SharedPreferences.Editor putFloat(String p0, float p1);
public android.content.SharedPreferences.Editor putInt(String p0, int p1);
public android.content.SharedPreferences.Editor putLong(String p0, long p1);
public android.content.SharedPreferences.Editor putString(String p0, String p1);
public android.content.SharedPreferences.Editor putStringSet(String p0, java.util.Set<java.lang.String> p1);
public android.content.SharedPreferences.Editor remove(String p0);
}
public static interface OnSharedPreferenceChangeListener {
public void onSharedPreferenceChanged(android.content.SharedPreferences p0, String p1);
}
}
