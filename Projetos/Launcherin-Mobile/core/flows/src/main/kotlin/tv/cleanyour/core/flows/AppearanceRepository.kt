package tv.cleanyour.core.flows
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import tv.cleanyour.core.model.*
import tv.cleanyour.core.ops.*
import tv.cleanyour.core.session.*
import tv.cleanyour.core.engine.RunState
import tv.cleanyour.core.spec.*
import java.util.concurrent.ConcurrentHashMap

data class AppearanceState(val options:AppearanceOptions=AppearanceOptions(),val saved:AppearanceOptions=AppearanceOptions(),val compatible:Boolean=false,val busy:Boolean=false,val error:String?=null,val photo:BlobRef?=null){val dirty:Boolean get()=options!=saved||photo!=null}
sealed interface AppearanceIntent {
 data object Refresh:AppearanceIntent
 data class Edit(val options:AppearanceOptions):AppearanceIntent
 data object Reset:AppearanceIntent
 class Photo(val jpeg:ByteArray,val dim:Int=10):AppearanceIntent
 data object Apply:AppearanceIntent
}
internal class AppearanceRepository(private val d:ManageDeps,private val scope:CoroutineScope,private val tvId:TvId,private val changed:(AppearanceState)->Unit){
 private var state=AppearanceState();private val mutex=locks.computeIfAbsent(tvId.value){Mutex()}
 private fun update(n:AppearanceState){state=n;changed(n)}
 fun event(event:AppearanceIntent){if(state.busy)return;when(event){
  AppearanceIntent.Refresh->{update(state.copy(busy=true));scope.launch {mutex.withLock {refresh()}}}
  is AppearanceIntent.Edit->update(state.copy(options=AppearanceOptions.read(event.options.json()),error=null))
  AppearanceIntent.Reset->update(state.copy(options=state.options.copy(family="glass_premium",apps="premium",intensity=45,autoPalette=false,reduced=false),error=null))
  is AppearanceIntent.Photo->{update(state.copy(busy=true));scope.launch {attempt {OwnPicture.stage(d.blobs,event.jpeg)}.onSuccess {photo->update(state.copy(busy=false,photo=photo,options=state.options.copy(personalSha=photo.sha256.hex,wallpaperMode="personal",dim=event.dim.coerceIn(0,90)),error=null))}.onFailure {update(state.copy(busy=false,error="Não foi possível preparar a imagem."))}}}
  AppearanceIntent.Apply->{val wanted=state;update(state.copy(busy=true,error=null));scope.launch {apply(wanted)}}
 }}
 private suspend fun refresh(){val session=d.access.live(tvId)?:return update(state.copy(busy=false,error="Conecte o celular à TV."));attempt {
  val r=session.exec(AppearanceLines.status(),ExecSpec(timeoutMs=15000));check(r.exit==0&&!r.truncated);check((Regex("version=(\\d+)").find(r.stdout)?.groupValues?.get(1)?.toIntOrNull()?:0)>=26)
  AppearanceOptions.read(AppearanceTransport.json(r.stdout))
 }.onSuccess {v->update(state.copy(saved=v,options=if(state.dirty)state.options else v,compatible=true,busy=false,error=null))}.onFailure {update(state.copy(compatible=false,busy=false,error="Atualize a tela inicial da TV para usar as novas opções de aparência."))}}
 private suspend fun materialize(ref:BlobRef):BlobRef=if(ref.path.startsWith(BundledThemeSource.PREFIX))d.blobs.putBytes(BundledThemeSource.bytes(ref)?:error("Imagem original indisponível.")) else d.blobs.get(ref.sha256)?:ref
 private suspend fun apply(wanted:AppearanceState)=mutex.withLock {attempt {
  val tv=d.tv(tvId)?:error("TV não encontrada.");val running=d.engine.runState(tvId).value;check(running is RunState.Idle||running is RunState.Finished){"Aguarde a operação em andamento."}
  if(d.gate.cached(tvId)==null)check(d.gate.pass(tvId,tv.nickname)!=null){"Desbloqueie o aplicativo para aplicar."}
  val session=d.access.live(tvId)?:error("Conecte o celular à TV.");var o=wanted.options
  session.hold().use {
   check(wanted.compatible){"Atualize a tela inicial da TV."}
   wanted.photo?.let {AppearanceTransport.push(session,it,"av-${it.sha256.hex.take(40)}.jpg")}
   if(o.wallpaperMode=="theme"){
    val original=d.themes.bundled().firstOrNull {it.id==o.theme}?.wallpaper
    o=o.copy(originalSha=original?.sha256?.hex?:"");original?.let {val ref=materialize(it);AppearanceTransport.push(session,ref,"av-${ref.sha256.hex.take(40)}.jpg")}
   }
   if(o.wallpaperMode=="dynamic"){
    check(o.dynamic.isNotEmpty()){"Escolha ao menos uma imagem para a rotação."}
    for(hash in o.dynamic){val ref=d.pictures.pictures().firstOrNull {it.wallpaper.sha256.hex==hash}?.wallpaper?.let {materialize(it)}?:d.blobs.get(Sha256(hash))?:error("Imagem da coleção indisponível.");AppearanceTransport.push(session,ref,"av-${hash.take(40)}.jpg")}
   }
   AppearanceTransport.commit(session,d.blobs.putBytes(o.json().toString().toByteArray()))
  }
  attempt {d.documents.write("tvs/${tvId.value}/appearance.json",o.json().toString().toByteArray())};update(state.copy(options=o,saved=o,busy=false,error=null,photo=null))
 }.onFailure {update(state.copy(busy=false,error=it.message?:"A aparência anterior foi mantida."))} }
 companion object {private val locks=ConcurrentHashMap<String,Mutex>()}
}
internal object AppearanceTransport {
 private val renderer=ShellRenderer()
 suspend fun push(session:TvSession,blob:BlobRef,name:String){require(name=="appearance.pending.json"||Regex("av-[a-f0-9]{40}\\.jpg").matches(name));require(blob.size in 1..2_000_000)
  val path=RemotePath(RemoteDir.OVERLAY,listOf(name));val mkdir=renderer.render(Op.Mkdir(RemoteDir.TMP));check(session.exec(mkdir.lines.single()).exit==0);val op=renderer.render(Op.PushAsset(blob,path,blob.sha256,FileRoute.THEME_PROVIDER));val t=op.transfer as Transfer.Push;session.push(t.src,t.dest);for(line in op.lines)check(session.exec(line).exit==0)
  val check=renderer.render(Op.Read(ProbeKind.LAUNCHER_FILE_SHA256,ReadArg.Path(path)));val read=session.exec(check.lines.single());check(read.exit==0&&!read.truncated&&read.stdout.trim().substringBefore(' ')==blob.sha256.hex){"A TV não confirmou a integridade do arquivo."}
 }
 suspend fun commit(session:TvSession,data:BlobRef){push(session,data,"appearance.pending.json");val r=session.exec(AppearanceLines.commit(),ExecSpec(timeoutMs=30000));check(r.exit==0&&!r.truncated&&r.stdout.contains("ok=true")){"Não foi possível aplicar. A aparência anterior foi mantida."}}
 fun json(text:String):JsonObject{val index=text.indexOf("json=");require(index>=0);val start=text.indexOf('{',index);require(start>=0);var depth=0;var quote=false;var escaped=false
  for(i in start until text.length){val c=text[i];if(quote){if(escaped)escaped=false else if(c=='\\')escaped=true else if(c=='\"')quote=false}else when(c){'\"'->quote=true;'{'->depth++;'}'->{depth--;if(depth==0)return Json.parseToJsonElement(text.substring(start,i+1)).jsonObject}}};error("Resposta incompleta da TV")
 }
}
