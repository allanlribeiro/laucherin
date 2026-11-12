package tv.cleanyour.core.flows
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import tv.cleanyour.core.model.*
import tv.cleanyour.core.ops.*
import tv.cleanyour.core.session.ExecResult
import tv.cleanyour.core.spec.MusicProfile
import tv.cleanyour.testing.fixtures.*
import kotlin.test.*

class MusicProfilesFeaturesTest {
 private fun Harness.responses(corrupt:Boolean=false,commitOk:Boolean=true,schema:Int=1){
  var profile=buildJsonObject{put("id","default");put("name","Perfil de teste");put("color",-6677791);put("photo","");put("revision","0".repeat(64))}
  session.recording.fallback={line ->
   val output=when{
    "--method musicProfilesStatus" in line->{val j=buildJsonObject{put("active","default");put("profiles",buildJsonArray{add(profile)})};"Result: Bundle[{ok=true, schema=$schema, json=$j}]"}
    "--method commitMusicProfile" in line->{if(commitOk){val data=session.files[RemoteDir.TMP.path+"/theme-music-profile.pending.json"]!!;val j=Json.parseToJsonElement(data.toString(Charsets.UTF_8)).jsonObject;profile=buildJsonObject{put("id",j.getValue("id"));put("name",j.getValue("name"));put("color",-6677791);put("photo",j.getValue("photo"));put("revision",sha256Of(data).hex)}};"Result: Bundle[{ok=$commitOk}]"}
    "sha256sum" in line->{val name=line.substringAfter("content://app.launcherin.tv.theme/").substringBefore(' ');if(corrupt)"0".repeat(64)+"  -" else session.files[RemoteDir.TMP.path+"/theme-"+name]?.let{sha256Of(it).hex+"  -"}.orEmpty()}
    else->""
   };ExecResult(0,output,"",false,1)
  }
 }
 @Test fun namesAndPhotosUseIntegrityCheckedPairedFilesAndNeverChangePlaybackOrHome()=runTest{
  val h=Harness();val tv=h.knownTv();h.responses();var state=MusicProfilesState();val r=MusicProfilesRepository(h.manageDeps(),backgroundScope,tv.id){state=it};r.event(MusicProfilesIntent.Refresh);settle();assertTrue(state.compatible)
  r.event(MusicProfilesIntent.Name("Mia; $(reboot)"));val photo=byteArrayOf(-1,-40,0,1);r.event(MusicProfilesIntent.Photo(photo));r.event(MusicProfilesIntent.Apply);settle();assertNull(state.error);assertFalse(state.dirty);assertEquals("Mia; $(reboot)",state.name);assertEquals(sha256Of(photo).hex,state.photo)
  val payload=h.session.files[RemoteDir.TMP.path+"/theme-music-profile.pending.json"]!!;val j=Json.parseToJsonElement(payload.toString(Charsets.UTF_8)).jsonObject;assertEquals("0".repeat(64),j.getValue("expected").jsonPrimitive.content);assertTrue(h.session.execLines.any{it.contains("--arg "+sha256Of(payload).hex)});assertTrue(h.session.execLines.none{it.contains("force-stop")||it.contains("am start")||it.contains("settings put")||it.contains("Mia;")})
 }
 @Test fun wrongHashPreventsCommitAndKeepsTheUnsavedPhotoForRetry()=runTest{val h=Harness();val tv=h.knownTv();h.responses(corrupt=true);var s=MusicProfilesState();val r=MusicProfilesRepository(h.manageDeps(),backgroundScope,tv.id){s=it};r.event(MusicProfilesIntent.Refresh);settle();r.event(MusicProfilesIntent.Photo(byteArrayOf(-1,-40,2,3)));r.event(MusicProfilesIntent.Apply);settle();assertNotNull(s.error);assertTrue(s.dirty);assertNotNull(s.preview);assertTrue(h.session.execLines.none{"commitMusicProfile" in it})}
 @Test fun rejectedCommitKeepsPriorServerMetadataAndLocalEdits()=runTest{val h=Harness();val tv=h.knownTv();h.responses(commitOk=false);var s=MusicProfilesState();val r=MusicProfilesRepository(h.manageDeps(),backgroundScope,tv.id){s=it};r.event(MusicProfilesIntent.Refresh);settle();r.event(MusicProfilesIntent.Name("Outro nome"));r.event(MusicProfilesIntent.Apply);settle();assertNotNull(s.error);assertEquals("Outro nome",s.name);assertEquals("Perfil de teste",s.saved?.name);assertTrue(s.dirty)}
 @Test fun unsupportedTvCannotApplyAndNewProfileDoesNotSelectItOnTheTv()=runTest{val h=Harness();val tv=h.knownTv();h.responses(schema=2);var s=MusicProfilesState();val r=MusicProfilesRepository(h.manageDeps(),backgroundScope,tv.id){s=it};r.event(MusicProfilesIntent.Refresh);settle();assertFalse(s.compatible);r.event(MusicProfilesIntent.Add);assertTrue(s.selected.isEmpty());assertTrue(h.session.execLines.none{"commitMusicProfile" in it})}
 @Test fun addingAndRemovingPhotoAreDraftOnlyUntilApply()=runTest{val h=Harness();val tv=h.knownTv();h.responses();var s=MusicProfilesState();val r=MusicProfilesRepository(h.manageDeps(),backgroundScope,tv.id){s=it};r.event(MusicProfilesIntent.Refresh);settle();r.event(MusicProfilesIntent.Add);assertTrue(MusicProfile.validId(s.selected));assertEquals("default",s.active);r.event(MusicProfilesIntent.Photo(byteArrayOf(-1,-40,1,2)));assertNotNull(s.preview);r.event(MusicProfilesIntent.RemovePhoto);assertNull(s.preview);assertEquals("",s.photo);assertTrue(h.session.execLines.none{"commitMusicProfile" in it})}
 @Test fun profileModelsAndFilePathsRejectTraversalExecutableAndBadRevision(){assertFails{MusicProfile("../escape","Pessoa",0,"","a".repeat(64))};assertFails{MusicProfile("default","Pessoa",0,"https://remote/image.jpg","a".repeat(64))};assertTrue(ThemeProviderLines.isThemeFile(RemotePath(RemoteDir.OVERLAY,listOf("music-profile.pending.json"))));assertTrue(ThemeProviderLines.isThemeFile(RemotePath(RemoteDir.OVERLAY,listOf("mp-"+"a".repeat(40)+".jpg"))));assertFalse(ThemeProviderLines.isThemeFile(RemotePath(RemoteDir.OVERLAY,listOf("music-profile.apk"))));assertFails{ThemeProviderLines.isThemeFile(RemotePath(RemoteDir.OVERLAY,listOf("mp-"+"a".repeat(64)+".jpg")))}}
}
