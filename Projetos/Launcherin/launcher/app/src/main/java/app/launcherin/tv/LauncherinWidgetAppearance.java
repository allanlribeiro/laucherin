package app.launcherin.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.function.IntConsumer;

/** Widget tint only: labels, media controls and focus remain readable at every transparency. */
final class LauncherinWidgetAppearance {
    static final String KEY = "widget_transparency";
    static final int DEFAULT = 82;
    static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
    static int transparency(android.content.Context context) {
        return clamp(LauncherinExtras.read(context).optInt(KEY, DEFAULT));
    }
    static GradientDrawable glass(android.content.Context context, int transparency, boolean focused) {
        float density = context.getResources().getDisplayMetrics().density;
        int alpha = Math.round(255 * (100 - clamp(transparency)) / 100f);
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{(alpha << 24) | 0x30323b, (alpha << 24) | 0x1a1b23});
        background.setCornerRadius(18 * density);
        background.setStroke(Math.max(1, Math.round(density)), focused ? 0xccffffff : 0x30ffffff);
        return background;
    }
    static void choose(Activity activity, IntConsumer preview) {
        int original = transparency(activity);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * activity.getResources().getDisplayMetrics().density);
        panel.setPadding(padding, padding / 2, padding, padding / 2);
        TextView value = new TextView(activity);
        value.setTextColor(0xffeeeeef);
        value.setTextSize(16);
        panel.addView(value);
        SeekBar slider = new SeekBar(activity);
        slider.setMax(100);
        slider.setKeyProgressIncrement(5);
        slider.setContentDescription("Transparência dos widgets");
        slider.setProgress(original);
        value.setText(original + "% transparente · 0% opaco / 100% transparente");
        panel.addView(slider, new LinearLayout.LayoutParams(-1, -2));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText(progress + "% transparente · 0% opaco / 100% transparente");
                preview.accept(progress);
            }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) { }
        });
        boolean[] saved = {false};
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Transparência dos widgets")
                .setView(panel).setPositiveButton("Salvar", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnDismissListener(d -> { if (!saved[0]) preview.accept(original); });
        dialog.setOnShowListener(d -> {
            slider.requestFocus();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    LauncherinExtras.setInt(activity, KEY, slider.getProgress());
                    saved[0] = true;
                    preview.accept(slider.getProgress());
                    dialog.dismiss();
                } catch (Exception error) {
                    Toast.makeText(activity, "Não foi possível salvar. Tente novamente.", Toast.LENGTH_LONG).show();
                }
            });
        });
        dialog.show();
    }
}
