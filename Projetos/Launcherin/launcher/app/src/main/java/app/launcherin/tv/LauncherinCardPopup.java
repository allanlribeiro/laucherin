package app.launcherin.tv;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/** Focusable anchored expansion. The wallpaper and other home rows remain visible. */
final class LauncherinCardPopup {
    final Activity host;final FrameLayout card;final LinearLayout content;final PopupWindow window;
    final int width,height;final boolean motion;private final View anchor;private Runnable dismissed;
    LauncherinCardPopup(Activity host,View anchor,int desiredWidth,int desiredHeight) {
        this.host=host;this.anchor=anchor;Config config=Config.load(host);motion=config.animations&&!config.reducedMotion;View root=anchor.getRootView();
        width=Math.min(dp(desiredWidth),Math.round(root.getWidth()*.78f));height=Math.min(dp(desiredHeight),Math.round(root.getHeight()*.70f));
        card=new FrameLayout(host);card.setBackground(LauncherinWidgetAppearance.glass(host,LauncherinWidgetAppearance.transparency(host),true));card.setClipToOutline(true);card.setElevation(dp(14));
        View backdrop=new View(host){private final int[] at=new int[2];@Override protected void onDraw(android.graphics.Canvas canvas){getLocationOnScreen(at);if(host instanceof MainActivity)((MainActivity)host).launcherinCardBackdrop(canvas,at[0],at[1]);android.graphics.drawable.Drawable glass=card.getBackground();glass.setBounds(0,0,getWidth(),getHeight());glass.draw(canvas);}};
        backdrop.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));
        content=new LinearLayout(host);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(22),dp(17),dp(22),dp(14));card.addView(content,new FrameLayout.LayoutParams(-1,-1));
        window=new PopupWindow(card,width,height,true);window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setOutsideTouchable(true);window.setElevation(dp(14));window.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        window.setOnDismissListener(()->{card.animate().cancel();if(dismissed!=null)dismissed.run();if(anchor.isAttachedToWindow())anchor.requestFocus();});
    }
    int dp(float n){return Math.round(n*host.getResources().getDisplayMetrics().density);}
    TextView label(String text,float size,int color){TextView view=new TextView(host);view.setText(text);view.setTextSize(size);view.setTextColor(color);view.setIncludeFontPadding(false);view.setTypeface(host.getResources().getFont(R.font.outfit_regular));view.setShadowLayer(dp(1),0,dp(1),0x55000000);return view;}
    TextView button(String text,Runnable action){TextView v=label(text,13,Color.WHITE);v.setGravity(Gravity.CENTER);v.setPadding(dp(13),dp(7),dp(13),dp(7));v.setFocusable(true);v.setClickable(true);v.setBackground(LauncherinWidgetAppearance.glass(host,65,false));v.setOnClickListener(w->action.run());v.setOnFocusChangeListener((w,focused)->v.setBackground(LauncherinWidgetAppearance.glass(host,65,focused)));return v;}
    void restyle(){card.setBackground(LauncherinSurface.card(host,LauncherinWidgetAppearance.transparency(host),true));styleText(content);card.invalidate();}
    private void styleText(View view){if(view instanceof TextView){TextView t=(TextView)view;t.setTypeface(host.getResources().getFont(t.getTextSize()/host.getResources().getDisplayMetrics().scaledDensity>=16?R.font.outfit_medium:R.font.outfit_regular));}if("expanded-surface".equals(view.getTag()))view.setBackground(LauncherinSurface.card(host,65,false));if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)styleText(((ViewGroup)view).getChildAt(i));}
    void onDismiss(Runnable listener){dismissed=listener;}
    boolean showing(){return window.isShowing();}
    void dismiss(){window.dismiss();}
    void show(){
        int[] location=new int[2];anchor.getLocationOnScreen(location);View root=anchor.getRootView();int[] origin=new int[2];root.getLocationOnScreen(origin);
        int x=Math.max(origin[0]+dp(22),Math.min(location[0],origin[0]+root.getWidth()-width-dp(22))),y=Math.max(origin[1]+dp(16),Math.min(location[1],origin[1]+root.getHeight()-height-dp(16)));
        card.setPivotX(Math.max(0,Math.min(width,location[0]-x+anchor.getWidth()/2f)));card.setPivotY(0);
        if(motion&&ValueAnimator.areAnimatorsEnabled()){card.setAlpha(0);card.setScaleX(.94f);card.setScaleY(.94f);}
        window.showAtLocation(root,Gravity.TOP|Gravity.LEFT,x,y);
        card.animate().alpha(1).scaleX(1).scaleY(1).setDuration(motion&&ValueAnimator.areAnimatorsEnabled()?180:0).start();
    }
}
