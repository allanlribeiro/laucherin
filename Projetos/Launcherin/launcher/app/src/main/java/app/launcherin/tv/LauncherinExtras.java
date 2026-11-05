package app.launcherin.tv;

import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Additional keys travel in the original config.json and through the original ThemeProvider. */
final class LauncherinExtras {
 static JSONObject read(Context context){
  File dir=Config.dir(context);if(dir==null)dir=context.getFilesDir();try(InputStream in=new FileInputStream(new File(dir,"config.json"))){return new JSONObject(new String(bytes(in,65536),StandardCharsets.UTF_8));}catch(Exception ignored){}
  return new JSONObject();
 }
 static void setInt(Context context,String key,int value)throws Exception{setValue(context,key,value);}
 static void setString(Context context,String key,String value)throws Exception{setValue(context,key,value);}
 static void setArray(Context context,String key,org.json.JSONArray value)throws Exception{setValue(context,key,value);}
 private static synchronized void setValue(Context context,String key,Object value)throws Exception{
  File dir=Config.dir(context);if(dir==null)dir=context.getFilesDir();if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Pasta indisponível");
  android.util.AtomicFile file=new android.util.AtomicFile(new File(dir,"config.json"));JSONObject config;
  if(file.getBaseFile().exists())try(InputStream in=file.openRead()){config=new JSONObject(new String(bytes(in,65536),StandardCharsets.UTF_8));}else config=new JSONObject();
  config.put(key,value);byte[] data=config.toString().getBytes(StandardCharsets.UTF_8);if(data.length>65536)throw new IOException("Configuração muito grande");
  FileOutputStream out=file.startWrite();try{out.write(data);file.finishWrite(out);}catch(Exception error){file.failWrite(out);throw error;}
  Config.localOverlayEdited(context);
 }
 static byte[] bytes(InputStream in,int limit)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;){if(out.size()+n>limit)throw new IOException("Arquivo muito grande");out.write(b,0,n);}return out.toByteArray();}
}
