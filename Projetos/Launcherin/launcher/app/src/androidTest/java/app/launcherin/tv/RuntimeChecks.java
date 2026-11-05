package app.launcherin.tv;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import org.json.JSONObject;

/** Real SettingsProvider, KeyEvent dispatcher and overlay; synthetic input is not a physical TCL test. */
final class RuntimeChecks {
    final Instrumentation test; final Context context;
    RuntimeChecks(Instrumentation test){this.test=test;context=test.getTargetContext();}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    void timers() throws Exception {
        require(LauncherinIdle.canApply(context),"Grant native timer permissions on the emulator first");
        File config=new File(Config.dir(context),"config.json");
        byte[] original=config.isFile()?java.nio.file.Files.readAllBytes(config.toPath()):null;
        String[] before=new String[5];for(int i=0;i<5;i++)before[i]=LauncherinIdle.read(context,i);
        try {
            LauncherinExtras.setInt(context,LauncherinIdle.SAVER,5);LauncherinExtras.setInt(context,LauncherinIdle.SLEEP,30);
            require(LauncherinIdle.apply(context).isEmpty(),"Timers refused");
            require("300000".equals(LauncherinIdle.read(context,0)),"Wrong screensaver time");
            require("1800000".equals(LauncherinIdle.read(context,1)),"Wrong standby time");
            require("1".equals(LauncherinIdle.read(context,3)),"Dream disabled");
            JSONObject appearance=LauncherinExtras.read(context);
            appearance.remove(LauncherinIdle.SAVER);appearance.remove(LauncherinIdle.SLEEP);
            java.nio.file.Files.write(config.toPath(),appearance.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            require(LauncherinIdle.saver(context)==5 && LauncherinIdle.sleep(context)==30,"Appearance-only update erased timer choices");
            require(LauncherinIdle.apply(context).isEmpty(),"Appearance-only update reset native timers");
            LauncherinExtras.setInt(context,LauncherinIdle.SAVER,0);LauncherinExtras.setInt(context,LauncherinIdle.SLEEP,0);
            require(LauncherinIdle.apply(context).isEmpty(),"Restore refused");
            for(int i=0;i<5;i++)require(java.util.Objects.equals(before[i],LauncherinIdle.read(context,i)),"Original setting lost at "+i);
            LauncherinExtras.setInt(context,LauncherinIdle.SAVER,5);LauncherinExtras.setInt(context,LauncherinIdle.SLEEP,30);
            require(LauncherinIdle.apply(context).isEmpty(),"Timers refused on second use");
            Settings.System.putString(context.getContentResolver(),"screen_off_timeout","420000");
            LauncherinExtras.setInt(context,LauncherinIdle.SAVER,0);LauncherinExtras.setInt(context,LauncherinIdle.SLEEP,0);
            require(LauncherinIdle.apply(context).isEmpty(),"Conditional restore refused");
            require("420000".equals(LauncherinIdle.read(context,0)),"Overwrote external screen-time change");
        } finally {
            Settings.System.putString(context.getContentResolver(),"screen_off_timeout",before[0]);
            if(original==null)java.nio.file.Files.deleteIfExists(config.toPath());else java.nio.file.Files.write(config.toPath(),original);
        }
    }
    void selectedDream() throws Exception {
        String original=Settings.Secure.getString(context.getContentResolver(),"screensaver_components");
        File config=new File(Config.dir(context),"config.json");byte[] bytes=java.nio.file.Files.readAllBytes(config.toPath());
        java.util.concurrent.atomic.AtomicBoolean started=new java.util.concurrent.atomic.AtomicBoolean(),stopped=new java.util.concurrent.atomic.AtomicBoolean();
        android.content.BroadcastReceiver receiver=new android.content.BroadcastReceiver(){public void onReceive(Context c,Intent i){if(Intent.ACTION_DREAMING_STARTED.equals(i.getAction()))started.set(true);else stopped.set(true);}};
        android.content.IntentFilter dreamEvents=new android.content.IntentFilter(Intent.ACTION_DREAMING_STARTED);dreamEvents.addAction(Intent.ACTION_DREAMING_STOPPED);
        context.registerReceiver(receiver,dreamEvents);
        try {
            test.runOnMainSync(LauncherinMediaService::cancelMusicCommand);
            for(String pkg:new String[]{"com.spotify.music","com.google.android.youtube.tv.music"})context.stopService(new Intent().setComponent(new android.content.ComponentName(pkg,"app.launcherin.fixture.MusicService")));
            long end=SystemClock.elapsedRealtime()+10000;while(LauncherinMediaService.playbackInProgress(context)&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
            LauncherinExtras.setInt(context,LauncherinIdle.SAVER,1);LauncherinExtras.setInt(context,LauncherinIdle.SLEEP,3);
            Settings.Secure.putString(context.getContentResolver(),"screensaver_components","app.launcherin.tv/.ClockDream");
            MainActivity home=(MainActivity)test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            end=SystemClock.elapsedRealtime()+10000;while(!home.hasWindowFocus()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
            require(home.hasWindowFocus(),"Home not focused for idle test");
            java.lang.reflect.Field field=MainActivity.class.getDeclaredField("showPhotoSaver");field.setAccessible(true);
            test.runOnMainSync(()->{try{((Runnable)field.get(home)).run();}catch(Exception e){throw new RuntimeException(e);}});
            end=SystemClock.elapsedRealtime()+12000;while(!started.get()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
            require(started.get(),"Selected OS dream did not start from Home idle callback");
            // A real injected input wakes a non-interactive DreamService; no production injection permission.
            test.getUiAutomation().injectInputEvent(new KeyEvent(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0),true);
            test.getUiAutomation().injectInputEvent(new KeyEvent(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0),true);
            end=SystemClock.elapsedRealtime()+8000;while(!stopped.get()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
            require(stopped.get(),"Previous dream still stopping before next test");
        } finally {
            context.unregisterReceiver(receiver);
            LauncherinExtras.setInt(context,LauncherinIdle.SAVER,0);LauncherinExtras.setInt(context,LauncherinIdle.SLEEP,0);LauncherinIdle.apply(context);
            Settings.Secure.putString(context.getContentResolver(),"screensaver_components",original);
            java.nio.file.Files.write(config.toPath(),bytes);
        }
    }
    void standbyDuringDream() throws Exception {
        // UiAutomation is the authorized Android accessibility service in this test process.
        android.app.UiAutomation automation=test.getUiAutomation();
        String[] keys={"screensaver_components","screensaver_enabled","screensaver_activate_on_sleep"};
        String[] values=new String[keys.length];for(int i=0;i<keys.length;i++)values[i]=Settings.Secure.getString(context.getContentResolver(),keys[i]);
        java.util.concurrent.atomic.AtomicBoolean off=new java.util.concurrent.atomic.AtomicBoolean();
        android.content.BroadcastReceiver receiver=new android.content.BroadcastReceiver(){public void onReceive(Context c,Intent i){off.set(true);}};
        context.registerReceiver(receiver,new android.content.IntentFilter(Intent.ACTION_SCREEN_OFF));
        try {
            Settings.Secure.putString(context.getContentResolver(),keys[0],"app.launcherin.tv/.ClockDream");
            Settings.Secure.putString(context.getContentResolver(),keys[1],"1");Settings.Secure.putString(context.getContentResolver(),keys[2],"1");
            final boolean[] armed={false};test.runOnMainSync(()->{armed[0]=LauncherinDreamDeadline.arm(context,SystemClock.elapsedRealtime()+20000,
                ()->automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN));
                require(LauncherinDreams.start(context,true),"No dream shortcut");});
            require(armed[0],"Original standby deadline not armed");
            long end=SystemClock.elapsedRealtime()+25000;while(!off.get()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
            require(off.get(),"Starting the dream delayed the original standby deadline: "+LauncherinDreamDeadline.status());
        } finally {
            test.runOnMainSync(LauncherinDreamDeadline::cancel);
            automation.injectInputEvent(new KeyEvent(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_WAKEUP,0),true);
            automation.injectInputEvent(new KeyEvent(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_WAKEUP,0),true);
            for(int i=0;i<keys.length;i++)Settings.Secure.putString(context.getContentResolver(),keys[i],values[i]);
            context.unregisterReceiver(receiver);
        }
    }
    void keyLearning() throws Exception {
        test.getUiAutomation();
        int original=LauncherinRemote.key(context);
        LauncherinRemoteActivity activity=(LauncherinRemoteActivity)test.startActivitySync(new Intent(context,LauncherinRemoteActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            test.runOnMainSync(()->{activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,4001));activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,4001));});
            long until=SystemClock.elapsedRealtime()+15000;boolean clicked=false;
            while(SystemClock.elapsedRealtime()<until&&!clicked){
                AccessibilityNodeInfo root=test.getUiAutomation().getRootInActiveWindow();
                if(root!=null)for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText("Salvar"))
                    if("Salvar".equalsIgnoreCase(String.valueOf(node.getText())))clicked=node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if(!clicked)SystemClock.sleep(150);
            }
            require(clicked,"Save confirmation not visible");
            test.waitForIdleSync();LauncherinRemote.refresh(context);
            require(LauncherinRemote.key(context)==4001,"Actual dispatcher did not persist learned key");
            require(!LauncherinRemote.global(context),"Learning silently enabled global filtering");
        } finally {LauncherinExtras.setInt(context,LauncherinRemote.KEY,original);LauncherinRemote.refresh(context);test.runOnMainSync(activity::finish);}
    }
    void overlay() throws Exception {
        require(Settings.canDrawOverlays(context),"Grant overlay access on the emulator first");
        JSONObject original=LauncherinExtras.read(context);
        File config=new File(Config.dir(context),"config.json");
        final LauncherinGoalMonitor[] monitor={null};
        try {
            LauncherinExtras.setArray(context,LauncherinSports.KEY,new org.json.JSONArray().put("819"));
            LauncherinExtras.setInt(context,LauncherinGoals.KEY,1);
            test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("launcherin_preview",true));
            test.waitForIdleSync();SystemClock.sleep(1200);
            test.runOnMainSync(()->{monitor[0]=new LauncherinGoalMonitor(context);monitor[0].announce("819","Gol · demonstração","Time favorito  1 × 0  Adversário");});
            java.lang.reflect.Field field=LauncherinGoalMonitor.class.getDeclaredField("showing");field.setAccessible(true);
            View card=(View)field.get(monitor[0]);require(card!=null,"No native overlay");
            WindowManager.LayoutParams params=(WindowManager.LayoutParams)card.getLayoutParams();
            require((params.flags&WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)!=0,"Overlay steals remote focus");
            require((params.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)!=0,"Overlay intercepts input");
            require(params.gravity==(android.view.Gravity.TOP|android.view.Gravity.END),"Wrong corner");
            SystemClock.sleep(300);
            android.graphics.Bitmap shot=test.getUiAutomation().takeScreenshot();require(shot!=null,"No screenshot");
            try(java.io.FileOutputStream out=context.openFileOutput("dashboard-preview7-goal.png",0)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}shot.recycle();
        } finally {
            if(monitor[0]!=null)test.runOnMainSync(monitor[0]::stop);
            java.nio.file.Files.write(config.toPath(),original.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
