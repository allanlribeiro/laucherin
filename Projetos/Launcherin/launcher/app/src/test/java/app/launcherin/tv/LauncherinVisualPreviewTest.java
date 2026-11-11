package app.launcherin.tv;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import org.json.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import static org.junit.Assert.*;

/** Native Android widget gallery with explicit sample data; not a screenshot of a physical TV. */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=35,qualifiers="w960dp-h540dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LauncherinVisualPreviewTest {
 public static class PreviewHost extends MainActivity {
  private void called(){ReflectionHelpers.setField(this,"mCalled",true);}
  @Override protected void onCreate(Bundle b){called();setContentView(R.layout.activity_main);}
  @Override protected void onStart(){called();}@Override protected void onResume(){called();}@Override protected void onPause(){called();}@Override protected void onStop(){called();}@Override protected void onDestroy(){called();}
  @Override void launcherinChromeFocused(){}
 }
 private static Object field(Object o,String n)throws Exception{java.lang.reflect.Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 private static void invoke(Object o,String n,Class<?>[] types,Object... args)throws Exception{java.lang.reflect.Method m=o.getClass().getDeclaredMethod(n,types);m.setAccessible(true);m.invoke(o,args);}
 private static void measure(View view,int w,int h){view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);view.forceLayout();view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);}
 private static File sourceRoot(){File f=new File(System.getProperty("user.dir"));while(f!=null&&!new File(f,"themes/nature-calm/wallpapers").isDirectory())f=f.getParentFile();if(f==null)throw new IllegalStateException("Run from the Launcherin source checkout");return f;}
 private void save(Bitmap bitmap,String name)throws Exception{File out=new File(System.getProperty("java.io.tmpdir")+"/launcherin-visual-previews/"+name+".png");out.getParentFile().mkdirs();try(FileOutputStream f=new FileOutputStream(out)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,f));}bitmap.recycle();}
 private void text(Canvas c,String s,float x,float y,float size,int color){Paint p=new Paint(3);p.setTypeface(Typeface.create("sans-serif",0));p.setTextSize(size);p.setColor(color);c.drawText(s,x,y,p);}
 private Bitmap backdrop;
 private void base(Canvas c,int w,int h,int accent){Paint p=new Paint(3);if(backdrop==null)backdrop=BitmapFactory.decodeFile(new File(sourceRoot(),"themes/nature-calm/wallpapers/004.jpg").getPath());c.drawBitmap(backdrop,null,new Rect(0,0,w,h),p);p.setColor(0xc5080c12);c.drawRect(0,0,w,h,p);p.setShader(new LinearGradient(0,0,w,h,new int[]{0xa0060a10,0x30ffffff&LauncherinAppearance.blend(accent,Color.BLACK,.83f),0xb00c141c},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);}
 @Test public void renderTheActualSevenWidgetsAndExistingExpansions()throws Exception{
  PreviewHost host=Robolectric.buildActivity(PreviewHost.class).setup().get();FrameLayout root=host.findViewById(R.id.root);RecyclerView grid=host.findViewById(R.id.grid);measure(root,960,540);
  File data=Config.dir(host);data.mkdirs();new File(LauncherinAppearance.dir(host),"options.json").delete();LauncherinAppearance.invalidate();
  for(String id:new String[]{"819","2029"}){File dir=new File(host.getCacheDir(),"sports-crests");dir.mkdirs();try(InputStream input=getClass().getResourceAsStream("/sports-crests/"+id+".png")){assertNotNull(input);Files.copy(input,new File(dir,id+".png").toPath(),StandardCopyOption.REPLACE_EXISTING);}}
  List<LauncherinSports.Team> teams=new ArrayList<>();for(LauncherinSports.Team t:LauncherinSports.catalogue(host))if(t.id.equals("819")||t.id.equals("2029"))teams.add(t);
  String today=java.time.LocalDate.now().toString();JSONObject agenda=new JSONObject().put("events",new JSONArray().put(new JSONObject().put("id","sample1").put("title","Consulta").put("date",today).put("time","14:30")).put(new JSONObject().put("id","sample2").put("title","Reunião").put("date",today).put("time","19:00")));
  Files.write(new File(data,"agenda.json").toPath(),agenda.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
  LauncherinDashboard dashboard=new LauncherinDashboard(host,root,grid);measure(root,960,540);
  Map<String,View> cards=(Map<String,View>)field(dashboard,"allWidgets");LauncherinExtraWidgets extra=(LauncherinExtraWidgets)field(dashboard,"extraWidgets");
  LauncherinWidgetFaces.Clock clock=(LauncherinWidgetFaces.Clock)field(dashboard,"clockFace");clock.update("13:45","quarta-feira","11 de novembro");
  JSONObject weatherJson=new JSONObject().put("query","sample").put("city","Minha cidade").put("temperature",28).put("code",2).put("updated",System.currentTimeMillis()).put("offset",0).put("days",new JSONArray());
  java.time.LocalDate date=java.time.LocalDate.now();JSONArray days=new JSONArray();for(int i=0;i<7;i++)days.put(new JSONObject().put("date",date.plusDays(i).toString()).put("code",i<3?2:61).put("low",20+i%3).put("high",28+i%4).put("rain",i<3?20:65));weatherJson.put("days",days);
  LauncherinWeather.Snapshot weather=new LauncherinWeather.Snapshot(weatherJson);invoke(dashboard,"showWeather",new Class[]{LauncherinWeather.Snapshot.class},weather);
  long now=System.currentTimeMillis();JSONObject match=new JSONObject().put("id","fixture1").put("homeId","819").put("awayId","2029").put("home","Flamengo").put("away","Palmeiras").put("time",now+3600000).put("state","pre").put("competition","Partida de exemplo");JSONObject before=new JSONObject(match.toString()).put("state","post").put("homeScore","2").put("awayScore","1").put("time",now-86400000);
  LauncherinSports.Snapshot fixture=new LauncherinSports.Snapshot(new JSONObject().put("updated",now).put("upcoming",new JSONArray().put(match).put(new JSONObject(match.toString()).put("id","fixture2").put("time",now+86400000*3))).put("previous",before));Map<String,LauncherinSports.Snapshot> games=(Map<String,LauncherinSports.Snapshot>)field(dashboard,"sportsData");for(LauncherinSports.Team t:teams)games.put(t.id,fixture);invoke(dashboard,"showSports",new Class[]{List.class},teams);
  invoke(extra,"refreshAgenda",new Class[0]);LauncherinWidgetFaces.System system=(LauncherinWidgetFaces.System)field(extra,"systemFace");system.update(new LauncherinSystemStats.Sample(2040109465L,751619276L,6d,false,17179869184L,6442450944L),12.8);
  Bitmap picture=BitmapFactory.decodeFile(new File(sourceRoot(),"themes/nature-calm/wallpapers/004.jpg").getPath());assertNotNull(picture);((ImageView)field(extra,"picture")).setImageBitmap(picture);((TextView)field(extra,"photoHint")).setVisibility(View.GONE);
  LauncherinAudioWidget audio=(LauncherinAudioWidget)field(dashboard,"audio");audio.refresh(new JSONObject().put("title","Faixa de exemplo").put("artist","Artista de exemplo").put("playing",false).put("duration",220000).put("position",95000));((ImageView)field(audio,"cover")).setImageBitmap(picture);audio.pause();
  String[] families={"glass_premium","minimal_cinematic","ambient_dynamic"},themes={"aurora","ruska","horizon"},titles={"Glass Premium · paleta Aurora","Minimal Cinematic · paleta Outono","Ambient Dynamic · paleta Horizonte"};
  String[] order={"clock","weather","sports","music","system","agenda","photos"};
  for(int family=0;family<families.length;family++){
   JSONObject settings=new JSONObject().put("family",families[family]).put("theme",themes[family]).put("intensity",65);
   Files.write(new File(LauncherinAppearance.dir(host),"options.json").toPath(),settings.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));LauncherinAppearance.invalidate();dashboard.restyle();
   Bitmap out=Bitmap.createBitmap(1800,950,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);base(c,1800,950,LauncherinAppearance.get(host).accent);text(c,"Launcherin · "+titles[family],70,83,36,Color.WHITE);text(c,"Prévia local dos sete widgets · componentes reais do Android",70,125,22,0xffbec8d2);
   for(int i=0;i<order.length;i++){View card=cards.get(order[i]);assertNotNull(card);measure(card,200,121);if(card==audio){invoke(audio,"onSizeChanged",new Class[]{int.class,int.class,int.class,int.class},200,121,0,0);measure(card,200,121);((ImageView)field(audio,"cover")).setImageDrawable(null);((ImageView)field(audio,"cover")).setImageBitmap(picture);invoke(audio,"updateProgress",new Class[0]);assertEquals(12,((ImageView)field(audio,"cover")).getLeft());}int x=70+(i%4)*427,y=200+(i/4)*330;c.save();c.translate(x,y);c.scale(2,2);Path clip=new Path();clip.addRoundRect(1,1,199,120,18,18,Path.Direction.CW);c.clipPath(clip);if(i==0)card.setBackground(LauncherinSurface.card(host,48,true));card.draw(c);c.restore();}
   text(c,"GALERIA DE WIDGETS · DADOS DE EXEMPLO · NÃO ALTERA AS POSIÇÕES DA HOME",70,912,19,0xffb7c4d0);save(out,families[family]);
  }
  Files.write(new File(LauncherinAppearance.dir(host),"options.json").toPath(),new JSONObject().put("family","glass_premium").put("theme","aurora").put("intensity",65).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));LauncherinAppearance.invalidate();
  LauncherinWeatherCard weatherPopup=new LauncherinWeatherCard(host,cards.get("weather"),()->{});weatherPopup.render(weather,System.currentTimeMillis());weatherPopup.popup.restyle();renderPopup(weatherPopup.popup,"Previsão expandida · próximos 5 dias","weather-expanded");weatherPopup.popup.dismiss();
  LauncherinSportsCard sportsPopup=new LauncherinSportsCard(host,cards.get("sports"),teams,()->{});sportsPopup.render(games,false);sportsPopup.popup.restyle();renderPopup(sportsPopup.popup,"Esportes expandido · dois times reais","sports-expanded");sportsPopup.popup.dismiss();dashboard.close();
 }
 private void renderPopup(LauncherinCardPopup popup,String title,String name)throws Exception{Bitmap b=Bitmap.createBitmap(1800,900,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);base(c,1800,900,0xffbdc5a4);text(c,title,80,90,35,Color.WHITE);measure(popup.card,popup.width,popup.height);float scale=Math.min(1600f/popup.width,650f/popup.height);c.save();c.translate((1800-popup.width*scale)/2,170);c.scale(scale,scale);popup.card.setAlpha(1);popup.card.setScaleX(1);popup.card.setScaleY(1);Path round=new Path();round.addRoundRect(1,1,popup.width-1,popup.height-1,18,18,Path.Direction.CW);c.clipPath(round);popup.card.draw(c);c.restore();text(c,"PRÉVIA LOCAL · DADOS DE EXEMPLO · MESMA EXPANSÃO E NAVEGAÇÃO",80,855,20,0xffbec8d2);save(b,name);}
}
