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
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.readText

private const val DOWNLOAD_BUFFER_SIZE = 1024L * 1024L
private val logger = KotlinLogging.logger { }

class MangaBakaDbDownloader(
    private val ktor: HttpClient,
    downloadDirectory: Path,
) {
    private val databaseArchive = downloadDirectory.resolve("mangabaka.tar.gz")
    private val databaseFile = downloadDirectory.resolve("mangabaka.sqlite")
    private val checksumFile = downloadDirectory.resolve("checksum.sha1")
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
            progressFlow.emit(ProgressEvent(0, 0, "downloading $checksumUrl"))
            val newChecksum = ktor.get(checksumUrl).bodyAsText()
            if (databaseFile.exists() && checksumFile.exists() && checksumFile.readText() == newChecksum) {
                return
            }
            checksumFile.deleteIfExists()
            databaseFile.deleteIfExists()
            databaseArchive.deleteIfExists()

            downloadDatabaseArchive()
            extractDatabaseFile()
            createSearchIndex()

            Files.writeString(checksumFile, newChecksum)
            databaseArchive.deleteIfExists()
            progressFlow.emit(FinishedEvent)

        } catch (e: Exception) {
            logger.catching(e)
            databaseArchive.deleteIfExists()
            checksumFile.deleteIfExists()
            databaseFile.deleteIfExists()
            progressFlow.emit(
                MangaBakaDownloadProgress.ErrorEvent(
                    e.message ?: "Encountered unexpected error during database download"
                )
            )
        } finally {
            lockedMutex.unlock()
        }
    }

    private suspend fun downloadDatabaseArchive() {
        val downloadText = "downloading $databaseUrl"
        progressFlow.emit(ProgressEvent(0, 0, downloadText))
        ktor.prepareGet(databaseUrl).execute { response ->
            val length = response.headers["Content-Length"]?.toLong() ?: 0L
            progressFlow.emit(ProgressEvent(length, 0, downloadText))
            val channel = response.bodyAsChannel().counted()

            databaseArchive.outputStream().buffered().use { outputStream ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DOWNLOAD_BUFFER_SIZE)
                    while (!packet.exhausted()) {
                        outputStream.write(packet.readByteArray())
                    }
                    progressFlow.emit(ProgressEvent(length, channel.totalBytesRead, downloadText))
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
            exec("CREATE VIRTUAL TABLE series_fts USING fts5(id, title, tokenize = 'trigram');")
            exec("INSERT INTO series_fts SELECT id, title FROM series WHERE state='active';")
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

