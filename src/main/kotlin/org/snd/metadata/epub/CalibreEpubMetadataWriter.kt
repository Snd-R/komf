package org.snd.metadata.epub

import org.snd.common.exceptions.ValidationException
import org.snd.metadata.model.metadata.Author
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.ReleaseDate
import org.snd.metadata.model.metadata.SeriesMetadata
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.extension
import kotlin.io.path.isWritable

class CalibreEpubMetadataWriter(
    private val executablePath: Path?
) {
    fun writeMetadata(bookPath: Path, seriesMetadata: SeriesMetadata, bookMetadata: BookMetadata?) {
        val arguments = listOfNotNull(
            (bookMetadata?.authors?.ifEmpty { null } ?: seriesMetadata.authors.ifEmpty { null })
                ?.let { "--authors" to authors(it) },
            bookMetadata?.summary?.let { "--comments" to it },
            bookMetadata?.releaseDate?.let { "--date" to it.toString() },
            bookMetadata?.isbn?.let { "--isbn" to it },
            bookMetadata?.number?.let { "--index" to it.start.toString() },
            seriesMetadata.publisher?.let { "--publisher" to it },
            seriesMetadata.title?.let { "--series" to it.name },
            seriesMetadata.genres.ifEmpty { null }?.let { "--tags" to it.joinToString(",") }
        ).flatMap { (k, v) -> listOf(k, v) }

        writeMetadata(bookPath, arguments)
    }

    fun writeSeriesMetadata(bookPath: Path, seriesMetadata: SeriesMetadata, bookMetadata: BookMetadata?) {
        val arguments = listOfNotNull(
            (seriesMetadata.authors.ifEmpty { null } ?: bookMetadata?.authors?.ifEmpty { null })
                ?.let { "--authors" to authors(it) },
            seriesMetadata.summary?.let { "--comments" to it },
            (seriesMetadata.releaseDate?.let { parseSeriesReleaseDate(it) } ?: bookMetadata?.releaseDate)
                ?.let { "--date" to it.toString() },
            bookMetadata?.isbn?.let { "--isbn" to it },
            bookMetadata?.number?.let { "--index" to it.start.toString() },
            seriesMetadata.publisher?.let { "--publisher" to it },
            seriesMetadata.title?.let { "--series" to it.name },
            seriesMetadata.genres.ifEmpty { null }?.let { "--tags" to it.joinToString(",") }
        ).flatMap { (k, v) -> listOf(k, v) }

        writeMetadata(bookPath, arguments)
    }

    private fun writeMetadata(bookPath: Path, arguments: List<String>) {
        validate(bookPath)
        if (arguments.isEmpty()) return

        val process = ProcessBuilder(
            executablePath?.toString() ?: "ebook-meta",
            bookPath.toString(),
            *arguments.toTypedArray()
        ).inheritIO().start()

        val exitCode = process.waitFor()
        if (exitCode != 0) throw IllegalStateException("process returned non zero exit code")
    }

    private fun authors(authors: List<Author>) = authors.map { it.name }.distinct().joinToString("&")

    private fun validate(path: Path) {
        if (path.extension.lowercase() != "epub") {
            throw ValidationException("Unsupported file extension $path")
        }
        if (!path.isWritable()) {
            throw ValidationException("No write permission for file $path")
        }
    }

    private fun parseSeriesReleaseDate(releaseDate: ReleaseDate): LocalDate? {
        if (releaseDate.day != null && releaseDate.month != null && releaseDate.year != null) {
            return LocalDate.of(releaseDate.year, releaseDate.month, releaseDate.day)
        }
        return null
    }
}