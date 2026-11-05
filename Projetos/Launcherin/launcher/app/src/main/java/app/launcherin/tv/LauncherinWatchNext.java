package app.launcherin.tv;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;

/** Public, searchable Watch Next rows. Restricted provider rows remain inaccessible. */
final class LauncherinWatchNext {
    static final int REQUEST = 7321;
    static final String PERMISSION = "android.permission.READ_TV_LISTINGS";
    private static final Uri URI = Uri.parse("content://android.media.tv/watch_next_program");
    static boolean granted(Context context) { return context.checkSelfPermission(PERMISSION)==PackageManager.PERMISSION_GRANTED; }
    private static String text(Cursor cursor,String column) { int index=cursor.getColumnIndex(column);return index<0||cursor.isNull(index)?"":cursor.getString(index); }
    private static long number(Cursor cursor,String column,long fallback) { int index=cursor.getColumnIndex(column);return index<0||cursor.isNull(index)?fallback:cursor.getLong(index); }
    static JSONArray read(Context context) {
        JSONArray result=new JSONArray();if(!granted(context)){status(context,"Permissão do histórico da TV não concedida",0);return null;}
        String[] columns={"_id","package_name","title","episode_title","poster_art_uri","thumbnail_uri","intent_uri","last_playback_position_millis","duration_millis","watch_next_type","last_engagement_time_utc_millis","browsable"};
        try(Cursor cursor=context.getContentResolver().query(URI,columns,null,null,"last_engagement_time_utc_millis DESC")) {
            if(cursor==null){status(context,"O sistema não disponibilizou a lista",0);return null;}
            int visited=0;while(cursor.moveToNext()&&visited++<300&&result.length()<30) {
                String pkg=text(cursor,"package_name"),title=text(cursor,"title");long type=number(cursor,"watch_next_type",-1);
                if(!LauncherinHistory.videoPackage(pkg)||LauncherinMediaService.music(pkg)||LauncherinMediaService.provider(pkg).isEmpty()||title.trim().isEmpty()||(type!=0&&type!=1)||number(cursor,"browsable",1)==0)continue;
                long duration=number(cursor,"duration_millis",0),position=number(cursor,"last_playback_position_millis",-1);if(type==0&&duration>0&&position>=duration*.97)continue;
                String art=text(cursor,"thumbnail_uri");if(art.isEmpty())art=text(cursor,"poster_art_uri");
                JSONObject item=new JSONObject().put("key",pkg+"|"+title.trim()).put("pkg",pkg).put("title",title.trim()).put("provider",LauncherinMediaService.provider(pkg))
                        .put("position",position).put("duration",duration).put("updated",number(cursor,"last_engagement_time_utc_millis",0)).put("art",art).put("intent_uri",text(cursor,"intent_uri")).put("source","watchnext");
                result.put(item);
            }
            status(context,"Lista pública consultada",result.length());
        }catch(SecurityException error){status(context,"O firmware restringiu a leitura da lista",0);return null;}catch(Exception error){status(context,"Lista nativa indisponível neste aparelho",0);return null;}
        return result;
    }
    private static void status(Context context,String status,int count){context.getSharedPreferences("media-diagnostic",0).edit().putString("watchnext.status",status).putInt("watchnext.count",count).apply();}
}
