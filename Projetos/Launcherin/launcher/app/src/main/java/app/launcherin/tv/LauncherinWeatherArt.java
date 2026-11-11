package app.launcherin.tv;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.View;

/** Four small local illustrations reused by compact/expanded forecasts. Bounded cache; no animator. */
final class LauncherinWeatherArt {
 private static final LruCache<Integer,Bitmap> images=new LruCache<Integer,Bitmap>(480*1024){@Override protected int sizeOf(Integer key,Bitmap value){return value.getAllocationByteCount();}};
 private static final Shader dayShade=new RadialGradient(-.4f,-.5f,1.6f,new int[]{0xfffff6b5,0xffffb53d},null,Shader.TileMode.CLAMP),nightShade=new RadialGradient(-.4f,-.5f,1.6f,new int[]{0xfffff7d3,0xffb4bedb},null,Shader.TileMode.CLAMP);
 private static final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 static void trim(){images.evictAll();}
 static boolean draw(Context context,Canvas canvas,RectF bounds,int code,boolean night){
  LauncherinWeatherScene.Sky sky=LauncherinWeatherScene.kind(code);int res;
  switch(sky){case PARTLY_CLOUDY:res=night?R.drawable.launcherin_weather_night:R.drawable.launcherin_weather_partly;break;case CLOUDY:case FOG:case SNOW:res=R.drawable.launcherin_weather_cloud;break;case RAIN:case STORM:res=R.drawable.launcherin_weather_rain;break;case CLEAR:clear(canvas,bounds,night);return true;default:return false;}
  Bitmap icon=images.get(res);if(icon==null){icon=BitmapFactory.decodeResource(context.getResources(),res);if(icon==null)return false;images.put(res,icon);}paint.setShader(null);paint.setColor(Color.WHITE);canvas.drawBitmap(icon,null,bounds,paint);
  if(sky==LauncherinWeatherScene.Sky.SNOW){paint.setColor(0xffdbf2ff);for(int i=0;i<3;i++)canvas.drawCircle(bounds.left+bounds.width()*(.3f+.2f*i),bounds.top+bounds.height()*.87f,bounds.width()*.025f,paint);}
  return true;
 }
 private static void clear(Canvas canvas,RectF b,boolean night){float x=b.centerX(),y=b.centerY(),r=Math.min(b.width(),b.height())*.24f;int save=canvas.save();canvas.translate(x,y);canvas.scale(r,r);paint.setShader(null);paint.setColor(night?0x20d5dbff:0x25ffcf6a);canvas.drawCircle(0,0,1.6f,paint);paint.setColor(Color.WHITE);paint.setShader(night?nightShade:dayShade);canvas.drawCircle(0,0,1,paint);paint.setShader(null);
  if(!night){paint.setColor(0xffffcf64);paint.setStrokeWidth(.1f);paint.setStrokeCap(Paint.Cap.ROUND);for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine((float)Math.cos(a)*1.25f,(float)Math.sin(a)*1.25f,(float)Math.cos(a)*1.6f,(float)Math.sin(a)*1.6f,paint);}}canvas.restoreToCount(save);
 }
 static final class Symbol extends Drawable {
  private final Context context;private final int code;private final boolean night;private final RectF dest=new RectF();
  Symbol(Context context,int code,boolean night){this.context=context;this.code=code;this.night=night;}
  @Override public void draw(Canvas canvas){dest.set(getBounds());LauncherinWeatherArt.draw(context,canvas,dest,code,night);}
  @Override public void setAlpha(int alpha){}@Override public void setColorFilter(ColorFilter filter){}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
 static final class Icon extends View {
  private final int code;private final boolean night;private final RectF dest=new RectF();
  Icon(Context context,int code,boolean night){super(context);this.code=code;this.night=night;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float side=Math.min(getWidth(),getHeight());dest.set((getWidth()-side)/2,0,(getWidth()+side)/2,side);LauncherinWeatherArt.draw(getContext(),c,dest,code,night);}
 }
}
