package snd.komf.providers.bangumi.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class BangumiSubject(
    val id: Long,
    val type: SubjectType,
    val name: String,

    @SerialName("name_cn")
    val nameCn: String,
    val summary: String,
    val nsfw: Boolean,
    val locked: Boolean,

    /* TV, Web, 欧美剧, PS4... */
    val platform: String?,
    val images: Images,

    /* 书籍条目的册数，由旧服务端从wiki中解析 */
    val volumes: Int,

    /* 由旧服务端从wiki中解析，对于书籍条目为`话数` */
    val eps: Int,

    /* 数据库中的章节数量 */
    @SerialName("total_episodes")
    val totalEpisodes: Int,
    val rating: SubjectRating,
    val collection: SubjectCollection,
    val tags: List<SubjectTag>,

    /* air date in `YYYY-MM-DD` format */
    val date: String? = null,
    val infobox: Collection<Infobox>? = null,
)

object InfoBoxSerializer : JsonContentPolymorphicSerializer<Infobox>(Infobox::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Infobox> {
        if (element !is JsonObject) throw SerializationException("Expected JsonObject go ${element::class}")
        val value = element["value"]

        return when (value) {
            is JsonArray -> Infobox.MultipleValues.serializer()
            is JsonPrimitive -> Infobox.SingleValue.serializer()
            else -> throw SerializationException("Unexpected element type ${element::class}")
        }
    }
}

@Serializable(with = InfoBoxSerializer::class)
sealed interface Infobox {
    val key: String

    @Serializable
    class SingleValue(
        override val key: String,
        val value: String
    ) : Infobox

    @Serializable
    class MultipleValues(
        override val key: String,
        val value: List<InfoboxNestedValue>,
    ) : Infobox
}

@Serializable
data class InfoboxNestedValue(
    @SerialName("k")
    val key: String? = null,
    @SerialName("v")
    val value: String
)

class SubjectTypeSerializer : KSerializer<SubjectType> {
    override val descriptor = PrimitiveSerialDescriptor("BangumiSubjectType", PrimitiveKind.INT).nullable
    override fun serialize(encoder: Encoder, value: SubjectType) = encoder.encodeInt(value.ordinal)
    override fun deserialize(decoder: Decoder): SubjectType = SubjectType.fromValue(decoder.decodeInt())
}

@Serializable(with = SubjectTypeSerializer::class)
enum class SubjectType(val value: Int) {
    BOOK(1),
    ANIME(2),
    MUSIC(3),
    GAME(4),
    REAL(6);

    companion object {
        fun fromValue(value: Int): SubjectType {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Invalid SubjectType value: $value")
        }
    }
}

@Serializable
data class SubjectTag(
    val name: String,
    val count: Int
)

@Serializable
data class SubjectRelation(
    val id: Long,
    val name: String,
    @SerialName("name_cn")
    val nameCn: String,
    val type: SubjectType,
    val relation: String,
    val images: RelationImages,
)

@Serializable
data class RelationImages(
    val small: String,
    val grid: String,
    val large: String,
    val medium: String,
    val common: String,
)

@Serializable
data class SubjectCollection(
    val wish: Int,
    val collect: Int,
    val doing: Int,
    @SerialName("on_hold")
    val onHold: Int,
    val dropped: Int

)

@Serializable
data class SubjectRating(
    val rank: Int,
    val total: Int,
    val count: Map<Int, Int>,
    val score: Double
)

@Serializable
data class Images(
    val large: String? = null,
    val common: String? = null,
    val medium: String? = null,
    val small: String? = null,
    val grid: String? = null
)
