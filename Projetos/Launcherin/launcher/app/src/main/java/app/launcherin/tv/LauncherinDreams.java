package app.launcherin.tv;

import android.content.*;
import android.content.pm.*;
import android.provider.Settings;
import android.widget.Toast;

/** Public SystemUI Start-now activity: starts the user's selected dream without selecting another album. */
final class LauncherinDreams {
    static final ComponentName START = new ComponentName("com.android.systemui","com.android.systemui.Somnambulator");
    static boolean start(Context context, boolean explain) {
        try {
            ActivityInfo activity=context.getPackageManager().getActivityInfo(START,0);
            boolean system=(activity.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;
            if(!system||!activity.exported||!activity.enabled||!activity.applicationInfo.enabled
                    ||activity.permission!=null&&context.checkSelfPermission(activity.permission)!=PackageManager.PERMISSION_GRANTED)
                throw new SecurityException("Start-now unavailable");
            context.startActivity(new Intent(Intent.ACTION_MAIN).setComponent(START).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch(PackageManager.NameNotFoundException|RuntimeException unavailable) {
            if(explain)Toast.makeText(context,"A TV não disponibilizou o atalho de iniciar o protetor. Use Visualizar no modo ambiente e envie o diagnóstico.",Toast.LENGTH_LONG).show();
            return false;
        }
    }
    static String selected(Context context) {
        String selected=Settings.Secure.getString(context.getContentResolver(),"screensaver_components");
        if(selected==null||selected.isEmpty())selected=Settings.Secure.getString(context.getContentResolver(),"screensaver_default_component");
        if(selected!=null)for(String value:selected.split(",")) {
            ComponentName component=ComponentName.unflattenFromString(value);
            if(component!=null)try {
                ServiceInfo service=context.getPackageManager().getServiceInfo(component,0);
                return service.applicationInfo.loadLabel(context.getPackageManager()).toString();
            } catch(PackageManager.NameNotFoundException ignored){}
        }
        return "Padrão do sistema";
    }
    static String diagnostic(Context c) {
        String[] names={"screensaver_components","screensaver_default_component","screensaver_enabled","screensaver_activate_on_sleep","sleep_timeout","attentive_timeout"};
        StringBuilder text=new StringBuilder("Protetor: "+selected(c)+"\nDesligamento durante o protetor: "+(HomeKeyService.canLockScreen()?"auxiliar disponível":"temporizador do sistema")+"\n");
        text.append("Prazo auxiliar: ").append(LauncherinDreamDeadline.status()).append("\n");
        for(String key:names)text.append(key).append(": ").append(Settings.Secure.getString(c.getContentResolver(),key)).append('\n');
        return text.append("screen_off_timeout: ").append(Settings.System.getString(c.getContentResolver(),"screen_off_timeout")).toString();
    }
}
