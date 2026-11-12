// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import android.content.*;import android.os.Bundle;import android.graphics.*;import android.graphics.drawable.Drawable;import android.util.*;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import org.json.*;

/** Fixed paired-provider routes. JPEG validation and optimistic per-profile updates, never playback changes. */
final class LauncherinProfilePhotos {
 static final String PENDING="music-profile.pending.json",CHANGED="app.launcherin.tv.MUSIC_PROFILES_CHANGED";
 static final Object LOCK=new Object();static final int MAX_BYTES=180000;
 private static final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(512*1024){protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
 static boolean incoming(String name){return PENDING.equals(name)||name!=null&&name.matches("mp-[a-f0-9]{40}\\.jpg");}
 static File dir(Context c){File d=new File(c.getFilesDir(),"music-profile-photos");if(!d.isDirectory())d.mkdirs();return d;}
 static File incoming(Context c,String name)throws FileNotFoundException{if(!incoming(name))throw new FileNotFoundException("Arquivo de perfil recusado");File d=new File(dir(c),"incoming");if(!d.isDirectory())d.mkdirs();return new File(d,name);}
 static File photo(Context c,String hash){if(!hash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Foto inválida");return new File(dir(c),hash+".jpg");}
 static String revision(LauncherinMusicProfiles.Profile p){try{return LauncherinMusicLibrary.hash((p.id+"\u0000"+p.name+"\u0000"+p.color+"\u0000"+p.photo).getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
 static JSONObject json(LauncherinMusicProfiles.Profile p)throws JSONException{return new JSONObject().put("id",p.id).put("name",p.name).put("color",p.color).put("photo",p.photo).put("revision",revision(p));}
 static byte[] checked(File f,String hash)throws Exception{if(f.length()<4||f.length()>MAX_BYTES)throw new IOException("Foto muito grande");byte[] bytes;try(InputStream in=new FileInputStream(f)){bytes=LauncherinExtras.bytes(in,MAX_BYTES);}if(!LauncherinMusicLibrary.hash(bytes).equals(hash)||bytes[0]!=(byte)0xff||bytes[1]!=(byte)0xd8)throw new IOException("Foto sem integridade");BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(o.outWidth<1||o.outHeight<1||o.outWidth>512||o.outHeight>512||!"image/jpeg".equals(o.outMimeType))throw new IOException("Use uma foto JPEG de até 512 pixels");Bitmap b=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(b==null)throw new IOException("Foto inválida");b.recycle();return bytes;}
 static Bundle call(Context c,String method){return call(c,method,null);}
 static Bundle call(Context c,String method,String expectedHash){synchronized(LOCK){Bundle result=new Bundle();try{
  LauncherinMusicProfiles store=new LauncherinMusicProfiles(c);
  if("commitMusicProfile".equals(method)){
   JSONObject j;try(InputStream in=new FileInputStream(incoming(c,PENDING))){byte[] data=LauncherinExtras.bytes(in,8192);if(expectedHash==null||!expectedHash.matches("[a-f0-9]{64}")||!LauncherinMusicLibrary.hash(data).equals(expectedHash))throw new IOException("Os dados recebidos mudaram");j=new JSONObject(new String(data,StandardCharsets.UTF_8));}
   if(j.length()!=4||!j.has("id")||!j.has("name")||!j.has("photo")||!j.has("expected"))throw new JSONException("Dados do perfil inválidos");
   String id=j.getString("id"),hash=j.getString("photo"),name=j.getString("name"),expected=j.getString("expected");LauncherinMusicProfiles.Profile old=null;for(LauncherinMusicProfiles.Profile p:store.list())if(p.id.equals(id))old=p;
   if(old==null&&!expected.isEmpty()||old!=null&&!revision(old).equals(expected))throw new IOException("O perfil mudou. Atualize a lista.");
   LauncherinMusicProfiles.Profile wanted=new LauncherinMusicProfiles.Profile(id,name,old==null?0xff6b88ac:old.color,hash);
   if(old==null&&store.list().size()>=8)throw new IOException("Até oito perfis");
   if(!hash.isEmpty()){
    File target=photo(c,hash);byte[] bytes=checked(target.exists()?target:incoming(c,"mp-"+hash.substring(0,40)+".jpg"),hash);
    if(!target.exists()){AtomicFile atomic=new AtomicFile(target);FileOutputStream out=atomic.startWrite();try{out.write(bytes);atomic.finishWrite(out);}catch(Exception e){atomic.failWrite(out);throw e;}}
   }
   store.update(wanted);incoming(c,PENDING).delete();prune(c,store.list());c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));
  }
  JSONArray profiles=new JSONArray();for(LauncherinMusicProfiles.Profile p:store.list())profiles.put(json(p));result.putInt("schema",1);result.putString("json",new JSONObject().put("profiles",profiles).put("active",store.active().id).toString());result.putBoolean("ok",true);
 }catch(Exception e){result.putBoolean("ok",false);result.putString("error","O perfil anterior foi mantido. Atualize os perfis e confira a foto.");}return result;}}
 static void prune(Context c,List<LauncherinMusicProfiles.Profile> profiles){Set<String> keep=new HashSet<>();for(LauncherinMusicProfiles.Profile p:profiles)if(!p.photo.isEmpty())keep.add(p.photo+".jpg");File[] files=dir(c).listFiles();if(files!=null)for(File f:files)if(f.getName().matches("[a-f0-9]{64}\\.jpg")&&!keep.contains(f.getName()))f.delete();File[] staged=new File(dir(c),"incoming").listFiles();if(staged!=null)for(File f:staged)if(f.getName().matches("mp-[a-f0-9]{40}\\.jpg"))f.delete();}
 static Drawable avatar(Context c,LauncherinMusicProfiles.Profile profile,int size){if(profile.photo.isEmpty())return null;String key=profile.photo+":"+size;Bitmap image=cache.get(key);if(image==null)try{BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=1;while(512/(o.inSampleSize*2)>=size)o.inSampleSize*=2;image=BitmapFactory.decodeFile(photo(c,profile.photo).getPath(),o);if(image==null)return null;cache.put(key,image);}catch(Exception e){return null;}final Bitmap bitmap=image;return new Drawable(){final Paint paint=new Paint(3);final BitmapShader shader=new BitmapShader(bitmap,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);final Matrix matrix=new Matrix();public void draw(Canvas canvas){Rect b=getBounds();float scale=Math.max(b.width()/(float)bitmap.getWidth(),b.height()/(float)bitmap.getHeight());matrix.setScale(scale,scale);matrix.postTranslate(b.left+(b.width()-bitmap.getWidth()*scale)/2,b.top+(b.height()-bitmap.getHeight()*scale)/2);shader.setLocalMatrix(matrix);paint.setShader(shader);canvas.drawCircle(b.exactCenterX(),b.exactCenterY(),Math.min(b.width(),b.height())/2f,paint);}public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}};}
}
