package tv.cleanyour.core.spec
import kotlinx.serialization.json.*

/** Independent appearance preferences. None of these keys changes Home layout or app behavior. */
data class AppearanceOptions(
 val theme:String="launcherin",val family:String="glass_premium",val apps:String="premium",val intensity:Int=45,
 val autoPalette:Boolean=false,val reduced:Boolean=false,val wallpaperMode:String="existing",val originalSha:String="",val personalSha:String="",val dynamic:List<String> = emptyList(),val rotationMinutes:Int=15,val dim:Int=10,
){
 fun json():JsonObject=buildJsonObject{put("theme",theme);put("family",family);put("apps",apps);put("intensity",intensity);put("auto_palette",autoPalette);put("reduced",reduced);put("wallpaper_mode",wallpaperMode);put("original_sha",originalSha);put("personal_sha",personalSha);put("dynamic",JsonArray(dynamic.map(::JsonPrimitive)));put("rotation_minutes",rotationMinutes);put("dim",dim)}
 companion object {
  val families=listOf("glass_premium","minimal_cinematic","ambient_dynamic");val appStyles=listOf("original","glass","minimal","premium")
  fun read(o:JsonObject):AppearanceOptions{
   require(o.keys.all {it in setOf("theme","family","apps","intensity","auto_palette","reduced","wallpaper_mode","original_sha","personal_sha","dynamic","rotation_minutes","dim")})

   fun s(k:String,d:String="")=o[k]?.jsonPrimitive?.contentOrNull?:d
   fun i(k:String,d:Int)=o[k]?.jsonPrimitive?.intOrNull?:d
   fun b(k:String)=o[k]?.jsonPrimitive?.booleanOrNull?:false
   val n=AppearanceOptions(s("theme","launcherin"),s("family","glass_premium"),s("apps","premium"),i("intensity",45),b("auto_palette"),b("reduced"),s("wallpaper_mode","existing"),s("original_sha"),s("personal_sha"),o["dynamic"]?.jsonArray.orEmpty().map {it.jsonPrimitive.content},i("rotation_minutes",15),i("dim",10))
   require(Regex("[a-z0-9-]{1,64}").matches(n.theme));require(n.family in families&&n.apps in appStyles&&n.intensity in 0..100&&n.dim in 0..90&&n.rotationMinutes in 1..1440&&n.dynamic.size<=10)
   require(n.wallpaperMode in listOf("existing","theme","personal","dynamic"));require((n.dynamic+listOf(n.originalSha,n.personalSha)).all {it.isEmpty()||Regex("[a-f0-9]{64}").matches(it)})
   return n
  }
 }
}
