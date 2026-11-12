package tv.cleanyour.feature.launcher
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tv.cleanyour.core.flows.*
import tv.cleanyour.ui.design.component.*

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable fun MusicProfilesPanel(state:HomeScreenState,connected:Boolean,picker:MusicProfilePhotoPicker?,onIntent:(HomeScreenIntent)->Unit,onClose:()->Unit){
 val a=state.musicProfiles
 fun send(e:MusicProfilesIntent)=onIntent(HomeScreenIntent.MusicProfiles(e))
 LaunchedEffect(connected,state.launcherVersionCode){if(connected)send(MusicProfilesIntent.Refresh)}
 val image=remember(a.photo,a.preview){a.preview?.let{BitmapFactory.decodeByteArray(it,0,it.size)?.asImageBitmap()}}
 CytBottomSheet(onClose){Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom=20.dp)){
  SheetHeader(stringResource(R.string.music_profiles_title));Body(stringResource(R.string.music_profiles_scope),Modifier.padding(bottom=12.dp))
  a.error?.let{Banner(it,BannerTone.WARNING,Modifier.padding(bottom=12.dp))};if(!a.compatible)Body(stringResource(R.string.music_profiles_install))
  ListGroup {for(p in a.profiles)ListRow(p.name,value=if(p.id==a.active)stringResource(R.string.music_profiles_active)else if(p.id==a.selected)"✓" else null,onClick=if(a.busy)null else({send(MusicProfilesIntent.Select(p.id))}))}
  if(a.selected.isNotBlank()){
   SheetHeader(stringResource(if(a.saved==null)R.string.music_profiles_new else R.string.music_profiles_edit))
   OutlinedTextField(a.name,{send(MusicProfilesIntent.Name(it))},label={Body(stringResource(R.string.music_profiles_name))},singleLine=true,enabled=!a.busy,modifier=Modifier.fillMaxWidth())
   if(image!=null)Image(image,stringResource(R.string.music_profiles_photo),Modifier.padding(top=16.dp).size(100.dp).clip(CircleShape),contentScale=ContentScale.Crop)
   else if(a.photo.isNotEmpty())Body(stringResource(R.string.music_profiles_photo_on_tv),Modifier.padding(top=12.dp))
   else Body(stringResource(R.string.music_profiles_initials),Modifier.padding(top=12.dp))
   TertiaryButton(stringResource(R.string.music_profiles_choose),{picker?.pick{send(MusicProfilesIntent.Photo(it))}},enabled=connected&&!a.busy&&a.compatible&&picker!=null)
   if(a.photo.isNotEmpty())TertiaryButton(stringResource(R.string.music_profiles_remove),{send(MusicProfilesIntent.RemovePhoto)},enabled=!a.busy)
   PrimaryButton(stringResource(R.string.music_profiles_apply),{send(MusicProfilesIntent.Apply)},enabled=connected&&a.compatible&&!a.busy&&a.dirty&&a.name.isNotBlank(),loading=a.busy,modifier=Modifier.padding(top=12.dp))
  }
  TertiaryButton(stringResource(R.string.music_profiles_add),{send(MusicProfilesIntent.Add)},enabled=connected&&a.compatible&&!a.busy&&a.profiles.size<8)
  TertiaryButton(stringResource(R.string.music_visual_refresh),{send(MusicProfilesIntent.Refresh)},enabled=connected&&!a.busy)
  TertiaryButton(stringResource(R.string.visual_close),onClose,enabled=!a.busy)
 }}
}
