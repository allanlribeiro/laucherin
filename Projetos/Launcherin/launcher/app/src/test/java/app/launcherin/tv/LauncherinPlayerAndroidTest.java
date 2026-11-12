package app.launcherin.tv;

import android.content.*;
import android.media.*;
import android.media.session.PlaybackState;
import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowMediaPlayer;
import org.robolectric.util.ReflectionHelpers;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class LauncherinPlayerAndroidTest {
    private Context c;private LauncherinMusicLibrary library;private ServiceController<LauncherinPlayerService> lifecycle;private LauncherinPlayerService service;
    @Before public void setup(){c=RuntimeEnvironment.getApplication();library=new LauncherinMusicLibrary(c);library.write("queue",Arrays.asList(t(1),t(2),t(3)));library.progress(1,12000);lifecycle=Robolectric.buildService(LauncherinPlayerService.class).create();service=lifecycle.get();}
    @After public void finish(){lifecycle.destroy();LauncherinIdle.stopListening();}
    private LauncherinTrack t(int i){return LauncherinNativeMusicTest.track(i);}
    private MediaPlayer engine(int position){MediaPlayer p=new MediaPlayer();ShadowMediaPlayer shadow=Shadows.shadowOf(p);shadow.setState(ShadowMediaPlayer.State.PREPARED);shadow.setCurrentPosition(position);ReflectionHelpers.setField(service,"player",p);ReflectionHelpers.setField(service,"prepared",true);ReflectionHelpers.setField(service,"state",PlaybackState.STATE_PLAYING);return p;}
    @Test public void queueAndFavoritesSurviveServiceRecreation(){assertEquals(t(2).url,LauncherinPlayerService.snapshot(c).optString("url"));library.toggle(t(3));assertTrue(new LauncherinMusicLibrary(c).favorite(t(3)));assertEquals(12000,LauncherinPlayerService.snapshot(c).optLong("position"));assertFalse(library.toggle(t(3)));}
    @Test public void queueLimitAndValidationCannotCorruptSavedTracks()throws Exception{List<LauncherinTrack> q=new ArrayList<>();for(int i=0;i<120;i++)q.add(t(i));library.write("bounded",q);assertEquals(100,library.read("bounded").size());c.getSharedPreferences("launcherin-player",0).edit().putString("bad","[{\"title\":\"bad\",\"url\":\"file:///x\"}]").commit();assertTrue(library.read("bad").isEmpty());}
    @Test public void removingAnEarlierQueueItemPreservesCurrentSongAndPosition(){engine(12000);LauncherinPlayerService.remove(c,0);assertEquals(2,LauncherinPlayerService.tracks(c).size());assertEquals(t(2).url,LauncherinPlayerService.snapshot(c).optString("url"));assertEquals(12000,LauncherinPlayerService.snapshot(c).optLong("position"));LauncherinPlayerService.remove(c,0);assertEquals(2,LauncherinPlayerService.tracks(c).size());}
    @Test public void nativeAudioProtectsStandbyWithoutNotificationListener(){engine(1000);assertFalse(LauncherinMediaService.granted(c));assertEquals(Boolean.TRUE,LauncherinMediaService.musicInProgress(c));assertTrue(LauncherinMediaService.playbackInProgress(c));}
    @Test public void externalModePausesOwnEngineAndKeepsLibrary(){MediaPlayer p=engine(9000);library.select(true);LauncherinPlayerService.external(c);assertFalse(library.nativeSelected());assertFalse(LauncherinPlayerService.active());assertEquals(3,library.read("queue").size());assertEquals(9000,library.position());assertEquals(ShadowMediaPlayer.State.PAUSED,Shadows.shadowOf(p).getState());}
    @Test public void pauseCancelsPendingResolveAndPersistsPosition(){engine(15500);service.onStartCommand(new Intent().putExtra("player_action","pause"),0,1);assertFalse(LauncherinPlayerService.active());assertEquals(15500,library.position());assertTrue(Shadows.shadowOf(service).isStoppedBySelf());}
    @Test public void replacementHonorsChosenIndexNotPreviousQueuePosition()throws Exception{library.write("pending",Arrays.asList(t(7),t(8),t(9)));CountDownLatch waiting=new CountDownLatch(1);service.resolver=track->{waiting.await(2,TimeUnit.SECONDS);return "https://example.org/audio.m4a";};service.onStartCommand(new Intent().putExtra("player_action","replace").putExtra("index",2),0,1);assertEquals(t(9).url,LauncherinPlayerService.snapshot(c).optString("url"));assertEquals(0,LauncherinPlayerService.snapshot(c).optLong("position"));service.onStartCommand(new Intent().putExtra("player_action","pause"),0,2);waiting.countDown();Thread.sleep(30);Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(LauncherinPlayerService.active());assertNull(ReflectionHelpers.getField(service,"player"));}
    @Test public void aFailedSourceDoesNotRemainBufferingOrHoldStandbyForever(){ReflectionHelpers.setField(service,"state",PlaybackState.STATE_BUFFERING);ReflectionHelpers.callInstanceMethod(service,"fail",ReflectionHelpers.ClassParameter.from(String.class,"Fonte indisponível"));assertFalse(LauncherinPlayerService.active());assertEquals("Fonte indisponível",library.error());assertEquals("Fonte indisponível",LauncherinPlayerService.snapshot(c).optString("error"));}
    private void drainAudio()throws Exception{ExecutorService io=ReflectionHelpers.getField(service,"io");io.submit(()->{}).get(2,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(80));}
    @Test public void resolvedAudioPreparesStartsAndPublishesOwnSession()throws Exception{
        ShadowMediaPlayer.setMediaInfoProvider(source->new ShadowMediaPlayer.MediaInfo(180000,1));service.resolver=t->"https://example.org/fixture.m4a";
        service.onStartCommand(new Intent().putExtra("player_action","play"),0,1);drainAudio();assertTrue(LauncherinPlayerService.playing());assertTrue(library.nativeSelected());assertTrue(LauncherinPlayerService.snapshot(c).optLong("position")>=12000);assertTrue(Shadows.shadowOf((MediaPlayer)ReflectionHelpers.getField(service,"player")).isReallyPlaying());
    }
    @Test public void audioCompletionAdvancesTheQueueWithoutOpeningAnActivity()throws Exception{
        ShadowMediaPlayer.setMediaInfoProvider(source->new ShadowMediaPlayer.MediaInfo(180000,1));service.resolver=t->"https://example.org/fixture.m4a";
        service.onStartCommand(new Intent().putExtra("player_action","play"),0,1);drainAudio();Shadows.shadowOf((MediaPlayer)ReflectionHelpers.getField(service,"player")).invokeCompletionListener();drainAudio();assertEquals(t(3).url,LauncherinPlayerService.snapshot(c).optString("url"));assertTrue(LauncherinPlayerService.playing());assertNull(Shadows.shadowOf((android.app.Application)c).getNextStartedActivity());
    }
    @Test public void nextAtEndOfPausedQueueDoesNotLeaveAnIdleForegroundService(){ReflectionHelpers.setField(service,"index",2);service.onStartCommand(new Intent().putExtra("player_action","next"),0,1);assertFalse(LauncherinPlayerService.active());assertTrue(Shadows.shadowOf(service).isStoppedBySelf());}
    @Test public void closingOwnServiceKeepsAResumableQueue(){engine(22000);service.onStartCommand(new Intent().putExtra("player_action","pause"),0,1);assertEquals(22000,new LauncherinMusicLibrary(c).position());assertEquals(t(2).url,library.read("queue").get(library.index(library.read("queue"))).url);}
    @Test public void transientAudioInterruptionRetainsForegroundLeaseUntilExplicitPause(){engine(3000);ReflectionHelpers.callInstanceMethod(service,"ensureForeground");ReflectionHelpers.callInstanceMethod(service,"pause",ReflectionHelpers.ClassParameter.from(boolean.class,true));assertTrue((Boolean)ReflectionHelpers.getField(service,"foreground"));assertFalse(Shadows.shadowOf(service).isStoppedBySelf());assertFalse(LauncherinPlayerService.playing());ReflectionHelpers.callInstanceMethod(service,"pause",ReflectionHelpers.ClassParameter.from(boolean.class,false));assertFalse((Boolean)ReflectionHelpers.getField(service,"foreground"));assertTrue(Shadows.shadowOf(service).isStoppedBySelf());}
    @Test public void profileSwitchPausesAndSavesTheOldQueueBeforeLoadingTheNewProfile(){engine(19000);LauncherinMusicProfiles profiles=new LauncherinMusicProfiles(c);String second=profiles.list().get(1).id;LauncherinPlayerService.switchProfile(c,second);assertFalse(LauncherinPlayerService.playing());assertTrue(LauncherinPlayerService.tracks(c).isEmpty());assertEquals(0,new LauncherinMusicLibrary(c).position());LauncherinPlayerService.switchProfile(c,"default");assertEquals(3,LauncherinPlayerService.tracks(c).size());assertEquals(19000,LauncherinPlayerService.snapshot(c).optLong("position"));assertFalse(LauncherinPlayerService.playing());}
    @Test public void shuffleAndRepeatChangeOnlyMusicOrderPreferences(){List<LauncherinTrack> before=LauncherinPlayerService.tracks(c);long position=library.position();LauncherinPlayerService.shuffle(c);assertTrue(library.shuffle());LauncherinPlayerService.repeat(c);assertEquals(1,library.repeat());LauncherinPlayerService.repeat(c);assertEquals(2,library.repeat());LauncherinPlayerService.repeat(c);assertEquals(0,library.repeat());assertEquals(before.size(),LauncherinPlayerService.tracks(c).size());assertEquals(before.get(0).url,LauncherinPlayerService.tracks(c).get(0).url);assertEquals(position,library.position());}
    @Test public void explicitNextRecordsOnlyARealQuickSkipAndDoesNotPenalizeTheNextSong(){
        service.resolver=t->{throw new java.io.IOException("fixture offline");};MediaPlayer p=engine(0);ReflectionHelpers.callInstanceMethod(service,"changed");
        org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofSeconds(10));Shadows.shadowOf(p).setCurrentPosition(10000);
        service.onStartCommand(new Intent().putExtra("player_action","next"),0,1);
        LauncherinMusicTaste taste=new LauncherinMusicTaste(c);assertEquals(1,taste.skips(t(2)));assertEquals(0,taste.skips(t(3)));assertEquals(0,taste.completed(t(2)));
    }
    @Test public void profileSwitchCreditsLongListeningToOnlyTheOldProfile(){
        MediaPlayer p=engine(0);ReflectionHelpers.callInstanceMethod(service,"changed");org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofSeconds(150));Shadows.shadowOf(p).setCurrentPosition(150000);
        LauncherinMusicTaste old=new LauncherinMusicTaste(c);LauncherinPlayerService.switchProfile(c,new LauncherinMusicProfiles(c).list().get(1).id);
        assertEquals(1,old.completed(t(2)));assertEquals(0,new LauncherinMusicTaste(c).completed(t(2)));assertFalse(LauncherinPlayerService.playing());
    }
    @Test public void seekToTheEndDoesNotTeachACompleteListen(){
        MediaPlayer p=engine(0);ReflectionHelpers.callInstanceMethod(service,"changed");LauncherinPlayerService.seek(c,175000);
        org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofSeconds(5));Shadows.shadowOf(p).setCurrentPosition(180000);
        LauncherinPlayerService.external(c);ReflectionHelpers.callInstanceMethod(service,"releasePlayer");assertEquals(0,new LauncherinMusicTaste(c).completed(t(2)));assertEquals(0,new LauncherinMusicTaste(c).skips(t(2)));
    }

}
