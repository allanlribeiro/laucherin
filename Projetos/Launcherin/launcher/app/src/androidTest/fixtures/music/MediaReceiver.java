package app.launcherin.fixture;
public final class MediaReceiver extends android.content.BroadcastReceiver {
 public void onReceive(android.content.Context context,android.content.Intent intent){android.view.KeyEvent event=intent.getParcelableExtra(android.content.Intent.EXTRA_KEY_EVENT);if(event!=null&&event.getAction()==android.view.KeyEvent.ACTION_DOWN&&event.getKeyCode()==android.view.KeyEvent.KEYCODE_MEDIA_PLAY)context.startForegroundService(new android.content.Intent(context,MusicService.class).putExtra("state",android.media.session.PlaybackState.STATE_PLAYING).putExtra("artist","Media receiver test"));}
}
