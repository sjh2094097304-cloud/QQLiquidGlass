package io.github.liuran001.mmliquidglass;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/** User-selected document export. No file permission, provider, or automatic upload. */
final class FeedbackExport {
    private static final int REQUEST=0x6c67;
    private static final Set<Method> hooked=new HashSet<>();
    private static WeakReference<Activity> owner=new WeakReference<>(null);
    private static String pending;
    private static long deadline;
    static void forget(Activity a) { if(owner.get()==a){pending=null;owner.clear();} }
    static String report(Activity a) {
        String version="unknown";
        try {
            android.content.pm.PackageInfo p=a.getPackageManager().getPackageInfo(a.getPackageName(),0);
            version=p.versionName+" ("+p.versionCode+")";
        } catch(Exception ignored) { }
        StringBuilder b=new StringBuilder("LiquidGlass ").append(FeedbackLog.VERSION)
                .append("\nQQ=").append(version).append("\nAndroid API=").append(android.os.Build.VERSION.SDK_INT)
                .append("\n").append(QqSettingsEntry.status).append("\n").append(QqSplitDock.status)
                .append("\n").append(QqAvatarBridge.status).append("\n").append(QqAvatarBridge.diagnostics)
                .append("\n").append(QqGlassBackdrop.status)
                .append("\nRenderer=upstream AGSL lens + springs; shared cropped sibling; no Window capture\n");
        for(DockOptions.Key key:DockOptions.Key.values()) b.append(key.name()).append('=')
                .append(key==DockOptions.Key.CUSTOM_ACCENT?RgbColor.format(GlassConfig.options.get(key)):String.valueOf(GlassConfig.options.get(key))).append(' ');
        b.append("\n\nEVENTS (UTC, in-memory only)\n").append(FeedbackLog.text());
        return b.toString();
    }
    static void copy(Activity a) {
        android.content.ClipboardManager c=(android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        if(c!=null) c.setPrimaryClip(android.content.ClipData.newPlainText("LiquidGlass 诊断",report(a)));
        Toast.makeText(a,"已复制诊断日志，不含账号和聊天内容",Toast.LENGTH_SHORT).show();
    }
    static void export(Activity a) {
        try {
            for(Class<?> c=a.getClass();c!=null && Activity.class.isAssignableFrom(c);c=c.getSuperclass()) {
                try {
                    Method m=c.getDeclaredMethod("onActivityResult",int.class,int.class,Intent.class);
                    if(hooked.add(m)) LiquidGlassModule.hookAfter(m,chain->{
                        if(chain.getThisObject()==owner.get() && ((Integer)chain.getArg(0))==REQUEST)
                            result((Activity)chain.getThisObject(),(Integer)chain.getArg(1),(Intent)chain.getArg(2));
                    });
                } catch(NoSuchMethodException ignored) { }
            }
            owner=new WeakReference<>(a); pending=report(a); deadline=android.os.SystemClock.uptimeMillis()+600000;
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain");
            i.putExtra(Intent.EXTRA_TITLE,"LiquidGlass-feedback.txt");
            a.startActivityForResult(i,REQUEST);
        } catch(Throwable t) { pending=null; owner.clear(); FeedbackLog.error("EXPORT_FAILED",t); Toast.makeText(a,"无法打开文件选择器，请使用复制日志",1).show(); }
    }
    private static void result(Activity a,int resultCode,Intent data) {
        String text=pending; pending=null; owner.clear(); // consume exactly once even with superclass hooks
        if(resultCode!=Activity.RESULT_OK || data==null || data.getData()==null || text==null
                || android.os.SystemClock.uptimeMillis()>deadline) return;
        Uri uri=data.getData();
        if(!"content".equals(uri.getScheme())) return;
        android.content.Context context=a.getApplicationContext();
        new Thread(()->{
            boolean success=false;
            try(java.io.OutputStream out=context.getContentResolver().openOutputStream(uri,"wt")) {
                if(out==null) throw new java.io.IOException();
                out.write(text.getBytes(StandardCharsets.UTF_8)); success=true;
            } catch(Throwable t) { FeedbackLog.error("EXPORT_WRITE_FAILED",t); }
            final boolean saved=success;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->Toast.makeText(context,
                    saved?"日志已导出到你选择的位置":"导出失败，可改用复制日志",Toast.LENGTH_LONG).show());
        },"LiquidGlass-feedback-export").start();
    }
}
