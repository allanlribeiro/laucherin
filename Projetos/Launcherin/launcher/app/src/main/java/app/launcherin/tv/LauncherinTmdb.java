package app.launcherin.tv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;

/** Optional artwork enrichment. Playback intents and the streaming history are never rewritten. */
final class LauncherinTmdb implements AutoCloseable {
    static final String KEY="tmdb_api_key", ENABLED="tmdb_artwork";
    static final String CREDIT="This product uses the TMDB API but is not endorsed or certified by TMDB.";
    static final class Settings {
        final String key; final boolean enabled;
        Settings(JSONObject config) { key=cleanKey(config.optString(KEY)); enabled=config.optBoolean(ENABLED,true)&&!key.isEmpty(); }
        String signature(){return enabled+":"+digest(key);}
    }
    static Settings settings(Context context){
        JSONObject config=LauncherinExtras.read(context);android.content.SharedPreferences saved=context.getSharedPreferences("tmdb-options",0);
        String key=config.has(KEY)?cleanKey(config.optString(KEY)):saved.getString(KEY,"");
        boolean enabled=config.has(ENABLED)?config.optBoolean(ENABLED,true):saved.getBoolean(ENABLED,true);
        if(!key.equals(saved.getString(KEY,""))||enabled!=saved.getBoolean(ENABLED,true))saved.edit().putString(KEY,key).putBoolean(ENABLED,enabled).apply();
        try{return new Settings(new JSONObject().put(KEY,key).put(ENABLED,enabled));}catch(JSONException impossible){throw new IllegalStateException(impossible);}
    }
    static String cleanKey(String value){return value!=null&&value.trim().matches("[a-fA-F0-9]{32}")?value.trim().toLowerCase(Locale.ROOT):"";}
    static final class Art {
        final String backdrop,logo,provider;
        Art(String backdrop,String logo){this(backdrop,logo,"");}
        Art(String backdrop,String logo,String provider){this.backdrop=backdrop;this.logo=logo;this.provider=provider;}
    }
    interface Ready {void accept(Art art);}
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Map<String,List<Ready>> waiting=new HashMap<>();
    private final Map<String,Art> memory=new LinkedHashMap<>();
    private final Map<String,Long> loaded=new HashMap<>();
    private boolean closed;
    LauncherinTmdb(Context context){this.context=context.getApplicationContext();}
    void request(JSONObject item,Settings settings,Ready callback){
        if(closed||!settings.enabled)return;
        String override=context.getSharedPreferences("tmdb-matches",0).getString(itemId(item),"");
        String id=digest(itemId(item)+"|"+item.optString("provider")+"|"+override+"|"+settings.signature());
        if(memory.containsKey(id)&&System.currentTimeMillis()-loaded.get(id)<300000){callback.accept(memory.get(id));return;}
        if(waiting.containsKey(id)){List<Ready> callbacks=waiting.get(id);if(callbacks.size()>=12)callbacks.remove(0);callbacks.add(callback);return;}
        if(waiting.size()>=12)return;List<Ready> callbacks=new ArrayList<>();callbacks.add(callback);waiting.put(id,callbacks);
        worker.execute(()->{
            Art art;try{art=resolve(item,settings,id,override);}catch(Exception unavailable){art=new Art("","");}
            finally{trim(new File(context.getCacheDir(),"tmdb"),new File(context.getCacheDir(),"tmdb/"+id+".json"));}
            final Art result=art;main.post(()->{if(closed)return;List<Ready> ready=waiting.remove(id);memory.put(id,result);loaded.put(id,System.currentTimeMillis());
                while(memory.size()>24){String oldest=memory.keySet().iterator().next();memory.remove(oldest);loaded.remove(oldest);}if(ready!=null)for(Ready target:ready)target.accept(result);});
        });
    }
    private Art resolve(JSONObject item,Settings settings,String id,String override)throws Exception{
        File dir=new File(context.getCacheDir(),"tmdb");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException();
        File entry=new File(dir,id+".json");long now=System.currentTimeMillis();
        if(entry.isFile())try(InputStream in=new FileInputStream(entry)){
            JSONObject cached=new JSONObject(new String(LauncherinExtras.bytes(in,8192),StandardCharsets.UTF_8));
            if(cached.optLong("expires")>now){String bg=local(dir,cached.optString("backdrop")),logo=local(dir,cached.optString("logo")),provider=local(dir,cached.optString("provider"));
                if((cached.optString("backdrop").isEmpty()||!bg.isEmpty())&&(cached.optString("logo").isEmpty()||!logo.isEmpty())&&(cached.optString("provider").isEmpty()||!provider.isEmpty()))return new Art(bg,logo,provider);}
        }catch(Exception ignored){}
        JSONObject found;
        if(override.matches("(tv|movie)/[1-9][0-9]{0,9}")){
            found=json("/"+override,settings.key,"&language=pt-BR");found.put("media_type",override.substring(0,override.indexOf('/')));
        }else found=match(item,json("/search/multi",settings.key,"&language=pt-BR&query="+URLEncoder.encode(query(item),"UTF-8")).optJSONArray("results"));
        String backdrop="",logo="";
        if(found!=null){
            String path="/"+found.optString("media_type")+"/"+found.optLong("id")+"/images";
            JSONObject images=json(path,settings.key,"&include_image_language=pt,en,null,"+found.optString("original_language","en"));
            String bg=found.optString("backdrop_path"),mark=logoPath(images.optJSONArray("logos"),found.optString("original_language"));
            if(validPath(bg))backdrop=download(dir,id+"-bg",bg,"w780");
            if(validPath(mark))logo=download(dir,id+"-logo",mark,"w500");
        }
        String provider="";try{provider=providerLogo(dir,item.optString("provider"),settings);}catch(Exception unavailable){}
        JSONObject cached=new JSONObject().put("expires",now+(found==null?6*3600000L:7*86400000L)).put("backdrop",backdrop).put("logo",logo).put("provider",provider);
        android.util.AtomicFile file=new android.util.AtomicFile(entry);FileOutputStream out=file.startWrite();
        try{out.write(cached.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception error){file.failWrite(out);throw error;}
        trim(dir,entry);return new Art(local(dir,backdrop),local(dir,logo),local(dir,provider));
    }
    static int network(String provider){switch(provider){case "Netflix":return 213;case "Prime Video":return 1024;case "Disney+":return 2739;case "HBO Max":return 3186;default:return 0;}}
    private static String providerLogo(File dir,String provider,Settings settings)throws Exception{
        int id=network(provider);if(id==0)return "";String name="provider-"+id;
        for(String suffix:new String[]{".png",".jpg",".jpeg"}){File file=new File(dir,name+suffix);if(file.isFile()&&System.currentTimeMillis()-file.lastModified()<7*86400000L)return file.getName();}
        String path=json("/network/"+id,settings.key,"").optString("logo_path");return validPath(path)?download(dir,name,path,"w154"):"";
    }
    private static String itemId(JSONObject item){return digest(query(item)+"|"+item.optString("year")+"|"+item.optString("media_type"));}
    void choose(android.app.Activity host,JSONObject item,Runnable changed){
        Settings settings=settings(context);
        if(!settings.enabled){android.widget.Toast.makeText(host,"Configure a chave TMDB em Imagens e logos dos conteúdos.",android.widget.Toast.LENGTH_LONG).show();return;}
        android.app.ProgressDialog progress=android.app.ProgressDialog.show(host,"Buscar imagem e logo","Consultando TMDB…",true,true);
        worker.execute(()->{List<JSONObject> choices=new ArrayList<>();
            try{JSONArray rows=json("/search/multi",settings.key,"&language=pt-BR&query="+URLEncoder.encode(query(item),"UTF-8")).optJSONArray("results");
                if(rows!=null)for(int i=0;i<rows.length()&&choices.size()<12;i++){JSONObject row=rows.optJSONObject(i);if(row!=null&&("tv".equals(row.optString("media_type"))||"movie".equals(row.optString("media_type")))&&row.optLong("id")>0)choices.add(row);}
            }catch(Exception unavailable){}
            main.post(()->{if(closed||host.isFinishing()||!progress.isShowing())return;progress.dismiss();
                if(choices.isEmpty()){android.widget.Toast.makeText(host,"Nenhum resultado disponível. Confira a chave e a conexão.",android.widget.Toast.LENGTH_LONG).show();return;}
                String[] labels=new String[choices.size()];for(int i=0;i<labels.length;i++){JSONObject row=choices.get(i);boolean tv="tv".equals(row.optString("media_type"));String date=row.optString(tv?"first_air_date":"release_date");labels[i]=row.optString(tv?"name":"title")+" · "+(tv?"Série":"Filme")+(date.length()>=4?" · "+date.substring(0,4):"");}
                new android.app.AlertDialog.Builder(host).setTitle("Imagem e logo de "+item.optString("title")).setItems(labels,(d,index)->{
                    JSONObject row=choices.get(index);context.getSharedPreferences("tmdb-matches",0).edit().putString(itemId(item),row.optString("media_type")+"/"+row.optLong("id")).apply();changed.run();
                }).setNeutralButton("Identificar automaticamente",(d,w)->{context.getSharedPreferences("tmdb-matches",0).edit().remove(itemId(item)).apply();changed.run();}).setNegativeButton("Voltar",null).show();
            });
        });
    }
    static String query(JSONObject item){return item.optString("title").replaceFirst("(?i)\\s*[-–:]?\\s*(?:S\\d+\\s*E\\d+|T\\d+\\s*E\\d+|Temporada \\d+.*)$","").trim();}
    static String normal(String value){return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]","");}
    /** No popularity guess between remakes, homonyms, movies and series with identical titles. */
    static JSONObject match(JSONObject item,JSONArray results){
        if(results==null)return null;String wanted=normal(query(item)),year=item.optString("year"),type=item.optString("media_type");if(wanted.isEmpty())return null;
        JSONObject chosen=null;Set<String> seen=new HashSet<>();
        for(int i=0;i<results.length();i++){
            JSONObject row=results.optJSONObject(i);if(row==null)continue;String media=row.optString("media_type");
            if(!("movie".equals(media)||"tv".equals(media))||(!type.isEmpty()&&!type.equals(media))||row.optLong("id")<=0)continue;
            String date=row.optString("movie".equals(media)?"release_date":"first_air_date");
            if(!year.isEmpty()&&!date.startsWith(year+"-"))continue;
            boolean same=false;for(String field:new String[]{"title","original_title","name","original_name"})if(wanted.equals(normal(row.optString(field))))same=true;
            if(!same||!seen.add(media+row.optLong("id")))continue;if(chosen!=null)return null;chosen=row;
        }return chosen;
    }
    static String logoPath(JSONArray logos,String original){
        if(logos==null)return "";String best="";double rank=-1;
        for(int i=0;i<logos.length();i++){JSONObject logo=logos.optJSONObject(i);if(logo==null||!validPath(logo.optString("file_path")))continue;
            String language=logo.optString("iso_639_1");int group="pt".equals(language)?5:(!original.isEmpty()&&original.equals(language))?4:"en".equals(language)?3:(language.isEmpty()||"null".equals(language))?2:0;
            if(group==0)continue;double score=group*100+Math.min(10,Math.max(0,logo.optDouble("vote_average",0)));
            if(score>rank){rank=score;best=logo.optString("file_path");}
        }return best;
    }
    static boolean validPath(String path){return path.matches("/[A-Za-z0-9_-]+\\.(png|jpg|jpeg)");}
    private static JSONObject json(String path,String key,String extra)throws Exception{
        return new JSONObject(new String(get("https://api.themoviedb.org/3"+path+"?api_key="+key+extra,1000000),StandardCharsets.UTF_8));
    }
    private static byte[] get(String url,int limit)throws IOException{
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(4000);connection.setReadTimeout(5000);connection.setInstanceFollowRedirects(false);
        try{if(connection.getResponseCode()!=200)throw new IOException("Artwork unavailable");try(InputStream in=connection.getInputStream()){return LauncherinExtras.bytes(in,limit);}}finally{connection.disconnect();}
    }
    private static String download(File dir,String name,String path,String size)throws IOException{
        String suffix=path.substring(path.lastIndexOf('.'));File file=new File(dir,name+suffix);
        byte[] data=get("https://image.tmdb.org/t/p/"+size+path,3000000);
        android.graphics.BitmapFactory.Options bounds=new android.graphics.BitmapFactory.Options();bounds.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeByteArray(data,0,data.length,bounds);
        if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>4096||bounds.outHeight>4096)throw new IOException("Artwork unavailable");
        android.util.AtomicFile target=new android.util.AtomicFile(file);FileOutputStream out=target.startWrite();
        try{out.write(data);target.finishWrite(out);}catch(IOException error){target.failWrite(out);throw error;}return file.getName();
    }
    private static String local(File dir,String name){if(!name.matches("([a-f0-9]{64}-(bg|logo)|provider-[1-9][0-9]{0,9})\\.(png|jpg|jpeg)"))return "";File file=new File(dir,name);return file.isFile()?file.getAbsolutePath():"";}
    private static void trim(File dir,File keep){
        File[] files=dir.listFiles();if(files==null)return;long bytes=0;for(File file:files)bytes+=file.length();
        Arrays.sort(files,Comparator.comparingLong(File::lastModified));String active=keep.getName().replace(".json","");
        for(File file:files){if(bytes<=18000000)break;if(file.getName().startsWith(active))continue;long size=file.length();if(file.delete())bytes-=size;}
        File[] entries=dir.listFiles((d,n)->n.endsWith(".json"));if(entries==null||entries.length<=30)return;
        Arrays.sort(entries,Comparator.comparingLong(File::lastModified));int remove=entries.length-30;
        for(File entry:entries){if(remove==0)break;if(entry.equals(keep))continue;String prefix=entry.getName().replace(".json","");
            File[] assets=dir.listFiles((d,n)->n.startsWith(prefix+"-"));if(assets!=null)for(File asset:assets)asset.delete();entry.delete();remove--;}
    }
    static String digest(String value){try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:hash)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}catch(Exception impossible){throw new IllegalStateException(impossible);}}
    @Override public void close(){closed=true;waiting.clear();memory.clear();loaded.clear();worker.shutdownNow();main.removeCallbacksAndMessages(null);}
}
