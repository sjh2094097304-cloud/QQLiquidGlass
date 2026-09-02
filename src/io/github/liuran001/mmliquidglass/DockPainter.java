package io.github.liuran001.mmliquidglass;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/** Shared by the actual dock and settings preview. Independent vector icons. */
final class DockPainter {
    // All callers are on the UI thread; no per-frame Paint/Path allocations.
    private static final Paint PAINT=new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path CHAT=new Path(),COMPASS=new Path();
    static {
        CHAT.moveTo(6,18);CHAT.lineTo(3,21);CHAT.lineTo(3,8);
        CHAT.cubicTo(3,3,7,2,12,2);CHAT.cubicTo(18,2,21,5,21,10);CHAT.cubicTo(21,15,17,18,12,18);CHAT.close();
        COMPASS.moveTo(15.5f,8.5f);COMPASS.lineTo(13.5f,13.5f);COMPASS.lineTo(8.5f,15.5f);COMPASS.lineTo(10.5f,10.5f);COMPASS.close();
    }
    static void tab(Canvas c,int width,int height,String title,int index,int count,boolean selected,
            DockOptions o,boolean dark,float density,float scaledDensity) {
        tab(c,width,height,title,index,count,selected,o,dark,density,scaledDensity,true);
    }
    static void tab(Canvas c,int width,int height,String title,int index,int count,boolean selected,
            DockOptions o,boolean dark,float density,float scaledDensity,boolean drawVectorIcon) {
        Paint p=PAINT; p.setStyle(Paint.Style.FILL);
        int mode=o.get(DockOptions.Key.MODE);
        p.setColor(selected?o.accent():dark?0xfff2f4f8:0xff283044);
        if(!selected) p.setAlpha(Math.round(o.get(DockOptions.Key.INACTIVE_ALPHA)*2.55f));
        float textSize=o.get(DockOptions.Key.TEXT)*scaledDensity*o.scale();
        float icon=iconSize(height,o,density);
        p.setTypeface(o.on(DockOptions.Key.BOLD)?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
        p.setTextSize(textSize);
        if(mode!=1) {
            float measured=p.measureText(title);
            if(measured>width-8*density) p.setTextSize(textSize*Math.max(.35f,(width-8*density)/measured));
        }
        float textHeight=p.descent()-p.ascent();
        float available=Math.max(1,height-(mode==2?icon+7*density:4*density));
        if(textHeight>available){p.setTextSize(p.getTextSize()*available/textHeight);textHeight=p.descent()-p.ascent();}
        float center=height/2f;
        if(mode!=0 && drawVectorIcon) {
            float top=iconTop(height,icon,textHeight,mode,density);
            int save=c.save(); c.translate(width/2f-icon/2,top); c.scale(icon/24,icon/24);
            icon(c,p,index,count); c.restoreToCount(save);
        }
        if(mode!=1) {
            p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER);
            float baseline=mode==2?(height+icon+3*density+textHeight)/2-p.descent():center-(p.ascent()+p.descent())/2;
            c.drawText(title,width/2f,baseline,p);
        }
    }
    static float iconSize(int height,DockOptions o,float density) {
        int mode=o.get(DockOptions.Key.MODE);
        return Math.min(o.get(DockOptions.Key.ICON)*density*o.scale(),height*(mode==2?.43f:.7f));
    }
    static float iconTop(int height,float icon,float textHeight,int mode,float density) {
        float center=height/2f;
        float cy=mode==2?(height-icon-textHeight-3*density)/2+icon/2:center;
        return cy-icon/2;
    }
    static void icon(Canvas c,Paint p,int index,int count) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        if(index==0) {
            c.drawPath(CHAT,p);
            c.drawLine(8,8,16,8,p); c.drawLine(8,12,13,12,p);
        } else if(index==1) {
            c.drawCircle(9,7,3.3f,p); c.drawArc(2,13,16,25,180,180,false,p);
            c.drawArc(13,4,20,11,-85,170,false,p); c.drawArc(14,13,23,23,-90,90,false,p);
        } else if(index==count-1) {
            c.drawCircle(12,12,9,p); c.drawPath(COMPASS,p);
        } else {
            for(int y=0;y<2;y++) for(int x=0;x<2;x++) c.drawRoundRect(3+x*11,3+y*11,10+x*11,10+y*11,2,2,p);
        }
        p.setStyle(Paint.Style.FILL);
    }
}
