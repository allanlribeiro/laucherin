package app.launcherin.tv;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executor;

/** Passive widgets; only visible cards are sampled. All imported pictures remain on the TV. */
final class LauncherinExtraWidgets {
    final MainActivity host;final Executor io;final FrameLayout agenda,photos,system;
    private final TextView appointments,memory,cpu,photoHint;private final ImageView picture;
    private final LauncherinSystemStats stats=new LauncherinSystemStats();
    private List<LauncherinAgendaData.Entry> events=Collections.emptyList();private String agendaText="";
    private String shown="";private long photoAt;private int photoIndex;private boolean sampling,decoding,active;
    private LauncherinCardPopup popup;private LauncherinSystemStats.Sample last;
    LauncherinExtraWidgets(MainActivity host,Executor io,FrameLayout agenda,FrameLayout photos,FrameLayout system){
        this.host=host;this.io=io;this.agenda=agenda;this.photos=photos;this.system=system;
        appointments=text("Cadastre compromissos pelo celular",15);place(agenda,appointments,37,14);agenda.setContentDescription("Agenda do dia");agenda.setOnClickListener(v->agendaPopup());
        picture=new ImageView(host);picture.setScaleType(ImageView.ScaleType.FIT_CENTER);photos.addView(picture,0,new FrameLayout.LayoutParams(-1,-1));photos.setClipToOutline(true);
        photoHint=text("Escolha fotos no celular",15);photoHint.setGravity(Gravity.CENTER);place(photos,photoHint,30,12);photos.setContentDescription("Porta-retratos");photos.setOnClickListener(v->{photoAt=0;photoIndex++;refreshPhoto();});
        memory=text("RAM · —",16);cpu=text("CPU · —",14);place(system,memory,38,12);place(system,cpu,73,12);system.setContentDescription("Memória RAM e CPU");system.setOnClickListener(v->systemPopup());
    }
    private int dp(int value){return Math.round(value*host.getResources().getDisplayMetrics().density);}
    private TextView text(String title,int size){TextView v=new TextView(host);v.setText(title);v.setTextSize(size);v.setTextColor(0xfff4f4fa);v.setEllipsize(TextUtils.TruncateAt.END);v.setMaxLines(3);v.setIncludeFontPadding(false);return v;}
    private void place(FrameLayout card,View view,int top,int side){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-1);p.setMargins(dp(side),dp(top),dp(side),dp(8));card.addView(view,p);}
    void active(boolean on){active=on;if(!on&&popup!=null)popup.dismiss();}
    void tick(){if(!active)return;if(agenda.isShown())refreshAgenda();if(photos.isShown())refreshPhoto();if(system.isShown())refreshSystem();}
    private void refreshAgenda(){
        List<LauncherinAgendaData.Entry> next=LauncherinAgendaData.today(host);StringBuilder s=new StringBuilder();
        for(int i=0;i<Math.min(2,next.size());i++){if(i>0)s.append('\n');LauncherinAgendaData.Entry e=next.get(i);s.append(e.time.isEmpty()?"Dia todo":e.time).append(" · ").append(e.title);}
        String text=next.isEmpty()?"Nenhum compromisso hoje":s.toString();if(!text.equals(agendaText)){agendaText=text;appointments.setText(text);}events=next;
    }
    private void agendaPopup(){
        if(popup!=null)popup.dismiss();refreshAgenda();popup=new LauncherinCardPopup(host,agenda,440,280);popup.content.addView(popup.label("Agenda de hoje",22,-1));
        ScrollView scroll=new ScrollView(host);LinearLayout list=new LinearLayout(host);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);popup.content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(events.isEmpty())list.addView(popup.label("Nenhum compromisso hoje.\nCadastre pelo app do celular.",16,0xffdddddd));
        for(LauncherinAgendaData.Entry e:events){TextView row=popup.label((e.time.isEmpty()?"Dia todo":e.time)+"  "+e.title,16,-1);row.setPadding(0,dp(8),0,dp(8));list.addView(row);}
        popup.content.addView(popup.button("Fechar",()->popup.dismiss()));popup.show();
    }
    private void refreshSystem(){
        if(sampling)return;sampling=true;io.execute(()->{
            LauncherinSystemStats.Sample sample=stats.sample(host);system.post(()->{sampling=false;if(!active||!system.isShown())return;last=sample;
                memory.setText(sample.totalMemory>0?String.format(Locale.forLanguageTag("pt-BR"),"RAM · %.1f / %.1f GB",sample.usedMemory()/1073741824d,sample.totalMemory/1073741824d):"RAM indisponível");
                cpu.setText((sample.wholeTv?"CPU da TV · ":"CPU da launcher · ")+(sample.cpu==null?"—":Math.round(sample.cpu)+"%"));
            });
        });
    }
    private void systemPopup(){
        if(popup!=null)popup.dismiss();popup=new LauncherinCardPopup(host,system,440,230);popup.content.addView(popup.label("RAM e CPU",22,-1));
        String scope=last!=null&&last.wholeTv?"O uso de CPU reúne todos os núcleos da TV.":"Este Android não disponibiliza o uso total de CPU. A medição exibida é somente da Launcherin, dividida entre os núcleos disponíveis.";
        popup.content.addView(popup.label("A RAM inclui o sistema e os aplicativos.\n\n"+scope+"\nAtualização a cada 5 segundos enquanto o widget está visível.",15,0xffdddddd),new LinearLayout.LayoutParams(-1,0,1));
        popup.content.addView(popup.button("Fechar",()->popup.dismiss()));popup.show();
    }
    private void refreshPhoto(){
        if(decoding||!active)return;JSONArray ids=LauncherinExtras.read(host).optJSONArray("frame_photos");List<String> valid=new ArrayList<>();
        if(ids!=null)for(int i=0;i<Math.min(10,ids.length());i++){String id=ids.optString(i);if(id.matches("[a-f0-9]{64}"))valid.add(id);}
        if(valid.isEmpty()){shown="";picture.setImageDrawable(null);photoHint.setVisibility(View.VISIBLE);return;}
        long now=android.os.SystemClock.elapsedRealtime();if(photoAt>0&&now-photoAt<15000&&valid.contains(shown))return;
        if(photoAt>0)photoIndex++;String id=valid.get(Math.floorMod(photoIndex,valid.size()));photoAt=now;if(id.equals(shown))return;decoding=true;
        io.execute(()->{Bitmap bitmap=null;try{File dir=Config.dir(host);if(dir==null)dir=host.getFilesDir();File file=new File(dir,"frame-"+id.substring(0,40)+".jpg");
            if(file.isFile()&&file.length()<=280000){BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),options);
                if(options.outWidth>0&&options.outHeight>0&&options.outWidth<=8192&&options.outHeight<=8192){options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1280)options.inSampleSize*=2;options.inJustDecodeBounds=false;bitmap=BitmapFactory.decodeFile(file.getPath(),options);}}
        }catch(Exception ignored){}Bitmap ready=bitmap;photos.post(()->{decoding=false;if(!active)return;shown=ready!=null?id:"";picture.setImageBitmap(ready);photoHint.setText("Foto indisponível. Envie novamente pelo celular.");photoHint.setVisibility(ready!=null?View.GONE:View.VISIBLE);});});
    }
}
