package app.launcherin.tv;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

final class LauncherinWeatherCard {
    final LauncherinCardPopup popup;private final LauncherinWeatherScene scene;
    private final TextView city,current,condition,source;private final LinearLayout days;
    LauncherinWeatherCard(Activity host,View anchor,Runnable chooseCity) {
        popup=new LauncherinCardPopup(host,anchor,640,310);scene=new LauncherinWeatherScene(host,popup.motion);popup.card.addView(scene,1,new android.widget.FrameLayout.LayoutParams(-1,-1));
        city=popup.label("Previsão do tempo",17,Color.WHITE);city.setSingleLine(true);city.setEllipsize(android.text.TextUtils.TruncateAt.END);popup.content.addView(city);
        current=popup.label("Consultando…",31,Color.WHITE);popup.content.addView(current,new LinearLayout.LayoutParams(-1,popup.dp(43)));
        condition=popup.label("",13,0xffdfe5ef);popup.content.addView(condition,new LinearLayout.LayoutParams(-1,popup.dp(23)));
        popup.content.addView(popup.label("PRÓXIMOS 5 DIAS",10,0xffc9d4e4));
        days=new LinearLayout(host);days.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams row=new LinearLayout.LayoutParams(-1,0,1);row.topMargin=popup.dp(8);row.bottomMargin=popup.dp(10);popup.content.addView(days,row);
        LinearLayout footer=new LinearLayout(host);footer.setGravity(Gravity.CENTER_VERTICAL);source=popup.label("Open-Meteo",10,0xffc9d4e4);footer.addView(source,new LinearLayout.LayoutParams(0,-2,1));
        TextView cityButton=popup.button("Cidade",()->{popup.dismiss();chooseCity.run();});footer.addView(cityButton);
        TextView close=popup.button("Fechar",popup::dismiss);LinearLayout.LayoutParams closeParams=new LinearLayout.LayoutParams(-2,-2);closeParams.leftMargin=popup.dp(10);footer.addView(close,closeParams);popup.content.addView(footer);
        popup.show();close.requestFocus();
    }
    void render(LauncherinWeather.Snapshot weather,long now) {
        if(!popup.showing())return;
        city.setText(weather.city);current.setText(weather.icon(now)+"  "+weather.temperature+"°");current.setTextColor(LauncherinWidgetFaces.Weather.accent(weather.code,weather.night(now)));
        condition.setText(weather.conditions+" · "+(weather.night(now)?"Noite":"Dia")+(weather.old()?" · Dados anteriores":""));
        scene.weather(weather.code,weather.night(now),LauncherinWidgetAppearance.transparency(popup.host));
        source.setText("Open-Meteo · "+new java.text.SimpleDateFormat("HH:mm",Locale.forLanguageTag("pt-BR")).format(new java.util.Date(weather.updated)));
        days.removeAllViews();List<LauncherinWeather.Day> forecast=weather.nextDays(now);
        if(forecast.size()<5){TextView empty=popup.label("Previsão de 5 dias indisponível. Tente novamente com conexão.",13,Color.WHITE);days.addView(empty);return;}
        DateTimeFormatter label=DateTimeFormatter.ofPattern("EEE · dd",Locale.forLanguageTag("pt-BR"));
        for(LauncherinWeather.Day day:forecast){
            LinearLayout column=new LinearLayout(popup.host);column.setOrientation(LinearLayout.VERTICAL);column.setGravity(Gravity.CENTER);
            TextView date=popup.label(label.format(day.date),12,0xffe3e8f0),icon=popup.label(LauncherinWeather.symbol(day.code),24,Color.WHITE),range=popup.label(day.high+"° / "+day.low+"°",14,Color.WHITE),rain=popup.label(day.rain<0?"Chuva —":"Chuva "+day.rain+"%",10,0xffd5e1ec);
            icon.setTextColor(LauncherinWidgetFaces.Weather.accent(day.code,false));
            for(TextView text:new TextView[]{date,icon,range,rain}){text.setGravity(Gravity.CENTER);column.addView(text,new LinearLayout.LayoutParams(-1,-2));}
            column.setContentDescription(date.getText()+" · "+LauncherinWeather.description(day.code)+" · máxima "+day.high+" graus, mínima "+day.low+" graus"+(day.rain<0?"":" · chance de chuva "+day.rain+" por cento"));
            days.addView(column,new LinearLayout.LayoutParams(0,-2,1));
        }
    }
    void unavailable(){if(popup.showing()){current.setText("Sem previsão agora");condition.setText("Verifique a conexão ou escolha outra cidade.");}}
}
