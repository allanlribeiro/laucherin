package app.launcherin.tv;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

final class WidgetChecks {
    private final Instrumentation instrument;private final Context context;private MainActivity activity;
    WidgetChecks(Instrumentation i){instrument=i;context=i.getTargetContext();}
    private void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private File file(String name){File dir=Config.dir(context);if(dir==null)dir=context.getFilesDir();dir.mkdirs();return new File(dir,name);}
    private void write(String name,byte[] bytes)throws Exception{try(OutputStream out=new FileOutputStream(file(name))){out.write(bytes);}}
    private void ui(Runnable action){final Throwable[] failure={null};instrument.runOnMainSync(()->{try{action.run();}catch(Throwable error){failure[0]=error;}});if(failure[0]!=null)throw new AssertionError(failure[0].getMessage(),failure[0]);}
    private interface Ready{boolean ready()throws Exception;}
    private void await(Ready check)throws Exception{long until=SystemClock.elapsedRealtime()+15000;while(SystemClock.elapsedRealtime()<until){if(check.ready())return;Thread.sleep(100);}throw new AssertionError("Widget update timed out");}
    private View find(String text){return find(activity.getWindow().getDecorView(),text);}
    private View find(View v,String text){if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View result=find(((ViewGroup)v).getChildAt(i),text);if(result!=null)return result;}return null;}
    private View tagged(String tag){return activity.getWindow().getDecorView().findViewWithTag(tag);}
    private void launch(JSONObject config)throws Exception{
        if(activity!=null){ui(()->activity.finish());activity=null;}
        write("config.json",config.toString().getBytes(StandardCharsets.UTF_8));
        instrument.setInTouchMode(false);
        activity=(MainActivity)instrument.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("launcherin_preview",true));
        await(()->{final boolean[] yes={false};ui(()->yes[0]=activity.hasWindowFocus()&&tagged("shelf")!=null&&tagged("shelf").getHeight()>0);return yes[0];});
    }
    private JSONObject config(String mode,JSONArray slots)throws Exception{return new JSONObject().put("dashboard_widgets",slots).put("watch_next_mode",mode).put("idle_screensaver_minutes",0).put("idle_standby_minutes",0).put("seasonal_theme","none").put("weather_city_query","").put("sports_favorite_teams",new JSONArray());}
    private JSONArray all(){JSONArray a=new JSONArray();for(int i=0;i<7;i++)a.put(new LauncherinWidgetLayout.Slot(LauncherinWidgetLayout.IDS.get(i),i<4?i*3:(i-4)*4,i<4?0:1,i<4?3:4,1,true).json());return a;}
    void layoutAndOfflinePhotos()throws Exception{
        Bitmap bitmap=Bitmap.createBitmap(160,90,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xff4380ac);ByteArrayOutputStream out=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);bitmap.recycle();
        byte[] jpeg=out.toByteArray();byte[] hash=MessageDigest.getInstance("SHA-256").digest(jpeg);StringBuilder digest=new StringBuilder();for(byte b:hash)digest.append(String.format(Locale.ROOT,"%02x",b&255));String id=digest.toString();write("frame-"+id.substring(0,40)+".jpg",jpeg);
        launch(config("above",all()).put("frame_photos",new JSONArray().put(id)));
        await(()->{final boolean[] ready={false};ui(()->{View v=find("PORTA-RETRATOS");if(v!=null){FrameLayout card=(FrameLayout)v.getParent();ready[0]=((ImageView)card.getChildAt(0)).getDrawable()!=null;}});return ready[0];});
        ui(()->{
            ViewGroup board=(ViewGroup)tagged("launcherin-widget-board");require(board.getChildCount()==7,"Seven widgets not visible");
            for(int i=0;i<board.getChildCount();i++){View a=board.getChildAt(i);require(a.getWidth()>0&&a.getHeight()>0,"Empty widget bounds");Rect ar=new Rect(a.getLeft(),a.getTop(),a.getRight(),a.getBottom());
                require(ar.left>=0&&ar.top>=0&&ar.right<=board.getWidth()&&ar.bottom<=board.getHeight(),"Widget outside board");
                for(int j=0;j<i;j++){View b=board.getChildAt(j);require(!Rect.intersects(ar,new Rect(b.getLeft(),b.getTop(),b.getRight(),b.getBottom())),"Overlapping widgets");}}
            View photos=find("PORTA-RETRATOS");ImageView image=(ImageView)((ViewGroup)photos.getParent()).getChildAt(0);require(image.getDrawable() instanceof BitmapDrawable,"Offline photo did not decode");
        });
    }
    void agendaDoesNotRestartOrChangeConfiguration()throws Exception{
        byte[] before;try(InputStream in=new FileInputStream(file("config.json"))){before=LauncherinExtras.bytes(in,65536);}
        JSONObject event=new JSONObject().put("id","native-event").put("title","Compromisso de exemplo").put("date",LocalDate.now().toString()).put("time","14:30");
        write(LauncherinAgendaData.PENDING,new JSONObject().put("events",new JSONArray().put(event)).toString().getBytes(StandardCharsets.UTF_8));
        String response;try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(instrument.getUiAutomation().executeShellCommand("content call --uri content://app.launcherin.tv.theme/agenda.pending.json --method commitAgenda"))){response=new String(LauncherinExtras.bytes(in,32768),StandardCharsets.UTF_8);}
        require(response.contains("ok=true"),"Provider did not atomically commit agenda");
        await(()->{final boolean[] ready={false};ui(()->ready[0]=find("14:30 · Compromisso de exemplo")!=null);return ready[0];});
        byte[] after;try(InputStream in=new FileInputStream(file("config.json"))){after=LauncherinExtras.bytes(in,65536);}
        require(Arrays.equals(before,after),"Agenda changed launcher configuration");ui(()->require(!activity.isDestroyed()&&activity.hasWindowFocus(),"Agenda restarted or left launcher"));
        byte[] data;try(InputStream in=new FileInputStream(file(LauncherinAgendaData.FILE))){data=LauncherinExtras.bytes(in,32768);}
        write(LauncherinAgendaData.PENDING,"broken".getBytes());
        try{LauncherinAgendaData.commit(context);throw new AssertionError("Malformed agenda committed");}catch(IOException expected){}
        try(InputStream in=new FileInputStream(file(LauncherinAgendaData.FILE))){require(Arrays.equals(data,LauncherinExtras.bytes(in,32768)),"Failed write erased agenda");}
    }
    void hiddenWidgetsAndHistoryHaveNoPlaceholders()throws Exception{
        launch(config("hidden",new JSONArray()));
        ui(()->{require(tagged("launcherin-widget-board").getVisibility()==View.GONE,"Widget board reserved visible cards");require(tagged("shelf").getVisibility()==View.GONE,"History still shown");activity.launcherinFocusApps();activity.onTileVerticalKey(0,KeyEvent.KEYCODE_DPAD_UP);require(activity.getCurrentFocus()!=null,"Focus lost with empty widgets");});
    }
    void belowHistoryKeepsTheAppBarVisible()throws Exception{
        launch(config("below",new JSONArray()));
        ui(()->{require(tagged("shelf").getTop()>=activity.getWindow().getDecorView().getHeight(),"Below history appears before Down");activity.launcherinFocusApps();activity.onTileVerticalKey(0,KeyEvent.KEYCODE_DPAD_DOWN);});
        Thread.sleep(500);
        ui(()->{
            View apps=tagged("launcherin-apps"),shelf=tagged("shelf");Rect appRect=new Rect(),watchRect=new Rect();
            require(apps.getGlobalVisibleRect(appRect)&&appRect.height()>=apps.getHeight()*.95,"Apps left the screen");
            require(shelf.getGlobalVisibleRect(watchRect)&&watchRect.height()>shelf.getHeight()*.9,"History not revealed");
            require(appRect.bottom<watchRect.top,"Apps not above history");require(activity.getCurrentFocus()!=null,"No focused content");
        });
        screenshot("continue-below-preview10.png");
        ui(()->{
            long now=SystemClock.uptimeMillis();activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_UP,0));activity.dispatchKeyEvent(new KeyEvent(now,now+1,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_UP,0));
        });Thread.sleep(500);
        ui(()->{View shelf=tagged("shelf");int[] xy=new int[2];shelf.getLocationOnScreen(xy);require(xy[1]>=activity.getWindow().getDecorView().getHeight(),"Up did not hide history");});
    }
    void systemShowsRealMemory()throws Exception{
        launch(config("above",all()));
        await(()->{final boolean[] ready={false};ui(()->ready[0]=hasRam(activity.getWindow().getDecorView()));return ready[0];});
        LauncherinSystemStats.Sample s=new LauncherinSystemStats().sample(context);require(s.totalMemory>0&&s.availableMemory>=0&&s.availableMemory<=s.totalMemory,"Invalid system RAM sample");
    }
    private boolean hasRam(View view){if(view instanceof TextView){String text=((TextView)view).getText().toString();if(text.startsWith("RAM · ")&&text.endsWith(" GB"))return true;}if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasRam(((ViewGroup)view).getChildAt(i)))return true;return false;}
    void screenshot(String name)throws Exception{Thread.sleep(300);Bitmap image=instrument.getUiAutomation().takeScreenshot();require(image!=null,"Screenshot unavailable");try(OutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}}
}
