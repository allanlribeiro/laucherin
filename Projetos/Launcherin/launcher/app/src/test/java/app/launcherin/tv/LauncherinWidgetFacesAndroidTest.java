package app.launcherin.tv;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LauncherinWidgetFacesAndroidTest {
    private final Context c=RuntimeEnvironment.getApplication();
    private LauncherinWeather.Snapshot weather(int code,boolean night)throws Exception{return new LauncherinWeather.Snapshot(new JSONObject().put("query","demo").put("city","Sua cidade").put("temperature",25).put("updated",System.currentTimeMillis()).put("code",code).put("offset",0).put("is_day",!night));}
    private void draw(View view,Canvas canvas,int w,int h){view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);view.draw(canvas);}
    @Test public void facesRenderInAllSupportedSizesAndKeepEmptyDataHonest()throws Exception{
        LauncherinWidgetFaces.Clock clock=new LauncherinWidgetFaces.Clock(c);clock.update("11:31","quarta-feira","4 de novembro");
        LauncherinWidgetFaces.Weather sky=new LauncherinWidgetFaces.Weather(c);
        LauncherinWidgetFaces.System metrics=new LauncherinWidgetFaces.System(c);
        assertTrue(metrics.description().contains("aguardando"));
        for(int[] size:new int[][]{{150,72},{220,77},{400,170}}){
            Bitmap bitmap=Bitmap.createBitmap(size[0],size[1],Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);
            for(int code:new int[]{0,2,3,45,61,71,95,-1}){sky.update(weather(code,false),java.time.Instant.parse("2026-11-07T12:00:00Z").toEpochMilli());draw(sky,canvas,size[0],size[1]);}
            sky.empty("Escolha sua cidade");draw(sky,canvas,size[0],size[1]);draw(clock,canvas,size[0],size[1]);
            metrics.update(new LauncherinSystemStats.Sample(2040109465L,751619276L,null,false),null);draw(metrics,canvas,size[0],size[1]);
            assertTrue(metrics.description().contains("CPU da launcher aguardando"));
            int colored=0;for(int y=0;y<size[1];y++)for(int x=0;x<size[0];x++)if(Color.alpha(bitmap.getPixel(x,y))>0)colored++;
            assertTrue(colored>100);bitmap.recycle();
        }
    }
    @Test public void renderPreviewWithRealAndroidComponents()throws Exception{
        Bitmap image=Bitmap.createBitmap(1500,580,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0,0,1500,580,new int[]{0xff172d34,0xff263448,0xff19222e},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,1500,580,p);p.setShader(null);
        p.setTypeface(Typeface.create("sans-serif-light",0));p.setTextSize(38);p.setColor(0xfff4f6f8);canvas.drawText("Launcherin · novos widgets",65,85,p);
        LauncherinWidgetFaces.Clock clock=new LauncherinWidgetFaces.Clock(c);clock.update("11:31","quarta-feira","4 de novembro");
        LauncherinWidgetFaces.Weather weather=new LauncherinWidgetFaces.Weather(c);weather.update(weather(61,false),java.time.Instant.parse("2026-11-07T12:00:00Z").toEpochMilli());
        LauncherinWidgetFaces.System system=new LauncherinWidgetFaces.System(c);system.update(new LauncherinSystemStats.Sample(2040109465L,751619276L,4d,false),12.8);
        View[] faces={clock,weather,system};String[] names={"DATA E HORA","PREVISÃO DO TEMPO","SISTEMA"};
        for(int i=0;i<3;i++){
            int x=65+i*463;canvas.save();canvas.translate(x,160);
            android.graphics.drawable.Drawable glass=LauncherinWidgetAppearance.glass(c,82,false);glass.setBounds(0,0,440,290);glass.draw(canvas);
            p.setColor(0xffbdcbd1);p.setTypeface(Typeface.create("sans-serif-medium",0));p.setTextSize(19);canvas.drawText(names[i],30,38,p);
            canvas.translate(30,66);draw(faces[i],canvas,380,190);canvas.restore();
        }
        p.setTextSize(21);p.setTypeface(Typeface.create("sans-serif",0));p.setColor(0xffa5b5c5);canvas.drawText("PRÉVIA · DADOS DE EXEMPLO · TRANSPARÊNCIA AJUSTÁVEL",65,527,p);
        File file=new File("/tmp/launcherin-widget-preview.png");try(FileOutputStream out=new FileOutputStream(file)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}image.recycle();
    }
}
