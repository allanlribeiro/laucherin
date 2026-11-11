package app.launcherin.tv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/** Public fixture data; bounded reads, cached per team, no account and no background polling. */
final class LauncherinSports {
    static final String KEY = "sports_favorite_teams";
    static final long REFRESH_MS = 15 * 60 * 1000L;
    static final class Team {
        final String id,name,league;
        Team(JSONObject row) throws Exception { id=row.getString("id");name=row.getString("name");league=row.getString("league");if(!id.matches("[0-9]{1,8}"))throw new IOException("Time inválido"); }
    }
    static List<Team> catalogue(Context context) {
        List<Team> teams=new ArrayList<>();
        try(InputStream in=context.getAssets().open("launcherin-teams.json")) {
            JSONArray rows=new JSONArray(new String(LauncherinExtras.bytes(in,131072),StandardCharsets.UTF_8));
            for(int i=0;i<rows.length();i++)teams.add(new Team(rows.getJSONObject(i)));
        } catch(Exception ignored) { }
        return teams;
    }
    static List<Team> selected(Context context) {
        JSONArray ids=LauncherinExtras.read(context).optJSONArray(KEY);List<Team> all=catalogue(context),result=new ArrayList<>();
        if(ids!=null)for(int i=0;i<ids.length()&&result.size()<2;i++)for(Team team:all)
            if(team.id.equals(ids.optString(i))&&!result.contains(team))result.add(team);
        return result;
    }
    static void select(Context context,List<Team> teams) throws Exception {
        JSONArray ids=new JSONArray();List<String> seen=new ArrayList<>();
        for(Team team:teams)if(!seen.contains(team.id)&&ids.length()<2){ids.put(team.id);seen.add(team.id);}
        LauncherinExtras.setArray(context,KEY,ids);
    }
    static final class Match {
        final String id,home,away,homeId,awayId,homeScore,awayScore,competition,state;
        final long time;
        Match(JSONObject row) throws Exception {
            id=row.getString("id");homeId=row.optString("homeId");awayId=row.optString("awayId");home=row.getString("home");away=row.getString("away");homeScore=row.optString("homeScore");awayScore=row.optString("awayScore");
            competition=row.optString("competition");state=row.getString("state");time=row.getLong("time");
        }
        String pairing() { return home+" × "+away; }
        String score() { return home+"  "+homeScore+" × "+awayScore+"  "+away; }
    }
    static final class Snapshot {
        final List<Match> upcoming=new ArrayList<>();final Match previous;final long updated;
        Snapshot(JSONObject row) throws Exception {
            updated=row.getLong("updated");JSONArray next=row.getJSONArray("upcoming");for(int i=0;i<Math.min(2,next.length());i++)upcoming.add(new Match(next.getJSONObject(i)));
            previous=row.optJSONObject("previous")==null?null:new Match(row.getJSONObject("previous"));
        }
        boolean old() { return System.currentTimeMillis()-updated>REFRESH_MS; }
    }
    /** Up to two distinct nearest fixtures, prioritising one for each favourite. */
    static List<Match> nextGames(List<Team> teams,java.util.Map<String,Snapshot> data){
        LinkedHashMap<String,Match> chosen=new LinkedHashMap<>();List<Match> others=new ArrayList<>();
        for(Team t:teams){Snapshot s=data.get(t.id);if(s==null)continue;for(int i=0;i<s.upcoming.size();i++){Match g=s.upcoming.get(i);if(i==0)chosen.put(g.id,g);else others.add(g);}}
        Collections.sort(others,Comparator.comparingLong(g->g.time));for(Match g:others)if(chosen.size()<2)chosen.put(g.id,g);
        List<Match> result=new ArrayList<>(chosen.values());Collections.sort(result,Comparator.comparingLong(g->g.time));return result.size()>2?new ArrayList<>(result.subList(0,2)):result;
    }
    private static SharedPreferences store(Context context) { return context.getSharedPreferences("launcherin-sports",0); }
    static Snapshot cached(Context context,String id) {try{return new Snapshot(new JSONObject(store(context).getString(id,"{}")));}catch(Exception ignored){return null;}}
    static JSONObject get(String id,boolean future) throws Exception {
        if(!id.matches("[0-9]{1,8}"))throw new IOException("Time inválido");
        HttpURLConnection connection=(HttpURLConnection)new URL("https://site.api.espn.com/apis/site/v2/sports/soccer/all/teams/"+id+"/schedule"+(future?"?fixture=true":"")).openConnection();
        connection.setConnectTimeout(5000);connection.setReadTimeout(8000);connection.setRequestProperty("Accept","application/json");
        try {
            if(connection.getResponseCode()!=200)throw new IOException("Jogos indisponíveis");
            try(InputStream in=connection.getInputStream()){
                JSONObject response=new JSONObject(new String(LauncherinExtras.bytes(in,2*1024*1024),StandardCharsets.UTF_8));
                if(!id.equals(response.getJSONObject("team").getString("id"))||!response.has("events"))throw new IOException("Agenda indisponível");
                return response;
            }
        } finally {connection.disconnect();}
    }
    static Snapshot load(Context context,Team team) throws Exception {
        Snapshot cached=cached(context,team.id);if(cached!=null&&!cached.old())return cached;
        SharedPreferences preferences=store(context);long now=System.currentTimeMillis();
        if(now-preferences.getLong(team.id+"-attempt",0)<5*60*1000L){if(cached!=null)return cached;throw new IOException("Aguardando consulta");}
        preferences.edit().putLong(team.id+"-attempt",now).apply();
        try {
            JSONObject next=get(team.id,true),past=get(team.id,false),saved=merge(team.id,next,past,now);
            preferences.edit().putString(team.id,saved.toString()).apply();return new Snapshot(saved);
        } catch(Exception error){if(cached!=null)return cached;throw error;}
    }
    /** Filter using actual competition status; cancelled/postponed games are not results. */
    static JSONObject merge(String team,JSONObject future,JSONObject past,long now) throws Exception {
        LinkedHashMap<String,JSONObject> games=new LinkedHashMap<>();
        for(JSONObject response:new JSONObject[]{past,future}) {
            JSONArray events=response.optJSONArray("events");if(events==null)continue;
            for(int i=0;i<Math.min(1000,events.length());i++)try {
                JSONObject event=events.getJSONObject(i),competition=event.getJSONArray("competitions").getJSONObject(0),status=competition.getJSONObject("status").getJSONObject("type");
                String name=status.optString("name"),state=status.optString("state");
                if(name.contains("CANCEL")||name.contains("POSTPON")||name.contains("ABANDON")||name.contains("SUSPEND"))continue;
                JSONArray sides=competition.getJSONArray("competitors");JSONObject home=null,away=null;boolean belongs=false;
                for(int j=0;j<sides.length();j++){JSONObject side=sides.getJSONObject(j);if(team.equals(side.getJSONObject("team").getString("id")))belongs=true;if("home".equals(side.optString("homeAway")))home=side;else if("away".equals(side.optString("homeAway")))away=side;}
                if(!belongs||home==null||away==null)continue;
                long time=java.time.OffsetDateTime.parse(event.getString("date")).toInstant().toEpochMilli();
                if("pre".equals(state)&&time<now||"post".equals(state)&&(!status.optBoolean("completed")||time>now))continue;
                if(!state.equals("pre")&&!state.equals("post")&&!state.equals("in"))continue;
                String h=score(home),a=score(away);if(state.equals("post")&&(h.isEmpty()||a.isEmpty()))continue;
                JSONObject league=event.optJSONObject("league");
                JSONObject row=new JSONObject().put("id",event.getString("id")).put("home",home.getJSONObject("team").getString("displayName"))
                        .put("away",away.getJSONObject("team").getString("displayName")).put("homeId",home.getJSONObject("team").getString("id")).put("awayId",away.getJSONObject("team").getString("id")).put("homeScore",h).put("awayScore",a)
                        .put("time",time).put("state",state).put("competition",league==null?"":league.optString("name"));
                games.put(row.getString("id"),row);
            }catch(Exception ignored){ }
        }
        List<JSONObject> next=new ArrayList<>();JSONObject previous=null;
        for(JSONObject row:games.values())if(row.getString("state").equals("post")){if(previous==null||row.getLong("time")>previous.getLong("time"))previous=row;}else next.add(row);
        Collections.sort(next,Comparator.comparingLong(row->row.optLong("time")));
        JSONArray upcoming=new JSONArray();for(int i=0;i<Math.min(2,next.size());i++)upcoming.put(next.get(i));
        return new JSONObject().put("updated",now).put("upcoming",upcoming).put("previous",previous==null?JSONObject.NULL:previous);
    }
    private static String score(JSONObject side) {
        Object value=side.opt("score");String text=value instanceof JSONObject?((JSONObject)value).optString("displayValue"):value instanceof String?(String)value:"";
        return text.matches("[0-9]{1,2}")?text:"";
    }
}
