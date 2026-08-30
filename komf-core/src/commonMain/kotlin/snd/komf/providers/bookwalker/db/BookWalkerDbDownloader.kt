package snd.komf.providers.bookwalker.db

import com.github.luben.zstd.ZstdInputStream
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
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
import org.apache.commons.io.IOUtils
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import snd.komf.model.DOWNLOAD_BUFFER_SIZE
import snd.komf.model.DownloadProgress
import snd.komf.model.DownloadProgress.FinishedEvent
import snd.komf.model.DownloadProgress.ProgressEvent
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.text.Charsets.UTF_8
import kotlin.time.Clock
import kotlin.time.Instant

private val logger = KotlinLogging.logger { }

class BookWalkerDbDownloader(
    private val ktor: HttpClient,
    private val databaseWorkDirectory: Path,
    private val databaseFile: Path,
    private val onStateRefresh: suspend () -> Unit,
) {
    private val databaseArchive = databaseWorkDirectory.resolve("bkwk-db.sqlite.zst")
    private val databaseUrl = "https://static.bookwalker.com/data/bkwk-db.sqlite.zst"
    private val progressFlow = MutableSharedFlow<DownloadProgress>(
        replay = 1,
        extraBufferCapacity = 1000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val downloadMutex = Mutex()
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val timestampFile = databaseWorkDirectory.resolve("timestamp")

    @Volatile
    var downloadTimestamp: Instant? = runCatching { Instant.parse(timestampFile.readText()) }.getOrNull()
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    fun launchDownload(): Flow<DownloadProgress> {
        if (downloadMutex.tryLock()) {
            progressFlow.resetReplayCache()
            downloadScope.launch {
                try {
                    doDownload()
                } finally {
                    downloadMutex.unlock()
                }
            }
        }

        return progressFlow
    }

    private suspend fun doDownload() {
        try {

            progressFlow.emit(ProgressEvent(0, 0, databaseUrl))
            databaseFile.deleteIfExists()
            databaseFile.createParentDirectories()

            downloadDatabaseArchive()
            extractDatabaseFile()
            createSearchIndex()

            val now = Clock.System.now()
            downloadTimestamp = now
            timestampFile.writeText(now.toString(), UTF_8, CREATE, WRITE, TRUNCATE_EXISTING)

            databaseArchive.deleteIfExists()
            progressFlow.emit(FinishedEvent)
            onStateRefresh()
        } catch (e: Exception) {
            logger.catching(e)
            databaseArchive.deleteIfExists()
            databaseFile.deleteIfExists()
            progressFlow.emit(
                DownloadProgress.ErrorEvent("${e::class.simpleName}: ${e.message}")
            )
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
        val zstdInput = ZstdInputStream(databaseArchive.inputStream())
        IOUtils.copyLarge(zstdInput, databaseFile.outputStream())

    }

    private suspend fun createSearchIndex() {
        progressFlow.emit(ProgressEvent(0, 0, "creating search index"))
        val db = Database.connect("jdbc:sqlite:$databaseFile")
        transaction(db) {
            exec(
                """
                    CREATE VIRTUAL TABLE series_fts USING fts5
                    (
                        id,
                        titles,
                        type,
                        tokenize = 'trigram'
                    );
                """.trimIndent()
            )

            exec(
                """
                    INSERT INTO series_fts
                    SELECT s.id,
                           GROUP_CONCAT(json_each.value, ', '),
                           s.type
                    FROM series s, json_each(alt_titles)
                    GROUP BY s.id;
                """.trimIndent()
            )
        }
    }
}
