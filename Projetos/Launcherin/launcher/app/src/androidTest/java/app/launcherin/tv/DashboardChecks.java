package app.launcherin.tv;

import android.app.Instrumentation;
import android.content.*;
import android.graphics.*;
import android.media.session.PlaybackState;
import android.media.tv.TvContract;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.io.*;

/** Cross-app media-session fixtures run only in our emulator; these are not real Spotify/YTM clients. */
final class DashboardChecks {
    private final Instrumentation instrument;
    private final Context context;
    private MainActivity activity;
    private static final String SPOTIFY="com.spotify.music", YTM="com.google.android.youtube.tv.music";
    DashboardChecks(Instrumentation instrument){this.instrument=instrument;context=instrument.getTargetContext();}
    private interface Probe { boolean ready() throws Exception; }
    private void await(Probe probe)throws Exception {long until=android.os.SystemClock.elapsedRealtime()+12000;do{if(probe.ready())return;Thread.sleep(80);}while(android.os.SystemClock.elapsedRealtime()<until);throw new AssertionError("Timed out waiting for media/UI: "+LauncherinMediaService.lastMusic(context));}
    private void ui(Runnable action){final Throwable[] failure={null};instrument.runOnMainSync(()->{try{action.run();}catch(Throwable error){failure[0]=error;}});if(failure[0]!=null)throw new AssertionError(failure[0]);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private void start(String pkg,String name,String artist,boolean subtitle,int state){
        context.startForegroundService(new Intent().setComponent(new ComponentName(pkg,"app.launcherin.fixture.MusicService"))
                .putExtra("title",name).putExtra("artist",artist).putExtra("subtitle",subtitle).putExtra("state",state));
    }
    void banner(){
        Bitmap image=Bitmap.createBitmap(200,100,Bitmap.Config.ARGB_8888);
        new Canvas(image).drawColor(Color.TRANSPARENT);android.graphics.Paint paint=new android.graphics.Paint();paint.setColor(0xff12abcd);
        new Canvas(image).drawRect(20,10,180,90,paint);
        android.graphics.drawable.Drawable trimmed=LauncherinAppArtwork.banner(context.getResources(),new android.graphics.drawable.BitmapDrawable(context.getResources(),image));
        Bitmap result=((android.graphics.drawable.BitmapDrawable)trimmed).getBitmap();
        require(result.getWidth()==160&&result.getHeight()==80,"Transparent padding still shrinks the visible logo");
        require(result.getPixel(80,40)==0xff12abcd,"Brand colours changed");
    }
    void spotify()throws Exception {
        for(String pkg:new String[]{SPOTIFY,YTM})context.stopService(new Intent().setComponent(new ComponentName(pkg,"app.launcherin.fixture.MusicService")));
        context.getSharedPreferences("launcherin-media",0).edit().clear().commit();
        start(SPOTIFY,"Faixa de teste Spotify","Artista de teste",false,PlaybackState.STATE_PLAYING);
        await(()->SPOTIFY.equals(LauncherinMediaService.lastMusic(context).optString("pkg"))&&"Faixa de teste Spotify".equals(LauncherinMediaService.lastMusic(context).optString("title"))&&LauncherinMediaService.lastMusic(context).optLong("position")>=90000&&LauncherinMediaService.lastMusic(context).optLong("duration")==240000);
        JSONObject track=LauncherinMediaService.lastMusic(context);
        require("Artista de teste".equals(track.optString("artist")),"Spotify artist missing");
        require(track.optLong("position")>=90000&&track.optLong("position")<240000&&track.optLong("duration")==240000,"Spotify progress missing");
        require(!track.optString("art").isEmpty(),"64px album cover rejected");
        Bitmap bitmap=BitmapFactory.decodeFile(track.optString("art"));
        require(bitmap!=null&&bitmap.getWidth()==64&&bitmap.getHeight()==64,"Cover not preserved");bitmap.recycle();
    }
    void transport()throws Exception {
        ui(()->LauncherinMediaService.toggleMusic(context));
        await(()->!LauncherinMediaService.lastMusic(context).optBoolean("playing"));
        ui(()->LauncherinMediaService.seek(context,120000));
        await(()->LauncherinMediaService.lastMusic(context).optLong("position")==120000);
        ui(()->LauncherinMediaService.skip(context,true));
        await(()->"Next fixture track".equals(LauncherinMediaService.lastMusic(context).optString("title")));
        ui(()->LauncherinMediaService.skip(context,false));
        await(()->"Previous fixture track".equals(LauncherinMediaService.lastMusic(context).optString("title")));
    }
    void youtubeMusic()throws Exception {
        start(YTM,"Faixa de teste YouTube Music","Artista de teste YT",true,PlaybackState.STATE_PLAYING);
        await(()->YTM.equals(LauncherinMediaService.lastMusic(context).optString("pkg"))&&"Faixa de teste YouTube Music".equals(LauncherinMediaService.lastMusic(context).optString("title")));
        require("Artista de teste YT".equals(LauncherinMediaService.lastMusic(context).optString("artist")),"DISPLAY_SUBTITLE not shown");
        start(SPOTIFY,"Spotify em pausa","Artista de teste",false,PlaybackState.STATE_PAUSED);
        Thread.sleep(300);
        require(YTM.equals(LauncherinMediaService.lastMusic(context).optString("pkg")),"Paused app overwrote current music");
        start(SPOTIFY,"Outra faixa em reprodução","Artista de teste",false,PlaybackState.STATE_PLAYING);
        await(()->SPOTIFY.equals(LauncherinMediaService.lastMusic(context).optString("pkg")));
        start(SPOTIFY,"Spotify em pausa","Artista de teste",false,PlaybackState.STATE_PAUSED);
        await(()->YTM.equals(LauncherinMediaService.lastMusic(context).optString("pkg")));
    }
    void tuner(){
        Intent trap=new Intent(Intent.ACTION_VIEW,TvContract.Channels.CONTENT_URI).setPackage("com.globo.globotv");
        require(trap.resolveActivity(context.getPackageManager())!=null,"Tuner trap fixture not installed");
        require(LauncherinTv.liveIntent(context,"com.globo.globotv","")==null,"Streaming app selected as tuner");
    }
    void showDashboard()throws Exception {
        context.getSharedPreferences("launcherin-dismissed",0).edit().clear().commit();
        instrument.setInTouchMode(false);
        activity=(MainActivity)instrument.startActivitySync(new Intent(context,MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("launcherin_preview",true));
        instrument.waitForIdleSync();
        await(()->{final boolean[] found={false};ui(()->found[0]=text(activity.getWindow().getDecorView(),"Faixa de teste YouTube Music")&&text(activity.getWindow().getDecorView(),"Artista de teste YT"));return found[0];});
    }
    private boolean text(View v,String value){if(v instanceof TextView&&value.contentEquals(((TextView)v).getText()))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(text(((ViewGroup)v).getChildAt(i),value))return true;return false;}
    void dock()throws Exception {
        final RecyclerView grid=activity.findViewById(R.id.grid);
        for(int scroll:new int[]{0,600,1200,0}){
            ui(()->{grid.scrollToPosition(scroll==0?0:Math.min(grid.getAdapter().getItemCount()-1,scroll==600?4:8));});
            instrument.waitForIdleSync();Thread.sleep(200);
            ui(()->{int w=-1,h=-1,count=0;for(int i=0;i<grid.getChildCount();i++){View card=grid.getChildAt(i).findViewById(R.id.card);if(card==null||card.getVisibility()!=View.VISIBLE)continue;if(w<0){w=card.getWidth();h=card.getHeight();}require(card.getWidth()==w&&card.getHeight()==h,"Unequal dock cards after scroll");require(w>0&&h>0,"Empty tile geometry");count++;}require(count>=3,"Too few tiles to compare");});
        }
    }
    void historyFocus()throws Exception {
        final View[] selected={null};
        ui(()->{ViewGroup root=activity.findViewById(android.R.id.content);java.util.ArrayList<View> cards=new java.util.ArrayList<>();collect(root,cards);require(cards.size()>=4,"Preview history missing");selected[0]=cards.get(1);selected[0].requestFocus();});
        await(()->{final boolean[] ready={false};ui(()->ready[0]=selected[0].hasFocus()&&Math.abs(selected[0].getScaleX()-1.035f*1.07f)<.002f);return ready[0];});
        ui(()->{View card=selected[0];require(card.hasFocus(),"History card lost focus");require(Math.abs(card.getScaleX()-1.035f*1.07f)<.002f,"Wrong selected scale");require(card.getForeground()!=null&&card.getTranslationZ()>0,"Selection outline is hidden");});
    }
    private void collect(View view,java.util.List<View> cards){if("launcherin-history".equals(view.getTag()))cards.add(view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collect(((ViewGroup)view).getChildAt(i),cards);}
    private void mode(String title,String extra,int state)throws Exception{
        context.startForegroundService(new Intent().setComponent(new ComponentName(YTM,"app.launcherin.fixture.MusicService"))
                .putExtra("title",title).putExtra("artist","Artista de teste").putExtra("state",state).putExtra(extra,true));
        await(()->title.equals(LauncherinMediaService.lastMusic(context).optString("title"))&&LauncherinMediaService.lastMusic(context).optBoolean("playing")== (state==PlaybackState.STATE_PLAYING));
    }
    private LauncherinAudioWidget audio(View view){if(view instanceof LauncherinAudioWidget)return (LauncherinAudioWidget)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){LauncherinAudioWidget found=audio(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}return null;}
    private void pressPlay(){
        ui(()->{LauncherinAudioWidget widget=audio(activity.getWindow().getDecorView());require(widget!=null&&widget.requestFocus(),"Music controls cannot receive focus");
            require("Reproduzir ou pausar".contentEquals(activity.getCurrentFocus().getContentDescription()),"Music widget did not focus Play");});
        ui(()->{
            long now=android.os.SystemClock.uptimeMillis();
            activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0));
            activity.dispatchKeyEvent(new KeyEvent(now,now+1,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0));
        });
    }
    private boolean playing(){return LauncherinMediaService.lastMusic(context).optBoolean("playing");}
    private void stillHome(){ui(()->require(activity.hasWindowFocus(),"Play unexpectedly left the launcher"));}
    void typedTuner(){
        String id="native.test/InputService/HW1";
        Intent automatic=LauncherinTv.tunerIntent("com.tcl.tv",id,"");
        require(TvContract.Channels.CONTENT_ITEM_TYPE.equals(automatic.getType()),"Tuner MIME type missing");
        require(TvContract.buildChannelUriForPassthroughInput(id).equals(automatic.getData()),"Not the Inputs menu tuner route");
        require("com.tcl.tv".equals(automatic.getPackage()),"Tuner not package-scoped");
        Intent configured=LauncherinTv.tunerIntent("com.tcl.tv",id,id);
        require(TvContract.buildChannelsUriForInput(id).equals(configured.getData())&&TvContract.Channels.CONTENT_TYPE.equals(configured.getType()),"Exact input profile ignored");
        require(LauncherinTv.tunerIntent("com.globo.globotv",id,"")==null,"Streaming app accepted as tuner");
    }
    void widgetToggleOnly()throws Exception{
        context.stopService(new Intent().setComponent(new ComponentName(SPOTIFY,"app.launcherin.fixture.MusicService")));
        mode("Controle pelo widget","button_only",PlaybackState.STATE_PAUSED);pressPlay();await(this::playing);stillHome();
        require("Media key test".equals(LauncherinMediaService.lastMusic(context).optString("artist")),"Did not use media-button callback");
        pressPlay();await(()->!playing());stillHome();
    }
    void widgetIgnoredTransport()throws Exception{
        mode("Comando direto ignorado","ignore_transport",PlaybackState.STATE_PAUSED);pressPlay();await(this::playing);stillHome();
        require("Media key test".equals(LauncherinMediaService.lastMusic(context).optString("artist")),"No media-key recovery");
    }
    void widgetMediaId()throws Exception{
        mode("Retomada por ID","id_only",PlaybackState.STATE_STOPPED);pressPlay();await(this::playing);stillHome();
        require("Media ID test".equals(LauncherinMediaService.lastMusic(context).optString("artist")),"Queue was not resumed by ID");
    }
    private void stopPlayer()throws Exception{
        ui(LauncherinMediaService::cancelMusicCommand);
        context.stopService(new Intent().setComponent(new ComponentName(YTM,"app.launcherin.fixture.MusicService")));
        await(()->{
            android.media.session.MediaSessionManager manager=context.getSystemService(android.media.session.MediaSessionManager.class);
            for(android.media.session.MediaController controller:manager.getActiveSessions(new ComponentName(context,LauncherinMediaService.class)))if(YTM.equals(controller.getPackageName()))return false;
            return true;
        });
        Thread.sleep(250);
    }
    void widgetBrowserResume()throws Exception{
        mode("Retomada sem abrir o app","normal",PlaybackState.STATE_PAUSED);stopPlayer();pressPlay();await(this::playing);stillHome();
        require("Retomada sem abrir o app".equals(LauncherinMediaService.lastMusic(context).optString("title")),"Wrong track after browser resume");
        require("Media ID test".equals(LauncherinMediaService.lastMusic(context).optString("artist")),"Browser did not resume the published media ID");
    }
    void widgetReceiverResume()throws Exception{
        mode("Retomada pelo receptor","reject_browser",PlaybackState.STATE_PAUSED);stopPlayer();pressPlay();await(this::playing);stillHome();
        require("Media receiver test".equals(LauncherinMediaService.lastMusic(context).optString("artist")),"Explicit background receiver was not used");
    }
    void widgetNotificationResume()throws Exception{
        mode("Retomada por notificação","private_controls",PlaybackState.STATE_PAUSED);
        await(()->LauncherinMusicActions.available(YTM).contains("Play"));
        stopPlayer();pressPlay();await(this::playing);stillHome();
        require("Notification resume test".equals(LauncherinMediaService.lastMusic(context).optString("artist")),"Creator-owned notification action not used");
    }
    void historyManagement()throws Exception{
        org.json.JSONArray backup=LauncherinMediaService.history(context);
        JSONObject originalRemoved=LauncherinMediaService.removed(context);
        try{
            android.app.UiAutomation automation=instrument.getUiAutomation();
            final java.util.ArrayList<View> cards=new java.util.ArrayList<>();
            ui(()->{collect(activity.findViewById(android.R.id.content),cards);require(cards.size()==4,"Expected four demo cards before removal");require(cards.get(0).performLongClick(),"No manual-removal menu on the card");});
            instrument.waitForIdleSync();
            final android.view.accessibility.AccessibilityNodeInfo[] option={null};
            await(()->{
                android.view.accessibility.AccessibilityNodeInfo root=automation.getRootInActiveWindow();
                if(root==null||!context.getPackageName().contentEquals(root.getPackageName()))return false;
                java.util.List<android.view.accessibility.AccessibilityNodeInfo> choices=root.findAccessibilityNodeInfosByText("Remover da lista");
                if(choices.isEmpty())return false;option[0]=choices.get(0);return true;
            });
            if(!option[0].performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)){
                android.graphics.Rect bounds=new android.graphics.Rect();option[0].getBoundsInScreen(bounds);long now=android.os.SystemClock.uptimeMillis();
                MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY(),0);
                MotionEvent up=MotionEvent.obtain(now,now+1,MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY(),0);
                down.setSource(InputDevice.SOURCE_TOUCHSCREEN);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                try{require(automation.injectInputEvent(down,true)&&automation.injectInputEvent(up,true),"Cannot activate removal row");}finally{down.recycle();up.recycle();}
            }
            instrument.setInTouchMode(false);
            await(()->{final int[] count={0};ui(()->{java.util.ArrayList<View> left=new java.util.ArrayList<>();collect(activity.findViewById(android.R.id.content),left);count[0]=left.size();});return count[0]==3;});
            org.json.JSONArray raw=new org.json.JSONArray();
            raw.put(new JSONObject().put("key","youtube").put("pkg","com.google.android.youtube.tv").put("provider","YouTube").put("updated",999999));
            for(int i=0;i<14;i++)raw.put(new JSONObject().put("key","fixture-film-"+i).put("pkg","com.netflix.ninja").put("provider","Netflix").put("title","Fixture film "+i).put("updated",i).put("position",100).put("duration",10000));
            LauncherinMediaService.save(context,raw);
            org.json.JSONArray visible=LauncherinMediaService.history(context);require(visible.length()==10,"Legacy cache not capped at ten");
            for(int i=0;i<visible.length();i++)require(!"YouTube".equals(visible.getJSONObject(i).optString("provider")),"YouTube survived cached-history migration");
            JSONObject removed=visible.getJSONObject(0);LauncherinMediaService.dismiss(context,removed);
            require(LauncherinMediaService.removed(context).has(LauncherinHistory.key(removed)),"Removal not persisted");
            LauncherinMediaService.importPrograms(context,new org.json.JSONArray().put(removed));
            for(int i=0;i<LauncherinMediaService.history(context).length();i++)require(!LauncherinHistory.key(removed).equals(LauncherinHistory.key(LauncherinMediaService.history(context).getJSONObject(i))),"Refresh reinserted removed item");
            LauncherinMediaService.updateVideo(context,removed,false);
            require(LauncherinMediaService.removed(context).has(LauncherinHistory.key(removed)),"Paused refresh undid removal");
            LauncherinMediaService.updateVideo(context,removed,true);
            require(!LauncherinMediaService.removed(context).has(LauncherinHistory.key(removed)),"New playback did not restore item");
        }finally{LauncherinMediaService.save(context,backup);context.getSharedPreferences("launcherin-dismissed",0).edit().putString("items",originalRemoved.toString()).commit();}
    }
    void screenshot()throws Exception {Thread.sleep(500);Bitmap bitmap=instrument.getUiAutomation().takeScreenshot();require(bitmap!=null,"Screenshot unavailable");try(OutputStream output=new FileOutputStream(new File(context.getExternalFilesDir(null),"dashboard-preview6.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}finally{bitmap.recycle();}}
}
