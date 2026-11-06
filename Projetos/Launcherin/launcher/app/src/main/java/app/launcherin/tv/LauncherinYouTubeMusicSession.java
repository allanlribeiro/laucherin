package app.launcherin.tv;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.view.KeyEvent;
import org.json.JSONObject;
import java.util.List;

/** Controls the publisher's own media session; never extracts audio or sends global media keys. */
final class LauncherinYouTubeMusicSession {
    private LauncherinYouTubeMusicSession() {}

    private static MediaController active(Context context) {
        if (!LauncherinMediaService.granted(context)) return null;
        try {
            MediaSessionManager manager = context.getSystemService(MediaSessionManager.class);
            if (manager == null) return null;
            JSONObject saved = LauncherinMediaService.lastMusic(context);
            List<MediaController> sessions = manager.getActiveSessions(
                    new ComponentName(context, LauncherinMediaService.class));
            MediaController best = null;
            int score = -1;
            for (MediaController controller : sessions) {
                String pkg = controller.getPackageName();
                if (!LauncherinYouTubeMusicPolicy.youtubePackage(pkg)
                        || !LauncherinMusic.matchesSaved(pkg, controller.getMetadata(), saved)) continue;
                PlaybackState state = controller.getPlaybackState();
                int s = state == null ? 0 : state.getState() == PlaybackState.STATE_PLAYING ? 3
                        : state.getState() == PlaybackState.STATE_BUFFERING ? 2
                        : state.getState() == PlaybackState.STATE_PAUSED ? 1 : 0;
                if (best == null || s > score) { best = controller; score = s; }
            }
            return best;
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    static JSONObject snapshot(Context context) {
        JSONObject saved = LauncherinMediaService.lastMusic(context);
        MediaController controller = active(context);
        if (controller == null) {
            if (!saved.optBoolean("music") ||
                    !LauncherinYouTubeMusicPolicy.youtubePackage(saved.optString("pkg")))
                return new JSONObject();
            try { JSONObject paused = new JSONObject(saved.toString()); paused.put("playing", false); return paused; }
            catch (Exception ignored) { return new JSONObject(); }
        }
        MediaMetadata md = controller.getMetadata();
        PlaybackState state = controller.getPlaybackState();
        String pkg = controller.getPackageName();
        String title = LauncherinMusic.text(md, MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
                MediaMetadata.METADATA_KEY_TITLE);
        String artist = LauncherinMusic.text(md, MediaMetadata.METADATA_KEY_ARTIST,
                MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        String id = LauncherinMusic.text(md, MediaMetadata.METADATA_KEY_MEDIA_ID);
        boolean same = pkg.equals(saved.optString("pkg")) &&
                ((!id.isEmpty() && id.equals(saved.optString("media_id"))) ||
                        (!title.isEmpty() && title.equals(saved.optString("title"))));
        if (title.isEmpty() && same) title = saved.optString("title");
        if (artist.isEmpty() && same) artist = saved.optString("artist");
        JSONObject result = new JSONObject();
        try {
            result.put("pkg", pkg);
            result.put("title", title);
            result.put("artist", artist);
            result.put("art", same ? saved.optString("art") : "");
            result.put("playing", state != null && state.getState() == PlaybackState.STATE_PLAYING);
            result.put("duration", md == null ? 0L : Math.max(0L, md.getLong(MediaMetadata.METADATA_KEY_DURATION)));
            result.put("position", state == null ? 0L : Math.max(0L, state.getPosition()));
            result.put("capturedElapsed", SystemClock.elapsedRealtime());
        } catch (Exception ignored) { /* A partial result is safe for the view. */ }
        return result;
    }

    static boolean toggle(Context context) {
        MediaController controller = active(context);
        if (controller == null) return false;
        PlaybackState state = controller.getPlaybackState();
        long actions = state == null ? 0 : state.getActions();
        boolean playing = state != null && LauncherinMusicCommand.started(state.getState());
        try {
            if (playing && (actions & PlaybackState.ACTION_PAUSE) != 0)
                controller.getTransportControls().pause();
            else if (!playing && (actions & PlaybackState.ACTION_PLAY) != 0)
                controller.getTransportControls().play();
            else if ((actions & PlaybackState.ACTION_PLAY_PAUSE) != 0) {
                long now = SystemClock.uptimeMillis();
                controller.dispatchMediaButtonEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0));
                controller.dispatchMediaButtonEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0));
            } else return false;
            return true;
        } catch (RuntimeException ignored) { return false; }
    }

    static boolean skip(Context context, boolean next) {
        MediaController controller = active(context);
        if (controller == null) return false;
        PlaybackState state = controller.getPlaybackState();
        long required = next ? PlaybackState.ACTION_SKIP_TO_NEXT : PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        if (state == null || (state.getActions() & required) == 0) return false;
        try {
            if (next) controller.getTransportControls().skipToNext();
            else controller.getTransportControls().skipToPrevious();
            return true;
        } catch (RuntimeException ignored) { return false; }
    }

    static boolean seek(Context context, long millis) {
        MediaController controller = active(context);
        if (controller == null) return false;
        PlaybackState state = controller.getPlaybackState();
        if (state == null || (state.getActions() & PlaybackState.ACTION_SEEK_TO) == 0) return false;
        try {
            controller.getTransportControls().seekTo(Math.max(0, millis));
            return true;
        } catch (RuntimeException ignored) { return false; }
    }

    /** Opens only an installed TV-compatible publisher. No credentials are handled by Launcherin. */
    static boolean openOfficial(Context context) {
        PackageManager packages = context.getPackageManager();
        String[] known = { "com.google.android.youtube.tvmusic",
                "com.google.android.youtube.tv.music",
                "com.google.android.apps.youtube.music",
                "com.google.android.youtube.tv" };
        for (String pkg : known) {
            try {
                Intent intent = packages.getLeanbackLaunchIntentForPackage(pkg);
                if (intent == null) intent = packages.getLaunchIntentForPackage(pkg);
                if (intent == null) continue;
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            } catch (RuntimeException ignored) { /* Try next installed version. */ }
        }
        return false;
    }
}
