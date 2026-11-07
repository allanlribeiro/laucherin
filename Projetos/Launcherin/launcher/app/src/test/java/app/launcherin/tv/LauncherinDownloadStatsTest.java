package app.launcherin.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class LauncherinDownloadStatsTest {
    @Test public void reportsReceivedMegabitsPerSecondWithoutGeneratingTraffic(){
        LauncherinDownloadStats stats=new LauncherinDownloadStats();
        assertNull(stats.sample(1000000,1000));
        assertEquals(8,stats.sample(6000000,6000),.00001);
        assertEquals(0,stats.sample(6000000,11000),.00001);
    }
    @Test public void unavailableOrResetCountersDoNotInventDownloadSpeed(){
        LauncherinDownloadStats stats=new LauncherinDownloadStats();
        assertNull(stats.sample(-1,1000));assertNull(stats.sample(1000,2000));
        assertNull(stats.sample(500,3000));assertEquals(.004,stats.sample(1000,4000),.00001);
        assertNull(stats.sample(2000,4000));assertNull(stats.sample(3000,1000));
    }
    @Test public void returningToTheHomeRequiresAFreshInterval(){
        LauncherinDownloadStats stats=new LauncherinDownloadStats();stats.sample(1000,1000);
        stats.reset();assertNull(stats.sample(8000000,60000));
        assertEquals(1.6,stats.sample(9000000,65000),.00001);
        assertEquals("1,6",LauncherinDownloadStats.format(1.6));assertEquals("—",LauncherinDownloadStats.format(null));
    }
}
