package app.launcherin.tv;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** Independent data file: updating appointments never touches config.json or the HOME service. */
final class LauncherinAgendaData {
    static final String FILE="agenda.json", PENDING="agenda.pending.json";
    static final int LIMIT=32768;
    static final class Entry {
        final String title,date,time;
        Entry(JSONObject o){title=o.optString("title");date=o.optString("date");time=o.optString("time");}
    }
    static boolean valid(byte[] data){
        try{JSONArray events=new JSONObject(new String(data,StandardCharsets.UTF_8)).getJSONArray("events");if(events.length()>40)return false;
            Set<String> ids=new HashSet<>();for(int i=0;i<events.length();i++){
                JSONObject e=events.getJSONObject(i);String id=e.getString("id"),title=e.getString("title"),time=e.optString("time");
                if(!id.matches("[a-zA-Z0-9_-]{1,64}")||!ids.add(id)||title.trim().isEmpty()||title.length()>120)return false;
                LocalDate.parse(e.getString("date"));if(!time.isEmpty()){if(!time.matches("[0-2][0-9]:[0-5][0-9]"))return false;LocalTime.parse(time);}
            }return true;
        }catch(Exception error){return false;}
    }
    static synchronized void commit(Context context)throws IOException {
        File dir=Config.dir(context);if(dir==null)dir=context.getFilesDir();File staged=new File(dir,PENDING);byte[] data;
        try(InputStream in=new FileInputStream(staged)){data=LauncherinExtras.bytes(in,LIMIT);}
        if(!valid(data))throw new IOException("Agenda inválida");
        AtomicFile file=new AtomicFile(new File(dir,FILE));FileOutputStream out=file.startWrite();
        try{out.write(data);file.finishWrite(out);}catch(IOException error){file.failWrite(out);throw error;}finally{staged.delete();}
    }
    static List<Entry> today(Context context){
        List<Entry> out=new ArrayList<>();File dir=Config.dir(context);if(dir==null)dir=context.getFilesDir();String date=LocalDate.now().toString();
        try(InputStream in=new AtomicFile(new File(dir,FILE)).openRead()){
            byte[] data=LauncherinExtras.bytes(in,LIMIT);if(!valid(data))return out;
            JSONArray events=new JSONObject(new String(data,StandardCharsets.UTF_8)).getJSONArray("events");
            for(int i=0;i<events.length();i++){Entry e=new Entry(events.getJSONObject(i));if(date.equals(e.date))out.add(e);}
        }catch(Exception ignored){}
        out.sort(Comparator.comparing(e->e.time));return out;
    }
}
