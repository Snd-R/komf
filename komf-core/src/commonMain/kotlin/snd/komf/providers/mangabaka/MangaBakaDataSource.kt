package snd.komf.providers.mangabaka

interface MangaBakaDataSource {
    suspend fun search(
        title: String,
        types: List<MangaBakaType>? = null,
        typesNot: List<MangaBakaType>? = null,
    ): List<MangaBakaSeries>

    suspend fun getSeries(id: MangaBakaSeriesId): MangaBakaSeries
}