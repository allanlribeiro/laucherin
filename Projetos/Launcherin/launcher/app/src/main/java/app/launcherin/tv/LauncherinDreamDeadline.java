package app.launcherin.tv;

import android.content.*;
import android.os.*;

/** Somnambulator pokes user activity. Keep the user's original standby deadline while dreaming. */
final class LauncherinDreamDeadline {
    private static Context context;
    private static BroadcastReceiver receiver;
    private static boolean dreaming;
    private static String status="não armado";
    static String status(){return status;}
    private static final Handler handler=new Handler(Looper.getMainLooper());
    static boolean arm(Context owner,long deadline) {
        if(!HomeKeyService.canLockScreen())return false;
        return arm(owner,deadline,HomeKeyService::lockScreen);
    }
    static boolean arm(Context owner,long deadline,java.util.function.BooleanSupplier standby) {
        cancel();if(deadline<=0)return false;
        context=owner.getApplicationContext();dreaming=false;status="aguardando protetor";
        receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
            if(Intent.ACTION_DREAMING_STARTED.equals(i.getAction())){dreaming=true;status="prazo preservado";}
            else if(Intent.ACTION_DREAMING_STOPPED.equals(i.getAction())){status="protetor encerrado";cancel();}
        }};
        IntentFilter filter=new IntentFilter(Intent.ACTION_DREAMING_STARTED);filter.addAction(Intent.ACTION_DREAMING_STOPPED);
        try {
            if(Build.VERSION.SDK_INT>=33)context.registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);
            else context.registerReceiver(receiver,filter);
        } catch(RuntimeException unavailable){cancel();return false;}
        handler.postDelayed(()->{status=dreaming?(standby.getAsBoolean()?"desligamento enviado":"TV recusou desligamento"):"protetor não iniciou antes do prazo";cancel();},Math.max(0,deadline-SystemClock.elapsedRealtime()));
        handler.postDelayed(()->{if(!dreaming){status="protetor não iniciou";cancel();}},15000);
        return true;
    }
    static void cancel() {
        handler.removeCallbacksAndMessages(null);
        if(context!=null&&receiver!=null)try{context.unregisterReceiver(receiver);}catch(RuntimeException ignored){}
        receiver=null;context=null;dreaming=false;
    }
}
