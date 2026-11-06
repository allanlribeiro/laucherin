package app.launcherin.tv;

import android.app.Application;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LauncherinTitleAndroidTest {
 @Test public void textRemainsWhenTheLogoIsMissingOrFailsToDecode(){
  LauncherinTitle title=new LauncherinTitle(RuntimeEnvironment.getApplication(),"Modern Family");
  title.showLoadedLogo();assertEquals(View.VISIBLE,title.text.getVisibility());assertEquals(View.GONE,title.image.getVisibility());
  title.image.setImageBitmap(BitmapFactory.decodeByteArray(new byte[]{0,1,2},0,3));title.showLoadedLogo();
  assertEquals(View.VISIBLE,title.text.getVisibility());assertEquals(View.GONE,title.image.getVisibility());
 }
 @Test public void successfulLogoReplacesTextWithoutStretchingIt(){
  LauncherinTitle title=new LauncherinTitle(RuntimeEnvironment.getApplication(),"Modern Family");
  title.image.setImageBitmap(Bitmap.createBitmap(300,80,Bitmap.Config.ARGB_8888));title.showLoadedLogo();
  assertEquals(View.GONE,title.text.getVisibility());assertEquals(View.VISIBLE,title.image.getVisibility());
  assertEquals(ImageView.ScaleType.FIT_START,title.image.getScaleType());
 }
 @Test public void artworkCredentialsSurviveAnUnrelatedAppearanceUpdateAndCanBeRemoved() throws Exception {
  Application context=RuntimeEnvironment.getApplication();
  context.getSharedPreferences("tmdb-options",0).edit().clear().commit();
  LauncherinExtras.setString(context,LauncherinTmdb.KEY,"0123456789abcdef0123456789abcdef");
  assertTrue(LauncherinTmdb.settings(context).enabled);
  File dir=app.launcherin.tv.Config.dir(context);if(dir==null)dir=context.getFilesDir();
  try(FileOutputStream out=new FileOutputStream(new File(dir,"config.json"))){out.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  assertTrue(LauncherinTmdb.settings(context).enabled);
  LauncherinExtras.setString(context,LauncherinTmdb.KEY,"");
  assertFalse(LauncherinTmdb.settings(context).enabled);
 }
 private static Bitmap sampleBitmap(int i,String kind)throws Exception{byte[] data=java.nio.file.Files.readAllBytes(new File("/tmp/launcherin-tmdb/sample-"+i+"-"+kind).toPath());return BitmapFactory.decodeByteArray(data,0,data.length);}
 @Test public void renderOptionalLocalArtworkPreview() throws Exception {
  File sample=new File("/tmp/launcherin-tmdb/sample-0-logo");if(!sample.isFile())return;
  for(int i=0;i<3;i++)for(String kind:new String[]{"logo","backdrop"})if(!new File("/tmp/launcherin-tmdb/sample-"+i+"-"+kind).isFile())return;
  for(String name:new String[]{"disney","netflix","prime"})if(!new File("/tmp/launcherin-tmdb/provider-"+name).isFile())return;
  Application app=RuntimeEnvironment.getApplication();float density=app.getResources().getDisplayMetrics().density;
  FrameLayout screen=new FrameLayout(app);screen.setBackgroundColor(0xff111923);
  TextView label=new TextView(app);label.setText("Continuar assistindo · prévia dos cards");label.setTextColor(-1);label.setTextSize(22);
  FrameLayout.LayoutParams heading=new FrameLayout.LayoutParams(1200,70);heading.leftMargin=40;heading.topMargin=28;screen.addView(label,heading);
  for(int i=0;i<3;i++){
   FrameLayout card=new FrameLayout(app);GradientDrawable bg=new GradientDrawable();bg.setColor(0xff202938);bg.setCornerRadius(25);card.setBackground(bg);card.setClipToOutline(true);
   FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(380,230);cp.leftMargin=40+i*410;cp.topMargin=122;screen.addView(card,cp);
   ImageView picture=new ImageView(app);picture.setScaleType(ImageView.ScaleType.CENTER_CROP);picture.setImageBitmap(sampleBitmap(i,"backdrop"));card.addView(picture,new FrameLayout.LayoutParams(-1,-1));
   View shade=new View(app);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0xe8000000}));card.addView(shade,new FrameLayout.LayoutParams(-1,135,Gravity.BOTTOM));
   LauncherinTitle title=new LauncherinTitle(app,new String[]{"Modern Family","Stranger Things","The Boys"}[i]);title.image.setImageBitmap(sampleBitmap(i,"logo"));title.showLoadedLogo();
   FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,(int)(48*density),Gravity.BOTTOM);tp.leftMargin=20;tp.rightMargin=20;tp.bottomMargin=42;card.addView(title,tp);
   byte[] brandData=java.nio.file.Files.readAllBytes(new File("/tmp/launcherin-tmdb/provider-"+new String[]{"disney","netflix","prime"}[i]).toPath());
   ImageView provider=new ImageView(app);provider.setScaleType(ImageView.ScaleType.FIT_CENTER);provider.setImageBitmap(BitmapFactory.decodeByteArray(brandData,0,brandData.length));if(i==0){GradientDrawable backing=new GradientDrawable();backing.setColor(0xd9f5f7fa);backing.setCornerRadius(10);provider.setBackground(backing);provider.setPadding(5,3,5,3);}
   FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(70,32,Gravity.BOTTOM|Gravity.RIGHT);pp.rightMargin=14;pp.bottomMargin=12;card.addView(provider,pp);
   View progress=new View(app);progress.setBackgroundColor(-1);card.addView(progress,new FrameLayout.LayoutParams(160,4,Gravity.BOTTOM));
  }
  TextView note=new TextView(app);note.setText("Componente Android com imagens de exemplo da TMDB. Não é uma captura da TCL.");note.setTextColor(0xffaeb9c8);note.setTextSize(12);FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,55);np.leftMargin=40;np.topMargin=395;screen.addView(note,np);
  screen.measure(View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(460,View.MeasureSpec.EXACTLY));screen.layout(0,0,1280,460);
  Bitmap bitmap=Bitmap.createBitmap(1280,460,Bitmap.Config.ARGB_8888);screen.draw(new Canvas(bitmap));
  ByteArrayOutputStream out=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.PNG,100,out);java.nio.file.Files.write(new File("/tmp/launcherin-tmdb/cards-preview14.png").toPath(),out.toByteArray());
 }
}
