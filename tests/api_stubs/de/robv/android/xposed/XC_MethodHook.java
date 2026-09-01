package de.robv.android.xposed;
public abstract class XC_MethodHook {
 public XC_MethodHook(){} public XC_MethodHook(int priority){}
 protected void beforeHookedMethod(MethodHookParam p)throws Throwable{} protected void afterHookedMethod(MethodHookParam p)throws Throwable{}
 public static class MethodHookParam { public Object thisObject; public Object[] args; public java.lang.reflect.Member method;
 public Object getResult(){return null;} public void setResult(Object o){} public void setObjectExtra(String k,Object v){} public Object getObjectExtra(String k){return null;} }
 public class Unhook {} }

