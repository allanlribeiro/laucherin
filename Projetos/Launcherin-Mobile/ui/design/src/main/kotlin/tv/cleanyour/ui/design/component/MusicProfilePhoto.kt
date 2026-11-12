package tv.cleanyour.ui.design.component
import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.*

object MusicProfilePhoto {
 fun read(context:Context,uri:Uri):ByteArray {
  val original=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver,uri)){decoder,info,_->
   require(info.size.width in 1..32768&&info.size.height in 1..32768)
   val scale=min(1f,1024f/max(info.size.width,info.size.height));decoder.setTargetSize(max(1,(info.size.width*scale).roundToInt()),max(1,(info.size.height*scale).roundToInt()));decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
  }
  return square(original)
 }
 fun square(original:Bitmap):ByteArray {
  var crop:Bitmap?=null;var scaled:Bitmap?=null
  try {
   val side=min(original.width,original.height);crop=Bitmap.createBitmap(original,(original.width-side)/2,(original.height-side)/2,side,side)
   scaled=if(side>512)Bitmap.createScaledBitmap(crop,512,512,true)else crop
   for(q in listOf(88,75,60,45)){val out=ByteArrayOutputStream();scaled.compress(Bitmap.CompressFormat.JPEG,q,out);if(out.size()<=180000)return out.toByteArray()};error("Foto muito grande")
  }finally{if(scaled!==crop)scaled?.recycle();if(crop!==original)crop?.recycle();original.recycle()}
 }
}
