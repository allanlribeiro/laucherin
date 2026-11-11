package app.launcherin.tv;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Small vector line icons. Their colour follows the active visual theme. */
final class LauncherinGlyph extends Drawable {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final String kind;private int color;
 LauncherinGlyph(String kind,int color){this.kind=kind;this.color=color;}
 @Override public void draw(Canvas c){Rect b=getBounds();int save=c.save();float scale=Math.min(b.width(),b.height())/24f;c.translate(b.exactCenterX()-12*scale,b.exactCenterY()-12*scale);c.scale(scale,scale);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
  switch(kind){
   case "time":case "agenda":c.drawRoundRect(4,5,20,21,3,3,p);c.drawLine(4,10,20,10,p);c.drawLine(8,3,8,7,p);c.drawLine(16,3,16,7,p);if("time".equals(kind)){c.drawCircle(12,15,3,p);c.drawLine(12,13,12,15,p);c.drawLine(12,15,14,16,p);}else{c.drawLine(8,14,16,14,p);c.drawLine(8,17,13,17,p);}break;
   case "weather":c.drawCircle(12,12,4,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(12+(float)Math.cos(a)*7,12+(float)Math.sin(a)*7,12+(float)Math.cos(a)*9,12+(float)Math.sin(a)*9,p);}break;
   case "sports":c.drawRoundRect(7,3,17,14,3,3,p);c.drawArc(3,4,11,12,90,180,false,p);c.drawArc(13,4,21,12,-90,180,false,p);c.drawLine(12,14,12,20,p);c.drawLine(8,21,16,21,p);break;
   case "music":c.drawArc(4,3,20,20,180,180,false,p);c.drawRoundRect(3,11,7,20,2,2,p);c.drawRoundRect(17,11,21,20,2,2,p);break;
   case "system":c.drawRoundRect(6,6,18,18,3,3,p);c.drawRect(9,9,15,15,p);for(int i=8;i<=16;i+=4){c.drawLine(i,3,i,6,p);c.drawLine(i,18,i,21,p);c.drawLine(3,i,6,i,p);c.drawLine(18,i,21,i,p);}break;
   case "tv":c.drawRoundRect(3,6,21,20,3,3,p);c.drawLine(8,2,12,6,p);c.drawLine(16,2,12,6,p);break;
   case "photo":c.drawRoundRect(3,4,21,20,3,3,p);c.drawCircle(16,9,2,p);c.drawLine(4,18,10,11,p);c.drawLine(10,11,16,18,p);c.drawLine(14,16,17,13,p);c.drawLine(17,13,20,17,p);break;
  }c.restoreToCount(save);
 }
 @Override public void setTint(int color){this.color=color;invalidateSelf();}
 public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
