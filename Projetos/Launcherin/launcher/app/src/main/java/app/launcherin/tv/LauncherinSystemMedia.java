package app.launcherin.tv;

import android.content.Context;
import android.media.AudioManager;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.SystemClock;
import android.view.KeyEvent;

/** API 33+: the OS can retain a player's private media receiver after its last session disappears. */
final class LauncherinSystemMedia {
    static boolean matches(String wanted,String systemOwner) {
        // Main YouTube also owns video queues: package-only routing cannot identify a saved song.
        return wanted!=null && LauncherinMusic.dedicated(wanted) && wanted.equals(systemOwner);
    }
    static String owner(Context context) {
        if(Build.VERSION.SDK_INT<33||!LauncherinMediaService.granted(context))return "";
        try{MediaSessionManager manager=context.getSystemService(MediaSessionManager.class);
            return manager==null?"":manager.getMediaKeyEventSessionPackageName();
        }catch(RuntimeException unavailable){return "";}
    }
    static boolean play(Context context,String pkg) {
        if(!matches(pkg,owner(context)))return false;
        AudioManager audio=context.getSystemService(AudioManager.class);if(audio==null)return false;
        try{
            // Recheck immediately before dispatch. No global key when the OS points at another app.
            if(!matches(pkg,owner(context)))return false;
            long now=SystemClock.uptimeMillis();
            audio.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY,0));
            audio.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PLAY,0));
            LauncherinMusicActions.record(context,pkg,"system_receiver","Play enviado ao último receptor do mesmo app");
            return true;
        }catch(RuntimeException unavailable){return false;}
    }
}
