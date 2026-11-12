package tv.cleanyour.core.flows
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import tv.cleanyour.core.model.*
import tv.cleanyour.core.ops.*
import tv.cleanyour.core.session.*
import tv.cleanyour.core.engine.RunState
import tv.cleanyour.core.spec.MusicVisualOptions
import java.util.concurrent.ConcurrentHashMap

data class MusicVisualState(val options:MusicVisualOptions=MusicVisualOptions(),val saved:MusicVisualOptions=MusicVisualOptions(),val profile:String="",val profileName:String="",val compatible:Boolean=false,val busy:Boolean=false,val error:String?=null){val dirty:Boolean get()=options!=saved}
sealed interface MusicVisualIntent {data object Refresh:MusicVisualIntent;data class Edit(val options:MusicVisualOptions):MusicVisualIntent;data object Apply:MusicVisualIntent;data object Reset:MusicVisualIntent}
internal class MusicVisualRepository(private val d:ManageDeps,private val scope:CoroutineScope,private val tvId:TvId,private val changed:(MusicVisualState)->Unit){
 private var state=MusicVisualState();private val mutex=locks.computeIfAbsent(tvId.value){Mutex()};private fun update(n:MusicVisualState){state=n;changed(n)}
 fun event(event:MusicVisualIntent){if(state.busy)return;when(event){
  MusicVisualIntent.Refresh->{update(state.copy(busy=true));scope.launch {mutex.withLock {attempt {val session=d.access.live(tvId)?:error("Conecte o celular à TV.");val r=session.exec(MusicVisualLines.status(),ExecSpec(timeoutMs=15000));check(r.exit==0&&!r.truncated&&r.stdout.contains("schema=1")&&r.stdout.contains("ok=true"));val j=AppearanceTransport.json(r.stdout);val profile=j.getValue("profile").jsonPrimitive.content;check(profile.matches(Regex("default|[a-f0-9]{32}")));Triple(profile,j.getValue("profile_name").jsonPrimitive.content,MusicVisualOptions.read(j.getValue("options").jsonObject))}.onSuccess {(id,name,o)->update(state.copy(options=if(state.dirty&&state.profile==id)state.options else o,saved=o,profile=id,profileName=name,compatible=true,busy=false,error=null))}.onFailure {update(state.copy(compatible=false,busy=false,error="Conecte e atualize a TV para a preview22 ou posterior."))}}}}
  is MusicVisualIntent.Edit->runCatching {MusicVisualOptions.read(event.options.json())}.onSuccess {update(state.copy(options=it,error=null))}.onFailure {update(state.copy(error="Os tempos devem seguir: reduzir < ocultar < minimizar."))}
  MusicVisualIntent.Reset->update(state.copy(options=MusicVisualOptions(),error=null))
  MusicVisualIntent.Apply->{val wanted=state;update(state.copy(busy=true,error=null));scope.launch {mutex.withLock {attempt {check(wanted.compatible&&wanted.profile.isNotBlank());val tv=d.tv(tvId)?:error("TV não encontrada.");val running=d.engine.runState(tvId).value;check(running is RunState.Idle||running is RunState.Finished){"Aguarde a operação em andamento."};if(d.gate.cached(tvId)==null)check(d.gate.pass(tvId,tv.nickname)!=null){"Desbloqueie o aplicativo para aplicar."};val session=d.access.live(tvId)?:error("Conecte o celular à TV.");session.hold().use {val payload=buildJsonObject{put("profile",wanted.profile);put("options",wanted.options.json())};MusicVisualTransport.apply(session,d.blobs.putBytes(payload.toString().toByteArray()))};update(state.copy(saved=wanted.options,options=wanted.options,busy=false,error=null))}.onFailure {update(state.copy(busy=false,error=it.message?:"As opções anteriores foram mantidas."))}}}}
 }}
 companion object {private val locks=ConcurrentHashMap<String,Mutex>()}
}
internal object MusicVisualTransport {
 private val renderer=ShellRenderer()
 suspend fun apply(session:TvSession,blob:BlobRef){require(blob.size in 1..8192);val path=RemotePath(RemoteDir.OVERLAY,listOf("music-visual.pending.json"));check(session.exec(renderer.render(Op.Mkdir(RemoteDir.TMP)).lines.single()).exit==0);val op=renderer.render(Op.PushAsset(blob,path,blob.sha256,FileRoute.THEME_PROVIDER));val t=op.transfer as Transfer.Push;session.push(t.src,t.dest);for(line in op.lines)check(session.exec(line).exit==0);val read=session.exec(renderer.render(Op.Read(ProbeKind.LAUNCHER_FILE_SHA256,ReadArg.Path(path))).lines.single());check(read.exit==0&&!read.truncated&&read.stdout.trim().substringBefore(' ')==blob.sha256.hex){"A TV não confirmou a integridade do arquivo."};val r=session.exec(MusicVisualLines.commit(),ExecSpec(timeoutMs=15000));check(r.exit==0&&!r.truncated&&r.stdout.contains("ok=true")){"O perfil ativo pode ter mudado. Atualize os dados e tente novamente; as opções anteriores foram mantidas."}}
}
