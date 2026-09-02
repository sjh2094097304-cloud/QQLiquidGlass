package io.github.liuran001.mmliquidglass;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;

/** Reads avatar image data only. No Window capture and no native View.draw. */
final class NativeImageReader {
    private int budget=64;
    private final IdentityHashMap<Object,Boolean> visited=new IdentityHashMap<>();
    Bitmap read(View view) {
        Bitmap b=view instanceof ImageView?image(((ImageView)view).getDrawable(),0):null;
        if(b==null) b=image(view.getBackground(),0);
        if(b==null) b=image(view.getForeground(),0);
        if(b==null) b=fields(view,0);
        if(b==null && view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<Math.min(8,group.getChildCount());i++) {
                View child=group.getChildAt(i);
                if(child.getWidth()<view.getWidth()/2 || child.getHeight()<view.getHeight()/2) continue;
                if(child instanceof ImageView) b=image(((ImageView)child).getDrawable(),1);
                if(b==null) b=image(child.getBackground(),1);
                if(b!=null) break;
            }
        }
        return b;
    }
    private Bitmap image(Object object,int depth) {
        if(object==null || depth>5 || --budget<0 || visited.put(object,true)!=null) return null;
        try {
            if(object instanceof Bitmap) return snapshot((Bitmap)object);
            if(object instanceof BitmapDrawable) return snapshot(((BitmapDrawable)object).getBitmap());
            if(!(object instanceof Drawable) || object instanceof ColorDrawable) return null;
            Drawable d=(Drawable)object;
            Drawable current=d.getCurrent();
            if(current!=d) { Bitmap b=image(current,depth+1); if(b!=null) return b; }
            // QQ's URL/face wrappers expose the image through zero-arg accessors.
            for(String method:new String[]{"getCurrDrawable","getBitmap","getDrawable","getCurrentDrawable"}) {
                try { Bitmap b=image(d.getClass().getMethod(method).invoke(d),depth+1); if(b!=null) return b; }
                catch(ReflectiveOperationException ignored) { }
            }
            Bitmap b=fields(d,depth+1);
            if(b!=null) return b;
            // Render only a detached, image-shaped clone; never mutate the host drawable.
            Drawable.ConstantState state=d.getConstantState();
            int w=d.getIntrinsicWidth(),h=d.getIntrinsicHeight();
            if(state!=null && w>=16 && h>=16 && w<=h*1.5f && h<=w*1.5f) {
                Drawable clone=state.newDrawable().mutate();
                if(clone==d || clone instanceof ColorDrawable) return null;
                Bitmap result=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
                clone.setBounds(0,0,128,128);
                clone.draw(new Canvas(result));
                return result;
            }
        } catch(Throwable t) { FeedbackLog.error("AVATAR_IMAGE_READ",t); }
        return null;
    }
    private Bitmap fields(Object object,int depth) {
        if(depth>4) return null;
        for(Class<?> c=object.getClass();c!=null && !c.getName().startsWith("android.") && !c.getName().startsWith("java.");c=c.getSuperclass()) {
            for(Field field:c.getDeclaredFields()) {
                if(--budget<0) return null;
                if(Modifier.isStatic(field.getModifiers())) continue;
                Class<?> type=field.getType();
                if(!Bitmap.class.isAssignableFrom(type) && !Drawable.class.isAssignableFrom(type)) continue;
                try { field.setAccessible(true); Bitmap b=image(field.get(object),depth+1); if(b!=null) return b; }
                catch(Throwable ignored) { }
            }
        }
        return null;
    }
    private static Bitmap snapshot(Bitmap source) {
        if(source==null || source.isRecycled() || source.getWidth()<16 || source.getHeight()<16
                || source.getWidth()>4096 || source.getHeight()>4096) return null;
        if(source.getConfig()==Bitmap.Config.HARDWARE) source=source.copy(Bitmap.Config.ARGB_8888,false);
        if(source==null) return null;
        int size=Math.min(source.getWidth(),source.getHeight());
        Bitmap crop=Bitmap.createBitmap(source,(source.getWidth()-size)/2,(source.getHeight()-size)/2,size,size);
        Bitmap small=Bitmap.createScaledBitmap(crop,128,128,true);
        // Own the pixels, independent from QQ recycling/reusing its image.
        return small.copy(Bitmap.Config.ARGB_8888,false);
    }
    static Bitmap copyBitmap(Bitmap source) {
        try { return snapshot(source); } catch(Throwable ignored) { return null; }
    }
}
