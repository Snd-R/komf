package snd.komf.providers.ehentai

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

@Serializable
data class EHentaiResponse(
    val gmetadata: List<EHentaiBook> = emptyList(),
    val gid: Int? = null,
    val error: String? = null
)

@Serializable
data class EHentaiBook(
    val gid: Int,
    val token: String? = null,
    val title: String? = null,
    @SerialName("title_jpn")
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
    val rating: String? = null,
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
    val name: String,
    @SerialName("tsize")
    val tSize: String,
    @SerialName("fsize")
    val fSize: String
)

object InstantEpochSecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant =
        Instant.fromEpochSeconds(decoder.decodeString().toLong())

    override fun serialize(encoder: Encoder, value: Instant) =
        encoder.encodeString(value.epochSeconds.toString())
}