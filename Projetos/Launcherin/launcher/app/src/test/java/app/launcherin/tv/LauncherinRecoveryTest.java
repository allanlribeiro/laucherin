package app.launcherin.tv;
import android.media.session.PlaybackState;
import org.junit.Test;
import static org.junit.Assert.*;
public class LauncherinRecoveryTest {
 @Test public void playCannotBecomePauseWhileThePlayerConnects() {
  long actions=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE;
  for(int state:new int[]{PlaybackState.STATE_BUFFERING,PlaybackState.STATE_CONNECTING,PlaybackState.STATE_PAUSED})
   assertEquals(LauncherinMusicCommand.PLAY,LauncherinMusicCommand.chooseRequested(true,state,actions,false));
 }
 @Test public void platformResumeIsAllowedOnlyForTheRequestedDedicatedMusicApp() {
  assertTrue(LauncherinSystemMedia.matches("com.spotify.tv.android","com.spotify.tv.android"));
  assertFalse(LauncherinSystemMedia.matches("com.spotify.tv.android","com.google.android.youtube.tv.music"));
  assertFalse(LauncherinSystemMedia.matches("com.google.android.youtube.tv","com.google.android.youtube.tv"));
  assertFalse(LauncherinSystemMedia.matches("com.spotify.tv.android",""));
 }
 @Test public void onlyExplicitPlaybackActionsAreAccepted() {
  assertEquals(1,LauncherinMusicActions.kind("Reproduzir",""));assertEquals(2,LauncherinMusicActions.kind("Pausar",""));
  assertEquals(1,LauncherinMusicActions.kind("Resume",""));
  for(String label:new String[]{"Next","Play next","Delete","Favorito","Stop"})assertEquals(0,LauncherinMusicActions.kind(label,"ic_play"));
 }
}
