package app.launcherin.tv;

import android.content.Context;
import android.view.KeyEvent;

/** Device-specific TV key, learned from the actual framework KeyEvent, never an evdev guess. */
final class LauncherinRemote {
    static final String KEY = "live_tv_remote_keycode", FILTER = "live_tv_remote_global";
    static final String SETTINGS_FILTER = "settings_remote_global";
    private static volatile int learnedKey;
    private static volatile boolean filter;
    private static volatile boolean settingsFilter;
    static void refresh(Context context) {
        org.json.JSONObject config=LauncherinExtras.read(context);
        android.content.SharedPreferences saved=context.getSharedPreferences("launcherin-remote",0);
        learnedKey=config.optInt(KEY,saved.getInt(KEY,0));
        filter=config.optInt(FILTER,saved.getInt(FILTER,0))==1;
        settingsFilter=config.optInt(SETTINGS_FILTER,saved.getInt(SETTINGS_FILTER,0))==1;
        saved.edit().putInt(KEY,learnedKey).putInt(FILTER,filter?1:0)
                .putInt(SETTINGS_FILTER,settingsFilter?1:0).apply();
    }
    static int key(Context context) { return learnedKey; }
    static boolean global(Context context) { return filter; }
    static boolean settingsGlobal(Context context) { return settingsFilter; }
    static boolean anyGlobal(Context context) { return filter || settingsFilter; }
    static boolean learnable(int key) {
        if (key <= 0 || key > 9999) return false;
        switch (key) {
            case KeyEvent.KEYCODE_HOME: case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_SLEEP: case KeyEvent.KEYCODE_WAKEUP: case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN: case KeyEvent.KEYCODE_VOLUME_MUTE: case KeyEvent.KEYCODE_MUTE:
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:
            case 83: case 4020: case 4077:
            case KeyEvent.KEYCODE_SETTINGS: case KeyEvent.KEYCODE_MENU: case KeyEvent.KEYCODE_ASSIST:
            case KeyEvent.KEYCODE_VOICE_ASSIST: case KeyEvent.KEYCODE_TV_INPUT:
            case KeyEvent.KEYCODE_MEDIA_PLAY: case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: return false;
            default: return true;
        }
    }
    static boolean matches(Context context, KeyEvent event) {
        int learned = key(context);
        return learnable(learned) && event.getKeyCode() == learned;
    }
}
