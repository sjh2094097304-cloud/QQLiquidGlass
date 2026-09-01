package android.os;
import java.util.*;
public class Bundle {
    public static final Bundle EMPTY = new Bundle();
    private final Map<String,Object> data = new HashMap<>();
    public Bundle() {}
    public Bundle(Bundle other) { data.putAll(other.data); }
    public java.util.Set<String> keySet(){return data.keySet();}
    public Object get(String k){return data.get(k);}
    public void remove(String k){data.remove(k);}
    public boolean isEmpty() { return data.isEmpty(); }
    public boolean containsKey(String key) { return data.containsKey(key); }
    public void setClassLoader(ClassLoader loader) {}
    public void putBoolean(String k,boolean v) { data.put(k,v); }
    public void putInt(String k,int v) { data.put(k,v); }
    public void putLong(String k,long v) { data.put(k,v); }
    public void putString(String k,String v) { data.put(k,v); }
    public void putIntArray(String k,int[] v) { data.put(k,v); }
    public boolean getBoolean(String k,boolean d) { Object v=data.get(k); return v instanceof Boolean?(Boolean)v:d; }
    public int getInt(String k,int d) { Object v=data.get(k); return v instanceof Integer?(Integer)v:d; }
    public long getLong(String k,long d) { Object v=data.get(k); return v instanceof Long?(Long)v:d; }
    public String getString(String k,String d) { Object v=data.get(k); return v instanceof String?(String)v:d; }
    public int[] getIntArray(String k) { Object v=data.get(k); return v instanceof int[]?(int[])v:null; }
    public String toString() { return data.toString(); }
}
