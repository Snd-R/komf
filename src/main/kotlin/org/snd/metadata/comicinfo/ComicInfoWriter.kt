package org.snd.metadata.comicinfo

import mu.KotlinLogging
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlDeclMode.Charset
import nl.adaptivity.xmlutil.core.XmlVersion.XML10
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.IOUtils
import org.snd.infra.ValidationException
import org.snd.metadata.MetadataMerger.mergeComicInfoMetadata
import org.snd.metadata.comicinfo.model.ComicInfo
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.extension
import kotlin.io.path.isWritable
import kotlin.io.path.moveTo


private const val COMIC_INFO = "ComicInfo.xml"
private val logger = KotlinLogging.logger {}

class ComicInfoWriter {

    @OptIn(ExperimentalXmlUtilApi::class)
    private val xml = XML {
        indent = 2
        xmlDeclMode = Charset
        xmlVersion = XML10
        unknownChildHandler = UnknownChildHandler { _, inputKind, descriptor, name, _ ->
            logger.warn { "Unknown Field: ${descriptor.tagName}/${name ?: "<CDATA>"} ($inputKind)" }
            emptyList()
        }
    }

    private val supportedExtensions = setOf("cbz", "zip")

    fun writeMetadata(archivePath: Path, comicInfo: ComicInfo) {
        validate(archivePath)

        val tempFile = createTempFile(archivePath.parent)
        runCatching {
            ZipFile(archivePath.toFile()).use { zip ->
                val oldComicInfo = getComicInfo(zip)
                val comicInfoToWrite = oldComicInfo?.let { old -> mergeComicInfoMetadata(old, comicInfo) }
                    ?: comicInfo
                if (oldComicInfo == comicInfoToWrite) {
                    tempFile.deleteIfExists()
                    return
                }

                ZipArchiveOutputStream(tempFile).use { output ->
                    copyEntries(zip, output)
                    putComicInfoEntry(comicInfoToWrite, output)
                }
            }
            tempFile.moveTo(archivePath, overwrite = true)
        }.onFailure {
            tempFile.deleteIfExists()
            throw it
        }
    }

    private fun getComicInfo(zipFile: ZipFile): ComicInfo? {
        return zipFile.entries.asSequence()
            .firstOrNull { it.name == COMIC_INFO }
            ?.let {
                zipFile.getInputStream(it).use { stream ->
                    xml.decodeFromString(ComicInfo.serializer(), stream.readAllBytes().toString(UTF_8))
                }
            }
    }

    private fun copyEntries(file: ZipFile, output: ZipArchiveOutputStream) {
        file.entries.asSequence()
            .filter { it.name != COMIC_INFO }
            .forEach { entry ->
                output.putArchiveEntry(entry)
                IOUtils.copyLarge(file.getInputStream(entry), output, ByteArray(8192))
                output.closeArchiveEntry()
            }
    }

    private fun putComicInfoEntry(comicInfo: ComicInfo, output: ZipArchiveOutputStream) {
        output.putArchiveEntry(ZipArchiveEntry(COMIC_INFO))
        val comicInfoXml = xml.encodeToString(ComicInfo.serializer(), comicInfo)
        IOUtils.copy(comicInfoXml.byteInputStream(), output)
        output.closeArchiveEntry()
    }

    private fun validate(path: Path) {
        if (!supportedExtensions.contains(path.extension)) {
            throw ValidationException("Unsupported file extension $path")
        }
        if (!path.isWritable()) {
            throw ValidationException("No write permission for file $path")
        }
    }
}

