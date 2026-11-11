// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import org.json.*;
import java.net.URI;

/** Stable public page identifiers only. Expiring audio URLs are resolved again, never persisted. */
final class LauncherinTrack {
    final String url,title,artist,art,source; final long duration;
    LauncherinTrack(String url,String title,String artist,String art,long duration,String source) {
        if(!validPage(url)||title==null||title.trim().isEmpty())throw new IllegalArgumentException("Música inválida");
        this.url=url;this.title=limit(title,240);this.artist=limit(artist,180);
        this.art=art!=null&&art.startsWith("https://")?limit(art,2000):"";
        this.duration=Math.max(0,Math.min(duration,86400000));this.source="soundcloud".equals(source)?"SoundCloud":"YouTube";
    }
    static String limit(String s,int n){return s==null?"":s.substring(0,Math.min(n,s.length())).trim();}
    static boolean validPage(String s) {
        try {URI u=new URI(s);String h=u.getHost();if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1||h==null||s.length()>2048)return false;
            if(h.equals("www.youtube.com")||h.equals("music.youtube.com")||h.equals("youtube.com"))
                return "/watch".equals(u.getPath())&&u.getRawQuery()!=null&&u.getRawQuery().matches("(?:[^&]*&)*v=[A-Za-z0-9_-]{11}(?:&.*)?");
            return (h.equals("soundcloud.com")||h.equals("www.soundcloud.com"))&&u.getPath().matches("/[A-Za-z0-9_-]+/[A-Za-z0-9_-]+/?");
        }catch(Exception e){return false;}
    }
    JSONObject json(){JSONObject j=new JSONObject();try{j.put("url",url).put("title",title).put("artist",artist).put("art",art).put("duration",duration).put("source",source.equals("SoundCloud")?"soundcloud":"youtube");}catch(JSONException impossible){throw new IllegalStateException(impossible);}return j;}
    static LauncherinTrack read(JSONObject j){if(j==null)return null;try{return new LauncherinTrack(j.getString("url"),j.getString("title"),j.optString("artist"),j.optString("art"),j.optLong("duration"),j.optString("source"));}catch(Exception e){return null;}}
}
