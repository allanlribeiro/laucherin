package app.launcherin.tv;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Small vector props. One reusable brush/path; no bitmaps, downloaded artwork or blur. */
final class LauncherinChristmasArt {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private void fill(int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);}
    private void stroke(int color,float width){paint.setStyle(Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);}
    /** A hat sits on a card's upper corner; size is the width of the brim. */
    void hat(Canvas c,float x,float y,float size,float angle){
        int save=c.save();c.translate(x,y);c.rotate(angle);c.scale(size/64,size/64);
        fill(0x35000000);c.drawOval(-34,-1,34,10,paint);
        path.reset();path.moveTo(-29,0);path.cubicTo(-26,-23,-10,-45,12,-45);path.cubicTo(31,-44,40,-29,35,-17);path.cubicTo(32,-32,16,-31,13,-25);path.cubicTo(10,-16,21,-10,27,0);path.close();
        fill(0xffbc2436);c.drawPath(path,paint);
        path.reset();path.moveTo(-21,-4);path.cubicTo(-17,-22,-3,-38,11,-40);path.cubicTo(-1,-27,-3,-15,0,-4);path.close();fill(0xffe4484c);c.drawPath(path,paint);
        fill(0xffdedbd3);c.drawRoundRect(-32,-6,32,8,7,7,paint);fill(0xfffff9ee);c.drawRoundRect(-31,-7,31,3,5,5,paint);
        fill(0xffe3dfd7);c.drawCircle(35,-17,9,paint);fill(0xfffffbf2);c.drawCircle(33,-19,8,paint);c.restoreToCount(save);
    }
    /** Wire attaches to the top rim, with a shallow natural sag and warm lamps. */
    void lights(Canvas c,float width,float scale,int bulbs){
        float left=8*scale,right=width-8*scale,sag=7*scale,top=-2*scale;
        path.reset();path.moveTo(left,top);path.quadTo(width*.5f,top+sag*2,right,top);stroke(0xff667052,1.15f*scale);c.drawPath(path,paint);
        for(int i=0;i<bulbs;i++){
            float t=(i+.5f)/bulbs,x=left+(right-left)*t,y=top+4*sag*t*(1-t);
            stroke(0xff667052,scale);c.drawLine(x,y,x,y+3*scale,paint);fill(0x26ffc964);c.drawCircle(x,y+5*scale,4.3f*scale,paint);
            fill(i%4==3?0xffe36b5c:0xffffd78d);c.drawOval(x-1.65f*scale,y+2.5f*scale,x+1.65f*scale,y+7*scale,paint);
            fill(0xfffff4d5);c.drawCircle(x-.35f*scale,y+3.6f*scale,.65f*scale,paint);
        }
    }
    /** Base point is the card edge: the little figure can sit/lean on that edge. */
    void snowman(Canvas c,float x,float y,float height,float angle){
        int save=c.save();c.translate(x,y);c.rotate(angle);c.scale(height/64,height/64);
        fill(0x33000000);c.drawOval(-19,-2,19,5,paint);
        stroke(0xff796049,2.2f);c.drawLine(-10,-31,-24,-38,paint);c.drawLine(-20,-36,-24,-43,paint);c.drawLine(11,-31,22,-43,paint);c.drawLine(22,-43,27,-45,paint);
        fill(0xffd7dddb);c.drawCircle(0,-17,18,paint);fill(0xfff8f4e9);c.drawCircle(-2,-18,16.5f,paint);
        fill(0xffd7dddb);c.drawCircle(0,-41,12.5f,paint);fill(0xfffffaee);c.drawCircle(-1.3f,-42,11.5f,paint);
        fill(0xffbd2d3e);c.drawRoundRect(-12,-33,12,-27,2,2,paint);c.drawRoundRect(5,-30,11,-15,2,2,paint);fill(0xffe4bd79);c.drawRect(5,-19,11,-17,paint);
        fill(0xff283431);c.drawRoundRect(-12,-59,11,-50,2,2,paint);c.drawRoundRect(-16,-52,15,-48,2,2,paint);fill(0xffbd2d3e);c.drawRect(-12,-54,11,-51,paint);
        fill(0xff303a36);c.drawCircle(-5,-43,1.35f,paint);c.drawCircle(3,-43,1.35f,paint);c.drawCircle(-1,-22,1.7f,paint);c.drawCircle(-1,-14,1.7f,paint);
        path.reset();path.moveTo(-1,-40);path.lineTo(10,-37);path.lineTo(-1,-36);path.close();fill(0xffde9552);c.drawPath(path,paint);c.restoreToCount(save);
    }
    void gifts(Canvas c,float x,float y,float size){
        int save=c.save();c.translate(x,y);c.scale(size/64,size/64);
        gift(c,-27,-30,31,29,0xff35634f);gift(c,-3,-38,33,37,0xffb83043);c.restoreToCount(save);
    }
    private void gift(Canvas c,float x,float y,float w,float h,int color){
        fill(0x33000000);c.drawRoundRect(x-1,y+3,x+w+2,y+h+3,2,2,paint);fill(color);c.drawRoundRect(x,y,x+w,y+h,2,2,paint);
        fill(0xffe5bc72);c.drawRect(x+w*.43f,y,x+w*.58f,y+h,paint);c.drawRect(x,y+h*.31f,x+w,y+h*.46f,paint);
        stroke(0xfff2d598,2.3f);path.reset();float mid=x+w*.5f;path.moveTo(mid,y);path.cubicTo(mid-17,y-14,mid-17,y+4,mid,y);path.cubicTo(mid+17,y-14,mid+17,y+4,mid,y);c.drawPath(path,paint);
    }
}
