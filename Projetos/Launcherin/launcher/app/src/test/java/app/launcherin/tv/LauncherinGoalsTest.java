package app.launcherin.tv;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public class LauncherinGoalsTest {
 @Test public void firstObservationAndRepeatedScoresNeverAnnounceOldGoals() throws Exception {
  JSONObject data=new JSONObject();assertFalse(LauncherinGoals.observe(data,"match:team",2,"in",1000));
  assertFalse(LauncherinGoals.observe(data,"match:team",2,"in",31000));
  assertTrue(LauncherinGoals.observe(data,"match:team",3,"in",61000));
  assertFalse(LauncherinGoals.observe(data,"match:team",3,"in",91000));
 }
 @Test public void varRollbackDoesNotReannounceTheSameScore() throws Exception {
  JSONObject data=new JSONObject();LauncherinGoals.observe(data,"a",0,"in",1000);
  assertTrue(LauncherinGoals.observe(data,"a",1,"in",31000));
  assertFalse(LauncherinGoals.observe(data,"a",0,"in",61000));
  assertFalse(LauncherinGoals.observe(data,"a",1,"in",91000));
  assertTrue(LauncherinGoals.observe(data,"a",2,"in",121000));
 }
 @Test public void networkGapAndCompletedMatchesCannotProduceCatchupAlerts() throws Exception {
  JSONObject data=new JSONObject();LauncherinGoals.observe(data,"a",0,"in",1000);
  assertFalse(LauncherinGoals.observe(data,"a",2,"in",150000));
  assertTrue(LauncherinGoals.observe(data,"a",3,"post",180000));
  assertFalse(LauncherinGoals.observe(data,"a",4,"post",210000));
 }
 @Test public void missingScoresAndOtherTeamsDoNotCorruptABaseline() throws Exception {
  JSONObject data=new JSONObject();assertFalse(LauncherinGoals.observe(data,"a",-1,"in",1000));assertEquals(0,data.length());
  LauncherinGoals.observe(data,"a",0,"in",2000);LauncherinGoals.observe(data,"b",3,"in",2000);
  assertTrue(LauncherinGoals.observe(data,"a",1,"in",32000));assertFalse(LauncherinGoals.observe(data,"b",3,"in",32000));
  assertEquals(-1,LauncherinGoals.score(""));assertEquals(-1,LauncherinGoals.score("null"));assertEquals(0,LauncherinGoals.score("0"));
 }
 @Test public void savedBaselinesSurviveSerializationAndStayBounded() throws Exception {
  JSONObject data=new JSONObject();LauncherinGoals.observe(data,"first",0,"in",1000);
  JSONObject reopened=new JSONObject(data.toString());assertTrue(LauncherinGoals.observe(reopened,"first",1,"in",31000));
  for(int i=0;i<100;i++)LauncherinGoals.observe(reopened,"match"+i,0,"in",32000+i);
  assertEquals(64,reopened.length());assertTrue(reopened.has("match99"));assertFalse(reopened.has("first"));
 }
}
