package app.launcherin.tv;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.time.Instant;
import static org.junit.Assert.*;

public class LauncherinSportsTest {
    private static final long NOW=Instant.parse("2026-11-04T15:00:00Z").toEpochMilli();
    private JSONObject event(String id,String date,String state,String status,String team) throws Exception {
        JSONArray sides=new JSONArray().put(new JSONObject().put("homeAway","home").put("team",new JSONObject().put("id",team).put("displayName","Casa")))
                .put(new JSONObject().put("homeAway","away").put("team",new JSONObject().put("id","42").put("displayName","Visitante")));
        if(state.equals("post")){sides.getJSONObject(0).put("score",new JSONObject().put("displayValue","2"));sides.getJSONObject(1).put("score",new JSONObject().put("displayValue","1"));}
        return new JSONObject().put("id",id).put("date",date).put("competitions",new JSONArray().put(new JSONObject().put("competitors",sides).put("status",new JSONObject().put("type",new JSONObject().put("state",state).put("name",status).put("completed",state.equals("post"))))));
    }
    private JSONObject response(JSONObject... events) throws Exception {JSONArray rows=new JSONArray();for(JSONObject event:events)rows.put(event);return new JSONObject().put("events",rows);}
    @Test public void nextTwoAreChronologicalAcrossCompetitionsAndDuplicatesAreRemoved() throws Exception {
        JSONObject a=event("a","2026-11-05T19:00Z","pre","STATUS_SCHEDULED","819"),b=event("b","2026-11-09T19:00Z","pre","STATUS_SCHEDULED","819"),c=event("c","2026-11-14T19:00Z","pre","STATUS_SCHEDULED","819");
        LauncherinSports.Snapshot result=new LauncherinSports.Snapshot(LauncherinSports.merge("819",response(c,b,a),response(a),NOW));
        assertEquals(2,result.upcoming.size());assertEquals("a",result.upcoming.get(0).id);assertEquals("b",result.upcoming.get(1).id);
    }
    @Test public void previousMeansLatestCompletedGameWithAnActualScore() throws Exception {
        JSONObject newest=event("new","2026-11-03T19:00Z","post","STATUS_FULL_TIME","819"),old=event("old","2026-10-28T19:00Z","post","STATUS_FULL_TIME","819");
        LauncherinSports.Snapshot result=new LauncherinSports.Snapshot(LauncherinSports.merge("819",response(),response(old,newest),NOW));
        assertEquals("new",result.previous.id);assertEquals("Casa  2 × 1  Visitante",result.previous.score());assertTrue(result.upcoming.isEmpty());
    }
    @Test public void cancelledPostponedForeignAndOldScheduledGamesAreExcluded() throws Exception {
        JSONObject cancelled=event("c","2026-11-03T19:00Z","post","STATUS_CANCELED","819"),postponed=event("p","2026-11-05T19:00Z","pre","STATUS_POSTPONED","819"),foreign=event("f","2026-11-05T19:00Z","pre","STATUS_SCHEDULED","999"),old=event("o","2026-11-01T19:00Z","pre","STATUS_SCHEDULED","819");
        LauncherinSports.Snapshot result=new LauncherinSports.Snapshot(LauncherinSports.merge("819",response(cancelled,postponed,foreign,old),response(),NOW));
        assertNull(result.previous);assertTrue(result.upcoming.isEmpty());
    }
    @Test public void liveMatchIsShownAndDoesNotBecomeAnInventedResult() throws Exception {
        JSONObject live=event("l","2026-11-04T14:00Z","in","STATUS_IN_PROGRESS","819");
        LauncherinSports.Snapshot result=new LauncherinSports.Snapshot(LauncherinSports.merge("819",response(live),response(),NOW));
        assertNull(result.previous);assertEquals("in",result.upcoming.get(0).state);assertEquals("",result.upcoming.get(0).homeScore);
    }
    @Test public void missingAndMalformedScoresDoNotTurnIntoZeroZero() throws Exception {
        JSONObject missing=event("m","2026-11-03T19:00Z","post","STATUS_FULL_TIME","819");missing.getJSONArray("competitions").getJSONObject(0).getJSONArray("competitors").getJSONObject(0).remove("score");
        assertNull(new LauncherinSports.Snapshot(LauncherinSports.merge("819",response(),response(missing),NOW)).previous);
    }
}
