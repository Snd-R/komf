package snd.komf.notifications.apprise

import snd.komf.notifications.discord.model.NotificationContext
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class AppriseCliService(
    private val urls: Collection<String>,
    private val templateRenderer: AppriseVelocityTemplates,
) {
    private val appriseExecutablePath: Path?

    init {
        appriseExecutablePath = runCatching {
            val path = if (System.getProperty("os.name").startsWith("Win")) {
                executeCommandAndReturnOutput("where", "apprise.exe")
            } else {
                executeCommandAndReturnOutput("which", "apprise")
            }
            path?.let { Path.of(it.trim()) }

        }.getOrNull()
    }

    private fun executeCommandAndReturnOutput(vararg command: String) = runCatching {
        val proc = ProcessBuilder(*command)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        proc.waitFor()
        proc.inputStream.bufferedReader().readText()
    }.getOrNull()

    fun send(
        context: NotificationContext,
        templates: AppriseStringTemplates? = null,
    ) {
        if (urls.isEmpty()) return
        requireNotNull(appriseExecutablePath) { "Could not find apprise executable" }

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
            appriseExecutablePath.absolutePathString(),
            *arguments.toTypedArray()
        ).inheritIO().start()

        require(process.waitFor() == 0) { "Apprise returned non zero exit code" }
    }
}