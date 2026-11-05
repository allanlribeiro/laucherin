package app.launcherin.tv;

import android.content.*;
import android.content.pm.*;
import android.media.browse.MediaBrowser;
import android.media.session.*;
import android.os.*;
import android.view.KeyEvent;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.*;

/** Background-only recovery through the publisher's public browser or explicit media receiver. */
final class LauncherinMusicResume {
    private static Attempt current;
    private LauncherinMusicResume() {}
    static boolean busy(){return current!=null&&current.alive()&&!current.started;}
    static void cancel(){if(current!=null){current.close();current=null;}}
    static void start(Context context,JSONObject saved){
        cancel();String pkg=saved.optString("pkg");
        if(pkg.isEmpty()||(!LauncherinMusic.dedicated(pkg)&&!(saved.optBoolean("music")
                &&pkg.equals("com.google.android.youtube.tv")))){
            Toast.makeText(context,"Reproduza uma música no Spotify ou YouTube Music uma vez para poder retomá-la aqui.",Toast.LENGTH_LONG).show();return;
        }
        current=new Attempt(context.getApplicationContext(),pkg,saved.optString("media_id"));
        Toast.makeText(context,"Retomando a música pelo widget…",Toast.LENGTH_SHORT).show();current.begin();
    }
    private static boolean accessible(Context context,ComponentInfo info,String permission){
        return info.exported&&info.enabled&&(permission==null
                ||context.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED);
    }
    private static final class Attempt {
        final Context context;final String pkg,id;final Handler handler=new Handler(Looper.getMainLooper());
        final long since=SystemClock.elapsedRealtime();
        MediaBrowser browser;MediaController controller;boolean closed,started,receiverTried,controllerCommandSent;
        MediaController.Callback callback;
        Attempt(Context context,String pkg,String id){this.context=context;this.pkg=pkg;this.id=id;}
        boolean alive(){return !closed&&current==this;}
        void begin(){
            if(LauncherinSystemMedia.play(context,pkg))waitForReply(this::notification);
            else notification();
        }
        void notification(){
            if(!alive()||started)return;
            if(LauncherinMusicActions.send(context,pkg,true))waitForReply(this::browser);
            else browser();
        }
        void waitForReply(Runnable next){
            for(int delay:new int[]{350,1200,2500})handler.postDelayed(this::rescan,delay);
            handler.postDelayed(()->{if(alive()&&!started)next.run();},3500);
        }
        void browser(){
            if(!alive()||started)return;
            LauncherinMusicActions.record(context,pkg,"browser","procurando serviço de mídia");
            List<ResolveInfo> services=context.getPackageManager().queryIntentServices(
                    new Intent("android.media.browse.MediaBrowserService").setPackage(pkg),0);
            for(ResolveInfo resolved:services){ServiceInfo info=resolved.serviceInfo;
                if(info==null||!pkg.equals(info.packageName)||!accessible(context,info,info.permission))continue;
                try{
                    browser=new MediaBrowser(context,new ComponentName(info.packageName,info.name),new MediaBrowser.ConnectionCallback(){
                        @Override public void onConnected(){if(!alive())return;try{
                            MediaController connected=new MediaController(context,browser.getSessionToken());
                            if(!pkg.equals(connected.getPackageName())){receiver();return;}
                            controller=connected;watch();play();
                        }catch(RuntimeException error){receiver();}}
                        @Override public void onConnectionFailed(){receiver();}
                        @Override public void onConnectionSuspended(){if(!started)receiver();}
                    },null);
                    browser.connect();handler.postDelayed(()->{if(alive()&&!started)receiver();},4000);
                    return;
                }catch(RuntimeException ignored){if(browser!=null)browser.disconnect();browser=null;}
            }
            receiver();
        }
        boolean requestedTrack(){return !pkg.equals("com.google.android.youtube.tv")||!id.isEmpty()
                &&id.equals(LauncherinMusic.text(controller.getMetadata(),android.media.MediaMetadata.METADATA_KEY_MEDIA_ID));}
        boolean playingRequested(){PlaybackState state=controller==null?null:controller.getPlaybackState();return state!=null
                &&state.getState()==PlaybackState.STATE_PLAYING&&requestedTrack();}
        void watch(){
            if(controller==null)return;
            callback=new MediaController.Callback(){@Override public void onPlaybackStateChanged(PlaybackState state){
                if(alive()&&!started&&playingRequested())success();
            }};controller.registerCallback(callback,handler);
        }
        void play(){
            if(!alive()||controller==null)return;
            PlaybackState state=controller.getPlaybackState();
            if(playingRequested()){success();return;}
            try{
                long actions=state==null?0:state.getActions();
                if(pkg.equals("com.google.android.youtube.tv")&&!requestedTrack()&&state!=null
                        &&(actions&PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)==0){receiver();return;}
                if(!id.isEmpty()&&(state==null||(actions&PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)!=0))
                    controller.getTransportControls().playFromMediaId(id,null);
                else if((actions&PlaybackState.ACTION_PLAY)!=0)controller.getTransportControls().play();
                else sendKey((actions&PlaybackState.ACTION_PLAY_PAUSE)!=0?KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:KeyEvent.KEYCODE_MEDIA_PLAY);
                controllerCommandSent=true;
                handler.postDelayed(()->{if(alive()&&!started)try{PlaybackState now=controller.getPlaybackState();
                    if(playingRequested())success();else if(requestedTrack())sendKey(KeyEvent.KEYCODE_MEDIA_PLAY);
                }catch(RuntimeException ignored){}},650);
                handler.postDelayed(()->{if(alive()&&!started)receiver();},1800);
            }catch(RuntimeException ignored){receiver();}
        }
        void sendKey(int key){
            long time=SystemClock.uptimeMillis();
            controller.dispatchMediaButtonEvent(new KeyEvent(time,time,KeyEvent.ACTION_DOWN,key,0));
            controller.dispatchMediaButtonEvent(new KeyEvent(time,time,KeyEvent.ACTION_UP,key,0));
        }
        void receiver(){
            if(!alive()||started||receiverTried)return;
            PlaybackState loading=controller==null?null:controller.getPlaybackState();
            if(loading!=null&&(loading.getState()==PlaybackState.STATE_BUFFERING||loading.getState()==PlaybackState.STATE_CONNECTING)
                    &&SystemClock.elapsedRealtime()-since<12000){handler.postDelayed(this::receiver,1000);return;}
            receiverTried=true;
            List<ResolveInfo> receivers=context.getPackageManager().queryBroadcastReceivers(
                    new Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(pkg),0);
            for(ResolveInfo resolved:receivers){if(pkg.equals("com.google.android.youtube.tv"))break;ActivityInfo info=resolved.activityInfo;
                if(info==null||!pkg.equals(info.packageName)||!accessible(context,info,info.permission))continue;
                ComponentName target=new ComponentName(info.packageName,info.name);long time=SystemClock.uptimeMillis();
                try{
                    for(int action:new int[]{KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP})context.sendBroadcast(
                            new Intent(Intent.ACTION_MEDIA_BUTTON).setComponent(target)
                                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                    .putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(time,time,action,KeyEvent.KEYCODE_MEDIA_PLAY,0)));
                    break;
                }catch(RuntimeException ignored){}
            }
            for(int delay:new int[]{350,1000,2200})handler.postDelayed(this::rescan,delay);
            handler.postDelayed(()->{if(alive()&&!started){
                LauncherinMusicActions.record(context,pkg,"unavailable","nenhum controle publicado respondeu à retomada");
                Toast.makeText(context,"O app encerrou a sessão e não respondeu à retomada em segundo plano. Segure OK no widget para ver o diagnóstico.",Toast.LENGTH_LONG).show();close();
            }},3500);
        }
        void rescan(){
            if(!alive()||started)return;
            try{
                MediaSessionManager manager=context.getSystemService(MediaSessionManager.class);
                if(manager==null||!LauncherinMediaService.granted(context))return;
                for(MediaController live:manager.getActiveSessions(new ComponentName(context,LauncherinMediaService.class))){
                    if(!pkg.equals(live.getPackageName()))continue;
                    PlaybackState state=live.getPlaybackState();
                    if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING&&(!pkg.equals("com.google.android.youtube.tv")
                            ||!id.isEmpty()&&id.equals(LauncherinMusic.text(live.getMetadata(),android.media.MediaMetadata.METADATA_KEY_MEDIA_ID)))){success();return;}
                    if(controller==null||!controller.getSessionToken().equals(live.getSessionToken())){
                        if(controller!=null&&callback!=null)controller.unregisterCallback(callback);
                        controller=live;watch();controllerCommandSent=false;
                    }
                    if(!controllerCommandSent)play();
                    return;
                }
            }catch(RuntimeException ignored){}
        }
        void success(){LauncherinMusicActions.record(context,pkg,"playback","reprodução confirmada pelo app");started=true;handler.removeCallbacksAndMessages(null);LauncherinMediaService.refreshSessions(context);}
        void close(){closed=true;handler.removeCallbacksAndMessages(null);
            if(controller!=null&&callback!=null)controller.unregisterCallback(callback);
            if(browser!=null)try{browser.disconnect();}catch(RuntimeException ignored){}
        }
    }
}
