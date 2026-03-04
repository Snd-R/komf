package snd.komf.providers.ehentai.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.round
import kotlin.time.Instant

@Serializable
data class EHentaiBook(
    val gid: Int,
    val token: String,
    @Serializable(with = HtmlUnescapeStringSerializer::class)
    val title: String,
    @SerialName("title_jpn")
    @Serializable(with = HtmlUnescapeStringSerializer::class)
    val titleJpn: String? = null,
    val category: String? = null,
    val thumb: String? = null,
    val uploader: String? = null,
    @Serializable(with = InstantEpochSecondsSerializer::class)
    val posted: Instant? = null,
    @SerialName("filecount")
    val fileCount: String? = null,
    @SerialName("filesize")
    val fileSize: Long? = null,
    val expunged: Boolean? = null,
    @Serializable(with = StringToRoundedDoubleSerializer::class)
    val rating: Double? = null,
    @SerialName("torrentcount")
    val torrentCount: String? = null,
    val torrents: List<EHentaiTorrent>? = null,
    val tags: List<String>? = null,
    @SerialName("parent_gid")
    val parentGid: String? = null,
    @SerialName("parent_key")
    val parentKey: String? = null,
    @SerialName("current_gid")
    val currentGid: String? = null,
    @SerialName("current_key")
    val currentKey: String? = null,
    @SerialName("first_gid")
    val firstGid: String? = null,
    @SerialName("first_key")
    val firstKey: String? = null,
    val error: String? = null
)

@Serializable
data class EHentaiTorrent(
    val hash: String,
    @Serializable(with = InstantEpochSecondsSerializer::class)
    val added: Instant,
    @Serializable(with = HtmlUnescapeStringSerializer::class)
    val name: String,
    @SerialName("tsize")
    val tSize: String,
    @SerialName("fsize")
    val fSize: String
)

/**
 * Automatically convert a rating string (e.g. "4.68") to a rounded Double (e.g. 5.0)
 */
object StringToRoundedDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringToRoundedDouble", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Double {
        val stringValue = decoder.decodeString()
        val doubleValue = stringValue.toDoubleOrNull() ?: 0.0
        return round(doubleValue)
    }

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeString(value.toString())
    }
}

/**
 * Automatically unescape HTML entities during JSON parsing
 */
object HtmlUnescapeStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("HtmlUnescapeString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        return decoder.decodeString()
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

object InstantEpochSecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant =
        Instant.fromEpochSeconds(decoder.decodeString().toLong())

    override fun serialize(encoder: Encoder, value: Instant) =
        encoder.encodeString(value.epochSeconds.toString())
}