package app.launcherin.tv;
import android.media.session.PlaybackState;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
public class LauncherinPlaybackControlsTest {
 @Test public void playPauseOnlyPlayersReceiveMediaToggle(){
  assertEquals(LauncherinMusicCommand.TOGGLE_KEY,LauncherinMusicCommand.choose(PlaybackState.STATE_PAUSED,PlaybackState.ACTION_PLAY_PAUSE,false));
  assertEquals(LauncherinMusicCommand.TOGGLE_KEY,LauncherinMusicCommand.choose(PlaybackState.STATE_PLAYING,PlaybackState.ACTION_PLAY_PAUSE,false));
 }
 @Test public void normalSessionsKeepTheirTransportControls(){
  assertEquals(LauncherinMusicCommand.PLAY,LauncherinMusicCommand.choose(PlaybackState.STATE_PAUSED,PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE,true));
  assertEquals(LauncherinMusicCommand.PAUSE,LauncherinMusicCommand.choose(PlaybackState.STATE_PLAYING,PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE,true));
 }
 @Test public void stoppedQueuesCanResumeByTheirPublishedMediaId(){
  assertEquals(LauncherinMusicCommand.PLAY_ID,LauncherinMusicCommand.choose(PlaybackState.STATE_STOPPED,PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PLAY_FROM_MEDIA_ID,true));
  assertEquals(LauncherinMusicCommand.PLAY,LauncherinMusicCommand.choose(PlaybackState.STATE_STOPPED,PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PLAY_FROM_MEDIA_ID,false));
 }
 @Test public void lateFallbackUsesExplicitPlayAndPauseInsteadOfTogglingAgain(){
  assertEquals(126,LauncherinMusicCommand.key(LauncherinMusicCommand.PLAY));
  assertEquals(127,LauncherinMusicCommand.key(LauncherinMusicCommand.PAUSE));
  assertEquals(85,LauncherinMusicCommand.key(LauncherinMusicCommand.TOGGLE_KEY));
 }
 @Test public void bufferingCountsAsPlaybackAlreadyStarting(){
  assertTrue(LauncherinMusicCommand.started(PlaybackState.STATE_BUFFERING));
  assertTrue(LauncherinMusicCommand.started(PlaybackState.STATE_CONNECTING));
  assertFalse(LauncherinMusicCommand.started(PlaybackState.STATE_PAUSED));
 }
 @Test public void tunerChoiceMatchesTheInputsMenuHardwarePreference(){
  assertEquals("native/Input/HW1",LauncherinTv.pickTuner(Arrays.asList("system/AirPlay","native/Input/HW1"),""));
 }
 @Test public void tunerChoicePrefersDigitalAndHonoursAnExactProfile(){
  java.util.List<String> ids=Arrays.asList("native/AnalogInputService","native/TunerInputService");
  assertEquals("native/TunerInputService",LauncherinTv.pickTuner(ids,""));
  assertEquals("native/AnalogInputService",LauncherinTv.pickTuner(ids,"native/AnalogInputService"));
 }
 @Test public void missingConfiguredTunerNeverSilentlySelectsAnotherSource(){
  assertNull(LauncherinTv.pickTuner(Arrays.asList("native/Input/HW0"),"native/Input/HW9"));
  assertEquals("native/Input/HW0",LauncherinTv.pickTuner(Arrays.asList("native/Input/HW0"),LauncherinTv.ROOT_CHANNEL_URI));
 }
}
