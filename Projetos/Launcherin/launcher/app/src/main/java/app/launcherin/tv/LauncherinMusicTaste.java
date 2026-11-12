// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import android.content.*;
import org.json.*;
import java.util.*;

/** Bounded local preferences. No account access, telemetry, model or background polling. */
final class LauncherinMusicTaste {
    static final int MAX=200;
    private static final String KEY="music_taste_v1";
    private final SharedPreferences prefs;
    private final LauncherinMusicLibrary library;
    static final class Entry {
        LauncherinTrack track;int completed,skips,feedback;long updated;
        Entry(LauncherinTrack t){track=t;}
    }
    LauncherinMusicTaste(Context c){prefs=c.getSharedPreferences(new LauncherinMusicProfiles(c).libraryName(),0);library=new LauncherinMusicLibrary(c);}
    private LinkedHashMap<String,Entry> entries(){
        LinkedHashMap<String,Entry> out=new LinkedHashMap<>();
        try{String raw=prefs.getString(KEY,"[]");if(raw.length()>2_000_000)return out;JSONArray all=new JSONArray(raw);for(int i=0;i<Math.min(MAX,all.length());i++){JSONObject j=all.optJSONObject(i);if(j==null)continue;LauncherinTrack t=LauncherinTrack.read(j.optJSONObject("track"));if(t==null)continue;Entry e=new Entry(t);e.completed=Math.max(0,Math.min(50,j.optInt("completed")));e.skips=Math.max(0,Math.min(50,j.optInt("skips")));e.feedback=Math.max(-1,Math.min(1,j.optInt("feedback")));e.updated=Math.max(0,j.optLong("updated"));out.put(t.url,e);}}catch(Exception ignored){}return out;
    }
    private void put(LinkedHashMap<String,Entry> all,Entry e){
        e.updated=System.currentTimeMillis();all.remove(e.track.url);all.put(e.track.url,e);
        // Retain explicit feedback before automatically learned entries when the bounded store fills.
        while(all.size()>MAX){String victim=null;for(Entry a:all.values())if(a.feedback==0){victim=a.track.url;break;}if(victim==null)victim=all.keySet().iterator().next();all.remove(victim);}
        JSONArray data=new JSONArray();for(Entry a:all.values())try{data.put(new JSONObject().put("track",a.track.json()).put("completed",a.completed).put("skips",a.skips).put("feedback",a.feedback).put("updated",a.updated));}catch(JSONException ignored){}prefs.edit().putString(KEY,data.toString()).apply();
    }
    void feedback(LauncherinTrack t,int value){LinkedHashMap<String,Entry> all=entries();Entry e=all.get(t.url);if(e==null)e=new Entry(t);e.track=t;e.feedback=Integer.compare(value,0);put(all,e);}
    int feedback(LauncherinTrack t){Entry e=entries().get(t.url);return e==null?0:e.feedback;}
    void listen(LauncherinTrack t,long audibleMs,boolean manuallySkipped){
        if(t==null||t.duration<=0)return;boolean completed=audibleMs>=t.duration*4/5;
        boolean quick=manuallySkipped&&audibleMs>=2000&&audibleMs<30000&&audibleMs<t.duration/5;
        if(!completed&&!quick)return;LinkedHashMap<String,Entry> all=entries();Entry e=all.get(t.url);if(e==null)e=new Entry(t);e.track=t;if(completed)e.completed=Math.min(50,e.completed+1);else e.skips=Math.min(50,e.skips+1);put(all,e);
    }
    int completed(LauncherinTrack t){Entry e=entries().get(t.url);return e==null?0:e.completed;}
    int skips(LauncherinTrack t){Entry e=entries().get(t.url);return e==null?0:e.skips;}
    List<LauncherinTrack> rated(){List<LauncherinTrack> out=new ArrayList<>();for(Entry e:entries().values())if(e.feedback!=0)out.add(e.track);Collections.reverse(out);return out;}
    int size(){return entries().size();}
    void reset(){prefs.edit().remove(KEY).remove("taste_candidates").remove("taste_cache_key").remove("taste_cache_time").apply();}
    static String artist(LauncherinTrack t){return source(t)+":"+LauncherinArtistPhotos.normalize(t.artist);}
    static String source(LauncherinTrack t){return "SoundCloud".equals(t.source)?"soundcloud":"youtube";}
    private Map<String,Double> artists(Map<String,Entry> all){
        Map<String,Double> scores=new HashMap<>();
        for(Entry e:all.values()){double value=Math.min(5,e.completed)*3-Math.min(3,e.skips)*4+(e.feedback==1?8:e.feedback==-1?-4:0);scores.merge(artist(e.track),value,Double::sum);}
        for(LauncherinTrack t:library.read("favorites"))scores.merge(artist(t),12d,Double::sum);
        for(LauncherinMusicLibrary.Artist a:library.artists())scores.merge(a.source+":"+LauncherinArtistPhotos.normalize(a.name),24d,Double::sum);
        scores.replaceAll((k,v)->Math.max(-18,Math.min(36,v)));return scores;
    }
    List<LauncherinTrack> rank(List<LauncherinTrack> candidates,String source,boolean discovery){
        Map<String,Entry> all=entries();Map<String,Double> artists=artists(all);Set<String> favorites=new HashSet<>(),heard=new HashSet<>();for(LauncherinTrack t:library.read("favorites"))favorites.add(t.url);for(LauncherinTrack t:library.read("recent"))heard.add(t.url);
        LinkedHashMap<String,LauncherinTrack> unique=new LinkedHashMap<>();for(LauncherinTrack t:candidates){if(unique.size()==300)break;Entry e=all.get(t.url);if(source(t).equals(source)&&(e==null||e.feedback!=-1))unique.putIfAbsent(t.url,t);}
        Map<String,Double> scores=new HashMap<>();for(LauncherinTrack t:unique.values()){Entry e=all.get(t.url);double v=artists.getOrDefault(artist(t),0d)+(favorites.contains(t.url)?35:0);if(e!=null)v+=Math.min(5,e.completed)*8-Math.min(3,e.skips)*15+(e.feedback==1?60:0);if(discovery&&!heard.contains(t.url)&&e==null&&!favorites.contains(t.url))v+=60;scores.put(t.url,v);}
        List<LauncherinTrack> pending=new ArrayList<>(unique.values());pending.sort((a,b)->Double.compare(scores.get(b.url),scores.get(a.url)));
        List<LauncherinTrack> out=new ArrayList<>();Map<String,Integer> counts=new HashMap<>();
        // Diversity is applied during selection, not by fabricating artist/genre metadata.
        while(!pending.isEmpty()&&out.size()<30){int at=0;for(int i=0;i<pending.size();i++)if(counts.getOrDefault(artist(pending.get(i)),0)<(out.size()<10?2:5)){at=i;break;}LauncherinTrack t=pending.remove(at);out.add(t);counts.merge(artist(t),1,Integer::sum);}
        return out;
    }
    List<LauncherinTrack> seeds(String source){List<LauncherinTrack> all=new ArrayList<>(library.read("favorites"));for(Entry e:entries().values())if(e.feedback==1||e.completed>0)all.add(e.track);all.addAll(library.read("recent"));return rank(all,source,false);}
    List<LauncherinMusicLibrary.Artist> seedArtists(String source){List<LauncherinMusicLibrary.Artist> out=new ArrayList<>();Set<String> seen=new HashSet<>();for(LauncherinTrack t:seeds(source))if(!t.artist.isEmpty()&&seen.add(artist(t)))out.add(new LauncherinMusicLibrary.Artist(t.artist,source,t.artistArt));for(LauncherinMusicLibrary.Artist a:library.artists())if(a.source.equals(source)&&seen.add(a.source+":"+LauncherinArtistPhotos.normalize(a.name)))out.add(a);return out;}
    String cacheKey(String source){StringBuilder key=new StringBuilder(source);List<LauncherinMusicLibrary.Artist> a=seedArtists(source);for(int i=0;i<Math.min(2,a.size());i++)key.append('|').append(a.get(i).key());return key.toString();}
    List<LauncherinTrack> cached(String key,long now){long at=prefs.getLong("taste_cache_time",0);return key.equals(prefs.getString("taste_cache_key",""))&&now>=at&&now-at<15*60*1000?library.read("taste_candidates"):Collections.emptyList();}
    void cache(String key,List<LauncherinTrack> items,long now){library.write("taste_candidates",items.subList(0,Math.min(60,items.size())));prefs.edit().putString("taste_cache_key",key).putLong("taste_cache_time",now).apply();}
}
