package io.github.liuran001.mmliquidglass;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Procedural glass styling; does not sample, blur, or redraw the host page. */
final class GlassSurface extends Drawable {
    private final DockOptions options;
    private final boolean dark,circle;
    private final float density;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    GlassSurface(DockOptions options,boolean dark,float density,boolean circle) {
        this.options=new DockOptions(options); this.dark=dark; this.density=density; this.circle=circle;
    }
    private float radius() { return getBounds().height()*(circle?.5f:options.get(DockOptions.Key.CORNER)/100f); }
    @Override public void draw(Canvas c) {
        RectF r=new RectF(getBounds());
        float radius=radius();
        int tint=options.get(DockOptions.Key.TINT);
        int base=dark?new int[]{0xff292e38,0xff20324c,0xff322842,0xff3a3028}[tint]
                :new int[]{0xffeef3fa,0xffdcecff,0xffeee3ff,0xffffefda}[tint];
        int alpha=Math.round(options.get(DockOptions.Key.OPACITY)*2.55f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((base&0xffffff)|(alpha<<24)); c.drawRoundRect(r,radius,radius,paint);
        int light=options.get(DockOptions.Key.LIGHT);
        if(light>0) {
            paint.setColor(Color.WHITE);
            paint.setShader(new LinearGradient(0,r.top,0,r.bottom,new int[]{(light*95/100)<<24|0xffffff,0x00ffffff,0x08ffffff},new float[]{0,.55f,1},Shader.TileMode.CLAMP));
            c.drawRoundRect(r,radius,radius,paint); paint.setShader(null);
        }
        int edge=options.get(DockOptions.Key.BORDER);
        if(edge>0) {
            r.inset(density*.5f,density*.5f);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(density);
            paint.setColor((Math.round(edge*2.55f)<<24)|0xffffff);
            c.drawRoundRect(r,radius,radius,paint); paint.setStyle(Paint.Style.FILL);
        }
    }
    @Override public void getOutline(Outline outline) { outline.setRoundRect(getBounds(),radius()); }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
