package app.launcherin.tv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Brasileirão Série A. Loaded on demand in the expanded sports card, never polled by Home. */
final class LauncherinStandings {
    static final long REFRESH_MS=15*60*1000L, RETRY_MS=5*60*1000L;
    static final String ENDPOINT="https://site.web.api.espn.com/apis/v2/sports/soccer/bra.1/standings?locale=pt_BR";
    interface Fetcher {JSONObject get() throws Exception;}
    static final class Row {
        final String id,name;final int rank,points,played,wins,draws,losses,goalDifference;
        Row(JSONObject o)throws Exception{
            id=o.getString("id");name=o.getString("name");rank=o.getInt("rank");points=o.getInt("points");played=o.getInt("played");wins=o.getInt("wins");draws=o.getInt("draws");losses=o.getInt("losses");goalDifference=o.getInt("gd");
            if(!id.matches("[0-9]{1,8}")||name.trim().isEmpty()||name.length()>100||rank<1||rank>20||played<0||wins<0||draws<0||losses<0)throw new IOException("Classificação inválida");
        }
    }
    static final class Snapshot {
        final int season;final long updated;final List<Row> rows;
        Snapshot(JSONObject o)throws Exception{
            if(!"bra.1".equals(o.getString("league")))throw new IOException("Campeonato inválido");season=o.getInt("season");updated=o.getLong("updated");if(season<2000||season>2200||updated<0)throw new IOException("Temporada inválida");
            JSONArray entries=o.getJSONArray("rows");if(entries.length()<1||entries.length()>20)throw new IOException("Tabela indisponível");List<Row> list=new ArrayList<>();Set<String> ids=new HashSet<>();Set<Integer> ranks=new HashSet<>();
            for(int i=0;i<entries.length();i++){Row r=new Row(entries.getJSONObject(i));if(!ids.add(r.id)||!ranks.add(r.rank))throw new IOException("Classificação duplicada");list.add(r);}
            Collections.sort(list,Comparator.comparingInt(r->r.rank));rows=Collections.unmodifiableList(list);
        }
        boolean old(long now){return now-updated>REFRESH_MS;}
    }
    private static int stat(JSONObject entry,String name)throws Exception{
        JSONArray stats=entry.getJSONArray("stats");for(int i=0;i<Math.min(64,stats.length());i++){JSONObject s=stats.getJSONObject(i);if(name.equals(s.optString("name"))){Object n=s.opt("value");if(!(n instanceof Number))throw new IOException("Estatística indisponível");double d=((Number)n).doubleValue();if(Double.isNaN(d)||Double.isInfinite(d)||d!=Math.rint(d)||Math.abs(d)>10000)throw new IOException("Estatística inválida");return (int)d;}}
        throw new IOException("Estatística ausente");
    }
    static JSONObject parse(JSONObject response,long now)throws Exception{
        // ESPN's API uses league UID 630; its site standings payload has root id 85.
        if(!"s:600~l:630".equals(response.optString("uid")))throw new IOException("Campeonato inesperado");
        JSONArray groups=response.getJSONArray("children");JSONObject standings=null;
        for(int i=0;i<Math.min(8,groups.length());i++){JSONObject s=groups.getJSONObject(i).optJSONObject("standings");if(s!=null&&"overall".equals(s.optString("name"))){if(standings!=null)throw new IOException("Classificação ambígua");standings=s;}}
        if(standings==null)throw new IOException("Tabela indisponível");JSONArray source=standings.getJSONArray("entries"),rows=new JSONArray();if(source.length()!=20)throw new IOException("Classificação completa indisponível");
        for(int i=0;i<source.length();i++){JSONObject e=source.getJSONObject(i),team=e.getJSONObject("team");rows.put(new JSONObject().put("id",team.getString("id")).put("name",team.getString("displayName")).put("rank",stat(e,"rank")).put("points",stat(e,"points")).put("played",stat(e,"gamesPlayed")).put("wins",stat(e,"wins")).put("draws",stat(e,"ties")).put("losses",stat(e,"losses")).put("gd",stat(e,"pointDifferential")));}
        JSONObject normalized=new JSONObject().put("league","bra.1").put("season",standings.getInt("season")).put("updated",now).put("rows",rows);new Snapshot(normalized);return normalized;
    }
    static JSONObject get()throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(ENDPOINT).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(8000);c.setRequestProperty("Accept","application/json");
        try{if(c.getResponseCode()!=200)throw new IOException("Tabela indisponível");try(InputStream in=c.getInputStream()){return new JSONObject(new String(LauncherinExtras.bytes(in,1024*1024),StandardCharsets.UTF_8));}}finally{c.disconnect();}
    }
    private static SharedPreferences store(Context c){return c.getSharedPreferences("launcherin-standings",0);}
    static Snapshot cached(Context c){try{return new Snapshot(new JSONObject(store(c).getString("bra.1","{}")));}catch(Exception ignored){return null;}}
    static Snapshot load(Context c)throws Exception{return load(c,LauncherinStandings::get,System.currentTimeMillis());}
    static synchronized Snapshot load(Context c,Fetcher fetcher,long now)throws Exception{
        Snapshot previous=cached(c);if(previous!=null&&!previous.old(now))return previous;SharedPreferences s=store(c);long attempt=s.getLong("attempt",0);
        if(attempt>0&&now-attempt<RETRY_MS){if(previous!=null)return previous;throw new IOException("Aguardando nova consulta");}
        s.edit().putLong("attempt",now).apply();try{JSONObject value=parse(fetcher.get(),now);Snapshot result=new Snapshot(value);s.edit().putString("bra.1",value.toString()).apply();return result;}catch(Exception e){if(previous!=null)return previous;throw e;}
    }
}
