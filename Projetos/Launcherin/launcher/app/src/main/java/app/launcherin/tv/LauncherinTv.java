package app.launcherin.tv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.content.ComponentName;
import java.util.*;
import android.view.KeyEvent;

/** Native tuner and remote actions. Never send a broadcast-TV request to an arbitrary IPTV handler. */
final class LauncherinTv {
    static final int NONE = 0, SETTINGS = 1, LIVE_TV = 2;
    static final String ROOT_CHANNEL_URI="content://android.media.tv/channel";
    private static final String[] NATIVE_PACKAGES = {
            "com.tcl.tv", "com.mediatek.wwtv.tvcenter", "com.android.tv", "com.google.android.tv"
    };
    private LauncherinTv() {}

    static int remoteAction(int key, boolean tcl, int[] settingsKeys) {
        if (key == KeyEvent.KEYCODE_SETTINGS) return SETTINGS;
        for (int configured : settingsKeys) if (key == configured) return SETTINGS;
        // Verified TCL Google TV keycodes in the source profiles; scancode 240 is shared by
        // unrelated buttons and must never be treated as a Settings shortcut.
        if (tcl && (key == 83 || key == 4020 || key == 4077)) return SETTINGS;
        // BT_RC833A_B2: examples/tcl-65c8l-google-tv/README.md reports TV guide as 4001.
        if (tcl && (key == 4001 || key == KeyEvent.KEYCODE_GUIDE)) return LIVE_TV;
        if (key == KeyEvent.KEYCODE_TV || key == KeyEvent.KEYCODE_TV_TERRESTRIAL_ANALOG
                || key == KeyEvent.KEYCODE_TV_TERRESTRIAL_DIGITAL) return LIVE_TV;
        return NONE;
    }

    static boolean nativePackage(String pkg) {
        for (String candidate : NATIVE_PACKAGES) if (candidate.equals(pkg)) return true;
        return false;
    }

    static String pickTuner(List<String> systemTunerIds, String configured) {
        boolean root=ROOT_CHANNEL_URI.equals(configured);
        String best=null;boolean bestHardware=false,bestAnalog=false;
        for(String id:systemTunerIds){
            if(id==null||(!configured.isEmpty()&&!root&&!configured.equals(id)))continue;
            boolean hardware=id.matches(".*/HW\\d+$"),analog=id.toLowerCase(Locale.ROOT).contains("analog");
            if(best==null||hardware&&!bestHardware||hardware==bestHardware&&bestAnalog&&!analog
                    ||hardware==bestHardware&&analog==bestAnalog&&id.compareTo(best)<0){
                best=id;bestHardware=hardware;bestAnalog=analog;
            }
        }
        return best;
    }

    static String tunerId(Context context,String configured) {
        ArrayList<String> ids=new ArrayList<>();
        TvInputManager manager=context.getSystemService(TvInputManager.class);
        if(manager!=null)try{for(TvInputInfo input:manager.getTvInputList())
            if(input.getType()==TvInputInfo.TYPE_TUNER
                    &&(input.getServiceInfo().applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)!=0)
                ids.add(input.getId());
        }catch(RuntimeException ignored){}
        return pickTuner(ids,configured);
    }

    /** The exact typed source request used by the Inputs menu, including the legacy OEM tuner route. */
    static Intent tunerIntent(String pkg,String tuner,String configured) {
        if(!nativePackage(pkg)||tuner==null||tuner.isEmpty())return null;
        boolean root=ROOT_CHANNEL_URI.equals(configured);
        boolean explicit=!configured.isEmpty()&&!root;
        return new Intent(Intent.ACTION_VIEW).setDataAndType(explicit
                ?TvContract.buildChannelsUriForInput(tuner):TvContract.buildChannelUriForPassthroughInput(tuner),
                explicit?TvContract.Channels.CONTENT_TYPE:TvContract.Channels.CONTENT_ITEM_TYPE).setPackage(pkg);
    }

    static Intent liveIntent(Context context,String configured,String action) {
        return liveIntent(context,configured,action,"");
    }

    static Intent liveIntent(Context context,String configured,String action,String input) {
        PackageManager pm=context.getPackageManager();String tuner=tunerId(context,input);
        LinkedHashSet<String> packages=new LinkedHashSet<>();
        if(nativePackage(configured))packages.add(configured);
        Collections.addAll(packages,NATIVE_PACKAGES);
        for(String pkg:packages)try{
            ApplicationInfo app=pm.getApplicationInfo(pkg,0);
            if(!app.enabled||(app.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))==0)continue;
            Intent tuned=tunerIntent(pkg,tuner,input);
            ComponentName handler=tuned==null?null:tuned.resolveActivity(pm);
            if(handler!=null){
                // Explicit root-channel profiles still use the handler resolved for the actual
                // tuner, never a generic root URI that can land in the vendor's streaming hub.
                return ROOT_CHANNEL_URI.equals(input)
                        ?new Intent(Intent.ACTION_VIEW,TvContract.Channels.CONTENT_URI).setComponent(handler):tuned;
            }
            Intent launch=pm.getLeanbackLaunchIntentForPackage(pkg);
            if(launch!=null)return launch;
            if(pkg.equals(configured)&&action!=null&&!action.isEmpty()){
                Intent custom=new Intent(action).setPackage(pkg);
                if(custom.resolveActivity(pm)!=null)return custom;
            }
            launch=pm.getLaunchIntentForPackage(pkg);if(launch!=null)return launch;
        }catch(PackageManager.NameNotFoundException|RuntimeException ignored){}
        return null;
    }
}
