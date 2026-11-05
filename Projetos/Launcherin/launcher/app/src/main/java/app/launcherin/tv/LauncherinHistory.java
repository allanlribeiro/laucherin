package app.launcherin.tv;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Public playback/program snapshots, bounded shelf and persistent per-item dismissals. */
final class LauncherinHistory {
    static final int MAX=10;
    static String key(JSONObject row){String key=row.optString("key");return key.isEmpty()?row.optString("pkg",row.optString("provider"))+"|"+row.optString("title").trim():key;}
    static boolean videoPackage(String pkg){return !pkg.startsWith("com.google.android.youtube")&&!pkg.startsWith("com.google.android.apps.youtube");}
    static boolean eligible(JSONObject row){return videoPackage(row.optString("pkg"))&&!"YouTube".equals(row.optString("provider"));}
    static JSONArray visible(JSONArray rows,JSONObject removed){
        JSONArray result=new JSONArray();HashSet<String> seen=new HashSet<>();
        for(int i=0;i<rows.length()&&result.length()<MAX;i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            String key=key(row);if(eligible(row)&&!removed.has(key)&&seen.add(key))result.put(row);
        }return result;
    }
    static void dismiss(JSONObject removed,JSONObject item,long now)throws Exception{
        removed.put(key(item),Math.max(now,item.optLong("updated")));
        if(removed.length()>1000){ArrayList<String> keys=new ArrayList<>();for(Iterator<String> it=removed.keys();it.hasNext();)keys.add(it.next());keys.sort(Comparator.comparingLong(k->removed.optLong(k)));for(int i=0;i<keys.size()-1000;i++)removed.remove(keys.get(i));}
    }
    static JSONArray playback(JSONArray old,JSONObject original,long now)throws Exception{return playback(old,original,now,new JSONObject(),false);}
    static JSONArray playback(JSONArray old,JSONObject original,long now,JSONObject removed,boolean newPlay)throws Exception{
        JSONObject item=new JSONObject(original.toString());String key=key(item);
        if(!eligible(item))return visible(old,removed);
        if(newPlay)removed.remove(key);
        if(removed.has(key))return visible(old,removed);
        for(int i=0;i<old.length();i++){JSONObject row=old.getJSONObject(i);if(key.equals(key(row)))carry(item,row);}
        item.put("source","media");JSONArray next=new JSONArray();long duration=item.optLong("duration"),position=item.optLong("position",-1);
        if(duration<=0||position<duration*.97)next.put(item);
        for(int i=0;i<old.length();i++){JSONObject row=old.getJSONObject(i);if(!key.equals(key(row))&&("watchnext".equals(row.optString("source"))||now-row.optLong("updated")<7*86400000L))next.put(row);}
        return visible(next,removed);
    }
    static JSONArray programs(JSONArray old,JSONArray programs)throws Exception{return programs(old,programs,new JSONObject());}
    static JSONArray programs(JSONArray old,JSONArray programs,JSONObject removed)throws Exception{
        LinkedHashMap<String,JSONObject> merged=new LinkedHashMap<>();
        for(int i=0;i<old.length();i++){JSONObject item=new JSONObject(old.getJSONObject(i).toString());if(eligible(item)&&!"watchnext".equals(item.optString("source"))&&!removed.has(key(item)))merged.put(key(item),item);}
        for(int i=0;i<programs.length();i++){
            JSONObject item=new JSONObject(programs.getJSONObject(i).toString());String key=key(item);
            if(!eligible(item))continue;
            if(removed.has(key)){if(item.optLong("updated")<=removed.optLong(key))continue;removed.remove(key);}
            JSONObject existing=merged.get(key);
            if(existing!=null){JSONObject newer=existing.optLong("updated")>=item.optLong("updated")?existing:item;carry(newer,newer==existing?item:existing);merged.put(key,newer);}
            else merged.put(key,item);
        }
        List<JSONObject> sorted=new ArrayList<>(merged.values());sorted.sort((a,b)->Long.compare(b.optLong("updated"),a.optLong("updated")));
        JSONArray rows=new JSONArray();for(JSONObject item:sorted)rows.put(item);return visible(rows,removed);
    }
    private static void carry(JSONObject target,JSONObject other)throws Exception{
        for(String field:new String[]{"art","intent_uri"})if(target.optString(field).isEmpty())target.put(field,other.optString(field));
    }
}
