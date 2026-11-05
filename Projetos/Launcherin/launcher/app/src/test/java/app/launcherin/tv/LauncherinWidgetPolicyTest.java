package app.launcherin.tv;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class LauncherinWidgetPolicyTest {
    @Test public void oldHomesKeepFourCardsAndNewWidgetsStartHidden(){
        List<LauncherinWidgetLayout.Slot> slots=LauncherinWidgetLayout.read(new JSONObject());assertEquals(7,slots.size());
        for(int i=0;i<slots.size();i++)assertEquals(i<4,slots.get(i).enabled);
        assertEquals(1,LauncherinWidgetLayout.usedRows(slots));
    }
    @Test public void anExplicitEmptyLayoutHidesEverything(){assertEquals(0,LauncherinWidgetLayout.usedRows(LauncherinWidgetLayout.clean(new JSONArray())));}
    @Test public void malformedCollisionsAndLargeCardsStayInsideTheBoard()throws Exception{
        JSONArray input=new JSONArray("[{\"id\":\"clock\",\"x\":99,\"y\":9,\"w\":6,\"h\":2},{\"id\":\"photos\",\"x\":6,\"w\":6,\"h\":2},{\"id\":\"system\",\"x\":0},{\"id\":\"../evil\"},{\"id\":\"clock\"}]");
        List<LauncherinWidgetLayout.Slot> slots=LauncherinWidgetLayout.clean(input);assertEquals(7,slots.size());
        for(LauncherinWidgetLayout.Slot a:slots){assertTrue(a.x>=0&&a.y>=0&&a.x+a.w<=12&&a.y+a.h<=2);for(LauncherinWidgetLayout.Slot b:slots)if(a!=b)assertFalse(a.overlaps(b));}
    }
    @Test public void cpuCountersNeverInventAValue(){
        assertNull(LauncherinSystemStats.parse("intr 10 50"));assertNull(LauncherinSystemStats.parse("cpu 1 -2 3 4"));
        long[] v=LauncherinSystemStats.parse("cpu 100 20 30 400 50 10 15 5 2 3");assertArrayEquals(new long[]{630,450},v);
        assertNull(LauncherinSystemStats.busy(0,0));assertNull(LauncherinSystemStats.busy(10,20));assertEquals(25,LauncherinSystemStats.busy(100,75),.001);
        assertEquals(5,LauncherinSystemStats.percentage(1000,5000,4),.001);
    }
    @Test public void agendaValidatesDatesAndDoesNotAcceptMalformedPayloads(){
        assertTrue(LauncherinAgendaData.valid("{\"events\":[{\"id\":\"event-1\",\"title\":\"Consulta\",\"date\":\"2026-11-05\",\"time\":\"09:30\"}]}".getBytes()));
        assertFalse(LauncherinAgendaData.valid("{\"events\":[{\"id\":\"1\",\"title\":\"x\",\"date\":\"2026-02-30\"}]}".getBytes()));
        assertFalse(LauncherinAgendaData.valid("{\"events\":[{\"id\":\"1\",\"title\":\"x\",\"date\":\"2026-11-05\",\"time\":\"25:00\"}]}".getBytes()));
    }
    @Test public void revealKeepsTheAppsInsideTheScreen(){
        for(int h:new int[]{480,540,720,1080}){
            int offset=LauncherinWatchMode.revealOffset(h,h+39,Math.round(h*.278f),16);
            assertTrue(Math.round(h*.76f)-offset>=0);
            assertTrue(Math.round(h*.965f)-offset<h);
            assertEquals(h-16,h+39+Math.round(h*.278f)-offset);
        }
        assertEquals("above",LauncherinWatchMode.clean("unknown"));assertEquals("hidden",LauncherinWatchMode.clean("hidden"));
    }
    @Test public void dataAndPhotosHaveOnlyNarrowProviderNames(){
        assertEquals("agenda.json",ThemeFiles.path(Collections.singletonList("agenda.json")));
        assertNotNull(ThemeFiles.path(Collections.singletonList("frame-"+String.join("",Collections.nCopies(40,"a"))+".jpg")));
        assertNull(ThemeFiles.path(Collections.singletonList("../agenda.json")));
        assertNull(ThemeFiles.path(Collections.singletonList("frame-user.jpg")));
        assertNull(ThemeFiles.path(Arrays.asList("agenda.json","other")));
    }
}
