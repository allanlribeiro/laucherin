package app.launcherin.tv;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Bundle;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.nio.file.Files;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LauncherinAppearanceTest {
 private Context c;
 @Before public void before(){c=RuntimeEnvironment.getApplication();delete(LauncherinAppearance.dir(c));LauncherinAppearance.dir(c);LauncherinAppearance.invalidate();LauncherinAppearance.listen(null);}
 @After public void after(){LauncherinAppearance.listen(null);LauncherinAppearance.invalidate();}
 private void delete(File f){File[] all=f.listFiles();if(all!=null)for(File child:all){if(child.isDirectory())delete(child);child.delete();}}
 private Bundle commit(JSONObject patch)throws Exception{Files.write(LauncherinAppearance.incoming(c,"appearance.pending.json").toPath(),patch.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));return LauncherinAppearance.call(c,"commitAppearance");}
 private String photo(int color)throws Exception{Bitmap b=Bitmap.createBitmap(80,45,Bitmap.Config.ARGB_8888);b.eraseColor(color);File tmp=new File(c.getCacheDir(),"appearance-test.jpg");try(OutputStream out=new FileOutputStream(tmp)){assertTrue(b.compress(Bitmap.CompressFormat.JPEG,90,out));}b.recycle();String sha=LauncherinAppearance.hash(tmp);Files.copy(tmp.toPath(),LauncherinAppearance.incoming(c,"av-"+sha.substring(0,40)+".jpg").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);return sha;}
 @Test public void appearanceCannotModifyLayoutAppsPermissionsOrTimers()throws Exception{
  for(String key:new String[]{"dock","dashboard_widgets","home_key","idle_standby_minutes","settings_key_panel","tmdb_api_key"})assertThrows(Exception.class,()->LauncherinAppearance.validate(c,new JSONObject(),new JSONObject().put(key,true)));
 }
 @Test public void rejectsUnknownKeysAndMalformedRangesAndFilenames()throws Exception{
  assertFalse(commit(new JSONObject().put("columns",99)).getBoolean("ok"));assertFalse(commit(new JSONObject().put("intensity",101)).getBoolean("ok"));assertFalse(commit(new JSONObject().put("intensity","45")).getBoolean("ok"));assertFalse(commit(new JSONObject().put("theme","missing-theme")).getBoolean("ok"));assertFalse(LauncherinAppearance.pending("../options.json"));assertFalse(LauncherinAppearance.pending("av-"+"a".repeat(40)+"Xjpg"));assertTrue(LauncherinAppearance.pending("av-"+"a".repeat(40)+".jpg"));
 }
 @Test public void themeChangesKeepPersonalPhotoConfigAndBackgroundSelection()throws Exception{
  File config=new File(Config.dir(c),"config.json");config.getParentFile().mkdirs();byte[] original="{\"dock\":[\"keep\"],\"idle_standby_minutes\":10}".getBytes();Files.write(config.toPath(),original);String sha=photo(Color.GREEN);
  assertTrue(commit(new JSONObject().put("wallpaper_mode","personal").put("personal_sha",sha)).getBoolean("ok"));String key=LauncherinAppearance.backgroundKey(LauncherinAppearance.saved(c));assertTrue(commit(new JSONObject().put("theme","ruska").put("family","minimal_cinematic")).getBoolean("ok"));
  assertEquals(sha,LauncherinAppearance.saved(c).getString("personal_sha"));assertEquals("personal",LauncherinAppearance.saved(c).getString("wallpaper_mode"));assertEquals(key,LauncherinAppearance.backgroundKey(LauncherinAppearance.saved(c)));assertArrayEquals(original,Files.readAllBytes(config.toPath()));assertTrue(LauncherinAppearance.image(c,sha).isFile());
  LauncherinAppearance.invalidate();assertEquals("ruska",LauncherinAppearance.get(c).theme);
 }
 @Test public void corruptImagesCannotReplaceThePreviousTheme()throws Exception{
  assertTrue(commit(new JSONObject().put("theme","aurora")).getBoolean("ok"));String sha=photo(Color.RED);Files.write(LauncherinAppearance.incoming(c,"av-"+sha.substring(0,40)+".jpg").toPath(),new byte[]{1,2,3});assertFalse(commit(new JSONObject().put("wallpaper_mode","personal").put("personal_sha",sha)).getBoolean("ok"));assertEquals("aurora",LauncherinAppearance.saved(c).getString("theme"));assertFalse(LauncherinAppearance.saved(c).has("personal_sha"));
 }
 @Test public void invalidImagesWithValidHashAreStillRejected()throws Exception{
  File tmp=new File(c.getCacheDir(),"invalid.jpg");Files.write(tmp.toPath(),"not an image".getBytes());String sha=LauncherinAppearance.hash(tmp);Files.copy(tmp.toPath(),LauncherinAppearance.incoming(c,"av-"+sha.substring(0,40)+".jpg").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);assertFalse(commit(new JSONObject().put("wallpaper_mode","personal").put("personal_sha",sha)).getBoolean("ok"));
 }
 @Test public void listenerFailureRollsBackAndReportsFailure()throws Exception{
  assertTrue(commit(new JSONObject().put("theme","aurora")).getBoolean("ok"));LauncherinAppearance.Listener listener=wallpaper->{if("ruska".equals(LauncherinAppearance.saved(c).optString("theme")))throw new IllegalStateException("simulate view failure");};LauncherinAppearance.listen(listener);Bundle result=commit(new JSONObject().put("theme","ruska"));assertFalse(result.getBoolean("ok"));assertEquals("aurora",LauncherinAppearance.saved(c).getString("theme"));assertEquals("aurora",LauncherinAppearance.get(c).theme);
 }
 @Test public void dynamicCollectionAndOriginalAndPersonalSourcesRemainIndependent()throws Exception{
  String first=photo(Color.RED),second=photo(Color.BLUE);assertTrue(commit(new JSONObject().put("wallpaper_mode","dynamic").put("dynamic",new JSONArray().put(first).put(second)).put("rotation_minutes",5).put("personal_sha",first)).getBoolean("ok"));assertEquals(2,LauncherinAppearance.saved(c).getJSONArray("dynamic").length());assertTrue(LauncherinAppearance.image(c,second).isFile());
  assertTrue(commit(new JSONObject().put("wallpaper_mode","theme").put("original_sha",second)).getBoolean("ok"));assertEquals(first,LauncherinAppearance.saved(c).getString("personal_sha"));assertEquals(second,LauncherinAppearance.saved(c).getString("original_sha"));
 }
 @Test public void photoCacheRetainsCurrentAndPreviousSelectionsOnly()throws Exception{
  String first=photo(Color.RED);assertTrue(commit(new JSONObject().put("wallpaper_mode","personal").put("personal_sha",first)).getBoolean("ok"));String second=photo(Color.BLUE);assertTrue(commit(new JSONObject().put("personal_sha",second)).getBoolean("ok"));assertTrue(LauncherinAppearance.image(c,first).isFile());String third=photo(Color.GREEN);assertTrue(commit(new JSONObject().put("personal_sha",third)).getBoolean("ok"));assertFalse(LauncherinAppearance.image(c,first).exists());assertTrue(LauncherinAppearance.image(c,second).isFile());assertTrue(LauncherinAppearance.image(c,third).isFile());
 }
 @Test public void paletteSamplingKeepsPixelsAndProvidesReadableThemeSpecificAccents()throws Exception{
  Bitmap b=Bitmap.createBitmap(64,36,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);int original=b.getPixel(0,0);assertTrue(commit(new JSONObject().put("auto_palette",true)).getBoolean("ok"));LauncherinAppearance.sample(c,b,"white");assertEquals(original,b.getPixel(0,0));assertTrue(LauncherinAppearance.get(c).lightWallpaper);assertTrue(LauncherinAppearance.luminance(LauncherinAppearance.get(c).accent)>=.35);
  assertTrue(commit(new JSONObject().put("auto_palette",false).put("theme","ruska")).getBoolean("ok"));int warm=LauncherinAppearance.get(c).accent;assertTrue(commit(new JSONObject().put("theme","aurora")).getBoolean("ok"));int green=LauncherinAppearance.get(c).accent;assertNotEquals(warm,green);b.recycle();
 }
 @Test public void everyExistingPaletteAcceptsAllThreeSharedFamilies()throws Exception{
  JSONArray themes=LauncherinAppearance.palettes(c).getJSONArray("themes");assertTrue(themes.length()>=16);for(int i=0;i<themes.length();i++)for(String family:new String[]{"glass_premium","minimal_cinematic","ambient_dynamic"}){String id=themes.getJSONObject(i).getString("id");assertTrue(id,commit(new JSONObject().put("theme",id).put("family",family)).getBoolean("ok"));LauncherinAppearance.Style style=LauncherinAppearance.get(c);assertEquals(id,style.theme);assertEquals(family,style.family);assertTrue(LauncherinAppearance.luminance(style.accent)>=.35);}
 }
 @Test public void surfacesHaveDistinctFinishesAndSameExactDrawableBounds()throws Exception{
  int[] values=new int[3];String[] families={"glass_premium","minimal_cinematic","ambient_dynamic"};for(int i=0;i<3;i++){assertTrue(commit(new JSONObject().put("family",families[i]).put("theme","aurora")).getBoolean("ok"));GradientDrawable d=LauncherinSurface.card(c,48,true);d.setBounds(0,0,200,121);Bitmap b=Bitmap.createBitmap(200,121,Bitmap.Config.ARGB_8888);d.draw(new Canvas(b));values[i]=b.getPixel(70,25);assertEquals(200,d.getBounds().width());assertEquals(121,d.getBounds().height());b.recycle();}assertNotEquals(values[0],values[1]);assertNotEquals(values[0],values[2]);
 }
 @Test public void uniformBackgroundRemovalPreservesOriginalForegroundColorAndDimensions(){
  Bitmap b=Bitmap.createBitmap(120,60,Bitmap.Config.ARGB_8888);b.eraseColor(Color.BLACK);Paint p=new Paint();p.setColor(Color.RED);new Canvas(b).drawRect(40,15,80,45,p);Drawable d=LauncherinAppFinish.mask(c.getResources(),new BitmapDrawable(c.getResources(),b));Bitmap result=((BitmapDrawable)d).getBitmap();assertEquals(120,result.getWidth());assertEquals(60,result.getHeight());assertEquals(Color.RED,result.getPixel(50,20));assertEquals(0,Color.alpha(result.getPixel(0,0)));
 }
 @Test public void firstAppBindingKeepsItsExistingRoundCornersBeforeMeasurement()throws Exception{
  androidx.recyclerview.widget.RecyclerView grid=new androidx.recyclerview.widget.RecyclerView(c);grid.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(c,androidx.recyclerview.widget.RecyclerView.HORIZONTAL,false));grid.measure(android.view.View.MeasureSpec.makeMeasureSpec(960,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(180,android.view.View.MeasureSpec.EXACTLY));AppAdapter adapter=new AppAdapter(c,null,6,Config.load(c));java.lang.reflect.Field entries=AppAdapter.class.getDeclaredField("items");entries.setAccessible(true);java.util.List<AppAdapter.Entry> list=(java.util.List<AppAdapter.Entry>)entries.get(adapter);list.add(new AppAdapter.Entry("Example",new android.content.ComponentName("com.example.tv","com.example.tv.MainActivity"),new ColorDrawable(Color.BLUE),new ColorDrawable(Color.BLUE)));AppAdapter.Holder holder=(AppAdapter.Holder)adapter.onCreateViewHolder(grid,0);adapter.onBindViewHolder(holder,0);assertEquals(0,holder.card.getWidth());float original=AppAdapter.cornerRadius(holder.card.getLayoutParams().width,c.getResources().getDisplayMetrics().density);assertTrue(original>0);assertEquals(original,((GradientDrawable)holder.card.getBackground()).getCornerRadius(),.01f);holder.card.layout(0,0,holder.card.getLayoutParams().width,holder.card.getLayoutParams().height);android.graphics.Outline actual=new android.graphics.Outline();holder.card.getOutlineProvider().getOutline(holder.card,actual);assertEquals(actual.getRadius(),((GradientDrawable)holder.card.getBackground()).getCornerRadius(),.01f);
 }
 @Test public void gradientBrandArtKeepsItsOriginalBackground(){
  Bitmap b=Bitmap.createBitmap(120,60,Bitmap.Config.ARGB_8888);Paint p=new Paint();p.setShader(new LinearGradient(0,0,120,60,Color.RED,Color.BLUE,Shader.TileMode.CLAMP));new Canvas(b).drawRect(0,0,120,60,p);Drawable original=new BitmapDrawable(c.getResources(),b);assertSame(original,LauncherinAppFinish.mask(c.getResources(),original));
 }
}
