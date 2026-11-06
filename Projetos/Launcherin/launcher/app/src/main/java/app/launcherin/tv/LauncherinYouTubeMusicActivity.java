package app.launcherin.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Optional, session-driven music screen, inspired by Streamer Launcher's Now Playing flow. */
public final class LauncherinYouTubeMusicActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService artWorker = Executors.newSingleThreadExecutor();
    private TextView title, artist, clock, help;
    private ImageView cover;
    private SeekBar seek;
    private Button toggle, open;
    private String artworkKey = "";
    private long duration;
    private boolean playing, seekDragging;
    private JSONObject track = new JSONObject();
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 1500L);
        }
    };

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable bg(boolean focused) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(focused ? 0xff31568c : 0xff1a2945);
        shape.setCornerRadius(dp(13));
        shape.setStroke(dp(focused ? 2 : 1), focused ? 0xffa8caff : 0x446c93c6);
        return shape;
    }

    private TextView text(int size, int color) {
        TextView view = new TextView(this);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        return view;
    }

    private Button button(String label) {
        Button view = new Button(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(15);
        view.setAllCaps(false);
        view.setMinHeight(0);
        view.setPadding(dp(13), dp(7), dp(13), dp(7));
        view.setBackground(bg(false));
        view.setOnFocusChangeListener((v, focused) -> view.setBackground(bg(focused)));
        return view;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xff080f22);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_VERTICAL);
        page.setPadding(dp(52), dp(30), dp(52), dp(30));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -1));
        setContentView(scroll);

        TextView eyebrow = text(15, 0xff8bbcf9);
        eyebrow.setText("LAUNCHERIN  ·  YOUTUBE MUSIC");
        page.addView(eyebrow);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.HORIZONTAL);
        info.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-1, -2);
        infoParams.topMargin = dp(23);
        page.addView(info, infoParams);

        cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setImageResource(android.R.drawable.ic_media_play);
        cover.setBackground(bg(false));
        info.addView(cover, new LinearLayout.LayoutParams(dp(145), dp(145)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(23), 0, 0, 0);
        info.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        title = text(29, Color.WHITE);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        details.addView(title);
        artist = text(17, 0xffb4c8ea);
        artist.setSingleLine(true);
        artist.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams artistParams = new LinearLayout.LayoutParams(-1, -2);
        artistParams.topMargin = dp(12);
        details.addView(artist, artistParams);

        seek = new SeekBar(this);
        seek.setMax(1000);
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(-1, dp(36));
        seekParams.topMargin = dp(17);
        page.addView(seek, seekParams);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (fromUser && duration > 0) clock.setText(
                        LauncherinYouTubeMusicPolicy.clock(Math.round(duration * value / 1000d))
                                + " / " + LauncherinYouTubeMusicPolicy.clock(duration));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { seekDragging = true; }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                seekDragging = false;
                if (duration > 0 && !LauncherinYouTubeMusicSession.seek(
                        LauncherinYouTubeMusicActivity.this, Math.round(duration * bar.getProgress() / 1000d)))
                    toast("Este player não permite avançar pela Launcherin.");
                refresh();
            }
        });

        clock = text(13, 0xffadc2e0);
        clock.setGravity(Gravity.END);
        page.addView(clock);

        LinearLayout commands = new LinearLayout(this);
        commands.setGravity(Gravity.CENTER);
        commands.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cmdParams = new LinearLayout.LayoutParams(-1, dp(54));
        cmdParams.topMargin = dp(18);
        page.addView(commands, cmdParams);
        Button previous = button("◀◀  Anterior");
        toggle = button("Reproduzir");
        Button next = button("Próxima  ▶▶");
        commands.addView(previous, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(0, -1, 1);
        toggleParams.setMargins(dp(10), 0, dp(10), 0);
        commands.addView(toggle, toggleParams);
        commands.addView(next, new LinearLayout.LayoutParams(0, -1, 1));

        previous.setOnClickListener(v -> {
            if (!LauncherinYouTubeMusicSession.skip(this, false)) toast("Faixa anterior indisponível.");
            refresh();
        });
        toggle.setOnClickListener(v -> {
            if (!LauncherinMediaService.granted(this)) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } else if (!LauncherinYouTubeMusicSession.toggle(this)) {
                toast("Inicie uma música no YouTube Music e tente novamente.");
            }
            refresh();
        });
        next.setOnClickListener(v -> {
            if (!LauncherinYouTubeMusicSession.skip(this, true)) toast("Próxima faixa indisponível.");
            refresh();
        });

        open = button("Abrir YouTube Music");
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(-1, dp(52));
        openParams.topMargin = dp(20);
        page.addView(open, openParams);
        open.setOnClickListener(v -> {
            if (!LauncherinMediaService.granted(this)) {
                try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
                catch (RuntimeException e) { toast("Ative o acesso às notificações nas configurações."); }
            } else if (!LauncherinYouTubeMusicSession.openOfficial(this))
                toast("Instale o YouTube Music ou YouTube na TV.");
        });

        help = text(13, 0xff93a9cb);
        help.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(-1, -2);
        helpParams.topMargin = dp(15);
        page.addView(help, helpParams);
        open.requestFocus();
        refresh();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void refresh() {
        track = LauncherinYouTubeMusicSession.snapshot(this);
        String song = track.optString("title");
        title.setText(song.isEmpty() ? "Nenhuma faixa em reprodução" : song);
        artist.setText(track.optString("artist", "YouTube Music"));
        duration = track.optLong("duration", 0);
        playing = track.optBoolean("playing");
        toggle.setText(playing ? "Pausar  ❚❚" : "Reproduzir  ▶");
        boolean granted = LauncherinMediaService.granted(this);
        open.setText(granted ? "Abrir YouTube Music" : "Autorizar acesso à mídia");
        help.setText(!granted
                ? "Autorize o acesso às notificações para controlar a sessão de música."
                : song.isEmpty()
                ? "Abra o YouTube Music, escolha uma faixa e volte à Launcherin."
                : "O áudio pertence ao aplicativo original. A Launcherin não solicita senha nem extrai o fluxo.");
        if (!seekDragging) {
            long position = LauncherinYouTubeMusicPolicy.position(
                    track.optLong("position"), track.optLong("capturedElapsed"),
                    SystemClock.elapsedRealtime(), playing, duration);
            seek.setProgress(LauncherinYouTubeMusicPolicy.progress(position, duration));
            clock.setText(LauncherinYouTubeMusicPolicy.clock(position) + " / "
                    + LauncherinYouTubeMusicPolicy.clock(duration));
        }
        seek.setEnabled(duration > 0 && granted);
        loadCover(track.optString("art"));
    }

    private void loadCover(String source) {
        if (source.equals(artworkKey)) return;
        artworkKey = source;
        cover.setImageResource(android.R.drawable.ic_media_play);
        if (source.isEmpty()) return;
        artWorker.execute(() -> {
            Bitmap bitmap = null;
            try {
                byte[] bytes;
                if (source.startsWith("https://")) {
                    HttpURLConnection connection = (HttpURLConnection) new URL(source).openConnection();
                    connection.setConnectTimeout(3500);
                    connection.setReadTimeout(3500);
                    try (InputStream in = connection.getInputStream()) {
                        bytes = LauncherinExtras.bytes(in, 1_500_000);
                    } finally { connection.disconnect(); }
                } else {
                    File file = new File(source).getCanonicalFile();
                    String owned = getFilesDir().getCanonicalPath() + File.separator;
                    if (!file.getPath().startsWith(owned)) return;
                    try (InputStream in = new FileInputStream(file)) {
                        bytes = LauncherinExtras.bytes(in, 1_500_000);
                    }
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                if (options.outWidth < 1 || options.outHeight < 1 ||
                        options.outWidth > 8192 || options.outHeight > 8192) return;
                options.inSampleSize = 1;
                while (options.outWidth / options.inSampleSize > 300 ||
                        options.outHeight / options.inSampleSize > 300)
                    options.inSampleSize *= 2;
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            } catch (Exception ignored) { /* Keep placeholder if artwork is unavailable. */ }
            final Bitmap result = bitmap;
            handler.post(() -> {
                if (!isFinishing() && !isDestroyed() && source.equals(artworkKey)) {
                    if (result != null) cover.setImageBitmap(result);
                } else if (result != null) result.recycle();
            });
        });
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        artWorker.shutdownNow();
        super.onDestroy();
    }
}
