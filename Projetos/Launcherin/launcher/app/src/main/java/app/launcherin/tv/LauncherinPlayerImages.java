// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded disk cache, sampled decoding, no animation timer or retained bitmap on the Home. */
final class LauncherinPlayerImages {
    static Bitmap load(Context c,String source,int width,int height)throws Exception {
        if(source==null||source.isEmpty())return null;Exception last=null;
        for(String candidate:LauncherinArtworkQuality.candidates(source,width,height)){
            if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Imagem cancelada");
            try{Bitmap bitmap=loadVariant(c,candidate,width,height,!candidate.equals(source));if(bitmap==null)continue;
                return bitmap;
            }catch(Exception error){last=error;}
        }if(last!=null)throw last;return null;
    }
    private static Bitmap loadVariant(Context c,String source,int width,int height,boolean upgraded)throws Exception {
        if(source==null||source.isEmpty())return null;File file;
        if(source.startsWith("https://")){
            File dir=new File(c.getCacheDir(),"player-art");if(!dir.exists()&&!dir.mkdirs())return null;
            file=new File(dir,LauncherinMusicLibrary.hash(("art-v2:"+source).getBytes(StandardCharsets.UTF_8))+".img");
            if(!file.isFile()){
                HttpURLConnection con=(HttpURLConnection)new URL(source).openConnection();con.setConnectTimeout(8000);con.setReadTimeout(8000);
                try(InputStream in=con.getInputStream()){byte[] bytes=LauncherinExtras.bytes(in,2000000);File tmp=new File(dir,file.getName()+".part");try(OutputStream out=new FileOutputStream(tmp)){out.write(bytes);}if(!tmp.renameTo(file))tmp.delete();}finally{con.disconnect();}
                File[] old=dir.listFiles(f->f.getName().endsWith(".img"));if(old!=null){Arrays.sort(old,Comparator.comparingLong(File::lastModified));long size=0;for(File f:old)size+=f.length();for(File f:old)if(size>8_000_000&&!f.equals(file)){size-=f.length();f.delete();}}
            }file.setLastModified(System.currentTimeMillis());
        }else {
            file=new File(source);String path=file.getCanonicalPath();File config=Config.dir(c);if(!path.startsWith(c.getFilesDir().getCanonicalPath()+"/")&&(config==null||!path.startsWith(config.getCanonicalPath()+"/")))return null;
        }
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),o);if(o.outWidth<=0||o.outHeight<=0||o.outWidth>16384||o.outHeight>16384){if(source.startsWith("https://"))file.delete();return null;}
        if(upgraded&&(o.outWidth<160||o.outHeight<90)||source.matches("https://(?:i\\.ytimg\\.com|img\\.youtube\\.com)/vi(?:_webp)?/[A-Za-z0-9_-]{11}/maxresdefault\\.(?:jpg|webp)")&&(o.outWidth<320||o.outHeight<180))return null;
        o.inSampleSize=LauncherinArtworkQuality.sample(o.outWidth,o.outHeight,width,height);while((long)(o.outWidth/o.inSampleSize)*(o.outHeight/o.inSampleSize)>1_048_576)o.inSampleSize*=2;o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeFile(file.getPath(),o);
    }
    static List<String> photos(Context c){List<String> out=new ArrayList<>();org.json.JSONArray ids=LauncherinExtras.read(c).optJSONArray("frame_photos");File dir=Config.dir(c);if(dir==null)dir=c.getFilesDir();if(ids!=null)for(int i=0;i<Math.min(10,ids.length());i++){String id=ids.optString(i);if(id.matches("[a-f0-9]{64}")){File f=new File(dir,"frame-"+id.substring(0,40)+".jpg");if(f.isFile())out.add(f.getPath());}}return out;}
}
