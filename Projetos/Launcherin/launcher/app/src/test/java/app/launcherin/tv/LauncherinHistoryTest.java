package app.launcherin.tv;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LauncherinHistoryTest {
    private JSONObject item(String key,String source,long updated)throws Exception{return new JSONObject().put("key",key).put("source",source).put("updated",updated).put("duration",1000).put("position",400);}
    @Test public void missingMetadataArtworkAndLinkComeFromThePublicProgram()throws Exception{
        JSONObject media=item("same","media",200),nativeRow=item("same","watchnext",100).put("art","https://example.com/poster.jpg").put("intent_uri","app://episode/2");
        JSONArray result=LauncherinHistory.programs(new JSONArray().put(media),new JSONArray().put(nativeRow));
        assertEquals(1,result.length());assertEquals(200,result.getJSONObject(0).getLong("updated"));
        assertEquals("https://example.com/poster.jpg",result.getJSONObject(0).getString("art"));assertEquals("app://episode/2",result.getJSONObject(0).getString("intent_uri"));
        assertFalse(media.has("art"));
    }
    @Test public void aFreshNativeSnapshotDropsRemovedNativeRowsButKeepsPlayback()throws Exception{
        JSONArray old=new JSONArray().put(item("removed","watchnext",100)).put(item("played","media",200));
        JSONArray result=LauncherinHistory.programs(old,new JSONArray().put(item("next","watchnext",300)));
        assertEquals(2,result.length());assertEquals("next",result.getJSONObject(0).getString("key"));assertEquals("played",result.getJSONObject(1).getString("key"));
    }
    @Test public void completedPlaybackIsRemovedAndUnknownNativeTimeIsNotInvented()throws Exception{
        JSONObject watched=item("done","media",200).put("position",990);
        assertEquals(0,LauncherinHistory.playback(new JSONArray().put(watched),watched,200).length());
        JSONArray result=LauncherinHistory.programs(new JSONArray(),new JSONArray().put(item("unknown","watchnext",0)));
        assertEquals(0,result.getJSONObject(0).getLong("updated"));
    }
    @Test public void historyIsBoundedAndSortedByActualEngagement()throws Exception{
        JSONArray programs=new JSONArray();for(int i=0;i<50;i++)programs.put(item("item"+i,"watchnext",i));
        JSONArray result=LauncherinHistory.programs(new JSONArray(),programs);assertEquals(10,result.length());assertEquals("item49",result.getJSONObject(0).getString("key"));
    }
}
