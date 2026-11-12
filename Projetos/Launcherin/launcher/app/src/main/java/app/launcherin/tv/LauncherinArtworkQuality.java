// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;
import java.net.URI;import java.util.*;

/** Upgrade only documented image-CDN size variants; retain the exact original as a fallback. */
final class LauncherinArtworkQuality {
 static List<String> candidates(String source,int width,int height){
  LinkedHashSet<String> urls=new LinkedHashSet<>();try{
   URI u=new URI(source);String host=u.getHost(),path=u.getRawPath();int size=Math.max(320,Math.min(1024,Math.max(width,height)*2));
   if("https".equals(u.getScheme())&&u.getUserInfo()==null&&u.getPort()==-1&&host!=null){
    if((host.matches("lh[0-9]+\\.googleusercontent\\.com")||host.equals("yt3.googleusercontent.com")||host.equals("yt3.ggpht.com"))&&path!=null){
     String high=path.replaceFirst("=w[0-9]+-h[0-9]+[^/]*$","=w"+size+"-h"+size+"-l90-rj").replaceFirst("=s[0-9]+[^/]*$","=s"+size+"-c-k-c0x00ffffff-no-rj");
     if(!high.equals(path))urls.add("https://"+host+high+(u.getRawQuery()==null?"":"?"+u.getRawQuery()));
    }
    if((host.equals("i.ytimg.com")||host.equals("img.youtube.com"))&&path!=null&&path.matches("/vi(?:_webp)?/[A-Za-z0-9_-]{11}/(?:default|mqdefault|hqdefault|sddefault|maxresdefault)\\.(?:jpg|webp)")){
     String prefix=path.substring(0,path.lastIndexOf('/')+1),ext=path.endsWith("webp")?"webp":"jpg";
     if(size>480)urls.add("https://"+host+prefix+"maxresdefault."+ext);
     urls.add("https://"+host+prefix+"hqdefault."+ext);
    }
    if(host.matches("i[0-9]+\\.sndcdn\\.com")&&path!=null&&path.matches(".*-(?:large|small|badge|t[0-9]+x[0-9]+)\\.(?:jpg|png)"))urls.add("https://"+host+path.replaceFirst("-(?:large|small|badge|t[0-9]+x[0-9]+)(\\.(?:jpg|png))$","-t500x500$1"));
   }
  }catch(Exception ignored){}urls.add(source);return new ArrayList<>(urls);
 }
 static int sample(int width,int height,int targetWidth,int targetHeight){int result=1;while(width/(result*2)>=Math.max(1,targetWidth)&&height/(result*2)>=Math.max(1,targetHeight))result*=2;return result;}
}
