package app.launcherin.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class LauncherinYouTubeMusicPolicyTest {
    @Test public void recognizesOnlyYouTubeMusicAndTheAmbiguousTVPublisher() {
        assertTrue(LauncherinYouTubeMusicPolicy.youtubePackage("com.google.android.youtube.tvmusic"));
        assertTrue(LauncherinYouTubeMusicPolicy.youtubePackage("com.google.android.apps.youtube.music"));
        assertTrue(LauncherinYouTubeMusicPolicy.youtubePackage("com.google.android.youtube.tv.music"));
        assertTrue(LauncherinYouTubeMusicPolicy.youtubePackage("com.google.android.youtube.tv"));
        assertFalse(LauncherinYouTubeMusicPolicy.youtubePackage("com.spotify.tv.android"));
        assertFalse(LauncherinYouTubeMusicPolicy.youtubePackage("com.google.android.youtube"));
        assertFalse(LauncherinYouTubeMusicPolicy.youtubePackage(null));
    }

    @Test public void progressIsClampedAndUnknownLengthIsSafe() {
        assertEquals(0, LauncherinYouTubeMusicPolicy.progress(5000, 0));
        assertEquals(0, LauncherinYouTubeMusicPolicy.progress(-2000, 240000));
        assertEquals(500, LauncherinYouTubeMusicPolicy.progress(120000, 240000));
        assertEquals(1000, LauncherinYouTubeMusicPolicy.progress(400000, 240000));
    }

    @Test public void extrapolatesOnlyWhenPlayingAndCapsStaleSession() {
        assertEquals(15000L, LauncherinYouTubeMusicPolicy.position(10000, 2000, 7000, true, 180000));
        assertEquals(10000L, LauncherinYouTubeMusicPolicy.position(10000, 2000, 7000, false, 180000));
        assertEquals(70000L, LauncherinYouTubeMusicPolicy.position(10000, 1000, 360000, true, 180000));
        assertEquals(180000L, LauncherinYouTubeMusicPolicy.position(179000, 1000, 5000, true, 180000));
    }

    @Test public void clockFormatsPositiveElapsedTime() {
        assertEquals("0:00", LauncherinYouTubeMusicPolicy.clock(-300));
        assertEquals("0:09", LauncherinYouTubeMusicPolicy.clock(9000));
        assertEquals("2:04", LauncherinYouTubeMusicPolicy.clock(124999));
    }
}
