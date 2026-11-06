package app.launcherin.tv;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/** The highlight travels with the app row and cannot draw across the content shelf. */
final class LauncherinDockFocus {
    static void attach(FrameLayout dock,View highlight){
        ViewGroup previous=(ViewGroup)highlight.getParent();
        if(previous!=null)previous.removeView(highlight);
        dock.setClipChildren(true);dock.setClipToPadding(true);
        dock.addView(highlight,new FrameLayout.LayoutParams(1,1));
    }
    static boolean historyDownToApps(String mode){return "above".equals(LauncherinWatchMode.clean(mode));}
}
