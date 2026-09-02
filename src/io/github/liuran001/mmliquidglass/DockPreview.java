package io.github.liuran001.mmliquidglass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import static io.github.liuran001.mmliquidglass.DockOptions.Key.*;

/** Isolated draft renderer: the production optics/springs on a module-owned scene. */
final class DockPreview extends LinearLayout {
    private DockOptions options;
    private boolean dark,expanded,collapsed,closed,queued;
    private int sceneMode;
    private Bitmap image;
    private Bitmap[] themeIcons;
    private final Paint themeIconPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final String[] titles;
    private final TextView heading,themeButton,sceneButton,sizeButton,caption;
    private final Stage stage;
    private final Runnable applyTask=()->{queued=false;if(!closed)apply();};

    DockPreview(Context context,DockOptions draft,boolean dark,Bitmap avatar,String[] titles,Bitmap[] themeIcons) {
        super(context);setOrientation(VERTICAL);setTag(QqSplitDock.OWNED);
        options=new DockOptions(draft);this.dark=dark;image=avatar;this.themeIcons=themeIcons;
        this.titles=titles.length>=3 && titles.length<=5?titles.clone():new String[]{"消息","联系人","动态"};
        collapsed=getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density<520;
        setPadding(dp(10),dp(6),dp(10),dp(8));
        LinearLayout toolbar=new LinearLayout(context);toolbar.setGravity(Gravity.CENTER_VERTICAL);
        heading=button("实时预览",()->{collapsed=!collapsed;apply();});
        toolbar.addView(heading,new LayoutParams(0,dp(36),1));
        themeButton=button("",()->{this.dark=!this.dark;apply();});
        sceneButton=button("",()->{sceneMode=1-sceneMode;apply();});
        sizeButton=button("",()->{expanded=!expanded;apply();});
        for(TextView b:new TextView[]{themeButton,sceneButton,sizeButton}) {
            LayoutParams p=new LayoutParams(dp(48),dp(36));p.leftMargin=dp(3);toolbar.addView(b,p);
        }
        addView(toolbar);
        stage=Build.VERSION.SDK_INT>=33?new Stage(context):null;
        if(stage!=null)addView(stage,new LayoutParams(-1,dp(168)));
        caption=new TextView(context);caption.setTextSize(10);caption.setPadding(dp(4),dp(5),dp(4),0);
        caption.setLineSpacing(dp(2),1);addView(caption,new LayoutParams(-1,-2));
        apply();
    }
    void update(DockOptions draft) {
        options=new DockOptions(draft);
        if(!queued && !closed){queued=true;postOnAnimation(applyTask);}
    }
    private void apply() {
        if(closed)return;
        setBackground(shape(dark?0xff1c2533:0xffe8eef7,18));
        heading.setText(collapsed?"展开预览 ▾":"实时预览 ▴");
        themeButton.setText(dark?"深色":"浅色");sceneButton.setText(sceneMode==0?"消息":"彩色");sizeButton.setText(expanded?"缩小":"放大");
        for(TextView b:new TextView[]{heading,themeButton,sceneButton,sizeButton}) {
            b.setTextColor(dark?0xffe5edf9:0xff26374e);
            b.setBackground(shape(dark?0xff263447:0xffd7e2f2,9));
            if(b!=heading)b.setVisibility(collapsed?GONE:VISIBLE);
        }
        caption.setTextColor(dark?0xffaab8ca:0xff5a6c83);
        if(stage==null){caption.setText("此设备不支持实时玻璃预览（需要 Android 13 以上）");return;}
        stage.setVisibility(collapsed?GONE:VISIBLE);caption.setVisibility(collapsed?GONE:VISIBLE);
        int h=dp(expanded?256:168);LayoutParams lp=(LayoutParams)stage.getLayoutParams();
        if(lp.height!=h){lp.height=h;stage.setLayoutParams(lp);}
        stage.applyStyle();
        String optical=stage.glass.isSupported()?"点按／拖动液滴 · 滑动示例背景":"当前设备仅显示玻璃蒙层预览";
        caption.setText(optical+"\n"+options.get(HEIGHT)+"dp 高 · "+options.get(WIDTH)+"% 宽 · 悬浮 "+options.get(OFFSET)+"dp（位置适配预览区）");
    }
    void dispose() {
        closed=true;removeCallbacks(applyTask);queued=false;image=null;
        if(themeIcons!=null){for(Bitmap b:themeIcons)if(b!=null && !b.isRecycled())b.recycle();themeIcons=null;}
        if(stage!=null){stage.drag.stop();stage.backdrop.dispose();}
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();update(options);}
    @Override protected void onDetachedFromWindow(){if(stage!=null){stage.drag.stop();stage.backdrop.setPaused(true);}removeCallbacks(applyTask);queued=false;super.onDetachedFromWindow();}
    @Override protected void onVisibilityChanged(View changed,int visibility) {
        super.onVisibilityChanged(changed,visibility);
        if(stage!=null){boolean visible=isShown() && !collapsed && !closed;stage.backdrop.setPaused(!visible);if(!visible)stage.drag.stop();}
    }
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(stage!=null && visibility!=VISIBLE)stage.drag.stop();}
    private TextView button(String title,Runnable action){TextView t=new TextView(getContext());t.setText(title);t.setTextSize(11);t.setGravity(Gravity.CENTER);t.setFocusable(true);t.setOnClickListener(v->action.run());return t;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}

    private final class Stage extends FrameLayout {
        final float density=getResources().getDisplayMetrics().density;
        final Scene scene;
        final View marker;
        final Capsule capsule;
        final LinearLayout labels;
        final FrameLayout avatarHost;
        final PreviewAvatar avatar;
        final LiquidGlassPanel glass,avatarGlass;
        final DropletPanel droplet;
        final DropletDragController drag;
        final QqGlassBackdrop backdrop;
        int selected;
        Stage(Context c) {
            super(c);setClipChildren(true);setClipToPadding(true);
            scene=new Scene(c);addView(scene,new FrameLayout.LayoutParams(-1,-1));
            marker=new View(c);marker.setVisibility(GONE);addView(marker,new FrameLayout.LayoutParams(0,0));
            capsule=new Capsule(c);avatarHost=new FrameLayout(c);
            capsule.setTag(QqSplitDock.OWNED);avatarHost.setTag(QqSplitDock.OWNED);
            backdrop=new QqGlassBackdrop(this,marker,capsule,avatarHost,density,false);
            glass=new LiquidGlassPanel(c,null,density,dark);glass.setQqBackdrop(backdrop);
            avatarGlass=new LiquidGlassPanel(c,null,density,dark);avatarGlass.setQqBackdrop(backdrop);
            labels=new LinearLayout(c);labels.setOrientation(HORIZONTAL);labels.setPadding(dp(4),0,dp(4),0);labels.setClipChildren(false);
            for(int i=0;i<titles.length;i++)labels.addView(new Tab(c,i),new LinearLayout.LayoutParams(0,-1,1));
            FrameLayout.LayoutParams inner=new FrameLayout.LayoutParams(-1,-1);inner.setMargins(capsule.bleed,capsule.bleed,capsule.bleed,capsule.bleed);
            capsule.addView(glass,inner);capsule.addView(labels,new FrameLayout.LayoutParams(inner));
            droplet=new DropletPanel(c,null,labels,density,dark);droplet.setQqBackdrop(backdrop);droplet.setPill(glass);
            capsule.addView(droplet,new FrameLayout.LayoutParams(0,0));
            drag=new DropletDragController(droplet,labels,density,dark);drag.setPill(glass);drag.setHost(capsule);
            avatarHost.addView(avatarGlass,new FrameLayout.LayoutParams(-1,-1));avatar=new PreviewAvatar(c);avatarHost.addView(avatar,new FrameLayout.LayoutParams(-1,-1));
            avatarHost.setOutlineProvider(new android.view.ViewOutlineProvider(){public void getOutline(View v,android.graphics.Outline o){o.setOval(0,0,v.getWidth(),v.getHeight());}});
            addView(capsule,new FrameLayout.LayoutParams(1,1,Gravity.BOTTOM|Gravity.LEFT));
            addView(avatarHost,new FrameLayout.LayoutParams(1,1,Gravity.BOTTOM|Gravity.LEFT));
            for(View v:new View[]{glass,avatarGlass,droplet})v.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            labels.getChildAt(0).setSelected(true);
        }
        void applyStyle() {
            drag.stop();drag.setAnimationDuration(options.get(ANIMATION));drag.setPressStrength(options.get(PRESS_STRENGTH));
            backdrop.setTheme(dark);backdrop.setPaused(!isShown() || collapsed);backdrop.changed();
            glass.configureQq(options,dark);droplet.configureQq(options,dark);
            DockOptions circle=new DockOptions(options);circle.set(CORNER,50);avatarGlass.configureQq(circle,dark);
            capsule.setElevation(dp(options.get(SHADOW)));capsule.invalidateOutline();avatarHost.setElevation(dp(options.get(SHADOW)));
            for(int i=0;i<labels.getChildCount();i++)labels.getChildAt(i).invalidate();
            avatar.invalidate();scene.invalidate();position();
        }
        void position() {
            if(getWidth()<=0 || getHeight()<=0)return;
            boolean enabled=options.on(ENABLED)&&options.on(SPLIT);
            capsule.setVisibility(enabled?VISIBLE:INVISIBLE);avatarHost.setVisibility(enabled&&options.on(AVATAR)?VISIBLE:GONE);
            DockGeometry g=new DockGeometry(getWidth(),density,titles.length,options);
            int h=dp(options.get(HEIGHT)*options.scale());int common=Math.max(h,g.avatarSize);
            int offset=PreviewGeometry.offset(getHeight(),common,density,options.get(OFFSET));
            place(capsule,g.left-capsule.bleed,g.barWidth+capsule.bleed*2,h+capsule.bleed*2,offset+(common-h)/2-capsule.bleed);
            place(avatarHost,g.avatarLeft,g.avatarSize,g.avatarSize,offset+(common-g.avatarSize)/2);
            FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)droplet.getLayoutParams();
            int w=Math.max(1,(g.barWidth-dp(8))/titles.length),height=Math.max(1,h-dp(8));
            if(p.width!=w || p.height!=height){p.width=w;p.height=height;p.topMargin=capsule.bleed+dp(4);droplet.setLayoutParams(p);}
            drag.animateToIndex(selected,true);repaintBackdrop();
        }
        void place(View view,int left,int width,int height,int bottom) {
            FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)view.getLayoutParams();
            if(p.leftMargin==left && p.width==width && p.height==height && p.bottomMargin==bottom)return;
            p.leftMargin=left;p.width=width;p.height=height;p.bottomMargin=bottom;view.setLayoutParams(p);
        }
        void repaintBackdrop(){backdrop.changed();glass.invalidate();avatarGlass.invalidate();droplet.refresh();}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);position();}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){super.onLayout(changed,l,t,r,b);backdrop.bindSource();if(changed)repaintBackdrop();}
        private final class Capsule extends FrameLayout {
            final int bleed=dp(32);
            Capsule(Context c){super(c);setClipChildren(false);setClipToPadding(false);
                setOutlineProvider(new android.view.ViewOutlineProvider(){public void getOutline(View v,android.graphics.Outline o){float radius=Math.max(0,v.getHeight()-bleed*2)*options.get(CORNER)/100f;o.setRoundRect(bleed,bleed,Math.max(bleed,v.getWidth()-bleed),Math.max(bleed,v.getHeight()-bleed),radius);}});
            }
            @Override protected void dispatchDraw(Canvas c){if(!QqGlassBackdrop.isCapturing())super.dispatchDraw(c);}
            @Override protected void onLayout(boolean changed,int l,int t,int r,int b){super.onLayout(changed,l,t,r,b);if(changed)drag.animateToIndex(selected,true);}
            @Override public boolean dispatchTouchEvent(MotionEvent e){
                if(e.getActionMasked()==MotionEvent.ACTION_DOWN){
                    if(e.getX()<bleed || e.getX()>getWidth()-bleed || e.getY()<bleed || e.getY()>getHeight()-bleed)return false;
                    if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                }
                try{return super.dispatchTouchEvent(e);}finally{
                    if((e.getActionMasked()==MotionEvent.ACTION_UP || e.getActionMasked()==MotionEvent.ACTION_CANCEL)&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
                }
            }
            @Override public boolean onInterceptTouchEvent(MotionEvent e){return drag.onIntercept(e)||super.onInterceptTouchEvent(e);}
            @Override public boolean onTouchEvent(MotionEvent e){return drag.onTouch(e)||super.onTouchEvent(e);}
        }
        private final class Tab extends View implements DropletPanel.TabContent {
            final int index;final Paint badge=new Paint(Paint.ANTI_ALIAS_FLAG);
            Tab(Context c,int i){super(c);index=i;setFocusable(true);setContentDescription(titles[i]+"预览");setOnClickListener(v->{
                selected=index;for(int j=0;j<labels.getChildCount();j++)labels.getChildAt(j).setSelected(j==index);
                drag.animateToIndex(index,false);repaintBackdrop();
            });}
            @Override protected void onDraw(Canvas c){paint(c,isSelected());}
            @Override public void drawForLens(Canvas c){paint(c,true);}
            void paint(Canvas c,boolean chosen){
                Bitmap themed=themeIcons!=null && index<themeIcons.length?themeIcons[index]:null;
                boolean useTheme=options.on(THEME_ICONS) && themed!=null && !themed.isRecycled() && options.get(MODE)!=0;
                DockPainter.tab(c,getWidth(),getHeight(),titles[index],index,titles.length,chosen,options,dark,density,getResources().getDisplayMetrics().scaledDensity,!useTheme);
                if(useTheme) drawThemeBitmap(c,themed);
                if(index==0 && options.on(BADGES)){
                    float r=dp(7),x=getWidth()-dp(9),y=dp(9);badge.setColor(0xffed4653);c.drawCircle(x,y,r,badge);
                    badge.setTextSize(dp(9));badge.setTypeface(Typeface.DEFAULT_BOLD);badge.setTextAlign(Paint.Align.CENTER);badge.setColor(Color.WHITE);
                    c.drawText("2",x,y-(badge.ascent()+badge.descent())/2,badge);
                }
            }
            private void drawThemeBitmap(Canvas c,Bitmap bitmap) {
                int mode=options.get(MODE);
                Paint metrics=new Paint(Paint.ANTI_ALIAS_FLAG);
                metrics.setTypeface(options.on(BOLD)?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
                metrics.setTextSize(options.get(TEXT)*getResources().getDisplayMetrics().scaledDensity*options.scale());
                float textHeight=metrics.descent()-metrics.ascent();
                float iconSize=DockPainter.iconSize(getHeight(),options,density);
                float top=DockPainter.iconTop(getHeight(),iconSize,textHeight,mode,density);
                float scale=Math.min(iconSize/bitmap.getWidth(),iconSize/bitmap.getHeight());
                float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;
                android.graphics.RectF dst=new android.graphics.RectF((getWidth()-w)/2f,top+(iconSize-h)/2f,(getWidth()+w)/2f,top+(iconSize+h)/2f);
                c.drawBitmap(bitmap,null,dst,themeIconPaint);
            }
        }
        private final class PreviewAvatar extends View {
            final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);final Path clip=new Path();
            PreviewAvatar(Context c){super(c);setContentDescription("账号头像预览，不打开侧边栏");}
            @Override protected void onDraw(Canvas c){
                float inset=dp(options.get(AVATAR_INSET));RectF r=new RectF(inset,inset,getWidth()-inset,getHeight()-inset);
                int save=c.save();clip.reset();clip.addOval(r,Path.Direction.CW);c.clipPath(clip);
                if(image!=null && !image.isRecycled())c.drawBitmap(image,null,r,paint);
                else{float cx=r.centerX(),cy=r.centerY(),radius=r.width()/2;paint.setColor(dark?0xffb6c4d8:0xff6883a6);c.drawCircle(cx,cy-radius*.28f,radius*.28f,paint);c.drawRoundRect(cx-radius*.55f,cy+radius*.05f,cx+radius*.55f,cy+radius*.60f,radius*.25f,radius*.25f,paint);}
                c.restoreToCount(save);
            }
        }
        private final class Scene extends FrameLayout {
            final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);float scroll,down;
            final int[] rowColors={0xff6799e9,0xffbc83c9,0xffe2ab74,0xff5fb7a0};
            final String[] rowTitles={"消息示例","联系人","动态更新","玻璃预览"};
            LinearGradient colorful;
            Scene(Context c){super(c);setWillNotDraw(false);setContentDescription("示例背景，可上下滑动查看折射");}
            @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);colorful=new LinearGradient(0,0,Math.max(1,w),Math.max(1,h),new int[]{0xffe892b2,0xff5d9eee,0xff40b9a2},null,Shader.TileMode.CLAMP);}
            @Override protected void onDraw(Canvas c){
                paint.setShader(null);paint.setColor(dark?0xff171d28:0xffedf2f9);c.drawRect(0,0,getWidth(),getHeight(),paint);
                if(sceneMode==1){
                    paint.setShader(colorful);c.drawRect(0,0,getWidth(),getHeight(),paint);paint.setShader(null);
                    for(int i=-2;i<12;i++){float y=i*dp(34)+scroll%dp(34);paint.setColor(0x66ffffff);c.drawRect(0,y,getWidth(),y+dp(2),paint);}
                    for(int x=dp(16);x<getWidth();x+=dp(34)){paint.setColor(0x33ffffff);c.drawRect(x,0,x+dp(2),getHeight(),paint);}
                } else {
                    for(int i=-1;i<8;i++){
                        float y=i*dp(48)+scroll%dp(48);paint.setColor(rowColors[(i+4)%4]);c.drawCircle(dp(22),y+dp(24),dp(13),paint);
                        paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(dp(12));paint.setColor(dark?0xffe4eaf3:0xff364a67);c.drawText(rowTitles[(i+4)%4],dp(45),y+dp(21),paint);
                        paint.setColor(dark?0xff65758d:0xffb9c7da);c.drawRoundRect(dp(45),y+dp(29),getWidth()-dp(25+(i+4)%3*25),y+dp(33),dp(2),dp(2),paint);
                    }
                }
                boolean enabled=options.on(ENABLED)&&options.on(SPLIT);
                if(!options.on(HIDE_NATIVE)||!enabled){paint.setColor(dark?0xff252c39:0xffdce5f1);c.drawRect(0,getHeight()-dp(54),getWidth(),getHeight(),paint);}
                if(!enabled){paint.setColor(dark?0xffeef3fa:0xff334b6f);paint.setTextSize(dp(13));paint.setTypeface(Typeface.DEFAULT);c.drawText("QQ 原生底栏（示意）",dp(14),getHeight()-dp(21),paint);}
            }
            @Override public boolean onTouchEvent(MotionEvent e){
                switch(e.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:down=e.getY();getParent().requestDisallowInterceptTouchEvent(true);return true;
                    case MotionEvent.ACTION_MOVE:scroll+=e.getY()-down;down=e.getY();invalidate();repaintBackdrop();return true;
                    case MotionEvent.ACTION_UP:performClick();
                    case MotionEvent.ACTION_CANCEL:getParent().requestDisallowInterceptTouchEvent(false);return true;
                    default:return true;
                }
            }
            @Override public boolean performClick(){super.performClick();return true;}
        }
    }
}
