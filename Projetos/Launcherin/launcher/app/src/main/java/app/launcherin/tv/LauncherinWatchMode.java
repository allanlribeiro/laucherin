package app.launcherin.tv;

final class LauncherinWatchMode {
    static String clean(String value){return "below".equals(value)||"hidden".equals(value)?value:"above";}
    static int revealOffset(int screenHeight,int shelfTop,int shelfHeight,int bottomGap){return Math.max(0,shelfTop+shelfHeight-(screenHeight-bottomGap));}
}
