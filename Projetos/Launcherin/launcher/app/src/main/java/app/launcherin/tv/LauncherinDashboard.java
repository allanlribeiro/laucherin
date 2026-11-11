package app.launcherin.tv;

import android.app.*;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.dynamicanimation.animation.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** The requested three-row composition around the unmodified upstream app adapter and glass focus. */
final class LauncherinDashboard {
 private final MainActivity host;private final FrameLayout root,layer;private final RecyclerView grid;
 private final LauncherinTmdb tmdb;
 private final float density;private final boolean preview;private boolean alive=true,registered,visible,weatherLoading,programsLoading;private long programsLoaded;private String historySignature;private boolean forceHistory;
 private final Handler handler=new Handler(Looper.getMainLooper());private final ExecutorService io=Executors.newFixedThreadPool(2);
 private final List<View> widgets=new ArrayList<>(),history=new ArrayList<>();private LinearLayout shelf,quick;private FrameLayout widgetRow;
 private FrameLayout appsDock;private int widgetTransparency;private String watchMode="above";private boolean watchRevealed;private int watchOffset;
 private final Map<String,View> allWidgets=new LinkedHashMap<>();private List<LauncherinWidgetLayout.Slot> widgetSlots=Collections.emptyList();private String widgetSignature="";private LauncherinExtraWidgets extraWidgets;
 private final Runnable extrasTick=new Runnable(){public void run(){if(alive&&visible){applyWidgetLayout();extraWidgets.tick();handler.postDelayed(this,5000);}}};
 private LauncherinWidgetFaces.Clock clockFace;private LauncherinWidgetFaces.Weather weatherFace;private TextView sportsTitle,sportsDetail;private LauncherinAudioWidget audio;private TextView historyLabel;private int lastHistory;
 private FrameLayout forecastWidget,sportsWidget;private LauncherinWeatherCard weatherCard;private LauncherinSportsCard sportsCard;
 private final Map<String,LauncherinSports.Snapshot> sportsData=new HashMap<>();private boolean sportsLoading;private String sportsSelection="",crestSignature="";private LinearLayout crests;
 private LauncherinSeasonOverlay seasonOverlay;
 private final BroadcastReceiver changes=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){if(visible)update();}};
 private final Runnable minute=new Runnable(){@Override public void run(){if(alive&&visible){updateClock();updateWeather();updateSports();handler.postDelayed(this,60000-System.currentTimeMillis()%60000);}}};
 LauncherinDashboard(MainActivity host,FrameLayout root,RecyclerView grid){
  this.host=host;this.root=root;this.grid=grid;tmdb=new LauncherinTmdb(host);density=host.getResources().getDisplayMetrics().density;
  preview=(host.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0&&host.getIntent().getBooleanExtra("launcherin_preview",false);
  ViewGroup content=(ViewGroup)grid.getParent();View outer=(View)content.getParent();content.removeView(grid);outer.setVisibility(View.GONE);
  host.findViewById(R.id.hero).setVisibility(View.GONE);host.findViewById(R.id.racePanel).setVisibility(View.GONE);
  layer=new FrameLayout(host);layer.setClipChildren(false);layer.setClipToPadding(false);
  root.addView(layer,Math.max(0,root.indexOfChild(host.findViewById(R.id.glass))),new FrameLayout.LayoutParams(-1,-1));
  widgetTransparency=LauncherinWidgetAppearance.transparency(host);build();root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot)layout(r-l,b-t);});root.post(()->layout(root.getWidth(),root.getHeight()));
  IntentFilter filter=new IntentFilter(LauncherinMediaService.CHANGED);filter.addAction(Intent.ACTION_TIME_CHANGED);filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
  registerChanges(filter);registered=true;
 }
 // The two-argument overload is used only on Android 8–12, before receiver export flags exist.
 @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
 private void registerChanges(IntentFilter filter){if(Build.VERSION.SDK_INT>=33)host.registerReceiver(changes,filter,Context.RECEIVER_NOT_EXPORTED);else host.registerReceiver(changes,filter);}
 private int dp(float value){return Math.round(value*density);}
 private TextView text(String value,int size,int color){TextView view=new TextView(host);view.setText(value);view.setTextSize(size);view.setTextColor(color);view.setFontFeatureSettings("kern");view.setIncludeFontPadding(false);view.setTypeface(host.getResources().getFont(R.font.outfit_regular));return view;}
 private GradientDrawable background(boolean focused){return LauncherinSurface.card(host,70,focused);}

 private void focusable(View view){view.setId(View.generateViewId());view.setFocusable(true);view.setClickable(true);view.setBackground(background(false));view.setOnFocusChangeListener((v,focused)->{
  if(focused)host.launcherinChromeFocused();
  if("launcherin-history".equals(v.getTag())){
   float selected=1.035f*1.07f;v.animate().cancel();v.setTranslationZ(focused?dp(10):0);
   v.setForeground(focused?LauncherinSurface.outline(host,true,18):null);
   v.animate().scaleX(focused?selected:1).scaleY(focused?selected:1).setDuration(160).start();return;
  }
  v.setBackground("launcherin-widget".equals(v.getTag())?LauncherinWidgetAppearance.glass(host,widgetTransparency,focused):background(focused));float scale=focused?1.035f:1f;
  for(SpringAnimation animation:new SpringAnimation[]{new SpringAnimation(v,DynamicAnimation.SCALE_X,scale),new SpringAnimation(v,DynamicAnimation.SCALE_Y,scale)})animation.setSpring(new SpringForce(scale).setStiffness(440).setDampingRatio(.82f)).start();
 });}
 private FrameLayout widget(String label){FrameLayout frame=new FrameLayout(host);frame.setClipChildren(false);frame.setTag("launcherin-widget");focusable(frame);frame.setBackground(LauncherinWidgetAppearance.glass(host,widgetTransparency,false));frame.setOnLongClickListener(v->{transparency();return true;});String heading=label.substring(0,1)+label.substring(1).toLowerCase(Locale.forLanguageTag("pt-BR"));TextView kicker=text(heading,11,0xffa8a8af);kicker.setTag("appearance-heading");kicker.setTypeface(host.getResources().getFont(R.font.outfit_medium));kicker.setTextColor(LauncherinAppearance.get(host).muted);String glyph=label.contains("HORA")?"time":label.contains("TEMPO")?"weather":label.contains("TIMES")?"sports":label.contains("AGENDA")?"agenda":label.contains("RETRATOS")?"photo":"system";LauncherinGlyph mark=new LauncherinGlyph(glyph,LauncherinAppearance.get(host).accent);mark.setBounds(0,0,dp(12),dp(12));kicker.setCompoundDrawables(mark,null,null,null);kicker.setCompoundDrawablePadding(dp(5));put(frame,kicker,15,13,-1,18);widgets.add(frame);return frame;}
 private void face(FrameLayout card,View content){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-1);p.setMargins(dp(15),dp(33),dp(15),dp(10));card.addView(content,p);}
 private void put(FrameLayout parent,View view,int x,int y,int w,int h){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));p.leftMargin=dp(x);p.topMargin=dp(y);if(w==-1)p.rightMargin=dp(15);parent.addView(view,p);}
 private void build(){
  widgetRow=new FrameLayout(host);widgetRow.setTag("launcherin-widget-board");widgetRow.setClipChildren(false);widgetRow.setClipToPadding(false);layer.addView(widgetRow);
  FrameLayout time=widget("DATA E HORA");clockFace=new LauncherinWidgetFaces.Clock(host);face(time,clockFace);time.setOnClickListener(v->host.startActivity(new Intent(host,ClockActivity.class)));
  FrameLayout forecast=widget("PREVISÃO DO TEMPO");forecastWidget=forecast;weatherFace=new LauncherinWidgetFaces.Weather(host);face(forecast,weatherFace);forecast.setOnClickListener(v->expandWeather());
  audio=new LauncherinAudioWidget(host,preview);widgets.add(audio);
  FrameLayout modules=widget("TIMES FAVORITOS");sportsWidget=modules;sportsTitle=text("Próximos jogos",19,0xfff7f7fa);sportsDetail=text("Escolha até 2 times  ›",12,0xffc3c3cb);sportsTitle.setMaxLines(2);sportsTitle.setEllipsize(TextUtils.TruncateAt.END);sportsDetail.setSingleLine(true);sportsDetail.setEllipsize(TextUtils.TruncateAt.END);put(modules,sportsTitle,15,37,-1,43);put(modules,sportsDetail,15,85,-1,23);
  crests=new LinearLayout(host);crests.setGravity(Gravity.CENTER);crests.setVisibility(View.GONE);put(modules,crests,10,35,-1,47);modules.setOnClickListener(v->expandSports());modules.setOnLongClickListener(v->{widgetSettings();return true;});
  FrameLayout agenda=widget("AGENDA DO DIA"),photos=widget("PORTA-RETRATOS"),system=widget("SISTEMA");
  extraWidgets=new LauncherinExtraWidgets(host,io,agenda,photos,system);
  for(int i=0;i<LauncherinWidgetLayout.IDS.size();i++)allWidgets.put(LauncherinWidgetLayout.IDS.get(i),widgets.get(i));widgets.clear();
  quick=new LinearLayout(host);quick.setOrientation(LinearLayout.VERTICAL);quick.setGravity(Gravity.CENTER);layer.addView(quick);
  TextView tv=text("",13,-1);tv.setForeground(host.getDrawable(R.drawable.ic_live_tv));tv.setForegroundGravity(Gravity.CENTER);tv.getForeground().setBounds(0,0,dp(18),dp(18));tv.setGravity(Gravity.CENTER);focusable(tv);tv.setContentDescription("TV aberta");tv.setOnClickListener(v->host.launcherinLiveTv());quick.addView(tv,new LinearLayout.LayoutParams(-1,dp(34)));
  for(int id:new int[]{R.id.btnSettings,R.id.btnInputs}){TextView button=host.findViewById(id);((ViewGroup)button.getParent()).removeView(button);button.setGravity(Gravity.CENTER);button.setPadding(0,0,0,0);
   if(id==R.id.btnInputs){button.setText(null);button.setContentDescription("HDMI e entradas");button.setCompoundDrawables(null,null,null,null);button.setBackground(null);button.setForeground(host.getDrawable(R.drawable.ic_input));button.setForegroundGravity(Gravity.CENTER);}
   else{button.setText(null);button.setContentDescription("Configurações");button.setCompoundDrawables(null,null,null,null);button.setBackground(null);button.setForeground(host.getDrawable(R.drawable.ic_settings));button.setForegroundGravity(Gravity.CENTER);}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(34));p.topMargin=dp(7);quick.addView(button,p);}
  for(int i=0;i<quick.getChildCount();i++){final int index=i;View action=quick.getChildAt(i);action.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;if(key==KeyEvent.KEYCODE_DPAD_UP){if(index>0)quick.getChildAt(index-1).requestFocus();return true;}if(key==KeyEvent.KEYCODE_DPAD_DOWN){if(index+1<quick.getChildCount())quick.getChildAt(index+1).requestFocus();else focusBelow(3);return true;}if(key==KeyEvent.KEYCODE_DPAD_LEFT){focusUpper(Integer.MAX_VALUE);return true;}return key==KeyEvent.KEYCODE_DPAD_RIGHT;});action.setOnFocusChangeListener((v,focused)->{if(focused)host.launcherinChromeFocused();v.setBackground(LauncherinSurface.shortcut(host,focused));v.animate().scaleX(focused?1.04f:1).scaleY(focused?1.04f:1).setDuration(120).start();});}

  historyLabel=text("Continuar assistindo",18,0xffececf1);layer.addView(historyLabel);
  HorizontalScrollView scroll=new HorizontalScrollView(host);scroll.setId(View.generateViewId());scroll.setHorizontalScrollBarEnabled(false);scroll.setClipToPadding(false);scroll.setFillViewport(false);scroll.setClipChildren(false);scroll.setPadding(dp(14),dp(8),dp(14),dp(8));shelf=new LinearLayout(host);shelf.setOrientation(LinearLayout.HORIZONTAL);shelf.setClipChildren(false);scroll.addView(shelf,new HorizontalScrollView.LayoutParams(-2,-1));layer.addView(scroll);scroll.setTag("shelf");
  appsDock=new FrameLayout(host);appsDock.setTag("launcherin-apps");appsDock.setBackground(LauncherinWidgetAppearance.glass(host,82,false));appsDock.setClipToOutline(true);appsDock.setClipChildren(true);layer.addView(appsDock);
  while(grid.getItemDecorationCount()>0)grid.removeItemDecorationAt(0);
  appsDock.addView(grid,new FrameLayout.LayoutParams(-1,-1));LauncherinDockFocus.attach(appsDock,host.findViewById(R.id.glass));grid.setClipToPadding(true);grid.setClipChildren(false);grid.setPadding(dp(14),dp(10),dp(14),dp(10));
  if(preview){TextView badge=text("PRÉVIA · CONTEÚDOS DE EXEMPLO",9,0xff8e8e98);badge.setTag("preview");layer.addView(badge);}
  audio.navigation(()->focusNeighbor(audio,-1,0),()->focusNeighbor(audio,1,0),()->focusNeighbor(audio,0,1));
  seasonOverlay=new LauncherinSeasonOverlay(host,widgets,history,grid,appsDock);layer.addView(seasonOverlay,new FrameLayout.LayoutParams(-1,-1));seasonOverlay.theme(LauncherinSeason.selected(host));applyWidgetLayout();restyle();
 }
 private void bounds(View view,int left,int top,int width,int height){ViewGroup.LayoutParams current=view.getLayoutParams();if(current instanceof FrameLayout.LayoutParams){FrameLayout.LayoutParams old=(FrameLayout.LayoutParams)current;if(old.width==width&&old.height==height&&old.leftMargin==left&&old.topMargin==top)return;}FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(width,height);p.leftMargin=left;p.topMargin=top;view.setLayoutParams(p);}
 private void applyWidgetLayout(){
  List<LauncherinWidgetLayout.Slot> next=LauncherinWidgetLayout.read(LauncherinExtras.read(host));String mode=LauncherinWatchMode.clean(LauncherinExtras.read(host).optString("watch_next_mode"));String signature=LauncherinWidgetLayout.signature(next)+mode;if(signature.equals(widgetSignature))return;watchMode=mode;watchRevealed=false;layer.animate().cancel();layer.setTranslationY(0);
  View focused=host.getCurrentFocus();String focusId=null;for(Map.Entry<String,View> e:allWidgets.entrySet())if(e.getValue().hasFocus())focusId=e.getKey();
  dismissCards();widgetSignature=signature;widgetSlots=next;widgetRow.removeAllViews();widgets.clear();
  for(View card:allWidgets.values())card.setVisibility(View.GONE);
  List<LauncherinWidgetLayout.Slot> sorted=new ArrayList<>(next);sorted.sort(Comparator.comparingInt((LauncherinWidgetLayout.Slot a)->a.y).thenComparingInt(a->a.x));
  for(LauncherinWidgetLayout.Slot slot:sorted){if(!slot.enabled)continue;View card=allWidgets.get(slot.id);if(card==null)continue;card.setVisibility(View.VISIBLE);widgets.add(card);widgetRow.addView(card);
   card.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;if(key==KeyEvent.KEYCODE_DPAD_UP){focusNeighbor(v,0,-1);return true;}if(key==KeyEvent.KEYCODE_DPAD_DOWN){focusNeighbor(v,0,1);return true;}if(key==KeyEvent.KEYCODE_DPAD_LEFT){focusNeighbor(v,-1,0);return true;}if(key==KeyEvent.KEYCODE_DPAD_RIGHT){focusNeighbor(v,1,0);return true;}return false;});
  }
  widgetRow.setVisibility(widgets.isEmpty()?View.GONE:View.VISIBLE);if(!widgets.contains(audio))audio.pause();applyWidgetTransparency(widgetTransparency);layout(root.getWidth(),root.getHeight());
  if(focusId!=null){View keep=allWidgets.get(focusId);if(keep!=null&&widgets.contains(keep)){if(focused!=null&&focused.isShown())focused.requestFocus();else keep.requestFocus();}else focusUpper(0);}
 }
 private LauncherinWidgetLayout.Slot slotOf(View view){for(LauncherinWidgetLayout.Slot slot:widgetSlots)if(allWidgets.get(slot.id)==view)return slot;return null;}
 private boolean focusNeighbor(View from,int dx,int dy){
  LauncherinWidgetLayout.Slot at=slotOf(from),best=null;double score=Double.MAX_VALUE;
  if(at!=null)for(LauncherinWidgetLayout.Slot slot:widgetSlots){if(!slot.enabled||slot.id.equals(at.id))continue;
   double x=slot.x+slot.w*.5-(at.x+at.w*.5),y=slot.y+slot.h*.5-(at.y+at.h*.5);
   if((dx<0&&x>=0)||(dx>0&&x<=0)||(dy<0&&y>=0)||(dy>0&&y<=0))continue;
   double distance=dx!=0?Math.abs(x)+Math.abs(y)*20:Math.abs(y)+Math.abs(x)*4;if(distance<score){best=slot;score=distance;}
  }
  if(best!=null)return allWidgets.get(best.id).requestFocus();if(dy>0){focusBelow(at==null?0:(at.x+at.w/2)/3);return true;}if(dx>0)return quick.getChildAt(0).requestFocus();return false;
 }
 private boolean focusUpper(int index){
  if(widgets.isEmpty())return quick.getChildAt(0).requestFocus();int lastRow=0;for(LauncherinWidgetLayout.Slot s:widgetSlots)if(s.enabled)lastRow=Math.max(lastRow,s.y);
  List<View> bottom=new ArrayList<>();for(View view:widgets){LauncherinWidgetLayout.Slot s=slotOf(view);if(s!=null&&s.y+s.h>lastRow)bottom.add(view);}
  return bottom.get(Math.max(0,Math.min(index,bottom.size()-1))).requestFocus();
 }
 private void focusBelow(int index){if(!"above".equals(watchMode)||!focusHistory(index))host.launcherinFocusApps();}
 private void layout(int width,int height){if(width<=0||height<=0)return;
  int margin=Math.round(width*.046f),top=Math.round(height*.075f),widgetHeight=Math.round(height*.224f),rail=Math.round(width*.060f),gap=dp(12),rows=LauncherinWidgetLayout.usedRows(widgetSlots);
  int labelTop=Math.round(height*.38f),shelfTop=Math.round(height*.435f),shelfHeight=Math.round(height*.278f),dockTop=Math.round(height*.76f),dockHeight=Math.round(height*.205f);
  if(rows==2){top=Math.round(height*.04f);widgetHeight=dp(220);labelTop=top+widgetHeight+dp(12);shelfTop=labelTop+dp(27);dockHeight=Math.round(height*.185f);dockTop=height-Math.round(height*.035f)-dockHeight;shelfHeight=Math.max(dp(72),dockTop-dp(16)-shelfTop);}
  if(rows==0){labelTop=Math.round(height*.15f);shelfTop=Math.round(height*.205f);shelfHeight=Math.round(height*.36f);}
  if(!"above".equals(watchMode)){
   labelTop=height+dp(12);shelfTop=labelTop+dp(27);shelfHeight=Math.round(height*.278f);
  }
  historyLabel.setVisibility("hidden".equals(watchMode)?View.GONE:View.VISIBLE);layer.findViewWithTag("shelf").setVisibility("hidden".equals(watchMode)?View.GONE:View.VISIBLE);
  watchOffset=LauncherinWatchMode.revealOffset(height,shelfTop,shelfHeight,dp(16));
  ViewGroup.LayoutParams page=layer.getLayoutParams();int pageHeight="below".equals(watchMode)?height+watchOffset:-1;if(page.height!=pageHeight){page.height=pageHeight;layer.setLayoutParams(page);}
  if(watchRevealed)layer.setTranslationY(-watchOffset);
  int boardWidth=width-2*margin-rail-gap;bounds(widgetRow,margin,top,boardWidth,widgetHeight);bounds(quick,width-margin-rail,top,rail,rows==2?dp(122):widgetHeight);
  float cellWidth=(boardWidth+dp(12))/12f,cellHeight=(widgetHeight+dp(12))/(float)Math.max(1,rows);
  for(LauncherinWidgetLayout.Slot slot:widgetSlots)if(slot.enabled){View card=allWidgets.get(slot.id);if(card!=null){int left=Math.round(slot.x*cellWidth),y=Math.round(slot.y*cellHeight);bounds(card,left,y,Math.round((slot.x+slot.w)*cellWidth)-dp(12)-left,Math.round((slot.y+slot.h)*cellHeight)-dp(12)-y);}}
  bounds(historyLabel,margin+dp(3),labelTop,width-2*margin,dp(24));View scroll=layer.findViewWithTag("shelf");int shelfWidth=rows==0?boardWidth:width-2*margin;
  bounds(scroll,margin-dp(14),shelfTop,shelfWidth+dp(28),shelfHeight);bounds(appsDock,margin,dockTop,width-2*margin,dockHeight);
  View badge=layer.findViewWithTag("preview");if(badge!=null)bounds(badge,margin,height-dp(15),width-2*margin,dp(13));
  if(shelf.getChildCount()>0){int cardWidth=(shelfWidth-dp(42))/4;for(int i=0;i<shelf.getChildCount();i++){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)shelf.getChildAt(i).getLayoutParams();if(p.width!=cardWidth||p.height!=-1){p.width=cardWidth;p.height=-1;shelf.getChildAt(i).setLayoutParams(p);}}}
 }
 private void updateClock(){Date now=new Date();clockFace.update(android.text.format.DateFormat.getTimeFormat(host).format(now),new SimpleDateFormat("EEEE",Locale.forLanguageTag("pt-BR")).format(now),new SimpleDateFormat("d 'de' MMMM",Locale.forLanguageTag("pt-BR")).format(now));allWidgets.get("clock").setContentDescription(clockFace.getContentDescription());}
 private JSONArray items(){if(preview)try(InputStream in=host.getAssets().open("launcherin-preview/items.json")){return LauncherinHistory.visible(new JSONArray(new String(LauncherinExtras.bytes(in,65536),java.nio.charset.StandardCharsets.UTF_8)),LauncherinMediaService.removed(host));}catch(Exception ignored){}return LauncherinMediaService.history(host);}
 private void update(){updateClock();if(host.hasWindowFocus()){applyWidgetTransparency(LauncherinWidgetAppearance.transparency(host));seasonOverlay.theme(LauncherinSeason.selected(host));}if(widgets.contains(audio))audio.refresh(LauncherinMediaService.lastMusic(host));
  if("hidden".equals(watchMode))return;if(!forceHistory&&!history.isEmpty()&&history.contains(host.getCurrentFocus()))return;forceHistory=false;JSONArray rows=items();LauncherinTmdb.Settings artSettings=LauncherinTmdb.settings(host);String signature=rows.toString()+LauncherinMediaService.granted(host)+LauncherinWatchNext.granted(host)+artSettings.signature();if(signature.equals(historySignature))return;historySignature=signature;shelf.removeAllViews();history.clear();
  for(int i=0;i<rows.length();i++){JSONObject item=rows.optJSONObject(i);if(item==null)continue;FrameLayout card=new FrameLayout(host);card.setClipToOutline(true);card.setTag("launcherin-history");focusable(card);ImageView picture=new ImageView(host);picture.setScaleType(ImageView.ScaleType.CENTER_CROP);card.addView(picture,new FrameLayout.LayoutParams(-1,-1));
   View scrim=new View(host);scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0xe8000000}));FrameLayout.LayoutParams shade=new FrameLayout.LayoutParams(-1,dp(67),Gravity.BOTTOM);card.addView(scrim,shade);
   LauncherinTitle title=new LauncherinTitle(host,item.optString("title"));FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,dp(48),Gravity.BOTTOM|Gravity.LEFT);tp.leftMargin=dp(12);tp.rightMargin=dp(14);tp.bottomMargin=dp(37);card.addView(title,tp);
   ImageView provider=new ImageView(host);provider.setScaleType(ImageView.ScaleType.FIT_CENTER);provider.setContentDescription(item.optString("provider"));android.graphics.drawable.Drawable brand=providerIcon(item);provider.setImageDrawable(brand);
   provider.setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View view,Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),dp(7));}});provider.setClipToOutline(true);
   FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(dp(brand!=null&&brand.getIntrinsicWidth()>brand.getIntrinsicHeight()*1.3f?46:27),dp(27),Gravity.BOTTOM|Gravity.RIGHT);pp.rightMargin=dp(10);pp.bottomMargin=dp(9);card.addView(provider,pp);
   long duration=item.optLong("duration"),position=item.optLong("position");if(duration>0&&position>=0){View progress=new View(host);progress.setBackground(LauncherinSurface.progress(host));progress.setTag("watch-progress");FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(60),dp(3),Gravity.BOTTOM);card.addView(progress,p);card.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int want=Math.round((r-l)*Math.min(1f,(float)position/duration));if(p.width!=want){p.width=want;progress.setLayoutParams(p);}});}
   final int index=history.size();card.setOnLongClickListener(v->{historyMenu(item,index);return true;});card.setOnClickListener(v->{if(preview)Toast.makeText(host,"Conteúdo de exemplo da prévia",Toast.LENGTH_SHORT).show();else LauncherinMediaService.open(host,item);});card.setContentDescription(item.optString("title")+" · "+item.optString("provider"));
   card.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;lastHistory=index;if(key==KeyEvent.KEYCODE_MENU){historyMenu(item,index);return true;}if(key==KeyEvent.KEYCODE_DPAD_UP){if("below".equals(watchMode))closeBelowWatch();else focusUpper(index);return true;}if(key==KeyEvent.KEYCODE_DPAD_DOWN){if(LauncherinDockFocus.historyDownToApps(watchMode))host.launcherinFocusApps();return true;}return key==KeyEvent.KEYCODE_DPAD_LEFT&&index==0||key==KeyEvent.KEYCODE_DPAD_RIGHT&&index==history.size()-1;});
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(198),-1);p.rightMargin=dp(14);shelf.addView(card,p);history.add(card);load(picture,item.optString("art"));
   tmdb.request(item,artSettings,art->{if(!alive||card.getParent()!=shelf)return;if(!art.backdrop.isEmpty())load(picture,art.backdrop);if(!art.logo.isEmpty())load(title.image,art.logo,title::showLoadedLogo);if(!art.provider.isEmpty()){ViewGroup.LayoutParams size=provider.getLayoutParams();size.width=dp(46);provider.setLayoutParams(size);load(provider,art.provider,()->{if("Disney+".equals(item.optString("provider"))||"HBO Max".equals(item.optString("provider"))){GradientDrawable backing=new GradientDrawable();backing.setColor(0xd9f5f7fa);backing.setCornerRadius(dp(7));provider.setBackground(backing);provider.setPadding(dp(3),dp(2),dp(3),dp(2));}});}});
  }
  if(history.isEmpty()){TextView empty=text((LauncherinMediaService.granted(host)||LauncherinWatchNext.granted(host))?"Seus filmes e séries aparecerão aqui\nReproduza em um app compatível e volte à Launcherin":"Continue seus filmes e séries\nAtive o acesso a mídias para começar",15,0xffc7c7cf);empty.setPadding(dp(20),0,dp(20),0);empty.setGravity(Gravity.CENTER_VERTICAL);focusable(empty);empty.setOnClickListener(v->mediaAccess());empty.setOnKeyListener((v,key,event)->{if(event.getAction()!=0)return false;if(key==19){if("below".equals(watchMode))closeBelowWatch();else focusUpper(0);return true;}if(key==20){if(LauncherinDockFocus.historyDownToApps(watchMode))host.launcherinFocusApps();return true;}return key==21||key==22;});shelf.addView(empty,new LinearLayout.LayoutParams(dp(840),-1));history.add(empty);}
  layout(root.getWidth(),root.getHeight());
 }
 private void historyMenu(JSONObject item,int index){
  new AlertDialog.Builder(host).setTitle(item.optString("title")).setItems(new String[]{"Remover da lista","Escolher imagem e logo · TMDB"},(dialog,which)->{
   if(which==1){tmdb.choose(host,item,()->{historySignature=null;forceHistory=true;update();focusHistory(index);});return;}
   io.execute(()->{try{LauncherinMediaService.dismiss(host,item);handler.post(()->{if(!alive)return;historySignature=null;forceHistory=true;update();focusHistory(index);Toast.makeText(host,"Conteúdo removido da lista",Toast.LENGTH_SHORT).show();});}
    catch(Exception error){handler.post(()->Toast.makeText(host,"Não foi possível remover este conteúdo.",Toast.LENGTH_LONG).show());}});
  }).setNegativeButton("Voltar",null).show();
 }
 private void load(ImageView view,String source){load(view,source,null);}
 private void load(ImageView view,String source,Runnable ready){if(source.isEmpty())return;view.setTag(source);io.execute(()->{try{byte[] data;if(source.startsWith("asset://")){try(InputStream in=host.getAssets().open(source.substring(8))){data=LauncherinExtras.bytes(in,5000000);}}else if(source.startsWith("/")){File file=new File(source);if(!file.getCanonicalPath().startsWith(host.getFilesDir().getCanonicalPath()+"/")&&!file.getCanonicalPath().startsWith(new File(host.getCacheDir(),"tmdb").getCanonicalPath()+"/"))return;try(InputStream in=new FileInputStream(file)){data=LauncherinExtras.bytes(in,5000000);}}else if(source.startsWith("content://")||source.startsWith("android.resource://")){try(InputStream in=host.getContentResolver().openInputStream(android.net.Uri.parse(source))){data=LauncherinExtras.bytes(in,5000000);}}else if(source.startsWith("https://")){HttpURLConnection connection=(HttpURLConnection)new URL(source).openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(8000);try(InputStream in=connection.getInputStream()){data=LauncherinExtras.bytes(in,5000000);}finally{connection.disconnect();}}else return;BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,options);if(options.outWidth<=0||options.outHeight<=0||options.outWidth>32768||options.outHeight>32768)return;options.inSampleSize=1;while(options.outWidth/options.inSampleSize>Math.min(1280,Math.max(320,root.getWidth()/3))||options.outHeight/options.inSampleSize>Math.min(1280,Math.max(320,root.getWidth()/3)))options.inSampleSize*=2;options.inJustDecodeBounds=false;Bitmap image=BitmapFactory.decodeByteArray(data,0,data.length,options);handler.post(()->{if(alive&&image!=null&&source.equals(view.getTag())){view.setImageBitmap(image);if(ready!=null)ready.run();}else if(image!=null)image.recycle();});}catch(Exception ignored){}});}
 private android.graphics.drawable.Drawable providerIcon(JSONObject item){
  String pkg=item.optString("pkg"),provider=item.optString("provider");if(pkg.isEmpty()){switch(provider){case "Netflix":pkg="com.netflix.ninja";break;case "Prime Video":pkg="com.amazon.amazonvideo.livingroom";break;case "Disney+":pkg="com.disney.disneyplus";break;case "HBO Max":pkg="com.wbd.stream";break;case "YouTube":pkg="com.google.android.youtube.tv";break;}}
  try{android.graphics.drawable.Drawable banner=host.getPackageManager().getApplicationBanner(pkg);return banner!=null?banner:host.getPackageManager().getApplicationIcon(pkg);}catch(Exception ignored){return host.getDrawable(R.drawable.ic_apps);}
 }
 void restyle(){
  LauncherinAppearance.Style skin=LauncherinAppearance.get(host);applyWidgetTransparency(LauncherinWidgetAppearance.transparency(host));appsDock.setBackground(LauncherinSurface.card(host,72,false));historyLabel.setTextColor(skin.text);
  for(View v:history){v.setBackground(background(v.hasFocus()));if(v.hasFocus())v.setForeground(LauncherinSurface.outline(host,true,18));tint(v);}
  for(View v:allWidgets.values()){if(v!=audio)v.setBackground(LauncherinWidgetAppearance.glass(host,widgetTransparency,v.hasFocus()));tint(v);v.invalidate();}for(int i=0;i<quick.getChildCount();i++)quick.getChildAt(i).setBackground(LauncherinSurface.shortcut(host,quick.getChildAt(i).hasFocus()));audio.restyle();extraWidgets.restyle();if(weatherCard!=null)weatherCard.popup.restyle();if(sportsCard!=null)sportsCard.popup.restyle();
 }
 private void tint(View v){if(v instanceof TextView){TextView t=(TextView)v;boolean heading="appearance-heading".equals(t.getTag());t.setTypeface(host.getResources().getFont(heading?R.font.outfit_medium:R.font.outfit_regular));if(heading){t.setTextColor(LauncherinAppearance.get(host).muted);for(android.graphics.drawable.Drawable d:t.getCompoundDrawables())if(d instanceof LauncherinGlyph)((LauncherinGlyph)d).setTint(LauncherinAppearance.get(host).accent);}}if("watch-progress".equals(v.getTag()))v.setBackground(LauncherinSurface.progress(host));if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)tint(((ViewGroup)v).getChildAt(i));v.invalidate();}
 private void applyWidgetTransparency(int value){widgetTransparency=LauncherinWidgetAppearance.clamp(value);for(View widget:widgets){if(widget==audio)audio.setTransparency(widgetTransparency);else widget.setBackground(LauncherinWidgetAppearance.glass(host,widgetTransparency,widget.hasFocus()));}}
 private void transparency(){LauncherinWidgetAppearance.choose(host,this::applyWidgetTransparency);}
 private void widgetSettings(){new AlertDialog.Builder(host).setTitle("Widgets").setItems(new String[]{"Tema decorativo","Times favoritos","Cidade da previsão","Transparência dos widgets","Escolher módulos","Acesso a mídias","Imagens e logos dos conteúdos · TMDB"},(d,which)->{if(which==0)LauncherinSeason.choose(host,()->seasonOverlay.theme(LauncherinSeason.selected(host)));else if(which==1)chooseTeams();else if(which==2)chooseCity();else if(which==3)transparency();else if(which==4)host.launcherinModules();else if(which==5)mediaAccess();else tmdbSettings();}).setNegativeButton("Voltar",null).show();}
 void resume(){LauncherinMediaService.refreshSessions(host);visible=true;extraWidgets.active(true);seasonOverlay.active(true);applyWidgetLayout();if("below".equals(watchMode)&&history.contains(host.getCurrentFocus()))host.launcherinFocusApps();update();updateWeather();updateSports();loadPrograms(false);handler.removeCallbacks(minute);handler.post(minute);handler.removeCallbacks(extrasTick);handler.post(extrasTick);}
 void pause(){visible=false;watchRevealed=false;layer.animate().cancel();layer.setTranslationY(0);extraWidgets.active(false);handler.removeCallbacks(extrasTick);seasonOverlay.active(false);handler.removeCallbacks(minute);audio.pause();dismissCards();}
 void close(){tmdb.close();extraWidgets.active(false);dismissCards();seasonOverlay.active(false);audio.close();alive=false;handler.removeCallbacksAndMessages(null);io.shutdownNow();if(registered)host.unregisterReceiver(changes);}
 private void dismissCards(){if(weatherCard!=null)weatherCard.popup.dismiss();if(sportsCard!=null)sportsCard.popup.dismiss();}
 boolean focusHistory(int index){if(history.isEmpty())return false;return history.get(Math.max(0,Math.min(index,history.size()-1))).requestFocus();}
 boolean focusHistory(){return "above".equals(watchMode)&&focusHistory(lastHistory);}
 boolean appsDirection(int key){
  if(key==KeyEvent.KEYCODE_DPAD_DOWN){if("below".equals(watchMode)){revealWatch();focusHistory(lastHistory);}return true;}
  if("above".equals(watchMode))return focusHistory();
  if(watchRevealed)closeBelowWatch();else focusUpper(0);return true;
 }
 private void revealWatch(){watchRevealed=true;layer.animate().cancel();layer.animate().translationY(-watchOffset).setDuration(Config.load(host).reducedMotion?0:200).start();}
 boolean closeBelowWatch(){if(!watchRevealed)return false;watchRevealed=false;layer.animate().cancel();layer.animate().translationY(0).setDuration(Config.load(host).reducedMotion?0:200).withEndAction(()->{if(appsDock.hasFocus())host.launcherinFocusApps();}).start();host.launcherinFocusApps();return true;}
 void tmdbSettings(){
  EditText key=new EditText(host);key.setSingleLine(true);key.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setText(LauncherinTmdb.settings(host).key);
  LinearLayout content=new LinearLayout(host);content.setOrientation(LinearLayout.VERTICAL);content.addView(key);ImageView credit=new ImageView(host);credit.setImageResource(R.drawable.tmdb_logo);credit.setContentDescription("TMDB");credit.setScaleType(ImageView.ScaleType.FIT_CENTER);content.addView(credit,new LinearLayout.LayoutParams(dp(220),dp(32)));
  new AlertDialog.Builder(host).setTitle("Imagens e logos · TMDB").setMessage("Cole sua chave da API para usar imagens e logos. A logo substitui o nome escrito quando disponível. Apagar a chave desativa as consultas.\n\n"+LauncherinTmdb.CREDIT).setView(content)
   .setPositiveButton("Salvar",(d,w)->{String value=key.getText().toString().trim();if(!value.isEmpty()&&LauncherinTmdb.cleanKey(value).isEmpty()){Toast.makeText(host,"Confira a chave: são 32 caracteres.",Toast.LENGTH_LONG).show();return;}
    try{LauncherinExtras.setString(host,LauncherinTmdb.KEY,value);historySignature=null;forceHistory=true;update();}catch(Exception error){Toast.makeText(host,"Não foi possível salvar a chave.",Toast.LENGTH_LONG).show();}}).setNegativeButton("Voltar",null).show();
 }
 void mediaAccess(){
  String status="Histórico do Android TV: "+(LauncherinWatchNext.granted(host)?"autorizado":"não autorizado")+"\nControles de mídia: "+(LauncherinMediaService.granted(host)?"autorizados":"não autorizados");
  new AlertDialog.Builder(host).setTitle("Continuar assistindo e ouvindo").setMessage(status+"\n\nA Launcherin combina a lista pública do Android TV com as informações de reprodução compartilhadas pelos apps. Alguns serviços podem limitar os títulos e as imagens disponíveis.")
   .setPositiveButton("Histórico da TV",(d,w)->{if(!LauncherinWatchNext.granted(host))host.requestPermissions(new String[]{LauncherinWatchNext.PERMISSION},LauncherinWatchNext.REQUEST);else refreshPrograms();})
   .setNeutralButton("Controles de mídia",(d,w)->{try{host.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(Exception error){Toast.makeText(host,"Abra o acesso a notificações nas configurações da TV.",Toast.LENGTH_LONG).show();}}).setNegativeButton("Voltar",null).show();
 }
 void refreshPrograms(){loadPrograms(true);}
 private void loadPrograms(boolean force){
  if("hidden".equals(watchMode)||preview||!alive||programsLoading||!LauncherinWatchNext.granted(host)||(!force&&programsLoaded>0&&SystemClock.elapsedRealtime()-programsLoaded<30000))return;
  programsLoading=true;programsLoaded=SystemClock.elapsedRealtime();io.execute(()->{JSONArray rows=LauncherinWatchNext.read(host);try{if(rows!=null)LauncherinMediaService.importPrograms(host,rows);}catch(Exception ignored){}handler.post(()->{programsLoading=false;if(alive&&visible)update();});});
 }

 private void showWeather(LauncherinWeather.Snapshot value){
  weatherFace.update(value,System.currentTimeMillis());forecastWidget.setContentDescription(weatherFace.getContentDescription());
  if(weatherCard!=null)weatherCard.render(value,System.currentTimeMillis());
 }
 private void updateWeather(){if(!widgets.contains(forecastWidget))return;
  if(preview||!alive||!visible)return;String query=LauncherinWeather.query(host);
  if(query.isEmpty()){weatherFace.empty("Escolha sua cidade");forecastWidget.setContentDescription("Previsão do tempo. Escolha sua cidade.");return;}
  LauncherinWeather.Snapshot cached=LauncherinWeather.cached(host,query);if(cached!=null)showWeather(cached);
  if(weatherLoading)return;weatherLoading=true;if(cached==null)weatherFace.empty("Consultando o tempo…");
  io.execute(()->{LauncherinWeather.Snapshot result=null;try{result=LauncherinWeather.load(host,query);}catch(Exception ignored){}final LauncherinWeather.Snapshot value=result;
   handler.post(()->{weatherLoading=false;if(!alive||!visible||!query.equals(LauncherinWeather.query(host)))return;if(value!=null)showWeather(value);else{weatherFace.empty("Consulta indisponível");forecastWidget.setContentDescription("Previsão indisponível. Escolha sua cidade ou verifique a conexão.");if(weatherCard!=null)weatherCard.unavailable();}});
  });
 }
 private void expandWeather(){
  if(LauncherinWeather.query(host).isEmpty()&&!preview){chooseCity();return;}dismissCards();weatherCard=new LauncherinWeatherCard(host,forecastWidget,this::chooseCity);
  weatherCard.popup.onDismiss(()->weatherCard=null);if(preview){renderPreviewWeather();return;}
  LauncherinWeather.Snapshot cached=LauncherinWeather.cached(host,LauncherinWeather.query(host));if(cached!=null)weatherCard.render(cached,System.currentTimeMillis());updateWeather();
 }
 private void chooseTeams(){LauncherinTeamsPicker.choose(host,()->{sportsSelection="";updateSports();});}
 private void expandSports(){
  List<LauncherinSports.Team> teams=LauncherinSports.selected(host);if(teams.isEmpty()){chooseTeams();return;}
  dismissCards();updateSports();sportsCard=new LauncherinSportsCard(host,sportsWidget,teams,this::chooseTeams,io);sportsCard.popup.onDismiss(()->sportsCard=null);sportsCard.render(sportsData,sportsLoading);
 }
 private void showSports(List<LauncherinSports.Team> teams){
  updateCrests(teams);if(teams.isEmpty()){sportsTitle.setVisibility(View.VISIBLE);sportsTitle.setText("Próximos jogos");sportsDetail.setText("Escolha até 2 times  ›");return;}sportsTitle.setVisibility(View.GONE);
  List<LauncherinSports.Match> next=LauncherinSports.nextGames(teams,sportsData);StringBuilder description=new StringBuilder();
  for(LauncherinSports.Team team:teams){if(description.length()>0)description.append(" · ");description.append(team.name);}
  for(LauncherinSports.Match game:next)description.append(" · ").append(game.pairing()).append(" · ").append(LauncherinSportsCard.date(game));
  sportsDetail.setText(next.isEmpty()?(sportsLoading?"Consultando jogos…":"Nenhum jogo anunciado  ›"):"Próximos jogos  ›");
  sportsWidget.setContentDescription(description+" · Abra para os próximos dois jogos de cada time, último resultado e tabela do Brasileirão Série A.");if(sportsCard!=null)sportsCard.render(sportsData,sportsLoading);
 }
 private View compactCrest(String id,String name){
  ImageView badge=new ImageView(host);Bitmap image=id.matches("[0-9]{1,8}")?LauncherinTeamCrests.cached(host,id):null;
  if(image!=null)badge.setImageBitmap(image);else badge.setImageDrawable(new LauncherinGlyph("sports",LauncherinAppearance.get(host).muted));badge.setScaleType(ImageView.ScaleType.FIT_CENTER);badge.setContentDescription(name);return badge;
 }
 private void updateCrests(List<LauncherinSports.Team> teams){
  List<LauncherinSports.Match> games=LauncherinSports.nextGames(teams,sportsData);StringBuilder key=new StringBuilder();for(LauncherinSports.Team team:teams)key.append(team.id).append(':').append(LauncherinTeamCrests.stamp(host,team.id)).append(',');
  for(LauncherinSports.Match g:games){key.append(g.id).append(':').append(g.time).append(':').append(g.state);for(String id:new String[]{g.homeId,g.awayId})if(id.matches("[0-9]{1,8}"))key.append(':').append(LauncherinTeamCrests.stamp(host,id));}
  crests.setVisibility(teams.isEmpty()?View.GONE:View.VISIBLE);if(crestSignature.equals(key.toString()))return;crestSignature=key.toString();crests.removeAllViews();
  if(!games.isEmpty()){
   crests.setOrientation(LinearLayout.VERTICAL);
   for(LauncherinSports.Match g:games){LinearLayout line=new LinearLayout(host);line.setGravity(Gravity.CENTER_VERTICAL);line.addView(compactCrest(g.homeId,g.home),new LinearLayout.LayoutParams(dp(19),dp(19)));TextView versus=text("×",10,LauncherinAppearance.get(host).muted);versus.setGravity(Gravity.CENTER);line.addView(versus,new LinearLayout.LayoutParams(dp(14),-1));line.addView(compactCrest(g.awayId,g.away),new LinearLayout.LayoutParams(dp(19),dp(19)));TextView when=text(g.state.equals("in")?"AO VIVO":new SimpleDateFormat("dd/MM · HH:mm",Locale.forLanguageTag("pt-BR")).format(new Date(g.time)),10,LauncherinAppearance.get(host).text);when.setSingleLine(true);when.setEllipsize(TextUtils.TruncateAt.END);when.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams date=new LinearLayout.LayoutParams(0,-1,1);date.leftMargin=dp(9);line.addView(when,date);line.setContentDescription(g.pairing()+" · "+LauncherinSportsCard.date(g));crests.addView(line,new LinearLayout.LayoutParams(-1,dp(23)));}
   return;
  }
  crests.setOrientation(LinearLayout.HORIZONTAL);
  for(LauncherinSports.Team team:teams){LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(45),dp(45));size.leftMargin=dp(6);size.rightMargin=dp(6);crests.addView(compactCrest(team.id,team.name),size);}
 }
 private void updateSports(){if(!widgets.contains(sportsWidget))return;
  if(!alive||!visible)return;List<LauncherinSports.Team> teams=LauncherinSports.selected(host);String signature="";for(LauncherinSports.Team t:teams)signature+=t.id+",";
  if(!signature.equals(sportsSelection)){sportsSelection=signature;sportsData.clear();if(sportsCard!=null)sportsCard.popup.dismiss();}
  boolean fresh=!teams.isEmpty(),needsCrests=false;for(LauncherinSports.Team team:teams){LauncherinSports.Snapshot cached=LauncherinSports.cached(host,team.id);if(cached!=null)sportsData.put(team.id,cached);if(cached==null||cached.old())fresh=false;if(LauncherinTeamCrests.needsLoad(host,team.id))needsCrests=true;}
  for(LauncherinSports.Match game:LauncherinSports.nextGames(teams,sportsData))for(String id:new String[]{game.homeId,game.awayId})if(id.matches("[0-9]{1,8}")&&LauncherinTeamCrests.needsLoad(host,id))needsCrests=true;
  showSports(teams);if(teams.isEmpty()||fresh&&!needsCrests||sportsLoading)return;sportsLoading=true;showSports(teams);final String wanted=signature;
  io.execute(()->{Map<String,LauncherinSports.Snapshot> found=new HashMap<>();for(LauncherinSports.Team team:teams){LauncherinTeamCrests.load(host,team.id);try{found.put(team.id,LauncherinSports.load(host,team));}catch(Exception ignored){}}
   for(LauncherinSports.Match game:LauncherinSports.nextGames(teams,found))for(String id:new String[]{game.homeId,game.awayId})if(id.matches("[0-9]{1,8}"))LauncherinTeamCrests.load(host,id);
   handler.post(()->{sportsLoading=false;if(!alive||!visible||!wanted.equals(sportsSelection))return;sportsData.putAll(found);showSports(teams);});});
 }
 private void renderPreviewWeather(){
  try{JSONObject demo=LauncherinExtras.read(host).optJSONObject("debug_weather_preview");if(demo!=null&&weatherCard!=null)weatherCard.render(new LauncherinWeather.Snapshot(demo),System.currentTimeMillis());else if(weatherCard!=null)weatherCard.unavailable();}catch(Exception ignored){}
 }
 private void chooseCity(){
  EditText city=new EditText(host);city.setSingleLine(true);city.setHint("Ex.: Serra");city.setText(LauncherinWeather.query(host));
  new AlertDialog.Builder(host).setTitle("Cidade da previsão do tempo").setMessage("Digite o nome e escolha a cidade na lista. Dados: Open-Meteo (CC BY 4.0).").setView(city)
   .setPositiveButton("Buscar",(d,w)->{String query=LauncherinWeather.clean(city.getText().toString());if(query.length()<2){Toast.makeText(host,"Digite pelo menos duas letras.",Toast.LENGTH_SHORT).show();return;}
    ProgressDialog progress=ProgressDialog.show(host,"Buscando cidades","Aguarde…",true,true);
    io.execute(()->{List<LauncherinWeather.City> found=new ArrayList<>();try{found=LauncherinWeather.search(query);}catch(Exception ignored){}final List<LauncherinWeather.City> choices=found;
     handler.post(()->{if(!alive||!progress.isShowing())return;progress.dismiss();if(choices.isEmpty()){Toast.makeText(host,"Não foi possível encontrar a cidade. Verifique a conexão e tente outra busca.",Toast.LENGTH_LONG).show();return;}
      String[] labels=new String[choices.size()];for(int i=0;i<labels.length;i++)labels[i]=choices.get(i).label();
      new AlertDialog.Builder(host).setTitle("Escolha sua cidade").setItems(labels,(dialog,index)->{try{LauncherinWeather.select(host,choices.get(index));weatherLoading=false;updateWeather();}catch(Exception error){Toast.makeText(host,"Não foi possível salvar a cidade.",Toast.LENGTH_LONG).show();}}).setNegativeButton("Voltar",null).show();
     });
    });
   }).setNegativeButton("Voltar",null).show();
 }

}
