package app.launcherin.tv;

import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import android.text.TextUtils;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/** Reference transport controls on the same transparent glass as the other dashboard widgets. */
final class LauncherinAudioWidget extends FrameLayout {
 private final MainActivity host;private final boolean preview;private final float density;
 private final SeekBar progress;private final Button previous,play,next;private final ImageView provider,cover;private final TextView title,artist;
 private final ExecutorService artwork=Executors.newSingleThreadExecutor();private String artSource="";private boolean closed;
 private final Handler handler=new Handler(Looper.getMainLooper());private JSONObject track=new JSONObject();
 private Runnable left=()->{},right=()->{},down=()->{};private boolean active;private int transparency;
 private final Runnable tick=new Runnable(){@Override public void run(){if(!active)return;updateProgress();if(track.optBoolean("playing"))handler.postDelayed(this,1000);}};
 LauncherinAudioWidget(MainActivity host,boolean preview){super(host);this.host=host;this.preview=preview;density=getResources().getDisplayMetrics().density;setId(View.generateViewId());setFocusable(true);setClipChildren(false);
  setTransparency(LauncherinWidgetAppearance.transparency(host));
  progress=new SeekBar(host);progress.setMax(1000);progress.setProgressTintList(ColorStateList.valueOf(0xddeeeef3));progress.setProgressBackgroundTintList(ColorStateList.valueOf(0x40ffffff));progress.setPadding(0,0,0,0);GradientDrawable thumb=new GradientDrawable();thumb.setColor(0xfff5f5fa);thumb.setShape(GradientDrawable.OVAL);thumb.setSize(dp(5),dp(5));progress.setThumb(thumb);progress.setThumbOffset(dp(2));progress.setFocusable(false);progress.setContentDescription("Posição da reprodução");addView(progress);
  progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar v,int p,boolean user){if(user)LauncherinMediaService.seek(host,Math.round(track.optLong("duration")*p/1000d));}public void onStartTrackingTouch(SeekBar v){}public void onStopTrackingTouch(SeekBar v){}});
  title=new TextView(host);title.setTextColor(0xfff7f7fa);title.setTextSize(13);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);title.setIncludeFontPadding(false);addView(title);
  artist=new TextView(host);artist.setTextColor(0xffc3c3cb);artist.setTextSize(10);artist.setSingleLine(true);artist.setEllipsize(TextUtils.TruncateAt.END);artist.setIncludeFontPadding(false);addView(artist);
  cover=new ImageView(host);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);addView(cover);
  previous=button("Faixa anterior",-1);play=button("Reproduzir ou pausar",0);next=button("Próxima faixa",1);addView(previous);addView(play);addView(next);
  previous.setOnClickListener(v->LauncherinMediaService.skip(host,false));next.setOnClickListener(v->LauncherinMediaService.skip(host,true));play.setOnClickListener(v->{if(!LauncherinMediaService.granted(host)){try{host.startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(RuntimeException error){Toast.makeText(host,"Abra o acesso a mídias nas configurações da TV.",Toast.LENGTH_LONG).show();}}else if(!LauncherinMusicResume.busy())LauncherinMediaService.setMusicPlaying(host,!track.optBoolean("playing"));});
  setOnLongClickListener(v->{LauncherinMusicActions.diagnostic(host);return true;});
  for(Button control:new Button[]{previous,next})control.setOnLongClickListener(v->{LauncherinMusicActions.diagnostic(host);return true;});
  play.setOnLongClickListener(v->{host.startActivity(new Intent(host,LauncherinYouTubeMusicActivity.class));return true;});
  provider=new ImageView(host);provider.setScaleType(ImageView.ScaleType.FIT_CENTER);provider.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);addView(provider);
  setOnFocusChangeListener((v,has)->{if(has)play.requestFocus();});
  for(Button button:new Button[]{previous,play,next})button.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;if(key==KeyEvent.KEYCODE_DPAD_DOWN){down.run();return true;}if(key==KeyEvent.KEYCODE_DPAD_UP)return true;if(key==KeyEvent.KEYCODE_DPAD_LEFT&&v==previous){left.run();return true;}if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&v==next){right.run();return true;}return false;});
  previous.setNextFocusRightId(play.getId());play.setNextFocusLeftId(previous.getId());play.setNextFocusRightId(next.getId());next.setNextFocusLeftId(play.getId());
 }
 private int dp(float n){return Math.round(n*density);}
 private GradientDrawable fill(int color,boolean focus){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(30));if(focus)d.setStroke(dp(1),0xccffffff);return d;}
 private Button button(String label,int kind){Button b=new Button(host);b.setId(View.generateViewId());b.setText("");b.setContentDescription(label);b.setMinWidth(0);b.setMinHeight(0);b.setMinimumWidth(0);b.setMinimumHeight(0);b.setPadding(0,0,0,0);b.setElevation(0);b.setBackground(fill(kind==0?0x50ffffff:0x1effffff,false));b.setForeground(new android.graphics.drawable.Drawable(){final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);public void draw(Canvas canvas){Rect bounds=getBounds();float cx=bounds.exactCenterX(),cy=bounds.exactCenterY(),s=dp(kind==0?5:3.2f);paint.setColor(kind==0?0xfffafafa:0xbbe8e8ed);if(kind==0&&track.optBoolean("playing")&&!preview){canvas.drawRect(cx-s,cy-s,cx-dp(1),cy+s,paint);canvas.drawRect(cx+dp(1),cy-s,cx+s,cy+s,paint);}else{Path p=new Path();int direction=kind<0?-1:1;p.moveTo(cx+direction*s,cy);p.lineTo(cx-direction*s*.7f,cy-s);p.lineTo(cx-direction*s*.7f,cy+s);p.close();canvas.drawPath(p,paint);if(kind!=0)canvas.drawRect(cx+direction*s-dp(.7f),cy-s,cx+direction*s+dp(.7f),cy+s,paint);}}public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}});
  b.setOnFocusChangeListener((v,has)->{if(has)host.launcherinChromeFocused();b.setBackground(fill(kind==0?0x50ffffff:0x1effffff,has));b.animate().scaleX(has?1.08f:1).scaleY(has?1.08f:1).setDuration(120).start();});return b;
 }
 private void place(View v,int x,int y,int w,int h){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(w,h);p.leftMargin=x;p.topMargin=y;v.setLayoutParams(p);}
 @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);
  place(cover,dp(12),dp(11),dp(30),dp(30));place(title,dp(51),dp(11),w-dp(64),dp(19));place(artist,dp(51),dp(31),w-dp(64),dp(15));
  place(progress,Math.round(w*.14f),Math.round(h*.43f),Math.round(w*.72f),dp(16));int pw=dp(42),ph=dp(31),button=dp(24),cy=Math.round(h*.75f);place(play,(w-pw)/2,cy-ph/2,pw,ph);place(previous,w/2-dp(48),cy-button/2,button,button);place(next,w/2+dp(24),cy-button/2,button,button);place(provider,w-dp(29),h-dp(27),dp(19),dp(19));}

 void setTransparency(int value){transparency=LauncherinWidgetAppearance.clamp(value);setBackground(LauncherinWidgetAppearance.glass(host,transparency,false));}
 void navigation(Runnable left,Runnable right,Runnable down){this.left=left;this.right=right;this.down=down;}
 void refresh(JSONObject track){this.track=track;if(!preview)try{this.track.put("playing",LauncherinMediaService.musicPlaying());}catch(Exception ignored){}
  String name=track.optString("title"),pkg=track.optString("pkg");
  title.setText(name.isEmpty()?"Continuar ouvindo":name);artist.setText(name.isEmpty()?(LauncherinMediaService.granted(host)?"Reproduza no Spotify ou YouTube Music":"Ative o acesso a mídias"):track.optString("artist"));
  setContentDescription("Continuar ouvindo · "+(name.isEmpty()?artist.getText():name+" · "+artist.getText()));
  try{provider.setImageDrawable(host.getPackageManager().getApplicationIcon(pkg));}catch(Exception ignored){provider.setImageResource(android.R.drawable.ic_media_play);}
  loadArtwork(track.optString("art"));if(track.optString("art").isEmpty())cover.setImageDrawable(provider.getDrawable()==null?null:provider.getDrawable().getConstantState()==null?null:provider.getDrawable().getConstantState().newDrawable());play.invalidate();handler.removeCallbacks(tick);active=true;handler.post(tick);}
 private void loadArtwork(String source){
  if(source.equals(artSource))return;artSource=source;cover.setImageDrawable(null);if(source.isEmpty()||closed)return;
  artwork.execute(()->{Bitmap bitmap=null;try{
   byte[] bytes;
   if(source.startsWith("https://")){HttpURLConnection connection=(HttpURLConnection)new URL(source).openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(5000);try(InputStream in=connection.getInputStream()){bytes=LauncherinExtras.bytes(in,2_000_000);}finally{connection.disconnect();}}
   else{File file=new File(source);if(!file.getCanonicalPath().startsWith(host.getFilesDir().getCanonicalPath()+"/"))return;try(InputStream in=new FileInputStream(file)){bytes=LauncherinExtras.bytes(in,2_000_000);}}
   BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(options.outWidth<=0||options.outHeight<=0||options.outWidth>32768||options.outHeight>32768)return;options.inSampleSize=1;while(options.outWidth/options.inSampleSize>256||options.outHeight/options.inSampleSize>256)options.inSampleSize*=2;options.inJustDecodeBounds=false;bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
  }catch(Exception ignored){}final Bitmap result=bitmap;handler.post(()->{if(!closed&&source.equals(artSource))cover.setImageBitmap(result);else if(result!=null)result.recycle();});});
 }

 private void updateProgress(){if(!preview&&!track.optString("title").isEmpty())artist.setText(LauncherinMusicResume.busy()?"Retomando a música…":track.optString("artist"));long duration=track.optLong("duration"),position=track.optLong("position");if(track.optBoolean("playing"))position+=Math.max(0,SystemClock.elapsedRealtime()-track.optLong("capturedElapsed",SystemClock.elapsedRealtime()));progress.setProgress(preview?840:duration>0?(int)Math.min(1000,1000*position/duration):0);}
 void close(){closed=true;pause();artwork.shutdownNow();}
 void pause(){LauncherinMediaService.cancelMusicCommand();active=false;handler.removeCallbacks(tick);}
}
