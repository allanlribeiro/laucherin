package app.launcherin.tv;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.Resources;
import android.os.*;
import android.util.LruCache;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;

/** Official artwork, unchanged dimensions. Uniform background removal is cached off the UI thread. */
final class LauncherinAppFinish {
 private static final ExecutorService worker=Executors.newSingleThreadExecutor();
 private static final Handler main=new Handler(Looper.getMainLooper());
 private static final LruCache<String,Drawable> cache=new LruCache<>(8);
 private static final java.util.Set<String> loading=java.util.Collections.synchronizedSet(new java.util.HashSet<>());
 static void apply(AppAdapter.Holder h){if(h.appearanceArt==null)return;LauncherinAppearance.Style style=LauncherinAppearance.get(h.card.getContext());String finish=style.apps;
  if("original".equals(finish)){h.banner.setImageDrawable(h.appearanceArt);h.card.setBackground(h.appearanceBackground);h.card.setForeground(null);return;}
  float density=h.card.getResources().getDisplayMetrics().density;int width=h.card.getWidth()>0?h.card.getWidth():Math.max(1,h.card.getLayoutParams().width);float radius=AppAdapter.cornerRadius(width,density)/density;boolean minimal="minimal".equals(finish);h.card.setBackground(new LauncherinSurface(h.card.getContext(),minimal?100:"premium".equals(finish)?35:65,false,false,false,radius));h.card.setForeground(LauncherinSurface.outline(h.card.getContext(),h.card.hasFocus(),radius));
  if("premium".equals(finish)){h.banner.setImageDrawable(h.appearanceArt);return;}
  String key=h.appearanceKey;Drawable art=cache.get(key);if(art!=null){h.banner.setImageDrawable(art.getConstantState()==null?art:art.getConstantState().newDrawable());return;}
  h.banner.setImageDrawable(h.appearanceArt);if(!loading.add(key))return;Drawable source=h.appearanceArt.getConstantState()==null?null:h.appearanceArt.getConstantState().newDrawable().mutate();Resources res=h.card.getResources();WeakReference<AppAdapter.Holder> holder=new WeakReference<>(h);
  worker.execute(()->{Drawable ready=source;try{if(source!=null)ready=mask(res,source);}catch(RuntimeException ignored){}Drawable result=ready;main.post(()->{loading.remove(key);if(result!=null)cache.put(key,result);AppAdapter.Holder current=holder.get();if(current!=null&&key.equals(current.appearanceKey)&&("glass".equals(LauncherinAppearance.get(current.card.getContext()).apps)||"minimal".equals(LauncherinAppearance.get(current.card.getContext()).apps))&&result!=null)current.banner.setImageDrawable(result.getConstantState()==null?result:result.getConstantState().newDrawable());});});
 }
 static Drawable mask(Resources res,Drawable source){int sw=source.getIntrinsicWidth(),sh=source.getIntrinsicHeight();if(sw<=0||sh<=0)return source;float k=Math.min(1,320f/Math.max(sw,sh));int w=Math.max(1,Math.round(sw*k)),h=Math.max(1,Math.round(sh*k));Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);source.setBounds(0,0,w,h);source.draw(new Canvas(bitmap));int[] p=new int[w*h];bitmap.getPixels(p,0,w,0,0,w,h);int bg=p[0];
  if(Color.alpha(bg)<250||distance(bg,p[w-1])>18||distance(bg,p[(h-1)*w])>18||distance(bg,p[p.length-1])>18){bitmap.recycle();return source;}
  boolean[] gone=new boolean[p.length];int[] queue=new int[p.length];int tail=0;
  for(int x=0;x<w;x++){if(distance(bg,p[x])<=18){gone[x]=true;queue[tail++]=x;}int i=(h-1)*w+x;if(!gone[i]&&distance(bg,p[i])<=18){gone[i]=true;queue[tail++]=i;}}
  for(int y=0;y<h;y++){tail=enqueue(y*w,bg,p,gone,queue,tail);tail=enqueue(y*w+w-1,bg,p,gone,queue,tail);}
  for(int head=0;head<tail;head++){int i=queue[head],x=i%w,y=i/w;if(x>0)tail=enqueue(i-1,bg,p,gone,queue,tail);if(x+1<w)tail=enqueue(i+1,bg,p,gone,queue,tail);if(y>0)tail=enqueue(i-w,bg,p,gone,queue,tail);if(y+1<h)tail=enqueue(i+w,bg,p,gone,queue,tail);}
  long bright=0;int foreground=0;for(int i=0;i<p.length;i++)if(!gone[i]&&Color.alpha(p[i])>128){bright+=Math.max(Color.red(p[i]),Math.max(Color.green(p[i]),Color.blue(p[i])));foreground++;}
  if(tail<p.length*.2||foreground==0||bright/foreground<100){bitmap.recycle();return source;}
  for(int i=0;i<p.length;i++)if(gone[i])p[i]=Color.TRANSPARENT;bitmap.setPixels(p,0,w,0,0,w,h);return new BitmapDrawable(res,bitmap);
 }
 static void trim(){cache.evictAll();}
 private static int enqueue(int i,int bg,int[] pixels,boolean[] gone,int[] queue,int tail){if(!gone[i]&&distance(bg,pixels[i])<=18){gone[i]=true;queue[tail++]=i;}return tail;}
 private static int distance(int a,int b){return Math.max(Math.abs(Color.red(a)-Color.red(b)),Math.max(Math.abs(Color.green(a)-Color.green(b)),Math.abs(Color.blue(a)-Color.blue(b))));}
}
