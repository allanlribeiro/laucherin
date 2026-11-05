package app.launcherin.tv;

import android.content.*;
import android.content.pm.*;
import android.provider.Settings;
import android.widget.Toast;

/** Legacy Start-now, or public Dream settings on Android 14 where that activity became a no-op. */
final class LauncherinDreams {
    static final ComponentName START = new ComponentName("com.android.systemui","com.android.systemui.Somnambulator");
    static boolean canStartImmediately(){return LauncherinMusicIdlePolicy.legacyDreamShortcut(android.os.Build.VERSION.SDK_INT);}
    static boolean start(Context context, boolean explain) {
        if(!canStartImmediately()) {
            if(explain)try {
                context.startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                Toast.makeText(context,"No Android 14, configure o protetor aqui e aguarde o tempo de inatividade. O antigo atalho não inicia mais as fotos.",Toast.LENGTH_LONG).show();
            }catch(RuntimeException unavailable){Toast.makeText(context,"Abra o modo ambiente nas configurações da TV.",Toast.LENGTH_LONG).show();}
            return false;
        }
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
        text.append("Proteção da música: ").append(LauncherinIdle.musicStatus(c)).append("\n");
        text.append("Prazo auxiliar: ").append(LauncherinDreamDeadline.status()).append("\n");
        for(String key:names)text.append(key).append(": ").append(Settings.Secure.getString(c.getContentResolver(),key)).append('\n');
        return text.append("screen_off_timeout: ").append(Settings.System.getString(c.getContentResolver(),"screen_off_timeout")).toString();
    }
}
