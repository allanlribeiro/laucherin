package app.launcherin.tv;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=35,qualifiers="w960dp-h540dp-land")
public class LauncherinDockFocusAndroidTest {
 @Test public void downAtTheLastShelfDoesNotJumpBackToTheAppsAbove(){
  assertFalse(LauncherinDockFocus.historyDownToApps("below"));
  assertTrue(LauncherinDockFocus.historyDownToApps("above"));
  assertFalse(LauncherinDockFocus.historyDownToApps("hidden"));
 }
 @Test public void highlightMovesWithTheAppRowWhenTheLowerShelfIsRevealed(){
  Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
  FrameLayout root=new FrameLayout(activity),page=new FrameLayout(activity),dock=new FrameLayout(activity);
  View card=new View(activity),highlight=new View(activity);
  root.addView(page,new FrameLayout.LayoutParams(-1,-1));
  FrameLayout.LayoutParams dockParams=new FrameLayout.LayoutParams(900,100);dockParams.topMargin=300;page.addView(dock,dockParams);
  FrameLayout.LayoutParams cardParams=new FrameLayout.LayoutParams(160,60);cardParams.leftMargin=100;cardParams.topMargin=20;dock.addView(card,cardParams);
  root.addView(highlight,new FrameLayout.LayoutParams(1,1));
  LauncherinDockFocus.attach(dock,highlight);highlight.setLayoutParams(new FrameLayout.LayoutParams(170,70));highlight.setTranslationX(95);highlight.setTranslationY(15);
  activity.setContentView(root);root.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));root.layout(0,0,960,540);
  for(float shift:new float[]{0,-120,-200,0}){
   page.setTranslationY(shift);int[] c=new int[2],h=new int[2];card.getLocationOnScreen(c);highlight.getLocationOnScreen(h);
   assertEquals(5,c[0]-h[0]);assertEquals(5,c[1]-h[1]);
  }
  assertSame(dock,highlight.getParent());assertTrue(dock.getClipChildren());
  activity.finish();
 }
 @Test public void oversizedHighlightCannotDrawIntoTheContentRow(){
  Activity activity=Robolectric.buildActivity(Activity.class).setup().get();FrameLayout root=new FrameLayout(activity),dock=new FrameLayout(activity);
  FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(800,100);p.topMargin=150;root.addView(dock,p);
  View highlight=new View(activity);root.addView(highlight);LauncherinDockFocus.attach(dock,highlight);highlight.setLayoutParams(new FrameLayout.LayoutParams(200,300));
  activity.setContentView(root);root.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));root.layout(0,0,960,540);
  Rect visible=new Rect(),band=new Rect();assertTrue(highlight.getGlobalVisibleRect(visible));assertTrue(dock.getGlobalVisibleRect(band));assertTrue(visible.bottom<=band.bottom);
  activity.finish();
 }
}
