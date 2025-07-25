package snd.komf.providers.mangabaka.db

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Instant
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText


class MangaBakaDbMetadata(
    private val timestampFile: Path,
    private val checksumFile: Path,
) {
    private val logger = KotlinLogging.logger { }

    @Volatile
    var timestamp: Instant? = null
        private set

    @Volatile
    var checksum: String? = null
        private set

    init {
        timestamp = runCatching { Instant.parse(timestampFile.readText()) }
            .onFailure { logger.warn { "failed to find MangaBaka timestamp file" } }
            .getOrNull()
        checksum = runCatching { checksumFile.readText() }
            .onFailure { logger.warn { "failed to find MangaBaka checksum file" } }
            .getOrNull()
    }

    fun setTimestamp(timestamp: Instant) {
        this.timestamp = timestamp
        Files.writeString(timestampFile, timestamp.toString())
    }

    fun setChecksum(checksum: String) {
        this.checksum = checksum
        Files.writeString(checksumFile, checksum)
    }

    fun isValid() = timestamp != null && checksum != null

    fun delete() {
        timestampFile.deleteIfExists()
        checksumFile.deleteIfExists()
        timestamp = null
        checksum = null
    }
}