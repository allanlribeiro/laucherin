package app.launcherin.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class LauncherinDashboardPolicyTest {
    @Test public void coldAndRecycledDockTilesUseTheSameAvailableSpace() {
        assertEquals(AppAdapter.dockTileWidth(844, 111, 111, 10, 10, 6, 1),
                AppAdapter.dockTileWidth(844, 0, 111, 10, 10, 6, 1));
    }
    @Test public void dockTilesAndTheirFocusZoomFitInsideTheDock() {
        for (float density : new float[]{1, 1.5f, 2, 3}) {
            int h=Math.round(111*density),w=AppAdapter.dockTileWidth(Math.round(844*density),h,h,
                    Math.round(10*density),Math.round(10*density),6,density);
            assertTrue(AppAdapter.tileHeightPx(w)*1.12f < h-20*density);
            assertTrue(w*1.12f < 844*density/6);
        }
    }
    @Test public void streamingAppsAreNeverNativeTuners() {
        assertFalse(LauncherinTv.nativePackage("com.globo.globotv"));
        assertFalse(LauncherinTv.nativePackage("com.netflix.ninja"));
        assertFalse(LauncherinTv.nativePackage("com.tcl.tv.fake"));
        assertTrue(LauncherinTv.nativePackage("com.tcl.tv"));
    }
    @Test public void standardAndTclSettingsKeysWorkWithoutAProfileOverride() {
        for (int code:new int[]{176,83,4020,4077})
            assertEquals(LauncherinTv.SETTINGS,LauncherinTv.remoteAction(code,true,new int[0]));
    }
    @Test public void notificationKeyKeepsItsMeaningOnOtherBrands() {
        assertEquals(LauncherinTv.NONE,LauncherinTv.remoteAction(83,false,new int[0]));
        assertEquals(LauncherinTv.SETTINGS,LauncherinTv.remoteAction(83,false,new int[]{83}));
    }
    @Test public void tvAndInputAreDistinctButtons() {
        assertEquals(LauncherinTv.LIVE_TV,LauncherinTv.remoteAction(170,true,new int[0]));
        for(int code:new int[]{178,24,25,3,4})
            assertEquals(LauncherinTv.NONE,LauncherinTv.remoteAction(code,true,new int[0]));
    }
    @Test public void bothDedicatedMusicAppsAreRecognizedWithEmptyMetadata() {
        assertTrue(LauncherinMusic.identifiesTrack("com.spotify.tv.android","","",""));
        assertTrue(LauncherinMusic.identifiesTrack("com.google.android.youtube.tvmusic","","",""));
        assertTrue(LauncherinMusic.identifiesTrack("com.google.android.apps.youtube.music","","",""));
    }
    @Test public void youtubeTvMusicNeedsPositiveEvidence() {
        String pkg="com.google.android.youtube.tv";
        assertFalse(LauncherinMusic.identifiesTrack(pkg,"","","Channel name"));
        assertTrue(LauncherinMusic.identifiesTrack(pkg,"https://music.youtube.com/watch?v=example","",""));
        assertTrue(LauncherinMusic.identifiesTrack(pkg,"","Album","Artist"));
        assertFalse(LauncherinMusic.identifiesTrack(pkg,"https://music.youtube.com.evil.example/watch","",""));
    }
    @Test public void similarlyNamedAppsDoNotGainMusicAccess() {
        assertFalse(LauncherinMusic.identifiesTrack("com.fake.spotify.tv.android","","",""));
        assertFalse(LauncherinMusic.identifiesTrack("com.google.android.youtube.tv.fake","https://music.youtube.com/watch","Album","Artist"));
    }
}
