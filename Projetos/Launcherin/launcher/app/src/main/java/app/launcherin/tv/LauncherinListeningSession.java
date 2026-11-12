// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

/** Measures audible progress, excluding seeks, pauses and buffering. One outcome per play. */
final class LauncherinListeningSession {
    LauncherinTrack track;
    private long heard,lastPosition,lastTime;private boolean playing;
    void sample(LauncherinTrack value,boolean audible,long position,long now){
        if(track==null){track=value;heard=0;lastPosition=position;lastTime=now;playing=audible;return;}
        if(playing&&now>=lastTime&&position>=lastPosition)heard+=Math.min(position-lastPosition,now-lastTime+250);
        lastPosition=position;lastTime=now;playing=audible;
    }
    void seek(long position,long now){lastPosition=position;lastTime=now;}
    long heard(){return heard;}
    void clear(){track=null;heard=0;playing=false;}
}
