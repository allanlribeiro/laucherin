package app.launcherin.tv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Open-Meteo weather for a chosen city; no GPS permission and no background service. */
final class LauncherinWeather {
    static final String QUERY = "weather_city_query";
    static final long REFRESH_MS = 30 * 60 * 1000L;
    private static SharedPreferences store(Context context) { return context.getSharedPreferences("launcherin-weather", 0); }
    static String query(Context context) { return clean(LauncherinExtras.read(context).optString(QUERY)); }
    static String clean(String text) {
        String value=text.replaceAll("\\p{Cntrl}"," ").replace('\u2028',' ').replace('\u2029',' ').trim();
        return value.substring(0,Math.min(80,value.length()));
    }
    static String canonical(String text) {
        return java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}","").toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]","");
    }
    static String regionTerm(String text,String countryCode) {
        String key=canonical(text);if(!countryCode.equalsIgnoreCase("BR"))return key;
        String states="AC=Acre|AL=Alagoas|AP=Amapá|AM=Amazonas|BA=Bahia|CE=Ceará|DF=Distrito Federal|ES=Espírito Santo|GO=Goiás|MA=Maranhão|MT=Mato Grosso|MS=Mato Grosso do Sul|MG=Minas Gerais|PA=Pará|PB=Paraíba|PR=Paraná|PE=Pernambuco|PI=Piauí|RJ=Rio de Janeiro|RN=Rio Grande do Norte|RS=Rio Grande do Sul|RO=Rondônia|RR=Roraima|SC=Santa Catarina|SP=São Paulo|SE=Sergipe|TO=Tocantins";
        for(String pair:states.split("\\|")){String[] fields=pair.split("=",2);if(key.equals(canonical(fields[0])))return canonical(fields[1]);}return key;
    }

    static final class City {
        final String name, region, country, countryCode;
        final double latitude, longitude;
        City(JSONObject json) throws Exception {
            name = json.getString("name"); region = json.optString("admin1"); country = json.optString("country");countryCode=json.optString("country_code");
            latitude = json.getDouble("latitude"); longitude = json.getDouble("longitude");
            if(name.isEmpty() || !Double.isFinite(latitude) || !Double.isFinite(longitude) || Math.abs(latitude)>90 || Math.abs(longitude)>180) throw new IOException("Cidade inválida");
        }
        String label() { return name + (region.isEmpty()?"":", "+region) + (country.isEmpty()?"":", "+country); }
        JSONObject json() throws Exception { return new JSONObject().put("name",name).put("admin1",region).put("country",country).put("country_code",countryCode).put("latitude",latitude).put("longitude",longitude); }
    }
    static final class Snapshot {
        final String query, city, conditions, symbol;
        final int temperature, code, offset;final java.time.ZoneId zone;final boolean observedDay;
        final long updated;
        final List<Day> days = new ArrayList<>();
        Snapshot(JSONObject json) throws Exception {
            query=json.getString("query");city=json.getString("city");temperature=json.getInt("temperature");updated=json.getLong("updated");
            code=json.getInt("code");conditions=description(code);symbol=symbol(code);offset=json.optInt("offset");zone=zone(json.optString("zone"),offset,updated);observedDay=json.optBoolean("is_day",true);
            JSONArray rows=json.optJSONArray("days");if(rows!=null)for(int i=0;i<Math.min(rows.length(),7);i++)days.add(new Day(rows.getJSONObject(i)));
        }
        boolean old() { return System.currentTimeMillis()-updated>REFRESH_MS; }
        boolean needsRefresh(long now) { return now-updated>REFRESH_MS||nextDays(now).size()<5; }
        boolean night(long now) {
            java.time.LocalDate today=java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
            for(Day day:days)if(day.date.equals(today)){if(day.sunrise>0&&day.sunset>day.sunrise)return now<day.sunrise||now>=day.sunset;return !observedDay;}
            int hour=java.time.Instant.ofEpochMilli(now).atZone(zone).getHour();return hour<6||hour>=18;
        }
        List<Day> nextDays(long now) {
            java.time.LocalDate today=java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
            List<Day> result=new ArrayList<>();for(Day day:days)if(day.date.isAfter(today)&&result.size()<5)result.add(day);return result;
        }
        String icon(long now) { return code==0&&night(now)?"☾":symbol; }
    }
    private static java.time.ZoneId zone(String name,int offset,long observed) {
        try{java.time.ZoneId zone=java.time.ZoneId.of(name);if(zone.getRules().getOffset(java.time.Instant.ofEpochMilli(observed)).getTotalSeconds()==offset)return zone;}catch(Exception ignored){}
        // Old Android timezone databases can still apply the abolished Brazilian daylight saving time.
        return java.time.ZoneOffset.ofTotalSeconds(offset);
    }
    static final class Day {
        final java.time.LocalDate date;final int code,low,high,rain;final long sunrise,sunset;
        Day(JSONObject row) throws Exception {
            date=java.time.LocalDate.parse(row.getString("date"));code=row.getInt("code");low=row.getInt("low");high=row.getInt("high");rain=row.optInt("rain",-1);
            sunrise=row.optLong("sunrise");sunset=row.optLong("sunset");
            if(low< -100||high>70||low>high||rain< -1||rain>100)throw new IOException("Previsão inválida");
        }
    }
    static JSONObject forecast(JSONObject response,String query,String city,long now) throws Exception {
        JSONObject current=response.getJSONObject("current"),daily=response.getJSONObject("daily");
        double temperature=current.getDouble("temperature_2m");if(!Double.isFinite(temperature)||temperature< -100||temperature>70)throw new IOException("Temperatura inválida");
        int offset=response.getInt("utc_offset_seconds");if(Math.abs(offset)>18*3600)throw new IOException("Fuso inválido");
        java.time.ZoneId zone=zone(response.optString("timezone"),offset,now);
        JSONArray times=daily.getJSONArray("time"),days=new JSONArray();if(times.length()<6)throw new IOException("Previsão incompleta");
        for(int i=0;i<Math.min(7,times.length());i++) {
            long epoch=times.getLong(i);String date=java.time.Instant.ofEpochSecond(epoch).atZone(zone).toLocalDate().toString();
            JSONObject row=new JSONObject().put("date",date).put("code",daily.getJSONArray("weather_code").getInt(i))
                    .put("low",Math.round(daily.getJSONArray("temperature_2m_min").getDouble(i)))
                    .put("high",Math.round(daily.getJSONArray("temperature_2m_max").getDouble(i)))
                    .put("rain",daily.getJSONArray("precipitation_probability_max").isNull(i)?-1:daily.getJSONArray("precipitation_probability_max").getInt(i))
                    .put("sunrise",daily.getJSONArray("sunrise").optLong(i,0)*1000).put("sunset",daily.getJSONArray("sunset").optLong(i,0)*1000);
            new Day(row);days.put(row);
        }
        return new JSONObject().put("query",query).put("city",city).put("temperature",Math.round(temperature)).put("code",current.getInt("weather_code"))
                .put("offset",offset).put("zone",zone.getId()).put("is_day",current.optInt("is_day",1)==1).put("days",days).put("updated",now);
    }
    private static JSONObject get(String url) throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(6000);connection.setReadTimeout(8000);
        try {
            if(connection.getResponseCode()!=200)throw new IOException("Consulta indisponível");
            try(InputStream in=connection.getInputStream()) {
                JSONObject json=new JSONObject(new String(LauncherinExtras.bytes(in,262144),StandardCharsets.UTF_8));
                if(json.optBoolean("error"))throw new IOException("Consulta inválida");return json;
            }
        } finally { connection.disconnect(); }
    }
    static List<City> search(String query) throws Exception {
        String name=clean(query).split(",",2)[0].trim();if(name.length()<2)return new ArrayList<>();
        JSONObject json=get("https://geocoding-api.open-meteo.com/v1/search?name="+URLEncoder.encode(name,"UTF-8")+"&count=10&language=pt&format=json");
        JSONArray rows=json.optJSONArray("results");List<City> result=new ArrayList<>();
        if(rows!=null)for(int i=0;i<rows.length();i++)try{City city=new City(rows.getJSONObject(i));String[] parts=clean(query).split(",");boolean matches=true;
            for(int k=1;k<parts.length;k++){String term=canonical(parts[k]),regional=regionTerm(parts[k],city.countryCode);if(!term.isEmpty()&&!canonical(city.region).contains(regional)&&!canonical(city.country).contains(term)&&!canonical(city.countryCode).equals(term))matches=false;}
            if(matches)result.add(city);}catch(Exception ignored){}
        return result;
    }
    static void select(Context context, City city) throws Exception {
        String query=clean(city.label());LauncherinExtras.setString(context,QUERY,query);
        store(context).edit().putString("selected",city.json().put("query",query).toString()).remove("attempt-query").remove("attempted").apply();
    }
    static Snapshot cached(Context context,String query) {
        try{Snapshot result=new Snapshot(new JSONObject(store(context).getString("forecast","{}")));return query.equals(result.query)?result:null;}catch(Exception ignored){return null;}
    }
    static Snapshot load(Context context,String query) throws Exception {
        Snapshot cached=cached(context,query);SharedPreferences preferences=store(context);long now=System.currentTimeMillis();
        if(cached!=null&&!cached.needsRefresh(now))return cached;
        String attemptKey=query+"@"+java.time.Instant.ofEpochMilli(now).atZone(cached==null?java.time.ZoneId.systemDefault():cached.zone).toLocalDate();
        if(attemptKey.equals(preferences.getString("attempt-query","")) && now-preferences.getLong("attempted",0)<5*60*1000L) {
            if(cached!=null)return cached;throw new IOException("Aguardando nova consulta");
        }
        preferences.edit().putString("attempt-query",attemptKey).putLong("attempted",now).apply();
        try {
            City city=null;
            try { JSONObject selected=new JSONObject(preferences.getString("selected","{}"));if(query.equals(selected.optString("query")))city=new City(selected); } catch(Exception ignored){}
            if(city==null) {
                List<City> results=search(query);if(results.isEmpty())throw new IOException("Cidade não encontrada");city=results.get(0);
                if(query.equals(query(context)))preferences.edit().putString("selected",city.json().put("query",query).toString()).apply();
            }
            JSONObject response=get("https://api.open-meteo.com/v1/forecast?latitude="+city.latitude+"&longitude="+city.longitude
                    +"&current=temperature_2m,weather_code,is_day&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset&forecast_days=6&temperature_unit=celsius&timezone=auto&timeformat=unixtime");
            JSONObject saved=forecast(response,query,city.label(),System.currentTimeMillis());
            if(query.equals(query(context)))preferences.edit().putString("forecast",saved.toString()).apply();
            return new Snapshot(saved);
        } catch(Exception error) { if(cached!=null)return cached;throw error; }
    }
    static String description(int code) {
        switch(code){case 0:return "Céu limpo";case 1:case 2:return "Parcialmente nublado";case 3:return "Nublado";case 45:case 48:return "Neblina";
        case 51:case 53:case 55:case 56:case 57:return "Garoa";case 61:case 63:case 65:case 66:case 67:return "Chuva";
        case 71:case 73:case 75:case 77:return "Neve";case 80:case 81:case 82:return "Pancadas de chuva";case 85:case 86:return "Pancadas de neve";
        case 95:case 96:case 99:return "Trovoadas";default:return "Tempo indisponível";}
    }
    static String symbol(int code) {
        switch(code){case 0:return "☀";case 1:case 2:case 3:return "☁";case 45:case 48:return "≋";case 71:case 73:case 75:case 77:case 85:case 86:return "❄";
        case 51:case 53:case 55:case 56:case 57:case 61:case 63:case 65:case 66:case 67:case 80:case 81:case 82:return "☂";case 95:case 96:case 99:return "ϟ";default:return "—";}
    }
}
