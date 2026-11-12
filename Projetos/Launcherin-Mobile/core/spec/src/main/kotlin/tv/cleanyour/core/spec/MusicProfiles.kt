package tv.cleanyour.core.spec

import kotlinx.serialization.json.*

data class MusicProfile(val id:String,val name:String,val color:Int,val photo:String,val revision:String){
 init {require(validId(id));require(name.isNotBlank()&&name.length<=40&&name.none{it<' '});require(photo.isEmpty()||photo.matches(Regex("[a-f0-9]{64}")));require(revision.matches(Regex("[a-f0-9]{64}")))}
 companion object {
  fun validId(id:String)=id.matches(Regex("default|[a-f0-9]{32}"))
  fun read(j:JsonObject)=MusicProfile(j.getValue("id").jsonPrimitive.content,j.getValue("name").jsonPrimitive.content,j.getValue("color").jsonPrimitive.int,j.getValue("photo").jsonPrimitive.content,j.getValue("revision").jsonPrimitive.content)
 }
}
