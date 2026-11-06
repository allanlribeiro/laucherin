package app.launcherin.tv;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import org.json.JSONObject;

/** Native photo timing, with a media-aware lease that suspends only the standby timeout. */
final class LauncherinIdle {
    private static final class MainHandler {static final Handler value=new Handler(Looper.getMainLooper());}
    private static Handler handler(){return MainHandler.value;}
    private static Context watched;
    private static ContentObserver observer;
    private static final Runnable recheck=()->{Context c=watched;if(c!=null)apply(c);};
    static final String SAVER = "idle_screensaver_minutes", SLEEP = "idle_standby_minutes";
    private static final String[] KEYS = {"screen_off_timeout", "sleep_timeout", "attentive_timeout",
            "screensaver_enabled", "screensaver_activate_on_sleep"};
    static int minutes(int value) { return Math.max(0, Math.min(1440, value)); }
    static int saver(Context c) { return minutes(LauncherinRuntimePrefs.read(c,SAVER)); }
    static int sleep(Context c) { return minutes(LauncherinRuntimePrefs.read(c,SLEEP)); }
    static boolean valid(int saver, int sleep) { return saver == 0 || sleep == 0 || sleep > saver; }
    static boolean canApply(Context c) {
        return c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED
                && Settings.System.canWrite(c);
    }
    static String read(Context c, int i) {
        return i == 0 ? Settings.System.getString(c.getContentResolver(), KEYS[i])
                : Settings.Secure.getString(c.getContentResolver(), KEYS[i]);
    }
    private static boolean write(Context c, int i, String value) {
        return i == 0 ? Settings.System.putString(c.getContentResolver(), KEYS[i], value)
                : Settings.Secure.putString(c.getContentResolver(), KEYS[i], value);
    }
    /** Back up once; restore only values still owned by this setting, preserving external changes. */
    static String apply(Context c) {return apply(c,LauncherinMediaService.musicInProgress(c));}
    static synchronized String apply(Context c,Boolean music) {
        SharedPreferences prefs = c.getSharedPreferences("launcherin-idle", 0);
        int saver = saver(c), sleep = sleep(c);
        if (!valid(saver, sleep)) return "O desligamento deve ocorrer depois do protetor de tela.";
        String desired = saver + ":" + sleep;
        if (saver == 0 && sleep == 0 && !prefs.contains("backup")) return "";
        if (!canApply(c)) return "Libere as permissões dos temporizadores para aplicar os tempos.";
        try {
            JSONObject backup = new JSONObject(prefs.getString("backup", "{}"));
            JSONObject owned = new JSONObject(prefs.getString("owned", "{}"));
            if (saver == 0 && sleep == 0) {
                handler().removeCallbacks(recheck);
                for (int i = 0; i < KEYS.length; i++) if (owned.has(KEYS[i])
                        && java.util.Objects.equals(read(c, i), owned.optString(KEYS[i]))) {
                    if (!write(c, i, backup.isNull(KEYS[i]) ? null : backup.getString(KEYS[i])))
                        throw new IllegalStateException("Restauração recusada");
                    owned.remove(KEYS[i]);
                    prefs.edit().putString("owned", owned.toString()).commit();
                }
                prefs.edit().clear().commit();
                return "";
            }
            boolean held=prefs.getBoolean("music_hold",false);
            long pausedAt=prefs.getLong("music_paused_at",-1);
            int boot=Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1);
            if(boot!=prefs.getInt("music_boot",boot))pausedAt=-1;
            LauncherinMusicIdlePolicy.Decision guard=LauncherinMusicIdlePolicy.decide(held,pausedAt,music,SystemClock.elapsedRealtime(),sleep);
            String sleepValue=guard.hold?"-1":sleep>0?Long.toString(sleep*60000L):"-1";
            if (desired.equals(prefs.getString("applied", ""))) {
                // Unrelated external changes remain untouched without a protection lease.
                if(guard.hold || held && "-1".equals(read(c,1))
                        || !guard.hold && "-1".equals(owned.optString(KEYS[1])) && "-1".equals(read(c,1))) {
                    if(!sleepValue.equals(read(c,1))) {
                        if(!write(c,1,sleepValue))throw new IllegalStateException("Proteção da música recusada");
                        owned.put(KEYS[1],sleepValue);
                    }
                }
                saveGuard(c,prefs,owned,guard,boot);
                return sleep>0&&music==null?"Ative o acesso a mídias: o desligamento fica suspenso enquanto a reprodução não puder ser verificada.":"";
            }
            if (!prefs.contains("backup")) {
                for (int i = 0; i < KEYS.length; i++) backup.put(KEYS[i], read(c, i) == null ? JSONObject.NULL : read(c, i));
                if (!prefs.edit().putString("backup", backup.toString()).commit()) throw new IllegalStateException();
            }
            String[] values = {Long.toString((saver > 0 ? saver : sleep) * 60000L),
                    sleepValue, "-1", saver > 0 ? "1" : "0", "1"};
            for (int i = 0; i < KEYS.length; i++) {
                if (!write(c, i, values[i])) throw new IllegalStateException("Gravação recusada");
                owned.put(KEYS[i], values[i]);
                if (!prefs.edit().putString("owned", owned.toString()).commit()) throw new IllegalStateException();
            }
            prefs.edit().putString("applied", desired).commit();
            saveGuard(c,prefs,owned,guard,boot);
            return sleep>0&&music==null?"Ative o acesso a mídias: o desligamento fica suspenso enquanto a reprodução não puder ser verificada.":"";
        } catch (Exception error) {
            prefs.edit().remove("applied").apply();
            return "A TV recusou os tempos. Verifique as permissões e tente novamente.";
        }
    }
    private static void saveGuard(Context context,SharedPreferences prefs,JSONObject owned,LauncherinMusicIdlePolicy.Decision guard,int boot) {
        if((!owned.toString().equals(prefs.getString("owned","")) || guard.hold!=prefs.getBoolean("music_hold",false)
                || guard.pausedAt!=prefs.getLong("music_paused_at",-1) || boot!=prefs.getInt("music_boot",-1))
                && !prefs.edit().putString("owned",owned.toString()).putBoolean("music_hold",guard.hold)
                .putLong("music_paused_at",guard.pausedAt).putInt("music_boot",boot).commit())
            throw new IllegalStateException("Proteção não salva");
        if(guard.hold)LauncherinDreamDeadline.cancel();
        handler().removeCallbacks(recheck);
        if(guard.checkAfter>=0){if(watched==null)watched=context.getApplicationContext();handler().postDelayed(recheck,guard.checkAfter);}
    }
    static String listen(Context c) {
        Context app=c.getApplicationContext();if(watched==app&&observer!=null)return apply(app);
        stopListening();watched=app;
        observer=new ContentObserver(handler()){@Override public void onChange(boolean self){
            Context owner=watched;if(owner!=null&&owner.getSharedPreferences("launcherin-idle",0).getBoolean("music_hold",false)) {
                handler().removeCallbacks(recheck);handler().post(recheck);
            }
        }};
        try{watched.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("sleep_timeout"),false,observer);}
        catch(RuntimeException unavailable){observer=null;}
        return apply(watched);
    }
    static void stopListening() {
        handler().removeCallbacks(recheck);
        if(watched!=null&&observer!=null)watched.getContentResolver().unregisterContentObserver(observer);
        watched=null;observer=null;
        // A held timeout stays suspended until a fresh media snapshot is available.
    }
    static String musicStatus(Context c) {
        SharedPreferences prefs=c.getSharedPreferences("launcherin-idle",0);
        if(!prefs.getBoolean("music_hold",false))return "temporizador normal";
        long paused=prefs.getLong("music_paused_at",-1);
        if(paused<0)return Boolean.TRUE.equals(LauncherinMediaService.musicInProgress(c))
                ?"música ativa; apenas o desligamento está suspenso":"desligamento suspenso; aguardando acesso ou estado de mídia";
        long seconds=Math.max(0,(paused+sleep(c)*60000L-SystemClock.elapsedRealtime()+999)/1000);
        return "proteção após pausar: "+seconds+" s restantes";
    }
    static boolean protectsMusic(Context c){return c.getSharedPreferences("launcherin-idle",0).getBoolean("music_hold",false);}
}
