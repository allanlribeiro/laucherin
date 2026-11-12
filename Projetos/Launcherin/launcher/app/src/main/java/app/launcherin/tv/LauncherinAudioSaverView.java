// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Passive audio display. Content moves only at intervals; there is no continuous frame timer. */
final class LauncherinAudioSaverView extends FrameLayout {
    private final LauncherinMusicDesign design;private final LauncherinMusicArtwork images;
    private final ImageView background,cover;private final LinearLayout floating;private final TextView title,artist;
    private final String mode;private final List<String> photos;private final long started;
    private int step=-1;private String trackKey="",photoKey="";
    LauncherinAudioSaverView(Context c,String mode,LauncherinMusicArtwork images){super(c);this.design=new LauncherinMusicDesign(c);this.images=images;this.photos=LauncherinPlayerImages.photos(c);this.mode="photos".equals(mode)&&photos.isEmpty()?"cover":LauncherinAudioSaverPolicy.mode(mode);started=SystemClock.elapsedRealtime();
        setBackgroundColor(Color.BLACK);setFocusable(true);setFocusableInTouchMode(true);setContentDescription("Protetor de áudio. Pressione OK ou Voltar para retornar.");
        background=new ImageView(c);background.setScaleType(ImageView.ScaleType.FIT_CENTER);addView(background,new FrameLayout.LayoutParams(-1,-1));
        floating=new LinearLayout(c);boolean big="cover".equals(this.mode);floating.setOrientation(big?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);floating.setGravity(Gravity.CENTER_VERTICAL);floating.setPadding(design.dp(10),design.dp(10),design.dp(10),design.dp(10));
        if(!big)floating.setBackgroundColor(0x8a000000);else setBackground(design.backdrop());
        FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(design.dp(big?220:255),design.dp(big?270:68));addView(floating,position);
        cover=new ImageView(c);cover.setScaleType(ImageView.ScaleType.FIT_CENTER);int size=design.dp(big?176:"dark".equals(this.mode)?34:42);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(size,size);cp.gravity=Gravity.CENTER;floating.addView(cover,cp);
        LinearLayout labels=new LinearLayout(c);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(design.dp(big?0:12),design.dp(big?10:0),0,0);floating.addView(labels,new LinearLayout.LayoutParams(big?-1:0,-2,big?0:1));
        title=design.label("",big?17:14,"dark".equals(this.mode)?0xff929a95:0xffe3e9e5);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);artist=design.label("",big?13:12,0xffa8b2ac);artist.setSingleLine(true);artist.setEllipsize(TextUtils.TruncateAt.END);if(big){title.setGravity(Gravity.CENTER);artist.setGravity(Gravity.CENTER);}labels.addView(title);labels.addView(artist);if("dark".equals(this.mode))cover.setAlpha(.5f);
        addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->place(step<0?0:step));
    }
    private void place(int n){FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)floating.getLayoutParams();int[] point=LauncherinAudioSaverPolicy.point(n,getWidth(),getHeight(),p.width,p.height,design.dp(35));if(p.leftMargin!=point[0]||p.topMargin!=point[1]){p.leftMargin=point[0];p.topMargin=point[1];floating.setLayoutParams(p);}}
    void update(LauncherinTrack track,long now){if(track==null)return;if(!track.url.equals(trackKey)){trackKey=track.url;title.setText(track.title);artist.setText(track.artist);images.bind(cover,track.art,"cover".equals(mode)?384:96,"cover".equals(mode)?384:96,design.placeholder(track.title,false));}
        int next=(int)Math.max(0,(now-started)/20000);if(step!=next){step=next;place(step);if("photos".equals(mode)&&!photos.isEmpty()){String path=photos.get(Math.floorMod(step,photos.size()));if(!path.equals(photoKey)){photoKey=path;images.bind(background,path,1280,720,null);}}}images.visible();
    }
    String actualMode(){return mode;}
    void release(){images.forget(background);images.forget(cover);}
}
