package tv.cleanyour.feature.launcher

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.cleanyour.core.flows.*
import tv.cleanyour.ui.design.component.*
import tv.cleanyour.ui.design.theme.*
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[35],qualifiers="pt-rBR-w360dp-h2400dp-xhdpi")
class WidgetPagesTest {
    @get:Rule val compose=createComposeRule()
    @Composable private fun Hub(state:HomeScreenState=HomeScreenState(tvName="TCL · Sala",homeScreenOurs=true,launcherVersionCode=18),onIntent:(HomeScreenIntent)->Unit){
        LauncherinHubScreen(state,"TCL · Sala",true,false,listOf("tv" to "TCL · Sala"),"tv",{},{},{},{},{},{},{},{},onIntent,initialTab=HubTab.WIDGETS)
    }
    @Test fun agendaSaveDispatchesOnlyAnAppointmentAndDoesNotApplyTheWholeLook(){
        val intents=mutableListOf<HomeScreenIntent>()
        compose.setContent { CytTheme(darkTheme=true,reducedMotion=true) { Hub(onIntent=intents::add) } }
        compose.onNodeWithText("Agenda do dia").performScrollTo().performClick()
        compose.onNodeWithText("Adicionar compromisso").performClick()
        compose.onAllNodes(hasSetTextAction()).onLast().performTextInput("Consulta de teste")
        compose.onNodeWithText("Salvar compromisso").performClick()
        assertEquals("Consulta de teste",intents.filterIsInstance<HomeScreenIntent.UpsertAgenda>().single().entry.title)
        assertTrue(intents.none { it==HomeScreenIntent.OpenLook||it==HomeScreenIntent.UseLook||it is HomeScreenIntent.SetTheme })
    }
    @Test fun watchModeIsChosenWithoutAnImmediateTvWrite(){
        val intents=mutableListOf<HomeScreenIntent>();val state=HomeScreenState(tvName="TCL · Sala",homeScreenOurs=true,launcherVersionCode=18,look=LookDraft("default",null))
        compose.setContent { CytTheme(darkTheme=true,reducedMotion=true) { Hub(state,intents::add) } }
        compose.onNodeWithText("Continuar assistindo").performScrollTo().performClick()
        compose.onNodeWithText("Abaixo dos apps, ao apertar ↓").performScrollTo().performClick()
        assertTrue(intents.any { it is HomeScreenIntent.Look && it.edit==LookEdit.SetWatchNextMode("below") })
        assertTrue(intents.none { it==HomeScreenIntent.UseLook })
    }
    @Test fun allWidgetsCanBeHiddenAndOneCanBeReenabledAndMoved(){
        var slots by mutableStateOf(LauncherinWidgets.defaults())
        compose.setContent { CytTheme(darkTheme=true,reducedMotion=true) { ScreenScaffold(Glow.Tv){LauncherinWidgetEditor(slots){slots=it}} } }
        for(name in listOf("Relógio","Tempo","Música","Esportes")){
            compose.onAllNodesWithText(if(name=="Relógio")"✓ Relógio" else name).onLast().performScrollTo().performClick()
            compose.onNode(isToggleable()).performScrollTo().performClick()
        }
        assertTrue(slots.none { it.enabled })
        compose.onNodeWithText("Nenhum widget visível").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sistema").performScrollTo().performClick()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithContentDescription("Mover para cima").performScrollTo().performClick()
        assertTrue(slots.single { it.id=="system" }.enabled);assertEquals(0,slots.single { it.id=="system" }.y)
    }
    @Test fun agendaScreenshot()=compose.shoot(Shot("launcherin_agenda_preview10"){
        ScreenScaffold(Glow.Tv){ScreenTitle("Agenda do dia");LauncherinAgendaEditor(listOf(AgendaEntry("1","Consulta de exemplo","2026-11-05","14:30"),AgendaEntry("2","Jantar em família","2026-11-05","19:00")),AgendaSync.SYNCED,{},{},{})}
    },Variant.DARK)
    @Test fun layoutScreenshot()=compose.shoot(Shot("launcherin_widgets_editor_preview10"){
        ScreenScaffold(Glow.Tv){ScreenTitle("Organizar widgets");LauncherinWidgetEditor(LauncherinWidgets.defaults()){} }
    },Variant.DARK)
    @Test fun watchingOptionsScreenshot()=compose.shoot(Shot("launcherin_watch_options_preview10"){
        ScreenScaffold(Glow.Tv){ScreenTitle("Continuar assistindo");LauncherinSettingsPanel(LauncherinPanel.WATCH,LookDetails(watchNextMode="below")){} }
    },Variant.DARK)
}
