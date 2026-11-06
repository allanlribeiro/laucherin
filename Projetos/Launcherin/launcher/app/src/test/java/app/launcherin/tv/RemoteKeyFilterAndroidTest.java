package app.launcherin.tv;

import android.accessibilityservice.AccessibilityServiceInfo;
import java.lang.reflect.Proxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

/** Exercise the Android service API across off/on changes without rebinding the helper. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class RemoteKeyFilterAndroidTest {
    private static final int CONNECTION = 4321;
    private Class<?> client;
    private HomeKeyService helper;
    private AccessibilityServiceInfo info;
    private int writes;

    @Before public void bindFrameworkConnection() throws Exception {
        helper=Robolectric.buildService(HomeKeyService.class).create().get();
        info=new AccessibilityServiceInfo();
        info.flags=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        Class<?> connectionType=Class.forName("android.accessibilityservice.IAccessibilityServiceConnection");
        Object connection=Proxy.newProxyInstance(connectionType.getClassLoader(),new Class<?>[]{connectionType},
                (proxy,method,args)->{
                    if(method.getName().equals("getServiceInfo"))return info;
                    if(method.getName().equals("setServiceInfo")){info=(AccessibilityServiceInfo)args[0];writes++;return null;}
                    if(method.getName().equals("asBinder"))return new android.os.Binder();
                    if(method.getReturnType()==boolean.class)return false;
                    if(method.getReturnType()==int.class)return 0;
                    if(method.getReturnType()==long.class)return 0L;
                    return null;
                });
        client=Class.forName("android.view.accessibility.AccessibilityInteractionClient");
        client.getMethod("addConnection",int.class,connectionType,boolean.class)
                .invoke(null,CONNECTION,connection,false);
        ReflectionHelpers.setField(helper,"mConnectionId",CONNECTION);
    }

    @After public void unbindFrameworkConnection() throws Exception {
        if(client!=null)client.getMethod("removeConnection",int.class).invoke(null,CONNECTION);
    }

    @Test public void optingInAfterAnEarlierOptOutReenablesKeyDeliveryWithoutRebinding() {
        RemoteKeys.applyKeyFilter(helper,false);
        assertEquals(0,helper.getServiceInfo().flags & AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS);
        RemoteKeys.applyKeyFilter(helper,true);
        assertNotEquals(0,helper.getServiceInfo().flags & AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS);
        assertNotEquals(0,helper.getServiceInfo().flags & AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS);
        assertEquals(2,writes);
        RemoteKeys.applyKeyFilter(helper,false);
        assertEquals(0,helper.getServiceInfo().flags & AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS);
        assertNotEquals(0,helper.getServiceInfo().flags & AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS);
        assertEquals(3,writes);
    }

    @Test public void repeatedRefreshesDoNotRewriteAnUnchangedServiceConfiguration() {
        RemoteKeys.applyKeyFilter(helper,true);assertEquals(0,writes);
        RemoteKeys.applyKeyFilter(helper,false);assertEquals(1,writes);
        RemoteKeys.applyKeyFilter(helper,false);assertEquals(1,writes);
    }

    @Test public void anUnboundServiceDoesNotCrashWhenAnOptionChanges() throws Exception {
        client.getMethod("removeConnection",int.class).invoke(null,CONNECTION);
        RemoteKeys.applyKeyFilter(helper,false);RemoteKeys.applyKeyFilter(helper,true);
        assertEquals(0,writes);
    }
}
