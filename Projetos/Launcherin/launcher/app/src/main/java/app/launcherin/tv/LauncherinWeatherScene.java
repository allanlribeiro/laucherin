package app.launcherin.tv;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;
import java.util.Random;

/** Small Canvas scene, clipped by the expanded card. No video, blur or background animator. */
final class LauncherinWeatherScene extends View {
    enum Sky { CLEAR, PARTLY_CLOUDY, CLOUDY, RAIN, STORM, SNOW, FOG, UNKNOWN }
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Path cloud=new android.graphics.Path();
    private final float[] stars=new float[90];
    private Sky sky=Sky.UNKNOWN;private boolean night;private int transparency=82;private Shader gradient;private final boolean motion;
    private final long started=SystemClock.uptimeMillis();
    private final Runnable frame=new Runnable(){public void run(){if(shouldAnimate()){invalidate();postDelayed(this,42);}}};
    LauncherinWeatherScene(Context context,boolean motion) {
        super(context);this.motion=motion;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        Random random=new Random(2047);for(int i=0;i<stars.length;i++)stars[i]=random.nextFloat();
    }
    static Sky kind(int code) {
        switch(code){case 0:return Sky.CLEAR;case 1:case 2:return Sky.PARTLY_CLOUDY;case 3:return Sky.CLOUDY;
            case 45:case 48:return Sky.FOG;case 51:case 53:case 55:case 56:case 57:case 61:case 63:case 65:case 66:case 67:case 80:case 81:case 82:return Sky.RAIN;
            case 71:case 73:case 75:case 77:case 85:case 86:return Sky.SNOW;case 95:case 96:case 99:return Sky.STORM;default:return Sky.UNKNOWN;}
    }
    void weather(int code,boolean night,int transparency) {
        this.sky=kind(code);this.night=night;this.transparency=LauncherinWidgetAppearance.clamp(transparency);shade();invalidate();schedule();
    }
    private void shade() {
        if(getHeight()==0)return;int alpha=Math.round(255*(100-transparency)/100f);
        int top=night?0x101c48:sky==Sky.CLEAR?0x317db1:0x52677b,bottom=night?0x27233c:sky==Sky.CLEAR?0xa9cdda:0x8f9d9e;
        gradient=new LinearGradient(0,0,0,getHeight(),new int[]{(alpha<<24)|top,(alpha<<24)|bottom},null,Shader.TileMode.CLAMP);
    }
    private boolean shouldAnimate(){return motion&&isAttachedToWindow()&&getWindowVisibility()==VISIBLE&&isShown()&&hasWindowFocus()&&ValueAnimator.areAnimatorsEnabled();}
    private void schedule(){removeCallbacks(frame);if(shouldAnimate())postDelayed(frame,42);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();schedule();}
    @Override protected void onDetachedFromWindow(){removeCallbacks(frame);super.onDetachedFromWindow();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);schedule();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);schedule();}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){shade();}
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);float w=getWidth(),h=getHeight(),t=(SystemClock.uptimeMillis()-started)/1000f;
        paint.setStyle(Paint.Style.FILL);paint.setShader(gradient);canvas.drawRect(0,0,w,h,paint);paint.setShader(null);
        boolean clear=sky==Sky.CLEAR||sky==Sky.PARTLY_CLOUDY;
        if(night&&clear)for(int i=0;i<30;i++) {
            paint.setColor(Color.argb((int)(100+110*(.5+.5*Math.sin(t*.7+i))),238,244,255));
            canvas.drawCircle(stars[i*3]*w,stars[i*3+1]*h*.66f,1+stars[i*3+2]*1.8f,paint);
        }
        if(clear) {
            float cx=w*.84f,cy=h*.24f,r=h*.085f;
            if(night){paint.setColor(0xbbe6eaf7);canvas.drawCircle(cx,cy,r,paint);paint.setColor(0x555b6892);canvas.drawCircle(cx-r*.18f,cy-r*.22f,r*.16f,paint);canvas.drawCircle(cx+r*.35f,cy+r*.2f,r*.24f,paint);}
            else {
                paint.setColor(0x16ffe79b);canvas.drawCircle(cx,cy,r*2.5f,paint);paint.setColor(0x28ffe79b);canvas.drawCircle(cx,cy,r*1.7f,paint);
                canvas.save();canvas.translate(cx,cy);canvas.rotate(t*2);paint.setColor(0x99ffe9a6);paint.setStrokeWidth(2.4f);
                for(int i=0;i<12;i++){canvas.drawLine(0,r*1.28f,0,r*1.6f,paint);canvas.rotate(30);}canvas.restore();
                paint.setColor(0xccffe8a0);canvas.drawCircle(cx,cy,r,paint);
            }
        }
        if(sky!=Sky.CLEAR&&sky!=Sky.UNKNOWN){
            int count=sky==Sky.PARTLY_CLOUDY?2:4;
            for(int i=0;i<count;i++){
                float x=((i*.28f+t*.009f)%1.55f-.25f)*w,y=h*(.12f+(i%3)*.14f),r=h*(.07f+(i%2)*.02f);
                paint.setColor(night?0x605e6b86:0x70dce3e8);cloud(canvas,x,y,r);
            }
        }
        if(sky==Sky.RAIN||sky==Sky.STORM||sky==Sky.SNOW){
            paint.setColor(night?0x997b9abb:0x999bbbd2);paint.setStrokeWidth(Math.max(1,w/600));
            for(int i=0;i<48;i++){
                float x=(stars[(i*2)%90]+.018f*t)%1f*w,y=((i*.213f+t*(sky==Sky.SNOW?.04f:.45f))%1f)*h;
                if(sky==Sky.SNOW){paint.setColor(0xbbecf1fa);canvas.drawCircle(x+(float)Math.sin(t+i)*5,y,1.8f,paint);}
                else canvas.drawLine(x,y,x-4,y+h*.045f,paint);
            }
        }
        if(sky==Sky.FOG){paint.setColor(0x30d9e0e5);for(int i=0;i<4;i++)canvas.drawRoundRect(-w*.1f,h*(.32f+i*.13f),w*1.1f,h*(.36f+i*.13f),h*.03f,h*.03f,paint);}
    }
    private void cloud(Canvas canvas,float x,float y,float r){
        cloud.reset();cloud.addRoundRect(x-r*1.4f,y,x+r*1.8f,y+r*.8f,r*.45f,r*.45f,android.graphics.Path.Direction.CW);
        cloud.addCircle(x-r*.65f,y,r*.67f,android.graphics.Path.Direction.CW);cloud.addCircle(x+r*.25f,y-r*.25f,r*.92f,android.graphics.Path.Direction.CW);cloud.addCircle(x+r*1.1f,y,r*.64f,android.graphics.Path.Direction.CW);canvas.drawPath(cloud,paint);
    }
}
