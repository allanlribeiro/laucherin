package tv.cleanyour.core.flows

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import tv.cleanyour.core.engine.RunStatus
import tv.cleanyour.core.model.LauncherState
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.ops.ShellRenderer
import tv.cleanyour.core.spec.StyleId
import tv.cleanyour.testing.fixtures.FakeTvSession
import tv.cleanyour.testing.fixtures.Samples
import kotlin.test.*

class HomeTestsTest {
    @Test fun previewOnlyStartsFixedActivitiesAndRequiresAnAcknowledgment() = runTest {
        val session = FakeTvSession()
        for (test in HomeTest.entries) {
            val line = HomeTests.line(test).text
            session.recording.on(line, "Status: ok\n")
            assertTrue(HomeTests.run(session, test))
            session.recording.on(line, "Error: Activity class does not exist.")
            assertFalse(HomeTests.run(session, test))
        }
        assertEquals(4, session.execLines.size)
        assertTrue(session.execLines.all { "'am' 'start' '-W'" in it })
        assertTrue(session.execLines.any { "'android.settings.DREAM_SETTINGS'" in it })
        assertTrue(session.execLines.none { "Somnambulator" in it })
        assertTrue(session.execLines.none { "force-stop" in it || "settings put" in it || "keyevent" in it })
    }

    @Test fun returningToTheHubPreservesPendingCityAndDoesNotWriteAnything() = runTest {
        val h = Harness()
        val tv = h.knownTv().copy(launcher = LauncherState(17, Samples.zeroHash, Samples.zeroHash, Samples.zeroHash))
        h.tvs.upsert(tv)
        HomePrefs(h.documents).save(tv.id, HomeChoice(emptyList(), null, style = StyleId.GLASS.id, picture = LookPicture.NONE_ID))
        val home = HomeScreenMachine(h.manageDeps(encoder = ConfigEncoder.Codec), backgroundScope, tv.id)
        settle()
        home.dispatch(HomeScreenIntent.OpenLook)
        home.dispatch(HomeScreenIntent.Look(LookEdit.SetWeatherCityQuery("Serra, ES")))
        assertTrue(assertNotNull(home.state.value.look).canSend)
        val planCount = h.planner.requests.size
        home.dispatch(HomeScreenIntent.Resumed)
        settle()
        assertEquals("Serra, ES", home.state.value.look?.details?.weatherCityQuery)
        assertEquals(planCount, h.planner.requests.size)
        home.dispatch(HomeScreenIntent.TestOnTv(HomeTest.LAUNCHER))
        assertNull(home.state.value.testing, "Do not test stale TV settings while changes are pending")
    }

    @Test fun aRolledBackSendDoesNotConfirmOrReplaceTheSavedChoiceAndKeepsTheDraft() = runTest {
        val h = Harness()
        val tv = h.knownTv().copy(launcher = LauncherState(17, Samples.zeroHash, Samples.zeroHash, Samples.zeroHash))
        h.tvs.upsert(tv)
        val saved = HomeChoice(emptyList(), null, style = StyleId.GLASS.id, picture = LookPicture.NONE_ID)
        HomePrefs(h.documents).save(tv.id, saved)
        val home = HomeScreenMachine(h.manageDeps(encoder = ConfigEncoder.Codec), backgroundScope, tv.id)
        val effects = mutableListOf<ManageEffect>()
        backgroundScope.launch { home.effects.collect(effects::add) }
        settle()
        home.dispatch(HomeScreenIntent.OpenLook)
        home.dispatch(HomeScreenIntent.Look(LookEdit.SetWeatherCityQuery("Serra, ES")))
        h.engine.onStart = { finish(it, RunStatus.ROLLED_BACK) }
        home.dispatch(HomeScreenIntent.UseLook)
        settle()
        assertTrue(home.state.value.lookFailed)
        assertEquals(saved, HomePrefs(h.documents).load(tv.id))
        assertTrue(effects.filterIsInstance<ManageEffect.Snackbar>().none { it.kind == SnackKind.HOME_SAVED })
        home.dispatch(HomeScreenIntent.OpenLook)
        assertEquals("Serra, ES", home.state.value.look?.details?.weatherCityQuery)
    }
}
