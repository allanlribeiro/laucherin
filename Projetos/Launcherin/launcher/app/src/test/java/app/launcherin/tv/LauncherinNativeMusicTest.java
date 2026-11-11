package app.launcherin.tv;

import org.junit.*;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class LauncherinNativeMusicTest {
    static LauncherinTrack track(int n){return new LauncherinTrack("https://www.youtube.com/watch?v=abcdefghij"+(n%10),"Faixa "+n,"Artista","",180000,"youtube");}
    @Test public void publicPagesAcceptOnlySupportedStableIdentifiers(){assertTrue(LauncherinTrack.validPage(track(1).url));assertTrue(LauncherinTrack.validPage("https://soundcloud.com/artist/song"));for(String bad:new String[]{"http://www.youtube.com/watch?v=abcdefghij1","https://youtube.com.evil/watch?v=abcdefghij1","https://user@www.youtube.com/watch?v=abcdefghij1","https://www.youtube.com:443/watch?v=abcdefghij1","https://www.youtube.com/results?search_query=music","file:///sdcard/music.mp3","https://soundcloud.com/user"})assertFalse(bad,LauncherinTrack.validPage(bad));}
    @Test public void roundTripPreservesTrackButNotExpiringStreamUrl()throws Exception{LauncherinTrack t=track(2);JSONObject j=t.json();assertEquals(t.url,LauncherinTrack.read(j).url);assertFalse(j.has("stream"));assertNull(LauncherinTrack.read(new JSONObject().put("url","https://127.0.0.1/music").put("title","bad")));}
    @Test public void lrcSupportsRepeatedTimestampsFractionsAndOffset(){List<LauncherinLyrics.Line> lines=LauncherinLyrics.parse("[offset:-100]\n[00:01.20][01:02.345]Teste\n[ar:Artista]\n[00:03]Outra");assertEquals(3,lines.size());assertEquals(1100,lines.get(0).time);assertEquals(2900,lines.get(1).time);assertEquals(62245,lines.get(2).time);assertEquals("Teste",lines.get(2).text);}
    @Test public void lyricSelectionDoesNotAdvanceEarlyOrAtPause(){LauncherinLyrics.Result r=new LauncherinLyrics.Result("","[00:02]A\n[00:05]B",false);assertEquals(-1,r.at(1999));assertEquals(0,r.at(2000));assertEquals(0,r.at(4999));assertEquals(1,r.at(5000));assertEquals(0,r.at(2500));}
    @Test public void wrongArtistOrRecordingCannotSupplySynchronizedLyrics()throws Exception{JSONObject j=new JSONObject().put("trackName","Canção").put("artistName","Artista").put("duration",180);assertTrue(LauncherinLyrics.matches(j,"Cancao","Artista - Topic",180000));assertFalse(LauncherinLyrics.matches(j,"Cancao","Outro",180000));assertFalse(LauncherinLyrics.matches(j,"Cancao ao vivo","Artista",180000));assertFalse(LauncherinLyrics.matches(j,"Cancao","Artista",195000));}
    @Test public void lyricsQuerySeparatesVideoTitleWithoutInventingMissingFields(){String[] q=LauncherinLyrics.query(new LauncherinTrack(track(1).url,"Artista - Canção (Official Video)","Canal","",180000,"youtube"));assertArrayEquals(new String[]{"Canção","Artista"},q);}
}
