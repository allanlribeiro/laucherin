// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import org.json.*;
import android.content.Context;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Optional lyrics. No guessed text, and unmatched recordings never receive synchronized lyrics. */
final class LauncherinLyrics {
    static final class Line {final long time;final String text;Line(long time,String text){this.time=time;this.text=text;}}
    static final class Result {
        final String plain;final List<Line> lines;final boolean instrumental;
        Result(String plain,String lrc,boolean instrumental){this.plain=plain;this.lines=parse(lrc);this.instrumental=instrumental;}
        int at(long position){int index=-1;for(int i=0;i<lines.size()&&lines.get(i).time<=position;i++)index=i;return index;}
    }
    static List<Line> parse(String lrc){
        List<Line> out=new ArrayList<>();if(lrc==null)return out;Matcher offset=Pattern.compile("\\[offset:([+-]?\\d+)\\]",Pattern.CASE_INSENSITIVE).matcher(lrc);long adjustment=0;if(offset.find())try{adjustment=Long.parseLong(offset.group(1));}catch(NumberFormatException ignored){}
        Pattern stamp=Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]");
        String[] raw=lrc.split("\\r?\\n",4001);for(int n=0;n<Math.min(raw.length,4000);n++){Matcher m=stamp.matcher(raw[n]);List<Long> times=new ArrayList<>();int end=0;while(m.find()){String frac=m.group(3);long ms=frac==null?0:Math.round(Double.parseDouble("0."+frac)*1000);times.add(Math.max(0,Long.parseLong(m.group(1))*60000+Long.parseLong(m.group(2))*1000+ms+adjustment));end=m.end();}String text=raw[n].substring(end).trim();if(!text.isEmpty())for(long t:times)out.add(new Line(t,LauncherinTrack.limit(text,500)));}
        out.sort(Comparator.comparingLong(l->l.time));return out;
    }
    private static String normalize(String s){return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    static boolean matches(JSONObject j,String title,String artist,long duration){
        String a=normalize(j.optString("artistName")),b=normalize(artist);
        return normalize(j.optString("trackName")).equals(normalize(title))&&!a.isEmpty()&&(a.equals(b)||b.equals(a+"topic"))&&Math.abs(j.optDouble("duration")-duration/1000d)<=3;
    }
    static String[] query(LauncherinTrack t){String title=t.title.replaceAll("(?i)\\s*[\\[(](?:official.*|audio.*|video.*|vídeo.*|lyric.*)[\\])]","").trim();String artist=t.artist.replaceFirst("(?i)\\s*-\\s*topic$","").trim();int split=title.indexOf(" - ");if(split>0){artist=title.substring(0,split).trim();title=title.substring(split+3).trim();}return new String[]{title,artist};}
    static Result load(Context c,LauncherinTrack track)throws Exception {
        String[] q=query(track);String key=LauncherinMusicLibrary.hash((q[0]+"\n"+q[1]+"\n"+track.duration).getBytes(StandardCharsets.UTF_8));File dir=new File(c.getCacheDir(),"player-lyrics");if(!dir.exists()&&!dir.mkdirs())throw new IOException();File file=new File(dir,key+".json");JSONObject json;
        if(file.isFile()&&System.currentTimeMillis()-file.lastModified()<7*86400000L)try(InputStream in=new FileInputStream(file)){json=new JSONObject(new String(LauncherinExtras.bytes(in,500000),StandardCharsets.UTF_8));}
        else {
            String url="https://lrclib.net/api/get?track_name="+URLEncoder.encode(q[0],"UTF-8")+"&artist_name="+URLEncoder.encode(q[1],"UTF-8")+"&duration="+Math.round(track.duration/1000d);
            HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(8000);connection.setReadTimeout(8000);connection.setRequestProperty("User-Agent","Launcherin/0.1.0 (https://github.com/allanlribeiro/laucherin)");
            try{if(connection.getResponseCode()==404)return new Result("","",false);if(connection.getResponseCode()!=200)throw new IOException("Letras indisponíveis");try(InputStream in=connection.getInputStream()){json=new JSONObject(new String(LauncherinExtras.bytes(in,500000),StandardCharsets.UTF_8));}}finally{connection.disconnect();}
            if(!matches(json,q[0],q[1],track.duration))return new Result("","",false);
            File[] files=dir.listFiles();if(files!=null&&files.length>=12){Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(int i=0;i<=files.length-12;i++)files[i].delete();}
            try(OutputStream out=new FileOutputStream(file)){out.write(json.toString().getBytes(StandardCharsets.UTF_8));}
        }
        return new Result(json.optString("plainLyrics",""),json.optString("syncedLyrics",""),json.optBoolean("instrumental"));
    }
}
