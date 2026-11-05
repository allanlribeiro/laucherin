package app.launcherin.tv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AtomicFile;
import android.util.LruCache;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/** Only the two selected clubs: public PNG badges, bounded decode and local cache. */
final class LauncherinTeamCrests {
    private static final LruCache<String,Bitmap> memory=new LruCache<>(4);
    private static File file(Context context,String id) {
        if(!id.matches("[0-9]{1,8}"))throw new IllegalArgumentException("Time inválido");
        return new File(new File(context.getCacheDir(),"sports-crests"),id+".png");
    }
    static long stamp(Context context,String id){return file(context,id).lastModified();}
    static Bitmap cached(Context context,String id) {
        Bitmap result=memory.get(id);if(result!=null)return result;File image=file(context,id);if(!image.isFile())return null;
        BitmapFactory.Options size=new BitmapFactory.Options();size.inJustDecodeBounds=true;BitmapFactory.decodeFile(image.getPath(),size);
        if(size.outWidth<1||size.outHeight<1||size.outWidth>2048||size.outHeight>2048||image.length()>524288)return null;
        size.inSampleSize=1;while(size.outWidth/size.inSampleSize>256||size.outHeight/size.inSampleSize>256)size.inSampleSize*=2;size.inJustDecodeBounds=false;
        result=BitmapFactory.decodeFile(image.getPath(),size);if(result!=null)memory.put(id,result);return result;
    }
    static boolean needsLoad(Context context,String id) {
        long now=System.currentTimeMillis();File image=file(context,id);
        if(image.isFile()&&now-image.lastModified()<30L*24*60*60*1000)return false;
        long retry=image.isFile()?6L*60*60*1000:5L*60*1000;
        return now-context.getSharedPreferences("launcherin-crests",0).getLong(id,0)>=retry;
    }
    static void load(Context context,String id) {
        if(!needsLoad(context,id))return;
        context.getSharedPreferences("launcherin-crests",0).edit().putLong(id,System.currentTimeMillis()).apply();
        HttpURLConnection connection=null;
        try {
            File image=file(context,id);connection=(HttpURLConnection)new URL("https://a.espncdn.com/i/teamlogos/soccer/500/"+id+".png").openConnection();
            connection.setConnectTimeout(5000);connection.setReadTimeout(7000);if(connection.getResponseCode()!=200)throw new IOException("Escudo indisponível");
            byte[] data;try(InputStream in=connection.getInputStream()){data=LauncherinExtras.bytes(in,524288);}
            BitmapFactory.Options size=new BitmapFactory.Options();size.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,size);
            if(size.outWidth<1||size.outHeight<1||size.outWidth>2048||size.outHeight>2048)throw new IOException("Escudo inválido");
            size.inSampleSize=1;while(size.outWidth/size.inSampleSize>256||size.outHeight/size.inSampleSize>256)size.inSampleSize*=2;size.inJustDecodeBounds=false;
            Bitmap bitmap=BitmapFactory.decodeByteArray(data,0,data.length,size);if(bitmap==null)throw new IOException("Escudo inválido");
            File folder=image.getParentFile();if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Cache indisponível");AtomicFile saved=new AtomicFile(image);FileOutputStream out=saved.startWrite();
            try{if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Escudo inválido");saved.finishWrite(out);}catch(Exception error){saved.failWrite(out);throw error;}
            memory.put(id,bitmap);
        } catch(Exception ignored) { } finally {if(connection!=null)connection.disconnect();}
    }
}
