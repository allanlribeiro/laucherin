package app.launcherin.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

/** Capture only while this screen is visible; no global filter is enabled by learning. */
public final class LauncherinRemoteActivity extends Activity {
    private boolean awaiting = true;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView help = new TextView(this);
        help.setText("Configurar tecla de TV aberta\n\nAperte a tecla com o desenho de TV abaixo do Home.\n\nVoltar cancela. Não aperte a tecla TCL Channels.\n\nSe nada aparecer, o firmware está interceptando a tecla antes da launcher. Nesse caso, será necessário medir o código por ADB.");
        help.setTextColor(0xffeeeeee); help.setTextSize(22);
        int pad = (int)(40 * getResources().getDisplayMetrics().density);
        help.setPadding(pad, pad, pad, pad);setContentView(help);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event);
        if (!awaiting || !LauncherinRemote.learnable(key)) return super.dispatchKeyEvent(event);
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            awaiting = false;
            new AlertDialog.Builder(this).setTitle("Usar esta tecla para TV aberta?")
                    .setMessage(KeyEvent.keyCodeToString(key) + " · código " + key + "\nEla abrirá a mesma TV ao vivo do ícone que já funciona.")
                    .setPositiveButton("Salvar", (d,w) -> {
                        try { LauncherinExtras.setInt(this, LauncherinRemote.KEY, key); LauncherinRemote.refresh(this); HomeKeyService.applyTvRemote(); finish(); }
                        catch (Exception error) { Toast.makeText(this,"Não foi possível salvar a tecla.",Toast.LENGTH_LONG).show(); awaiting=true; }
                    }).setNegativeButton("Tentar outra", (d,w) -> awaiting=true)
                    .setOnCancelListener(d -> awaiting=true).show();
        }
        return true;
    }
}
