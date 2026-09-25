package snd.komf.mangabaka.external

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.counted
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.io.readByteArray
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.IOUtils
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.SchemaUtils.sortTablesByReferences
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import snd.komf.mangabaka.model.MangaBakaImportMetadata
import snd.komf.mangabaka.repository.tables.ArtistsTable
import snd.komf.mangabaka.repository.tables.AuthorsTable
import snd.komf.mangabaka.repository.tables.ImportMetadataTable
import snd.komf.mangabaka.repository.tables.LinksTable
import snd.komf.mangabaka.repository.tables.PublishersTable
import snd.komf.mangabaka.repository.tables.RelationshipsTable
import snd.komf.mangabaka.repository.tables.SeriesTable
import snd.komf.mangabaka.repository.tables.SeriesTagsTable
import snd.komf.mangabaka.repository.tables.TagsTable
import snd.komf.mangabaka.repository.tables.TitlesTable
import snd.komf.model.DOWNLOAD_BUFFER_SIZE
import snd.komf.model.DownloadProgress
import snd.komf.model.DownloadProgress.FinishedEvent
import snd.komf.model.DownloadProgress.ProgressEvent
import java.io.BufferedInputStream
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.time.Clock
import kotlin.time.Instant

private val logger = KotlinLogging.logger { }

