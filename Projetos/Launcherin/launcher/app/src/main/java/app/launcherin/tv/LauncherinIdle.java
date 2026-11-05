package app.launcherin.tv;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Native power timers see remote activity in every app and respect media wake locks. */
final class LauncherinIdle {
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
    static synchronized String apply(Context c) {
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
            if (desired.equals(prefs.getString("applied", ""))) return "";
            if (!prefs.contains("backup")) {
                for (int i = 0; i < KEYS.length; i++) backup.put(KEYS[i], read(c, i) == null ? JSONObject.NULL : read(c, i));
                if (!prefs.edit().putString("backup", backup.toString()).commit()) throw new IllegalStateException();
            }
            String[] values = {Long.toString((saver > 0 ? saver : sleep) * 60000L),
                    sleep > 0 ? Long.toString(sleep * 60000L) : "-1", "-1", saver > 0 ? "1" : "0", "1"};
            for (int i = 0; i < KEYS.length; i++) {
                if (!write(c, i, values[i])) throw new IllegalStateException("Gravação recusada");
                owned.put(KEYS[i], values[i]);
                if (!prefs.edit().putString("owned", owned.toString()).commit()) throw new IllegalStateException();
            }
            prefs.edit().putString("applied", desired).commit();
            return "";
        } catch (Exception error) {
            prefs.edit().remove("applied").apply();
            return "A TV recusou os tempos. Verifique as permissões e tente novamente.";
        }
    }
}
