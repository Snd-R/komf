package snd.komf.mediaserver.repository

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.SqlDriver
import kotlinx.datetime.Instant
import snd.komf.mediaserver.jobs.MetadataJobId
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerThumbnailId
import snd.komf.model.ProviderSeriesId
import java.util.*

expect class DriverFactory {
    fun createDriver(): SqlDriver
}


fun createDatabase(driverFactory: DriverFactory): Database {
    val dbDriver = driverFactory.createDriver()
    val database = Database(
        dbDriver,
        BookThumbnailAdapter = BookThumbnail.Adapter(
            bookIdAdapter = BookIdAdapter,
            seriesIdAdapter = SeriesIdAdapter,
            thumbnailIdAdapter = ThumbnailIdAdapter,
            mediaServerAdapter = EnumColumnAdapter()
        ),
        KomfJobRecordAdapter = KomfJobRecord.Adapter(
            idAdapter = MetadataJobIdAdapter,
            seriesIdAdapter = SeriesIdAdapter,
            statusAdapter = EnumColumnAdapter(),
            startedAtAdapter = InstantAdapter,
            finishedAtAdapter = InstantAdapter,
        ),
        SeriesMatchAdapter = SeriesMatch.Adapter(
            seriesIdAdapter = SeriesIdAdapter,
            typeAdapter = EnumColumnAdapter(),
            mediaServerAdapter = EnumColumnAdapter(),
            providerAdapter = EnumColumnAdapter(),
            providerSeriesIdAdapter = ProviderSeriesIdIdAdapter,
        ),
        SeriesThumbnailAdapter = SeriesThumbnail.Adapter(
            seriesIdAdapter = SeriesIdAdapter,
            thumbnailIdAdapter = ThumbnailIdAdapter,
            mediaServerAdapter = EnumColumnAdapter()
        ),
    )
    return database
}

private object SeriesIdAdapter : ColumnAdapter<MediaServerSeriesId, String> {
    override fun decode(databaseValue: String) = MediaServerSeriesId(databaseValue)
    override fun encode(value: MediaServerSeriesId) = value.value
}

private object BookIdAdapter : ColumnAdapter<MediaServerBookId, String> {
    override fun decode(databaseValue: String) = MediaServerBookId(databaseValue)
    override fun encode(value: MediaServerBookId) = value.value
}

private object ThumbnailIdAdapter : ColumnAdapter<MediaServerThumbnailId, String> {
    override fun decode(databaseValue: String) = MediaServerThumbnailId(databaseValue)
    override fun encode(value: MediaServerThumbnailId) = value.value
}

private object ProviderSeriesIdIdAdapter : ColumnAdapter<ProviderSeriesId, String> {
    override fun decode(databaseValue: String) = ProviderSeriesId(databaseValue)
    override fun encode(value: ProviderSeriesId) = value.value
}

private object MetadataJobIdAdapter : ColumnAdapter<MetadataJobId, String> {
    override fun decode(databaseValue: String) = MetadataJobId(UUID.fromString(databaseValue))
    override fun encode(value: MetadataJobId) = value.value.toString()
}

private object InstantAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long) = Instant.fromEpochMilliseconds(databaseValue)
    override fun encode(value: Instant) = value.toEpochMilliseconds()
}
