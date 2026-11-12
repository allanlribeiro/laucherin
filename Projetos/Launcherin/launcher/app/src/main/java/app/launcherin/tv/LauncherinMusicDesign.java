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
    static final int CYAN=0xff39d9ff,VIOLET=0xffab75ff,TEXT=0xfff5f6ff,MUTED=0xffb8c4df;
    /** Layered strokes and gradients, no blur, animated shader or software shadow layer. */
    Drawable surface(boolean focus,int radius){return glass(focus,radius,false);}
    Drawable glass(boolean focus,int radius,boolean lit){return new Drawable(){
        final Paint p=new Paint(3);final RectF r=new RectF();
        public void draw(Canvas c){Rect b=getBounds();float inset=dp(focus?3:1),round=dp(radius);r.set(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);
            p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{focus?0xf2182860:0xe7132849,0xf0061029,lit?0xf3221947:0xef10152e},null,Shader.TileMode.CLAMP));c.drawRoundRect(r,round,round,p);
            int save=c.save();Path clip=new Path();clip.addRoundRect(r,round,round,Path.Direction.CW);c.clipPath(clip);
            p.setShader(new RadialGradient(r.left+r.width()*.08f,r.top,Math.max(1,r.width()*.65f),new int[]{focus?0x6029ceff:lit?0x3029ceff:0x1229ceff,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawRect(r,p);
            p.setShader(new RadialGradient(r.right,r.bottom,Math.max(1,r.width()*.5f),new int[]{focus?0x706d30e0:lit?0x384f2b9a:0x174f2b9a,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawRect(r,p);c.restoreToCount(save);
            p.setStyle(Paint.Style.STROKE);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{focus?CYAN:lit?0xcc36c5ff:0x884b9dcf,focus?0xffc5abff:0x33498ca8,focus?VIOLET:lit?0xbb9570df:0x886d59a9},null,Shader.TileMode.CLAMP));
            p.setStrokeWidth(dp(focus?7:5));p.setAlpha(focus?55:lit?30:15);c.drawRoundRect(r,round,round,p);p.setAlpha(255);p.setStrokeWidth(dp(focus?1.7f:.8f));c.drawRoundRect(r,round,round,p);p.setShader(null);p.setStyle(Paint.Style.FILL);
        }
        public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    };}
    Drawable rim(int radius){return new Drawable(){final Paint p=new Paint(3);public void draw(Canvas c){Rect b=getBounds();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setShader(new LinearGradient(0,0,b.right,b.bottom,new int[]{0xb343a9ec,0x447858aa,0xca9c70df},null,Shader.TileMode.CLAMP));c.drawRoundRect(b.left+dp(1),b.top+dp(1),b.right-dp(1),b.bottom-dp(1),dp(radius),dp(radius),p);}public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}};}
    TextView label(String value,float size,int color){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(skin.font);t.setIncludeFontPadding(false);return t;}
    TextView button(String text,String icon,Runnable action){TextView t=label(text,14,TEXT);t.setId(View.generateViewId());t.setGravity(Gravity.CENTER);t.setFocusable(true);t.setClickable(true);t.setPadding(dp(12),dp(8),dp(12),dp(8));t.setBackground(surface(false,12));if(icon!=null){t.setCompoundDrawablesRelativeWithIntrinsicBounds(new Icon(icon,TEXT,dp(19)),null,null,null);t.setCompoundDrawablePadding(dp(8));}t.setOnClickListener(v->action.run());t.setOnFocusChangeListener((v,f)->t.setBackground(surface(f,12)));return t;}
    TextView icon(String name,String description,int size,Runnable action){TextView t=button("",null,action);t.setContentDescription(description);t.setForeground(new Icon(name,TEXT,dp(size)));t.setForegroundGravity(Gravity.CENTER);t.setPadding(0,0,0,0);t.setBackground(surface(false,40));t.setOnFocusChangeListener((v,f)->t.setBackground(surface(f,40)));return t;}
    Drawable backdrop(){return new Drawable(){final Paint p=new Paint(3);public void draw(Canvas c){Rect b=getBounds();float w=b.width(),h=b.height();p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff020817,0xff03112b,0xff090c27},null,Shader.TileMode.CLAMP));c.drawRect(b,p);
        p.setShader(new RadialGradient(w*.5f,0,w*.46f,new int[]{0x504e24c4,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawRect(b,p);
        p.setShader(new RadialGradient(w*.06f,h*.85f,w*.28f,new int[]{0x28264ff2,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawRect(b,p);
        wave(c,p,w,h,0f,.10f,.58f,-.04f,.75f,.10f,0xff193aab,0xff8555ef);
        wave(c,p,w,h,0f,.91f,.07f,.97f,.13f,.82f,0xff146ddc,0xff563ac5);p.setShader(null);
    }public void setAlpha(int n){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.OPAQUE;}};}
    private void wave(Canvas c,Paint p,float w,float h,float x,float y,float endX,float endY,float bend,float bottom,int a,int b){Path path=new Path();path.moveTo(x*w,y*h);path.cubicTo(w*bend,h*(y+.13f),w*(endX-.1f),h*(endY+.13f),w*endX,h*endY);p.setStyle(Paint.Style.STROKE);p.setShader(new LinearGradient(0,h*y,w*endX,h*endY,new int[]{a,b},null,Shader.TileMode.CLAMP));p.setStrokeWidth(dp(22));p.setAlpha(25);c.drawPath(path,p);p.setStrokeWidth(dp(8));p.setAlpha(35);c.drawPath(path,p);p.setStrokeWidth(dp(1));p.setAlpha(140);c.drawPath(path,p);p.setAlpha(255);p.setStyle(Paint.Style.FILL);}
    Drawable placeholder(String text,boolean circle){return new Drawable(){final Paint p=new Paint(3);public void draw(Canvas c){Rect b=getBounds();int save=c.save();Path clip=new Path();clip.addRoundRect(new RectF(b),circle?b.width()/2f:dp(10),circle?b.height()/2f:dp(10),Path.Direction.CW);c.clipPath(clip);int accent=mix(CYAN,VIOLET,(text.hashCode()&3)*.15f);p.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{mix(0xff10202b,accent,.35f),0xff050f15},null,Shader.TileMode.CLAMP));c.drawRect(b,p);p.setShader(null);p.setColor(0x306f5eca);Path hill=new Path();hill.moveTo(b.left,b.bottom);hill.cubicTo(b.left,b.top+b.height()*.55f,b.exactCenterX(),b.top+b.height()*.9f,b.right,b.top+b.height()*.3f);hill.lineTo(b.right,b.bottom);hill.close();c.drawPath(hill,p);p.setColor(0xffd7ddff);p.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));p.setTextSize(Math.min(b.width(),b.height())*.28f);p.setTextAlign(Paint.Align.CENTER);String letters="♪";if(circle&&!text.trim().isEmpty()){String[] words=text.trim().split("\\s+");letters=words[0].substring(0,1)+(words.length>1?words[words.length-1].substring(0,1):"");}c.drawText(letters.toUpperCase(java.util.Locale.ROOT),b.exactCenterX(),b.exactCenterY()+p.getTextSize()*.34f,p);c.restoreToCount(save);}public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}};}
    static final class Icon extends Drawable {
        private final String name;private final int color,size;private final Paint p=new Paint(3);
        Icon(String name,int color,int size){this.name=name;this.color=color;this.size=size;}
        public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
        public void draw(Canvas c){Rect b=getBounds();int save=c.save();float s=Math.min(b.width(),b.height())/24f;c.translate(b.exactCenterX()-12*s,b.exactCenterY()-12*s);c.scale(s,s);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Path a=new Path();switch(name){
            case "wave":for(int i=0;i<7;i++){float z=4+Math.abs(3-i)*2;c.drawLine(3+i*3,z,3+i*3,24-z,p);}break;
            case "artist":c.drawCircle(12,7,4,p);c.drawRoundRect(4,14,20,23,5,5,p);break;
            case "band":c.drawCircle(8,7,3,p);c.drawCircle(18,8,2.5f,p);c.drawRoundRect(2,13,14,22,4,4,p);c.drawLine(18,14,22,14,p);c.drawLine(22,14,22,21,p);break;
            case "spark":a.moveTo(12,2);a.lineTo(15,9);a.lineTo(22,12);a.lineTo(15,15);a.lineTo(12,22);a.lineTo(9,15);a.lineTo(2,12);a.lineTo(9,9);a.close();c.drawPath(a,p);break;
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
            case "settings":for(int i=0;i<32;i++){double angle=i*Math.PI/16-Math.PI/2;float radius=i%4<2?10:8;float x=12+(float)Math.cos(angle)*radius,y=12+(float)Math.sin(angle)*radius;if(i==0)a.moveTo(x,y);else a.lineTo(x,y);}a.close();c.drawPath(a,p);c.drawCircle(12,12,3.5f,p);break;
            default:c.drawCircle(12,12,8,p);
        }c.restoreToCount(save);}
        public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
