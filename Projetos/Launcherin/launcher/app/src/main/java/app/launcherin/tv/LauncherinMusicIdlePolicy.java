package app.launcherin.tv;

/** Suspend only standby during audio; the photo timer remains independent. */
final class LauncherinMusicIdlePolicy {
    static final class Decision {
        final boolean hold;
        final long pausedAt, checkAfter;
        Decision(boolean hold,long pausedAt,long checkAfter){this.hold=hold;this.pausedAt=pausedAt;this.checkAfter=checkAfter;}
    }
    static Decision decide(boolean held,long pausedAt,Boolean playing,long now,int minutes) {
        if(minutes<=0)return new Decision(false,-1,-1);
        if(playing==null)return new Decision(true,-1,10000);
        if(playing)return new Decision(true,-1,10000);
        if(!held)return new Decision(false,-1,-1);
        if(pausedAt<0||pausedAt>now)pausedAt=now;
        long remaining=pausedAt+minutes*60000L-now;
        return remaining>0?new Decision(true,pausedAt,remaining):new Decision(false,-1,-1);
    }
    static boolean legacyDreamShortcut(int sdk){return sdk<34;}
}
