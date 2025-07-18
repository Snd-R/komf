package snd.komf.notifications.apprise

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.tika.config.TikaConfig
import org.apache.tika.metadata.Metadata
import snd.komf.model.Image
import snd.komf.notifications.NotificationContext
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeBytes

private val logger = KotlinLogging.logger {}

class AppriseCliService(
    private val urls: Collection<String>,
    private val templateRenderer: AppriseVelocityTemplates,
    private val seriesCover: Boolean
) {
    private val tikaConfig = TikaConfig.getDefaultConfig()

    fun send(
        context: NotificationContext,
        templates: AppriseStringTemplates? = null,
    ) {
        var coverAttachment: Path? = null
        try {
            if (urls.isEmpty()) return

            coverAttachment = getCoverAttachment(context)

            val renderResult = templates
                ?.let { templateRenderer.render(context, it) }
                ?: templateRenderer.render(context)

            val arguments = listOfNotNull(
                renderResult.title?.let { "-t" to it },
                "-b" to renderResult.body,
                coverAttachment?.let { "--attach" to it.absolutePathString() },
            ).flatMap { (k, v) -> listOf(k, v) }.plus(urls)

            val process = ProcessBuilder("apprise", *arguments.toTypedArray())
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            val statusCode = process.waitFor()
            if (statusCode != 0) {
                val errorMessage = process.errorStream.bufferedReader().readText()
                logger.error { errorMessage }
                error("Apprise returned non zero exit code: $errorMessage")
            }
        } finally {
            coverAttachment?.deleteIfExists()
        }
    }

    private fun getCoverAttachment(context: NotificationContext): Path? {
        if (!seriesCover) return null
        val cover = context.seriesCover ?: return null

        val tmpFile = createTempFile(
            prefix = "${context.series.name}_",
            suffix = "_${getFileExtension(cover)}"
        )
        tmpFile.writeBytes(cover.bytes)
        return tmpFile
    }

    private fun getFileExtension(image: Image): String {
        val parsedExtension = runCatching {
            if (image.mimeType != null) {
                tikaConfig.mimeRepository.forName(image.mimeType).extension
            } else {
                val mediaType = tikaConfig.mimeRepository.detect(image.bytes.inputStream(), Metadata())
                tikaConfig.mimeRepository.forName(mediaType.toString()).extension
            }
        }.onFailure { logger.catching(it) }.getOrNull()

        return parsedExtension ?: ".jpg"
    }
}