// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;
import android.content.*;import android.os.*;import android.util.AtomicFile;import org.json.*;import java.io.*;import java.util.*;
/** Visual-only preferences, independently and atomically synchronized; never changes Home config. */
final class LauncherinVisualOptions {
 static final String CHANGED="app.launcherin.tv.MUSIC_VISUAL_CHANGED",PENDING="music-visual.pending.json";
 static final Set<String> KEYS=new HashSet<>(Arrays.asList("enabled","palette","intensity","motion","dim_minutes","hide_minutes","minimal_minutes","dark","ambient","smart"));
 final boolean enabled,motion,dark,ambient,smart;final String palette;final int intensity,dim,hide,minimal;
 LauncherinVisualOptions(JSONObject j)throws JSONException{
  for(Iterator<String> it=j.keys();it.hasNext();)if(!KEYS.contains(it.next()))throw new JSONException("Opção desconhecida");
  enabled=bool(j,"enabled",false);motion=bool(j,"motion",true);dark=bool(j,"dark",false);ambient=bool(j,"ambient",false);smart=bool(j,"smart",true);palette=j.optString("palette","electric");intensity=integer(j,"intensity",65);dim=integer(j,"dim_minutes",5);hide=integer(j,"hide_minutes",15);minimal=integer(j,"minimal_minutes",30);
  if(!Arrays.asList("electric","cyan","violet","blue").contains(palette)||intensity<0||intensity>100||dim<1||hide<=dim||minimal<=hide||minimal>240)throw new JSONException("Cores, intensidade ou tempos inválidos");
 }
 private static boolean bool(JSONObject j,String k,boolean d)throws JSONException{if(!j.has(k))return d;Object v=j.get(k);if(!(v instanceof Boolean))throw new JSONException("Valor inválido");return (Boolean)v;}
 private static int integer(JSONObject j,String k,int d)throws JSONException{if(!j.has(k))return d;Object v=j.get(k);if(!(v instanceof Integer))throw new JSONException("Tempo inválido");return (Integer)v;}
 JSONObject json(){JSONObject j=new JSONObject();try{j.put("enabled",enabled).put("palette",palette).put("intensity",intensity).put("motion",motion).put("dim_minutes",dim).put("hide_minutes",hide).put("minimal_minutes",minimal).put("dark",dark).put("ambient",ambient).put("smart",smart);}catch(JSONException ignored){}return j;}
 static File file(Context c){String id=new LauncherinMusicProfiles(c).active().id;return new File(c.getFilesDir(),"default".equals(id)?"music-visual.json":"music-visual-"+id+".json");}
 static File pending(Context c){return new File(c.getFilesDir(),PENDING);}
 static LauncherinVisualOptions read(Context c){try(InputStream in=new AtomicFile(file(c)).openRead()){return new LauncherinVisualOptions(new JSONObject(new String(LauncherinExtras.bytes(in,8192),java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){try{return new LauncherinVisualOptions(new JSONObject());}catch(JSONException impossible){throw new IllegalStateException(impossible);}}}
 static synchronized void save(Context c,LauncherinVisualOptions o)throws IOException{AtomicFile a=new AtomicFile(file(c));FileOutputStream out=a.startWrite();try{out.write(o.json().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));a.finishWrite(out);}catch(Exception e){a.failWrite(out);throw new IOException(e);}c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));}
 static Bundle call(Context c,String method){Bundle b=new Bundle();try{if("commitMusicVisual".equals(method)){try(InputStream in=new FileInputStream(pending(c))){JSONObject data=new JSONObject(new String(LauncherinExtras.bytes(in,8192),java.nio.charset.StandardCharsets.UTF_8));if(data.length()!=2||!data.getString("profile").equals(new LauncherinMusicProfiles(c).active().id))throw new JSONException("O perfil ativo mudou");save(c,new LauncherinVisualOptions(data.getJSONObject("options")));}pending(c).delete();}LauncherinMusicProfiles.Profile p=new LauncherinMusicProfiles(c).active();b.putBoolean("ok",true);b.putInt("schema",1);b.putString("json",new JSONObject().put("profile",p.id).put("profile_name",p.name).put("options",read(c).json()).toString());}catch(Exception e){b.putBoolean("ok",false);b.putString("error","As opções anteriores foram mantidas. Confira o perfil ativo.");}return b;}
 int stage(long idle){return idle>=minimal*60000L?3:idle>=hide*60000L?2:idle>=dim*60000L?1:0;}
}
