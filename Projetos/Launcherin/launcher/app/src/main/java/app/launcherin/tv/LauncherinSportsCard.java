package app.launcherin.tv;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executor;
import java.util.concurrent.Callable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class LauncherinSportsCard {
    final LauncherinCardPopup popup;private final LinearLayout columns;private final TextView source;
    private final List<LauncherinSports.Team> teams;private String signature="";
    private final FrameLayout body;private final LinearLayout table;private final TextView gamesTab,tableTab,close;
    private final Executor io;private final Callable<LauncherinStandings.Snapshot> standingsLoader;
    private boolean tableSelected,standingsStarted;private LauncherinStandings.Snapshot standings;
    LauncherinSportsCard(Activity host,View anchor,List<LauncherinSports.Team> teams,Runnable chooseTeams) {
        this(host,anchor,teams,chooseTeams,command->{Thread t=new Thread(command,"launcherin-standings");t.setDaemon(true);t.start();});
    }
    LauncherinSportsCard(Activity host,View anchor,List<LauncherinSports.Team> teams,Runnable chooseTeams,Executor io){this(host,anchor,teams,chooseTeams,io,()->LauncherinStandings.load(host));}
    LauncherinSportsCard(Activity host,View anchor,List<LauncherinSports.Team> teams,Runnable chooseTeams,Executor io,Callable<LauncherinStandings.Snapshot> loader) {
        this.teams=teams;this.io=io;this.standingsLoader=loader;popup=new LauncherinCardPopup(host,anchor,teams.size()>1?740:580,310);
        popup.content.addView(popup.label("Times favoritos",21,Color.WHITE));
        LinearLayout tabs=new LinearLayout(host);gamesTab=popup.button("Próximos jogos",()->selectTable(false));tableTab=popup.button("Brasileirão Série A",()->selectTable(true));gamesTab.setId(View.generateViewId());tableTab.setId(View.generateViewId());tabs.addView(gamesTab);LinearLayout.LayoutParams tabGap=new LinearLayout.LayoutParams(-2,-2);tabGap.leftMargin=popup.dp(8);tabs.addView(tableTab,tabGap);popup.content.addView(tabs);
        body=new FrameLayout(host);columns=new LinearLayout(host);body.addView(columns,new FrameLayout.LayoutParams(-1,-1));table=new LinearLayout(host);table.setOrientation(LinearLayout.VERTICAL);table.setVisibility(View.GONE);body.addView(table,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout.LayoutParams area=new LinearLayout.LayoutParams(-1,0,1);area.topMargin=popup.dp(8);area.bottomMargin=popup.dp(8);popup.content.addView(body,area);
        LinearLayout footer=new LinearLayout(host);footer.setGravity(Gravity.CENTER_VERTICAL);source=popup.label("Dados: ESPN · Horário local",10,0xffc9cbd3);source.setSingleLine(true);source.setEllipsize(android.text.TextUtils.TruncateAt.END);footer.addView(source,new LinearLayout.LayoutParams(0,-2,1));
        footer.addView(popup.button("Escolher times",()->{popup.dismiss();chooseTeams.run();}));close=popup.button("Fechar",popup::dismiss);close.setId(View.generateViewId());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);cp.leftMargin=popup.dp(9);footer.addView(close,cp);popup.content.addView(footer);
        gamesTab.setNextFocusRightId(tableTab.getId());tableTab.setNextFocusLeftId(gamesTab.getId());
        popup.show();selectTable(false);close.requestFocus();
    }
    void render(Map<String,LauncherinSports.Snapshot> data,boolean loading) {
        if(!popup.showing())return;StringBuilder state=new StringBuilder(Boolean.toString(loading));for(LauncherinSports.Team team:teams){LauncherinSports.Snapshot value=data.get(team.id);state.append(team.id).append(':').append(value==null?"missing":value.updated+":"+value.old()).append(':').append(LauncherinTeamCrests.stamp(popup.host,team.id));}
        if(signature.equals(state.toString()))return;signature=state.toString();columns.removeAllViews();boolean old=false;
        for(LauncherinSports.Team team:teams){
            ScrollView scroll=new ScrollView(popup.host);scroll.setFillViewport(false);LinearLayout column=new LinearLayout(popup.host);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(popup.dp(12),popup.dp(9),popup.dp(12),popup.dp(9));column.setTag("expanded-surface");column.setBackground(LauncherinWidgetAppearance.glass(popup.host,LauncherinWidgetAppearance.transparency(popup.host),false));scroll.addView(column);
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
        if(!tableSelected)source.setText("ESPN · Horário local"+(old?" · Dados anteriores":""));
    }
    void selectTable(boolean selected){
        tableSelected=selected;columns.setVisibility(selected?View.GONE:View.VISIBLE);table.setVisibility(selected?View.VISIBLE:View.GONE);
        gamesTab.setSelected(!selected);tableTab.setSelected(selected);gamesTab.setTextColor(!selected?LauncherinAppearance.get(popup.host).accent:Color.WHITE);tableTab.setTextColor(selected?LauncherinAppearance.get(popup.host).accent:Color.WHITE);
        if(!selected){close.setNextFocusUpId(View.NO_ID);source.setText("ESPN · Horário local");return;}
        if(!standingsStarted){standingsStarted=true;standings=LauncherinStandings.cached(popup.host);renderStandings(standings,true);io.execute(()->{LauncherinStandings.Snapshot result=null;try{result=standingsLoader.call();}catch(Exception ignored){}final LauncherinStandings.Snapshot value=result;new Handler(Looper.getMainLooper()).post(()->{if(!popup.showing())return;standings=value;renderStandings(value,false);});});}
        else if(standings!=null)renderStandings(standings,false);
    }
    private void renderStandings(LauncherinStandings.Snapshot snapshot,boolean loading){
        View focused=popup.host.getCurrentFocus();if(popup.window.getContentView().findFocus()!=null)focused=popup.window.getContentView().findFocus();String keep=focused!=null&&focused.getTag() instanceof String?(String)focused.getTag():"";
        int oldScroll=table.getChildCount()>1&&table.getChildAt(1) instanceof ScrollView?((ScrollView)table.getChildAt(1)).getScrollY():0;table.removeAllViews();
        if(snapshot==null){TextView message=popup.label(loading?"Consultando a classificação…":"Classificação indisponível. Verifique a conexão e abra novamente mais tarde.",14,0xffd7d9e2);table.addView(message);if(tableSelected)source.setText("Brasileirão Série A · ESPN");tableTab.setNextFocusDownId(close.getId());return;}
        table.addView(standingRow(null,false),new LinearLayout.LayoutParams(-1,popup.dp(25)));ScrollView scroll=new ScrollView(popup.host);scroll.setFillViewport(false);LinearLayout list=new LinearLayout(popup.host);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);table.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(LauncherinStandings.Row r:snapshot.rows){boolean favourite=false;for(LauncherinSports.Team t:teams)if(t.id.equals(r.id))favourite=true;LinearLayout row=standingRow(r,favourite);row.setId(View.generateViewId());row.setTag("standings-"+r.id);row.setFocusable(true);row.setContentDescription(r.rank+"º, "+r.name+(favourite?", time favorito":"")+", "+r.points+" pontos, "+r.played+" jogos, "+r.wins+" vitórias, "+r.draws+" empates, "+r.losses+" derrotas, saldo "+r.goalDifference);
            row.setBackground(LauncherinSurface.card(popup.host,90,false));row.setOnFocusChangeListener((v,has)->v.setBackground(LauncherinSurface.card(popup.host,has?55:90,has)));list.addView(row,new LinearLayout.LayoutParams(-1,popup.dp(29)));
        }
        for(int i=0;i<list.getChildCount();i++){View row=list.getChildAt(i);row.setNextFocusUpId(i==0?tableTab.getId():list.getChildAt(i-1).getId());row.setNextFocusDownId(i+1==list.getChildCount()?close.getId():list.getChildAt(i+1).getId());if(keep.equals(row.getTag()))row.requestFocus();}
        tableTab.setNextFocusDownId(list.getChildAt(0).getId());close.setNextFocusUpId(list.getChildAt(list.getChildCount()-1).getId());scroll.scrollTo(0,oldScroll);scroll.post(()->scroll.scrollTo(0,oldScroll));
        if(tableSelected)source.setText("Brasileirão Série A "+snapshot.season+" · ESPN · "+new SimpleDateFormat("dd/MM HH:mm",Locale.forLanguageTag("pt-BR")).format(new Date(snapshot.updated))+(snapshot.old(System.currentTimeMillis())?" · Dados anteriores":""));
    }
    private LinearLayout standingRow(LauncherinStandings.Row r,boolean favourite){
        LinearLayout row=new LinearLayout(popup.host);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(popup.dp(5),popup.dp(2),popup.dp(5),popup.dp(2));
        String[] labels=r==null?new String[]{"#","Time","P","J","V","E","D","SG"}:new String[]{String.valueOf(r.rank),(favourite?"★ ":"")+r.name,String.valueOf(r.points),String.valueOf(r.played),String.valueOf(r.wins),String.valueOf(r.draws),String.valueOf(r.losses),String.valueOf(r.goalDifference)};
        for(int i=0;i<labels.length;i++){TextView text=popup.label(labels[i],12,i==2||favourite&&i==1?LauncherinAppearance.get(popup.host).accent:r==null?0xffc9cbd3:Color.WHITE);text.setSingleLine(true);text.setEllipsize(android.text.TextUtils.TruncateAt.END);text.setGravity(i==1?Gravity.CENTER_VERTICAL:Gravity.CENTER);text.setTypeface(popup.host.getResources().getFont(i==2||r==null?R.font.outfit_medium:R.font.outfit_regular));row.addView(text,new LinearLayout.LayoutParams(0,-1,i==1?4f:i==0?.7f:1f));}
        return row;
    }
    private void section(LinearLayout column,String text){TextView v=popup.label(text,10,0xffbfc8d8);v.setPadding(0,popup.dp(11),0,popup.dp(3));column.addView(v);}
    static String date(LauncherinSports.Match game){return game.state.equals("in")?"AO VIVO · "+new SimpleDateFormat("dd/MM",Locale.forLanguageTag("pt-BR")).format(new Date(game.time)):new SimpleDateFormat(game.state.equals("post")?"dd/MM '· Encerrado'":"EEE, dd/MM · HH:mm",Locale.forLanguageTag("pt-BR")).format(new Date(game.time));}
}
