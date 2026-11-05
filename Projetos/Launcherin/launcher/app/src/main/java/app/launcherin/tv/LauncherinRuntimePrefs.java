package app.launcherin.tv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Omitted operational keys keep the TV choice; an explicit zero disables it. */
final class LauncherinRuntimePrefs {
    static int resolve(JSONObject config,String key,int previous) {
        return config.has(key) && !config.isNull(key) ? config.optInt(key,previous) : previous;
    }
    static int read(Context context, String key) {
        SharedPreferences saved = context.getSharedPreferences("launcherin-runtime-options",0);
        JSONObject config = LauncherinExtras.read(context);
        int previous = saved.getInt(key,0);
        int value = resolve(config,key,previous);
        if (value != previous) saved.edit().putInt(key,value).apply();
        return value;
    }
}
