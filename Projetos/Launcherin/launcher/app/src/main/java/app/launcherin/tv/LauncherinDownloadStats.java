package app.launcherin.tv;

import android.net.TrafficStats;
import android.os.SystemClock;
import java.util.Locale;

/** Passive device-wide received-byte counters. Never creates traffic or runs a speed test. */
final class LauncherinDownloadStats {
    private long bytes=-1,time=-1;
    synchronized void reset(){bytes=time=-1;}
    Double sample(){return sample(TrafficStats.getTotalRxBytes(),SystemClock.elapsedRealtime());}
    synchronized Double sample(long received,long now){
        if(received<0||now<0){reset();return null;}
        Double rate=bytes>=0&&received>=bytes&&now>time?(received-bytes)*.008d/(now-time):null;
        bytes=received;time=now;return rate;
    }
    static String format(Double mbps){return mbps==null?"—":String.format(Locale.forLanguageTag("pt-BR"),mbps>=100?"%.0f":"%.1f",mbps);}
}
