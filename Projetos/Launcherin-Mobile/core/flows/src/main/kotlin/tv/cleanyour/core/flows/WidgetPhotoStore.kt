package tv.cleanyour.core.flows

import tv.cleanyour.core.model.*

internal object WidgetPhotoStore {
    fun path(tv: TvId, id: String) = "tvs/${tv.value}/frames/$id.jpg"
    suspend fun stage(d: ManageDeps,tv: TvId,pictures: List<ByteArray>): List<String> {
        require(pictures.size in 1..10)
        return pictures.map { jpeg ->
            require(jpeg.size in 4..280000 && jpeg[0]==0xff.toByte() && jpeg[1]==0xd8.toByte())
            val ref=d.blobs.putBytes(jpeg);d.documents.write(path(tv,ref.sha256.hex),jpeg);ref.sha256.hex
        }.distinct()
    }
    suspend fun refs(d: ManageDeps,tv: TvId,ids: List<String>?): List<BlobRef> = ids.orEmpty().distinct().take(10).map { id ->
        require(Regex("[a-f0-9]{64}").matches(id))
        val bytes=d.documents.read(path(tv,id)) { it } ?: error("Foto indisponível no celular")
        require(bytes.size in 4..280000 && sha256Of(bytes).hex==id)
        d.blobs.get(Sha256(id)) ?: d.blobs.putBytes(bytes)
    }
}
