package app.launcherin.tv;

import android.app.Application;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Process;
import android.provider.Settings;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;
import static org.junit.Assert.*;

/** Real Android Settings/SharedPreferences implementations under Robolectric, no streaming mocks. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class LauncherinMusicIdleAndroidTest {
    private Context c;
    @Before public void prepare() throws Exception {
        c=RuntimeEnvironment.getApplication();
        Shadows.shadowOf((Application)c).grantPermissions("android.permission.WRITE_SECURE_SETTINGS","android.permission.WRITE_SETTINGS");
        Shadows.shadowOf(c.getSystemService(AppOpsManager.class)).setMode(AppOpsManager.OPSTR_WRITE_SETTINGS,Process.myUid(),c.getPackageName(),AppOpsManager.MODE_ALLOWED);
        assertTrue("Test timer permissions",LauncherinIdle.canApply(c));
        Settings.System.putString(c.getContentResolver(),"screen_off_timeout","600000");
        Settings.Secure.putString(c.getContentResolver(),"sleep_timeout","900000");
        Settings.Secure.putString(c.getContentResolver(),"attentive_timeout","-1");
        Settings.Secure.putString(c.getContentResolver(),"screensaver_enabled","1");
        Settings.Secure.putString(c.getContentResolver(),"screensaver_activate_on_sleep","1");
        Settings.Global.putInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,42);
        LauncherinExtras.setInt(c,LauncherinIdle.SAVER,5);
        LauncherinExtras.setInt(c,LauncherinIdle.SLEEP,10);
    }
    @After public void finish(){LauncherinIdle.stopListening();}
    private void apply(Boolean music){assertEquals("",LauncherinIdle.apply(c,music));}
    @Test public void silentTvCanBeVerifiedWithoutNotificationAccess() {
        android.media.AudioManager audio=c.getSystemService(android.media.AudioManager.class);
        Shadows.shadowOf(audio).setIsMusicActive(false);
        assertFalse(LauncherinMediaService.granted(c));
        assertEquals(Boolean.FALSE,LauncherinMediaService.musicInProgress(c));
    }
    @Test public void activeAudioWithoutNotificationAccessStillProtectsMusic() {
        android.media.AudioManager audio=c.getSystemService(android.media.AudioManager.class);
        Shadows.shadowOf(audio).setIsMusicActive(true);
        audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,0,0);
        assertEquals(Boolean.TRUE,LauncherinMediaService.musicInProgress(c));
    }
    @Test public void pausedProtectionExpiresWithoutAConnectedMediaListener() {
        Shadows.shadowOf(c.getSystemService(android.media.AudioManager.class)).setIsMusicActive(false);
        apply(true);apply(false);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMinutes(10));
        assertEquals("600000",LauncherinIdle.read(c,1));assertFalse(LauncherinIdle.protectsMusic(c));
    }
    @Test public void disconnectingTheMediaListenerDoesNotCancelThePauseDeadline() {
        LauncherinIdle.listen(c);apply(true);apply(false);
        Shadows.shadowOf(c.getSystemService(android.media.AudioManager.class)).setIsMusicActive(false);
        LauncherinMediaService service=org.robolectric.Robolectric.buildService(LauncherinMediaService.class).create().get();
        service.onListenerDisconnected();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMinutes(10));
        assertEquals("600000",LauncherinIdle.read(c,1));assertFalse(LauncherinIdle.protectsMusic(c));
    }
    @Test public void interruptedLeaseWriteCannotLeaveOurOwnSleepValueDisabledForever() {
        apply(true);c.getSharedPreferences("launcherin-idle",0).edit().remove("music_hold").commit();
        apply(false);assertEquals("600000",LauncherinIdle.read(c,1));
    }
    @Test public void explicitExternalDisablingRemainsUntouchedWhenNotOwnedByTheLease() {
        apply(false);Settings.Secure.putString(c.getContentResolver(),"sleep_timeout","-1");apply(false);
        assertEquals("-1",LauncherinIdle.read(c,1));
    }
    @Test public void photosRemainEnabledWhileOnlyStandbyIsSuspended() {
        apply(true);
        assertEquals("300000",LauncherinIdle.read(c,0));
        assertEquals("-1",LauncherinIdle.read(c,1));
        assertEquals("1",LauncherinIdle.read(c,3));
        assertEquals("1",LauncherinIdle.read(c,4));
        ShadowSystemClock.advanceBy(Duration.ofMinutes(90));apply(true);
        assertEquals("-1",LauncherinIdle.read(c,1));
        assertEquals("300000",LauncherinIdle.read(c,0));
    }
    @Test public void pauseAfterLongMusicGetsFreshTimeAndCanResume() {
        apply(true);ShadowSystemClock.advanceBy(Duration.ofHours(2));apply(false);
        ShadowSystemClock.advanceBy(Duration.ofMinutes(9));apply(false);assertEquals("-1",LauncherinIdle.read(c,1));
        apply(true);ShadowSystemClock.advanceBy(Duration.ofMinutes(20));apply(false);
        ShadowSystemClock.advanceBy(Duration.ofMinutes(9));apply(false);assertEquals("-1",LauncherinIdle.read(c,1));
        ShadowSystemClock.advanceBy(Duration.ofMinutes(1));apply(false);assertEquals("600000",LauncherinIdle.read(c,1));
    }
    @Test public void releasingThenDisablingRestoresTheOriginalNativeSettings() throws Exception {
        apply(true);apply(false);ShadowSystemClock.advanceBy(Duration.ofMinutes(10));apply(false);
        LauncherinExtras.setInt(c,LauncherinIdle.SAVER,0);LauncherinExtras.setInt(c,LauncherinIdle.SLEEP,0);apply(false);
        assertEquals("600000",LauncherinIdle.read(c,0));assertEquals("900000",LauncherinIdle.read(c,1));
        assertFalse(LauncherinIdle.protectsMusic(c));
    }
    @Test public void unavailableMediaDoesNotReleaseTheProtection() {
        apply(true);apply(false);ShadowSystemClock.advanceBy(Duration.ofMinutes(11));
        assertTrue(LauncherinIdle.apply(c,null).contains("Ative o acesso"));assertEquals("-1",LauncherinIdle.read(c,1));
        apply(false);assertEquals("-1",LauncherinIdle.read(c,1));
        ShadowSystemClock.advanceBy(Duration.ofMinutes(10));apply(false);assertEquals("600000",LauncherinIdle.read(c,1));
    }
    @Test public void mediaChangesNeverRewriteTheAppearanceFile() throws Exception {
        File file=new File(app.launcherin.tv.Config.dir(c),"config.json");
        byte[] before=Files.readAllBytes(file.toPath());
        apply(true);apply(false);ShadowSystemClock.advanceBy(Duration.ofMinutes(10));apply(false);
        assertArrayEquals(before,Files.readAllBytes(file.toPath()));
    }
    @Test public void unrelatedExternalChangesArePreservedWithoutAnActiveLease() throws Exception {
        apply(false);Settings.Secure.putString(c.getContentResolver(),"sleep_timeout","777000");apply(false);
        assertEquals("777000",LauncherinIdle.read(c,1));
        LauncherinExtras.setInt(c,LauncherinIdle.SAVER,0);LauncherinExtras.setInt(c,LauncherinIdle.SLEEP,0);apply(false);
        assertEquals("777000",LauncherinIdle.read(c,1));
    }
    @Test public void removingTheTimerPermissionsCannotBeReportedAsSuccess() {
        Shadows.shadowOf((Application)c).denyPermissions("android.permission.WRITE_SECURE_SETTINGS");
        assertTrue(LauncherinIdle.apply(c,true).contains("Libere as permissões"));
        assertEquals("900000",LauncherinIdle.read(c,1));
    }
    @Test public void aChangedNativeSleepValueCannotCutActiveMusicOff() {
        apply(true);Settings.Secure.putString(c.getContentResolver(),"sleep_timeout","30000");apply(true);
        assertEquals("-1",LauncherinIdle.read(c,1));assertEquals("300000",LauncherinIdle.read(c,0));
    }
    @Test public void aNewBootNeverReusesAnOldPauseDeadline() {
        apply(true);apply(false);ShadowSystemClock.advanceBy(Duration.ofMinutes(9));
        Settings.Global.putInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,43);apply(false);
        ShadowSystemClock.advanceBy(Duration.ofMinutes(9));apply(false);assertEquals("-1",LauncherinIdle.read(c,1));
        ShadowSystemClock.advanceBy(Duration.ofMinutes(1));apply(false);assertEquals("600000",LauncherinIdle.read(c,1));
    }
}
