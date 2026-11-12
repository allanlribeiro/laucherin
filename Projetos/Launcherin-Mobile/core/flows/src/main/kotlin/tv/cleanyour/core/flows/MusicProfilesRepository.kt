package tv.cleanyour.core.flows
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import kotlinx.serialization.json.*
import tv.cleanyour.core.model.*
import tv.cleanyour.core.ops.*
import tv.cleanyour.core.session.*
import tv.cleanyour.core.engine.RunState
import tv.cleanyour.core.spec.MusicProfile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class MusicProfilesState(val profiles:List<MusicProfile> = emptyList(),val active:String="",val selected:String="",val name:String="",val photo:String="",val preview:ByteArray?=null,val compatible:Boolean=false,val busy:Boolean=false,val error:String?=null){
 val saved:MusicProfile? get()=profiles.firstOrNull{it.id==selected}
 val dirty:Boolean get()=selected.isNotBlank()&&(saved==null||saved?.name!=name.trim()||saved?.photo!=photo)
}
sealed interface MusicProfilesIntent {
 data object Refresh:MusicProfilesIntent
 data class Select(val id:String):MusicProfilesIntent
 data object Add:MusicProfilesIntent
 data class Name(val value:String):MusicProfilesIntent
 class Photo(val jpeg:ByteArray):MusicProfilesIntent
 data object RemovePhoto:MusicProfilesIntent
 data object Apply:MusicProfilesIntent
}
internal class MusicProfilesRepository(private val d:ManageDeps,private val scope:CoroutineScope,private val tvId:TvId,private val changed:(MusicProfilesState)->Unit){
 private var state=MusicProfilesState();private val mutex=locks.computeIfAbsent(tvId.value){Mutex()}
 private fun update(n:MusicProfilesState){state=n;changed(n)}
 private fun path(hash:String)="tvs/${tvId.value}/music-profile-photos/$hash.jpg"
 private suspend fun preview(hash:String):ByteArray?=if(hash.isEmpty())null else d.documents.read(path(hash)){it}?.takeIf{it.size<=180000&&sha256Of(it).hex==hash}
 private suspend fun fetch(session:TvSession):Pair<List<MusicProfile>,String>{
  val r=session.exec(MusicProfileLines.status(),ExecSpec(timeoutMs=15000));check(r.exit==0&&!r.truncated&&r.stdout.contains("schema=1")&&r.stdout.contains("ok=true"));val j=AppearanceTransport.json(r.stdout)
  val all=j.getValue("profiles").jsonArray;require(all.size in 1..8);val profiles=all.map{MusicProfile.read(it.jsonObject)};require(profiles.map{it.id}.distinct().size==profiles.size);val active=j.getValue("active").jsonPrimitive.content;require(profiles.any{it.id==active});return profiles to active
 }
 fun event(e:MusicProfilesIntent){if(state.busy)return;when(e){
  MusicProfilesIntent.Refresh->{val before=state;update(state.copy(busy=true,error=null));scope.launch {mutex.withLock {attempt {val session=d.access.live(tvId)?:error("Conecte o celular à TV.");val (all,active)=fetch(session);val selected=all.firstOrNull{it.id==before.selected}?:all.first{it.id==active};val keep=before.dirty&&before.selected==selected.id;MusicProfilesState(all,active,selected.id,if(keep)before.name else selected.name,if(keep)before.photo else selected.photo,if(keep)before.preview else preview(selected.photo),true)}.onSuccess{update(it)}.onFailure{update(state.copy(busy=false,compatible=false,error="Atualize a TV para a preview24 e conecte o celular."))}}}}
  is MusicProfilesIntent.Select->{val selected=state.profiles.firstOrNull{it.id==e.id}?:return;update(state.copy(busy=true));scope.launch {val bytes=attempt{preview(selected.photo)}.getOrNull();update(state.copy(selected=selected.id,name=selected.name,photo=selected.photo,preview=bytes,busy=false,error=null))}}
  MusicProfilesIntent.Add->{if(!state.compatible||state.profiles.size>=8)return;update(state.copy(selected=UUID.randomUUID().toString().replace("-",""),name="",photo="",preview=null,error=null))}
  is MusicProfilesIntent.Name->update(state.copy(name=e.value.take(40),error=null))
  is MusicProfilesIntent.Photo->{val bytes=e.jpeg.copyOf();if(bytes.size !in 4..180000||bytes[0]!=0xff.toByte()||bytes[1]!=0xd8.toByte()){update(state.copy(error="Escolha uma foto compatível pelo seletor do celular."));return};update(state.copy(photo=sha256Of(bytes).hex,preview=bytes,error=null))}
  MusicProfilesIntent.RemovePhoto->update(state.copy(photo="",preview=null,error=null))
  MusicProfilesIntent.Apply->{val wanted=state;if(!wanted.compatible||!wanted.dirty)return;update(state.copy(busy=true,error=null));scope.launch {mutex.withLock {attempt {
   require(MusicProfile.validId(wanted.selected)&&wanted.name.trim().isNotEmpty()&&wanted.name.trim().length<=40&&wanted.name.none{it<' '}){"Informe um nome de até 40 caracteres."}
   val tv=d.tv(tvId)?:error("TV não encontrada.");val run=d.engine.runState(tvId).value;check(run is RunState.Idle||run is RunState.Finished){"Aguarde a operação em andamento."};if(d.gate.cached(tvId)==null)check(d.gate.pass(tvId,tv.nickname)!=null){"Desbloqueie o aplicativo para aplicar."};val session=d.access.live(tvId)?:error("Conecte o celular à TV.")
   session.hold().use {
    if(wanted.photo.isNotEmpty()&&wanted.photo!=wanted.saved?.photo){val bytes=wanted.preview?:error("Escolha a foto novamente.");require(sha256Of(bytes).hex==wanted.photo&&bytes.size<=180000);MusicProfileTransport.push(session,d.blobs.putBytes(bytes),"mp-${wanted.photo.take(40)}.jpg");d.documents.write(path(wanted.photo),bytes)}
    val payload=buildJsonObject{put("id",wanted.selected);put("name",wanted.name.trim());put("photo",wanted.photo);put("expected",wanted.saved?.revision.orEmpty())};val metadata=d.blobs.putBytes(payload.toString().toByteArray());MusicProfileTransport.push(session,metadata,"music-profile.pending.json")
    val commit=session.exec(MusicProfileLines.commit(metadata.sha256),ExecSpec(timeoutMs=15000));check(commit.exit==0&&!commit.truncated&&commit.stdout.contains("ok=true")){"O perfil mudou ou a foto foi recusada. Atualize os perfis antes de tentar novamente; os dados anteriores foram mantidos."}
    val (all,active)=fetch(session);val saved=all.first{it.id==wanted.selected};check(saved.name==wanted.name.trim()&&saved.photo==wanted.photo){"A TV não confirmou a alteração deste perfil."};update(MusicProfilesState(all,active,saved.id,saved.name,saved.photo,preview(saved.photo),true))
   }
  }.onFailure{update(state.copy(busy=false,error=it.message?:"Não foi possível aplicar. O perfil anterior foi mantido."))}}}}
 }}
 companion object {private val locks=ConcurrentHashMap<String,Mutex>()}
}
internal object MusicProfileTransport {
 private val renderer=ShellRenderer()
 suspend fun push(session:TvSession,blob:BlobRef,name:String){require(name=="music-profile.pending.json"||name.matches(Regex("mp-[a-f0-9]{40}\\.jpg")));require(blob.size in 1..if(name.endsWith("json"))8192 else 180000)
  val path=RemotePath(RemoteDir.OVERLAY,listOf(name));check(session.exec(renderer.render(Op.Mkdir(RemoteDir.TMP)).lines.single()).exit==0);val rendered=renderer.render(Op.PushAsset(blob,path,blob.sha256,FileRoute.THEME_PROVIDER));val transfer=rendered.transfer as Transfer.Push;session.push(transfer.src,transfer.dest);for(line in rendered.lines)check(session.exec(line).exit==0)
  val read=session.exec(renderer.render(Op.Read(ProbeKind.LAUNCHER_FILE_SHA256,ReadArg.Path(path))).lines.single());check(read.exit==0&&!read.truncated&&read.stdout.trim().substringBefore(' ')==blob.sha256.hex){"A integridade da foto não foi confirmada pela TV."}
 }
}
