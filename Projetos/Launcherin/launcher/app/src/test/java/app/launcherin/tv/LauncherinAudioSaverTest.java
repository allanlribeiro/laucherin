package app.launcherin.tv;
import org.junit.*;import static org.junit.Assert.*;
public class LauncherinAudioSaverTest {
 @Test public void fourModesAndLegacyFallbackAreStable(){assertEquals("google",LauncherinAudioSaverPolicy.mode("google"));assertEquals("cover",LauncherinAudioSaverPolicy.mode("bad"));assertEquals("photos",LauncherinAudioSaverPolicy.mode("photos"));assertEquals("dark",LauncherinAudioSaverPolicy.mode("dark"));}
 @Test public void localSaverWaitsForChosenInactivity(){assertFalse(LauncherinAudioSaverPolicy.start("cover",60,true,false,1000,60999));assertTrue(LauncherinAudioSaverPolicy.start("cover",60,true,false,1000,61000));}
 @Test public void nativeGooglePhotosIsNotFalselyForcedByLocalTimer(){assertFalse(LauncherinAudioSaverPolicy.start("google",30,true,false,0,900000));}
 @Test public void pauseDialogsAndManualOnlyPreventAutomaticEntry(){assertFalse(LauncherinAudioSaverPolicy.start("cover",60,false,false,0,100000));assertFalse(LauncherinAudioSaverPolicy.start("photos",60,true,true,0,100000));assertFalse(LauncherinAudioSaverPolicy.start("cover",0,true,false,0,100000));assertFalse(LauncherinAudioSaverPolicy.start("cover",60,true,false,100000,0));}
 @Test public void floatingElementsVisitDifferentBoundedPositions(){java.util.Set<String> seen=new java.util.HashSet<>();for(int i=0;i<8;i++){int[] p=LauncherinAudioSaverPolicy.point(i,960,540,220,270,35);assertTrue(p[0]>=35&&p[0]+220<=925);assertTrue(p[1]>=35&&p[1]+270<=505);seen.add(p[0]+":"+p[1]);}assertEquals(8,seen.size());}
 @Test public void shortViewportNeverProducesNegativeCoordinates(){for(int i=0;i<8;i++){int[] p=LauncherinAudioSaverPolicy.point(i,100,90,220,270,35);assertEquals(0,p[0]);assertEquals(0,p[1]);}}
}
