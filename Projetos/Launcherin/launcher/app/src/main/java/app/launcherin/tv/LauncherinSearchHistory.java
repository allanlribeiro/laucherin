// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;
import android.content.*;import java.text.Normalizer;import java.util.*;import org.json.*;

/** Last explicit searches in this local listening profile. No network requests and no playback signals. */
final class LauncherinSearchHistory {
 static final int MAX=12;private static final String KEY="music_search_history_v1";private final SharedPreferences prefs;
 static final class Item {final String query,kind;Item(String query,String kind){this.query=query.trim().replaceAll("\\s+"," ");if(this.query.isEmpty()||this.query.length()>160)throw new IllegalArgumentException("Pesquisa inválida");this.kind=Arrays.asList("songs","artists","albums","playlists").contains(kind)?kind:"songs";}String key(){return Normalizer.normalize(query,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);}}
 LauncherinSearchHistory(Context c){prefs=c.getSharedPreferences(new LauncherinMusicProfiles(c).libraryName(),0);}
 List<Item> read(){List<Item> all=new ArrayList<>();Set<String> seen=new HashSet<>();try{String raw=prefs.getString(KEY,"[]");if(raw.length()>16384)return all;JSONArray rows=new JSONArray(raw);for(int i=0;i<rows.length()&&all.size()<MAX;i++)try{JSONObject j=rows.getJSONObject(i);Item row=new Item(j.getString("query"),j.optString("kind"));if(seen.add(row.key()))all.add(row);}catch(Exception ignored){}}catch(Exception ignored){}return all;}
 void record(String query,String kind){try{Item item=new Item(query,kind);List<Item> all=read();all.removeIf(a->a.key().equals(item.key()));all.add(0,item);write(all);}catch(IllegalArgumentException ignored){}}
 void remove(Item item){List<Item> all=read();all.removeIf(a->a.key().equals(item.key()));write(all);}
 void clear(){prefs.edit().remove(KEY).apply();}
 private void write(List<Item> all){JSONArray data=new JSONArray();for(int i=0;i<Math.min(MAX,all.size());i++)try{data.put(new JSONObject().put("query",all.get(i).query).put("kind",all.get(i).kind));}catch(JSONException ignored){}prefs.edit().putString(KEY,data.toString()).apply();}
}
