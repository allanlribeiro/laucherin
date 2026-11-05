package app.launcherin.tv;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lives with the already-authorized media listener; no foreground polling while screen is off. */
final class LauncherinGoalMonitor {
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ArrayDeque<Notice> queue = new ArrayDeque<>();
    private final Runnable poll = this::poll;
    private final Runnable hide = this::hide;
    private volatile boolean stopped;
    private final java.util.Set<String> initialized = new java.util.HashSet<>();
    private View showing;
    private String watching="";
    private static final class Notice {
        final String team, title, detail;
        Notice(String team, String title, String detail) { this.team=team; this.title=title; this.detail=detail; }
    }
    LauncherinGoalMonitor(Context context) { this.context = context; }
    void start() { main.post(poll); }
    void stop() { stopped=true;main.removeCallbacksAndMessages(null);io.shutdownNow();queue.clear();remove(); }
    private boolean enabled() { return LauncherinRuntimePrefs.read(context,LauncherinGoals.KEY) == 1; }
    private boolean awake() {
        PowerManager power=context.getSystemService(PowerManager.class);
        return power != null && power.isInteractive();
    }
    private void poll() {
        if(stopped)return;
        if(!enabled() || !awake() || !Settings.canDrawOverlays(context)) {
            synchronized(initialized){initialized.clear();}queue.clear();remove(); main.postDelayed(poll, 60000); return;
        }
        List<LauncherinSports.Team> teams = LauncherinSports.selected(context);
        String selection=signature(teams);
        if(!selection.equals(watching)){synchronized(initialized){initialized.clear();}watching=selection;}
        if(teams.isEmpty()) { main.postDelayed(poll,60000);return; }
        io.execute(() -> {
            long delay = 15 * 60000L;
            List<Notice> found = new ArrayList<>();
            android.content.SharedPreferences prefs = context.getSharedPreferences("launcherin-goals",0);
            try {
                JSONObject state = new JSONObject(prefs.getString("baselines","{}"));
                for(LauncherinSports.Team team : teams) {
                    if(stopped) return;
                    try {
                        long now=System.currentTimeMillis();
                        boolean primed; synchronized(initialized){primed=initialized.contains(team.id);}
                        LauncherinSports.Snapshot data=new LauncherinSports.Snapshot(LauncherinSports.merge(team.id,
                                LauncherinSports.get(team.id,true),LauncherinSports.get(team.id,false),now));
                        List<LauncherinSports.Match> games = new ArrayList<>(data.upcoming);
                        if(data.previous!=null)games.add(data.previous);
                        for(LauncherinSports.Match game : games) {
                            if("in".equals(game.state)) delay=30000;
                            else if("pre".equals(game.state) && game.time-now<30*60000L) delay=Math.min(delay,60000);
                            String value = team.id.equals(game.homeId) ? game.homeScore : team.id.equals(game.awayId) ? game.awayScore : "";
                            if(LauncherinGoals.observe(state,game.id+":"+team.id,LauncherinGoals.score(value),game.state,now) && primed)
                                found.add(new Notice(team.id,"Gol do "+team.name+"!",game.score()));
                        }
                        synchronized(initialized){initialized.add(team.id);}
                    } catch(Exception unavailable) { delay=Math.min(delay,60000); }
                }
                prefs.edit().putString("baselines",state.toString()).commit();
            } catch(Exception ignored) { delay=60000; }
            final long next=delay;
            main.post(() -> {
                if(stopped)return;
                // Never queue scores while the user has disabled alerts or switched clubs.
                if(enabled() && awake() && Settings.canDrawOverlays(context)) {
                    for(Notice n:found)announce(n.team,n.title,n.detail);
                }
                // Check disabled/config/screen state at least once a minute, even outside match time.
                schedule(next);
            });
        });
    }
    void announce(String team, String title, String detail) {
        if(stopped || !enabled() || !awake() || !Settings.canDrawOverlays(context) || !stillSelected(team))return;
        if(queue.size()<4)queue.add(new Notice(team,title,detail));showNext();
    }
    private long fetchAfter;
    private void schedule(long delay) {
        fetchAfter=android.os.SystemClock.elapsedRealtime()+delay;
        main.postDelayed(new Runnable() { public void run() {
            if(stopped)return;
            if(!enabled() || !awake() || !Settings.canDrawOverlays(context)) { fetchAfter=0;poll(); }
            else if(!signature(LauncherinSports.selected(context)).equals(watching)) poll();
            else if(android.os.SystemClock.elapsedRealtime()>=fetchAfter) poll();
            else main.postDelayed(this,Math.min(60000,fetchAfter-android.os.SystemClock.elapsedRealtime()));
        } },Math.min(delay,60000));
    }
    private static String signature(List<LauncherinSports.Team> teams) {
        StringBuilder ids=new StringBuilder();for(LauncherinSports.Team team:teams)ids.append(team.id).append(';');return ids.toString();
    }
    private boolean stillSelected(String id) { for(LauncherinSports.Team t:LauncherinSports.selected(context))if(t.id.equals(id))return true;return false; }
    private int dp(int value) { return Math.round(value*context.getResources().getDisplayMetrics().density); }
    private void showNext() {
        if(stopped||showing!=null||queue.isEmpty()||!awake()||!enabled())return;
        Notice notice=queue.removeFirst();
        LinearLayout card=new LinearLayout(context);card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(18),dp(14),dp(18),dp(14));
        card.setBackground(LauncherinWidgetAppearance.glass(context,18,true));
        android.graphics.Bitmap crest=LauncherinTeamCrests.cached(context,notice.team);
        if(crest!=null) { ImageView image=new ImageView(context);image.setImageBitmap(crest);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            card.addView(image,new LinearLayout.LayoutParams(dp(42),dp(42))); }
        LinearLayout texts=new LinearLayout(context);texts.setOrientation(LinearLayout.VERTICAL);texts.setPadding(dp(12),0,0,0);
        TextView title=new TextView(context);title.setText(notice.title);title.setTextColor(0xffffffff);title.setTextSize(19);
        TextView detail=new TextView(context);detail.setText(notice.detail);detail.setTextColor(0xffeeeeee);detail.setTextSize(14);
        detail.setMaxLines(2);texts.addView(title);texts.addView(detail);card.addView(texts,new LinearLayout.LayoutParams(0,-2,1));
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(dp(370),-2,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.END;params.x=dp(24);params.y=dp(24);params.setTitle("Launcherin · Gol");
        try { context.getSystemService(WindowManager.class).addView(card,params);showing=card;main.postDelayed(hide,7000); }
        catch(RuntimeException denied) { queue.clear(); }
    }
    private void hide() { remove();if(!enabled()||!awake())queue.clear();else showNext(); }
    private void remove() { main.removeCallbacks(hide);if(showing!=null)try{context.getSystemService(WindowManager.class).removeView(showing);}catch(RuntimeException ignored){}finally{showing=null;} }
}
