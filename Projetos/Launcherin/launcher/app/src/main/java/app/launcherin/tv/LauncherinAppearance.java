package app.launcherin.tv;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Appearance-only state. Never writes the personal config, apps, navigation, accessibility or timers. */
final class LauncherinAppearance {
 interface Listener {void changed(boolean wallpaper);}
 private static java.lang.ref.WeakReference<Listener> listener=new java.lang.ref.WeakReference<>(null);
 private static final Handler main=new Handler(Looper.getMainLooper());
 private static volatile Style style;private static final Object WRITE_LOCK=new Object();
 private static JSONObject palettes;
 static final Set<String> KEYS=new HashSet<>(Arrays.asList("theme","family","apps","intensity","auto_palette","reduced","wallpaper_mode","original_sha","personal_sha","dynamic","rotation_minutes","dim"));
 static File dir(Context c){File f=new File(c.getFilesDir(),"appearance");if(!f.isDirectory())f.mkdirs();return f;}
 static boolean pending(String name){return "appearance.pending.json".equals(name)||name!=null&&name.matches("av-[a-f0-9]{40}\\.jpg");}
 static File incoming(Context c,String name)throws IOException{if(!pending(name))throw new IOException("Arquivo visual recusado");File d=Config.dir(c);return new File(d==null?dir(c):d,name);}
 static JSONObject read(File f,int max)throws Exception{try(InputStream in=new FileInputStream(f)){return new JSONObject(new String(LauncherinExtras.bytes(in,max),StandardCharsets.UTF_8));}}
 static JSONObject saved(Context c){try{return read(new File(dir(c),"options.json"),16384);}catch(Exception ignored){return new JSONObject();}}
 private static void write(File file,JSONObject data)throws IOException{AtomicFile a=new AtomicFile(file);FileOutputStream out=a.startWrite();try{out.write(data.toString().getBytes(StandardCharsets.UTF_8));a.finishWrite(out);}catch(Exception e){a.failWrite(out);throw new IOException(e);}}
 static synchronized JSONObject palettes(Context c)throws Exception{if(palettes==null)try(InputStream in=c.getAssets().open("appearance-palettes.json")){palettes=new JSONObject(new String(LauncherinExtras.bytes(in,32768),StandardCharsets.UTF_8));}return palettes;}
 static JSONObject theme(Context c,String id)throws Exception{JSONArray rows=palettes(c).getJSONArray("themes");for(int i=0;i<rows.length();i++)if(id.equals(rows.getJSONObject(i).getString("id")))return rows.getJSONObject(i);throw new IOException("Tema ausente desta versão");}
 static JSONObject validate(Context c,JSONObject old,JSONObject patch)throws Exception{
  JSONObject n=new JSONObject(old.toString());for(Iterator<String> it=patch.keys();it.hasNext();){String k=it.next();if(!KEYS.contains(k))throw new IOException("Opção visual desconhecida");Object v=patch.get(k);
   if(Arrays.asList("intensity","rotation_minutes","dim").contains(k)){if(!(v instanceof Integer))throw new IOException("Valor visual inválido");int x=(Integer)v,max="rotation_minutes".equals(k)?1440:"dim".equals(k)?90:100;if(x<0||x>max||"rotation_minutes".equals(k)&&x==0)throw new IOException("Valor fora do limite");}
   else if("auto_palette".equals(k)||"reduced".equals(k)){if(!(v instanceof Boolean))throw new IOException("Opção inválida");}
   else if("dynamic".equals(k)){if(!(v instanceof JSONArray)||((JSONArray)v).length()>10)throw new IOException("Coleção inválida");JSONArray a=(JSONArray)v;for(int i=0;i<a.length();i++)checkHash(a.getString(i));}
   else {if(!(v instanceof String))throw new IOException("Texto visual inválido");String s=(String)v;
    if("theme".equals(k))theme(c,s);else if("family".equals(k)&&!Arrays.asList("glass_premium","minimal_cinematic","ambient_dynamic").contains(s))throw new IOException("Estilo inválido");
    else if("apps".equals(k)&&!Arrays.asList("original","glass","minimal","premium").contains(s))throw new IOException("Acabamento inválido");
    else if("wallpaper_mode".equals(k)&&!Arrays.asList("existing","theme","personal","dynamic").contains(s))throw new IOException("Fundo inválido");
    else if(k.endsWith("_sha")&&!s.isEmpty())checkHash(s);
   }n.put(k,v);
  }
  return n;
 }
 private static void checkHash(String s)throws IOException{if(!s.matches("[a-f0-9]{64}"))throw new IOException("Imagem não verificada");}
 static File image(Context c,String hash)throws Exception{checkHash(hash);return new File(dir(c),"image-"+hash+".jpg");}
 private static void verifyImage(File input,String hash)throws Exception{
  if(!input.isFile()||input.length()<1||input.length()>2000000||!hash.equals(hash(input)))throw new IOException("Imagem corrompida");BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeFile(input.getPath(),b);if(b.outWidth<=0||b.outHeight<=0||b.outWidth>8192||b.outHeight>8192||(long)b.outWidth*b.outHeight>16777216)throw new IOException("Imagem incompatível");
  b.inSampleSize=1;while(b.outWidth/b.inSampleSize>64||b.outHeight/b.inSampleSize>64)b.inSampleSize*=2;b.inJustDecodeBounds=false;Bitmap test=BitmapFactory.decodeFile(input.getPath(),b);if(test==null)throw new IOException("Não foi possível decodificar a imagem");test.recycle();
 }
 private static void ensureImage(Context c,String hash)throws Exception{File stored=image(c,hash);if(stored.isFile()){verifyImage(stored,hash);return;}File input=incoming(c,"av-"+hash.substring(0,40)+".jpg");verifyImage(input,hash);
  File tmp=new File(dir(c),"image.tmp");try(InputStream in=new FileInputStream(input);OutputStream out=new FileOutputStream(tmp)){byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))!=-1;)out.write(buffer,0,n);}if(!tmp.renameTo(stored))throw new IOException("Não foi possível guardar a imagem");input.delete();
 }
 private static void keepImages(JSONObject o,Set<String> keep){for(String k:new String[]{"personal_sha","original_sha"}){String h=o.optString(k);if(h.matches("[a-f0-9]{64}"))keep.add("image-"+h+".jpg");}JSONArray a=o.optJSONArray("dynamic");if(a!=null)for(int i=0;i<a.length();i++){String h=a.optString(i);if(h.matches("[a-f0-9]{64}"))keep.add("image-"+h+".jpg");}}
 static void prune(Context c,JSONObject current,JSONObject previous){Set<String> keep=new HashSet<>();keepImages(current,keep);keepImages(previous,keep);File[] files=dir(c).listFiles();if(files!=null)for(File f:files)if(f.getName().matches("image-[a-f0-9]{64}\\.jpg")&&!keep.contains(f.getName()))f.delete();}
 static void listen(Listener l){listener=new java.lang.ref.WeakReference<>(l);}static void forget(Listener l){if(listener.get()==l)listener.clear();}
 private static void notifyChange(boolean wallpaper)throws Exception{Listener l=listener.get();if(l==null)return;if(Looper.myLooper()==Looper.getMainLooper()){l.changed(wallpaper);return;}CountDownLatch done=new CountDownLatch(1);Throwable[] failure=new Throwable[1];main.post(()->{try{l.changed(wallpaper);}catch(Throwable e){failure[0]=e;}finally{done.countDown();}});if(!done.await(10,TimeUnit.SECONDS)||failure[0]!=null)throw new IOException("Falha na atualização visual");}
 static Bundle call(Context c,String method){synchronized(WRITE_LOCK){return callLocked(c,method);}}
 static Bundle callLocked(Context c,String method){Bundle b=new Bundle();try{
  if("appearanceStatus".equals(method)){b.putBoolean("ok",true);b.putInt("version",26);b.putString("json",saved(c).toString());return b;}
  JSONObject old=saved(c),patch=read(incoming(c,"appearance.pending.json"),16384),next=validate(c,old,patch);
  String mode=next.optString("wallpaper_mode","existing");if("personal".equals(mode))ensureImage(c,next.getString("personal_sha"));if("theme".equals(mode)&&!next.optString("original_sha").isEmpty())ensureImage(c,next.getString("original_sha"));
  if("dynamic".equals(mode)){JSONArray a=next.optJSONArray("dynamic");if(a==null||a.length()==0)throw new IOException("Escolha imagens para o fundo dinâmico");for(int i=0;i<a.length();i++)ensureImage(c,a.getString(i));}
  boolean background=!backgroundKey(old).equals(backgroundKey(next))||!old.optBoolean("auto_palette")&&next.optBoolean("auto_palette");write(new File(dir(c),"previous.json"),old);write(new File(dir(c),"options.json"),next);invalidate();
  try{notifyChange(background);}catch(Exception failure){write(new File(dir(c),"options.json"),old);invalidate();try{notifyChange(background);}catch(Exception ignored){}throw new IOException("A aparência anterior foi restaurada",failure);}
  prune(c,next,old);b.putBoolean("ok",true);b.putString("json",next.toString());
 }catch(Exception e){b.putBoolean("ok",false);b.putString("error",e.getMessage());}return b;}
 static String backgroundKey(JSONObject o){String mode=o.optString("wallpaper_mode","existing");if("existing".equals(mode))return mode;String source="personal".equals(mode)?o.optString("personal_sha"):"theme".equals(mode)?o.optString("original_sha"):String.valueOf(o.optJSONArray("dynamic"));return mode+":"+source+":"+o.optInt("rotation_minutes",15)+":"+o.optInt("dim",10);}

 static void invalidate(){style=null;}
 static Style get(Context c){Style s=style;if(s==null)synchronized(LauncherinAppearance.class){s=style;if(s==null)style=s=new Style(c,saved(c));}return s;}
 static final class Style {
  final String family,apps,theme;final int accent,surface,text=0xfff6f7fa,muted=0xffc6cbd2,intensity;final boolean reduced,auto,lightWallpaper;final Typeface font;
  Style(Context c,JSONObject o){family=o.optString("family","glass_premium");apps=o.optString("apps","premium");theme=o.optString("theme","launcherin");intensity=o.optInt("intensity",45);reduced=o.optBoolean("reduced",false);auto=o.optBoolean("auto_palette",false);JSONObject meta;try{meta=LauncherinAppearance.theme(c,theme);}catch(Exception e){meta=new JSONObject();}
   int a=color(meta.optString("accent"),0xffb7cbd8),s=color(meta.optString("surface"),0xff17232d);
   if(auto)try{JSONObject cache=read(new File(dir(c),"palette.json"),8192);a=cache.getInt("accent");s=cache.getInt("surface");}catch(Exception ignored){}
   boolean light=false;try{light=read(new File(dir(c),"palette.json"),8192).optDouble("luminance",0)>.5;}catch(Exception ignored){}lightWallpaper=light;accent=readableAccent(a);surface=s;font=c.getResources().getFont(R.font.outfit_regular);
  }
 }
 static int color(String s,int fallback){try{return Color.parseColor(s);}catch(Exception e){return fallback;}}
 static int blend(int a,int b,float t){return Color.rgb(Math.round(Color.red(a)*(1-t)+Color.red(b)*t),Math.round(Color.green(a)*(1-t)+Color.green(b)*t),Math.round(Color.blue(a)*(1-t)+Color.blue(b)*t));}
 static double luminance(int c){double[] a={Color.red(c)/255d,Color.green(c)/255d,Color.blue(c)/255d};for(int i=0;i<3;i++)a[i]=a[i]<=.04045?a[i]/12.92:Math.pow((a[i]+.055)/1.055,2.4);return .2126*a[0]+.7152*a[1]+.0722*a[2];}
 static int readableAccent(int c){while(luminance(c)<.35)c=blend(c,Color.WHITE,.18f);return c;}
 static void sample(Context c,Bitmap bitmap,String key){String wanted=backgroundKey(saved(c));try{File file=new File(dir(c),"palette.json");try{if(key.equals(read(file,8192).optString("key")))return;}catch(Exception ignored){}
  Bitmap small=Bitmap.createScaledBitmap(bitmap,64,36,true);int[] pixels=new int[64*36],counts=new int[64];long[] rs=new long[64],gs=new long[64],bs=new long[64];small.getPixels(pixels,0,64,0,0,64,36);
  for(int p:pixels){int bin=(Color.red(p)>>6)*16+(Color.green(p)>>6)*4+(Color.blue(p)>>6);counts[bin]++;rs[bin]+=Color.red(p);gs[bin]+=Color.green(p);bs[bin]+=Color.blue(p);}
  int best=0;for(int i=1;i<64;i++)if(counts[i]>counts[best])best=i;int n=Math.max(1,counts[best]),base=Color.rgb((int)(rs[best]/n),(int)(gs[best]/n),(int)(bs[best]/n));if(small!=bitmap)small.recycle();
  if(!wanted.equals(backgroundKey(saved(c))))return;write(file,new JSONObject().put("key",key).put("luminance",luminance(base)).put("accent",readableAccent(base)).put("surface",blend(base,0xff081019,.78f)));invalidate();main.post(()->{try{notifyChange(false);}catch(Exception ignored){}});
 }catch(Exception ignored){}}
 static String hash(File file)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte n:d.digest())s.append(String.format(Locale.ROOT,"%02x",n&255));return s.toString();}
}
