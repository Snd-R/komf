package org.snd.metadata.providers.bangumi.model
import com.squareup.moshi.JsonClass

/**
 * 用户信息
 *
 * @param total 搜索结果数量
 * @param limit 当前分页参数
 * @param offset 当前分页参数
 * @param data
 */

@JsonClass(generateAdapter = true)
data class SearchSubjectsResponse (

    /* 搜索结果数量 */
    val total: Int? = null,

    /* 当前分页参数 */
    val limit: Int? = null,

    /* 当前分页参数 */
    val offset: Int? = null,

    val data: List<SearchSubjectsResponseData>? = null

)