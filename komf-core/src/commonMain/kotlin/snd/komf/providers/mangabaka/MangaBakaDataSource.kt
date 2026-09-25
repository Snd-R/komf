package snd.komf.providers.mangabaka

import snd.komf.mangabaka.model.MangaBakaSeries
import snd.komf.mangabaka.model.MangaBakaSeriesId
import snd.komf.mangabaka.model.MangaBakaType

interface MangaBakaDataSource {
    suspend fun search(
        title: String,
        types: List<MangaBakaType>? = null,
        typesNot: List<MangaBakaType>? = null,
    ): List<MangaBakaSeries>

    suspend fun getSeries(id: MangaBakaSeriesId): MangaBakaSeries
}