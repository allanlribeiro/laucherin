package app.launcherin.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

/** Decorative season is independent of wallpaper, glass opacity and the original layout styles. */
final class LauncherinSeason {
    static final String KEY="seasonal_theme";
    static final String NONE="none",CHRISTMAS="christmas";
    static String clean(String value){return CHRISTMAS.equals(value)?CHRISTMAS:NONE;}
    static String selected(Context context){return clean(LauncherinExtras.read(context).optString(KEY,NONE));}
    static void choose(Activity activity,Runnable changed){
        String original=selected(activity);final int[] choice={CHRISTMAS.equals(original)?1:0};
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Tema decorativo")
                .setSingleChoiceItems(new String[]{"Padrão · sem enfeites","Natal · gorrinhos, luzes e presentes"},choice[0],(d,index)->choice[0]=index)
                .setPositiveButton("Aplicar",null).setNegativeButton("Cancelar",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{LauncherinExtras.setString(activity,KEY,choice[0]==1?CHRISTMAS:NONE);dialog.dismiss();changed.run();}
            catch(Exception error){Toast.makeText(activity,"Não foi possível salvar o tema.",Toast.LENGTH_LONG).show();}
        }));dialog.show();
    }
}
