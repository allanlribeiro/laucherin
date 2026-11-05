package app.launcherin.tv;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONObject;

/** Uses the real Android Intent parser; no runtime or APK dependency on a test framework. */
public final class PlaybackInstrumentation extends Instrumentation {
    private static final String OWNER = "com.netflix.ninja";
    private Bundle options;
    private int passed;
    private final StringBuilder report = new StringBuilder();
    private interface Check { void run() throws Exception; }

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); options=arguments; start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            if(options!=null&&"true".equals(options.getString("widgets_only"))){
                WidgetChecks checks=new WidgetChecks(this);
                check("Seven widgets fit their chosen grid positions and photos work offline",checks::layoutAndOfflinePhotos);
                checks.screenshot("widgets-preview10.png");
                check("Agenda commits atomically without configuration or HOME restart",checks::agendaDoesNotRestartOrChangeConfiguration);
                check("Hidden widgets and history leave no placeholders or broken focus",checks::hiddenWidgetsAndHistoryHaveNoPlaceholders);
                check("Down reveals history below apps and Up returns; apps stay visible",checks::belowHistoryKeepsTheAppBarVisible);
                check("RAM comes from actual system memory counters",checks::systemShowsRealMemory);
                result.putString("stream","\n"+report+"Passed "+passed+" widget checks.\n");result.putInt("passed",passed);finish(Activity.RESULT_OK,result);return;
            }
            check("component-only playback keeps episode and typed position", () -> {
                Intent original = new Intent(Intent.ACTION_VIEW)
                        .setComponent(new ComponentName(OWNER, OWNER + ".PlaybackActivity"))
                        .putExtra("episode_id", "season-1/episode-2")
                        .putExtra("position_ms", 123456L).putExtra("resume", true);
                Intent opened = required(original.toUri(Intent.URI_INTENT_SCHEME));
                require(opened.getData() == null, "component-only intent gained data");
                require(original.getComponent().equals(opened.getComponent()), "component lost");
                require("season-1/episode-2".equals(opened.getStringExtra("episode_id")), "episode lost");
                require(opened.getLongExtra("position_ms", -1) == 123456L, "typed position lost");
                require(opened.getBooleanExtra("resume", false), "resume flag lost");
            });
            check("Watch Next toUri(0) preserves custom action, type and categories", () -> {
                Intent original = new Intent(OWNER + ".PLAY_EPISODE")
                        .setDataAndType(Uri.parse("streaming://watch/episode-2"), "video/example")
                        .setPackage(OWNER).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                        .putExtra("episode_id", "episode-2").putExtra("position_ms", 98765);
                Intent opened = required(original.toUri(0));
                require(original.getAction().equals(opened.getAction()), "action replaced");
                require(original.getData().equals(opened.getData()), "data changed");
                require(original.getType().equals(opened.getType()), "type lost");
                require(opened.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), "category lost");
                require(!opened.hasCategory(Intent.CATEGORY_BROWSABLE), "added a category the publisher did not require");
                require(opened.getIntExtra("position_ms", -1) == 98765, "integer extra lost");
            });
            check("plain HTTPS links keep query and fragment", () -> {
                String uri = "https://example.com/watch/episode-2?resume=true&t=123#player";
                Intent opened = required(uri);
                require(uri.equals(opened.getDataString()), "URL changed");
                require(Intent.ACTION_VIEW.equals(opened.getAction()), "wrong URL action");
            });
            check("app scheme links keep their content ID", () -> {
                require("streaming://watch/episode-2".equals(required("streaming://watch/episode-2").getDataString()), "app URL changed");
            });
            check("android-app URIs stay in the publishing package", () -> {
                Intent original = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://example.com/watch/episode-2")).setPackage(OWNER);
                Intent opened = required(original.toUri(Intent.URI_ANDROID_APP_SCHEME));
                require(original.getData().equals(opened.getData()), "android-app data changed");
            });
            check("another package cannot be targeted", () -> {
                String uri = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/"))
                        .setPackage("other.streaming.app").toUri(0);
                require(LauncherinPlayback.contentIntent(OWNER, uri) == null, "foreign package accepted");
            });
            check("another package's explicit component cannot escape the scope", () -> {
                String uri = new Intent(Intent.ACTION_VIEW)
                        .setComponent(new ComponentName("other.streaming.app", "other.streaming.app.Play"))
                        .putExtra("episode_id", "episode-2").toUri(0);
                require(LauncherinPlayback.contentIntent(OWNER, uri) == null, "foreign component accepted");
            });
            check("selector cannot redirect playback into another package", () -> {
                Intent original = new Intent(Intent.ACTION_VIEW, Uri.parse("streaming://episode-2"));
                original.setSelector(new Intent(Intent.ACTION_VIEW).setComponent(
                        new ComponentName("other.streaming.app", "other.streaming.app.Play")));
                Intent opened = required(original.toUri(0));
                require(opened.getSelector() == null, "selector retained");
                require(OWNER.equals(opened.getPackage()), "package scope lost");
            });
            check("URI permissions and task result capabilities are removed", () -> {
                Intent original = new Intent(Intent.ACTION_VIEW, Uri.parse("streaming://episode-2"))
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                                | Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Intent opened = required(original.toUri(0));
                require(opened.getFlags() == (Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP), "unsafe flags retained");
                require(opened.getClipData() == null, "ClipData retained");
            });
            check("missing or malformed links cannot be launched", () -> {
                for (String uri : new String[]{null, "", " ", "not a link", "intent:#Intent;S.bad=broken"})
                    require(LauncherinPlayback.contentIntent(OWNER, uri) == null, "invalid link accepted: " + uri);
                require(LauncherinPlayback.contentIntent("", "streaming://episode-2") == null, "empty package accepted");
            });
            check("oversized stored links are rejected", () -> {
                char[] chars = new char[65537]; java.util.Arrays.fill(chars, 'a');
                require(LauncherinPlayback.contentIntent(OWNER, "streaming://" + new String(chars)) == null, "oversized link accepted");
            });
            check("document and script schemes are not playback links", () -> {
                for (String uri : new String[]{"file:///private/file", "javascript:alert(1)", "data:text/html,example"})
                    require(LauncherinPlayback.contentIntent(OWNER, uri) == null, "non-playback link accepted");
            });
            check("click dispatches the full content intent before an app-home fallback", () -> {
                RecordingContext context = new RecordingContext(getTargetContext(), false);
                Intent original = new Intent(Intent.ACTION_VIEW)
                        .setComponent(new ComponentName(OWNER, OWNER + ".PlaybackActivity"))
                        .putExtra("episode_id", "episode-2").putExtra("position_ms", 123456L);
                JSONObject item = new JSONObject().put("pkg", OWNER).put("title", "Fixture episode")
                        .put("intent_uri", original.toUri(0));
                require(LauncherinPlayback.open(context, item) == LauncherinPlayback.Route.CONTENT, "content route not used");
                require(context.starts == 1, "launched more than once");
                require("episode-2".equals(context.opened.getStringExtra("episode_id")), "dispatch lost episode");
                require(context.opened.getLongExtra("position_ms", -1) == 123456L, "dispatch lost position");
            });
            check("a missing app cannot produce a successful playback result", () -> {
                RecordingContext context = new RecordingContext(getTargetContext(), true);
                JSONObject item = new JSONObject().put("pkg", "com.netflix.ninja.missing.fixture")
                        .put("intent_uri", "streaming://episode-2");
                require(LauncherinPlayback.open(context, item) == LauncherinPlayback.Route.UNAVAILABLE, "invented success");
                require(context.starts == 1, "incorrect number of dispatch attempts");
            });
            if(options!=null&&"true".equals(options.getString("dashboard"))){
                DashboardChecks checks=new DashboardChecks(this);
                check("Transparent banner margins are trimmed without recolouring", checks::banner);
                check("Spotify session supplies title, artist, progress and 64px cover", checks::spotify);
                check("Music transport controls update the selected session", checks::transport);
                check("YouTube Music DISPLAY metadata wins over a paused Spotify session", checks::youtubeMusic);
                check("Globoplay's channel handler is not used as the native tuner", checks::tuner);
                check("Music details are visible in the actual dashboard", checks::showDashboard);
                check("Initial and recycled app tiles have uniform dimensions", checks::dock);
                check("Continue Watching focus grows another seven percent with a visible outline", checks::historyFocus);
                check("Live TV uses the exact typed Inputs-menu source request", checks::typedTuner);
                check("Widget Activity D-pad Play works with PLAY_PAUSE-only sessions", checks::widgetToggleOnly);
                check("Ignored transport Play recovers with an explicit media key", checks::widgetIgnoredTransport);
                check("Widget restores stopped queues through their published media ID", checks::widgetMediaId);
                check("Widget resumes an ended session through MediaBrowser without opening an app", checks::widgetBrowserResume);
                check("Widget resumes through an explicit background receiver without opening an app", checks::widgetReceiverResume);
                if(android.os.Build.VERSION.SDK_INT>=31)check("Widget resumes an ended session through a private creator-owned notification action", checks::widgetNotificationResume);
                check("History is capped at ten without YouTube and manual removal survives refresh", checks::historyManagement);
                checks.screenshot();
            }
            if(options!=null&&"true".equals(options.getString("runtime"))){
                RuntimeChecks runtime=new RuntimeChecks(this);
                check("Native inactivity timers apply and conditionally restore original settings",runtime::timers);
                check("TV-key learning persists a framework key without enabling global filtering",runtime::keyLearning);
                check("Goal card appears top-right without focus or touch interception",runtime::overlay);
                check("Home idle callback actually starts the selected OS DreamService",runtime::selectedDream);
                check("Standby keeps its original deadline after the selected dream starts",runtime::standbyDuringDream);
            }
            if(options!=null&&"true".equals(options.getString("standby"))){
                RuntimeChecks runtime=new RuntimeChecks(this);
                check("Standby keeps its original deadline after the selected dream starts",runtime::standbyDuringDream);
            }
            result.putString("stream", "\n" + report + "Passed " + passed + " Android playback checks.\n");
            result.putInt("passed", passed);
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("stream", "\n" + report + android.util.Log.getStackTraceString(failure));
            result.putInt("passed", passed);
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void check(String name, Check check) throws Exception {
        check.run(); passed++; report.append("PASS ").append(name).append('\n');
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static Intent required(String uri) {
        Intent intent = LauncherinPlayback.contentIntent(OWNER, uri);
        require(intent != null, "valid publisher intent rejected: " + uri);
        require(OWNER.equals(intent.getPackage()), "not scoped to publisher");
        return intent;
    }
    private static final class RecordingContext extends ContextWrapper {
        private final boolean unavailable;
        int starts;
        Intent opened;
        RecordingContext(Context base, boolean unavailable) { super(base); this.unavailable = unavailable; }
        @Override public void startActivity(Intent intent) {
            starts++; opened = intent;
            if (unavailable) throw new ActivityNotFoundException("Fixture: missing activity");
        }
        @Override public Object getSystemService(String name) {
            if (Context.MEDIA_SESSION_SERVICE.equals(name)) return null;
            return super.getSystemService(name);
        }
    }
}
