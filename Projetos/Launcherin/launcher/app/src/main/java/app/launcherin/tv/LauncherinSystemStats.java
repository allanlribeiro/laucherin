package app.launcherin.tv;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.os.SystemClock;
import java.io.*;

/** Real counters only. Android may deny /proc/stat; the fallback is explicitly app-scoped. */
final class LauncherinSystemStats {
    static final class Sample {
        final long totalMemory,availableMemory,totalStorage,availableStorage;final Double cpu;final boolean wholeTv;
        Sample(long total,long available,Double cpu,boolean wholeTv){this(total,available,cpu,wholeTv,0,0);}
        Sample(long total,long available,Double cpu,boolean wholeTv,long storage,long free){totalMemory=total;availableMemory=available;this.cpu=cpu;this.wholeTv=wholeTv;totalStorage=storage;availableStorage=free;}
        long usedMemory(){return Math.max(0,totalMemory-availableMemory);}
    }
    private long previousTotal=-1,previousIdle=-1,previousApp=-1,previousTime=-1;
    Sample sample(Context context){
        ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        if(manager!=null)manager.getMemoryInfo(memory);
        long storage=0,free=0;try{android.os.StatFs disk=new android.os.StatFs(context.getFilesDir().getPath());storage=disk.getTotalBytes();free=disk.getAvailableBytes();}catch(RuntimeException ignored){}
        long app=Process.getElapsedCpuTime(),now=SystemClock.elapsedRealtime();Double local=percentage(app-previousApp,now-previousTime,Math.max(1,Runtime.getRuntime().availableProcessors()));
        if(previousApp<0||previousTime<0)local=null;previousApp=app;previousTime=now;
        try(BufferedReader reader=new BufferedReader(new FileReader("/proc/stat"))){
            long[] counters=parse(reader.readLine());Double global=null;
            if(counters!=null){if(previousTotal>=0)global=busy(counters[0]-previousTotal,counters[1]-previousIdle);previousTotal=counters[0];previousIdle=counters[1];return new Sample(memory.totalMem,memory.availMem,global,true,storage,free);}
        }catch(IOException|SecurityException ignored){previousTotal=previousIdle=-1;}
        return new Sample(memory.totalMem,memory.availMem,local,false,storage,free);
    }
    static long[] parse(String line){
        if(line==null)return null;String[] fields=line.trim().split("\\s+");if(fields.length<5||!"cpu".equals(fields[0]))return null;
        try{long total=0,idle=0;for(int i=1;i<Math.min(fields.length,9);i++){long n=Long.parseLong(fields[i]);if(n<0)return null;total+=n;if(i==4||i==5)idle+=n;}return new long[]{total,idle};}catch(NumberFormatException error){return null;}
    }
    static Double busy(long total,long idle){if(total<=0||idle<0||idle>total)return null;return Math.max(0d,Math.min(100d,100d*(total-idle)/total));}
    static Double percentage(long cpu,long elapsed,int cores){if(cpu<0||elapsed<=0||cores<=0)return null;return Math.min(100d,100d*cpu/elapsed/cores);}
}
