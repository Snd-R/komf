package snd.komf.api.mangabaka

import kotlinx.serialization.Serializable
import snd.komf.api.KomfServerSeriesId

@Serializable
data class KomfMangaBakaUnlinkRequest(
    val komgaId: KomfServerSeriesId,
)