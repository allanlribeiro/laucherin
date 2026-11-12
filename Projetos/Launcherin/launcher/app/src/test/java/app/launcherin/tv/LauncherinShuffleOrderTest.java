package app.launcherin.tv;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class LauncherinShuffleOrderTest {
 @Test public void shuffleVisitsEachRemainingTrackOnceAndStopsWithoutRepeat(){LauncherinShuffleOrder order=new LauncherinShuffleOrder();Random random=new Random(7);Set<Integer> heard=new HashSet<>();int current=2;heard.add(current);for(int i=0;i<5;i++){current=order.next(current,6,false,random);assertTrue(current>=0&&current<6);assertTrue(heard.add(current));}assertEquals(6,heard.size());assertEquals(-1,order.next(current,6,false,random));}
 @Test public void previousRestoresForwardOrderWithoutRepeatingAlreadyQueuedEntries(){LauncherinShuffleOrder order=new LauncherinShuffleOrder();Random random=new Random(7);int a=order.next(0,5,false,random),b=order.next(a,5,false,random);assertEquals(a,order.previous(b,5));assertEquals(b,order.next(a,5,false,random));}
 @Test public void repeatAllStartsANewCycleAndSingleSongRequiresExplicitRepeat(){LauncherinShuffleOrder order=new LauncherinShuffleOrder();Random r=new Random(1);int current=0;for(int i=0;i<12;i++){int next=order.next(current,3,true,r);assertNotEquals(current,next);current=next;}order.reset();assertEquals(-1,order.next(0,1,false,r));assertEquals(0,order.next(0,1,true,r));}
 @Test public void resetAndQueueSizeChangesCannotReturnStaleIndices(){LauncherinShuffleOrder order=new LauncherinShuffleOrder();Random r=new Random(1);order.next(0,20,false,r);int next=order.next(0,2,false,r);assertEquals(1,next);order.reset();assertEquals(-1,order.previous(1,2));}
}
