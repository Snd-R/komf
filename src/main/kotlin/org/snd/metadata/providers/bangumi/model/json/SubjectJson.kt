package org.snd.metadata.providers.bangumi.model.json

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.snd.metadata.providers.bangumi.model.Images
import org.snd.metadata.providers.bangumi.model.SubjectCollection
import org.snd.metadata.providers.bangumi.model.SubjectRating
import org.snd.metadata.providers.bangumi.model.SubjectTag
import org.snd.metadata.providers.bangumi.model.SubjectType

@JsonClass(generateAdapter = true)
data class SubjectJson(
    val id: Long,
    val type: SubjectType,
    val name: String,

    @Json(name = "name_cn")
    val nameCn: String,
    val summary: String,
    val nsfw: Boolean,
    val locked: Boolean,

    /* TV, Web, 欧美剧, PS4... */
    val platform: String,
    val images: Images,

    /* 书籍条目的册数，由旧服务端从wiki中解析 */
    val volumes: Int,

    /* 由旧服务端从wiki中解析，对于书籍条目为`话数` */
    val eps: Int,

    /* 数据库中的章节数量 */
    @Json(name = "total_episodes")
    val totalEpisodes: Int,
    val rating: SubjectRating,
    val collection: SubjectCollection,
    val tags: List<SubjectTag>,

    /* air date in `YYYY-MM-DD` format */
    val date: String? = null,
    val infobox: Collection<Infobox>? = null,
)

@JsonClass(generateAdapter = true)
data class Infobox(
    val key: String,
    val value: InfoboxValue
)

sealed class InfoboxValue {
    class SingleValue(val value: String) : InfoboxValue()
    class MultipleValues(val value: List<InfoboxNestedValue>) : InfoboxValue()
}

sealed class InfoboxNestedValue {
    class SingleValue(val value: String) : InfoboxNestedValue()
    class PairValue(val key: String, val value: String) : InfoboxNestedValue()
}
