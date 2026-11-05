package app.launcherin.tv;

import android.media.MediaMetadata;
import org.json.JSONObject;

/** Positive identification only: ordinary YouTube videos remain in Continue Watching. */
final class LauncherinMusic {
    private LauncherinMusic() {}
    static boolean dedicated(String pkg) {
        return pkg.equals("com.spotify.tv.android") || pkg.equals("com.spotify.music")
                || pkg.equals("com.google.android.apps.youtube.music")
                || pkg.equals("com.google.android.youtube.tvmusic")
                || pkg.equals("com.google.android.youtube.tv.music");
    }
    static String text(MediaMetadata metadata, String... keys) {
        if (metadata != null) for (String key : keys) {
            String value = metadata.getString(key);
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
    static boolean matches(String pkg, MediaMetadata metadata) {
        if (dedicated(pkg)) return true;
        if (!pkg.equals("com.google.android.youtube.tv")) return false;
        return identifiesTrack(pkg, text(metadata, MediaMetadata.METADATA_KEY_MEDIA_URI),
                text(metadata, MediaMetadata.METADATA_KEY_ALBUM), text(metadata, MediaMetadata.METADATA_KEY_ARTIST));
    }

    static boolean matchesSaved(String pkg,MediaMetadata metadata,JSONObject saved){
        if(matches(pkg,metadata))return true;
        String id=text(metadata,MediaMetadata.METADATA_KEY_MEDIA_ID);
        return pkg.equals("com.google.android.youtube.tv")&&saved.optBoolean("music")
                &&pkg.equals(saved.optString("pkg"))&&!id.isEmpty()&&id.equals(saved.optString("media_id"));
    }

    static boolean identifiesTrack(String pkg, String uri, String album, String artist) {
        if (dedicated(pkg)) return true;
        if (!pkg.equals("com.google.android.youtube.tv")) return false;
        try { if ("music.youtube.com".equalsIgnoreCase(new java.net.URI(uri).getHost())) return true; }
        catch (Exception ignored) { }
        return album != null && !album.trim().isEmpty() && artist != null && !artist.trim().isEmpty();
    }
}
