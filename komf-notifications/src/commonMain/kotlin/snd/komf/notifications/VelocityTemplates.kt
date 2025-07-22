package snd.komf.notifications

import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.runtime.RuntimeInstance
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.Path
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.notExists
import kotlin.io.path.writeBytes

internal object VelocityTemplates {

    fun NotificationContext.toVelocityContext(): VelocityContext {
        val context = VelocityContext()
        context.put("library", library)
        context.put("series", series)
        context.put("books", books.sortedBy { it.name })
        context.put("mediaServer", mediaServer)
        return context
    }

    fun RuntimeInstance.loadTemplateByName(name: String): Template? {
        if (getLoaderNameForResource(name) == null) return null
        return runCatching { getTemplate(name) }.getOrNull()
    }

    fun renderTemplate(template: Template, context: VelocityContext): String {
        return StringWriter().use {
            template.merge(context, it)
            it.toString()
        }
    }

    fun RuntimeInstance.templateFromString(template: String): Template {
        val runtimeService = this
        return Template().apply {
            setRuntimeServices(runtimeService)
            data = runtimeService.parse(StringReader(template), this)
            initDocument()
        }
    }

    fun RuntimeInstance.templateWriteAndGet(stringTemplate: String?, file: Path): Template? {
        if (stringTemplate == null || stringTemplate.isBlank()) {
            file.deleteIfExists()
            return null
        } else {
            if (file.notExists()) file.createFile()
            file.writeBytes(stringTemplate.toByteArray(Charsets.UTF_8), TRUNCATE_EXISTING)
            return templateFromString(stringTemplate)
        }

    }
}