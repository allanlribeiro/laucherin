package app.launcherin.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

final class LauncherinTeamsPicker {
    static void choose(Activity host,Runnable saved) {
        List<LauncherinSports.Team> all=LauncherinSports.catalogue(host),picked=new ArrayList<>(LauncherinSports.selected(host)),shown=new ArrayList<>();
        int dp=Math.round(host.getResources().getDisplayMetrics().density);
        LinearLayout content=new LinearLayout(host);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(22*dp,8*dp,22*dp,4*dp);
        EditText search=new EditText(host);search.setSingleLine(true);search.setHint("Buscar time");content.addView(search);
        TextView count=new TextView(host);count.setTextColor(0xffdddded);count.setPadding(0,8*dp,0,8*dp);content.addView(count);
        ListView list=new ListView(host);list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);content.addView(list,new LinearLayout.LayoutParams(-1,220*dp));
        Runnable summary=()->{StringBuilder text=new StringBuilder(picked.size()+"/2 · ");for(LauncherinSports.Team t:picked){if(text.charAt(text.length()-1)!=' ')text.append(" e ");text.append(t.name);}if(picked.isEmpty())text.append("Nenhum time selecionado");count.setText(text);};
        Runnable filter=()->{
            String query=LauncherinWeather.canonical(search.getText().toString());shown.clear();List<String> names=new ArrayList<>();
            for(LauncherinSports.Team t:all)if(LauncherinWeather.canonical(t.name+" "+t.league).contains(query)){shown.add(t);names.add(t.name+" · "+t.league);}
            list.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_list_item_multiple_choice,names));
            for(int i=0;i<shown.size();i++){final String id=shown.get(i).id;boolean chosen=false;for(LauncherinSports.Team t:picked)if(t.id.equals(id))chosen=true;list.setItemChecked(i,chosen);}summary.run();
        };
        list.setOnItemClickListener((parent,view,index,id)->{
            LauncherinSports.Team team=shown.get(index);boolean removed=false;for(int i=0;i<picked.size();i++)if(picked.get(i).id.equals(team.id)){picked.remove(i);removed=true;break;}
            if(!removed){if(picked.size()>=2){list.setItemChecked(index,false);Toast.makeText(host,"Escolha no máximo dois times. Desmarque um para trocar.",Toast.LENGTH_SHORT).show();}else picked.add(team);}summary.run();
        });
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){filter.run();}public void afterTextChanged(Editable s){}});
        AlertDialog dialog=new LauncherinDialogs.Builder(host).setTitle("Até 2 times favoritos").setView(content).setPositiveButton("Salvar",null).setNegativeButton("Cancelar",null).create();
        dialog.setOnShowListener(d->{filter.run();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{LauncherinSports.select(host,picked);dialog.dismiss();saved.run();}catch(Exception error){Toast.makeText(host,"Não foi possível salvar os times.",Toast.LENGTH_LONG).show();}});list.requestFocus();});dialog.show();
    }
}
