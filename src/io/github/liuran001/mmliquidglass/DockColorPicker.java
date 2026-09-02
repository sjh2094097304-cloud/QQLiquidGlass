package io.github.liuran001.mmliquidglass;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Inline RGB editor; incomplete input never overwrites the last valid draft. */
final class DockColorPicker extends LinearLayout {
    interface Listener { void onColor(int rgb); }
    private final Listener listener;
    private final int text,muted;
    private final float[] hsv=new float[3];
    private final SeekBar[] tracks=new SeekBar[3];
    private final TextView[] values=new TextView[3];
    private final ColorTrack[] gradients=new ColorTrack[3];
    private final View swatch;
    private final EditText hex;
    private final TextView hint;
    private boolean updating;

    DockColorPicker(Context context,int rgb,boolean dark,Listener listener) {
        super(context);this.listener=listener;
        text=dark?0xfff2f5fa:0xff162338;muted=dark?0xff99a8bd:0xff6c7e96;
        setOrientation(VERTICAL);setPadding(0,dp(12),0,dp(4));
        LinearLayout input=new LinearLayout(context);input.setGravity(Gravity.CENTER_VERTICAL);
        swatch=new View(context);swatch.setContentDescription("当前自定义颜色");
        input.addView(swatch,new LayoutParams(dp(38),dp(38)));
        hex=new EditText(context);hex.setSingleLine(true);hex.setTextSize(15);hex.setTextColor(text);
        hex.setTypeface(Typeface.MONOSPACE);hex.setSelectAllOnFocus(true);hex.setHint("#RRGGBB");hex.setHintTextColor(muted);
        hex.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});hex.setImeOptions(EditorInfo.IME_ACTION_DONE);
        hex.setContentDescription("自定义颜色，输入六位十六进制色值");
        LayoutParams hp=new LayoutParams(0,dp(48),1);hp.leftMargin=dp(12);input.addView(hex,hp);addView(input);
        hint=label("",11,muted);hint.setPadding(0,dp(4),0,dp(8));addView(hint);
        String[] names={"色相","饱和度","明度"};int[] maximum={360,100,100};
        for(int i=0;i<3;i++) {
            final int axis=i;LinearLayout heading=new LinearLayout(context);heading.setGravity(Gravity.CENTER_VERTICAL);
            heading.addView(label(names[i],13,text),new LayoutParams(0,-2,1));
            values[i]=label("",12,muted);heading.addView(values[i]);addView(heading);
            SeekBar track=new SeekBar(context);tracks[i]=track;track.setMax(maximum[i]);track.setSplitTrack(false);
            gradients[i]=new ColorTrack();track.setProgressDrawable(gradients[i]);
            track.setThumbTintList(ColorStateList.valueOf(dark?Color.WHITE:0xff162338));
            track.setContentDescription("自定义强调色"+names[i]);addView(track,new LayoutParams(-1,dp(38)));
            track.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar s,int p,boolean user){
                    if(updating || !user)return;
                    hsv[axis]=axis==0?p:p/100f;int value=Color.HSVToColor(hsv)&0xffffff;
                    updateUi(value,true);listener.onColor(value);
                }
                public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
            });
        }
        Color.colorToHSV(0xff000000|rgb,hsv);updateUi(rgb,true);
        hex.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                if(updating)return;Integer value=RgbColor.parse(s.toString());
                if(value==null){hint.setText("请补全 #RRGGBB；当前仍使用上一个有效颜色");return;}
                Color.colorToHSV(0xff000000|value,hsv);updateUi(value,false);listener.onColor(value);
            }
            public void afterTextChanged(Editable s){}
        });
        hex.setOnEditorActionListener((v,action,event)->{
            if(action!=EditorInfo.IME_ACTION_DONE)return false;
            if(RgbColor.parse(hex.getText().toString())==null){hex.setError("请输入 #RRGGBB，例如 #66CCFF");return true;}
            hex.clearFocus();InputMethodManager keyboard=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.hideSoftInputFromWindow(hex.getWindowToken(),0);return true;
        });
    }
    private void updateUi(int rgb,boolean updateHex) {
        updating=true;
        try {
            int color=0xff000000|rgb;
            GradientDrawable fill=new GradientDrawable();fill.setColor(color);fill.setCornerRadius(dp(10));fill.setStroke(dp(1),0xff8190a4);
            swatch.setBackground(fill);swatch.setContentDescription("当前颜色 "+RgbColor.format(rgb));
            if(updateHex)hex.setText(RgbColor.format(rgb));hex.setError(null);
            hint.setText("当前色值 "+RgbColor.format(rgb)+" · 保存后应用");
            for(int i=0;i<3;i++) {
                int p=Math.round(hsv[i]*(i==0?1:100));tracks[i].setProgress(p);values[i].setText(p+(i==0?"°":"%"));
            }
            gradients[0].colors(new int[]{0xffff0000,0xffffff00,0xff00ff00,0xff00ffff,0xff0000ff,0xffff00ff,0xffff0000});
            gradients[1].colors(new int[]{Color.HSVToColor(new float[]{hsv[0],0,hsv[2]}),Color.HSVToColor(new float[]{hsv[0],1,hsv[2]})});
            gradients[2].colors(new int[]{Color.BLACK,Color.HSVToColor(new float[]{hsv[0],hsv[1],1})});
        } finally {updating=false;}
    }
    private final class ColorTrack extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private int[] colors={Color.BLACK,Color.WHITE};
        void colors(int[] value){colors=value;invalidateSelf();}
        @Override public void draw(Canvas c){
            android.graphics.Rect b=getBounds();float y=b.exactCenterY(),r=dp(3);
            paint.setShader(new LinearGradient(b.left,y,Math.max(b.left+1,b.right),y,colors,null,Shader.TileMode.CLAMP));
            c.drawRoundRect(b.left,y-r,b.right,y+r,r,r,paint);
        }
        @Override public int getIntrinsicHeight(){return dp(6);}
        @Override public void setAlpha(int alpha){}
        @Override public void setColorFilter(android.graphics.ColorFilter filter){}
        @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }
    private TextView label(String s,int size,int color){TextView t=new TextView(getContext());t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
