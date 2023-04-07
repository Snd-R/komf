package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param id 条目ID
 * @param date 上映/开播/连载开始日期，可能为空字符串
 * @param image 封面
 * @param summary 条目描述
 * @param name 条目原名
 * @param nameCn 条目中文名
 * @param tags 
 * @param score 评分
 * @param rank 排名
 * @param type 
 */


@JsonClass(generateAdapter = true)
data class SearchSubjectsResponseData (

    /* 条目ID */
    val id: Int,

    /* 上映/开播/连载开始日期，可能为空字符串 */
    val date: String,

    /* 封面 */
    val image: String,

    /* 条目描述 */
    val summary: String?,

    /* 条目原名 */
    val name: String,

    /* 条目中文名 */
    @Json(name = "name_cn")
    val nameCn: String,

    val tags: List<Tag>,

    /* 评分 */
    val score: java.math.BigDecimal,

    /* 排名 */
    val rank: Int,

    val type: SubjectType? = null

)
