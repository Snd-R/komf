package snd.komf.providers.mangabaka

import snd.komf.mangabaka.model.MangaBakaSeries
import snd.komf.mangabaka.model.MangaBakaSeriesId
import snd.komf.mangabaka.model.MangaBakaType
import snd.komf.mangabaka.repository.MangaBakaRepository

class MangaBakaDbDataSource(private val repository: MangaBakaRepository) : MangaBakaDataSource {

    override suspend fun search(
        title: String,
        types: List<MangaBakaType>?,
        typesNot: List<MangaBakaType>?
    ): List<MangaBakaSeries> {
        return repository.search(title, types, typesNot)
    }

    override suspend fun getSeries(id: MangaBakaSeriesId): MangaBakaSeries {
        return repository.get(id)
    }
}