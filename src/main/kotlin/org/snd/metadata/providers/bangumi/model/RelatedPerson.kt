package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.JsonClass

/**
 *
 *
 * @param id
 * @param name
 * @param type `1`, `2`, `3` 表示 `个人`, `公司`, `组合`
 * @param career
 * @param relation
 * @param images
 */


@JsonClass(generateAdapter = true)
data class RelatedPerson (

    val id: Int,

    val name: String,

    /* `1`, `2`, `3` 表示 `个人`, `公司`, `组合` */
    val type: PersonType,

    val career: List<PersonCareer>,

    val relation: String,

    // Note: the schema returns Images that do not have `common`
    val images: Images? = null
)

