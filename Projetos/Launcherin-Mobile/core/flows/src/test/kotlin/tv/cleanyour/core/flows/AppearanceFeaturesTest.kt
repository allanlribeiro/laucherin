package tv.cleanyour.core.flows

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import tv.cleanyour.core.model.*
import tv.cleanyour.core.spec.*
import tv.cleanyour.core.session.ExecResult
import tv.cleanyour.testing.fixtures.*
import kotlin.test.*

class AppearanceFeaturesTest {
 @Test fun appearancePayloadContainsNoLayoutOrTimerSettingsAndKeepsPersonalPhoto(){
  val o=AppearanceOptions(theme="aurora",personalSha="a".repeat(64),wallpaperMode="personal",apps="glass")
  val changed=o.copy(theme="horizon",family="ambient_dynamic")
  assertEquals(o.personalSha,AppearanceOptions.read(changed.json()).personalSha);assertEquals("personal",AppearanceOptions.read(changed.json()).wallpaperMode)
  assertTrue(changed.json().keys.none {it in listOf("dock","dashboard_widgets","idle_standby_minutes","home_key")});assertFails {AppearanceOptions.read(buildJsonObject {put("dock","bad")})}
  assertFails {AppearanceOptions.read(o.copy(family="unknown").json())};assertFails {AppearanceOptions.read(o.copy(personalSha="../../file").json())}
 }
 @Test fun bundleJsonParserHandlesNestedObjectsAndEscapedBraces(){
  val json="""{"theme":"aurora","note":"quote \" and }","nested":{"v":1}}"""
  val parsed=AppearanceTransport.json("Result: Bundle[{ok=true, json=$json, version=26}]")
  assertEquals("aurora",parsed["theme"]!!.jsonPrimitive.content);assertEquals(1,parsed["nested"]!!.jsonObject["v"]!!.jsonPrimitive.int)
  assertFails {AppearanceTransport.json("Bundle[{ok=true}]")}
 }
 private fun Harness.appearanceResponses(version:Int=26,corrupt:Boolean=false,commitOk:Boolean=true){
  session.recording.fallback={line->val output=when {
   "--method appearanceStatus" in line->"Result: Bundle[{ok=true, version=$version, json={}}]"
   "--method commitAppearance" in line->"Result: Bundle[{ok=$commitOk}]"
   "sha256sum" in line->{val name=Regex("(?:av-[a-f0-9]{40}\\.jpg|appearance\\.pending\\.json)").find(line)?.value.orEmpty();if(corrupt)"0".repeat(64)+"  -" else session.files[RemoteDir.TMP.path+"/theme-"+name]?.let {sha256Of(it).hex+"  -"}.orEmpty()}
   else->""
  };ExecResult(0,output,"",false,1)}
 }
 @Test fun changingStylePushesOnlyAppearanceAndNeverRestartsHome()=runTest {
  val h=Harness();val tv=h.knownTv();h.appearanceResponses();var state=AppearanceState();val repo=AppearanceRepository(h.manageDeps(),backgroundScope,tv.id){state=it}
  repo.event(AppearanceIntent.Refresh);settle();assertTrue(state.compatible);repo.event(AppearanceIntent.Edit(state.options.copy(theme="aurora",family="glass_premium")));assertTrue(state.dirty)
  repo.event(AppearanceIntent.Apply);settle();assertNull(state.error);assertFalse(state.busy);assertFalse(state.dirty);assertEquals("aurora",state.saved.theme)
  assertTrue(h.session.execLines.any {"commitAppearance" in it});assertTrue(h.session.execLines.none {"force-stop" in it||"am start" in it||"settings put" in it});assertTrue(h.session.files.keys.none {it.endsWith("theme-config.json")})
 }
 @Test fun invalidHashStopsBeforeCommitAndKeepsTheUnappliedDraft()=runTest{
  val h=Harness();val tv=h.knownTv();h.appearanceResponses(corrupt=true);var state=AppearanceState();val repo=AppearanceRepository(h.manageDeps(),backgroundScope,tv.id){state=it};repo.event(AppearanceIntent.Refresh);settle();repo.event(AppearanceIntent.Edit(state.options.copy(theme="ruska")));repo.event(AppearanceIntent.Apply);settle();assertNotNull(state.error);assertEquals("launcherin",state.saved.theme);assertEquals("ruska",state.options.theme);assertTrue(h.session.execLines.none {"commitAppearance" in it})
 }
 @Test fun commitFailureDoesNotMarkDraftAsApplied()=runTest{
  val h=Harness();val tv=h.knownTv();h.appearanceResponses(commitOk=false);var state=AppearanceState();val repo=AppearanceRepository(h.manageDeps(),backgroundScope,tv.id){state=it};repo.event(AppearanceIntent.Refresh);settle();repo.event(AppearanceIntent.Edit(state.options.copy(family="minimal_cinematic")));repo.event(AppearanceIntent.Apply);settle();assertNotNull(state.error);assertEquals("glass_premium",state.saved.family);assertTrue(state.dirty)
 }
 @Test fun olderTvMustBeUpdatedBeforeAppearanceCanBeApplied()=runTest{
  val h=Harness();val tv=h.knownTv();h.appearanceResponses(version=25);var state=AppearanceState();val repo=AppearanceRepository(h.manageDeps(),backgroundScope,tv.id){state=it};repo.event(AppearanceIntent.Refresh);settle();assertFalse(state.compatible);assertFalse(state.busy);assertNotNull(state.error);assertTrue(h.session.execLines.none {"commitAppearance" in it})
 }
 @Test fun fixedProviderRouteAcceptsOnlyHashedJpegsAndThePendingAppearanceFile(){
  assertTrue(tv.cleanyour.core.ops.ThemeProviderLines.isThemeFile(RemotePath(RemoteDir.OVERLAY,listOf("appearance.pending.json"))))
  assertTrue(tv.cleanyour.core.ops.ThemeProviderLines.isThemeFile(RemotePath(RemoteDir.OVERLAY,listOf("av-"+"a".repeat(40)+".jpg"))))
  assertFalse(tv.cleanyour.core.ops.ThemeProviderLines.isThemeFile(RemotePath(RemoteDir.OVERLAY,listOf("av-any.apk"))))
 }
}
