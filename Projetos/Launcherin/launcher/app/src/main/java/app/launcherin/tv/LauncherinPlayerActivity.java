// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.TextUtils;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Native TV library and ambient player. Search, queue and lyrics stay inside Launcherin. */
public final class LauncherinPlayerActivity extends Activity {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor network=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),new ThreadPoolExecutor.DiscardOldestPolicy());
    private final ExecutorService images=Executors.newSingleThreadExecutor();
    private LinearLayout page,body,browser,rows,transport;private FrameLayout stage;private ImageView artwork;
    private TextView title,artist,status,position,lyricsText,source;private EditText query;private SeekBar seek;
    private TextView play,favorite;private ScrollView scroll,lyricsScroll;
    private LauncherinMusicLibrary library;private LauncherinAppearance.Style skin;
    private List<LauncherinTrack> visible=new ArrayList<>();private boolean soundcloud,active,ambient,showLyrics,closed,busy;
    private int searchGeneration,artGeneration,lyricGeneration,photoIndex;private String artSource="",trackUrl="",lyricUrl="";
    private LauncherinLyrics.Result lyrics;private long nextPhotoAt;private int lastLine=-2;private int artWidth=380;
    private final BroadcastReceiver changed=new BroadcastReceiver(){public void onReceive(Context c,Intent i){refresh();}};
    private final Runnable tick=new Runnable(){public void run(){if(!active)return;refresh();main.postDelayed(this,1000);}};
    @Override protected void onCreate(Bundle state){super.onCreate(state);skin=LauncherinAppearance.get(this);library=new LauncherinMusicLibrary(this);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(28),dp(20),dp(28),dp(16));page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff060a10,skin.surface,0xff080b13}));setContentView(page);
        LinearLayout header=line();TextView heading=text("Música",25,skin.text);header.addView(heading,new LinearLayout.LayoutParams(0,dp(44),1));
        header.addView(button("Sobre",this::about));header.addView(button("Apps externos",()->{LauncherinPlayerService.external(this);finish();}));header.addView(button("Voltar à Home",this::finish));page.addView(header);
        body=line();LinearLayout.LayoutParams bodySize=new LinearLayout.LayoutParams(-1,0,1);bodySize.topMargin=dp(14);page.addView(body,bodySize);
        stage=new FrameLayout(this);stage.setPadding(dp(14),dp(14),dp(14),dp(14));stage.setBackground(LauncherinSurface.card(this,45,false));body.addView(stage,new LinearLayout.LayoutParams(0,-1,4));
        LinearLayout face=new LinearLayout(this);face.setOrientation(LinearLayout.VERTICAL);face.setGravity(Gravity.CENTER);stage.addView(face,new FrameLayout.LayoutParams(-1,-1));
        artwork=new ImageView(this);artwork.setScaleType(ImageView.ScaleType.FIT_CENTER);artwork.setAdjustViewBounds(true);face.addView(artwork,new LinearLayout.LayoutParams(-1,0,1));
        title=text("Seu espaço de música",22,skin.text);title.setGravity(Gravity.CENTER);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);face.addView(title,new LinearLayout.LayoutParams(-1,-2));
        artist=text("Pesquise uma música para começar",14,skin.muted);artist.setGravity(Gravity.CENTER);face.addView(artist,new LinearLayout.LayoutParams(-1,dp(30)));
        browser=new LinearLayout(this);browser.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-1,6);bp.leftMargin=dp(20);body.addView(browser,bp);
        LinearLayout find=line();query=new EditText(this);query.setId(View.generateViewId());query.setSingleLine(true);query.setTextSize(15);query.setTextColor(skin.text);query.setHintTextColor(skin.muted);query.setHint("Música ou artista");query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);query.setPadding(dp(10),0,dp(10),0);query.setBackground(LauncherinSurface.card(this,60,false));find.addView(query,new LinearLayout.LayoutParams(0,dp(40),1));find.addView(button("Buscar",this::search));browser.addView(find);query.setOnEditorActionListener((v,action,event)->{search();return true;});
        LinearLayout tabs=line();source=button("Fonte: YouTube",()->{soundcloud=!soundcloud;source.setText(soundcloud?"Fonte: SoundCloud":"Fonte: YouTube");});tabs.addView(source);tabs.addView(button("Fila",()->list(LauncherinPlayerService.tracks(this),"Fila de reprodução",true)));tabs.addView(button("Favoritos",()->list(library.read("favorites"),"Favoritos",false)));browser.addView(tabs);
        status=text("Escolha uma faixa para tocar. Segure OK para adicionar à fila.",12,skin.muted);status.setPadding(0,dp(9),0,dp(8));browser.addView(status);
        scroll=new ScrollView(this);scroll.setFillViewport(true);rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);browser.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        lyricsScroll=new ScrollView(this);lyricsScroll.setFillViewport(true);lyricsText=text("",22,skin.text);lyricsText.setPadding(dp(24),dp(16),dp(24),dp(16));lyricsText.setGravity(Gravity.CENTER);lyricsScroll.addView(lyricsText);lyricsScroll.setFocusable(true);lyricsScroll.setId(View.generateViewId());lyricsScroll.setVisibility(View.GONE);stage.addView(lyricsScroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout timeline=line();position=text("0:00 / 0:00",12,skin.muted);timeline.addView(position,new LinearLayout.LayoutParams(dp(112),dp(30)));seek=new SeekBar(this);seek.setMax(1000);seek.setFocusable(true);seek.setContentDescription("Posição da música");seek.setProgressTintList(android.content.res.ColorStateList.valueOf(skin.accent));timeline.addView(seek,new LinearLayout.LayoutParams(0,dp(30),1));seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){if(user){JSONObject now=LauncherinPlayerService.snapshot(LauncherinPlayerActivity.this);LauncherinPlayerService.seek(LauncherinPlayerActivity.this,now.optLong("duration")*p/1000);}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});page.addView(timeline);
        transport=line();transport.setGravity(Gravity.CENTER);transport.addView(button("Anterior",()->command("previous")));play=button("Reproduzir",()->command("toggle"));transport.addView(play);transport.addView(button("Próxima",()->command("next")));favorite=button("Favoritar",()->{LauncherinTrack t=track();if(t!=null){library.toggle(t);refresh();}});transport.addView(favorite);
        transport.addView(button("Artes",()->ambient(false)));transport.addView(button("Letras",()->ambient(true)));transport.addView(button("Fotos / Capa",()->{library.artMode("photos".equals(library.artMode())?"album":"photos");nextPhotoAt=0;artSource="";refresh();}));page.addView(transport);
        list(library.read("favorites"),"Suas músicas favoritas",false);query.requestFocus();refresh();
    }
    private void about(){new AlertDialog.Builder(this).setTitle("Música da Launcherin").setMessage("Player integrado, sem necessidade de instalar NewPipe.\n\nBusca e áudio: YouTube e SoundCloud, por NewPipe Extractor v0.27.1 (GPL-3.0-ou-posterior). Letras opcionais: LRCLIB, conforme disponibilidade.\n\nA versão com este player é distribuída sob GPL-3.0-ou-posterior, preservando os avisos MIT da base. Fontes, dependências e licenças: github.com/allanlribeiro/laucherin\n\nRhino (MPL-2.0), jsoup (MIT), nanojson e JSR-305 (Apache-2.0), Protocol Buffers (BSD), bibliotecas Android/AndroidX e desugaring (licenças originais). Textos em assets/licenses.").setPositiveButton("Licença GPL",(d,w)->{try(java.io.InputStream in=getAssets().open("licenses/GPL-3.0.txt")){TextView text=text(new String(LauncherinExtras.bytes(in,60000),java.nio.charset.StandardCharsets.UTF_8),12,skin.text);text.setPadding(dp(18),dp(15),dp(18),dp(15));ScrollView scroll=new ScrollView(this);scroll.addView(text);new AlertDialog.Builder(this).setTitle("GPL-3.0-ou-posterior").setView(scroll).setPositiveButton("Fechar",null).show();}catch(Exception ignored){}}).setNeutralButton("Diagnóstico externo",(d,w)->LauncherinMusicActions.diagnostic(this)).setNegativeButton("Fechar",null).show();}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private LinearLayout line(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(skin.font);t.setIncludeFontPadding(false);return t;}
    private TextView button(String value,Runnable action){TextView b=text(value,14,skin.text);b.setId(View.generateViewId());b.setGravity(Gravity.CENTER);b.setPadding(dp(13),dp(9),dp(13),dp(9));b.setFocusable(true);b.setClickable(true);b.setBackground(LauncherinSurface.card(this,55,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(40));p.leftMargin=dp(6);b.setLayoutParams(p);b.setOnClickListener(v->action.run());b.setOnFocusChangeListener((v,f)->b.setBackground(LauncherinSurface.card(this,55,f)));return b;}
    private void command(String action){if(LauncherinPlayerService.tracks(this).isEmpty()){status.setText("Pesquise e escolha uma música para começar.");query.requestFocus();return;}LauncherinPlayerService.command(this,action);}
    private LauncherinTrack track(){return LauncherinTrack.read(LauncherinPlayerService.snapshot(this));}
    private void search(){String term=query.getText().toString().trim();if(term.isEmpty()){query.requestFocus();return;}if(term.length()>160){status.setText("Use uma busca de até 160 caracteres.");return;}final int request=++searchGeneration;final boolean provider=soundcloud;busy=true;status.setText("Buscando músicas…");getSystemService(InputMethodManager.class).hideSoftInputFromWindow(query.getWindowToken(),0);
        network.execute(()->{try{List<LauncherinTrack> found=LauncherinMusicSource.search(term,provider);main.post(()->{if(closed||request!=searchGeneration)return;busy=false;list(found,found.isEmpty()?"Nenhuma música encontrada. Tente outra busca.":"Resultados · "+(provider?"SoundCloud":"YouTube"),false);if(rows.getChildCount()>0)rows.getChildAt(0).requestFocus();});}catch(Exception e){main.post(()->{if(!closed&&request==searchGeneration){busy=false;status.setText(LauncherinMusicSource.error(e));}});}});
    }
    private void list(List<LauncherinTrack> items,String label,boolean queue){visible=new ArrayList<>(items);rows.removeAllViews();status.setText(label);scroll.scrollTo(0,0);
        for(int i=0;i<items.size();i++){LauncherinTrack t=items.get(i);final int at=i;TextView row=button((i+1)+"  "+t.title+"\n"+t.artist+" · "+time(t.duration),()->{LauncherinPlayerService.play(this,visible,at);status.setText("Carregando áudio…");});row.setTextSize(15);row.setGravity(Gravity.CENTER_VERTICAL);row.setMaxLines(2);row.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(66));p.bottomMargin=dp(7);row.setLayoutParams(p);row.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle(t.title).setItems(queue?new String[]{"Adicionar novamente à fila","Favoritar / remover favorito","Remover da fila"}:new String[]{"Adicionar à fila","Favoritar / remover favorito"},(d,which)->{if(which==0){LauncherinPlayerService.enqueue(this,t);Toast.makeText(this,"Adicionada à fila",Toast.LENGTH_SHORT).show();}else if(which==1){library.toggle(t);refresh();}else remove(at);}).setNegativeButton("Voltar",null).show();return true;});rows.addView(row);}
        if(items.isEmpty()){TextView empty=text("Pesquise pelo nome de uma música ou artista.\nSeus favoritos ficam salvos nesta TV.",16,skin.muted);empty.setPadding(dp(14),dp(25),dp(14),0);rows.addView(empty);}
    }
    private void remove(int at){List<LauncherinTrack> q=LauncherinPlayerService.tracks(this);if(at<0||at>=q.size())return;JSONObject now=LauncherinPlayerService.snapshot(this);if(q.get(at).url.equals(now.optString("url"))){Toast.makeText(this,"Escolha outra faixa antes de remover a atual.",Toast.LENGTH_SHORT).show();return;}LauncherinPlayerService.remove(this,at);list(LauncherinPlayerService.tracks(this),"Fila de reprodução",true);}
    private void ambient(boolean lyricsMode){ambient=true;showLyrics=lyricsMode;browser.setVisibility(View.GONE);title.setVisibility(lyricsMode?View.GONE:View.VISIBLE);artist.setVisibility(lyricsMode?View.GONE:View.VISIBLE);artwork.setVisibility(lyricsMode?View.GONE:View.VISIBLE);lyricsScroll.setVisibility(lyricsMode?View.VISIBLE:View.GONE);artSource="";nextPhotoAt=0;artWidth=1280;refresh();if(lyricsMode)lyricsScroll.requestFocus();else play.requestFocus();}
    private void normal(){ambient=false;showLyrics=false;browser.setVisibility(View.VISIBLE);title.setVisibility(View.VISIBLE);artist.setVisibility(View.VISIBLE);artwork.setVisibility(View.VISIBLE);lyricsScroll.setVisibility(View.GONE);artWidth=380;artSource="";query.requestFocus();refresh();}
    @Override public void onBackPressed(){if(ambient)normal();else super.onBackPressed();}
    @Override public boolean onKeyDown(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE){command("toggle");return true;}if(key==KeyEvent.KEYCODE_MEDIA_NEXT){command("next");return true;}if(key==KeyEvent.KEYCODE_MEDIA_PREVIOUS){command("previous");return true;}return super.onKeyDown(key,event);}
    private void refresh(){if(closed||library==null)return;JSONObject j=LauncherinPlayerService.snapshot(this);LauncherinTrack t=LauncherinTrack.read(j);boolean playing=j.optBoolean("playing"),loading=j.optBoolean("buffering");play.setText(loading?"Cancelar":playing?"Pausar":"Reproduzir");
        if(t==null){title.setText("Seu espaço de música");artist.setText("Pesquise uma música para começar");return;}
        title.setText(t.title);artist.setText(t.artist+" · "+t.source);favorite.setText(library.favorite(t)?"★ Favorita":"☆ Favoritar");long p=j.optLong("position");seek.setProgress(t.duration>0?(int)Math.min(1000,p*1000/t.duration):0);position.setText(time(p)+" / "+time(t.duration));
        if(!j.optString("error").isEmpty()&&!busy)status.setText(j.optString("error"));
        if(!t.url.equals(trackUrl)){trackUrl=t.url;lyricUrl="";lyrics=null;lastLine=-2;nextPhotoAt=0;artSource="";}
        if(showLyrics){if(!t.url.equals(lyricUrl))loadLyrics(t);renderLyrics(p);return;}
        String image=t.art;if(ambient&&"photos".equals(library.artMode())){List<String> photos=LauncherinPlayerImages.photos(this);if(!photos.isEmpty()){long now=SystemClock.elapsedRealtime();if(now>=nextPhotoAt){photoIndex++;nextPhotoAt=now+20000;artSource="";}image=photos.get(Math.floorMod(photoIndex,photos.size()));}}
        if(!image.equals(artSource))loadArt(image);
    }
    private void loadArt(String source){artSource=source;final int generation=++artGeneration;final int width=artWidth;images.execute(()->{Bitmap bitmap=null;try{bitmap=LauncherinPlayerImages.load(this,source,width,ambient?720:380);}catch(Exception ignored){}final Bitmap ready=bitmap;main.post(()->{if(closed||!active||generation!=artGeneration){if(ready!=null)ready.recycle();return;}artwork.setImageBitmap(ready);});});}
    private void loadLyrics(LauncherinTrack t){lyricUrl=t.url;lyrics=null;lyricsText.setText("Buscando letra…");lastLine=-2;final int generation=++lyricGeneration;network.execute(()->{LauncherinLyrics.Result result=null;try{result=LauncherinLyrics.load(this,t);}catch(Exception ignored){}final LauncherinLyrics.Result ready=result;main.post(()->{if(closed||!active||generation!=lyricGeneration)return;lyrics=ready==null?new LauncherinLyrics.Result("","",false):ready;lastLine=-2;renderLyrics(LauncherinPlayerService.snapshot(this).optLong("position"));});});}
    private void renderLyrics(long position){if(lyrics==null)return;if(lyrics.instrumental){lyricsText.setText("Faixa instrumental");return;}if(lyrics.lines.isEmpty()){lyricsText.setText(lyrics.plain.isEmpty()?"Letra não encontrada para esta gravação.":lyrics.plain);return;}
        int at=lyrics.at(position);if(at==lastLine)return;lastLine=at;StringBuilder text=new StringBuilder();int first=Math.max(0,at-2),end=Math.min(lyrics.lines.size(),Math.max(5,at+4));for(int i=first;i<end;i++){if(i==at)text.append("›  ");text.append(lyrics.lines.get(i).text).append("\n\n");}lyricsText.setText(text.toString());lyricsScroll.scrollTo(0,0);
    }
    static String time(long millis){long sec=Math.max(0,millis)/1000;return sec/60+":"+String.format(Locale.ROOT,"%02d",sec%60);}
    // The unflagged overload is used only on API 26–32, where non-exported flags do not exist.
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override protected void onStart(){super.onStart();active=true;if(Build.VERSION.SDK_INT>=33)registerReceiver(changed,new IntentFilter(LauncherinPlayerService.CHANGED),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(changed,new IntentFilter(LauncherinPlayerService.CHANGED));main.post(tick);}
    @Override protected void onStop(){active=false;++searchGeneration;++artGeneration;++lyricGeneration;if(busy)status.setText("Busca interrompida. Selecione Buscar para tentar novamente.");busy=false;network.getQueue().clear();artSource="";lyricUrl="";main.removeCallbacks(tick);unregisterReceiver(changed);artwork.setImageDrawable(null);super.onStop();}
    @Override protected void onDestroy(){closed=true;++searchGeneration;main.removeCallbacksAndMessages(null);network.shutdownNow();images.shutdownNow();super.onDestroy();}
}
