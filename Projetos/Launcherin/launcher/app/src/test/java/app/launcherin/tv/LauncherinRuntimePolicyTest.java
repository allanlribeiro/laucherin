package app.launcherin.tv;
import android.view.KeyEvent;
import org.junit.Test;
import static org.junit.Assert.*;
public class LauncherinRuntimePolicyTest {
 @Test public void documentedTclGuideKeyUsesTvButInputAndOtherBrandsAreUntouched() {
  assertEquals(LauncherinTv.LIVE_TV,LauncherinTv.remoteAction(4001,true,new int[0]));
  assertEquals(LauncherinTv.LIVE_TV,LauncherinTv.remoteAction(172,true,new int[0]));
  assertEquals(LauncherinTv.NONE,LauncherinTv.remoteAction(4001,false,new int[0]));
  assertEquals(LauncherinTv.NONE,LauncherinTv.remoteAction(178,true,new int[0]));
 }
 @Test public void learningCannotHijackNavigationPowerVolumeInputOrMedia() {
  for(int key:new int[]{0,3,4,19,20,21,22,23,24,25,26,66,82,85,87,88,91,126,127,164,176,178,219,223,224,231,83,4020,4077})
   assertFalse("Key "+key,LauncherinRemote.learnable(key));
  assertTrue(LauncherinRemote.learnable(4001));assertTrue(LauncherinRemote.learnable(KeyEvent.KEYCODE_TV));
 }
 @Test public void appearanceOnlyConfigKeepsTimersButExplicitZeroDisables() throws Exception {
  org.json.JSONObject appearance=new org.json.JSONObject("{\"widget_transparency\":65,\"seasonal_theme\":\"christmas\"}");
  assertEquals(5,LauncherinRuntimePrefs.resolve(appearance,LauncherinIdle.SAVER,5));
  assertEquals(30,LauncherinRuntimePrefs.resolve(appearance,LauncherinIdle.SLEEP,30));
  assertEquals(1,LauncherinRuntimePrefs.resolve(appearance,LauncherinGoals.KEY,1));
  appearance.put(LauncherinIdle.SLEEP,0);
  assertEquals(0,LauncherinRuntimePrefs.resolve(new org.json.JSONObject(appearance.toString()),LauncherinIdle.SLEEP,30));
 }
 @Test public void standbyMustFollowSaverAndTimersHaveBoundedDurations() {
  assertTrue(LauncherinIdle.valid(5,30));assertTrue(LauncherinIdle.valid(0,30));assertTrue(LauncherinIdle.valid(5,0));
  assertFalse(LauncherinIdle.valid(30,5));assertFalse(LauncherinIdle.valid(5,5));
  assertEquals(0,LauncherinIdle.minutes(-1));assertEquals(1440,LauncherinIdle.minutes(Integer.MAX_VALUE));
 }
}
