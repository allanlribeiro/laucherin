// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;
import android.content.Context;import android.graphics.Color;import android.os.SystemClock;import android.view.*;import android.widget.*;
/** Passive overlay: controls remain in the existing screen and resume unchanged on dismissal. */
final class LauncherinCircularScreen extends FrameLayout {
 final LauncherinCircularView circle;private final LauncherinMusicArtwork images;private LauncherinVisualOptions options;private final boolean manualDark;private String key="";private int priorStage=-1;private final long start=SystemClock.elapsedRealtime();
 LauncherinCircularScreen(Context c,LauncherinVisualOptions o,LauncherinMusicArtwork images,boolean dark){super(c);this.images=images;options=o;manualDark=dark;setBackgroundColor(Color.BLACK);setFocusable(true);setFocusableInTouchMode(true);setContentDescription(dark?"Tela preta. O painel LED continua iluminado. Pressione uma tecla para voltar.":"Proteção musical. Pressione uma tecla para voltar.");circle=new LauncherinCircularView(c,o);LayoutParams p=new LayoutParams(1,1,Gravity.CENTER);addView(circle,p);}
 void update(LauncherinTrack track,int session,boolean playing,int stage,long now){if(track==null)return;boolean black=manualDark||options.dark;if(black){circle.setVisibility(GONE);circle.activate(false);return;}circle.setVisibility(VISIBLE);{int bound=Math.min(getWidth(),getHeight());int n=Math.max(100,Math.min(new LauncherinMusicDesign(getContext()).dp(stage>=3?200:360),(int)(bound*.7f)));LayoutParams p=(LayoutParams)circle.getLayoutParams();if(p.width!=n){p.width=n;p.height=n;circle.setLayoutParams(p);}priorStage=stage;}
  if(!track.url.equals(key)){key=track.url;images.bind(circle.cover,track.art,480,480,new LauncherinMusicDesign(getContext()).placeholder(track.title,true));circle.track(key);}circle.activate(true);circle.update(session,playing,stage>=3,stage>=3?.3f:.5f);
  if(options.motion){double t=(now-start)/1000.0;circle.setTranslationX((float)Math.sin(t/83)*getWidth()*.15f);circle.setTranslationY((float)Math.sin(t/107)*getHeight()*.08f);}else{circle.setTranslationX(0);circle.setTranslationY(0);}images.visible();
 }
 void release(){circle.close();images.forget(circle.cover);}
}
