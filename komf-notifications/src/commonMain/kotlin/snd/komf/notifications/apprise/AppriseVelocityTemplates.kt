package snd.komf.notifications.apprise

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.velocity.Template
import org.apache.velocity.runtime.RuntimeInstance
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import snd.komf.notifications.NotificationContext
import snd.komf.notifications.VelocityTemplates.loadTemplateByName
import snd.komf.notifications.VelocityTemplates.renderTemplate
import snd.komf.notifications.VelocityTemplates.templateFromString
import snd.komf.notifications.VelocityTemplates.templateWriteAndGet
import snd.komf.notifications.VelocityTemplates.toVelocityContext
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.readText


data class AppriseRenderResult(
    val title: String?,
    val body: String,
)

data class AppriseStringTemplates(
    val titleTemplate: String?,
    val bodyTemplate: String?,
)

private const val titleFileName = "apprise_title.vm"
private const val bodyFileName = "apprise_body.vm"

class AppriseVelocityTemplates(
    templatesBaseDirectory: String
) {
    private val appriseDirectory = Path(templatesBaseDirectory).resolve("apprise")

    private val velocityEngine = RuntimeInstance().apply {
        val properties = Properties()
        properties.setProperty("resource.loaders", "file,class")
        properties.setProperty("resource.loader.file.path", appriseDirectory.absolutePathString())
        properties.setProperty("resource.loader.class.class", ClasspathResourceLoader::class.java.name)
        init(properties)
    }
    private val templateWriteMutex = Mutex()
    private val titleTemplate: AtomicRef<Template?>
    private val bodyTemplate: AtomicRef<Template?>

    init {
        val titleTemplate = velocityEngine.loadTemplateByName(titleFileName)
        val bodyTemplate = velocityEngine.loadTemplateByName(bodyFileName)

        this.titleTemplate = atomic(titleTemplate)
        this.bodyTemplate = atomic(bodyTemplate)
    }

    fun render(context: NotificationContext): AppriseRenderResult {
        return render(
            context = context,
            titleTemplate = titleTemplate.value,
            bodyTemplate = bodyTemplate.value
        )
    }

    fun render(
        context: NotificationContext,
        templates: AppriseStringTemplates,
    ): AppriseRenderResult {
        return render(
            context,
            templates.titleTemplate?.let { velocityEngine.templateFromString(it) },
            templates.bodyTemplate?.let { velocityEngine.templateFromString(it) }
        )
    }

    private fun render(
        context: NotificationContext,
        titleTemplate: Template?,
        bodyTemplate: Template?,
    ): AppriseRenderResult {
        val velocityContext = context.toVelocityContext()
        val title = titleTemplate?.let { renderTemplate(it, velocityContext) }
        val body = renderTemplate(
            requireNotNull(bodyTemplate) { "Body template is required" },
            velocityContext
        )
        return AppriseRenderResult(title, body)
    }

    fun getCurrentTemplates(): AppriseStringTemplates {
        val defaultTitleTemplate = AppriseVelocityTemplates::class.java.getResource("/${titleFileName}")?.readText()
        val defaultBodyTemplate = AppriseVelocityTemplates::class.java.getResource("/${bodyFileName}")?.readText()
        if (appriseDirectory.notExists()) {
            return AppriseStringTemplates(
                titleTemplate = defaultTitleTemplate,
                bodyTemplate = defaultBodyTemplate
            )
        }
        return AppriseStringTemplates(
            titleTemplate = appriseDirectory.resolve(titleFileName)
                .let { if (it.exists()) it.readText() else defaultTitleTemplate },
            bodyTemplate = appriseDirectory.resolve(bodyFileName)
                .let { if (it.exists()) it.readText() else defaultBodyTemplate }
        )
    }

    suspend fun updateTemplates(templates: AppriseStringTemplates) {
        templateWriteMutex.withLock {
            appriseDirectory.createDirectories()
            val titleTemplate = templates.titleTemplate?.let { template ->
                velocityEngine.templateWriteAndGet(
                    template,
                    appriseDirectory.resolve(titleFileName)
                )
            } ?: velocityEngine.loadTemplateByName(titleFileName)

            val bodyTemplate = templates.bodyTemplate?.let { template ->
                velocityEngine.templateWriteAndGet(
                    template,
                    appriseDirectory.resolve(bodyFileName)
                )
            } ?: velocityEngine.loadTemplateByName(bodyFileName)

            this.titleTemplate.value = titleTemplate
            this.bodyTemplate.value = bodyTemplate

        }
    }
}