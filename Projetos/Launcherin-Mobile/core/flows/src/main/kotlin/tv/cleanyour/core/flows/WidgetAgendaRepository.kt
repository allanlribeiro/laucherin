package tv.cleanyour.core.flows

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tv.cleanyour.core.model.*
import tv.cleanyour.core.ops.*
import tv.cleanyour.core.session.ExecSpec
import tv.cleanyour.core.session.TvSession
import tv.cleanyour.core.engine.RunState

/** Local-first appointments; only agenda.json changes on the TV. No theme, HOME or package op. */
internal class WidgetAgendaRepository(private val d: ManageDeps, private val scope: CoroutineScope, private val tvId: TvId,
    private val changed: (List<AgendaEntry>, AgendaSync) -> Unit) {
    private val mutex = Mutex()
    private var sending: Job? = null
    private var entries = emptyList<AgendaEntry>()
    private val path = "tvs/${tvId.value}/agenda.json"
    private val sentPath = "tvs/${tvId.value}/agenda-sent.sha256"
    fun load() { scope.launch {
        mutex.withLock { entries = d.documents.read(path) { LauncherinAgenda.decode(it.toString(Charsets.UTF_8)) }.orEmpty() }
        changed(entries, if (d.documents.read(path) { it } == null) AgendaSync.READY else AgendaSync.PENDING)
        sync(false)
    } }
    fun upsert(value: AgendaEntry) = edit { old ->
        require(old.any { it.id==value.id } || old.size < LauncherinAgenda.MAX)
        val cleaned = LauncherinAgenda.clean(listOf(value)).single()
        LauncherinAgenda.clean(old.filterNot { it.id == cleaned.id } + cleaned)
    }
    fun remove(id: String) = edit { old -> old.filterNot { it.id==id } }
    private fun edit(edit: (List<AgendaEntry>) -> List<AgendaEntry>) { scope.launch {
        attempt { mutex.withLock {
            val old = d.documents.read(path) { LauncherinAgenda.decode(it.toString(Charsets.UTF_8)) }.orEmpty()
            val next = edit(old); d.documents.write(path, LauncherinAgenda.encode(next).toByteArray()); entries=next
        } }.onSuccess { changed(entries,AgendaSync.PENDING); sync(true) }.onFailure { changed(entries,AgendaSync.FAILED) }
    } }
    fun sync(ask: Boolean) {
        val active = sending
        if (active?.isActive == true) {
            // A save may finish while the previous send is doing its final disk read.
            // Recheck after that job, even if it had already captured the older bytes.
            scope.launch { active.join(); sync(ask) }
            return
        }
        sending = scope.launch {
            while (true) {
                val bytes=d.documents.read(path) { it } ?: return@launch
                val hash=sha256Of(bytes).hex
                if (d.documents.read(sentPath) { it.toString(Charsets.UTF_8) } == hash) { changed(entries,AgendaSync.SYNCED); return@launch }
                val tv=d.tv(tvId) ?: return@launch
                val snapshotVersion=d.snapshots.latest(tvId)?.packages?.get(PackageName("app.launcherin.tv"))?.versionCode ?: 0
                if (maxOf(tv.launcher?.versionCode ?: 0,snapshotVersion)<18) { changed(entries,AgendaSync.NEEDS_UPDATE); return@launch }
                val session=d.access.live(tvId) ?: run { changed(entries,AgendaSync.PENDING); return@launch }
                val running=d.engine.runState(tvId).value
                if (running !is RunState.Idle && running !is RunState.Finished) { changed(entries,AgendaSync.PENDING); return@launch }
                if (d.gate.cached(tvId)==null && (!ask || d.gate.pass(tvId,tv.nickname)==null)) { changed(entries,AgendaSync.NEEDS_UNLOCK); return@launch }
                changed(entries,AgendaSync.SENDING)
                val ok=attempt { session.hold().use { WidgetAgendaTransport.send(session,d.blobs.putBytes(bytes)) } }.getOrDefault(false)
                if(!ok) { changed(entries,AgendaSync.FAILED); return@launch }
                d.documents.write(sentPath,hash.toByteArray())
                // An edit during transmission stays pending, then the newest local document is sent.
                if(d.documents.read(path) { sha256Of(it).hex }==hash) { changed(entries,AgendaSync.SYNCED); return@launch }
            }
        }
    }
}

internal object WidgetAgendaTransport {
    private val renderer=ShellRenderer()
    suspend fun send(session: TvSession, data: BlobRef): Boolean {
        require(data.size in 1..32768)
        val pending=RemotePath(RemoteDir.OVERLAY,listOf("agenda.pending.json"))
        val mkdir=renderer.render(Op.Mkdir(RemoteDir.TMP))
        if(session.exec(mkdir.lines.single()).exit!=0)return false
        val rendered=renderer.render(Op.PushAsset(data,pending,data.sha256,FileRoute.THEME_PROVIDER))
        val transfer=rendered.transfer as Transfer.Push
        session.push(transfer.src,transfer.dest)
        for(line in rendered.lines) if(session.exec(line).exit!=0)return false
        val result=session.exec(WidgetDataLines.commitAgenda(),ExecSpec(timeoutMs=15_000))
        if(result.exit!=0 || result.truncated || !result.stdout.contains("ok=true"))return false
        val read=renderer.render(Op.Read(ProbeKind.LAUNCHER_FILE_SHA256,ReadArg.Path(RemotePath(RemoteDir.OVERLAY,listOf("agenda.json")))))
        val verified=session.exec(read.lines.single())
        return verified.exit==0 && !verified.truncated && verified.stdout.trim().substringBefore(' ')==data.sha256.hex
    }
}
