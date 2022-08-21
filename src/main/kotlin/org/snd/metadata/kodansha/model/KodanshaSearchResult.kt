package org.snd.metadata.kodansha.model

import org.snd.metadata.model.Provider.KODANSHA
import org.snd.metadata.model.SeriesSearchResult

data class KodanshaSearchResult(
    val seriesId: KodanshaSeriesId,
    val title: String,
    val imageUrl: String?,
)

fun KodanshaSearchResult.toSeriesSearchResult(): SeriesSearchResult {
    return SeriesSearchResult(
        imageUrl = imageUrl,
        title = title,
        provider = KODANSHA,
        resultId = seriesId.id
    )
}
