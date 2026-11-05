package app.launcherin.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class LauncherinMusicIdlePolicyTest {
    @Test public void musicNeverExpiresEvenAfterSeveralStandbyPeriods() {
        for(long now:new long[]{0,600000,7200000,86400000}) {
            LauncherinMusicIdlePolicy.Decision d=LauncherinMusicIdlePolicy.decide(true,-1,true,now,10);
            assertTrue(d.hold);assertEquals(-1,d.pausedAt);assertEquals(10000,d.checkAfter);
        }
    }
    @Test public void pausingAfterLongPlaybackGetsTenFreshMinutes() {
        long paused=7200000;
        LauncherinMusicIdlePolicy.Decision first=LauncherinMusicIdlePolicy.decide(true,-1,false,paused,10);
        assertTrue(first.hold);assertEquals(paused,first.pausedAt);assertEquals(600000,first.checkAfter);
        assertTrue(LauncherinMusicIdlePolicy.decide(true,paused,false,paused+599999,10).hold);
        assertFalse(LauncherinMusicIdlePolicy.decide(true,paused,false,paused+600000,10).hold);
    }
    @Test public void repeatedPausedSamplesDoNotRestartTheDeadline() {
        LauncherinMusicIdlePolicy.Decision d=LauncherinMusicIdlePolicy.decide(true,1000,false,301000,10);
        assertEquals(1000,d.pausedAt);assertEquals(300000,d.checkAfter);
    }
    @Test public void resumingCancelsTheOldPauseDeadline() {
        LauncherinMusicIdlePolicy.Decision resumed=LauncherinMusicIdlePolicy.decide(true,1000,true,500000,10);
        assertTrue(resumed.hold);assertEquals(-1,resumed.pausedAt);
        LauncherinMusicIdlePolicy.Decision paused=LauncherinMusicIdlePolicy.decide(resumed.hold,resumed.pausedAt,false,700000,10);
        assertTrue(paused.hold);assertEquals(600000,paused.checkAfter);
    }
    @Test public void lostAccessCannotCutAnUnverifiedPlayerOff() {
        LauncherinMusicIdlePolicy.Decision unknown=LauncherinMusicIdlePolicy.decide(true,0,null,900000,10);
        assertTrue(unknown.hold);assertEquals(-1,unknown.pausedAt);
        LauncherinMusicIdlePolicy.Decision fresh=LauncherinMusicIdlePolicy.decide(unknown.hold,unknown.pausedAt,false,901000,10);
        assertEquals(600000,fresh.checkAfter);
    }
    @Test public void unavailableInitialSnapshotFailsSafely() {
        assertTrue(LauncherinMusicIdlePolicy.decide(false,-1,null,0,10).hold);
    }
    @Test public void ordinaryIdleDoesNotCreateAnExtraDelay() {
        LauncherinMusicIdlePolicy.Decision d=LauncherinMusicIdlePolicy.decide(false,-1,false,10000000,10);
        assertFalse(d.hold);assertEquals(-1,d.checkAfter);
    }
    @Test public void changingTheTimeoutUsesTheSamePauseMoment() {
        assertFalse(LauncherinMusicIdlePolicy.decide(true,1000,false,361000,5).hold);
        assertEquals(240000,LauncherinMusicIdlePolicy.decide(true,1000,false,361000,10).checkAfter);
    }
    @Test public void disabledStandbyAndRebootAreSafe() {
        assertFalse(LauncherinMusicIdlePolicy.decide(true,-1,true,900000,0).hold);
        assertEquals(600000,LauncherinMusicIdlePolicy.decide(true,900000,false,1000,10).checkAfter);
    }
    @Test public void android14CannotUseTheDeprecatedDreamShortcut() {
        assertTrue(LauncherinMusicIdlePolicy.legacyDreamShortcut(28));
        assertTrue(LauncherinMusicIdlePolicy.legacyDreamShortcut(33));
        assertFalse(LauncherinMusicIdlePolicy.legacyDreamShortcut(34));
        assertFalse(LauncherinMusicIdlePolicy.legacyDreamShortcut(36));
    }
}
