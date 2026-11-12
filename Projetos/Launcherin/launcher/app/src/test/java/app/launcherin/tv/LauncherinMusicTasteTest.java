package app.launcherin.tv;

import android.content.Context;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={34,35})
public class LauncherinMusicTasteTest {
    Context c;LauncherinMusicTaste taste;LauncherinMusicLibrary library;
    @Before public void setup(){c=RuntimeEnvironment.getApplication();taste=new LauncherinMusicTaste(c);library=new LauncherinMusicLibrary(c);}
    private LauncherinTrack t(int i,String artist){return new LauncherinTrack("https://www.youtube.com/watch?v="+String.format(java.util.Locale.ROOT,"%011d",i),"Faixa "+i,artist,"",180000,"youtube","");}
    @Test public void completionAndRepeatedListeningPromoteTracksAndArtists(){LauncherinTrack a=t(1,"A"),b=t(2,"B"),newA=t(3,"A");taste.listen(a,150000,false);taste.listen(a,180000,false);assertEquals(2,taste.completed(a));assertEquals(a.url,taste.rank(Arrays.asList(b,newA,a),"youtube",false).get(0).url);assertEquals(newA.url,taste.rank(Arrays.asList(b,newA),"youtube",false).get(0).url);}
    @Test public void onlyManualEarlySkipsCountAndFailureOrLongListeningIsNotDislike(){LauncherinTrack a=t(1,"A");taste.listen(a,10000,false);taste.listen(a,1000,true);taste.listen(a,60000,true);assertEquals(0,taste.skips(a));taste.listen(a,10000,true);assertEquals(1,taste.skips(a));taste.listen(a,150000,true);assertEquals(1,taste.completed(a));assertEquals(1,taste.skips(a));}
    @Test public void blockingExcludesAFavoriteOnlyFromRecommendationsAndCanBeUndone(){LauncherinTrack a=t(1,"A"),b=t(2,"B");library.toggle(a);taste.feedback(a,-1);assertEquals(b.url,taste.rank(Arrays.asList(a,b),"youtube",false).get(0).url);assertTrue(library.favorite(a));assertEquals(a.url,taste.rated().get(0).url);taste.feedback(a,0);assertEquals(a.url,taste.rank(Arrays.asList(b,a),"youtube",false).get(0).url);}
    @Test public void explicitFeedbackIsIdempotentAndOverridesSkipPreference(){LauncherinTrack a=t(1,"A"),b=t(2,"B");for(int i=0;i<4;i++)taste.listen(a,10000,true);taste.feedback(a,1);taste.feedback(a,1);assertEquals(1,taste.size());assertEquals(a.url,taste.rank(Arrays.asList(b,a),"youtube",false).get(0).url);}
    @Test public void profilesNeverShareLearningOrCandidateCache(){LauncherinTrack a=t(1,"A");taste.feedback(a,-1);taste.cache("youtube",Arrays.asList(a),100);new LauncherinMusicProfiles(c).select(new LauncherinMusicProfiles(c).list().get(1).id);LauncherinMusicTaste second=new LauncherinMusicTaste(c);assertEquals(0,second.feedback(a));assertTrue(second.cached("youtube",110).isEmpty());second.feedback(a,1);assertEquals(-1,taste.feedback(a));}
    @Test public void resetLeavesAllUserLibraryDataAndPreferencesUntouched(){LauncherinTrack a=t(1,"A");library.toggle(a);library.recent(a);library.write("queue",Arrays.asList(a));library.progress(0,12000);library.shuffle(true);new LauncherinMusicPlaylists(c).create("Minha playlist");taste.listen(a,180000,false);taste.feedback(a,-1);taste.reset();assertEquals(0,taste.size());assertTrue(library.favorite(a));assertEquals(1,library.read("recent").size());assertEquals(1,library.read("queue").size());assertEquals(12000,library.position());assertTrue(library.shuffle());assertEquals(1,new LauncherinMusicPlaylists(c).read().size());}
    @Test public void candidatesAreDeduplicatedSourceScopedAndArtistDiverse(){LauncherinTrack a=t(1,"A"),b=t(2,"A"),d=t(3,"A"),e=t(4,"B");List<LauncherinTrack> result=taste.rank(Arrays.asList(a,a,b,d,e),"youtube",false);assertEquals(4,result.size());assertEquals(e.url,result.get(2).url);assertTrue(taste.rank(result,"soundcloud",false).isEmpty());}
    @Test public void discoveryPrioritizesNewMusicAndFavoritesSeedWithoutMigration(){LauncherinTrack a=t(1,"A"),b=t(2,"B");library.recent(a);library.toggle(a);assertEquals(a.url,taste.seeds("youtube").get(0).url);assertEquals(b.url,taste.rank(Arrays.asList(a,b),"youtube",true).get(0).url);}
    @Test public void cacheExpiresAndBackwardClockCannotKeepOldRecommendations(){LauncherinTrack a=t(1,"A");taste.cache("key",Arrays.asList(a),1000);assertEquals(1,taste.cached("key",1001).size());assertTrue(taste.cached("other",1001).isEmpty());assertTrue(taste.cached("key",999).isEmpty());assertTrue(taste.cached("key",901000).isEmpty());}
    @Test public void boundedHistoryRetainsExplicitFeedbackBeforeAutomaticSignals(){LauncherinTrack a=t(1,"A");taste.feedback(a,-1);for(int i=2;i<210;i++)taste.listen(t(i,"B"),180000,false);assertEquals(LauncherinMusicTaste.MAX,taste.size());assertEquals(-1,taste.feedback(a));}
    @Test public void corruptAndOversizedPreferenceDataFallsBackToEmpty(){c.getSharedPreferences("launcherin-player",0).edit().putString("music_taste_v1","bad json").commit();assertEquals(0,taste.size());assertEquals(1,taste.rank(Arrays.asList(t(1,"A")),"youtube",false).size());}
    @Test public void pausedBufferingAndSeeksNeverBecomeAudibleListening(){LauncherinListeningSession s=new LauncherinListeningSession();LauncherinTrack a=t(1,"A");s.sample(a,true,0,0);s.sample(a,true,10000,10000);assertEquals(10000,s.heard());s.seek(150000,10000);s.sample(a,true,151000,11000);s.sample(a,false,152000,12000);s.sample(a,false,152000,100000);s.sample(a,true,152000,200000);s.sample(a,true,153000,201000);assertEquals(13000,s.heard());s.clear();assertNull(s.track);assertEquals(0,s.heard());}
    @Test public void unannouncedPositionJumpIsCappedByElapsedTime(){LauncherinListeningSession s=new LauncherinListeningSession();s.sample(t(1,"A"),true,0,0);s.sample(t(1,"A"),true,160000,1000);assertTrue(s.heard()<=1250);}
}
