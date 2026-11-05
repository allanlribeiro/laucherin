package app.launcherin.tv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.net.Uri;
import java.io.*;

/** Copies transient media artwork while the publishing app still grants access. */
final class LauncherinMediaArt {
    private static final String[] BITMAPS={MediaMetadata.METADATA_KEY_ART,MediaMetadata.METADATA_KEY_ALBUM_ART,MediaMetadata.METADATA_KEY_DISPLAY_ICON};
    private static final String[] URIS={MediaMetadata.METADATA_KEY_ART_URI,MediaMetadata.METADATA_KEY_ALBUM_ART_URI,MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI};
    static boolean supplied(MediaMetadata metadata){
        if(metadata==null)return false;
        for(String key:BITMAPS)try{if(metadata.getBitmap(key)!=null)return true;}catch(RuntimeException ignored){}
        for(String key:URIS)try{String uri=metadata.getString(key);if(uri!=null&&!uri.trim().isEmpty())return true;}catch(RuntimeException ignored){}
        return false;
    }
    static String capture(Context context,String key,MediaMetadata metadata,String previous){return capture(context,key,metadata,previous,160,90);}
    static String captureMusic(Context context,String key,MediaMetadata metadata,String previous){return capture(context,key,metadata,previous,48,48);}
    private static String capture(Context context,String key,MediaMetadata metadata,String previous,int minWidth,int minHeight){
        for(String field:BITMAPS)try{
            Bitmap bitmap=metadata.getBitmap(field);
            if(bitmap!=null&&bitmap.getWidth()>=minWidth&&bitmap.getHeight()>=minHeight)return save(context,key,bitmap);
        }catch(RuntimeException|IOException ignored){}
        for(String field:URIS)try{
            String source=metadata.getString(field);if(source==null)continue;source=source.trim();
            Uri uri=Uri.parse(source);String scheme=uri.getScheme();
            if("https".equalsIgnoreCase(scheme)&&uri.getHost()!=null)return source;
            if(!"content".equalsIgnoreCase(scheme)&&!"android.resource".equalsIgnoreCase(scheme))continue;
            byte[] bytes;try(InputStream input=context.getContentResolver().openInputStream(uri)){if(input==null)continue;bytes=LauncherinExtras.bytes(input,5_000_000);}
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            if(options.outWidth<minWidth||options.outHeight<minHeight)continue;
            options.inSampleSize=1;while(options.outWidth/options.inSampleSize>1920||options.outHeight/options.inSampleSize>1080)options.inSampleSize*=2;
            options.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            if(bitmap!=null)try{return save(context,key,bitmap);}finally{bitmap.recycle();}
        }catch(RuntimeException|IOException ignored){}
        return previous==null?"":previous;
    }
    private static String save(Context context,String key,Bitmap bitmap)throws IOException{
        File dir=new File(context.getFilesDir(),"cw");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Artwork directory unavailable");
        File file=new File(dir,Integer.toHexString(key.hashCode())+".jpg"),temporary=new File(dir,file.getName()+".part");
        float scale=Math.min(1f,1280f/Math.max(bitmap.getWidth(),bitmap.getHeight()));
        Bitmap small=scale<1?Bitmap.createScaledBitmap(bitmap,Math.max(1,Math.round(bitmap.getWidth()*scale)),Math.max(1,Math.round(bitmap.getHeight()*scale)),true):bitmap;
        try{
            try(OutputStream out=new FileOutputStream(temporary)){if(!small.compress(Bitmap.CompressFormat.JPEG,88,out))throw new IOException("Artwork encoding failed");}
            if(!temporary.renameTo(file))throw new IOException("Artwork replace failed");return file.getAbsolutePath();
        }finally{if(small!=bitmap)small.recycle();temporary.delete();}
    }
}
