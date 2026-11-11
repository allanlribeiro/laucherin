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
import tv.cleanyour.core.spec.*
import tv.cleanyour.ui.design.theme.*
import kotlin.test.*
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[35],qualifiers="pt-rBR-w400dp-h2400dp-xhdpi")
class AppearancePagesTest {
 @get:Rule val compose=createComposeRule()
 private fun show(wallpaper:Boolean=false,compatible:Boolean=true):MutableList<HomeScreenIntent>{val intents=mutableListOf<HomeScreenIntent>();val state=HomeScreenState(tvName="Sala",homeScreenOurs=true,launcherVersionCode=26,appearance=AppearanceState(compatible=compatible));compose.setContent {CytTheme(darkTheme=true,reducedMotion=true){AppearancePanel(state,true,wallpaper,null,intents::add,{},{})}};return intents}
 @OptIn(ExperimentalRoborazziApi::class) @Config(qualifiers="pt-rBR-w400dp-h900dp-xhdpi") @Test fun screenshotAppearance(){show();compose.waitForIdle();compose.onAllNodes(isRoot()).onLast().captureRoboImage("/tmp/launcherin-mobile-appearance.png")}
 @OptIn(ExperimentalRoborazziApi::class) @Config(qualifiers="pt-rBR-w400dp-h900dp-xhdpi") @Test fun screenshotWallpaper(){show(wallpaper=true);compose.waitForIdle();compose.onAllNodes(isRoot()).onLast().captureRoboImage("/tmp/launcherin-mobile-wallpaper.png")}
 @Test fun styleSelectionCreatesADraftWithoutReconfiguringHome(){val intents=show();compose.onNodeWithText("Minimal Cinematic").performScrollTo().performClick();assertTrue(intents.any {it is HomeScreenIntent.Appearance&&it.event is AppearanceIntent.Edit});assertTrue(intents.none {it==HomeScreenIntent.UseLook||it is HomeScreenIntent.SetTheme})}
 @Test fun wallpaperChoicesAndAutomaticPaletteAreInSeparatePages(){show(wallpaper=true);compose.onNodeWithText("Imagem personalizada").assertExists();compose.onNodeWithText("Imagem dinâmica").assertExists();compose.onNodeWithText("Estilo dos widgets e painéis").assertDoesNotExist()}
 @Test fun unsupportedTvHasAnUpdateRouteAndCannotApply(){show(compatible=false);compose.onNodeWithText("Atualizar a tela inicial").assertExists();compose.onNodeWithText("Aplicar na TV").performScrollTo().assertIsNotEnabled()}
}
