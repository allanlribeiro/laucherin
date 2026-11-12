package tv.cleanyour.core.flows
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import tv.cleanyour.core.model.*
import tv.cleanyour.core.ops.ThemeProviderLines
import tv.cleanyour.core.spec.MusicVisualOptions
import tv.cleanyour.core.session.ExecResult
import tv.cleanyour.testing.fixtures.*
import kotlin.test.*
class MusicVisualFeaturesTest {
 @Test fun optionsAreVisualOnlyAndRejectUnknownCommandsBadTypesAndInvalidTimes(){val o=MusicVisualOptions();assertEquals(5,o.dimMinutes);assertEquals(15,o.hideMinutes);assertEquals(30,o.minimalMinutes);assertFalse(o.ambient);assertTrue(o.smart);assertFalse(MusicVisualOptions.read(o.copy(smart=false).json()).smart);assertEquals(o,MusicVisualOptions.read(o.json()));for(data in listOf("""{"command":"reboot"}""","""{"enabled":"true"}""","""{"hide_minutes":2}""","""{"intensity":101}"""))assertFails{MusicVisualOptions.read(Json.parseToJsonElement(data).jsonObject)};assertTrue(o.json().keys.none{it in listOf("dock","home_key","idle_standby_minutes","lyrics")})}
 private fun Harness.responses(corrupt:Boolean=false,commitOk:Boolean=true,schema:Int=1){session.recording.fallback={line->val output=when{
  "--method musicVisualStatus" in line->"""Result: Bundle[{ok=true, schema=$schema, json={"profile":"default","profile_name":"Perfil de teste","options":{}}}]"""
  "--method commitMusicVisual" in line->"Result: Bundle[{ok=$commitOk}]"
  "sha256sum" in line->if(corrupt)"0".repeat(64)+"  -" else session.files[RemoteDir.TMP.path+"/theme-music-visual.pending.json"]?.let{sha256Of(it).hex+"  -"}.orEmpty()
  else->""
 };ExecResult(0,output,"",false,1)}}
 @Test fun applyingUsesVerifiedPairedTransportAndDoesNotRestartOrChangeHome()=runTest{val h=Harness();val tv=h.knownTv();h.responses();var state=MusicVisualState();val r=MusicVisualRepository(h.manageDeps(),backgroundScope,tv.id){state=it};r.event(MusicVisualIntent.Refresh);settle();assertTrue(state.compatible);assertEquals("default",state.profile);r.event(MusicVisualIntent.Edit(state.options.copy(enabled=true,palette="violet")));r.event(MusicVisualIntent.Apply);settle();assertNull(state.error);assertTrue(state.saved.enabled);assertFalse(state.dirty);assertTrue(h.session.execLines.any{"commitMusicVisual" in it});assertTrue(h.session.execLines.none{"force-stop" in it||"am start" in it||"settings put" in it});val bytes=h.session.files[RemoteDir.TMP.path+"/theme-music-visual.pending.json"];assertNotNull(bytes);val json=Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject;assertEquals("default",json.getValue("profile").jsonPrimitive.content);assertTrue(json.getValue("options").jsonObject.getValue("enabled").jsonPrimitive.boolean)}
 @Test fun invalidHashPreventsCommitAndKeepsDraft()=runTest{val h=Harness();val tv=h.knownTv();h.responses(corrupt=true);var state=MusicVisualState();val r=MusicVisualRepository(h.manageDeps(),backgroundScope,tv.id){state=it};r.event(MusicVisualIntent.Refresh);settle();r.event(MusicVisualIntent.Edit(state.options.copy(enabled=true)));r.event(MusicVisualIntent.Apply);settle();assertNotNull(state.error);assertFalse(state.saved.enabled);assertTrue(state.dirty);assertTrue(h.session.execLines.none{"commitMusicVisual" in it})}
 @Test fun profileChangeOrCommitFailureDoesNotMarkOptionsAsSaved()=runTest{val h=Harness();val tv=h.knownTv();h.responses(commitOk=false);var state=MusicVisualState();val r=MusicVisualRepository(h.manageDeps(),backgroundScope,tv.id){state=it};r.event(MusicVisualIntent.Refresh);settle();r.event(MusicVisualIntent.Edit(state.options.copy(enabled=true)));r.event(MusicVisualIntent.Apply);settle();assertNotNull(state.error);assertTrue(state.error!!.contains("perfil"));assertFalse(state.saved.enabled);assertTrue(state.dirty)}
 @Test fun incompatibleProtocolDisablesApply()=runTest{val h=Harness();val tv=h.knownTv();h.responses(schema=9);var state=MusicVisualState();val r=MusicVisualRepository(h.manageDeps(),backgroundScope,tv.id){state=it};r.event(MusicVisualIntent.Refresh);settle();assertFalse(state.compatible);assertNotNull(state.error)}
 @Test fun providerAllowsOnlyFixedPendingFileAndNoExecutable(){assertTrue(ThemeProviderLines.isThemeFile(RemotePath(RemoteDir.OVERLAY,listOf("music-visual.pending.json"))));assertFalse(ThemeProviderLines.isThemeFile(RemotePath(RemoteDir.OVERLAY,listOf("music-visual.apk"))));assertFalse(ThemeProviderLines.isThemeFile(RemotePath(RemoteDir.OVERLAY,listOf("music-visual.pending.json","child"))))}
}
