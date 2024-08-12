package snd.komf.comicinfo

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.IOUtils
import snd.komf.comicinfo.ComicInfoWriter.ComicInfoException
import snd.komf.comicinfo.model.ComicInfo
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.util.zip.Deflater.NO_COMPRESSION
import java.util.zip.ZipEntry
import kotlin.io.path.*

private const val COMIC_INFO = "ComicInfo.xml"
private val logger = KotlinLogging.logger {}

class JvmComicInfoWriter private constructor(private val overrideComicInfo: Boolean) : ComicInfoWriter {

    @OptIn(ExperimentalXmlUtilApi::class, XmlUtilInternal::class)
    private val xml = XML {
        indent = 2
        xmlDeclMode = XmlDeclMode.Charset
        xmlVersion = XmlVersion.XML10
        policy = DefaultXmlSerializationPolicy.Builder()
            .apply {
                pedantic = false
                autoPolymorphic = false
                encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.ANNOTATED
                unknownChildHandler = UnknownChildHandler { _, inputKind, descriptor, name, _ ->
                    logger.warn { "Unknown Field: ${descriptor.tagName}/${name ?: "<CDATA>"} ($inputKind)" }
                    emptyList()
                }
            }.build()
    }

    private val supportedExtensions = setOf("cbz", "zip")

    override fun removeComicInfo(localPath: String) {
        val archivePath = Path.of(localPath)
        validate(archivePath)

        val tempFile = createTempFile(archivePath.parent)
        runCatching {
            ZipFile(archivePath.toFile()).use { zip ->
                if (zip.entries.asSequence().none { it.name == COMIC_INFO }) {
                    tempFile.deleteIfExists()
                    return
                }

                ZipArchiveOutputStream(tempFile).use { output ->
                    output.setLevel(NO_COMPRESSION)
                    copyEntries(zip, output)
                }
            }

            copyPermissions(from = archivePath, to = tempFile)
            tempFile.moveTo(archivePath, overwrite = true)
        }.onFailure {
            tempFile.deleteIfExists()
            throw it
        }
    }

    override fun writeMetadata(localPath: String, comicInfo: ComicInfo) {
        val archivePath = Path.of(localPath)
        validate(archivePath)

        val tempFile = createTempFile(archivePath.parent)
        runCatching {
            ZipFile.builder()
                .setFile(archivePath.toFile())
                .get()
                .use { zip ->
                    val oldComicInfo = getComicInfo(zip)

                    val comicInfoToWrite =
                        if (overrideComicInfo) comicInfo
                        else oldComicInfo?.let { old -> mergeComicInfoMetadata(old, comicInfo) } ?: comicInfo

                    if (oldComicInfo == comicInfoToWrite) {
                        tempFile.deleteIfExists()
                        return
                    }

                    ZipArchiveOutputStream(tempFile).use { output ->
                        output.setLevel(NO_COMPRESSION)
                        copyEntries(zip, output)
                        putComicInfoEntry(comicInfoToWrite, output)
                    }
                }

            copyPermissions(from = archivePath, to = tempFile)
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
                file.getInputStream(entry).use { entryStream ->
                    output.putArchiveEntry(entry)
                    IOUtils.copyLarge(entryStream, output, ByteArray(4096))
                    output.closeArchiveEntry()
                }
            }
    }

    private fun putComicInfoEntry(comicInfo: ComicInfo, output: ZipArchiveOutputStream) {
        output.putArchiveEntry(ZipArchiveEntry(COMIC_INFO).apply { method = ZipEntry.STORED })
        val comicInfoXml = xml.encodeToString(ComicInfo.serializer(), comicInfo)
        IOUtils.copy(comicInfoXml.byteInputStream(), output)
        output.closeArchiveEntry()
    }

    private fun validate(path: Path) {
        if (!supportedExtensions.contains(path.extension.lowercase())) {
            throw ComicInfoException("Unsupported file extension $path")
        }
        if (!path.isWritable()) {
            throw ComicInfoException("No write permission for file $path")

        }
    }

    private fun copyPermissions(from: Path, to: Path) {
        runCatching { from.getPosixFilePermissions() }.getOrNull()
            ?.let { to.setPosixFilePermissions(it) }
    }

    private fun mergeComicInfoMetadata(old: ComicInfo, new: ComicInfo): ComicInfo {
        return ComicInfo(
            title = new.title ?: old.title,
            series = new.series ?: old.series,
            number = new.number ?: old.number,
            count = new.count ?: old.count,
            volume = new.volume ?: old.volume,
            alternateSeries = new.alternateSeries ?: old.alternateSeries,
            alternateNumber = new.alternateNumber ?: old.alternateNumber,
            alternateCount = new.alternateCount ?: old.alternateCount,
            summary = new.summary ?: old.summary,
            notes = new.notes ?: old.notes,
            year = new.year ?: old.year,
            month = new.month ?: old.month,
            day = new.day ?: old.day,
            writer = new.writer ?: old.writer,
            penciller = new.penciller ?: old.penciller,
            inker = new.inker ?: old.inker,
            colorist = new.colorist ?: old.colorist,
            letterer = new.letterer ?: old.letterer,
            coverArtist = new.coverArtist ?: old.coverArtist,
            editor = new.editor ?: old.editor,
            translator = new.translator ?: old.translator,
            publisher = new.publisher ?: old.publisher,
            imprint = new.imprint ?: old.imprint,
            genre = new.genre ?: old.genre,
            tags = new.tags ?: old.tags,
            web = new.web ?: old.web,
            pageCount = new.pageCount ?: old.pageCount,
            languageISO = new.languageISO ?: old.languageISO,
            format = new.format ?: old.format,
            blackAndWhite = new.blackAndWhite ?: old.blackAndWhite,
            manga = new.manga ?: old.manga,
            characters = new.characters ?: old.characters,
            teams = new.teams ?: old.teams,
            locations = new.locations ?: old.locations,
            scanInformation = new.scanInformation ?: old.scanInformation,
            storyArc = new.storyArc ?: old.storyArc,
            seriesGroup = new.seriesGroup ?: old.seriesGroup,
            ageRating = new.ageRating ?: old.ageRating,
            rating = new.rating ?: old.rating,
            localizedSeries = new.localizedSeries ?: old.localizedSeries,
            pages = new.pages ?: old.pages
        )
    }

    companion object {
        private val withComicInfoOverride: ComicInfoWriter = JvmComicInfoWriter(true)
        private val withComicInfoAppend: ComicInfoWriter = JvmComicInfoWriter(false)

        fun getInstance(overrideComicInfo: Boolean): ComicInfoWriter {
            return when (overrideComicInfo) {
                true -> withComicInfoOverride
                false -> withComicInfoAppend
            }
        }
    }
}