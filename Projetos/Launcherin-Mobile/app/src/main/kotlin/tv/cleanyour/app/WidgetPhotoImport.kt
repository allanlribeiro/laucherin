package tv.cleanyour.app

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Selected image grants only; no storage permission, retained URI, EXIF or network fetch. */
internal object WidgetPhotoImport {
    fun read(context: Context,uri: Uri): ByteArray {
        var bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver,uri)) { decoder,info,_ ->
            require(info.size.width in 1..32768&&info.size.height in 1..32768)
            val scale=1600f/max(info.size.width,info.size.height).coerceAtLeast(1600)
            decoder.setTargetSize((info.size.width*scale).roundToInt().coerceAtLeast(1),(info.size.height*scale).roundToInt().coerceAtLeast(1))
            decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
        }
        try {
            for(pass in 0..5){
                for(quality in listOf(90,75,60,45)) {
                    val bytes=ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,quality,bytes)
                    if(bytes.size()<=280000)return bytes.toByteArray()
                }
                val smaller=Bitmap.createScaledBitmap(bitmap,(bitmap.width*.75f).roundToInt().coerceAtLeast(1),(bitmap.height*.75f).roundToInt().coerceAtLeast(1),true)
                if(smaller!==bitmap)bitmap.recycle();bitmap=smaller
            }
            error("Image too large")
        }finally{bitmap.recycle()}
    }
}
