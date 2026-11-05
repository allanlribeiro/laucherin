package tv.cleanyour.core.flows

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CompletableDeferred
import tv.cleanyour.core.model.*
import tv.cleanyour.core.spec.*
import tv.cleanyour.core.session.ExecResult
import tv.cleanyour.testing.fixtures.*
import kotlin.test.*

class WidgetFeaturesTest {
    @Test fun layoutsRejectCollisionsAndAllowEveryWidgetToBeHidden() {
        val original=LauncherinWidgets.defaults()
        assertNull(LauncherinWidgets.change(original,original[0].copy(x=3)))
        val added=assertNotNull(LauncherinWidgets.change(original,original[4].copy(enabled=true),true))
        assertEquals(1,added[4].y)
        val hidden=added.map { it.copy(enabled=false) }
        assertTrue(LauncherinWidgets.decode(LauncherinWidgets.encode(hidden).toString()).none { it.enabled })
        assertEquals(7,LauncherinWidgets.decode("[]").size)
    }
    @Test fun layoutPhotosAndWatchModeRoundTripThroughTheActualSavedLook() {
        val detail=LookDetails(widgetLayout=LauncherinWidgets.defaults().map { it.copy(enabled=it.id=="system") },
            framePhotos=listOf("a".repeat(64)),watchNextMode="below")
        val saved=HomeChoice(emptyList(),null,style="glass",picture="none",details=detail)
        assertEquals(detail,HomePrefs.decode(HomePrefs.encode(saved)).details)
        val themes=BundledThemeSource();val drafts=LookDrafts(themes,themes,ConfigEncoder.Codec,FakeClock(Samples.NOW_MS));val ctx=LookContext(tv=LookTv(18,2048))
        val draft=drafts.edit(drafts.open(saved,ctx),LookEdit.SelectStyle(StyleId.CALM),ctx)
        assertEquals(detail.widgetLayout,draft.details.widgetLayout)
        assertEquals("below",draft.details.watchNextMode)
        assertTrue(draft.fullFile.contains("dashboard_widgets"));assertTrue(draft.fullFile.contains("frame_photos"))
    }
    @Test fun invalidAgendaEntriesCannotTravelToTheTv() {
        val entries=listOf(AgendaEntry("1","Consulta","2026-11-05","09:30"),AgendaEntry("2","bad","2026-02-30"),AgendaEntry("3","bad","2026-11-05","25:00"))
        assertEquals(listOf(entries[0]),LauncherinAgenda.decode(LauncherinAgenda.encode(entries)))
    }
    private fun Harness.agendaResponses() {
        session.recording.fallback={ line ->
            val output=when {
                "--method commitAgenda" in line->"Result: Bundle[{ok=true}]"
                "sha256sum" in line->session.files[RemoteDir.TMP.path+"/theme-agenda.pending.json"]?.let { sha256Of(it).hex+"  -" }.orEmpty()
                else->""
            };ExecResult(0,output,"",false,1)
        }
    }
    @Test fun addingAnAppointmentChangesOnlyTheDataFileAndNeverPlansAReconfiguration() = runTest {
        val h=Harness();val tv=h.knownTv().copy(launcher=LauncherState(18,Samples.zeroHash,Samples.zeroHash,Samples.zeroHash));h.tvs.upsert(tv);h.agendaResponses()
        val home=HomeChoice(emptyList(),null,style="glass",details=LookDetails(watchNextMode="hidden"));HomePrefs(h.documents).save(tv.id,home)
        var phase=AgendaSync.READY;var seen=emptyList<AgendaEntry>()
        val repo=WidgetAgendaRepository(h.manageDeps(),backgroundScope,tv.id){entries,sync->phase=sync;seen=entries}
        repo.load();settle();repo.upsert(AgendaEntry("manual-1","Consulta","2026-11-05","14:30"));settle()
        assertEquals(AgendaSync.SYNCED,phase);assertEquals(1,seen.size)
        assertEquals(home,HomePrefs(h.documents).load(tv.id));assertTrue(h.planner.requests.isEmpty());assertTrue(h.engine.calls.isEmpty())
        assertTrue(h.session.execLines.none { "am " in it || "force-stop" in it || "settings put" in it || "config.json" in it })
        repo.upsert(seen.single().copy(title="Consulta alterada"));settle()
        assertEquals("Consulta alterada",seen.single().title);assertEquals(AgendaSync.SYNCED,phase)
    }
    @Test fun offlineEditsSurviveARepositoryRestartThenSynchronizeOnReconnect() = runTest {
        val h=Harness();val tv=h.knownTv().copy(launcher=LauncherState(18,Samples.zeroHash,Samples.zeroHash,Samples.zeroHash));h.tvs.upsert(tv);h.agendaResponses()
        val connected=h.session.state.value;h.session.setState(ConnectionState.Idle)
        var phase=AgendaSync.READY;var seen=emptyList<AgendaEntry>()
        val d=h.manageDeps();val first=WidgetAgendaRepository(d,backgroundScope,tv.id){entries,sync->phase=sync;seen=entries}
        first.upsert(AgendaEntry("manual-1","Compromisso offline","2026-11-05"));settle()
        assertEquals(AgendaSync.PENDING,phase);assertTrue(h.session.execLines.isEmpty())
        val reopened=WidgetAgendaRepository(d,backgroundScope,tv.id){entries,sync->phase=sync;seen=entries};reopened.load();settle()
        assertEquals("Compromisso offline",seen.single().title)
        h.session.setState(connected);reopened.sync(true);settle();assertEquals(AgendaSync.SYNCED,phase)
        reopened.remove("manual-1");settle();assertTrue(seen.isEmpty());assertEquals(AgendaSync.SYNCED,phase)
    }
    @Test fun anEditDuringTheFinalReadIsStillSentWithoutAnotherUserAction() = runTest {
        val h=Harness();val tv=h.knownTv().copy(launcher=LauncherState(18,Samples.zeroHash,Samples.zeroHash,Samples.zeroHash));h.tvs.upsert(tv);h.agendaResponses()
        val captured=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>();var blocked=false
        val documents=object : tv.cleanyour.core.store.DocumentStore by h.documents {
            override suspend fun <T> read(path:String,decode:(ByteArray)->T):T? {
                val value=h.documents.read(path,decode)
                if(!blocked && path.endsWith("/agenda.json") && h.documents.read("tvs/${tv.id.value}/agenda-sent.sha256"){it}!=null) {
                    blocked=true;captured.complete(Unit);release.await()
                }
                return value
            }
        }
        var phase=AgendaSync.READY
        val repo=WidgetAgendaRepository(h.manageDeps(documents=documents),backgroundScope,tv.id){_,sync->phase=sync}
        repo.upsert(AgendaEntry("one","Primeira versão","2026-11-05"));settle();assertTrue(captured.isCompleted)
        repo.upsert(AgendaEntry("one","Versão mais recente","2026-11-05"));settle()
        release.complete(Unit);settle()
        assertEquals(AgendaSync.SYNCED,phase)
        val transmitted=h.session.files[RemoteDir.TMP.path+"/theme-agenda.pending.json"]!!
        assertEquals("Versão mais recente",LauncherinAgenda.decode(transmitted.toString(Charsets.UTF_8)).single().title)
        assertEquals(2,h.session.execLines.count { "--method commitAgenda" in it })
    }
}
