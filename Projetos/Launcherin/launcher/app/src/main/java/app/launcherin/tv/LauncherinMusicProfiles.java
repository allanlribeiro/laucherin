// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;
import android.content.*;import org.json.*;import java.util.*;
/** Local listening identities, not accounts or credentials at a music service. */
final class LauncherinMusicProfiles {
 static final class Profile{final String id,name;final int color;Profile(String id,String name,int color){if(!id.matches("default|[a-f0-9]{32}"))throw new IllegalArgumentException("Perfil inválido");this.id=id;this.name=LauncherinTrack.limit(name,40);if(this.name.isEmpty())throw new IllegalArgumentException("Informe um nome");this.color=color;}}
 private final android.content.SharedPreferences prefs;
 LauncherinMusicProfiles(Context c){prefs=c.getSharedPreferences("music-profiles",0);}
 List<Profile> list(){List<Profile> out=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("profiles","[]"));for(int i=0;i<a.length()&&out.size()<8;i++){JSONObject j=a.getJSONObject(i);out.add(new Profile(j.getString("id"),j.getString("name"),j.optInt("color",0xff9770d7)));}}catch(Exception ignored){}if(out.isEmpty()){out.add(new Profile("default","Perfil 1",0xff9273c8));out.add(new Profile("22222222222222222222222222222222","Perfil 2",0xff588aa4));save(out);}return out;}
 Profile active(){String id=prefs.getString("active","default");for(Profile p:list())if(p.id.equals(id))return p;return list().get(0);}
 void select(String id){for(Profile p:list())if(p.id.equals(id)){prefs.edit().putString("active",id).commit();return;}throw new IllegalArgumentException("Perfil ausente");}
 void rename(String id,String name){List<Profile> all=list();for(int i=0;i<all.size();i++)if(all.get(i).id.equals(id)){Profile old=all.get(i);all.set(i,new Profile(id,name,old.color));save(all);return;}}
 void add(String name){List<Profile> all=list();if(all.size()>=8)throw new IllegalStateException("Até oito perfis");all.add(new Profile(UUID.randomUUID().toString().replace("-",""),name,0xff6b88ac));save(all);}
 String libraryName(){String id=active().id;return "default".equals(id)?"launcherin-player":"launcherin-player-profile-"+id;}
 private void save(List<Profile> profiles){JSONArray a=new JSONArray();try{for(Profile p:profiles)a.put(new JSONObject().put("id",p.id).put("name",p.name).put("color",p.color));}catch(JSONException impossible){throw new IllegalStateException(impossible);}prefs.edit().putString("profiles",a.toString()).apply();}
}
