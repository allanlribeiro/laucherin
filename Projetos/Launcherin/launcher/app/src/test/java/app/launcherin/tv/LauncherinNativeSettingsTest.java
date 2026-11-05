package app.launcherin.tv;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.view.KeyEvent;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowBuild;
import static org.junit.Assert.*;

/** Android intent and key integration; actual TCL firmware behavior also needs a physical key test. */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=35)
public class LauncherinNativeSettingsTest {
    private Application app;
    private ServiceInfo window;

    @Before public void prepare() {
        app=RuntimeEnvironment.getApplication();
        ShadowBuild.setManufacturer("TCL");
        ShadowBuild.setBrand("TCL");
        PackageInfo pkg=new PackageInfo();
        pkg.packageName=LauncherinNativeSettings.TCL_WINDOW.getPackageName();
        pkg.applicationInfo=new ApplicationInfo();
        pkg.applicationInfo.packageName=pkg.packageName;
        pkg.applicationInfo.enabled=true;
        pkg.applicationInfo.flags=ApplicationInfo.FLAG_SYSTEM;
        window=new ServiceInfo();window.packageName=pkg.packageName;
        window.name=LauncherinNativeSettings.TCL_WINDOW.getClassName();
        window.applicationInfo=pkg.applicationInfo;window.enabled=true;window.exported=true;
        pkg.services=new ServiceInfo[]{window};
        Shadows.shadowOf(app.getPackageManager()).installPackage(pkg);
        app.getSharedPreferences("launcherin-remote",0).edit().clear().commit();
        File dir=Config.dir(app);
        if(dir!=null)new File(dir,"config.json").delete();
        LauncherinRemote.refresh(app);
    }

    @Test public void nativePanelUsesTheVerifiedSidePanelWithoutStartingAnyActivity() {
        assertTrue(LauncherinNativeSettings.open(app,false));
        Intent sent=Shadows.shadowOf(app).getNextStartedService();
        assertNotNull(sent);assertEquals(LauncherinNativeSettings.TCL_WINDOW,sent.getComponent());
        assertEquals("com.tcl.settings.SHOW_WINDOW",sent.getAction());
        assertEquals("Settings",sent.getStringExtra("Type"));
        assertEquals("picture",sent.getStringExtra("Catagory"));
        assertNull(Shadows.shadowOf(app).getNextStartedActivity());
    }

    @Test public void explicitlyChosenLauncherPanelIsPreserved() {
        assertTrue(LauncherinNativeSettings.open(app,true));
        assertNull(Shadows.shadowOf(app).getNextStartedService());
        assertEquals(QuickSettingsActivity.class.getName(),
                Shadows.shadowOf(app).getNextStartedActivity().getComponent().getClassName());
    }

    @Test public void nonTclDevicesNeverCallTheOemService() {
        ShadowBuild.setManufacturer("Other");ShadowBuild.setBrand("Other");
        assertTrue(LauncherinNativeSettings.open(app,false));
        assertNull(Shadows.shadowOf(app).getNextStartedService());
        assertNotNull(Shadows.shadowOf(app).getNextStartedActivity());
    }

    @Test public void disabledUnexportedOrNonSystemServicesAreNotUsed() {
        window.exported=false;assertFalse(LauncherinNativeSettings.available(app));
        window.exported=true;window.enabled=false;assertFalse(LauncherinNativeSettings.available(app));
        window.enabled=true;window.applicationInfo.flags=0;assertFalse(LauncherinNativeSettings.available(app));
    }

    @Test public void aSignatureProtectedServiceIsNotUsed() {
        window.permission="com.tcl.settings.permission.ShortCutMenuReceiver";
        assertFalse(LauncherinNativeSettings.available(app));
    }

    @Test public void backgroundServiceDenialFallsBackOnceWithoutGoingHome() {
        Context denied=new ContextWrapper(app) {
            @Override public ComponentName startService(Intent intent) {throw new SecurityException("Denied");}
        };
        assertTrue(LauncherinNativeSettings.open(denied,false));
        Intent fallback=Shadows.shadowOf(app).getNextStartedActivity();
        assertNotNull(fallback);
        assertFalse(fallback.hasCategory(Intent.CATEGORY_HOME));
        assertNull(Shadows.shadowOf(app).getNextStartedActivity());
    }

    @Test public void globalHelperOpensPanelOnlyWhenGearIsReleased() throws Exception {
        LauncherinExtras.setInt(app,LauncherinRemote.SETTINGS_FILTER,1);LauncherinRemote.refresh(app);
        HomeKeyService helper=Robolectric.buildService(HomeKeyService.class).create().get();
        assertTrue(helper.onKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,83)));
        assertNull(Shadows.shadowOf(app).getNextStartedService());
        assertTrue(helper.onKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,83)));
        assertEquals(LauncherinNativeSettings.TCL_WINDOW,
                Shadows.shadowOf(app).getNextStartedService().getComponent());
        assertNull(Shadows.shadowOf(app).getNextStartedActivity());
        assertFalse(helper.onKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_UP)));
        assertFalse(helper.onKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,4020)));
    }

    @Test public void cancelledGearPressDoesNotOpenThePanel() throws Exception {
        LauncherinExtras.setInt(app,LauncherinRemote.SETTINGS_FILTER,1);LauncherinRemote.refresh(app);
        HomeKeyService helper=Robolectric.buildService(HomeKeyService.class).create().get();
        KeyEvent cancelled=new KeyEvent(0,0,KeyEvent.ACTION_UP,83,0,0,0,0,KeyEvent.FLAG_CANCELED);
        assertTrue(helper.onKeyEvent(cancelled));
        assertNull(Shadows.shadowOf(app).getNextStartedService());
        assertNull(Shadows.shadowOf(app).getNextStartedActivity());
    }

    @Test public void tvKeyOptInAloneDoesNotHijackTheTclGearButton() throws Exception {
        LauncherinExtras.setInt(app,LauncherinRemote.FILTER,1);LauncherinRemote.refresh(app);
        HomeKeyService helper=Robolectric.buildService(HomeKeyService.class).create().get();
        assertFalse(helper.onKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,83)));
        assertNull(Shadows.shadowOf(app).getNextStartedService());
    }

    @Test public void globalSettingsOptInIsIndependentFromLiveTvAndSurvivesThemeReplacement() throws Exception {
        assertFalse(LauncherinRemote.anyGlobal(app));
        LauncherinExtras.setInt(app,LauncherinRemote.SETTINGS_FILTER,1);LauncherinRemote.refresh(app);
        assertTrue(LauncherinRemote.settingsGlobal(app));assertTrue(LauncherinRemote.anyGlobal(app));
        assertFalse(LauncherinRemote.global(app));
        new File(Config.dir(app),"config.json").delete();LauncherinRemote.refresh(app);
        assertTrue(LauncherinRemote.settingsGlobal(app));
        LauncherinExtras.setInt(app,LauncherinRemote.SETTINGS_FILTER,0);LauncherinRemote.refresh(app);
        assertFalse(LauncherinRemote.anyGlobal(app));
    }
}
