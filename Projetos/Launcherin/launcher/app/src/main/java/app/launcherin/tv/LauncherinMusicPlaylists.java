// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;
import android.content.*;import org.json.*;import java.util.*;
/** Bounded local playlists in the selected listening profile; only stable validated track URLs. */
final class LauncherinMusicPlaylists {
 static final class Playlist {final String id,name;final List<LauncherinTrack> tracks;Playlist(String id,String name,List<LauncherinTrack> tracks){this.id=id;this.name=LauncherinTrack.limit(name,80);if(this.name.isEmpty())throw new IllegalArgumentException("Informe o nome");this.tracks=new ArrayList<>(tracks.subList(0,Math.min(100,tracks.size())));}}
 final android.content.SharedPreferences prefs;
 LauncherinMusicPlaylists(Context c){prefs=c.getSharedPreferences(new LauncherinMusicProfiles(c).libraryName(),0);}
 List<Playlist> read(){List<Playlist> all=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("playlists","[]"));for(int i=0;i<a.length()&&all.size()<40;i++){JSONObject j=a.getJSONObject(i);List<LauncherinTrack> tracks=new ArrayList<>();JSONArray items=j.getJSONArray("tracks");for(int t=0;t<items.length()&&tracks.size()<100;t++){LauncherinTrack track=LauncherinTrack.read(items.optJSONObject(t));if(track!=null)tracks.add(track);}all.add(new Playlist(j.getString("id"),j.getString("name"),tracks));}}catch(Exception ignored){}return all;}
 Playlist create(String name){List<Playlist> all=read();if(all.size()>=40)throw new IllegalStateException("Até 40 playlists");Playlist p=new Playlist(UUID.randomUUID().toString(),name,Collections.emptyList());all.add(p);save(all);return p;}
 void add(String id,LauncherinTrack track){List<Playlist> all=read();for(Playlist p:all)if(p.id.equals(id)){if(p.tracks.stream().anyMatch(t->t.url.equals(track.url)))return;if(p.tracks.size()>=100)throw new IllegalStateException("Até 100 músicas");p.tracks.add(track);save(all);return;}}
 void remove(String id){List<Playlist> all=read();all.removeIf(p->p.id.equals(id));save(all);}
 void removeTrack(String id,String url){List<Playlist> all=read();for(Playlist p:all)if(p.id.equals(id)){p.tracks.removeIf(t->t.url.equals(url));save(all);return;}}
 private void save(List<Playlist> all){JSONArray a=new JSONArray();try{for(Playlist p:all){JSONArray tracks=new JSONArray();for(LauncherinTrack t:p.tracks)tracks.put(t.json());a.put(new JSONObject().put("id",p.id).put("name",p.name).put("tracks",tracks));}}catch(JSONException impossible){throw new IllegalStateException(impossible);}prefs.edit().putString("playlists",a.toString()).apply();}
}
