package snd.komf.mangabaka.model

import snd.komf.model.KomgaSeriesId

data class MangaBakaLinkedSeries(
    val komgaId: KomgaSeriesId,
    val mangaBaka: MangaBakaSeries,
)