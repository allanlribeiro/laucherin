package tv.cleanyour.feature.launcher
import android.graphics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.cleanyour.core.flows.*
import tv.cleanyour.core.spec.MusicProfile
import tv.cleanyour.ui.design.component.*
import tv.cleanyour.ui.design.theme.*
import kotlin.test.*
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
@RunWith(RobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE) @Config(sdk=[35],qualifiers="pt-rBR-w400dp-h1000dp-xhdpi")
class MusicProfilesPagesTest {
 @get:Rule val compose=createComposeRule()
 private fun show(compatible:Boolean=true):MutableList<HomeScreenIntent>{val sent=mutableListOf<HomeScreenIntent>();val profile=MusicProfile("default","Perfil de exemplo",0xff9273c8.toInt(),"","a".repeat(64));compose.setContent{CytTheme(darkTheme=true,reducedMotion=true){MusicProfilesPanel(HomeScreenState(tvName="TV de teste",launcherVersionCode=32,musicProfiles=MusicProfilesState(profiles=listOf(profile),active="default",selected="default",name=profile.name,compatible=compatible)),true,MusicProfilePhotoPicker{it(byteArrayOf(-1,-40,1,2))},sent::add,{})}};return sent}
 @Test fun selectingAPhotoUsesAnExplicitPickerAndDoesNotTouchTheHomeOrPlayMusic(){val sent=show();compose.onNodeWithText("Escolher foto no celular").performScrollTo().performClick();assertTrue(sent.any{it is HomeScreenIntent.MusicProfiles&&it.event is MusicProfilesIntent.Photo});assertTrue(sent.none{it==HomeScreenIntent.UseLook||it is HomeScreenIntent.SetTheme});compose.onNodeWithText("Salvar perfil na TV").assertIsNotEnabled()}
 @Test fun oldTvShowsTheRequiredVersionAndDisablesApply(){show(false);compose.onNodeWithText("Instale a Launcherin TV preview24 ou posterior e conecte o celular à TV.").assertExists();compose.onNodeWithText("Salvar perfil na TV").performScrollTo().assertIsNotEnabled()}
 @Test fun photoNormalizationMakesABoundedSquareJpegAndReleasesOriginal(){val bitmap=Bitmap.createBitmap(1200,800,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xff5678ab.toInt());val bytes=MusicProfilePhoto.square(bitmap);assertTrue(bitmap.isRecycled);assertTrue(bytes.size<=180000);val o=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,o);assertEquals(512,o.outWidth);assertEquals(512,o.outHeight);assertEquals("image/jpeg",o.outMimeType)}
 @OptIn(ExperimentalRoborazziApi::class) @Test fun captureMobileProfileSettings(){show();compose.waitForIdle();compose.onAllNodes(isRoot()).onLast().captureRoboImage("/tmp/launcherin24/captures/mobile-music-profiles.png")}
}
