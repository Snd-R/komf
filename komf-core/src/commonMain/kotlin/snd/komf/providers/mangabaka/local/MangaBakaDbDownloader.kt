package snd.komf.providers.mangabaka.local

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.counted
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.IOUtils
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
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

    fun launchDownload(): Flow<MangaBakaDownloadProgress> {
        return flow {
            runCatching {
                emit(MangaBakaDownloadProgress(0, 0, "downloading $checksumUrl"))
                val newChecksum = ktor.get(checksumUrl).bodyAsText()
                if (databaseFile.exists() && checksumFile.exists() && checksumFile.readText() == newChecksum) {
                    return@flow
                }
                checksumFile.deleteIfExists()
                databaseFile.deleteIfExists()
                databaseArchive.deleteIfExists()

                downloadDatabaseArchive()
                extractDatabaseFile()
                createSearchIndex()

                Files.writeString(checksumFile, newChecksum)
                databaseArchive.deleteIfExists()
            }.onFailure {
                logger.catching(it)
                databaseArchive.deleteIfExists()
                checksumFile.deleteIfExists()
                databaseFile.deleteIfExists()
                throw it
            }
        }.flowOn(Dispatchers.IO)
    }

    private suspend fun FlowCollector<MangaBakaDownloadProgress>.downloadDatabaseArchive() {
        val downloadText = "downloading $databaseUrl"
        emit(MangaBakaDownloadProgress(0, 0, downloadText))
        ktor.prepareGet(databaseUrl).execute { response ->
            val length = response.headers["Content-Length"]?.toLong() ?: 0L
            emit(MangaBakaDownloadProgress(length, 0, downloadText))
            val channel = response.bodyAsChannel().counted()

            databaseArchive.outputStream().buffered().use { outputStream ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DOWNLOAD_BUFFER_SIZE)
                    while (!packet.exhausted()) {
                        outputStream.write(packet.readByteArray())
                    }
                    emit(MangaBakaDownloadProgress(length, channel.totalBytesRead, downloadText))
                }
                outputStream.flush()
            }
        }
    }

    private suspend fun FlowCollector<MangaBakaDownloadProgress>.extractDatabaseFile() {
        emit(MangaBakaDownloadProgress(0, 0, "extracting $databaseArchive"))
        TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(databaseArchive.inputStream())))
            .use { archiveStream ->
                // take only first entry
                archiveStream.nextEntry
                IOUtils.copyLarge(archiveStream, databaseFile.outputStream())
            }
    }

    private suspend fun FlowCollector<MangaBakaDownloadProgress>.createSearchIndex() {
        emit(MangaBakaDownloadProgress(0, 0, "creating search index"))
        val db = Database.connect("jdbc:sqlite:$databaseFile")
        transaction(db) {
            exec("CREATE VIRTUAL TABLE title_search USING fts5(id, title, tokenize = 'trigram');")
            exec("INSERT INTO title_search SELECT id, title FROM series WHERE state='active';")
        }
    }
}

@Serializable
data class MangaBakaDownloadProgress(
    val total: Long,
    val completed: Long,
    val info: String? = null,
)
