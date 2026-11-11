package app.launcherin.tv;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;

/** Optical finish built from cached shaders. No blur, animation timer, bitmap layer or per-frame allocation. */
final class LauncherinSurface extends GradientDrawable {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final RectF edge=new RectF(),inner=new RectF();private final Path clip=new Path(),reflection=new Path();
 private final LauncherinAppearance.Style skin;private final boolean focused,circle,outline,minimal,premium;private final int transparency;
 private final float density,radius,strength;private Shader body,rim,bevel,shine,light,reflectionShade;
 LauncherinSurface(Context c,int transparency,boolean focus,boolean circle,boolean outline,float radius){
  skin=LauncherinAppearance.get(c);this.transparency=LauncherinWidgetAppearance.clamp(transparency);focused=focus;this.circle=circle;this.outline=outline;density=c.getResources().getDisplayMetrics().density;this.radius=radius*density;
  minimal="minimal_cinematic".equals(skin.family);premium="glass_premium".equals(skin.family);strength=skin.reduced?0:skin.intensity/100f;setColor(Color.TRANSPARENT);setCornerRadius(this.radius);
 }
 private int alpha(int color,int a){return (Math.max(0,Math.min(255,a))<<24)|(color&0xffffff);}
 @Override protected void onBoundsChange(Rect b){
  super.onBoundsChange(b);float inset=(minimal?1:2)*density;edge.set(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);if(circle){float side=Math.min(edge.width(),edge.height());edge.set(b.exactCenterX()-side/2,b.exactCenterY()-side/2,b.exactCenterX()+side/2,b.exactCenterY()+side/2);}
  if(edge.width()<=0||edge.height()<=0)return;float r=circle?edge.width()/2:radius;inner.set(edge);inner.inset(2*density,2*density);clip.reset();clip.addRoundRect(edge,r,r,Path.Direction.CW);
  int a=Math.round(255*(100-transparency)/100f);if(skin.lightWallpaper&&(skin.auto||"ambient_dynamic".equals(skin.family))&&transparency<100)a=Math.max(a,190);int top=LauncherinAppearance.blend(skin.surface,skin.accent,minimal?.015f:.16f),deep=LauncherinAppearance.blend(skin.surface,Color.BLACK,.38f),silver=LauncherinAppearance.blend(skin.accent,Color.WHITE,.8f);
  body=new LinearGradient(edge.left,edge.top,edge.right,edge.bottom,new int[]{alpha(top,Math.min(255,a+12)),alpha(skin.surface,a),alpha(deep,Math.min(255,a+25)),alpha(top,a)},new float[]{0,.3f,.73f,1},Shader.TileMode.CLAMP);
  int hi=focused?255:minimal?90:190,lo=focused?190:minimal?28:65;
  rim=new LinearGradient(edge.left,edge.top,edge.right,edge.bottom,new int[]{alpha(silver,hi),alpha(skin.accent,lo),alpha(silver,hi),alpha(skin.accent,lo),alpha(silver,hi)},new float[]{0,.28f,.52f,.76f,1},Shader.TileMode.CLAMP);
  bevel=new LinearGradient(0,edge.top,0,edge.bottom,new int[]{alpha(Color.WHITE,focused?150:70),alpha(skin.accent,20),alpha(Color.BLACK,120),alpha(silver,focused?160:80)},new float[]{0,.12f,.7f,1},Shader.TileMode.CLAMP);
  shine=new LinearGradient(0,edge.top,0,edge.top+edge.height()*.37f,new int[]{alpha(Color.WHITE,(int)(75*strength)),Color.TRANSPARENT},null,Shader.TileMode.CLAMP);
  light=new RadialGradient(edge.left+edge.width()*.05f,edge.bottom,Math.max(1,edge.width()*.72f),new int[]{alpha(skin.accent,(int)((focused?120:55)*strength)),Color.TRANSPARENT},null,Shader.TileMode.CLAMP);
  reflection.reset();reflection.moveTo(edge.left,edge.top);reflection.lineTo(edge.right,edge.top);reflection.lineTo(edge.left+edge.width()*.12f,edge.top+edge.height()*.82f);reflection.lineTo(edge.left,edge.bottom*.73f+edge.top*.27f);reflection.close();
  reflectionShade=new LinearGradient(edge.left,edge.top,edge.right*.75f+edge.left*.25f,edge.bottom,new int[]{alpha(Color.WHITE,(int)(48*strength)),alpha(Color.WHITE,(int)(8*strength)),Color.TRANSPARENT},new float[]{0,.65f,1},Shader.TileMode.CLAMP);
 }
 private void fill(Canvas c,Shader shader,RectF bounds,float r){paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setShader(shader);c.drawRoundRect(bounds,r,r,paint);}
 private void stroke(Canvas c,Shader shader,RectF bounds,float r,float width){paint.setStyle(Paint.Style.STROKE);paint.setColor(Color.WHITE);paint.setStrokeWidth(width);paint.setShader(shader);c.drawRoundRect(bounds,r,r,paint);}
 @Override public void draw(Canvas c){
  if(edge.width()<=0||edge.height()<=0)return;float r=circle?edge.width()/2:radius;
  if(!outline){fill(c,body,edge,r);if(!minimal&&strength>0){fill(c,light,edge,r);if(premium){fill(c,shine,edge,r);int save=c.save();c.clipPath(clip);paint.setStyle(Paint.Style.FILL);paint.setShader(reflectionShade);c.drawPath(reflection,paint);c.restoreToCount(save);}}}
  if(!minimal&&strength>0)stroke(c,bevel,inner,Math.max(0,r-2*density),density);
  if(focused&&!minimal&&strength>0){paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setColor(alpha(skin.accent,(int)(65*strength)));paint.setStrokeWidth(6*density);c.drawRoundRect(edge,r,r,paint);}
  stroke(c,rim,edge,r,(focused?1.7f:minimal?.7f:1.05f)*density);paint.setShader(null);paint.setStyle(Paint.Style.FILL);
 }
 static GradientDrawable card(Context c,int transparency,boolean focused){return new LauncherinSurface(c,transparency,focused,false,false,18);}
 static GradientDrawable outline(Context c,boolean focused,float radius){return new LauncherinSurface(c,100,focused,false,true,radius);}
 static GradientDrawable shortcut(Context c,boolean focused){return new LauncherinSurface(c,55,focused,true,false,100);}
 static GradientDrawable progress(Context c){int accent=LauncherinAppearance.get(c).accent;GradientDrawable d=new GradientDrawable(Orientation.LEFT_RIGHT,new int[]{LauncherinAppearance.blend(accent,Color.WHITE,.45f),accent});d.setCornerRadius(3*c.getResources().getDisplayMetrics().density);return d;}
}
