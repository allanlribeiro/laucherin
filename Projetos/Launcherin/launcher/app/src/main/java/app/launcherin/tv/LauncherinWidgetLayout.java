package app.launcherin.tv;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Shared wire contract with the companion: a bounded 12 x 2 grid, no overlapping visible cards. */
final class LauncherinWidgetLayout {
    static final String KEY="dashboard_widgets";
    static final int COLUMNS=12, ROWS=2;
    static final List<String> IDS=Collections.unmodifiableList(Arrays.asList("clock","weather","music","sports","agenda","photos","system"));
    static final class Slot {
        final String id; final int x,y,w,h; final boolean enabled;
        Slot(String id,int x,int y,int w,int h,boolean enabled){this.id=id;this.x=x;this.y=y;this.w=w;this.h=h;this.enabled=enabled;}
        boolean overlaps(Slot b){return enabled&&b.enabled&&x<b.x+b.w&&x+w>b.x&&y<b.y+b.h&&y+h>b.y;}
        JSONObject json(){JSONObject o=new JSONObject();try{o.put("id",id).put("x",x).put("y",y).put("w",w).put("h",h).put("enabled",enabled);}catch(Exception ignored){}return o;}
    }
    static List<Slot> defaults(){List<Slot> out=new ArrayList<>();for(int i=0;i<IDS.size();i++)out.add(new Slot(IDS.get(i),(i%4)*3,i<4?0:1,3,1,i<4));return out;}
    static List<Slot> read(JSONObject config){return clean(config.optJSONArray(KEY));}
    static List<Slot> clean(JSONArray array){
        if(array==null)return defaults();List<Slot> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(int i=0;i<Math.min(32,array.length());i++){
            JSONObject o=array.optJSONObject(i);if(o==null)continue;String id=o.optString("id");if(!IDS.contains(id)||!seen.add(id))continue;
            int w=o.optInt("w",3),h=o.optInt("h",1);if(!((w==3&&h==1)||(w==4&&h==1)||(w==6&&h==2))){w=3;h=1;}
            int x=Math.max(0,Math.min(COLUMNS-w,o.optInt("x"))),y=Math.max(0,Math.min(ROWS-h,o.optInt("y")));
            Slot slot=new Slot(id,x,y,w,h,o.optBoolean("enabled",true));
            if(collides(out,slot))slot=place(out,id,w,h);
            out.add(slot);
        }
        // Missing entries in an explicit layout stay hidden. An empty array intentionally hides all.
        for(String id:IDS)if(!seen.contains(id))out.add(new Slot(id,0,0,3,1,false));
        return out;
    }
    private static Slot place(List<Slot> out,String id,int w,int h){
        for(int y=0;y<=ROWS-h;y++)for(int x=0;x<=COLUMNS-w;x++){Slot s=new Slot(id,x,y,w,h,true);if(!collides(out,s))return s;}
        return new Slot(id,0,0,w,h,false);
    }
    private static boolean collides(List<Slot> out,Slot slot){for(Slot other:out)if(slot.overlaps(other))return true;return false;}
    static int usedRows(List<Slot> slots){int rows=0;for(Slot s:slots)if(s.enabled)rows=Math.max(rows,s.y+s.h);return rows;}
    static String signature(List<Slot> slots){JSONArray a=new JSONArray();for(Slot s:slots)a.put(s.json());return a.toString();}
}
