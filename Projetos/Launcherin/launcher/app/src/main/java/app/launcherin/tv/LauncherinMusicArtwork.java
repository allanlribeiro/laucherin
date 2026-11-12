// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import java.util.*;
import java.util.concurrent.*;

/** Only visible artwork is requested, at most four pending URLs and 2 MiB of thumbnails. */
final class LauncherinMusicArtwork {
    interface Loader {Bitmap load(Context context,String source,int width,int height)throws Exception;}
    Loader loader=LauncherinPlayerImages::load;
    private static final class Entry {final ImageView view;final String source;final int width,height;final Drawable placeholder;Entry(ImageView v,String s,int w,int h,Drawable p){view=v;source=s;width=w;height=h;placeholder=p;}}
    private final Context context;private final Handler main=new Handler(Looper.getMainLooper());private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>());
    private final List<Entry> entries=new ArrayList<>();private final Set<String> pending=new HashSet<>(),failed=new HashSet<>();private int generation;private boolean active=true,closed;
    private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(2*1024*1024){@Override protected int sizeOf(String s,Bitmap b){return b.getAllocationByteCount();}};
    LauncherinMusicArtwork(Context c){context=c;}
    void bind(ImageView v,String source,int width,int height,Drawable placeholder){entries.removeIf(e->e.view==v);v.setTag(null);v.setImageDrawable(placeholder);entries.add(new Entry(v,source,width,height,placeholder));v.post(this::visible);}
    void forget(ImageView v){entries.removeIf(e->e.view==v);v.setTag(null);v.setImageDrawable(null);}
    void clear(){generation++;entries.clear();pending.clear();failed.clear();worker.getQueue().clear();}
    void visible(){if(!active||closed)return;for(Entry e:new ArrayList<>(entries)){if(!e.view.isShown()||!e.view.isAttachedToWindow()||!e.view.getGlobalVisibleRect(new Rect())){if(e.view.getTag() instanceof Bitmap){e.view.setTag(null);e.view.setImageDrawable(e.placeholder);}continue;}if(e.source.isEmpty()||e.view.getTag() instanceof Bitmap)continue;String key=e.source+"|"+e.width+"x"+e.height;Bitmap b=cache.get(key);if(b!=null){e.view.setImageBitmap(b);e.view.setTag(b);continue;}if(failed.contains(key)||pending.contains(key)||pending.size()>=4)continue;pending.add(key);int request=generation;worker.execute(()->{Bitmap ready=null;try{ready=loader.load(context,e.source,e.width,e.height);}catch(Exception ignored){}final Bitmap result=ready;main.post(()->{if(request!=generation||closed||!active){if(result!=null)result.recycle();return;}pending.remove(key);if(result==null)failed.add(key);else{if(result.getAllocationByteCount()<=2*1024*1024)cache.put(key,result);for(Entry target:entries)if((target.source+"|"+target.width+"x"+target.height).equals(key)){target.view.setImageBitmap(result);target.view.setTag(result);}}visible();});});}}
    void pause(){active=false;generation++;pending.clear();worker.getQueue().clear();for(Entry e:entries){e.view.setTag(null);e.view.setImageDrawable(e.placeholder);}cache.evictAll();}
    void resume(){active=true;failed.clear();visible();}
    void close(){closed=true;pause();entries.clear();main.removeCallbacksAndMessages(null);worker.shutdownNow();}
}
