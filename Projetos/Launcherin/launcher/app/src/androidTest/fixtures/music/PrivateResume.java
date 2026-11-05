package app.launcherin.fixture;
import android.content.*;
import android.media.session.PlaybackState;
public final class PrivateResume extends BroadcastReceiver {
 public void onReceive(Context context,Intent intent) {
  context.startForegroundService(new Intent(context,MusicService.class).putExtra("private_controls",true)
    .putExtra("state",PlaybackState.STATE_PLAYING).putExtra("artist","Notification resume test"));
 }
}
