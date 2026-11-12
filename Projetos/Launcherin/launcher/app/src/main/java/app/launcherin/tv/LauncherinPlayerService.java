// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import android.os.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Own audio engine, started only by a user action. Never acquires a screen wake lock. */
public final class LauncherinPlayerService extends Service {
    static final String CHANGED="app.launcherin.tv.PLAYER_CHANGED";
    private static final String ACTION="player_action", CHANNEL="launcherin-music";
    private static final int NOTICE=7301;
    private static volatile LauncherinPlayerService current;
    interface Resolver {String resolve(LauncherinTrack track)throws Exception;}
    Resolver resolver=LauncherinMusicSource::resolve;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();private Future<?> resolving;
    private LauncherinMusicTaste taste;private final LauncherinListeningSession listening=new LauncherinListeningSession();
    private LauncherinMusicLibrary library;private List<LauncherinTrack> queue=new ArrayList<>();
    private int index=-1,state=PlaybackState.STATE_NONE,generation;private long savedPosition;
    private MediaPlayer player;private MediaSession session;private AudioManager audio;private AudioFocusRequest focus;
    private String historyKey="";private final LauncherinShuffleOrder shuffleOrder=new LauncherinShuffleOrder();private final java.util.Random random=new java.util.Random();
    private boolean prepared,closed,foreground,resumeAfterFocus;private String problem="";
    private final Runnable timeout=()->fail("O áudio demorou para iniciar. Tente outra música.");
    private final Runnable checkpoint=new Runnable(){public void run(){if(closed||!playing())return;sampleListening();save();main.postDelayed(this,30000);}};

