// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import android.content.*;
import org.json.*;
import java.util.*;

/** Private bounded library, independent of theme/config.json and of the external apps' history. */
final class LauncherinMusicLibrary {
    static final int MAX=100;
    private final SharedPreferences prefs;
    LauncherinMusicLibrary(Context c){prefs=c.getSharedPreferences("launcherin-player",0);}
    List<LauncherinTrack> read(String key){List<LauncherinTrack> out=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString(key,"[]"));for(int i=0;i<a.length()&&out.size()<MAX;i++){LauncherinTrack t=LauncherinTrack.read(a.optJSONObject(i));if(t!=null)out.add(t);}}catch(Exception ignored){}return out;}
    void write(String key,List<LauncherinTrack> tracks){JSONArray a=new JSONArray();for(LauncherinTrack t:tracks){if(a.length()==MAX)break;a.put(t.json());}prefs.edit().putString(key,a.toString()).apply();}
    int index(List<LauncherinTrack> queue){return queue.isEmpty()?-1:Math.max(0,Math.min(queue.size()-1,prefs.getInt("index",0)));}
    long position(){return Math.max(0,prefs.getLong("position",0));}
    void progress(int index,long position){prefs.edit().putInt("index",index).putLong("position",Math.max(0,position)).apply();}
    boolean nativeSelected(){return prefs.getBoolean("native",false);}
    String error(){return prefs.getString("error","");}
    void error(String value){prefs.edit().putString("error",value).apply();}
    void select(boolean nativePlayer){prefs.edit().putBoolean("native",nativePlayer).apply();}
    boolean favorite(LauncherinTrack t){for(LauncherinTrack f:read("favorites"))if(f.url.equals(t.url))return true;return false;}
    boolean toggle(LauncherinTrack t){List<LauncherinTrack> list=read("favorites");boolean exists=list.removeIf(f->f.url.equals(t.url));if(!exists){if(list.size()==MAX)list.remove(list.size()-1);list.add(0,t);}write("favorites",list);return !exists;}
    String artMode(){return prefs.getString("art_mode","album");}
    void artMode(String mode){prefs.edit().putString("art_mode","photos".equals(mode)?"photos":"album").apply();}
    static final class Artist {
        final String name,source,art;
        Artist(String name,String source,String art){this.name=LauncherinTrack.limit(name,120);if(this.name.isEmpty()||this.name.matches(".*[\\r\\n\\t].*"))throw new IllegalArgumentException("Informe o artista ou a banda.");this.source="soundcloud".equalsIgnoreCase(source)?"soundcloud":"youtube";this.art=art!=null&&art.startsWith("https://")?LauncherinTrack.limit(art,2000):"";}
        String key(){return java.text.Normalizer.normalize(name,java.text.Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).trim();}
        JSONObject json(){JSONObject j=new JSONObject();try{j.put("name",name).put("source",source).put("art",art);}catch(JSONException ignored){}return j;}
    }
    List<Artist> artists(){List<Artist> out=new ArrayList<>();Set<String> keys=new HashSet<>();try{JSONArray a=new JSONArray(prefs.getString("artists","[]"));for(int i=0;i<a.length()&&out.size()<50;i++)try{JSONObject j=a.getJSONObject(i);Artist artist=new Artist(j.getString("name"),j.optString("source"),j.optString("art"));if(keys.add(artist.key()))out.add(artist);}catch(Exception ignored){}}catch(Exception ignored){}return out;}
    void saveArtist(Artist artist){List<Artist> a=artists();a.removeIf(x->x.key().equals(artist.key()));a.add(0,artist);JSONArray j=new JSONArray();for(int i=0;i<Math.min(50,a.size());i++)j.put(a.get(i).json());prefs.edit().putString("artists",j.toString()).apply();}
    void removeArtist(Artist artist){List<Artist> a=artists();a.removeIf(x->x.key().equals(artist.key()));JSONArray j=new JSONArray();for(Artist x:a)j.put(x.json());prefs.edit().putString("artists",j.toString()).apply();}
    String source(){return prefs.getString("source","youtube");}
    void source(String source){prefs.edit().putString("source","soundcloud".equals(source)?"soundcloud":"youtube").apply();}
    String saverMode(){return LauncherinAudioSaverPolicy.mode(prefs.getString("saver_mode","photos".equals(artMode())?"photos":"cover"));}
    void saverMode(String mode){prefs.edit().putString("saver_mode",LauncherinAudioSaverPolicy.mode(mode)).apply();}
    int saverSeconds(){return LauncherinAudioSaverPolicy.seconds(prefs.getInt("saver_seconds",60));}
    void saverSeconds(int seconds){prefs.edit().putInt("saver_seconds",LauncherinAudioSaverPolicy.seconds(seconds)).apply();}
    static String hash(byte[] bytes)throws Exception{StringBuilder s=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
}
