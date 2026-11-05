package app.launcherin.tv;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.provider.Settings;
import android.view.KeyEvent;
import java.util.Locale;

/** The native TCL side panel, verified on the user's firmware, without bringing Home to the front. */
final class LauncherinNativeSettings {
    static final ComponentName TCL_WINDOW = new ComponentName(
            "com.android.tv.settings", "com.tcl.settings.ShowWindowService");
    private LauncherinNativeSettings() {}

    static boolean tcl() {
        return (Build.MANUFACTURER + " " + Build.BRAND).toLowerCase(Locale.ROOT).contains("tcl");
    }

    static boolean gear(int code, boolean tcl) {
        // 4020 is also TCL Channel on some remotes; it must not become a global Settings shortcut.
        return code == KeyEvent.KEYCODE_SETTINGS || (tcl && code == 83);
    }

    static Intent nativeIntent() {
        // "quicksettings" opens the bottom bar on this firmware, not the requested side panel.
        // Catagory is the OEM's spelling, as used by its own ShortcutKey receiver.
        return new Intent("com.tcl.settings.SHOW_WINDOW").setComponent(TCL_WINDOW)
                .putExtra("Type", "Settings").putExtra("Catagory", "picture");
    }

    static boolean available(Context context) {
        if (!tcl()) return false;
        try {
            ServiceInfo service = context.getPackageManager().getServiceInfo(TCL_WINDOW, 0);
            ApplicationInfo app = service.applicationInfo;
            return service.enabled && service.exported && app != null && app.enabled
                    && (app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                    && (service.permission == null || service.permission.isEmpty()
                        || context.checkSelfPermission(service.permission) == PackageManager.PERMISSION_GRANTED);
        } catch (PackageManager.NameNotFoundException | RuntimeException unavailable) {
            return false;
        }
    }

    static boolean open(Context context, boolean launcherPanel) {
        // Preserve an explicitly configured Launcherin panel. Generic Settings screens remain unchanged.
        if (!launcherPanel && available(context)) {
            try {
                if (context.startService(nativeIntent()) != null) return true;
            } catch (RuntimeException unavailable) {
                // The shell test alone cannot establish background-service access for an ordinary app.
            }
        }
        try {
            Intent fallback = launcherPanel ? new Intent(context, QuickSettingsActivity.class)
                    : SystemSettings.intent(context, Settings.ACTION_SETTINGS);
            context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (RuntimeException unavailable) {
            return false;
        }
    }
}
