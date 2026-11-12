package app.launcherin.tv;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.session.*;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.view.KeyEvent;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.*;

/** Creator-owned notification controls can resume a non-exported player after its session ends. */
final class LauncherinMusicActions {
    private static final Map<String,Control> controls=new LinkedHashMap<>();
    private static final class Control {
        final PendingIntent play,pause; final long time;
        Control(PendingIntent play,PendingIntent pause){this.play=play;this.pause=pause;time=SystemClock.elapsedRealtime();}
    }
    static int kind(String label,String icon) {
        String title=java.text.Normalizer.normalize(label==null?"":label,java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","").trim().toLowerCase(Locale.ROOT);
        if(title.matches("play|resume|reproduzir|retomar|tocar|reproducir|continuar reproduccion"))return 1;
        if(title.matches("pause|pausar|pausa"))return 2;
        if(!title.isEmpty())return 0; // Never interpret a labelled Delete/Next/Favourite action by its icon.
        String name=icon==null?"":icon.toLowerCase(Locale.ROOT);
        if(name.matches(".*(^|_)(pause|pausa)($|_).*"))return 2;
        if(name.matches(".*(^|_)play($|_).*"))return 1;
        return 0;
    }
    static synchronized void capture(Context context,StatusBarNotification posted,MediaController controller) {
        if(android.os.Build.VERSION.SDK_INT<31)return;
        String pkg=posted.getPackageName();Notification notification=posted.getNotification();
        if(!pkg.equals(controller.getPackageName())||notification.actions==null
                ||!LauncherinMusic.matchesSaved(pkg,controller.getMetadata(),LauncherinMediaService.lastMusic(context)))return;
        PendingIntent play=null,pause=null;
        for(Notification.Action action:notification.actions){
            PendingIntent intent=action.actionIntent;
            if(intent==null||intent.isActivity()||!pkg.equals(intent.getCreatorPackage())||intent.getCreatorUid()!=posted.getUid())continue;
            String icon="";
            try{if(action.icon!=0)icon=context.getPackageManager().getResourcesForApplication(pkg).getResourceEntryName(action.icon);}catch(Exception ignored){}
            int kind=kind(action.title==null?"":action.title.toString(),icon);
            if(kind==1)play=intent;else if(kind==2)pause=intent;
        }
        Control previous=controls.get(pkg);
        // A playing notification normally publishes Pause only. Retain the most recent trusted Play.
        if(previous!=null&&SystemClock.elapsedRealtime()-previous.time<24*60*60*1000L){
            if(play==null)play=previous.play;if(pause==null)pause=previous.pause;
        }
        controls.put(pkg,new Control(play,pause));
        while(controls.size()>8)controls.remove(controls.keySet().iterator().next());
    }
    static synchronized boolean send(Context context,String pkg,boolean play) {
        if(!LauncherinMediaService.granted(context)||!LauncherinMusic.dedicated(pkg))return false;
        Control control=controls.get(pkg);if(control==null||SystemClock.elapsedRealtime()-control.time>24*60*60*1000L)return false;
        PendingIntent intent=play?control.play:control.pause;if(intent==null)return false;
        try{intent.send();record(context,pkg,"notification", "enviado; aguardando reprodução");return true;}
        catch(PendingIntent.CanceledException|RuntimeException unavailable){controls.remove(pkg);record(context,pkg,"notification","controle expirado");return false;}
    }
    static synchronized String available(String pkg){Control c=controls.get(pkg);return c==null?"nenhum":(c.play!=null?"Play ":"")+(c.pause!=null?"Pausa":"");}
    static synchronized void clear(){controls.clear();}
    static void record(Context c,String pkg,String route,String result){c.getSharedPreferences("launcherin-music-command",0).edit().putString("pkg",pkg).putString("route",route).putString("result",result).apply();}
    static void diagnostic(Activity host) {
        String pkg=LauncherinMediaService.lastMusic(host).optString("pkg");
        StringBuilder text=new StringBuilder("Acesso a mídias: "+LauncherinMediaService.granted(host)+"\nApp: "+pkg+"\nControles publicados: "+available(pkg)+"\n");
        try{MediaSessionManager manager=host.getSystemService(MediaSessionManager.class);int count=0;
            if(manager!=null&&LauncherinMediaService.granted(host))for(MediaController controller:manager.getActiveSessions(new ComponentName(host,LauncherinMediaService.class)))
                if(pkg.equals(controller.getPackageName())){PlaybackState state=controller.getPlaybackState();text.append("Sessão: ").append(state==null?"sem estado":state.getState()).append(" · ações: ").append(state==null?0:state.getActions()).append('\n');count++;}
            if(count==0)text.append("Sessão encerrada\n");
        }catch(RuntimeException denied){text.append("Sessão indisponível\n");}
        android.content.SharedPreferences saved=host.getSharedPreferences("launcherin-music-command",0);
        text.append("Receptor do Android: ").append(LauncherinSystemMedia.owner(host)).append("\n");
        text.append("Última rota: ").append(saved.getString("route","—")).append("\nResultado: ").append(saved.getString("result","—"));
        text.append("\n\n").append(LauncherinDreams.diagnostic(host));
        String result=text.toString();new LauncherinDialogs.Builder(host).setTitle("Diagnóstico · música e protetor").setMessage(result)
                .setPositiveButton("Copiar",(d,w)->{host.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Launcherin",result));Toast.makeText(host,"Diagnóstico copiado.",Toast.LENGTH_SHORT).show();})
                .setNegativeButton("Fechar",null).show();
    }
}
