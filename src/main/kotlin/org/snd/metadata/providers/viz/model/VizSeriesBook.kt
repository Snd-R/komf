package org.snd.metadata.providers.viz.model

import org.snd.metadata.model.Provider.VIZ
import org.snd.metadata.model.SeriesSearchResult

data class VizSeriesBook(
    val id: VizBookId,
    val name: String,
    val seriesName: String,
    val number: Int?,
    val imageUrl: String?,
    val final: Boolean = false
)

fun VizSeriesBook.toSeriesSearchResult(): SeriesSearchResult {
    return SeriesSearchResult(
        imageUrl = imageUrl,
        title = name,
        provider = VIZ,
        resultId = id.id
    )
}
