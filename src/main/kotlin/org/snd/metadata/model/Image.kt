package org.snd.metadata.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

@JsonClass(generateAdapter = true)
data class Image(
    @Json(ignore = true)
    val image: ByteArray = byteArrayOf(),
    val mimeType: String? = null
) {

    fun toBufferedImage(): BufferedImage? {
        return runCatching { ImageIO.read(image.inputStream()) }
            .getOrNull()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Image

        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        return image.contentHashCode()
    }
}