class MangaBakaDbDownloader(
    private val ktor: HttpClient,
    private val mangaBakaApi: MangaBakaApiClient,
    private val workDir: Path,
    private val mangaBakaDatabase: Database,
    private val onStateRefresh: suspend () -> Unit,
) {
    private val databaseUrl = "https://api.mangabaka.org/v1/database/series.sqlite.tar.gz"
    private val checksumUrl = "https://api.mangabaka.org/v1/database/series.sqlite.tar.gz.sha1"
    private val databaseArchiveFile: Path = workDir.resolve("series.sqlite.tar.gz")
    private val databaseImportFile: Path = workDir.resolve("series.sqlite")

    private val progressFlow = MutableSharedFlow<DownloadProgress>(
        replay = 1,
        extraBufferCapacity = 1000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val downloadMutex = Mutex()
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun launchDownload(): Flow<DownloadProgress> {
        if (downloadMutex.tryLock()) {
            progressFlow.resetReplayCache()
            downloadScope.launch { doDownload(lockedMutex = downloadMutex) }
        }

        return progressFlow
    }

    private suspend fun doDownload(lockedMutex: Mutex) {
        try {
            progressFlow.emit(ProgressEvent(0, 0, checksumUrl))

            val oldChecksum = getDbMetadata()?.checksum
            val newChecksum = ktor.get(checksumUrl).bodyAsText().trim()
            if (oldChecksum == newChecksum) {
                progressFlow.emit(FinishedEvent)
                return
            }

            databaseArchiveFile.deleteIfExists()

            downloadDatabaseArchive()
            extractDatabaseFile()
            prepareTables()
            importTags()
            importData()
            createSearchIndex()
            saveDbMetadata(MangaBakaImportMetadata(Clock.System.now(), newChecksum))

            databaseArchiveFile.deleteIfExists()
            databaseImportFile.deleteIfExists()
            progressFlow.emit(FinishedEvent)
            onStateRefresh()
        } catch (e: Exception) {
            logger.catching(e)
            databaseArchiveFile.deleteIfExists()
            databaseImportFile.deleteIfExists()
            progressFlow.emit(
                DownloadProgress.ErrorEvent("${e::class.simpleName}: ${e.message}")
            )
            currentCoroutineContext().ensureActive()
        } finally {
            lockedMutex.unlock()
        }
    }

    private suspend fun downloadDatabaseArchive() {
        progressFlow.emit(ProgressEvent(0, 0, databaseUrl))
        ktor.prepareGet(databaseUrl).execute { response ->
            val length = response.headers["Content-Length"]?.toLong() ?: 0L
            progressFlow.emit(ProgressEvent(length, 0, databaseUrl))
            val channel = response.bodyAsChannel().counted()

            databaseArchiveFile.outputStream().buffered().use { outputStream ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DOWNLOAD_BUFFER_SIZE)
                    while (!packet.exhausted()) {
                        outputStream.write(packet.readByteArray())
                    }
                    progressFlow.emit(ProgressEvent(length, channel.totalBytesRead, databaseUrl))
                }
                outputStream.flush()
            }
        }
    }

    private suspend fun extractDatabaseFile() {
        progressFlow.emit(ProgressEvent(0, 0, "extracting $databaseArchiveFile"))
        TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(databaseArchiveFile.inputStream())))
            .use { archiveStream ->
                // take only first entry
                archiveStream.nextEntry
                IOUtils.copyLarge(archiveStream, databaseImportFile.outputStream())
            }
    }

    private suspend fun prepareTables() {
        suspendTransaction(mangaBakaDatabase) {
            val tables = sortTablesByReferences(
                listOf(
                    ArtistsTable,
                    AuthorsTable,
                    LinksTable,
                    PublishersTable,
                    RelationshipsTable,
                    SeriesTable,
                    SeriesTagsTable,
                    TagsTable,
                    TitlesTable
                )
            ).toTypedArray()


            if (tables.any { it.exists() }) progressFlow.emit(ProgressEvent(0, 0, "dropping old data tables"))
            exec("DROP TABLE IF EXISTS titles_fts;")
            SchemaUtils.drop(*tables, inBatch = true)

            progressFlow.emit(ProgressEvent(0, 0, "creating new data tables"))
            execInBatch(SchemaUtils.createStatements(*tables))
        }
        transaction(mangaBakaDatabase) {
            connection.autoCommit = true
            exec("VACUUM")
            connection.autoCommit = false

        }
    }

    private suspend fun importData() {
        progressFlow.emit(ProgressEvent(0, 0, "importing database"))
        try {
            suspendTransaction(mangaBakaDatabase) {
                exec("ATTACH DATABASE '${databaseImportFile.absolutePathString()}' AS import_db;")
            }
        } catch (_: ExposedSQLException) {
            // ignore
        }
        suspendTransaction(mangaBakaDatabase) {
            progressFlow.emit(ProgressEvent(0, 0, "importing series"))
            exec(
                """
                        INSERT INTO series SELECT
                            import.id,
                            UPPER(import.type),
                            import.rating,
                            UPPER(import.status),
                            import.description,
                            import.content_rating,
                            import.has_anime,
                            import.anime_start,
                            import.anime_end,
                            import.is_licensed,
                            import.total_chapters,
                            import.final_volume,
                            import.published_start_date,
                            import.published_start_date_is_estimated,
                            import.published_end_date,
                            import.published_end_date_is_estimated,
                            UPPER(import.state),
                            import.merged_with,
                            import.last_updated_at,
                            import.canonical_url,
                            import.cover_raw_url,
                            import.cover_raw_size,
                            import.cover_raw_width,
                            import.cover_raw_height,
                            import.cover_raw_format,
                            import.cover_raw_blurhash,
                            import.cover_raw_thumbhash,
                            import.cover_x350_x1,
                            import.source_kitsu_id,
                            import.source_kitsu_rating,
                            import.source_kitsu_rating_normalized,
                            import.source_anilist_id,
                            import.source_anilist_rating,
                            import.source_anilist_rating_normalized,
                            import.source_shikimori_id,
                            import.source_shikimori_rating,
                            import.source_shikimori_rating_normalized,
                            import.source_anime_planet_id,
                            import.source_anime_planet_rating,
                            import.source_anime_planet_rating_normalized,
                            import.source_manga_updates_id,
                            import.source_manga_updates_rating,
                            import.source_manga_updates_rating_normalized,
                            import.source_my_anime_list_id,
                            import.source_my_anime_list_rating,
                            import.source_my_anime_list_rating_normalized,
                            import.source_anime_news_network_id,
                            import.source_anime_news_network_rating,
                            import.source_anime_news_network_rating_normalized
                            FROM import_db.series as import WHERE import.state = 'active';
                    """
            )
            progressFlow.emit(ProgressEvent(0, 0, "importing artists"))
            exec(
                """
                        INSERT INTO artists SELECT import.id, json_each.value
                        FROM import_db.series as import, json_each(import.artists) WHERE import.state = 'active';
                    """
            )
            progressFlow.emit(ProgressEvent(0, 0, "importing authors"))
            exec(
                """
                        INSERT INTO authors SELECT import.id, json_each.value
                        FROM import_db.series as import, json_each(import.authors) WHERE import.state = 'active';
                    """
            )
            progressFlow.emit(ProgressEvent(0, 0, "importing links"))
            exec(
                """
                        INSERT INTO links SELECT 
                            import.id,
                            json_each.value ->> '$.name',
                            json_each.value ->> '$.name_display',
                            json_each.value ->> '$.language',
                            UPPER(json_each.value ->> '$.type'),
                            json_each.value ->> '$.url',
                            json_each.value ->> '$.id'
                        FROM import_db.series as import, json_each(import.links_v2) WHERE import.state = 'active';
                    """
            )
            progressFlow.emit(ProgressEvent(0, 0, "importing publishers"))
            exec(
                """
                        INSERT INTO publishers SELECT 
                            import.id,
                            json_each.value ->> '$.name',
                            json_each.value ->> '$.type',
                            json_each.value ->> '$.note'
                        FROM import_db.series as import, json_each(import.publishers) WHERE import.state = 'active';
                    """
            )

            progressFlow.emit(ProgressEvent(0, 0, "importing relationships"))
            exec(
                """
                        INSERT INTO relationships SELECT 
                            import.id,
                            UPPER(json_each.value ->> '$.chronology'),
                            json_each.value ->> '$.is_manual',
                            json_each.value ->> '$.note',
                            UPPER(json_each.value ->> '$.relation_type'),
                            json_each.value ->> '$.id'
                        FROM import_db.series as import, json_each(import.relationships_v2) WHERE import.state = 'active';
                    """
            )

            progressFlow.emit(ProgressEvent(0, 0, "importing titles"))
            exec(
                """
                        INSERT INTO titles SELECT 
                            import.id,
                            json_each.value ->> '$.title',
                            json_each.value ->> '$.language',
                            json_each.value ->> '$.traits',
                            json_each.value ->> '$.is_primary',
                            json_each.value ->> '$.note'
                        FROM import_db.series as import, json_each(import.titles) WHERE import.state = 'active';
                    """
            )

            progressFlow.emit(ProgressEvent(0, 0, "importing tags"))
            exec(
                """
                        INSERT INTO series_tags SELECT 
                            import.id,
                            json_each.value ->> '$.id',
                            json_each.value ->> '$.is_spoiler',
                            json_each.value ->> '$.is_explicit',
                            json_each.value ->> '$.implied_by_tag_ids',
                            UPPER(json_each.value ->> '$.weight')
                        FROM import_db.series as import, json_each(import.tags_v2) WHERE import.state = 'active';
                    """
            )
        }
        try {
            suspendTransaction(mangaBakaDatabase) {
                exec("DETACH DATABASE import_db;")
            }
        } catch (_: ExposedSQLException) {
            // ignore
        }
    }

    private suspend fun createSearchIndex() {
        progressFlow.emit(ProgressEvent(0, 0, "creating search index"))
        transaction(mangaBakaDatabase) {
            exec(
                """
                    CREATE VIRTUAL TABLE titles_fts USING fts5
                    (
                        id,
                        title,
                        type,
                        tokenize = 'trigram'
                    );
                """.trimIndent()
            )

            exec(
                """
                    INSERT INTO titles_fts
                    SELECT t.id, t.title, s.type
                    FROM titles as t
                             LEFT JOIN series as s on t.id = s.id;
                """.trimIndent()
            )
        }
    }

    private suspend fun importTags() {
        progressFlow.emit(ProgressEvent(0, 0, "importing tags"))
        val allTags = mangaBakaApi.getTags()
        transaction(mangaBakaDatabase) {
            TagsTable.batchInsert(
                data = allTags,
                ignore = false,
                shouldReturnGeneratedValues = false
            ) { tag ->
                this[TagsTable.id] = tag.id.value
                this[TagsTable.contentRating] = tag.contentRating.name
                this[TagsTable.description] = tag.description
                this[TagsTable.isSpoiler] = tag.isSpoiler ?: false
                this[TagsTable.level] = tag.level
                this[TagsTable.name] = tag.name
                this[TagsTable.namePath] = tag.namePath
                this[TagsTable.parentId] = tag.parentId?.value
                this[TagsTable.seriesCount] = tag.seriesCount
                this[TagsTable.isGenre] = tag.isGenre
                this[TagsTable.mergedWith] = tag.mergedWith
            }
        }
    }

    private suspend fun getDbMetadata(): MangaBakaImportMetadata? {
        return transaction(mangaBakaDatabase) {
            val timestamp = ImportMetadataTable.selectAll()
                .where { ImportMetadataTable.key.eq("download_date") }.firstOrNull()
                ?.let { Instant.parse(it[ImportMetadataTable.value]) }

            val checksum = ImportMetadataTable.selectAll()
                .where { ImportMetadataTable.key.eq("checksum") }.firstOrNull()
                ?.let { it[ImportMetadataTable.value] }

            if (timestamp == null || checksum == null) null
            else MangaBakaImportMetadata(timestamp, checksum)
        }
    }

    private suspend fun saveDbMetadata(metadata: MangaBakaImportMetadata) {
        transaction(mangaBakaDatabase) {
            ImportMetadataTable.upsert {
                it[ImportMetadataTable.key] = "download_date"
                it[ImportMetadataTable.value] = metadata.timestamp.toString()
            }
            ImportMetadataTable.upsert {
                it[ImportMetadataTable.key] = "checksum"
                it[ImportMetadataTable.value] = metadata.checksum
            }
        }
    }
}
