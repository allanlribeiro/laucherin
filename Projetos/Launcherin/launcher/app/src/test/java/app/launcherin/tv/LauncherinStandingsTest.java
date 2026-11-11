package app.launcherin.tv;

import android.content.Context;
import android.view.View;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w960dp-h540dp-land-mdpi")
public class LauncherinStandingsTest {
    static final long NOW=1794391200000L;
    private Context context;
    @Before public void reset(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("launcherin-standings",0).edit().clear().commit();}
    static JSONObject entry(String id,String name,int rank,int points,int played)throws Exception{
        JSONArray stats=new JSONArray();String[] names={"rank","points","gamesPlayed","wins","ties","losses","pointDifferential"};int[] values={rank,points,played,10,4,2,-3};for(int i=0;i<names.length;i++)stats.put(new JSONObject().put("name",names[i]).put("value",(double)values[i]));return new JSONObject().put("team",new JSONObject().put("id",id).put("displayName",name)).put("stats",stats);
    }
    static JSONObject response(JSONObject...entries)throws Exception{
        JSONArray list=new JSONArray();for(JSONObject e:entries)list.put(e);return new JSONObject().put("uid","s:600~l:630").put("children",new JSONArray().put(new JSONObject().put("standings",new JSONObject().put("name","overall").put("season",2026).put("entries",list))));
    }
    static JSONObject example()throws Exception{JSONObject[] all=new JSONObject[20];all[0]=entry("819","Flamengo",1,34,16);all[1]=entry("2029","Palmeiras",2,34,16);for(int i=2;i<20;i++)all[i]=entry(""+(300+i),"Time "+i,i+1,20,16);return response(all);}
    static LauncherinSports.Team team(String id)throws Exception{return new LauncherinSports.Team(new JSONObject().put("id",id).put("name","Time "+id).put("league","bra.1"));}
    @Test public void parsesAllTwentyClubsAndPreservesOfficialTiedRanking()throws Exception{
        List<JSONObject> rows=new ArrayList<>();for(int i=20;i>=1;i--)rows.add(entry(""+(100+i),"Clube "+i,i,30,16));LauncherinStandings.Snapshot s=new LauncherinStandings.Snapshot(LauncherinStandings.parse(response(rows.toArray(new JSONObject[0])),NOW));
        assertEquals(20,s.rows.size());assertEquals(1,s.rows.get(0).rank);assertEquals("101",s.rows.get(0).id);assertEquals(20,s.rows.get(19).rank);assertEquals(2026,s.season);assertEquals(4,s.rows.get(0).draws);assertEquals(-3,s.rows.get(0).goalDifference);
    }
    @Test public void recordedEspnResponseParsesTwentyClubsWithoutInventedValues()throws Exception{
        try(java.io.InputStream in=getClass().getResourceAsStream("/standings/bra-1-2026.json")){assertNotNull(in);JSONObject r=new JSONObject(new String(LauncherinExtras.bytes(in,65536),java.nio.charset.StandardCharsets.UTF_8));LauncherinStandings.Snapshot result=new LauncherinStandings.Snapshot(LauncherinStandings.parse(r,NOW));assertEquals(20,result.rows.size());assertEquals(2026,result.season);assertEquals(1,result.rows.get(0).rank);assertEquals(20,result.rows.get(19).rank);}
    }
    @Test public void incompleteClassificationIsRejected()throws Exception{assertThrows(Exception.class,()->LauncherinStandings.parse(response(entry("819","Flamengo",1,34,16)),NOW));}
    @Test public void rejectsOtherChampionshipRatherThanMislabelingIt()throws Exception{JSONObject r=example().put("uid","s:600~l:631");assertThrows(Exception.class,()->LauncherinStandings.parse(r,NOW));}
    @Test public void missingAndInvalidStatsDoNotBecomeZero()throws Exception{
        JSONObject r=example();JSONObject e=r.getJSONArray("children").getJSONObject(0).getJSONObject("standings").getJSONArray("entries").getJSONObject(0);e.getJSONArray("stats").remove(1);assertEquals("Estatística ausente",assertThrows(Exception.class,()->LauncherinStandings.parse(r,NOW)).getMessage());
        JSONObject invalid=example();invalid.getJSONArray("children").getJSONObject(0).getJSONObject("standings").getJSONArray("entries").getJSONObject(0).getJSONArray("stats").getJSONObject(0).put("value",1.5);assertEquals("Estatística inválida",assertThrows(Exception.class,()->LauncherinStandings.parse(invalid,NOW)).getMessage());
    }
    @Test public void rejectsDuplicatesAndEmptyOrOversizedTables()throws Exception{
        assertThrows(Exception.class,()->LauncherinStandings.parse(response(),NOW));
        JSONObject duplicate=example();duplicate.getJSONArray("children").getJSONObject(0).getJSONObject("standings").getJSONArray("entries").getJSONObject(1).getJSONObject("team").put("id","819");assertEquals("Classificação duplicada",assertThrows(Exception.class,()->LauncherinStandings.parse(duplicate,NOW)).getMessage());
        JSONObject rank=example();rank.getJSONArray("children").getJSONObject(0).getJSONObject("standings").getJSONArray("entries").getJSONObject(1).getJSONArray("stats").getJSONObject(0).put("value",1.0);assertEquals("Classificação duplicada",assertThrows(Exception.class,()->LauncherinStandings.parse(rank,NOW)).getMessage());
        JSONObject[] many=new JSONObject[21];for(int i=0;i<many.length;i++)many[i]=entry(""+(100+i),"Time",i+1,1,1);assertThrows(Exception.class,()->LauncherinStandings.parse(response(many),NOW));
    }
    @Test public void cacheAvoidsRequestsAndFailedRefreshPreservesPreviousTable()throws Exception{
        AtomicInteger hits=new AtomicInteger();LauncherinStandings.Fetcher f=()->{hits.incrementAndGet();return example();};LauncherinStandings.Snapshot first=LauncherinStandings.load(context,f,NOW);LauncherinStandings.Snapshot cached=LauncherinStandings.load(context,f,NOW+1000);assertEquals(1,hits.get());assertEquals(first.updated,cached.updated);
        LauncherinStandings.Snapshot stale=LauncherinStandings.load(context,()->{hits.incrementAndGet();throw new Exception("offline");},NOW+LauncherinStandings.REFRESH_MS+1);assertEquals(first.updated,stale.updated);assertTrue(stale.old(NOW+LauncherinStandings.REFRESH_MS+1));assertEquals(2,hits.get());
        LauncherinStandings.load(context,f,NOW+LauncherinStandings.REFRESH_MS+1000);assertEquals(2,hits.get());assertEquals(20,LauncherinStandings.cached(context).rows.size());
    }
    @Test public void firstFailureCreatesNoFakeTableAndRetriesAreThrottled()throws Exception{
        AtomicInteger hits=new AtomicInteger();LauncherinStandings.Fetcher f=()->{hits.incrementAndGet();throw new Exception("offline");};assertThrows(Exception.class,()->LauncherinStandings.load(context,f,NOW));assertNull(LauncherinStandings.cached(context));assertThrows(Exception.class,()->LauncherinStandings.load(context,f,NOW+100));assertEquals(1,hits.get());
    }
    @Test public void nearestFixturesIncludeBothFavouritesAndDeduplicateTheirSharedMatch()throws Exception{
        Map<String,LauncherinSports.Snapshot> data=new HashMap<>();JSONObject a=match("shared",NOW+100),b=match("second",NOW+200),c=match("third",NOW+300);
        data.put("819",snapshot(a,b));data.put("2029",snapshot(a,c));List<LauncherinSports.Match> result=LauncherinSports.nextGames(Arrays.asList(team("819"),team("2029")),data);assertEquals(2,result.size());assertEquals("shared",result.get(0).id);assertEquals("second",result.get(1).id);
        data.put("2029",snapshot(c));result=LauncherinSports.nextGames(Arrays.asList(team("819"),team("2029")),data);assertEquals("shared",result.get(0).id);assertEquals("third",result.get(1).id);
    }
    static JSONObject match(String id,long at)throws Exception{return new JSONObject().put("id",id).put("homeId","819").put("awayId","2029").put("home","Flamengo").put("away","Palmeiras").put("state","pre").put("time",at);}
    static LauncherinSports.Snapshot snapshot(JSONObject...games)throws Exception{JSONArray a=new JSONArray();for(JSONObject g:games)a.put(g);return new LauncherinSports.Snapshot(new JSONObject().put("updated",NOW).put("upcoming",a));}
    @Test public void tableIsOnDemandScrollableAndKeepsPopupBoundsAndFavouriteHighlight()throws Exception{
        LauncherinVisualPreviewTest.PreviewHost host=Robolectric.buildActivity(LauncherinVisualPreviewTest.PreviewHost.class).setup().get();FrameLayout root=host.findViewById(R.id.root);root.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));root.layout(0,0,960,540);View anchor=new View(host);root.addView(anchor,new FrameLayout.LayoutParams(200,121));
        AtomicInteger loads=new AtomicInteger();List<LauncherinSports.Team> teams=Arrays.asList(team("819"),team("2029"));JSONObject[] all=new JSONObject[20];all[0]=entry("819","Flamengo",1,34,16);all[1]=entry("2029","Palmeiras",2,34,16);for(int i=2;i<20;i++)all[i]=entry(""+(300+i),"Time "+i,i+1,20,16);
        LauncherinStandings.Snapshot s=new LauncherinStandings.Snapshot(LauncherinStandings.parse(response(all),System.currentTimeMillis()));LauncherinSportsCard card=new LauncherinSportsCard(host,anchor,teams,()->{},Runnable::run,()->{loads.incrementAndGet();return s;});int width=card.popup.width,height=card.popup.height;
        assertEquals(0,loads.get());card.render(new HashMap<>(),false);card.selectTable(true);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(1,loads.get());assertEquals(width,card.popup.width);assertEquals(height,card.popup.height);assertTrue(width<960);assertTrue(height<540);
        View first=card.popup.card.findViewWithTag("standings-819");assertNotNull(first);assertTrue(first.isFocusable());assertEquals(card.popup.card.findViewWithTag("standings-2029").getId(),first.getNextFocusDownId());assertTrue(first.getContentDescription().toString().contains("time favorito"));assertNotNull(card.popup.card.findViewWithTag("standings-319"));assertTrue(card.popup.card.findViewWithTag("standings-319").getParent().getParent() instanceof ScrollView);
        card.selectTable(false);assertTrue(card.popup.showing());card.selectTable(true);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(1,loads.get());card.popup.dismiss();assertFalse(card.popup.showing());host.finish();
    }
}
