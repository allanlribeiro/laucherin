package app.launcherin.tv;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @org.robolectric.annotation.Config(sdk=35,qualifiers="w960dp-h540dp-land-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LauncherinPlayerUiTest {
    private ActivityController<LauncherinPlayerActivity> lifecycle;private LauncherinPlayerActivity activity;
    @Before public void create(){new android.app.Instrumentation().setInTouchMode(false);Context c=RuntimeEnvironment.getApplication();LauncherinMusicLibrary lib=new LauncherinMusicLibrary(c);lib.write("queue",Arrays.asList(LauncherinNativeMusicTest.track(1),LauncherinNativeMusicTest.track(2)));lib.write("favorites",Arrays.asList(LauncherinNativeMusicTest.track(1)));lib.progress(0,35000);lifecycle=Robolectric.buildActivity(LauncherinPlayerActivity.class).create().start().resume().visible().windowFocusChanged(true);activity=lifecycle.get();measure(activity.findViewById(android.R.id.content));Shadows.shadowOf(Looper.getMainLooper()).idle();}
    @After public void close(){lifecycle.pause().stop().destroy();}
    private static void measure(View v){v.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));v.layout(0,0,960,540);}
    private TextView button(String label){return find(activity.findViewById(android.R.id.content),label);}
    private static TextView find(View v,String s){if(v instanceof TextView&&((TextView)v).getText().toString().equals(s))return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView t=find(((ViewGroup)v).getChildAt(i),s);if(t!=null)return t;}return null;}
    @Test public void remoteCanReachLibraryAndControlsWithoutTouch(){EditText input=ReflectionHelpers.getField(activity,"query");input.requestFocus();View focused=input.getRootView().findFocus();assertTrue("Initial focus was "+focused+" queryFocusable="+input.isFocusable()+" shown="+input.isShown()+" touch="+input.isInTouchMode()+" queryFocus="+input.hasFocus()+" root="+input.getRootView().findFocus(),focused instanceof EditText);View next=focused.focusSearch(View.FOCUS_RIGHT);assertNotNull(next);assertEquals("Buscar",((TextView)next).getText().toString());next.requestFocus();assertTrue("Search button could not take focus: "+next+" touch="+next.isInTouchMode(),next.hasFocus());for(String label:new String[]{"Fila","Favoritos","Artes","Letras","Anterior","Próxima","Apps externos"}){TextView b=button(label);assertNotNull(label,b);assertTrue(label,b.isFocusable());}}
    @Test public void artAndLyricsExpandInsidePlayerAndBackRestoresLibrary(){button("Artes").performClick();assertEquals(View.GONE,((View)ReflectionHelpers.getField(activity,"browser")).getVisibility());activity.onBackPressed();assertEquals(View.VISIBLE,((View)ReflectionHelpers.getField(activity,"browser")).getVisibility());assertFalse(activity.isFinishing());}
    @Test public void foregroundTvIconHasVisiblePixelsAfterRealViewLayout(){TextView tv=new TextView(activity);tv.setForeground(activity.getDrawable(R.drawable.ic_live_tv));tv.setForegroundGravity(Gravity.CENTER);tv.measure(View.MeasureSpec.makeMeasureSpec(48,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(48,View.MeasureSpec.EXACTLY));tv.layout(0,0,48,48);Bitmap b=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888);tv.draw(new Canvas(b));int pixels=0;for(int y=0;y<48;y++)for(int x=0;x<48;x++)if(Color.alpha(b.getPixel(x,y))>100)pixels++;assertTrue("TV glyph must survive foreground's intrinsic-size bounds",pixels>40);}
    @Test public void stoppedPlayerScreenStopsRefreshAndDropsArtwork(){lifecycle.pause().stop();assertFalse(ReflectionHelpers.getField(activity,"active"));assertNull(((ImageView)ReflectionHelpers.getField(activity,"artwork")).getDrawable());lifecycle.start().resume().visible();}
    @Test public void rhinoInterpreterWorksOnAndroidApi35(){assertEquals("cba",org.schabi.newpipe.extractor.utils.JavaScript.run("function f(x){return x.split('').reverse().join('');}","f","abc"));}
    @Test public void widgetLongOkOpensOwnPlayerWithoutMediaAccess(){ActivityController<LauncherinVisualPreviewTest.PreviewHost> host=Robolectric.buildActivity(LauncherinVisualPreviewTest.PreviewHost.class).setup();LauncherinAudioWidget widget=new LauncherinAudioWidget(host.get(),false);Button b=ReflectionHelpers.getField(widget,"play");assertFalse(LauncherinMediaService.granted(host.get()));assertTrue(b.performLongClick());Intent launch=Shadows.shadowOf(host.get()).getNextStartedActivity();assertEquals(LauncherinPlayerActivity.class.getName(),launch.getComponent().getClassName());widget.close();host.pause().stop().destroy();}
    @Test public void captureNativeLibraryAndAmbientLayouts()throws Exception {
        ImageView image=ReflectionHelpers.getField(activity,"artwork");Bitmap art=Bitmap.createBitmap(500,500,Bitmap.Config.RGB_565);Canvas c=new Canvas(art);Paint p=new Paint(3);p.setShader(new LinearGradient(0,0,500,500,new int[]{0xff11172a,0xff4a526c,0xff152d33},null,Shader.TileMode.CLAMP));c.drawRect(0,0,500,500,p);p.setShader(null);p.setColor(0xffdce8ea);p.setTextSize(150);c.drawText("♪",185,305,p);image.setImageBitmap(art);save("player-library");button("Artes").performClick();image.setImageBitmap(art);measure(activity.findViewById(android.R.id.content));save("player-art");
        ReflectionHelpers.setField(activity,"showLyrics",true);ReflectionHelpers.setField(activity,"lyrics",new LauncherinLyrics.Result("","[00:00]Linha de exemplo para visualizar\n[00:20]A letra acompanha a reprodução\n[00:40]Dados ilustrativos desta captura",false));ReflectionHelpers.setField(activity,"lastLine",-2);ReflectionHelpers.callInstanceMethod(activity,"renderLyrics",ReflectionHelpers.ClassParameter.from(long.class,35000L));image.setVisibility(View.GONE);((View)ReflectionHelpers.getField(activity,"lyricsScroll")).setVisibility(View.VISIBLE);measure(activity.findViewById(android.R.id.content));save("player-lyrics");
    }
    private void save(String name)throws Exception {View view=activity.findViewById(android.R.id.content);Bitmap b=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);view.draw(c);Paint p=new Paint(3);p.setColor(0xffa6b7c8);p.setTextSize(10);c.drawText("PRÉVIA LOCAL · COMPONENTES ANDROID · MÚSICA E LETRA DE EXEMPLO",30,533,p);File dir=new File(System.getProperty("launcherin.capture.dir","/tmp/launcherin19/captures"));dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
}
