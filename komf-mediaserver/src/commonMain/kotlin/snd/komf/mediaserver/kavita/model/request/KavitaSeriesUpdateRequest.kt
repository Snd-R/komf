package snd.komf.mediaserver.kavita.model.request

import kotlinx.serialization.Serializable
import snd.komf.mediaserver.kavita.model.KavitaSeriesId

@Serializable
data class KavitaSeriesUpdateRequest(
    val id: KavitaSeriesId,
    val name: String,
    val localizedName: String? = null,
    val sortName: String,
    val coverImageLocked: Boolean,
    val nameLocked: Boolean,
    val sortNameLocked: Boolean,
    val localizedNameLocked: Boolean
)