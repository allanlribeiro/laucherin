package app.launcherin.tv;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import org.json.JSONObject;

/** Opens the publisher's content intent without flattening its playback arguments into a URL. */
final class LauncherinPlayback {
    enum Route { CONTENT, SESSION, APP, UNAVAILABLE }

    private LauncherinPlayback() {}

    static Intent contentIntent(String owner, String uri) {
        if (owner == null || owner.isEmpty() || uri == null || uri.trim().isEmpty()
                || uri.length() > 65536) return null;
        try {
            // The public TV provider can store serialized intents as well as plain URLs.
            // Parsing the complete value retains extras, action, type, component and categories.
            Intent intent = Intent.parseUri(uri, 0);
            if (intent.getPackage() != null && !owner.equals(intent.getPackage())) return null;
            if (intent.getComponent() != null
                    && !owner.equals(intent.getComponent().getPackageName())) return null;
            // A selector must not bypass the publishing package. Never forward URI grants,
            // ClipData, task-result flags or other capabilities from a stored external intent.
            intent.setSelector(null);
            intent.setClipData(null);
            intent.setSourceBounds(null);
            intent.setPackage(owner);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | (intent.getFlags()
                    & (Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)));
            if (intent.getData() != null) {
                String scheme = intent.getData().getScheme();
                if (scheme == null || "file".equalsIgnoreCase(scheme)
                        || "javascript".equalsIgnoreCase(scheme)
                        || "data".equalsIgnoreCase(scheme)) return null;
            } else if (intent.getComponent() == null && intent.getExtras() == null
                    && (intent.getAction() == null || Intent.ACTION_VIEW.equals(intent.getAction()))) {
                return null;
            }
            return intent;
        } catch (Exception ignored) {
            return null;
        }
    }

    static boolean sessionMatches(JSONObject item, String title, String mediaId) {
        String savedId = item.optString("media_id");
        // An ID supplied by the same app is stronger than the human-readable title.
        if (!savedId.isEmpty()) return savedId.equals(mediaId);
        String savedTitle = item.optString("title").trim();
        return !savedTitle.isEmpty() && title != null && savedTitle.equals(title.trim());
    }

    static Route open(Context context, JSONObject item) {
        String pkg = item.optString("pkg");
        if (LauncherinMediaService.provider(pkg).isEmpty() && !LauncherinMediaService.music(pkg))
            return Route.UNAVAILABLE;
        Intent content = contentIntent(pkg, item.optString("intent_uri"));
        if (content != null) {
            try { context.startActivity(content); return Route.CONTENT; }
            catch (RuntimeException ignored) { /* Try the same app's matching active session. */ }
        }
        try {
            MediaSessionManager manager = context.getSystemService(MediaSessionManager.class);
            if (manager != null && LauncherinMediaService.granted(context)) {
                for (MediaController controller : manager.getActiveSessions(
                        new ComponentName(context, LauncherinMediaService.class))) {
                    if (!pkg.equals(controller.getPackageName())) continue;
                    MediaMetadata metadata = controller.getMetadata();
                    if (metadata == null) continue;
                    String title = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
                    if (title == null || title.trim().isEmpty())
                        title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                    if (!sessionMatches(item, title,
                            metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID))) continue;
                    PendingIntent activity = controller.getSessionActivity();
                    if (activity == null || !pkg.equals(activity.getCreatorPackage())) continue;
                    try { activity.send(); return Route.SESSION; }
                    catch (PendingIntent.CanceledException ignored) { /* Keep trying. */ }
                }
            }
        } catch (RuntimeException ignored) { /* App launch remains available without media access. */ }
        try {
            Intent launch = context.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (launch == null) launch = context.getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) {
                context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return Route.APP;
            }
        } catch (RuntimeException ignored) { /* Disabled or removed app. */ }
        return Route.UNAVAILABLE;
    }
}
