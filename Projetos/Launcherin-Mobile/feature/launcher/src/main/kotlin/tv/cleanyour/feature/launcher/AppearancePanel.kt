package tv.cleanyour.feature.launcher

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tv.cleanyour.core.flows.*
import tv.cleanyour.core.spec.*
import tv.cleanyour.ui.design.component.*

/** Appearance and wallpaper are independent; editing either never opens or rebuilds the layout editor. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppearancePanel(state:HomeScreenState,connected:Boolean,wallpaper:Boolean,photos:PhotoPicker?,onIntent:(HomeScreenIntent)->Unit,onClose:()->Unit,onUpdate:()->Unit){
 val a=state.appearance;val o=a.options;var picked by remember {mutableStateOf<Uri?>(null)}
 fun send(e:AppearanceIntent)=onIntent(HomeScreenIntent.Appearance(e))
 fun edit(n:AppearanceOptions)=send(AppearanceIntent.Edit(n))
 LaunchedEffect(connected,state.launcherVersionCode){if(connected)send(AppearanceIntent.Refresh)}
 CytBottomSheet(onClose){
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom=20.dp)){
   SheetHeader(stringResource(if(wallpaper)R.string.visual_wallpaper else R.string.visual_title))
   Body(stringResource(R.string.visual_scope),Modifier.padding(bottom=12.dp))
   a.error?.let { Banner(it,BannerTone.WARNING,Modifier.padding(bottom=12.dp)) }
   if(!a.compatible)TertiaryButton(stringResource(R.string.visual_update),onUpdate)
   if(!wallpaper){
    SheetHeader(stringResource(R.string.visual_theme))
    ListGroup {ListRow(stringResource(R.string.visual_neutral),value=if(o.theme=="launcherin")"✓" else null,onClick=if(a.busy)null else ({edit(o.copy(theme="launcherin"))}))
     state.themes.forEach {t->ListRow(themeName(t.id),value=if(o.theme==t.id)"✓" else null,onClick=if(a.busy)null else ({edit(o.copy(theme=t.id))}))}
    }
    SheetHeader(stringResource(R.string.visual_widget_style))
    ListGroup {for((id,label) in listOf("glass_premium" to R.string.visual_glass,"minimal_cinematic" to R.string.visual_minimal,"ambient_dynamic" to R.string.visual_ambient))ListRow(stringResource(label),value=if(o.family==id)"✓" else null,onClick=if(a.busy)null else ({edit(o.copy(family=id))}))}
    SheetHeader(stringResource(R.string.visual_apps))
    ListGroup {for((id,label) in listOf("original" to R.string.visual_original,"glass" to R.string.visual_app_glass,"minimal" to R.string.visual_app_minimal,"premium" to R.string.visual_app_premium))ListRow(stringResource(label),value=if(o.apps==id)"✓" else null,onClick=if(a.busy)null else ({edit(o.copy(apps=id))}))}
    Body(stringResource(R.string.visual_intensity,o.intensity),Modifier.padding(top=16.dp));Slider(o.intensity.toFloat(),{edit(o.copy(intensity=it.toInt()))},valueRange=0f..100f,enabled=!a.busy)
    ListGroup {
     ListRow(stringResource(R.string.visual_palette),subtitle=stringResource(R.string.visual_palette_desc),value=stringResource(if(o.autoPalette)R.string.visual_on else R.string.visual_off),onClick=if(a.busy)null else ({edit(o.copy(autoPalette=!o.autoPalette))}))
     ListRow(stringResource(R.string.visual_reduce),subtitle=stringResource(R.string.visual_reduce_desc),value=stringResource(if(o.reduced)R.string.visual_on else R.string.visual_off),onClick=if(a.busy)null else ({edit(o.copy(reduced=!o.reduced))}))
    }
    TertiaryButton(stringResource(R.string.visual_reset),{send(AppearanceIntent.Reset)},enabled=!a.busy)
   }else{
    ListGroup {for((id,label) in listOf("existing" to R.string.visual_keep_wall,"theme" to R.string.visual_theme_wall,"personal" to R.string.visual_personal_wall,"dynamic" to R.string.visual_dynamic_wall))ListRow(stringResource(label),value=if(o.wallpaperMode==id)"✓" else null,onClick=if(a.busy)null else ({edit(o.copy(wallpaperMode=id))}))}
    Body(stringResource(R.string.visual_wall_independent),Modifier.padding(top=12.dp))
    if(o.wallpaperMode=="personal"){
     PrimaryButton(stringResource(R.string.visual_choose_photo),{photos?.pick {picked=it}},enabled=photos!=null&&!a.busy,modifier=Modifier.padding(top=12.dp))
     if(o.personalSha.isNotEmpty())Body(stringResource(R.string.visual_photo_saved),Modifier.padding(top=8.dp))
    }
    if(o.wallpaperMode=="dynamic"){
     SheetHeader(stringResource(R.string.visual_collection))
     val gallery=remember {BundledThemeSource.gallery}
     ListGroup {gallery.forEach {p->val hash=p.wallpaper.sha256.hex;ListRow(pictureName(p.id),value=if(hash in o.dynamic)"✓" else null,onClick=if(a.busy)null else ({edit(o.copy(dynamic=if(hash in o.dynamic)o.dynamic-hash else (o.dynamic+hash).take(10)))}))}}
     SheetHeader(stringResource(R.string.visual_frequency))
     ListGroup {for(minutes in listOf(1,5,15,30,60))ListRow(stringResource(R.string.visual_minutes,minutes),value=if(o.rotationMinutes==minutes)"✓" else null,onClick=if(a.busy)null else ({edit(o.copy(rotationMinutes=minutes))}))}
    }
    if(o.wallpaperMode!="existing"){Body(stringResource(R.string.visual_dim,o.dim),Modifier.padding(top=16.dp));Slider(o.dim.toFloat(),{edit(o.copy(dim=it.toInt()))},valueRange=0f..90f,enabled=!a.busy)}
   }
   if(a.busy)Body(stringResource(R.string.visual_sending),Modifier.padding(top=12.dp))
   PrimaryButton(stringResource(R.string.hub_apply),{send(AppearanceIntent.Apply)},enabled=connected&&a.compatible&&a.dirty&&!a.busy,loading=a.busy,modifier=Modifier.padding(top=16.dp))
   TertiaryButton(stringResource(R.string.visual_close),onClose,enabled=!a.busy)
  }
 }
 picked?.let {uri->PhotoCropDialog(uri,null,o.dim,onDone={jpeg,dim->send(AppearanceIntent.Photo(jpeg,dim));picked=null},onCancel={picked=null})}
}
private fun themeName(id:String):String=when(id){"default"->"Padrão";"six-columns"->"Seis colunas";"cinema"->"Cinema";"console"->"Console";"horizon"->"Horizonte";"quiet"->"Preto discreto";"diwali"->"Diwali";"library"->"Biblioteca";"arcade"->"Arcade";"mist"->"Névoa";"aurora"->"Aurora";else->id}
private fun pictureName(id:String):String=when(id){"glass"->"Azul profundo";"ruska"->"Lago de outono";"curtains"->"Cortinas";"mountains"->"Montanhas";"lamp"->"Luz ambiente";"neon"->"Neon";"cobalt"->"Cobalto";else->themeName(id)}
