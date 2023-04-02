package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Subject (
    val id: Int,

    val type: SubjectType,

    val name: String,

    val name_cn: String,

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
    val total_episodes: Int,

    val rating: Rating,

    val collection: Collection,

    val tags: List<Tag>,

    /* air date in `YYYY-MM-DD` format */
    val date: String? = null,

    val infobox: kotlin.collections.Collection<Info>? = null
)
