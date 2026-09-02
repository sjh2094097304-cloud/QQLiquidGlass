package io.github.liuran001.mmliquidglass;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/** Native settings data integration. Never reparents QQ's RecyclerView. */
final class NativeSettingsBridge {
    static final int ITEM_ID=0x6c670301;
    private static final Set<Object> ownGroups=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<Activity> installed=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<Method> hooks=new HashSet<>();
    static boolean hasEntry(Activity a) { return installed.contains(a); }
    static void install(ClassLoader loader) {
        for(String name:new String[]{"com.tencent.mobileqq.setting.main.MainSettingConfigProvider",
                "com.tencent.mobileqq.setting.main.NewSettingConfigProvider","com.tencent.mobileqq.setting.main.b"}) {
            try {
                Class<?> c=loader.loadClass(name);
                for(Method m:c.getDeclaredMethods()) {
                    Class<?>[] p=m.getParameterTypes();
                    if(p.length!=1 || p[0]!=Context.class || !List.class.isAssignableFrom(m.getReturnType()) || !hooks.add(m)) continue;
                    LiquidGlassModule.hookIntercept(m,chain->{
                        Object result=chain.proceed();
                        try {
                            Context context=(Context)chain.getArg(0);
                            if(result instanceof List) return add(context,(List<?>)result,loader);
                        } catch(Throwable t) { FeedbackLog.error("SETTINGS_NATIVE_FAILED",t); }
                        return result;
                    });
                }
            } catch(ClassNotFoundException ignored) { }
            catch(Throwable t) { FeedbackLog.error("SETTINGS_PROVIDER",t); }
        }
    }
    private static Object add(Context context,List<?> groups,ClassLoader loader) throws Exception {
        if(groups.isEmpty()) return groups;
        for(Object group:groups) if(ownGroups.contains(group)) return groups;
        Constructor<?> itemConstructor=null, groupConstructor=null;
        for(Object group:groups) {
            if(group==null) continue;
            if(groupConstructor==null) {
                for(Constructor<?> c:group.getClass().getDeclaredConstructors()) {
                    Class<?>[] p=c.getParameterTypes();
                    if((p.length==3 || p.length==5) && p[0]==List.class && p[1]==CharSequence.class && p[2]==CharSequence.class
                            && (p.length==3 || p[3]==int.class && p[4].getName().endsWith("DefaultConstructorMarker"))) groupConstructor=c;
                }
            }
            // Prefer a real processor already used by this version's settings.
            for(Class<?> c=group.getClass();c!=null && c!=Object.class;c=c.getSuperclass()) {
                for(Field field:c.getDeclaredFields()) {
                    if(!List.class.isAssignableFrom(field.getType()) || Modifier.isStatic(field.getModifiers())) continue;
                    field.setAccessible(true);
                    Object value=field.get(group);
                    if(!(value instanceof List)) continue;
                    List<?> items=(List<?>)value;
                    for(int i=0;i<Math.min(items.size(),64) && itemConstructor==null;i++) {
                        Object item=items.get(i);
                        if(item!=null) itemConstructor=itemConstructor(item.getClass());
                    }
                }
            }
        }
        if(itemConstructor==null) {
            // Bounded package-local compatibility probe, not a whole-dex scan.
            for(char suffix='a';suffix<='z' && itemConstructor==null;suffix++) {
                try { itemConstructor=itemConstructor(loader.loadClass("com.tencent.mobileqq.setting.processor."+suffix)); }
                catch(ClassNotFoundException ignored) { }
            }
        }
        if(itemConstructor==null || groupConstructor==null) {
            QqSettingsEntry.status="原生设置数据结构未识别，可长按底栏打开设置";
            FeedbackLog.event("SETTINGS_STRUCTURE","native processor/group unavailable");
            return groups;
        }
        Activity a=activity(context);
        if(a==null) return groups;
        int icon=context.getResources().getIdentifier("qui_tuning","drawable",context.getPackageName());
        itemConstructor.setAccessible(true);
        Object item=itemConstructor.getParameterTypes().length==5
                ?itemConstructor.newInstance(context,ITEM_ID,"液态玻璃",icon,null)
                :itemConstructor.newInstance(context,ITEM_ID,"液态玻璃",icon);
        boolean callback=false;
        ArrayList<Method> actions=new ArrayList<>();
        for(Method m:item.getClass().getMethods()) {
            if(m.getReturnType()!=void.class || m.getParameterTypes().length!=1) continue;
            Class<?> function=m.getParameterTypes()[0];
            if(!function.isInterface() || !function.getName().equals("kotlin.jvm.functions.Function0")) continue;
            actions.add(m);
        }
        // Supported QQ processors have one or two Function0 setters. The first
        // by name is the click setter; do not invoke unrelated callback setters.
        if(actions.isEmpty() || actions.size()>2) return groups;
        actions.sort(java.util.Comparator.comparing(Method::getName));
        for(Method m:actions.subList(0,1)) {
            Class<?> function=m.getParameterTypes()[0];
            Object unit=loader.loadClass("kotlin.Unit").getField("INSTANCE").get(null);
            Object listener=Proxy.newProxyInstance(loader,new Class<?>[]{function},(proxy,method,args)->{
                switch(method.getName()) {
                    case "invoke": a.runOnUiThread(()->QqSettingsEntry.show(a)); return unit;
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return args!=null && args.length==1 && proxy==args[0];
                    case "toString": return "LiquidGlassSettingsAction";
                    default: return null;
                }
            });
            m.setAccessible(true);
            m.invoke(item,listener);
            callback=true;
        }
        if(!callback) return groups;
        ArrayList<Object> children=new ArrayList<>(); children.add(item);
        groupConstructor.setAccessible(true);
        Object group=groupConstructor.getParameterTypes().length==3
                ?groupConstructor.newInstance(children,"","")
                :groupConstructor.newInstance(children,"","",6,null);
        ArrayList<Object> result=new ArrayList<>(groups);
        result.add(Math.min(1,result.size()),group);
        ownGroups.add(group);
        installed.add(a);
        QqSettingsEntry.status="已接入 QQ 原生设置数据列表";
        FeedbackLog.event("SETTINGS_NATIVE_READY",item.getClass().getName());
        return result;
    }
    private static Constructor<?> itemConstructor(Class<?> type) {
        boolean hasAction=false;
        for(Method m:type.getMethods()) if(m.getReturnType()==void.class && m.getParameterTypes().length==1
                && m.getParameterTypes()[0].getName().equals("kotlin.jvm.functions.Function0")) hasAction=true;
        if(!hasAction) return null;
        for(Constructor<?> c:type.getDeclaredConstructors()) {
            Class<?>[] p=c.getParameterTypes();
            if((p.length==4 || p.length==5) && p[0]==Context.class && p[1]==int.class
                    && p[2]==CharSequence.class && p[3]==int.class && (p.length==4 || p[4]==String.class)) return c;
        }
        return null;
    }
    private static Activity activity(Context c) {
        for(int i=0;i<8 && c!=null;i++) {
            if(c instanceof Activity) return (Activity)c;
            if(!(c instanceof ContextWrapper)) break;
            Context next=((ContextWrapper)c).getBaseContext(); if(next==c) break; c=next;
        }
        return null;
    }
}
