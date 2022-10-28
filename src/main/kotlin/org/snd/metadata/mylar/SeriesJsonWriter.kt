package org.snd.metadata.mylar

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import org.apache.commons.io.FileUtils
import org.snd.infra.ValidationException
import org.snd.metadata.MetadataMerger.mergeMylarMetadata
import org.snd.metadata.mylar.model.MylarMetadata
import org.snd.metadata.mylar.model.MylarSeries
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isWritable
import kotlin.io.path.readText

private const val SERIES_JSON = "series.json"

class SeriesJsonWriter(
    private val moshi: Moshi
) {

    fun write(path: Path, metadata: MylarMetadata) {
        val seriesJsonPath = path.resolve(SERIES_JSON)
        validate(seriesJsonPath)

        val oldMetadata = if (seriesJsonPath.exists()) moshi.adapter<MylarSeries>().fromJson(seriesJsonPath.readText())
        else null

        if (metadata == oldMetadata?.metadata) return

        val metadataToWrite = oldMetadata?.let { old -> mergeMylarMetadata(old.metadata, metadata) }
            ?: metadata
        val json = moshi.adapter<MylarSeries>().indent("  ").toJson(MylarSeries(metadataToWrite))
        FileUtils.writeStringToFile(seriesJsonPath.toFile(), json, UTF_8)

    }

    private fun validate(path: Path) {
        if (path.isDirectory() || (path.exists() && !path.isWritable())) {
            throw ValidationException("No write permission for file $path")
        }
    }
}