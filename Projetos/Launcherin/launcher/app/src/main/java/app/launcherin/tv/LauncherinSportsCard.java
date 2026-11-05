package app.launcherin.tv;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class LauncherinSportsCard {
    final LauncherinCardPopup popup;private final LinearLayout columns;private final TextView source;
    private final List<LauncherinSports.Team> teams;private String signature="";
    LauncherinSportsCard(Activity host,View anchor,List<LauncherinSports.Team> teams,Runnable chooseTeams) {
        this.teams=teams;popup=new LauncherinCardPopup(host,anchor,teams.size()>1?740:580,310);
        popup.content.addView(popup.label("Seus times · próximos jogos",21,Color.WHITE));
        columns=new LinearLayout(host);LinearLayout.LayoutParams area=new LinearLayout.LayoutParams(-1,0,1);area.topMargin=popup.dp(12);area.bottomMargin=popup.dp(12);popup.content.addView(columns,area);
        LinearLayout footer=new LinearLayout(host);footer.setGravity(Gravity.CENTER_VERTICAL);source=popup.label("Dados: ESPN · Horário local",10,0xffc9cbd3);footer.addView(source,new LinearLayout.LayoutParams(0,-2,1));
        footer.addView(popup.button("Escolher times",()->{popup.dismiss();chooseTeams.run();}));TextView close=popup.button("Fechar",popup::dismiss);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);cp.leftMargin=popup.dp(9);footer.addView(close,cp);popup.content.addView(footer);
        popup.show();close.requestFocus();
    }
    void render(Map<String,LauncherinSports.Snapshot> data,boolean loading) {
        if(!popup.showing())return;StringBuilder state=new StringBuilder(Boolean.toString(loading));for(LauncherinSports.Team team:teams){LauncherinSports.Snapshot value=data.get(team.id);state.append(team.id).append(':').append(value==null?"missing":value.updated+":"+value.old()).append(':').append(LauncherinTeamCrests.stamp(popup.host,team.id));}
        if(signature.equals(state.toString()))return;signature=state.toString();columns.removeAllViews();boolean old=false;
        for(LauncherinSports.Team team:teams){
            ScrollView scroll=new ScrollView(popup.host);scroll.setFillViewport(false);LinearLayout column=new LinearLayout(popup.host);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(popup.dp(12),popup.dp(9),popup.dp(12),popup.dp(9));column.setBackground(LauncherinWidgetAppearance.glass(popup.host,LauncherinWidgetAppearance.transparency(popup.host),false));scroll.addView(column);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);if(columns.getChildCount()>0)cp.leftMargin=popup.dp(12);columns.addView(scroll,cp);
            LinearLayout heading=new LinearLayout(popup.host);heading.setGravity(Gravity.CENTER_VERTICAL);android.graphics.Bitmap crest=LauncherinTeamCrests.cached(popup.host,team.id);
            if(crest!=null){ImageView logo=new ImageView(popup.host);logo.setImageBitmap(crest);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(popup.dp(27),popup.dp(27));size.rightMargin=popup.dp(9);heading.addView(logo,size);}
            TextView title=popup.label(team.name,17,Color.WHITE);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));column.addView(heading);LauncherinSports.Snapshot value=data.get(team.id);
            if(value==null){TextView empty=popup.label(loading?"Consultando jogos…":"Agenda indisponível. Tente novamente com conexão.",13,0xffd7d9e2);empty.setPadding(0,popup.dp(18),0,0);column.addView(empty);continue;}
            old|=value.old();section(column,"ÚLTIMO RESULTADO");
            if(value.previous==null)column.addView(popup.label("Sem resultado disponível",12,0xffc9cbd3));
            else {column.addView(popup.label(value.previous.score(),14,Color.WHITE));column.addView(popup.label(date(value.previous),10,0xffbec7d5));}
            section(column,"PRÓXIMOS 2 JOGOS");
            if(value.upcoming.isEmpty())column.addView(popup.label("Nenhuma partida anunciada",12,0xffc9cbd3));
            for(LauncherinSports.Match game:value.upcoming){TextView name=popup.label(game.state.equals("in")&&!game.homeScore.isEmpty()&&!game.awayScore.isEmpty()?game.score():game.pairing(),14,Color.WHITE);name.setPadding(0,popup.dp(5),0,0);column.addView(name);column.addView(popup.label(date(game),11,0xffbec7d5));}
        }
        source.setText("ESPN · Horário local"+(old?" · Dados anteriores":""));
    }
    private void section(LinearLayout column,String text){TextView v=popup.label(text,10,0xffbfc8d8);v.setPadding(0,popup.dp(11),0,popup.dp(3));column.addView(v);}
    static String date(LauncherinSports.Match game){return game.state.equals("in")?"AO VIVO · "+new SimpleDateFormat("dd/MM",Locale.forLanguageTag("pt-BR")).format(new Date(game.time)):new SimpleDateFormat(game.state.equals("post")?"dd/MM '· Encerrado'":"EEE, dd/MM · HH:mm",Locale.forLanguageTag("pt-BR")).format(new Date(game.time));}
}
