package app.launcherin.tv;

import android.content.Context;
import android.graphics.*;
import android.text.TextUtils;
import android.view.View;
import java.util.Locale;

/** Small, event-driven drawings inside the existing glass cards. No frame loop or offscreen blur. */
final class LauncherinWidgetFaces {
    private LauncherinWidgetFaces(){}
    abstract static class Face extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Typeface regular=Typeface.create("sans-serif",Typeface.NORMAL);
        final Typeface medium=Typeface.create("sans-serif-medium",Typeface.NORMAL);
        final Typeface numbers;
        float width,height;
        Face(Context c){super(c);numbers=Typeface.create("sans-serif-light",Typeface.NORMAL);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float scale=Math.min(getWidth()/180f,getHeight()/77f);
            if(scale<=0)return;int save=c.save();c.scale(scale,scale);width=getWidth()/scale;height=getHeight()/scale;
            c.translate(0,Math.max(0,(height-77)/2));render(c);c.restoreToCount(save);
        }
        abstract void render(Canvas c);
        void text(Canvas c,String s,float x,float y,float size,int color,Typeface font){p.setStyle(Paint.Style.FILL);p.setShader(null);p.setTypeface(font);p.setTextSize(size);p.setColor(color==0xfff4f9f8?LauncherinAppearance.get(getContext()).text:color);c.drawText(s,x,y,p);}
        String fit(String text,float size,float available){p.setTypeface(regular);p.setTextSize(size);return TextUtils.ellipsize(text,new android.text.TextPaint(p),Math.max(0,available),TextUtils.TruncateAt.END).toString();}
        void line(Canvas c,float x,float y,float end,float value,int color){p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(3);p.setColor(0x25ffffff);c.drawLine(x,y,end,y,p);if(value>0){p.setShader(new LinearGradient(x,y,end,y,new int[]{LauncherinAppearance.blend(color,Color.WHITE,.25f),color},null,Shader.TileMode.CLAMP));c.drawLine(x,y,x+(end-x)*Math.min(1,value),y,p);p.setShader(null);}p.setStyle(Paint.Style.FILL);}
    }
    static final class Clock extends Face {
        private final Typeface clockNumbers,clockDate;
        private String time="—",date="",day="";
        Clock(Context c){super(c);clockNumbers=c.getResources().getFont(R.font.outfit_medium);clockDate=c.getResources().getFont(R.font.outfit_regular);}
        void update(String time,String day,String date){this.time=time;this.day=day;this.date=date;setContentDescription(day+", "+date+", "+time);invalidate();}
        @Override void render(Canvas c){
            text(c,day.toUpperCase(Locale.forLanguageTag("pt-BR")),0,10,10,LauncherinAppearance.get(getContext()).accent,medium);
            p.setTypeface(clockNumbers);p.setTextSize(48);float size=Math.min(48,48*(width-2)/Math.max(1,p.measureText(time)));
            text(c,time,-1,56,size,0xfff4f9f8,clockNumbers);
            text(c,fit(date,11,width),1,75,11,0xffd2deda,clockDate);
        }
    }
    static final class Weather extends Face {
        private final Typeface temperatureFont;private int code=-1;private boolean night;private final RectF artwork=new RectF(0,0,61,56);
        private String temperature="—",city="Escolha sua cidade",condition="";
        Weather(Context c){super(c);temperatureFont=c.getResources().getFont(R.font.outfit_medium);}
        void update(LauncherinWeather.Snapshot s,long now){code=s.code;night=s.night(now);temperature=s.temperature+"°";city=s.city;condition=s.old()?"Dados anteriores":s.conditions;describe();invalidate();}
        void empty(String label){code=-1;temperature="—";city=label;condition="";describe();invalidate();}
        private void describe(){setContentDescription(temperature+" · "+city+" · "+condition);}
        @Override void render(Canvas c){
            LauncherinWeatherScene.Sky kind=LauncherinWeatherScene.kind(code);
            int accent=accent(code,night);
            text(c,temperature,65,45,39,0xfff4f9f8,temperatureFont);
            int save=c.save();c.translate(0,3);icon(c,kind,night);c.restoreToCount(save);
            text(c,fit(city,10,width),0,61,10,0xfff1f4f8,medium);
            text(c,fit(condition,10,width),0,76,10,0xffc5d5df,regular);
        }
        static int accent(int code,boolean night){return night?0xffc1baff:LauncherinWeatherScene.kind(code)==LauncherinWeatherScene.Sky.CLEAR?0xffffd47c:0xff9cdeff;}
        private void icon(Canvas c,LauncherinWeatherScene.Sky sky,boolean night){
            if(LauncherinWeatherArt.draw(getContext(),c,artwork,code,night))return;
            if(sky==LauncherinWeatherScene.Sky.UNKNOWN){text(c,"—",20,35,28,0xffb9c4d6,numbers);return;}
            if(sky==LauncherinWeatherScene.Sky.CLEAR||sky==LauncherinWeatherScene.Sky.PARTLY_CLOUDY){
                p.setColor(night?0x25c8bdff:0x25ffd16b);c.drawCircle(31,21,21,p);
                p.setColor(night?0xffcfc6ff:0xffffcb5c);c.drawCircle(31,21,12,p);
                if(night){p.setColor(0xffede8ff);c.drawCircle(27,18,3,p);c.drawCircle(36,25,2,p);p.setColor(0xffd6d0ff);c.drawCircle(7,9,1.5f,p);c.drawCircle(52,4,1,p);}
                else {p.setStrokeWidth(2);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(31+(float)Math.cos(a)*16,21+(float)Math.sin(a)*16,31+(float)Math.cos(a)*20,21+(float)Math.sin(a)*20,p);}}
            }
            if(sky!=LauncherinWeatherScene.Sky.CLEAR){
                p.setShader(new LinearGradient(0,15,0,44,new int[]{0xffecf8ff,night?0xff8b9fce:0xff8fc2dd},null,Shader.TileMode.CLAMP));
                c.drawRoundRect(5,29,57,42,7,7,p);c.drawCircle(19,28,11,p);c.drawCircle(33,25,14,p);c.drawCircle(47,31,9,p);p.setShader(null);
                if(sky==LauncherinWeatherScene.Sky.RAIN||sky==LauncherinWeatherScene.Sky.STORM){p.setColor(0xff68c8ff);p.setStrokeWidth(2.3f);for(int i=0;i<3;i++)c.drawLine(16+i*14,46,12+i*14,53,p);}
                if(sky==LauncherinWeatherScene.Sky.STORM){Path bolt=new Path();bolt.moveTo(32,36);bolt.lineTo(25,47);bolt.lineTo(32,47);bolt.lineTo(28,56);bolt.lineTo(42,41);bolt.lineTo(34,41);bolt.close();p.setColor(0xffffce63);c.drawPath(bolt,p);}
                if(sky==LauncherinWeatherScene.Sky.SNOW){p.setColor(0xffdbf5ff);for(int i=0;i<3;i++)c.drawCircle(15+i*16,49,2,p);}
                if(sky==LauncherinWeatherScene.Sky.FOG){p.setColor(0xffbbd9e7);p.setStrokeWidth(2);c.drawLine(10,47,53,47,p);c.drawLine(18,52,45,52,p);}
            }
        }
    }
    static final class System extends Face {
        private LauncherinSystemStats.Sample sample;private Double download;
        System(Context c){super(c);}
        void update(LauncherinSystemStats.Sample sample,Double download){this.sample=sample;this.download=download;setContentDescription(description());invalidate();}
        String description(){return "Sistema. "+(sample==null?"Aguardando medição.":String.format(Locale.forLanguageTag("pt-BR"),"RAM %.1f de %.1f GB. ",sample.usedMemory()/1073741824d,sample.totalMemory/1073741824d)+(sample.wholeTv?"CPU da TV ":"CPU da launcher ")+(sample.cpu==null?"aguardando":Math.round(sample.cpu)+" por cento"))+(sample!=null&&sample.totalStorage>0?String.format(Locale.forLanguageTag("pt-BR"),". Armazenamento %.1f de %.1f GB em uso",(sample.totalStorage-sample.availableStorage)/1073741824d,sample.totalStorage/1073741824d):"")+". Download da TV "+(download==null?"indisponível ou aguardando amostra":LauncherinDownloadStats.format(download)+" megabits por segundo");}
        @Override void render(Canvas c){
            boolean ram=sample!=null&&sample.totalMemory>0;float memory=ram?(float)sample.usedMemory()/sample.totalMemory:0;
            Double cpu=sample==null?null:sample.cpu;
            text(c,"RAM",0,10,9,0xffa9c2d3,medium);
            text(c,ram?String.format(Locale.forLanguageTag("pt-BR"),"%.1f / %.1f GB",sample.usedMemory()/1073741824d,sample.totalMemory/1073741824d):"—",width*.15f,10,10,LauncherinAppearance.get(getContext()).accent,medium);
            line(c,1,19,width*.58f,memory,LauncherinAppearance.get(getContext()).accent);
            boolean disk=sample!=null&&sample.totalStorage>0;float used=disk?(float)(sample.totalStorage-sample.availableStorage)/sample.totalStorage:0;text(c,"DISCO",width*.65f,10,8,0xffa9c2d3,medium);text(c,disk?Math.round(used*100)+"%":"—",width*.85f,10,10,0xffcbd9d5,medium);line(c,width*.65f,19,width-2,used,0xffa7d8ba);
            text(c,sample!=null&&!sample.wholeTv?"CPU · APP":"CPU · TV",0,35,9,0xffa9c2d3,medium);
            text(c,cpu==null?"—":Math.round(cpu)+"%",width*.68f,35,12,LauncherinAppearance.get(getContext()).accent,medium);
            line(c,1,43,width-2,cpu==null?0:cpu.floatValue()/100,LauncherinAppearance.get(getContext()).accent);
            text(c,"↓",0,70,23,0xff8fe3c6,numbers);
            String speed=LauncherinDownloadStats.format(download);text(c,speed,21,70,24,LauncherinAppearance.get(getContext()).accent,numbers);
            p.setTypeface(numbers);p.setTextSize(24);float end=21+p.measureText(speed);
            text(c,"Mb/s",Math.min(width-28,end+5),70,10,0xffd0e4dc,regular);
        }
    }
}
