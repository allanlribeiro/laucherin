// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

/** Local music-screen timing only. Google Photos retains the system's existing Dream timer. */
final class LauncherinAudioSaverPolicy {
    static String mode(String s){return "photos".equals(s)||"dark".equals(s)||"google".equals(s)?s:"cover";}
    static int seconds(int value){return value==0||value==30||value==60||value==120||value==300?value:60;}
    static boolean start(String mode,int delay,boolean playing,boolean blocked,long last,long now){return !"google".equals(mode(mode))&&seconds(delay)>0&&playing&&!blocked&&now>=last&&now-last>=seconds(delay)*1000L;}
    static int[] point(int step,int width,int height,int cardWidth,int cardHeight,int margin){
        int[] xs={0,2,1,0,2,1,2,0},ys={0,2,1,2,0,0,1,1};int n=Math.floorMod(step,8);int x=Math.max(0,Math.min(margin,(width-cardWidth)/2)),y=Math.max(0,Math.min(margin,(height-cardHeight)/2));
        return new int[]{x+Math.max(0,width-cardWidth-2*x)*xs[n]/2,y+Math.max(0,height-cardHeight-2*y)*ys[n]/2};
    }
}
