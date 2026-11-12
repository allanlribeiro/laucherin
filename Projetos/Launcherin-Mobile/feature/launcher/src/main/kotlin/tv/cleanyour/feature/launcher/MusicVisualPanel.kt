package tv.cleanyour.feature.launcher
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tv.cleanyour.core.flows.*
import tv.cleanyour.core.spec.MusicVisualOptions
import tv.cleanyour.ui.design.component.*

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable fun MusicVisualPanel(state:HomeScreenState,connected:Boolean,onIntent:(HomeScreenIntent)->Unit,onClose:()->Unit,onUpdate:()->Unit){
 val a=state.musicVisual;val o=a.options
 fun send(e:MusicVisualIntent)=onIntent(HomeScreenIntent.MusicVisual(e))
 fun edit(n:MusicVisualOptions)=send(MusicVisualIntent.Edit(n))
 LaunchedEffect(connected,state.launcherVersionCode){if(connected)send(MusicVisualIntent.Refresh)}
 CytBottomSheet(onClose){Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom=20.dp)){
  SheetHeader(stringResource(R.string.music_visual_title));Body(stringResource(R.string.music_visual_scope),Modifier.padding(bottom=12.dp));if(a.profileName.isNotBlank())Body(stringResource(R.string.music_visual_profile,a.profileName),Modifier.padding(bottom=12.dp))
  a.error?.let {Banner(it,BannerTone.WARNING,Modifier.padding(bottom=12.dp))};if(!a.compatible)Body(stringResource(R.string.music_visual_install))
  ListGroup {
   ListRow(stringResource(R.string.music_visual_enable),value=stringResource(if(o.enabled)R.string.visual_on else R.string.visual_off),onClick=if(a.busy)null else ({edit(o.copy(enabled=!o.enabled))}))
   ListRow(stringResource(R.string.music_visual_smart),subtitle=stringResource(R.string.music_visual_smart_note),value=stringResource(if(o.smart)R.string.visual_on else R.string.visual_off),onClick=if(a.busy)null else ({edit(o.copy(smart=!o.smart))}))
   ListRow(stringResource(R.string.music_visual_motion),value=stringResource(if(o.motion)R.string.visual_on else R.string.visual_off),onClick=if(a.busy)null else ({edit(o.copy(motion=!o.motion))}))
   ListRow(stringResource(R.string.music_visual_ambient),subtitle=stringResource(R.string.music_visual_ambient_note),value=stringResource(if(o.ambient)R.string.visual_on else R.string.visual_off),onClick=if(a.busy)null else ({edit(o.copy(ambient=!o.ambient))}))
  }
  SheetHeader(stringResource(R.string.music_visual_colors));ListGroup {for((id,label) in listOf("electric" to R.string.music_visual_electric,"cyan" to R.string.music_visual_cyan,"violet" to R.string.music_visual_violet,"blue" to R.string.music_visual_blue))ListRow(stringResource(label),value=if(o.palette==id)"✓" else null,onClick=if(a.busy)null else ({edit(o.copy(palette=id))}))}
  Body(stringResource(R.string.visual_intensity,o.intensity),Modifier.padding(top=12.dp));Slider(o.intensity.toFloat(),{edit(o.copy(intensity=it.toInt()))},valueRange=0f..100f,enabled=!a.busy)
  SheetHeader(stringResource(R.string.music_visual_timers));Body(stringResource(R.string.music_visual_timer_note));ListGroup {for(times in listOf(Triple(5,15,30),Triple(10,20,45),Triple(15,30,60)))ListRow(stringResource(R.string.music_visual_times,times.first,times.second,times.third),value=if(o.dimMinutes==times.first&&o.hideMinutes==times.second&&o.minimalMinutes==times.third)"✓" else null,onClick=if(a.busy)null else ({edit(o.copy(dimMinutes=times.first,hideMinutes=times.second,minimalMinutes=times.third))}))}
  ListGroup {ListRow(stringResource(R.string.music_visual_dark),subtitle=stringResource(R.string.music_visual_dark_note),value=stringResource(if(o.dark)R.string.visual_on else R.string.visual_off),onClick=if(a.busy)null else ({edit(o.copy(dark=!o.dark))}))}
  Body(stringResource(R.string.music_visual_permission),Modifier.padding(top=12.dp));TertiaryButton(stringResource(R.string.music_visual_refresh),{send(MusicVisualIntent.Refresh)},enabled=connected&&!a.busy)
  PrimaryButton(stringResource(R.string.hub_apply),{send(MusicVisualIntent.Apply)},enabled=connected&&a.compatible&&a.dirty&&!a.busy,loading=a.busy,modifier=Modifier.padding(top=12.dp));TertiaryButton(stringResource(R.string.visual_reset),{send(MusicVisualIntent.Reset)},enabled=!a.busy);TertiaryButton(stringResource(R.string.visual_close),onClose,enabled=!a.busy)
 }}
}
