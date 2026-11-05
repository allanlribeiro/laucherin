package tv.cleanyour.feature.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.cleanyour.core.flows.HomeScreenIntent
import tv.cleanyour.core.flows.HomeScreenState
import tv.cleanyour.core.flows.LookDraft
import tv.cleanyour.core.flows.LookDetails
import tv.cleanyour.ui.design.theme.CytTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "pt-rBR-w360dp-h2400dp-xhdpi")
class LauncherinHubTest {
    @get:Rule val compose = createComposeRule()

    @Composable
    private fun Hub(tab: HubTab = HubTab.HOME, state: HomeScreenState = sample(), onExit: () -> Unit = {}, onSettings: () -> Unit = {}, onIntent: (HomeScreenIntent) -> Unit = {}) {
        LauncherinHubScreen(state, "TCL · Sala", true, false, listOf("tv" to "TCL · Sala"), "tv",
            {}, {}, {}, {}, {}, {}, onSettings, onExit, onIntent, initialTab = tab)
    }

    @Test fun homeScreenshot() = compose.shoot(Shot("launcherin_hub_inicio") { Hub() }, Variant.DARK)
    @Test fun personalizeScreenshot() = compose.shoot(Shot("launcherin_hub_personalizar") { Hub(HubTab.PERSONALIZE) }, Variant.DARK)
    @Test fun widgetsScreenshot() = compose.shoot(Shot("launcherin_hub_widgets") { Hub(HubTab.WIDGETS) }, Variant.DARK)
    @Test fun tvScreenshot() = compose.shoot(Shot("launcherin_hub_tv") { Hub(HubTab.TV) }, Variant.DARK)
    @Test fun largeTextScreenshot() = compose.shoot(Shot("launcherin_hub_fonte_grande") { Hub(HubTab.WIDGETS) }, Variant.FONT200)

    @Test fun searchFindsSettingsAcrossTabsIgnoringAccents() {
        compose.setContent { CytTheme(darkTheme = true, reducedMotion = true) { Hub() } }
        compose.onNode(hasSetTextAction()).performTextInput("futebol")
        compose.onNodeWithText("Esportes").assertIsDisplayed().performClick()
        compose.onAllNodesWithText("Até dois times favoritos e avisos de gol.").onLast().assertIsDisplayed()
        compose.onNodeWithText("Voltar às configurações").assertIsDisplayed()
    }

    @Test fun changingTabsKeepsUnsentDraftAndLeavingAsksBeforeDiscard() {
        var navigations = 0
        val intents = mutableListOf<HomeScreenIntent>()
        val state = sample().copy(look = LookDraft("default", null, changed = true,
            details = LookDetails(weatherCityQuery = "Serra, ES")))
        compose.setContent { CytTheme(darkTheme = true, reducedMotion = true) { Hub(state = state, onSettings = { navigations++ }, onIntent = intents::add) } }
        compose.onNodeWithText("Widgets", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Aplicar na TV").assertIsDisplayed()
        assertTrue(intents.all { it is HomeScreenIntent.SyncAgenda })
        compose.onNodeWithContentDescription("Configurações do aplicativo").performClick()
        compose.onNodeWithText("Alterações ainda não enviadas").assertIsDisplayed()
        assertEquals(0, navigations)
        compose.onNodeWithText("Continuar editando").performClick()
        assertEquals(0, navigations)
        assertTrue(intents.all { it is HomeScreenIntent.SyncAgenda })
        compose.onNodeWithContentDescription("Configurações do aplicativo").performClick()
        compose.onNodeWithText("Descartar alterações e continuar").performClick()
        assertEquals(listOf<HomeScreenIntent>(HomeScreenIntent.CloseLook), intents.filterNot { it is HomeScreenIntent.SyncAgenda })
        assertEquals(1, navigations)
    }

    companion object {
        private fun sample() = HomeScreenState(tvName = "TCL · Sala", homeScreenOurs = true, launcherVersionCode = 18)
    }
}
