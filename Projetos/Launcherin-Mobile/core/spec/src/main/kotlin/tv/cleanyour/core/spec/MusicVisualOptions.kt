package tv.cleanyour.core.spec
import kotlinx.serialization.json.*
/** Visual-only settings. Defaults and limits match TV schema1. No playback, Home or account data. */
data class MusicVisualOptions(val enabled:Boolean=false,val palette:String="electric",val intensity:Int=65,val motion:Boolean=true,val dimMinutes:Int=5,val hideMinutes:Int=15,val minimalMinutes:Int=30,val dark:Boolean=false,val ambient:Boolean=false,val smart:Boolean=true){
 fun json()=buildJsonObject {put("enabled",enabled);put("palette",palette);put("intensity",intensity);put("motion",motion);put("dim_minutes",dimMinutes);put("hide_minutes",hideMinutes);put("minimal_minutes",minimalMinutes);put("dark",dark);put("ambient",ambient);put("smart",smart)}
 companion object {
  fun read(j:JsonObject):MusicVisualOptions {
   require(j.keys.all {it in setOf("enabled","palette","intensity","motion","dim_minutes","hide_minutes","minimal_minutes","dark","ambient","smart")})
   fun b(k:String,d:Boolean):Boolean {val v=j[k]?:return d;require(v is JsonPrimitive&&!v.isString&&v.booleanOrNull!=null);return v.boolean}
   fun n(k:String,d:Int):Int {val v=j[k]?:return d;require(v is JsonPrimitive&&!v.isString&&v.intOrNull!=null);return v.int}
   val p=j["palette"]?.let {require(it is JsonPrimitive&&it.isString);it.content}?:"electric"
   val o=MusicVisualOptions(b("enabled",false),p,n("intensity",65),b("motion",true),n("dim_minutes",5),n("hide_minutes",15),n("minimal_minutes",30),b("dark",false),b("ambient",false),b("smart",true));require(o.palette in listOf("electric","cyan","violet","blue")&&o.intensity in 0..100&&o.dimMinutes>=1&&o.hideMinutes>o.dimMinutes&&o.minimalMinutes>o.hideMinutes&&o.minimalMinutes<=240);return o
  }
 }
}
