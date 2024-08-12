package snd.komf.providers.viz.model

import snd.komf.model.BookRange

data class VizSeriesBook(
    val id: VizBookId,
    val name: String,
    val seriesName: String,
    val number: BookRange?,
    val imageUrl: String?,
    val final: Boolean = false
)

