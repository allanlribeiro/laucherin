package app.launcherin.tv;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LauncherinTmdbTest {
 @Test public void localizedAndOriginalNamesAreMatchedWithoutChangingThePlaybackIntent() throws Exception {
  JSONObject item=new JSONObject().put("title","La Casa de Papel").put("intent_uri","streaming://keep-original");
  JSONArray rows=new JSONArray("[{media_type:'tv',id:1,name:'A Casa de Papel',original_name:'La casa de papel'}]");
  assertEquals(1,LauncherinTmdb.match(item,rows).optInt("id"));
  assertEquals("streaming://keep-original",item.optString("intent_uri"));
 }
 @Test public void homonymousRemakesNeedAYearRatherThanAPopularityGuess() throws Exception {
  JSONArray rows=new JSONArray("[{media_type:'movie',id:1,title:'Duna',release_date:'1984-01-01'},{media_type:'movie',id:2,title:'Duna',release_date:'2021-01-01'}]");
  JSONObject item=new JSONObject().put("title","Duna");assertNull(LauncherinTmdb.match(item,rows));
  item.put("year","2021");assertEquals(2,LauncherinTmdb.match(item,rows).optInt("id"));
 }
 @Test public void similarlyNamedMoviesAndPeopleDoNotReplaceTheCardArtwork() throws Exception {
  JSONObject item=new JSONObject().put("title","Friends");
  assertNull(LauncherinTmdb.match(item,new JSONArray("[{media_type:'movie',id:3,title:'Friends Forever'},{media_type:'person',id:4,name:'Friends'}]")));
 }
 @Test public void portugueseLogoWinsOverHigherRatedForeignLogos() throws Exception {
  JSONArray logos=new JSONArray("[{file_path:'/en.png',iso_639_1:'en',vote_average:10},{file_path:'/pt.png',iso_639_1:'pt',vote_average:1},{file_path:'/de.png',iso_639_1:'de',vote_average:10}]");
  assertEquals("/pt.png",LauncherinTmdb.logoPath(logos,"en"));
 }
 @Test public void missingOrUnsupportedLogosLeaveTextAsFallback() throws Exception {
  assertEquals("",LauncherinTmdb.logoPath(null,"en"));
  assertEquals("",LauncherinTmdb.logoPath(new JSONArray("[{file_path:'https://other.example/logo.png',iso_639_1:'pt'}]"),"en"));
  assertFalse(LauncherinTmdb.validPath("/../private.png"));
 }
 @Test public void episodeSuffixIsNotMistakenForPartOfTheSeriesName() throws Exception {
  assertEquals("Modern Family",LauncherinTmdb.query(new JSONObject().put("title","Modern Family - T2 E3")));
 }
 @Test public void invalidOrDisabledCredentialsDoNotEnableRequests() throws Exception {
  assertFalse(new LauncherinTmdb.Settings(new JSONObject()).enabled);
  assertFalse(new LauncherinTmdb.Settings(new JSONObject().put("tmdb_api_key","not-a-key")).enabled);
  assertFalse(new LauncherinTmdb.Settings(new JSONObject().put("tmdb_api_key","00000000000000000000000000000000").put("tmdb_artwork",false)).enabled);
 }
}
