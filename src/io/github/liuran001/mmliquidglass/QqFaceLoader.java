package io.github.liuran001.mmliquidglass;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Optional host face decoder, bound only to the currently logged-in account. */
final class QqFaceLoader {
    interface Result { void accept(Bitmap bitmap); }
    private Object decoder;
    private Method request;
    private String wanted;
    private Result callback;
    private long pendingUntil;
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean unavailable;

    void load(Object runtime,ClassLoader loader,String account,Result result) {
        if(runtime==null || account==null || account.isEmpty() || unavailable) return;
        if(account.equals(wanted) && SystemClock.uptimeMillis()<pendingUntil) return;
        try {
            if(decoder==null) initialize(runtime,loader);
            if(decoder==null || request==null) { unavailable=true; return; }
            wanted=account; callback=result; pendingUntil=SystemClock.uptimeMillis()+5000;
            Object accepted=request.invoke(decoder,account,1,true,(byte)0);
            if(Boolean.FALSE.equals(accepted)) pendingUntil=0;
        } catch(Throwable t) { unavailable=true; FeedbackLog.error("FACE_DECODER_UNAVAILABLE",t); }
    }
    private void initialize(Object runtime,ClassLoader loader) throws Exception {
        Class<?> type=null;
        for(String name:new String[]{"com.tencent.mobileqq.util.FaceDecoder","com.tencent.mobileqq.app.face.FaceDecoder"}) {
            try { type=loader.loadClass(name); break; } catch(ClassNotFoundException ignored) { }
        }
        if(type==null) return;
        for(Constructor<?> c:type.getConstructors()) {
            if(c.getParameterTypes().length==1 && c.getParameterTypes()[0].isInstance(runtime)) {
                c.setAccessible(true); decoder=c.newInstance(runtime); break;
            }
        }
        if(decoder==null) return;
        boolean listenerSet=false;
        for(Method m:type.getMethods()) {
            Class<?>[] p=m.getParameterTypes();
            if(p.length==4 && p[0]==String.class && p[1]==int.class && p[2]==boolean.class && p[3]==byte.class
                    && m.getReturnType()==boolean.class) request=m;
            if(p.length!=1 || !p[0].isInterface() || !p[0].getName().contains("DecodeTaskCompletionListener")) continue;
            Class<?> listener=p[0];
            Object proxy=Proxy.newProxyInstance(loader,new Class<?>[]{listener},(self,method,args)->{
                if(method.getName().equals("hashCode")) return System.identityHashCode(self);
                if(method.getName().equals("equals")) return args!=null && args.length==1 && self==args[0];
                if(method.getName().equals("toString")) return "LiquidGlassFaceListener";
                if(args!=null && args.length==4 && args[1] instanceof Integer && ((Integer)args[1])==1
                        && args[2] instanceof String && args[3] instanceof Bitmap) {
                    String id=(String)args[2];
                    Bitmap owned=NativeImageReader.copyBitmap((Bitmap)args[3]);
                    if(owned!=null) main.post(()->{
                        if(!id.equals(wanted)) return;
                        pendingUntil=0;
                        Result sink=callback;
                        if(sink!=null) sink.accept(owned);
                    });
                }
                return null;
            });
            m.setAccessible(true); m.invoke(decoder,proxy);listenerSet=true;break;
        }
        // The listener and request may appear in either order in getMethods().
        if(request==null) for(Method m:type.getMethods()) {
            Class<?>[] p=m.getParameterTypes();
            if(p.length==4 && p[0]==String.class && p[1]==int.class && p[2]==boolean.class && p[3]==byte.class
                    && m.getReturnType()==boolean.class) {request=m;break;}
        }
        if(!listenerSet) request=null;
        if(request!=null) request.setAccessible(true);
        FeedbackLog.event("FACE_DECODER_INIT",request==null?"unsupported signature":type.getName());
    }
    void cancel() { wanted=null; callback=null; pendingUntil=0; }
    void dispose() {
        cancel();
        if(decoder!=null) for(String name:new String[]{"destory","destroy","onDestroy"}) {
            try { decoder.getClass().getMethod(name).invoke(decoder); break; } catch(Exception ignored) { }
        }
        decoder=null; request=null;
    }
}
