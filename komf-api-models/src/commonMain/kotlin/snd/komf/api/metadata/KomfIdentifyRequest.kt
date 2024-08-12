package snd.komf.api.metadata

import kotlinx.serialization.Serializable
import snd.komf.api.KomfProviders
import snd.komf.api.KomfServerLibraryId
import snd.komf.api.KomfServerSeriesId

@Serializable
data class KomfIdentifyRequest(
    val libraryId: KomfServerLibraryId?,
    val seriesId: KomfServerSeriesId,
    val provider: KomfProviders,
    val providerSeriesId: String,
)
