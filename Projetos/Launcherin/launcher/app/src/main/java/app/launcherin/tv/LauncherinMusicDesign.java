// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Static native surfaces and vector icons. No live blur, video or frame loop. */
final class LauncherinMusicDesign {
    /** Explicit clipping also works on TV firmware/software rendering that ignores outline clips. */
    static final class Image extends ImageView {
        private final boolean circle;private final float radius;private final Path clip=new Path();
        Image(Context c,boolean circle){super(c);this.circle=circle;radius=9*c.getResources().getDisplayMetrics().density;setCropToPadding(true);}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);clip.reset();if(circle)clip.addCircle(w/2f,h/2f,Math.min(w,h)/2f,Path.Direction.CW);else clip.addRoundRect(0,0,w,h,radius,radius,Path.Direction.CW);}
        @Override protected void onDraw(Canvas canvas){int save=canvas.save();canvas.clipPath(clip);super.onDraw(canvas);canvas.restoreToCount(save);}
    }
    final Context context;final LauncherinAppearance.Style skin;
    LauncherinMusicDesign(Context c){context=c;skin=LauncherinAppearance.get(c);}
    int dp(float v){return Math.round(v*context.getResources().getDisplayMetrics().density);}
    static int mix(int a,int b,float t){return Color.rgb(Math.round(Color.red(a)*(1-t)+Color.red(b)*t),Math.round(Color.green(a)*(1-t)+Color.green(b)*t),Math.round(Color.blue(a)*(1-t)+Color.blue(b)*t));}
    GradientDrawable surface(boolean focus,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{mix(0xff10191c,skin.accent,.12f),0xf2091015});d.setCornerRadius(dp(radius));d.setStroke(dp(focus?1.5f:.7f),focus?skin.accent:0x354f6568);return d;}
    TextView label(String value,float size,int color){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(skin.font);t.setIncludeFontPadding(false);return t;}
    TextView button(String text,String icon,Runnable action){TextView t=label(text,14,skin.text);t.setId(View.generateViewId());t.setGravity(Gravity.CENTER);t.setFocusable(true);t.setClickable(true);t.setPadding(dp(12),dp(8),dp(12),dp(8));t.setBackground(surface(false,12));if(icon!=null){t.setCompoundDrawablesRelativeWithIntrinsicBounds(new Icon(icon,skin.text,dp(19)),null,null,null);t.setCompoundDrawablePadding(dp(8));}t.setOnClickListener(v->action.run());t.setOnFocusChangeListener((v,f)->t.setBackground(surface(f,12)));return t;}
    TextView icon(String name,String description,int size,Runnable action){TextView t=button("",null,action);t.setContentDescription(description);t.setForeground(new Icon(name,skin.text,dp(size)));t.setForegroundGravity(Gravity.CENTER);t.setPadding(0,0,0,0);t.setBackground(surface(false,40));t.setOnFocusChangeListener((v,f)->t.setBackground(surface(f,40)));return t;}
    Drawable backdrop(){return new Drawable(){final Paint p=new Paint(3);public void draw(Canvas c){Rect b=getBounds();float w=b.width(),h=b.height();p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff030a0e,mix(0xff071312,skin.surface,.35f),0xff100e0b},null,Shader.TileMode.CLAMP));c.drawRect(b,p);Path a=new Path();a.moveTo(w*.32f,h);a.cubicTo(w*.55f,h*.75f,w*.7f,h*.92f,w,h*.1f);a.lineTo(w,h);a.close();p.setShader(new LinearGradient(w*.35f,h,w,h*.2f,new int[]{Color.TRANSPARENT,0x203ca898,0x00618474},null,Shader.TileMode.CLAMP));c.drawPath(a,p);a.reset();a.moveTo(w*.7f,0);a.cubicTo(w*.7f,h*.3f,w*.9f,h*.2f,w,h*.65f);a.lineTo(w,0);a.close();p.setShader(new LinearGradient(w*.65f,0,w,h*.65f,new int[]{0x003e3523,0x286f542e},null,Shader.TileMode.CLAMP));c.drawPath(a,p);p.setShader(null);}public void setAlpha(int n){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.OPAQUE;}};}
    Drawable placeholder(String text,boolean circle){return new Drawable(){final Paint p=new Paint(3);public void draw(Canvas c){Rect b=getBounds();int save=c.save();Path clip=new Path();clip.addRoundRect(new RectF(b),circle?b.width()/2f:dp(10),circle?b.height()/2f:dp(10),Path.Direction.CW);c.clipPath(clip);int accent=mix(skin.accent,0xffc8a775,(text.hashCode()&3)*.15f);p.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{mix(0xff10202b,accent,.35f),0xff050f15},null,Shader.TileMode.CLAMP));c.drawRect(b,p);p.setShader(null);p.setColor(0x305c928f);Path hill=new Path();hill.moveTo(b.left,b.bottom);hill.cubicTo(b.left,b.top+b.height()*.55f,b.exactCenterX(),b.top+b.height()*.9f,b.right,b.top+b.height()*.3f);hill.lineTo(b.right,b.bottom);hill.close();c.drawPath(hill,p);p.setColor(0xffcedbd3);p.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));p.setTextSize(Math.min(b.width(),b.height())*.28f);p.setTextAlign(Paint.Align.CENTER);String letters="♪";if(circle&&!text.trim().isEmpty()){String[] words=text.trim().split("\\s+");letters=words[0].substring(0,1)+(words.length>1?words[words.length-1].substring(0,1):"");}c.drawText(letters.toUpperCase(java.util.Locale.ROOT),b.exactCenterX(),b.exactCenterY()+p.getTextSize()*.34f,p);c.restoreToCount(save);}public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}};}
    static final class Icon extends Drawable {
        private final String name;private final int color,size;private final Paint p=new Paint(3);
        Icon(String name,int color,int size){this.name=name;this.color=color;this.size=size;}
        public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
        public void draw(Canvas c){Rect b=getBounds();int save=c.save();float s=Math.min(b.width(),b.height())/24f;c.translate(b.exactCenterX()-12*s,b.exactCenterY()-12*s);c.scale(s,s);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Path a=new Path();switch(name){
            case "home":a.moveTo(3,11);a.lineTo(12,3);a.lineTo(21,11);a.moveTo(5,10);a.lineTo(5,21);a.lineTo(10,21);a.lineTo(10,15);a.lineTo(14,15);a.lineTo(14,21);a.lineTo(19,21);a.lineTo(19,10);c.drawPath(a,p);break;
            case "search":c.drawCircle(10,10,6,p);c.drawLine(15,15,21,21,p);break;
            case "heart":a.moveTo(12,21);a.cubicTo(8,17,1,12,3,7);a.cubicTo(5,2,10,3,12,7);a.cubicTo(14,3,19,2,21,7);a.cubicTo(23,12,16,17,12,21);c.drawPath(a,p);break;
            case "queue":for(int y=5;y<=17;y+=6)c.drawLine(4,y,17,y,p);c.drawLine(19,14,23,17,p);c.drawLine(23,17,19,20,p);break;
            case "play":case "next":case "previous":p.setStyle(Paint.Style.FILL);int d="previous".equals(name)?-1:1;a.moveTo(12+d*5,12);a.lineTo(12-d*4,6);a.lineTo(12-d*4,18);a.close();c.drawPath(a,p);if(!"play".equals(name))c.drawRect(d<0?5:18,6,d<0?7:20,18,p);break;
            case "pause":p.setStyle(Paint.Style.FILL);c.drawRoundRect(6,5,10,19,1,1,p);c.drawRoundRect(14,5,18,19,1,1,p);break;
            case "add":c.drawLine(5,12,19,12,p);c.drawLine(12,5,12,19,p);break;
            case "lyrics":c.drawRoundRect(4,3,20,21,2,2,p);for(int y=7;y<=15;y+=4)c.drawLine(8,y,16,y,p);break;
            case "saver":c.drawRoundRect(2,4,22,18,2,2,p);c.drawLine(8,22,16,22,p);c.drawLine(12,18,12,22,p);break;
            case "back":c.drawLine(14,5,7,12,p);c.drawLine(7,12,14,19,p);break;
            case "more":p.setStyle(Paint.Style.FILL);for(int x=5;x<=19;x+=7)c.drawCircle(x,12,1.5f,p);break;
            case "settings":c.drawCircle(12,12,4,p);for(int i=0;i<8;i++){double v=i*Math.PI/4;c.drawLine(12+(float)Math.cos(v)*7,12+(float)Math.sin(v)*7,12+(float)Math.cos(v)*10,12+(float)Math.sin(v)*10,p);}break;
            default:c.drawCircle(12,12,8,p);
        }c.restoreToCount(save);}
        public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
