package app.launcherin.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class LauncherinWeatherTest {
    private org.json.JSONObject snapshot() throws Exception {
        org.json.JSONArray days=new org.json.JSONArray();
        for(int i=4;i<=9;i++)days.put(new org.json.JSONObject().put("date","2026-11-0"+i).put("code",3).put("low",19).put("high",27).put("rain",30)
                .put("sunrise",java.time.Instant.parse("2026-11-0"+i+"T08:00:00Z").toEpochMilli()).put("sunset",java.time.Instant.parse("2026-11-0"+i+"T21:00:00Z").toEpochMilli()));
        return new org.json.JSONObject().put("query","Serra, ES").put("city","Serra").put("temperature",25).put("updated",1).put("code",3).put("offset",-10800).put("days",days);
    }
    @Test public void daylightUsesCitySunriseAndSunsetRatherThanTvTimezone() throws Exception {
        LauncherinWeather.Snapshot weather=new LauncherinWeather.Snapshot(snapshot());
        assertTrue(weather.night(java.time.Instant.parse("2026-11-04T07:59:59Z").toEpochMilli()));
        assertFalse(weather.night(java.time.Instant.parse("2026-11-04T08:00:00Z").toEpochMilli()));
        assertFalse(weather.night(java.time.Instant.parse("2026-11-04T20:59:59Z").toEpochMilli()));
        assertTrue(weather.night(java.time.Instant.parse("2026-11-04T21:00:00Z").toEpochMilli()));
    }
    @Test public void fiveDaysStartTomorrowInTheCityEvenWhenUtcDateAlreadyChanged() throws Exception {
        LauncherinWeather.Snapshot weather=new LauncherinWeather.Snapshot(snapshot());
        java.util.List<LauncherinWeather.Day> next=weather.nextDays(java.time.Instant.parse("2026-11-05T01:00:00Z").toEpochMilli());
        assertEquals(5,next.size());assertEquals("2026-11-05",next.get(0).date.toString());assertEquals("2026-11-09",next.get(4).date.toString());
    }
    @Test public void forecastRefreshesAtCityMidnightEvenWhenTheCacheIsOnlyTwoMinutesOld() throws Exception {
        long before=java.time.Instant.parse("2026-11-05T02:59:00Z").toEpochMilli(),after=before+120000;
        LauncherinWeather.Snapshot weather=new LauncherinWeather.Snapshot(snapshot().put("updated",before));
        assertFalse(weather.needsRefresh(before));assertTrue(weather.needsRefresh(after));assertEquals(4,weather.nextDays(after).size());
    }
    @Test public void providerOffsetWinsWhenAndroidTimezoneRulesAreOutdated() throws Exception {
        long now=java.time.Instant.parse("2026-11-05T02:30:00Z").toEpochMilli();
        LauncherinWeather.Snapshot weather=new LauncherinWeather.Snapshot(snapshot().put("updated",now).put("zone","Europe/London"));
        assertEquals("-03:00",weather.zone.getId());assertEquals(5,weather.nextDays(now).size());assertTrue(weather.night(now));
    }
    @Test public void cloudyRainAndSnowRemainDifferentAtNight() {
        assertEquals(LauncherinWeatherScene.Sky.CLOUDY,LauncherinWeatherScene.kind(3));
        assertEquals(LauncherinWeatherScene.Sky.RAIN,LauncherinWeatherScene.kind(80));
        assertEquals(LauncherinWeatherScene.Sky.SNOW,LauncherinWeatherScene.kind(85));
        assertEquals(LauncherinWeatherScene.Sky.UNKNOWN,LauncherinWeatherScene.kind(-1));
    }
    @Test public void clearNightUsesMoonInsteadOfSunAndUnknownDataStaysUnknown() throws Exception {
        org.json.JSONObject row=snapshot().put("code",0);LauncherinWeather.Snapshot weather=new LauncherinWeather.Snapshot(row);
        assertEquals("☾",weather.icon(java.time.Instant.parse("2026-11-04T23:00:00Z").toEpochMilli()));
        assertEquals("☀",weather.icon(java.time.Instant.parse("2026-11-04T12:00:00Z").toEpochMilli()));
        assertEquals("—",new LauncherinWeather.Snapshot(row.put("code",-1)).icon(System.currentTimeMillis()));
    }
    @Test public void sixDayApiForecastKeepsDatesThroughDaylightSavingTransition() throws Exception {
        java.time.ZoneId zone=java.time.ZoneId.of("America/New_York");org.json.JSONArray times=new org.json.JSONArray(),sunrise=new org.json.JSONArray(),sunset=new org.json.JSONArray(),codes=new org.json.JSONArray(),lows=new org.json.JSONArray(),highs=new org.json.JSONArray(),rain=new org.json.JSONArray();
        for(int i=0;i<6;i++){
            java.time.LocalDate date=java.time.LocalDate.of(2026,3,7).plusDays(i);times.put(date.atStartOfDay(zone).toEpochSecond());sunrise.put(date.atTime(7,0).atZone(zone).toEpochSecond());sunset.put(date.atTime(18,0).atZone(zone).toEpochSecond());codes.put(3);lows.put(4);highs.put(12);rain.put(i==1?org.json.JSONObject.NULL:20);
        }
        org.json.JSONObject api=new org.json.JSONObject().put("timezone",zone.getId()).put("utc_offset_seconds",-18000)
            .put("current",new org.json.JSONObject().put("temperature_2m",8.2).put("weather_code",3).put("is_day",1))
            .put("daily",new org.json.JSONObject().put("time",times).put("sunrise",sunrise).put("sunset",sunset).put("weather_code",codes).put("temperature_2m_min",lows).put("temperature_2m_max",highs).put("precipitation_probability_max",rain));
        long now=java.time.Instant.parse("2026-03-07T17:00:00Z").toEpochMilli();LauncherinWeather.Snapshot result=new LauncherinWeather.Snapshot(LauncherinWeather.forecast(api,"New York","New York",now));
        assertEquals(5,result.nextDays(now).size());assertEquals("2026-03-08",result.nextDays(now).get(0).date.toString());assertEquals("2026-03-12",result.nextDays(now).get(4).date.toString());assertEquals(-1,result.days.get(1).rain);
        assertFalse(result.night(java.time.Instant.parse("2026-03-08T21:00:00Z").toEpochMilli()));
    }
    @Test public void showersAndSnowUseTheirActualWmoCodes() {
        assertEquals("Pancadas de chuva",LauncherinWeather.description(80));
        assertEquals("☂",LauncherinWeather.symbol(82));
        assertEquals("Pancadas de neve",LauncherinWeather.description(85));
        assertEquals("❄",LauncherinWeather.symbol(86));
        assertEquals("Tempo indisponível",LauncherinWeather.description(-1));
    }
    @Test public void regionAbbreviationsDisambiguateBrazilianCities() {
        assertEquals(LauncherinWeather.canonical("Espírito Santo"),LauncherinWeather.regionTerm("ES","BR"));
        assertEquals(LauncherinWeather.canonical("Mato Grosso do Sul"),LauncherinWeather.regionTerm("MS","BR"));
        assertEquals("es",LauncherinWeather.regionTerm("ES","ES"));
        assertEquals("saopaulo",LauncherinWeather.canonical("São Paulo"));
    }
    @Test public void cityQueriesAreOneLineAndBounded() {
        assertEquals("Serra , ES",LauncherinWeather.clean(" Serra\n, ES "));
        assertEquals(80,LauncherinWeather.clean(new String(new char[100]).replace('\0','x')).length());
    }
}
