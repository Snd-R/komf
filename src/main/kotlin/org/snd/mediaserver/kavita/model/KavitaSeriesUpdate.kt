package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KavitaSeriesUpdate(
    val id: Int,
    val name: String,
    val localizedName: String?,
    val sortName: String,
    val coverImageLocked: Boolean,
    val nameLocked: Boolean,
    val sortNameLocked: Boolean,
    val localizedNameLocked: Boolean
)

fun KavitaSeries.kavitaTitleUpdate(newName: String?, newSortName: String?, newLocalizedName: String?) = KavitaSeriesUpdate(
    id = id,
    name = newName ?: name,
    sortName = newSortName ?: sortName,
    localizedName = newLocalizedName ?: localizedName,
    nameLocked = nameLocked,
    sortNameLocked = sortNameLocked,
    localizedNameLocked = localizedNameLocked,

    coverImageLocked = coverImageLocked
)

fun KavitaSeries.kavitaCoverResetRequest() = KavitaSeriesUpdate(
    id = id,
    name = name,
    localizedName = localizedName,
    sortName = sortName,
    nameLocked = false,
    sortNameLocked = false,
    localizedNameLocked = false,

    coverImageLocked = false
)