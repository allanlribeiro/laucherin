package app.launcherin.tv;

import android.content.*;
import android.app.Notification;
import android.app.Activity;
import android.view.KeyEvent;
import android.service.notification.StatusBarNotification;
import android.media.MediaMetadata;
import android.media.session.*;
import android.service.notification.NotificationListenerService;
import android.os.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Public media sessions; music notifications fill missing title/artist only for their own media token. */
public final class LauncherinMediaService extends NotificationListenerService {
 static final String CHANGED="app.launcherin.tv.MEDIA_CHANGED";
 private static volatile LauncherinMediaService current;
 private MediaSessionManager manager;
 private LauncherinGoalMonitor goals;
 private final Map<MediaSession.Token,MediaController> controllers=new LinkedHashMap<>();
 // getPlaybackState() can rewrite its timestamp while extrapolating position. Rank only
 // actual playback callbacks, otherwise map iteration order masquerades as recent activity.
 private final Map<MediaSession.Token,Long> activityTimes=new HashMap<>();
 private final Map<MediaSession.Token,Integer> playbackStates=new HashMap<>();
 private final Map<MediaSession.Token,MediaController.Callback> callbacks=new HashMap<>();
 private final ExecutorService storage=Executors.newSingleThreadExecutor();
 private final MediaSessionManager.OnActiveSessionsChangedListener listener=this::sessions;
 static String provider(String pkg){if(pkg.contains("netflix"))return "Netflix";if(pkg.contains("amazonvideo")||pkg.contains("avod"))return "Prime Video";if(pkg.contains("disneyplus"))return "Disney+";if(pkg.startsWith("com.hbo.")||pkg.startsWith("com.wbd."))return "HBO Max";if(pkg.startsWith("com.google.android.youtube"))return "YouTube";return "";}
 static boolean music(String pkg){return LauncherinMusic.dedicated(pkg);}
 static boolean granted(Context context){try{ComponentName component=new ComponentName(context,LauncherinMediaService.class);if(Build.VERSION.SDK_INT>=27)return context.getSystemService(android.app.NotificationManager.class).isNotificationListenerAccessGranted(component);String enabled=android.provider.Settings.Secure.getString(context.getContentResolver(),"enabled_notification_listeners");if(enabled!=null)for(String value:enabled.split(":"))if(component.equals(ComponentName.unflattenFromString(value)))return true;}catch(RuntimeException ignored){}return false;}
 private final Handler main=new Handler(Looper.getMainLooper());
 private static long reboundAt;
 static void refreshSessions(Context context){
  LauncherinMediaService service=current;
  if(service!=null&&service.manager!=null){service.main.post(()->{try{service.sessions(service.manager.getActiveSessions(new ComponentName(service,LauncherinMediaService.class)));}catch(RuntimeException ignored){}});}
  else if(granted(context)&&SystemClock.elapsedRealtime()-reboundAt>10000){reboundAt=SystemClock.elapsedRealtime();try{requestRebind(new ComponentName(context,LauncherinMediaService.class));}catch(RuntimeException ignored){}}
 }
 @Override public void onListenerConnected(){current=this;if(goals!=null)goals.stop();goals=new LauncherinGoalMonitor(this);goals.start();try{manager=getSystemService(MediaSessionManager.class);manager.addOnActiveSessionsChangedListener(listener,new ComponentName(this,LauncherinMediaService.class));sessions(manager.getActiveSessions(new ComponentName(this,LauncherinMediaService.class)));for(StatusBarNotification notification:getActiveNotifications())onNotificationPosted(notification);}catch(RuntimeException ignored){}LauncherinIdle.listen(this);}
 private void observe(MediaController controller){
  MediaSession.Token token=controller.getSessionToken();if(controllers.containsKey(token))return;
  MediaController.Callback callback=new MediaController.Callback(){@Override public void onMetadataChanged(MediaMetadata metadata){capture(controller);LauncherinIdle.apply(LauncherinMediaService.this);}@Override public void onPlaybackStateChanged(PlaybackState state){activityTimes.put(token,state==null?0L:state.getLastPositionUpdateTime());int status=state==null?PlaybackState.STATE_NONE:state.getState();Integer previous=playbackStates.put(token,status);capture(controller,null,status==PlaybackState.STATE_PLAYING&&(previous==null||previous!=PlaybackState.STATE_PLAYING));LauncherinIdle.apply(LauncherinMediaService.this);}@Override public void onSessionDestroyed(){MediaController gone=controllers.remove(token);activityTimes.remove(token);playbackStates.remove(token);MediaController.Callback callback=callbacks.remove(token);if(gone!=null&&callback!=null)gone.unregisterCallback(callback);LauncherinIdle.apply(LauncherinMediaService.this);sendBroadcast(new Intent(CHANGED).setPackage(getPackageName()));}};
  controllers.put(token,controller);activityTimes.put(token,0L);PlaybackState initial=controller.getPlaybackState();playbackStates.put(token,initial==null?PlaybackState.STATE_NONE:initial.getState());callbacks.put(token,callback);controller.registerCallback(callback,main);
 }
 private void sessions(List<MediaController> sessions){
  Set<MediaSession.Token> live=new HashSet<>();for(MediaController controller:sessions)live.add(controller.getSessionToken());
  for(MediaSession.Token token:new ArrayList<>(controllers.keySet()))if(!live.contains(token)){MediaController gone=controllers.remove(token);activityTimes.remove(token);playbackStates.remove(token);MediaController.Callback callback=callbacks.remove(token);if(gone!=null&&callback!=null)gone.unregisterCallback(callback);}
  for(MediaController controller:sessions)if(music(controller.getPackageName())||!provider(controller.getPackageName()).isEmpty())observe(controller);
  for(MediaController controller:new ArrayList<>(controllers.values()))capture(controller);
  LauncherinIdle.apply(this);
 }
 @Override public void onNotificationPosted(StatusBarNotification posted){
  String pkg=posted.getPackageName();if(!music(pkg)&&!pkg.equals("com.google.android.youtube.tv"))return;
  try{
   Notification notification=posted.getNotification();MediaSession.Token token=notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
   if(token==null)return;MediaController controller=new MediaController(this,token);
   if(!pkg.equals(controller.getPackageName())||!LauncherinMusic.matchesSaved(pkg,controller.getMetadata(),lastMusic(this)))return;
   LauncherinMusicActions.capture(this,posted,controller);observe(controller);capture(controller,notification);
  }catch(RuntimeException ignored){}
 }
 private MediaController selectedMusic(){
  MediaController best=null;int score=-1;long newest=-1;String previous=lastMusic(this).optString("pkg");
  for(MediaController controller:controllers.values())try{
   if(!LauncherinMusic.matchesSaved(controller.getPackageName(),controller.getMetadata(),lastMusic(this)))continue;
   PlaybackState state=controller.getPlaybackState();int value=state==null?0:state.getState()==PlaybackState.STATE_PLAYING?300:state.getState()==PlaybackState.STATE_BUFFERING?200:state.getState()==PlaybackState.STATE_PAUSED?100:10;
   long time=activityTimes.getOrDefault(controller.getSessionToken(),0L);if(value>score||value==score&&(time>newest||time==newest&&previous.equals(controller.getPackageName()))){best=controller;score=value;newest=time;}
  }catch(RuntimeException ignored){}return best;
 }
 private static MediaController musicController(){LauncherinMediaService service=current;return service==null?null:service.selectedMusic();}
 private void capture(MediaController controller){capture(controller,null);}
 private void capture(MediaController source,Notification suppliedNotification){capture(source,suppliedNotification,false);}
 private void capture(MediaController source,Notification suppliedNotification,boolean newPlay){
  MediaController preferred=LauncherinMusic.matchesSaved(source.getPackageName(),source.getMetadata(),lastMusic(this))?selectedMusic():null;
  MediaController controller=preferred==null?source:preferred;
  final Notification notification=controller.getSessionToken().equals(source.getSessionToken())?suppliedNotification:null;
  final MediaMetadata supplied=controller.getMetadata();PlaybackState state=controller.getPlaybackState();String pkg=controller.getPackageName();
  boolean audio=LauncherinMusic.matchesSaved(pkg,supplied,lastMusic(this));
  if(!audio&&!LauncherinHistory.videoPackage(pkg))return;
  if(storage.isShutdown())return;try{storage.execute(()->{try{
   MediaMetadata metadata=supplied;
   String title=metadata==null?"":metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);if((title==null||title.trim().isEmpty())&&metadata!=null)title=metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
   getSharedPreferences("media-diagnostic",0).edit().putLong(pkg+".seen",System.currentTimeMillis()).putBoolean(pkg+".title",title!=null&&!title.trim().isEmpty()).apply();
   String artist=LauncherinMusic.text(metadata,MediaMetadata.METADATA_KEY_ARTIST,MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
   if(audio&&notification!=null){if(title==null||title.trim().isEmpty()){CharSequence value=notification.extras.getCharSequence(Notification.EXTRA_TITLE);if(value!=null)title=value.toString();}if(artist.isEmpty()){CharSequence value=notification.extras.getCharSequence(Notification.EXTRA_TEXT);if(value!=null)artist=value.toString();}}
   if(title==null||title.trim().isEmpty())return;
   if(metadata==null)metadata=new MediaMetadata.Builder().build();
   String key=pkg+"|"+title;JSONArray old=history(this);String previous="";for(int i=0;i<old.length();i++)if(key.equals(old.getJSONObject(i).optString("key")))previous=old.getJSONObject(i).optString("art");
   if(audio){JSONObject last=lastMusic(this);if(key.equals(last.optString("key"))){previous=last.optString("art");if(artist.isEmpty())artist=last.optString("artist");}}
   String art=audio?LauncherinMediaArt.captureMusic(this,key,metadata,previous):LauncherinMediaArt.capture(this,key,metadata,previous);long position=state==null?-1:state.getPosition(),duration=metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
   JSONObject item=new JSONObject().put("key",key).put("pkg",pkg).put("title",title.trim()).put("provider",provider(pkg)).put("art",art).put("position",position).put("duration",duration).put("updated",System.currentTimeMillis()).put("capturedElapsed",SystemClock.elapsedRealtime()).put("playing",state!=null&&state.getState()==PlaybackState.STATE_PLAYING).put("media_id",metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID));
   if(audio){item.put("artist",artist).put("music",true);getSharedPreferences("launcherin-media",0).edit().putString("music",item.toString()).apply();}
   else updateVideo(this,item,newPlay);
   sendBroadcast(new Intent(CHANGED).setPackage(getPackageName()));
  }catch(Exception ignored){}});}catch(RejectedExecutionException ignored){}
 }
 static synchronized JSONObject removed(Context context){try{return new JSONObject(context.getSharedPreferences("launcherin-dismissed",0).getString("items","{}"));}catch(Exception ignored){return new JSONObject();}}
 private static void saveRemoved(Context context,JSONObject items)throws IOException{if(!context.getSharedPreferences("launcherin-dismissed",0).edit().putString("items",items.toString()).commit())throw new IOException("Não foi possível salvar a remoção");}
 static synchronized JSONArray history(Context context){try(InputStream in=new FileInputStream(new File(context.getFilesDir(),"launcherin-watch.json"))){return LauncherinHistory.visible(new JSONArray(new String(LauncherinExtras.bytes(in,1000000),StandardCharsets.UTF_8)),removed(context));}catch(Exception ignored){return new JSONArray();}}
 static synchronized void dismiss(Context context,JSONObject item)throws Exception{JSONObject hidden=removed(context);LauncherinHistory.dismiss(hidden,item,System.currentTimeMillis());saveRemoved(context,hidden);save(context,LauncherinHistory.visible(history(context),hidden));}
 static synchronized void save(Context context,JSONArray rows)throws Exception{File file=new File(context.getFilesDir(),"launcherin-watch.json"),tmp=new File(context.getFilesDir(),"launcherin-watch.part");try(OutputStream out=new FileOutputStream(tmp)){out.write(rows.toString().getBytes(StandardCharsets.UTF_8));}if(!tmp.renameTo(file))tmp.delete();}
 static synchronized void updateVideo(Context context,JSONObject item)throws Exception{updateVideo(context,item,false);}
 static synchronized void updateVideo(Context context,JSONObject item,boolean newPlay)throws Exception{JSONObject hidden=removed(context);JSONArray next=LauncherinHistory.playback(history(context),item,System.currentTimeMillis(),hidden,newPlay);saveRemoved(context,hidden);save(context,next);}
 static synchronized void importPrograms(Context context,JSONArray programs)throws Exception{JSONObject hidden=removed(context);JSONArray next=LauncherinHistory.programs(history(context),programs,hidden);saveRemoved(context,hidden);save(context,next);}
 static JSONObject lastMusic(Context context){try{return new JSONObject(context.getSharedPreferences("launcherin-media",0).getString("music","{}"));}catch(JSONException ignored){return new JSONObject();}}
 static void open(Context context,JSONObject item){
  LauncherinPlayback.Route route=LauncherinPlayback.open(context,item);
  context.getSharedPreferences("media-diagnostic",0).edit().putString("open.provider",provider(item.optString("pkg"))).putString("open.route",route.name()).apply();
  if(route==LauncherinPlayback.Route.UNAVAILABLE)android.widget.Toast.makeText(context,"Não foi possível abrir este aplicativo.",android.widget.Toast.LENGTH_LONG).show();
  else if(route==LauncherinPlayback.Route.APP&&!music(item.optString("pkg"))&&!item.optBoolean("music"))android.widget.Toast.makeText(context,"Abrindo o app. A retomada direta deste item não está disponível.",android.widget.Toast.LENGTH_LONG).show();
 }
 private static int musicCommandGeneration;
 static void cancelMusicCommand(){musicCommandGeneration++;LauncherinMusicResume.cancel();}
 private static MediaController freshMusicController(Context context){
  try{
   MediaSessionManager manager=context.getSystemService(MediaSessionManager.class);
   if(manager!=null&&granted(context)){
    List<MediaController> live=manager.getActiveSessions(new ComponentName(context,LauncherinMediaService.class));
    LauncherinMediaService service=current;
    if(service!=null){service.sessions(live);MediaController selected=service.selectedMusic();if(selected!=null)return selected;}
    MediaController paused=null;String last=lastMusic(context).optString("pkg");
    for(MediaController controller:live)if(LauncherinMusic.matchesSaved(controller.getPackageName(),controller.getMetadata(),lastMusic(context))){
     PlaybackState state=controller.getPlaybackState();if(state!=null&&LauncherinMusicCommand.started(state.getState()))return controller;
     if(paused==null||last.equals(controller.getPackageName()))paused=controller;
    }return paused;
   }
  }catch(RuntimeException ignored){}return null;
 }
 private static void mediaKey(MediaController controller,int key){
  long now=SystemClock.uptimeMillis();
  controller.dispatchMediaButtonEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,key,0));
  controller.dispatchMediaButtonEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,key,0));
 }
 private static boolean commandCompleted(MediaController controller,boolean pause){
  PlaybackState state=controller.getPlaybackState();return pause?(state==null||!LauncherinMusicCommand.started(state.getState()))
          :state!=null&&state.getState()==PlaybackState.STATE_PLAYING;
 }
 static void toggleMusic(Context context){MediaController active=freshMusicController(context);PlaybackState state=active==null?null:active.getPlaybackState();setMusicPlaying(context,state==null||!LauncherinMusicCommand.started(state.getState()));}
 static void setMusicPlaying(Context context,boolean wantPlay){
  final int generation=++musicCommandGeneration;
  final MediaController controller=freshMusicController(context);
  if(controller==null){if(wantPlay)LauncherinMusicResume.start(context,lastMusic(context));return;}
  PlaybackState state=controller.getPlaybackState();int status=state==null?PlaybackState.STATE_NONE:state.getState();
  if(wantPlay&&status==PlaybackState.STATE_PLAYING){refreshSessions(context);return;}
  final boolean pause=!wantPlay;
  JSONObject saved=lastMusic(context);
  String id=LauncherinMusic.text(controller.getMetadata(),MediaMetadata.METADATA_KEY_MEDIA_ID);
  if(id.isEmpty()&&controller.getPackageName().equals(saved.optString("pkg")))id=saved.optString("media_id");
  final String mediaId=id;
  final int command=LauncherinMusicCommand.chooseRequested(wantPlay,status,state==null?0:state.getActions(),!id.isEmpty());
  LauncherinMusicActions.record(context,controller.getPackageName(),"session",wantPlay?"Play solicitado":"Pausa solicitada");
  try{
   if(command==LauncherinMusicCommand.PAUSE)controller.getTransportControls().pause();
   else if(command==LauncherinMusicCommand.PLAY)controller.getTransportControls().play();
   else if(command==LauncherinMusicCommand.PLAY_ID)controller.getTransportControls().playFromMediaId(id,null);
   else mediaKey(controller,LauncherinMusicCommand.key(command));
  }catch(RuntimeException unavailable){if(!pause)LauncherinMusicResume.start(context,lastMusic(context));return;}
  Handler handler=new Handler(Looper.getMainLooper());
  handler.postDelayed(()->{
   if(generation!=musicCommandGeneration)return;
   try{
    MediaController selected=musicController();
    if(selected!=null&&!selected.getSessionToken().equals(controller.getSessionToken()))return;
    String currentId=LauncherinMusic.text(controller.getMetadata(),MediaMetadata.METADATA_KEY_MEDIA_ID);
    if(!mediaId.isEmpty()&&!currentId.isEmpty()&&!mediaId.equals(currentId))return;
    if(!commandCompleted(controller,pause))mediaKey(controller,pause?KeyEvent.KEYCODE_MEDIA_PAUSE:KeyEvent.KEYCODE_MEDIA_PLAY);
   }catch(RuntimeException ignored){}
  },650);
  // Recover through background media APIs only. A Play click never opens a music activity.
  handler.postDelayed(()->{
   if(generation!=musicCommandGeneration||!(context instanceof Activity))return;
   Activity activity=(Activity)context;if(activity.isFinishing()||activity.isDestroyed()||!activity.hasWindowFocus())return;
   try{
    MediaController selected=musicController();
    if(selected!=null&&!selected.getSessionToken().equals(controller.getSessionToken()))return;
    String currentId=LauncherinMusic.text(controller.getMetadata(),MediaMetadata.METADATA_KEY_MEDIA_ID);
    if(!mediaId.isEmpty()&&!currentId.isEmpty()&&!mediaId.equals(currentId))return;
    if(!commandCompleted(controller,pause)){
     if(pause)android.widget.Toast.makeText(context,"O app não respondeu ao comando de pausa.",android.widget.Toast.LENGTH_LONG).show();
     else LauncherinMusicResume.start(context,lastMusic(context));
    }
   }catch(RuntimeException ignored){}
  },2500);
 }
 static boolean playbackInProgress(Context context){
  try{android.media.session.MediaSessionManager manager=context.getSystemService(android.media.session.MediaSessionManager.class);
   if(manager!=null&&granted(context))for(MediaController controller:manager.getActiveSessions(new ComponentName(context,LauncherinMediaService.class))){
    PlaybackState state=controller.getPlaybackState();if(state!=null&&LauncherinMusicCommand.started(state.getState()))return true;
   }
  }catch(RuntimeException ignored){}return false;
 }
 /** Public audio activity is available even when notification/session access has disconnected. */
 static Boolean musicInProgress(Context context){
  Boolean sessions=null;
  if(granted(context))try{MediaSessionManager manager=context.getSystemService(MediaSessionManager.class);
   if(manager!=null){sessions=false;JSONObject saved=lastMusic(context);
    for(MediaController controller:manager.getActiveSessions(new ComponentName(context,LauncherinMediaService.class))){
     PlaybackState state=controller.getPlaybackState();
     if(state!=null&&LauncherinMusicCommand.started(state.getState())
        &&LauncherinMusic.matchesSaved(controller.getPackageName(),controller.getMetadata(),saved))return true;
    }
   }
  }catch(RuntimeException unavailable){sessions=null;}
  try{android.media.AudioManager audio=context.getSystemService(android.media.AudioManager.class);
   if(audio!=null)return audio.isMusicActive();
  }catch(RuntimeException unavailable){}
  // Unknown is still protected if neither public signal can verify playback.
  return sessions;
 }
 static boolean musicPlaying(){MediaController controller=musicController();PlaybackState state=controller==null?null:controller.getPlaybackState();return state!=null&&state.getState()==PlaybackState.STATE_PLAYING;}

 static void skip(Context context,boolean next){cancelMusicCommand();MediaController controller=musicController();if(controller!=null){if(next)controller.getTransportControls().skipToNext();else controller.getTransportControls().skipToPrevious();}}
 static void seek(Context context,long position){MediaController controller=musicController();if(controller!=null)controller.getTransportControls().seekTo(position);}
 @Override public void onListenerDisconnected(){LauncherinMusicActions.clear();if(goals!=null){goals.stop();goals=null;}current=null;if(manager!=null)manager.removeOnActiveSessionsChangedListener(listener);for(MediaSession.Token token:new ArrayList<>(controllers.keySet()))controllers.get(token).unregisterCallback(callbacks.get(token));controllers.clear();activityTimes.clear();playbackStates.clear();callbacks.clear();}
 @Override public void onDestroy(){onListenerDisconnected();main.removeCallbacksAndMessages(null);storage.shutdown();super.onDestroy();}
}
