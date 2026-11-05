package app.launcherin.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/** Non-interactive decorations anchored to live cards, including their focus transforms and scroll. */
final class LauncherinSeasonOverlay extends View implements ViewTreeObserver.OnPreDrawListener {
    private final List<View> widgets,history;private final RecyclerView grid;private final FrameLayout dock;
    private final LauncherinChristmasArt art=new LauncherinChristmasArt();private final Path clip=new Path();
    private final int[] origin=new int[2],location=new int[2];private final float density;
    private boolean christmas,active;private long geometry=Long.MIN_VALUE;
    LauncherinSeasonOverlay(Context context,List<View> widgets,List<View> history,RecyclerView grid,FrameLayout dock){
        super(context);this.widgets=widgets;this.history=history;this.grid=grid;this.dock=dock;density=getResources().getDisplayMetrics().density;
        setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setWillNotDraw(false);setVisibility(GONE);
    }
    void theme(String theme){boolean next=LauncherinSeason.CHRISTMAS.equals(LauncherinSeason.clean(theme));if(christmas==next)return;christmas=next;geometry=Long.MIN_VALUE;setVisibility(next?VISIBLE:GONE);invalidate();}
    void active(boolean value){active=value;if(value)invalidate();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();getViewTreeObserver().addOnPreDrawListener(this);}
    @Override protected void onDetachedFromWindow(){if(getViewTreeObserver().isAlive())getViewTreeObserver().removeOnPreDrawListener(this);super.onDetachedFromWindow();}
    private long stamp(long value,View view){view.getLocationInWindow(location);value=value*31+location[0];value=value*31+location[1];value=value*31+view.getWidth();value=value*31+view.getHeight();value=value*31+Math.round(view.getScaleX()*1000);value=value*31+Math.round(view.getScaleY()*1000);return value;}
    @Override public boolean onPreDraw(){
        if(!christmas||!active||!isShown())return true;long next=stamp(17,this);
        for(View view:widgets)next=stamp(next,view);for(View view:history)next=stamp(next,view);
        for(int i=0;i<grid.getChildCount();i++){View row=grid.getChildAt(i),card=row.findViewById(R.id.card);next=next*31+grid.getChildAdapterPosition(row);if(card!=null)next=stamp(next,card);}
        if(next!=geometry){geometry=next;invalidate();}return true;
    }
    private int anchor(Canvas canvas,View view){view.getLocationInWindow(location);int saved=canvas.save();canvas.translate(location[0]-origin[0],location[1]-origin[1]);canvas.scale(view.getScaleX(),view.getScaleY());return saved;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(!christmas)return;getLocationInWindow(origin);
        for(int i=0;i<widgets.size();i++){
            View card=widgets.get(i);if(!card.isShown())continue;int saved=anchor(canvas,card);float w=card.getWidth(),h=card.getHeight();
            if(i==0)art.hat(canvas,w-14*density,2*density,39*density,18);
            else if(i==1)art.lights(canvas,w,density,8);
            else if(i==2)art.snowman(canvas,15*density,h+2*density,39*density,-8);
            else art.gifts(canvas,w-9*density,h+4*density,27*density);
            canvas.restoreToCount(saved);
        }
        if(!history.isEmpty()){
        int historySave=canvas.save();View viewport=(View)((View)history.get(0).getParent()).getParent();
        viewport.getLocationInWindow(location);canvas.clipRect(location[0]-origin[0],location[1]-origin[1]-22*density,location[0]-origin[0]+viewport.getWidth(),location[1]-origin[1]+viewport.getHeight()+8*density);
        for(int i=0;i<history.size();i++){
            View card=history.get(i);if(!card.isShown())continue;int saved=anchor(canvas,card);float w=card.getWidth(),h=card.getHeight();
            if(i%4==0)art.hat(canvas,23*density,7*density,31*density,-18);
            else if(i%4==1)art.lights(canvas,w,density*.82f,9);
            else if(i%4==2)art.snowman(canvas,w-2*density,h*.43f,33*density,-12);
            else art.gifts(canvas,w-8*density,10*density,27*density);
            canvas.restoreToCount(saved);
        }
        canvas.restoreToCount(historySave);
        }
        int dockSave=canvas.save();dock.getLocationInWindow(location);float left=location[0]-origin[0],top=location[1]-origin[1];clip.reset();clip.addRoundRect(left,top,left+dock.getWidth(),top+dock.getHeight(),18*density,18*density,Path.Direction.CW);canvas.clipPath(clip);
        for(int i=0;i<grid.getChildCount();i++){
            View row=grid.getChildAt(i),card=row.findViewById(R.id.card);int position=grid.getChildAdapterPosition(row);if(card==null||position<0||!card.isShown())continue;int saved=anchor(canvas,card);float w=card.getWidth(),h=card.getHeight();
            switch(position%4){case 0:art.hat(canvas,13*density,2*density,25*density,-17);break;case 1:art.lights(canvas,w,density*.7f,6);break;case 2:art.gifts(canvas,w-4*density,h+3*density,20*density);break;default:art.snowman(canvas,w-1*density,h+2*density,27*density,-8);break;}
            canvas.restoreToCount(saved);
        }
        canvas.restoreToCount(dockSave);
    }
}