    static void open(Context c){c.startActivity(new Intent(c,LauncherinPlayerActivity.class));}
    static boolean selected(Context c){return new LauncherinMusicLibrary(c).nativeSelected();}
    static boolean active(){LauncherinPlayerService s=current;return s!=null&&(s.playing()||s.state==PlaybackState.STATE_BUFFERING);}
    static boolean playing(){LauncherinPlayerService s=current;return s!=null&&s.state==PlaybackState.STATE_PLAYING;}
    /** Read-only visual effect attachment. Never exposes the global audio mix (session zero). */
    static int audioSessionId(){LauncherinPlayerService s=current;if(s==null||s.player==null||!s.prepared)return -1;try{int id=s.player.getAudioSessionId();return id>0?id:-1;}catch(RuntimeException e){return -1;}}
    static void switchProfile(Context c,String id){LauncherinPlayerService s=current;if(s!=null){s.pause(false);s.releasePlayer();}new LauncherinMusicProfiles(c).select(id);if(s!=null){s.library=new LauncherinMusicLibrary(c);s.taste=new LauncherinMusicTaste(c);s.queue=s.library.read("queue");s.index=s.library.index(s.queue);s.savedPosition=s.library.position();s.problem="";s.historyKey="";s.shuffleOrder.reset();s.publish();}else c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));}
    static void resetTaste(Context c){new LauncherinMusicTaste(c).reset();if(current!=null){current.listening.clear();current.sampleListening();}}
    static void shuffle(Context c){LauncherinMusicLibrary l=new LauncherinMusicLibrary(c);l.shuffle(!l.shuffle());if(current!=null)current.shuffleOrder.reset();c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));}
    static void repeat(Context c){LauncherinMusicLibrary l=new LauncherinMusicLibrary(c);l.repeat(l.repeat()+1);c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));}
    static void command(Context c,String action){try{
        c.startForegroundService(new Intent(c,LauncherinPlayerService.class).putExtra(ACTION,action));
    }catch(RuntimeException e){android.widget.Toast.makeText(c,"Abra o player da Launcherin para iniciar a música.",android.widget.Toast.LENGTH_LONG).show();}}
    static void play(Context c,List<LauncherinTrack> list,int index){LauncherinMusicLibrary l=new LauncherinMusicLibrary(c);l.write("pending",list);try{c.startForegroundService(new Intent(c,LauncherinPlayerService.class).putExtra(ACTION,"replace").putExtra("index",index));}catch(RuntimeException e){android.widget.Toast.makeText(c,"Não foi possível iniciar o player.",android.widget.Toast.LENGTH_SHORT).show();}}
    static JSONObject snapshot(Context c){
        LauncherinPlayerService s=current;LauncherinMusicLibrary l=new LauncherinMusicLibrary(c);List<LauncherinTrack> q=s!=null?s.queue:l.read("queue");int i=s!=null?s.index:l.index(q);JSONObject j=new JSONObject();
        try{if(i>=0&&i<q.size()){LauncherinTrack t=q.get(i);j=t.json();j.put("pkg",c.getPackageName()).put("native",true).put("music",true).put("media_id",t.url).put("position",s!=null?s.position():l.position()).put("capturedElapsed",SystemClock.elapsedRealtime()).put("playing",playing()).put("buffering",s!=null&&s.state==PlaybackState.STATE_BUFFERING).put("error",s==null?l.error():s.problem);}}catch(JSONException ignored){}return j;
    }
    static List<LauncherinTrack> tracks(Context c){LauncherinPlayerService s=current;return s==null?new LauncherinMusicLibrary(c).read("queue"):new ArrayList<>(s.queue);}
    static void remove(Context c,int at){List<LauncherinTrack> q=tracks(c);if(at<0||at>=q.size())return;LauncherinMusicLibrary l=new LauncherinMusicLibrary(c);LauncherinPlayerService s=current;int index=s==null?l.index(q):s.index;if(at==index)return;q.remove(at);if(at<index)index--;if(s!=null){s.queue=q;s.index=index;s.shuffleOrder.reset();s.save();s.publish();}else{l.write("queue",q);l.progress(index,l.position());}}
    static void enqueue(Context c,LauncherinTrack t){List<LauncherinTrack> q=tracks(c);if(q.size()>=LauncherinMusicLibrary.MAX){android.widget.Toast.makeText(c,"A fila aceita até 100 músicas.",android.widget.Toast.LENGTH_SHORT).show();return;}q.add(t);LauncherinPlayerService s=current;if(s!=null){s.queue=q;s.save();s.publish();}else new LauncherinMusicLibrary(c).write("queue",q);}
    static void seek(Context c,long position){LauncherinPlayerService s=current;if(s!=null)s.seek(position);else{LauncherinMusicLibrary l=new LauncherinMusicLibrary(c);List<LauncherinTrack> q=l.read("queue");int i=l.index(q);if(i>=0){l.progress(i,Math.max(0,Math.min(position,q.get(i).duration)));c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));}}}
    static void external(Context c){LauncherinPlayerService s=current;if(s!=null)s.pause(false);new LauncherinMusicLibrary(c).select(false);c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));}

    @Override public void onCreate(){super.onCreate();library=new LauncherinMusicLibrary(this);taste=new LauncherinMusicTaste(this);queue=library.read("queue");index=library.index(queue);savedPosition=library.position();audio=getSystemService(AudioManager.class);
        session=new MediaSession(this,"LauncherinPlayer");session.setCallback(new MediaSession.Callback(){
            @Override public void onPlay(){ensureForeground();playCurrent();}@Override public void onPause(){pause(false);}
            @Override public void onStop(){pause(false);stopSelf();}@Override public void onSkipToNext(){step(1);}@Override public void onSkipToPrevious(){step(-1);}@Override public void onSeekTo(long p){seek(p);}
            @Override public boolean onMediaButtonEvent(Intent intent){return super.onMediaButtonEvent(intent);}
        });session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Música da Launcherin",NotificationManager.IMPORTANCE_LOW));current=this;
        registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }
    private final BroadcastReceiver noisy=new BroadcastReceiver(){public void onReceive(Context c,Intent i){pause(false);}};
    @Override public int onStartCommand(Intent intent,int flags,int id){
        ensureForeground();String action=intent==null?"pause":intent.getStringExtra(ACTION);library.select(true);
        if("replace".equals(action)){save();List<LauncherinTrack> pending=library.read("pending");if(!pending.isEmpty()){finishListening(false);queue=pending;shuffleOrder.reset();index=Math.max(0,Math.min(queue.size()-1,intent.getIntExtra("index",0)));savedPosition=0;library.write("queue",queue);load();}else pause(false);}
        else if("play".equals(action))playCurrent();else if("toggle".equals(action)){if(active())pause(false);else playCurrent();}
        else if("next".equals(action))step(1);else if("previous".equals(action))step(-1);else if("stop".equals(action)){pause(false);stopSelf();}else pause(false);
        return START_NOT_STICKY;
    }
    private void ensureForeground(){if(!foreground){startForeground(NOTICE,notification());foreground=true;}}
    private void playCurrent(){
        if(index<0||index>=queue.size()){problem="Pesquise e escolha uma música para começar.";publish();stopForeground(STOP_FOREGROUND_REMOVE);foreground=false;stopSelf();return;}
        library.select(true);if(prepared&&player!=null){if(requestFocus()){try{ensureForeground();player.start();state=PlaybackState.STATE_PLAYING;problem="";changed();}catch(RuntimeException e){load();}}else fail("Outro aplicativo está usando o áudio.");}
        else load();
    }
    private boolean requestFocus(){
        if(focus==null)focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(value->{
            if(value==AudioManager.AUDIOFOCUS_GAIN){if(player!=null)player.setVolume(1,1);if(resumeAfterFocus){resumeAfterFocus=false;playCurrent();}}
            else if(value==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){if(player!=null)player.setVolume(.2f,.2f);}
            else if(value<0){boolean resume=value==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT&&active();pause(true);resumeAfterFocus=resume;if(value==AudioManager.AUDIOFOCUS_LOSS)library.select(false);}
        },main).build();return audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }
    private void load(){
        if(index<0||index>=queue.size())return;if(resolving!=null)resolving.cancel(true);ensureForeground();releasePlayer();problem="";library.error("");state=PlaybackState.STATE_BUFFERING;prepared=false;final int request=++generation;final LauncherinTrack track=queue.get(index);changed();
        main.removeCallbacks(timeout);main.postDelayed(timeout,45000);
        resolving=io.submit(()->{try{String url=resolver.resolve(track);main.post(()->{if(closed||request!=generation)return;try{
            if(!requestFocus()){fail("Outro aplicativo está usando o áudio.");return;}
            MediaPlayer p=new MediaPlayer();player=p;p.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());p.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            p.setOnPreparedListener(ready->{if(closed||request!=generation||player!=ready)return;main.removeCallbacks(timeout);prepared=true;try{if(savedPosition>0)ready.seekTo(Math.min(savedPosition,Math.max(0,ready.getDuration()-1000)),MediaPlayer.SEEK_CLOSEST);ready.start();library.select(true);state=PlaybackState.STATE_PLAYING;changed();}catch(RuntimeException e){fail("Não foi possível iniciar o áudio.");}});
            p.setOnCompletionListener(done->{if(player==done){finishListening(false);savedPosition=0;if(library.repeat()==1){seek(0);playCurrent();}else if(!step(1,false)){seek(0);pause(false);}}});
            p.setOnErrorListener((bad,what,extra)->{if(player==bad)fail("Não foi possível reproduzir esta faixa. Tente outra música.");return true;});
            p.setOnInfoListener((mp,what,extra)->{if(player==mp&&prepared){if(what==MediaPlayer.MEDIA_INFO_BUFFERING_START){state=PlaybackState.STATE_BUFFERING;main.removeCallbacks(timeout);main.postDelayed(timeout,45000);changed();}else if(what==MediaPlayer.MEDIA_INFO_BUFFERING_END){main.removeCallbacks(timeout);state=PlaybackState.STATE_PLAYING;changed();}}return false;});
            p.setDataSource(this,android.net.Uri.parse(url),Collections.singletonMap("User-Agent",LauncherinMusicSource.USER_AGENT));p.prepareAsync();
        }catch(Exception e){fail("O formato desta música não pôde ser reproduzido.");}});}catch(Exception e){main.post(()->{if(!closed&&request==generation)fail(LauncherinMusicSource.error(e));});}});
    }
    private boolean step(int direction){return step(direction,true);}
    private boolean step(int direction,boolean manual){if(queue.isEmpty()){pause(false);return false;}if(direction<0&&position()>3000){seek(0);if(!active())pause(false);return true;}int next;if(library.shuffle())next=direction>0?shuffleOrder.next(index,queue.size(),library.repeat()==2,random):shuffleOrder.previous(index,queue.size());else{next=index+direction;if(library.repeat()==2)next=Math.floorMod(next,queue.size());}if(next<0||next>=queue.size()){if(!active())pause(false);return false;}finishListening(manual&&direction>0);save();index=next;savedPosition=0;load();return true;}
    private void seek(long p){sampleListening();savedPosition=Math.max(0,p);if(prepared&&player!=null)try{savedPosition=Math.min(savedPosition,Math.max(0,player.getDuration()));player.seekTo(savedPosition,MediaPlayer.SEEK_CLOSEST);}catch(RuntimeException ignored){}listening.seek(savedPosition,SystemClock.elapsedRealtime());save();publish();}
    private long position(){if(prepared&&player!=null)try{return Math.max(0,player.getCurrentPosition());}catch(RuntimeException ignored){}return savedPosition;}
    private void save(){if(library==null)return;savedPosition=position();library.write("queue",queue);library.progress(index,savedPosition);}
    private void pause(boolean keepFocus){
        sampleListening();++generation;if(resolving!=null)resolving.cancel(true);main.removeCallbacks(timeout);main.removeCallbacks(checkpoint);save();if(player!=null&&prepared)try{player.pause();}catch(RuntimeException ignored){}else releasePlayer();
        state=PlaybackState.STATE_PAUSED;if(!keepFocus){resumeAfterFocus=false;if(focus!=null)audio.abandonAudioFocusRequest(focus);}changed();
        // A transient interruption retains the foreground lease, so audio-focus recovery does
        // not attempt a forbidden background foreground-service start on Android 12+.
        if(foreground&&!keepFocus){stopForeground(STOP_FOREGROUND_DETACH);foreground=false;}
        // Paused service is disposable: persisted queue allows resumption after Android reclaims it.
        if(!keepFocus)stopSelf();
    }
    private void fail(String message){problem=message;library.error(message);pause(false);state=PlaybackState.STATE_ERROR;publish();}
    private void changed(){sampleListening();if(state==PlaybackState.STATE_PLAYING&&index>=0&&index<queue.size()&&!queue.get(index).url.equals(historyKey)){historyKey=queue.get(index).url;library.recent(queue.get(index));}save();main.removeCallbacks(checkpoint);if(playing())main.postDelayed(checkpoint,30000);publish();LauncherinIdle.apply(this);}
    // Android exempts genuine MediaSession notifications from POST_NOTIFICATIONS.
    @android.annotation.SuppressLint("NotificationPermission")
    private void publish(){
        if(session==null)return;LauncherinTrack t=index>=0&&index<queue.size()?queue.get(index):null;
        if(t!=null)session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,t.title).putString(MediaMetadata.METADATA_KEY_ARTIST,t.artist).putString(MediaMetadata.METADATA_KEY_MEDIA_ID,t.url).putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI,t.art).putLong(MediaMetadata.METADATA_KEY_DURATION,t.duration).build());else session.setMetadata(null);
        PlaybackState.Builder b=new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_STOP|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_SEEK_TO).setState(state,position(),playing()?1:0,SystemClock.elapsedRealtime());if(!problem.isEmpty())b.setErrorMessage(problem);session.setPlaybackState(b.build());session.setActive(active());
        getSystemService(NotificationManager.class).notify(NOTICE,notification());sendBroadcast(new Intent(CHANGED).setPackage(getPackageName()));sendBroadcast(new Intent(LauncherinMediaService.CHANGED).setPackage(getPackageName()));
    }
    private PendingIntent action(String command,int code){return PendingIntent.getForegroundService(this,code,new Intent(this,LauncherinPlayerService.class).putExtra(ACTION,command),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private Notification notification(){LauncherinTrack t=index>=0&&index<queue.size()?queue.get(index):null;PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,LauncherinPlayerActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(t==null?"Música da Launcherin":t.title).setContentText(!problem.isEmpty()?problem:state==PlaybackState.STATE_BUFFERING?"Carregando áudio…":t==null?"Escolha uma música":t.artist).setContentIntent(open).setOnlyAlertOnce(true).setOngoing(active()).setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_previous,"Anterior",action("previous",1)).build()).addAction(new Notification.Action.Builder(playing()?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing()?"Pausar":"Reproduzir",action(playing()?"pause":"play",2)).build()).addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next,"Próxima",action("next",3)).build())
            .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2)).build();
    }
    private void sampleListening(){if(prepared&&player!=null&&index>=0&&index<queue.size())listening.sample(queue.get(index),state==PlaybackState.STATE_PLAYING,position(),SystemClock.elapsedRealtime());}
    private void finishListening(boolean skipped){sampleListening();if(listening.track!=null&&taste!=null)taste.listen(listening.track,listening.heard(),skipped);listening.clear();}
    private void releasePlayer(){finishListening(false);prepared=false;if(player!=null){player.release();player=null;}}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){save();closed=true;++generation;main.removeCallbacksAndMessages(null);releasePlayer();if(focus!=null)audio.abandonAudioFocusRequest(focus);unregisterReceiver(noisy);session.release();if(current==this)current=null;io.shutdownNow();getSystemService(NotificationManager.class).cancel(NOTICE);LauncherinIdle.apply(this);super.onDestroy();}
}
