package app.launcherin.tv;

/** Pure, testable rules for the optional YouTube Music panel. */
final class LauncherinYouTubeMusicPolicy {
    private LauncherinYouTubeMusicPolicy() {}

    static boolean youtubePackage(String pkg) {
        if (pkg == null) return false;
        return pkg.equals("com.google.android.apps.youtube.music")
                || pkg.equals("com.google.android.youtube.tvmusic")
                || pkg.equals("com.google.android.youtube.tv.music")
                || pkg.equals("com.google.android.youtube.tv");
    }

    static long position(long position, long observedAt, long now, boolean playing, long duration) {
        long result = Math.max(0L, position);
        if (playing && observedAt > 0 && now > observedAt)
            result += Math.min(now - observedAt, 60_000L);
        return duration > 0 ? Math.min(result, duration) : result;
    }

    static int progress(long position, long duration) {
        if (duration <= 0) return 0;
        return (int) Math.max(0, Math.min(1000,
                Math.round(1000d * Math.max(0, position) / duration)));
    }

    static String clock(long millis) {
        long seconds = Math.max(0L, millis) / 1000;
        return (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
    }
}
