package snd.komf.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Image(
    @Transient
    val image: ByteArray = byteArrayOf(),
    val mimeType: String? = null
) {

    override fun hashCode(): Int {
        return image.contentHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Image

        if (!image.contentEquals(other.image)) return false
        if (mimeType != other.mimeType) return false

        return true
    }
}
