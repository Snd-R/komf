package snd.komf.notifications.apprise

import io.github.oshai.kotlinlogging.KotlinLogging
import snd.komf.notifications.discord.model.NotificationContext

private val logger = KotlinLogging.logger {}

class AppriseCliService(
    private val urls: Collection<String>,
    private val templateRenderer: AppriseVelocityTemplates,
) {
    fun send(
        context: NotificationContext,
        templates: AppriseStringTemplates? = null,
    ) {
        if (urls.isEmpty()) return

        val renderResult = templates
            ?.let { templateRenderer.render(context, it) }
            ?: templateRenderer.render(context)

        val arguments = listOfNotNull(
            renderResult.title?.let { "-t" to it },
            "-b" to renderResult.body
        )
            .flatMap { (k, v) -> listOf(k, v) }
            .plus(urls)

        val process = ProcessBuilder(
            "apprise",
            *arguments.toTypedArray()
        )
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        val statusCode = process.waitFor()
        if (statusCode != 0) {
            val errorMessage = process.errorStream.bufferedReader().readText()
            logger.error { errorMessage }
            error("Apprise returned non zero exit code: $errorMessage")
        }
    }
}