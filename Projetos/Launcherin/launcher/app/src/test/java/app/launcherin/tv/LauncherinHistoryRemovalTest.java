package app.launcherin.tv;
import org.json.*;import org.junit.Test;import static org.junit.Assert.*;
public class LauncherinHistoryRemovalTest {
 private JSONObject row(String key,long updated)throws Exception{return new JSONObject().put("key",key).put("pkg","com.netflix.ninja").put("provider","Netflix").put("title",key).put("updated",updated).put("duration",10000).put("position",1000);}
 @Test public void cachedAndImportedShelvesContainAtMostTenAndNeverYoutube()throws Exception{
  JSONArray old=new JSONArray();old.put(row("youtube",1000).put("pkg","com.google.android.youtube.tv").put("provider","YouTube"));for(int i=0;i<15;i++)old.put(row("movie"+i,i));
  JSONArray visible=LauncherinHistory.visible(old,new JSONObject());assertEquals(10,visible.length());assertEquals("movie0",visible.getJSONObject(0).getString("key"));
  JSONArray sorted=LauncherinHistory.programs(new JSONArray(),old);assertEquals(10,sorted.length());assertEquals("movie14",sorted.getJSONObject(0).getString("key"));
 }
 @Test public void pausedMetadataRefreshDoesNotUndoManualRemoval()throws Exception{
  JSONObject removed=new JSONObject(),item=row("dismissed",100);LauncherinHistory.dismiss(removed,item,200);
  assertEquals(0,LauncherinHistory.playback(new JSONArray(),row("dismissed",999),999,removed,false).length());
  assertEquals(0,LauncherinHistory.programs(new JSONArray(),new JSONArray().put(item),removed).length());assertTrue(removed.has("dismissed"));
 }
 @Test public void actualPlaybackOrNewProviderEngagementCanRestoreAnItem()throws Exception{
  JSONObject removed=new JSONObject(),item=row("again",100);LauncherinHistory.dismiss(removed,item,200);
  assertEquals(1,LauncherinHistory.playback(new JSONArray(),row("again",300),300,removed,true).length());assertFalse(removed.has("again"));
  LauncherinHistory.dismiss(removed,item,200);assertEquals(1,LauncherinHistory.programs(new JSONArray(),new JSONArray().put(row("again",201)),removed).length());assertFalse(removed.has("again"));
 }
 @Test public void publisherClockAheadOfTheTvDoesNotImmediatelyReinsertAnItem()throws Exception{
  JSONObject removed=new JSONObject(),item=row("future-clock",500);LauncherinHistory.dismiss(removed,item,200);
  assertEquals(0,LauncherinHistory.programs(new JSONArray(),new JSONArray().put(item),removed).length());
 }
 @Test public void oldRowsWithoutExplicitKeysRemainDistinctAndRemovable()throws Exception{
  JSONObject a=new JSONObject().put("provider","Netflix").put("title","A"),b=new JSONObject().put("provider","Disney+").put("title","B"),removed=new JSONObject();
  assertEquals(2,LauncherinHistory.visible(new JSONArray().put(a).put(b),removed).length());LauncherinHistory.dismiss(removed,a,100);
  assertEquals(1,LauncherinHistory.visible(new JSONArray().put(a).put(b),removed).length());
 }
}
