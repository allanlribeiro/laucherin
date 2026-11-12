// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Static native surfaces and vector icons. No live blur, video or frame loop. */
final class LauncherinMusicDesign {
    static final class Panel extends FrameLayout {private final Path clip=new Path();private final float radius;Panel(Context c,int r){super(c);radius=r*c.getResources().getDisplayMetrics().density;}@Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);clip.reset();clip.addRoundRect(0,0,w,h,radius,radius,Path.Direction.CW);}@Override public void draw(Canvas c){int n=c.save();c.clipPath(clip);super.draw(c);c.restoreToCount(n);}}
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
            p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{focus?0xf24a4279:0xef2b334b,0xf0161c2d,lit?0xf3332d51:0xef20273b},null,Shader.TileMode.CLAMP));c.drawRoundRect(r,round,round,p);
            int save=c.save();Path clip=new Path();clip.addRoundRect(r,round,round,Path.Direction.CW);c.clipPath(clip);
            p.setShader(new RadialGradient(r.left+r.width()*.08f,r.top,Math.max(1,r.width()*.65f),new int[]{focus?0x304e60b0:lit?0x153c519c:0x103c519c,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawRect(r,p);
            p.setShader(new RadialGradient(r.right,r.bottom,Math.max(1,r.width()*.5f),new int[]{focus?0x706d30e0:lit?0x384f2b9a:0x174f2b9a,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawRect(r,p);c.restoreToCount(save);
            p.setStyle(Paint.Style.STROKE);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{focus?0xffc8a1ff:lit?0x887e7c9f:0x446f758c,focus?0xffc5abff:0x444d536c,focus?VIOLET:lit?0x998a79af:0x556c678b},null,Shader.TileMode.CLAMP));
            p.setStrokeWidth(dp(focus?7:5));p.setAlpha(focus?55:lit?30:15);c.drawRoundRect(r,round,round,p);p.setAlpha(255);p.setStrokeWidth(dp(focus?1.7f:.8f));c.drawRoundRect(r,round,round,p);p.setShader(null);p.setStyle(Paint.Style.FILL);
        }
        public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    };}
    Drawable tab(boolean selected,boolean focus){if(!selected)return glass(focus,22,focus);GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff9a7bd5,0xff735bb9});d.setCornerRadius(dp(23));d.setStroke(dp(focus?2:1),focus?0xffeadbff:0xffaa8bdf);return d;}
    Drawable rim(int radius){return new Drawable(){final Paint p=new Paint(3);public void draw(Canvas c){Rect b=getBounds();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setShader(new LinearGradient(0,0,b.right,b.bottom,new int[]{0xb343a9ec,0x447858aa,0xca9c70df},null,Shader.TileMode.CLAMP));c.drawRoundRect(b.left+dp(1),b.top+dp(1),b.right-dp(1),b.bottom-dp(1),dp(radius),dp(radius),p);}public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}};}
    TextView label(String value,float size,int color){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(skin.font);t.setIncludeFontPadding(false);return t;}
    TextView button(String text,String icon,Runnable action){TextView t=label(text,14,TEXT);t.setId(View.generateViewId());t.setGravity(Gravity.CENTER);t.setFocusable(true);t.setClickable(true);t.setPadding(dp(12),dp(8),dp(12),dp(8));t.setBackground(surface(false,12));if(icon!=null){t.setCompoundDrawablesRelativeWithIntrinsicBounds(new Icon(icon,TEXT,dp(19)),null,null,null);t.setCompoundDrawablePadding(dp(8));}t.setOnClickListener(v->action.run());t.setOnFocusChangeListener((v,f)->t.setBackground(surface(f,12)));return t;}
    TextView icon(String name,String description,int size,Runnable action){TextView t=button("",null,action);t.setContentDescription(description);t.setForeground(new Icon(name,TEXT,dp(size)));t.setForegroundGravity(Gravity.CENTER);t.setPadding(0,0,0,0);t.setBackground(surface(false,40));t.setOnFocusChangeListener((v,f)->t.setBackground(surface(f,40)));return t;}
    Drawable backdrop(){return new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff121827,0xff080d1a,0xff151831});}
    /** Original vector collection artwork; no remote download or fake album imagery. */
    Drawable collectionArt(int index){return new Drawable(){final Paint p=new Paint(3);public void draw(Canvas c){Rect b=getBounds();int[] colors={0xff285678,0xff79416f,0xff243a81,0xff76323e,0xff413c88,0xff96602e};p.setShader(new LinearGradient(0,0,b.width(),b.height(),new int[]{colors[Math.floorMod(index,colors.length)],0xff111526},null,Shader.TileMode.CLAMP));c.drawRoundRect(new RectF(b),dp(10),dp(10),p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(0x447fb5db);for(int i=0;i<3;i++){Path wave=new Path();wave.moveTo(b.left,b.top+b.height()*(.15f+i*.18f));wave.cubicTo(b.width()*.25f,b.bottom,b.width()*.6f,b.top,b.right,b.top+b.height()*(.6f+i*.1f));c.drawPath(wave,p);}p.setShader(null);p.setStyle(Paint.Style.FILL);int save=c.save();c.clipRect(b);float x=b.left+b.width()*.76f,y=b.top+b.height()*.54f,z=Math.min(b.width()*.25f,b.height()*.7f);p.setColor(0x50343e6b);c.drawCircle(x,y,z*1.3f,p);p.setColor(0xaa11152c);p.setStrokeWidth(Math.max(2,z*.13f));p.setStyle(Paint.Style.STROKE);if(Math.floorMod(index,4)==0){c.drawArc(x-z*.65f,y-z*.65f,x+z*.65f,y+z*.65f,180,180,false,p);p.setStyle(Paint.Style.FILL);c.drawRoundRect(x-z*.78f,y-z*.1f,x-z*.45f,y+z*.65f,z*.12f,z*.12f,p);c.drawRoundRect(x+z*.45f,y-z*.1f,x+z*.78f,y+z*.65f,z*.12f,z*.12f,p);}else if(Math.floorMod(index,4)==1){c.rotate(-30,x,y);p.setStyle(Paint.Style.FILL);c.drawOval(x-z*.45f,y-z*.12f,x+z*.45f,y+z*.85f,p);c.drawOval(x-z*.3f,y-z*.4f,x+z*.3f,y+z*.4f,p);c.drawRect(x-z*.08f,y-z,x+z*.08f,y+z*.35f,p);p.setColor(0xaaecc5a2);c.drawCircle(x,y+z*.15f,z*.15f,p);}else if(Math.floorMod(index,4)==2){p.setStyle(Paint.Style.FILL);c.drawRoundRect(x-z,y-z*.35f,x+z,y+z*.65f,z*.1f,z*.1f,p);p.setColor(0x998dbce9);c.drawCircle(x-z*.5f,y+z*.1f,z*.28f,p);c.drawCircle(x+z*.5f,y+z*.1f,z*.28f,p);}else{c.rotate(25,x,y);c.drawRoundRect(x-z*.25f,y-z*.7f,x+z*.25f,y+z*.2f,z*.2f,z*.2f,p);c.drawLine(x,y+z*.2f,x,y+z,p);}c.restoreToCount(save);p.setStyle(Paint.Style.FILL);}public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}};}
    private void wave(Canvas c,Paint p,float w,float h,float x,float y,float endX,float endY,float bend,float bottom,int a,int b){Path path=new Path();path.moveTo(x*w,y*h);path.cubicTo(w*bend,h*(y+.13f),w*(endX-.1f),h*(endY+.13f),w*endX,h*endY);p.setStyle(Paint.Style.STROKE);p.setShader(new LinearGradient(0,h*y,w*endX,h*endY,new int[]{a,b},null,Shader.TileMode.CLAMP));p.setStrokeWidth(dp(22));p.setAlpha(25);c.drawPath(path,p);p.setStrokeWidth(dp(8));p.setAlpha(35);c.drawPath(path,p);p.setStrokeWidth(dp(1));p.setAlpha(140);c.drawPath(path,p);p.setAlpha(255);p.setStyle(Paint.Style.FILL);}
    Drawable placeholder(String text,boolean circle){return new Drawable(){final Paint p=new Paint(3);public void draw(Canvas c){Rect b=getBounds();int save=c.save();Path clip=new Path();clip.addRoundRect(new RectF(b),circle?b.width()/2f:dp(10),circle?b.height()/2f:dp(10),Path.Direction.CW);c.clipPath(clip);int accent=mix(CYAN,VIOLET,(text.hashCode()&3)*.15f);p.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{mix(0xff10202b,accent,.35f),0xff050f15},null,Shader.TileMode.CLAMP));c.drawRect(b,p);p.setShader(null);p.setColor(0x306f5eca);Path hill=new Path();hill.moveTo(b.left,b.bottom);hill.cubicTo(b.left,b.top+b.height()*.55f,b.exactCenterX(),b.top+b.height()*.9f,b.right,b.top+b.height()*.3f);hill.lineTo(b.right,b.bottom);hill.close();c.drawPath(hill,p);p.setColor(0xffd7ddff);p.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));p.setTextSize(Math.min(b.width(),b.height())*.28f);p.setTextAlign(Paint.Align.CENTER);String letters="♪";if(circle&&!text.trim().isEmpty()){String[] words=text.trim().split("\\s+");letters=words[0].substring(0,1)+(words.length>1?words[words.length-1].substring(0,1):"");}c.drawText(letters.toUpperCase(java.util.Locale.ROOT),b.exactCenterX(),b.exactCenterY()+p.getTextSize()*.34f,p);c.restoreToCount(save);}public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}};}
    static final class Icon extends Drawable {
        private final String name;private final int color,size;private final Paint p=new Paint(3);
        Icon(String name,int color,int size){this.name=name;this.color=color;this.size=size;}
        public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
        public void draw(Canvas c){Rect b=getBounds();int save=c.save();float s=Math.min(b.width(),b.height())/24f;c.translate(b.exactCenterX()-12*s,b.exactCenterY()-12*s);c.scale(s,s);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Path a=new Path();switch(name){
            case "shuffle":a.moveTo(3,6);a.lineTo(7,6);a.lineTo(17,18);a.lineTo(22,18);a.moveTo(3,18);a.lineTo(7,18);a.lineTo(17,6);a.lineTo(22,6);a.moveTo(19,3);a.lineTo(22,6);a.lineTo(19,9);a.moveTo(19,15);a.lineTo(22,18);a.lineTo(19,21);c.drawPath(a,p);break;
            case "repeat":a.moveTo(19,5);a.lineTo(22,8);a.lineTo(19,11);a.moveTo(22,8);a.lineTo(7,8);a.cubicTo(1,8,1,16,5,16);a.moveTo(5,13);a.lineTo(2,16);a.lineTo(5,19);a.moveTo(2,16);a.lineTo(17,16);a.cubicTo(23,16,23,8,19,8);c.drawPath(a,p);break;
            case "bell":a.moveTo(5,17);a.lineTo(6,8);a.cubicTo(6,1,18,1,18,8);a.lineTo(19,17);a.close();c.drawPath(a,p);c.drawArc(9,17,15,23,0,180,false,p);break;
            case "library":c.drawRoundRect(3,8,21,21,3,3,p);c.drawLine(6,4,18,4,p);c.drawLine(7,13,17,13,p);break;
            case "album":c.drawCircle(12,12,9,p);c.drawCircle(12,12,3,p);break;
            case "radio":for(int i=0;i<3;i++)c.drawArc(3+i*3,3+i*3,21-i*3,21-i*3,135,270,false,p);c.drawLine(11,10,11,22,p);break;
            case "volume":a.moveTo(3,9);a.lineTo(7,9);a.lineTo(12,4);a.lineTo(12,20);a.lineTo(7,15);a.lineTo(3,15);a.close();c.drawPath(a,p);c.drawArc(10,5,22,19,-60,120,false,p);break;
            case "recent":c.drawCircle(12,12,9,p);c.drawLine(12,6,12,12,p);c.drawLine(12,12,17,15,p);break;
            case "related":c.drawCircle(5,5,3,p);c.drawCircle(19,6,3,p);c.drawCircle(12,20,3,p);c.drawLine(8,5,16,6,p);c.drawLine(6,8,10,17,p);c.drawLine(18,9,14,17,p);break;
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
