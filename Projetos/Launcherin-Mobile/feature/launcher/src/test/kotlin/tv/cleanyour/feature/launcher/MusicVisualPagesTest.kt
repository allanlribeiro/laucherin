package tv.cleanyour.feature.launcher
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.cleanyour.core.flows.*
import tv.cleanyour.ui.design.theme.*
import kotlin.test.*
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
@RunWith(RobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE) @Config(sdk=[35],qualifiers="pt-rBR-w400dp-h2200dp-xhdpi")
class MusicVisualPagesTest {
 @get:Rule val compose=createComposeRule()
 private fun show(compatible:Boolean=true):MutableList<HomeScreenIntent>{val sent=mutableListOf<HomeScreenIntent>();compose.setContent{CytTheme(darkTheme=true,reducedMotion=true){MusicVisualPanel(HomeScreenState(tvName="TV de teste",launcherVersionCode=30,musicVisual=MusicVisualState(compatible=compatible,profile="default",profileName="Perfil de teste")),true,sent::add,{},{})}};return sent}
 @Test fun controlsProduceOnlyVisualDraftsWithoutReconfiguringHome(){val intents=show();compose.onNodeWithText("Ativar visualizador circular").performClick();assertTrue(intents.any{it is HomeScreenIntent.MusicVisual&&it.event is MusicVisualIntent.Edit});assertTrue(intents.none{it==HomeScreenIntent.UseLook||it is HomeScreenIntent.SetTheme})}
 @Test fun darkScreenAndPermissionLimitationsAreVisible(){show();compose.onNodeWithText("Usar tela preta durante a proteção").performScrollTo().assertExists();compose.onNodeWithText("Mantém o áudio. O painel LED continua iluminado; não desliga a TV.").assertExists();compose.onNodeWithText("Perfil ativo na TV: Perfil de teste").assertExists()}
 @Test fun oldTvDoesNotOfferTheOldBundledApkAsIfItWasTheNewMusicUpdate(){show(false);compose.onNodeWithText("Instale o APK Launcherin TV preview22 ou posterior na televisão. O instalador antigo de tela inicial não inclui essa versão.").assertExists();compose.onNodeWithText("Aplicar na TV").performScrollTo().assertIsNotEnabled()}
 @OptIn(ExperimentalRoborazziApi::class) @Config(qualifiers="pt-rBR-w400dp-h1000dp-xhdpi") @Test fun captureVisualSettings(){show();compose.waitForIdle();compose.onAllNodes(isRoot()).onLast().captureRoboImage("/tmp/launcherin22/captures/mobile-music-visual.png")}
}
