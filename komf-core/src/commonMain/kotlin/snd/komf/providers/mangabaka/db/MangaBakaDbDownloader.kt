package snd.komf.providers.mangabaka.db

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.IOUtils
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import snd.komf.providers.mangabaka.db.MangaBakaDownloadProgress.FinishedEvent
import snd.komf.providers.mangabaka.db.MangaBakaDownloadProgress.ProgressEvent
import java.io.BufferedInputStream
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

private const val DOWNLOAD_BUFFER_SIZE = 1024L * 1024L
private val logger = KotlinLogging.logger { }

class MangaBakaDbDownloader(
    private val ktor: HttpClient,
    private val databaseArchive: Path,
    private val databaseFile: Path,
    private val dbMetadata: MangaBakaDbMetadata,
) {
    private val databaseUrl = "https://api.mangabaka.dev/v1/database/series.sqlite.tar.gz"
    private val checksumUrl = "https://api.mangabaka.dev/v1/database/series.sqlite.tar.gz.sha1"

    private val progressFlow = MutableSharedFlow<MangaBakaDownloadProgress>(
        replay = 1,
        extraBufferCapacity = 1000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val downloadMutex = Mutex()
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun launchDownload(): Flow<MangaBakaDownloadProgress> {
        if (downloadMutex.tryLock()) {
            progressFlow.resetReplayCache()
            downloadScope.launch { doDownload(lockedMutex = downloadMutex) }
        }

        return progressFlow
    }

    private suspend fun doDownload(lockedMutex: Mutex) {
        try {
            progressFlow.emit(ProgressEvent(0, 0, checksumUrl))
            val newChecksum = ktor.get(checksumUrl).bodyAsText()
            if (databaseFile.exists() && dbMetadata.isValid() && dbMetadata.checksum == newChecksum) {
                progressFlow.emit(FinishedEvent)
                return
            }
            dbMetadata.delete()
            databaseFile.deleteIfExists()
            databaseArchive.deleteIfExists()

            downloadDatabaseArchive()
            extractDatabaseFile()
            createSearchIndex()

            dbMetadata.setTimestamp(Clock.System.now())
            dbMetadata.setChecksum(newChecksum)
            databaseArchive.deleteIfExists()
            progressFlow.emit(FinishedEvent)

        } catch (e: Exception) {
            logger.catching(e)
            databaseArchive.deleteIfExists()
            databaseFile.deleteIfExists()
            dbMetadata.delete()
            progressFlow.emit(
                MangaBakaDownloadProgress.ErrorEvent("${e::class.simpleName}: ${e.message}")
            )
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

            databaseArchive.outputStream().buffered().use { outputStream ->
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
        progressFlow.emit(ProgressEvent(0, 0, "extracting $databaseArchive"))
        TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(databaseArchive.inputStream())))
            .use { archiveStream ->
                // take only first entry
                archiveStream.nextEntry
                IOUtils.copyLarge(archiveStream, databaseFile.outputStream())
            }
    }

    private suspend fun createSearchIndex() {
        progressFlow.emit(ProgressEvent(0, 0, "creating search index"))
        val db = Database.connect("jdbc:sqlite:$databaseFile")
        transaction(db) {
            exec("CREATE VIRTUAL TABLE series_fts USING fts5(id, title, type, tokenize = 'trigram');")
            exec("INSERT INTO series_fts SELECT id, title, type FROM series WHERE state='active';")
        }
    }
}

@Serializable
sealed interface MangaBakaDownloadProgress {
    @Serializable
    data class ProgressEvent(
        val total: Long,
        val completed: Long,
        val info: String? = null,
    ) : MangaBakaDownloadProgress

    @Serializable
    data object FinishedEvent : MangaBakaDownloadProgress

    @Serializable
    data class ErrorEvent(val message: String) : MangaBakaDownloadProgress
}

