package app.launcherin.tv;

import android.media.session.PlaybackState;
import android.view.KeyEvent;

/** Uses only controls advertised by the publisher, with a media-key path for TV players. */
final class LauncherinMusicCommand {
    static final int PLAY=1,PAUSE=2,PLAY_ID=3,TOGGLE_KEY=4,PLAY_KEY=5,PAUSE_KEY=6;
    private LauncherinMusicCommand() {}
    static boolean started(int state) {
        return state==PlaybackState.STATE_PLAYING||state==PlaybackState.STATE_BUFFERING
                ||state==PlaybackState.STATE_CONNECTING||state==PlaybackState.STATE_FAST_FORWARDING
                ||state==PlaybackState.STATE_REWINDING;
    }
    static int choose(int state,long actions,boolean mediaId) {
        boolean pause=started(state);
        if(pause&&(actions&PlaybackState.ACTION_PAUSE)!=0)return PAUSE;
        if(!pause&&(state==PlaybackState.STATE_STOPPED||state==PlaybackState.STATE_NONE
                ||state==PlaybackState.STATE_ERROR)&&mediaId
                &&(actions&PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)!=0)return PLAY_ID;
        if(!pause&&(actions&PlaybackState.ACTION_PLAY)!=0)return PLAY;
        if((actions&PlaybackState.ACTION_PLAY_PAUSE)!=0)return TOGGLE_KEY;
        if(!pause&&mediaId&&(actions&PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)!=0)return PLAY_ID;
        return pause?PAUSE_KEY:PLAY_KEY;
    }
    static int chooseRequested(boolean play,int state,long actions,boolean mediaId) {
        if(!play)return (actions&PlaybackState.ACTION_PAUSE)!=0?PAUSE:(actions&PlaybackState.ACTION_PLAY_PAUSE)!=0?TOGGLE_KEY:PAUSE_KEY;
        // A visible Play control must never turn into Pause because the publisher reports BUFFERING.
        if((state==PlaybackState.STATE_STOPPED||state==PlaybackState.STATE_NONE||state==PlaybackState.STATE_ERROR)
                &&mediaId&&(actions&PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)!=0)return PLAY_ID;
        if((actions&PlaybackState.ACTION_PLAY)!=0)return PLAY;
        if((actions&PlaybackState.ACTION_PLAY_PAUSE)!=0)return started(state)?PLAY_KEY:TOGGLE_KEY;
        if(mediaId&&(actions&PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)!=0)return PLAY_ID;
        return PLAY_KEY;
    }
    static int key(int command) {
        return command==TOGGLE_KEY?KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                :command==PAUSE_KEY||command==PAUSE?KeyEvent.KEYCODE_MEDIA_PAUSE:KeyEvent.KEYCODE_MEDIA_PLAY;
    }
}
