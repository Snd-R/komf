package snd.komf.notifications.discord

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
import snd.komf.notifications.discord.model.EmbedField
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeBytes

private const val titleFileName = "title.vm"
private const val titleUrlFileName = "title_url.vm"
private const val descriptionFileName = "description.vm"
private const val footerFileName = "footer.vm"

data class DiscordRenderResult(
    val title: String?,
    val titleUrl: String?,
    val description: String?,
    val fields: List<EmbedField>,
    val footer: String?,
)

data class DiscordStringTemplates(
    val titleTemplate: String? = null,
    val titleUrlTemplate: String? = null,
    val descriptionTemplate: String? = null,
    val fieldTemplates: List<FieldStringTemplates> = emptyList(),
    val footerTemplate: String? = null,
)

data class FieldStringTemplates(
    val nameTemplate: String,
    val valueTemplate: String,
    val inline: Boolean
)

private data class FieldTemplates(
    val name: Template,
    val value: Template,
    val inline: Boolean
)

private data class FieldTemplateFiles(
    val nameTemplate: Path,
    val valueTemplate: Path,
    val inline: Boolean
)

class DiscordVelocityTemplates(templateDirectory: String) {
    private val discordDirectory = Path(templateDirectory).resolve("discord")

    private val velocityEngine = RuntimeInstance().apply {
        val properties = Properties()
        properties.setProperty("resource.loaders", "file,class")
        properties.setProperty("resource.loader.class.class", ClasspathResourceLoader::class.java.name)
        properties.setProperty("resource.loader.file.path", discordDirectory.absolutePathString())

        init(properties)
    }

    private val templateWriteMutex = Mutex()

    private val titleTemplate: AtomicRef<Template?>
    private val titleUrlTemplate: AtomicRef<Template?>
    private val descriptionTemplate: AtomicRef<Template?>
    private val footerTemplate: AtomicRef<Template?>
    private val fieldTemplates: AtomicRef<List<FieldTemplates>>

    init {
        val titleTemplate = velocityEngine.loadTemplateByName(titleFileName)
        val titleUrlTemplate = velocityEngine.loadTemplateByName(titleUrlFileName)
        val descriptionTemplate = velocityEngine.loadTemplateByName(descriptionFileName)
        val footerTemplate = velocityEngine.loadTemplateByName(footerFileName)
        val fieldTemplates =
            getFieldTemplateFiles().map {
                FieldTemplates(
                    name = velocityEngine.templateFromString(it.nameTemplate.readText()),
                    value = velocityEngine.templateFromString(it.valueTemplate.readText()),
                    inline = it.inline
                )
            }

        this.titleTemplate = atomic(titleTemplate)
        this.titleUrlTemplate = atomic(titleUrlTemplate)
        this.descriptionTemplate = atomic(descriptionTemplate)
        this.footerTemplate = atomic(footerTemplate)
        this.fieldTemplates = atomic(fieldTemplates)
    }

    fun render(context: NotificationContext): DiscordRenderResult {
        return render(
            context = context,
            titleTemplate = titleTemplate.value,
            titleUrlTemplate = titleUrlTemplate.value,
            descriptionTemplate = descriptionTemplate.value,
            fieldTemplates = fieldTemplates.value,
            footerTemplate = footerTemplate.value
        )
    }

    fun render(
        context: NotificationContext,
        templates: DiscordStringTemplates,
    ): DiscordRenderResult {
        return render(
            context = context,
            titleTemplate = templates.titleTemplate?.let { velocityEngine.templateFromString(it) },
            titleUrlTemplate = templates.titleUrlTemplate?.let { velocityEngine.templateFromString(it) },
            descriptionTemplate = templates.descriptionTemplate?.let { velocityEngine.templateFromString(it) },
            fieldTemplates = templates.fieldTemplates.map {
                FieldTemplates(
                    name = velocityEngine.templateFromString(it.nameTemplate),
                    value = velocityEngine.templateFromString(it.valueTemplate),
                    inline = it.inline
                )
            },
            footerTemplate = templates.footerTemplate?.let { velocityEngine.templateFromString(it) }
        )

    }

    private fun render(
        context: NotificationContext,
        titleTemplate: Template?,
        titleUrlTemplate: Template?,
        descriptionTemplate: Template?,
        fieldTemplates: List<FieldTemplates>,
        footerTemplate: Template?,
    ): DiscordRenderResult {
        val velocityContext = context.toVelocityContext()

        val title = titleTemplate?.let { renderTemplate(it, velocityContext).take(256) }
        val titleUrl = titleUrlTemplate?.let { renderTemplate(it, velocityContext) }?.trim()
        val description = descriptionTemplate?.let { renderTemplate(it, velocityContext).take(4095) }
        val fields = fieldTemplates.map {
            EmbedField(
                name = renderTemplate(it.name, velocityContext).take(256),
                value = renderTemplate(it.value, velocityContext).take(1024),
                inline = it.inline
            )
        }
        val footer = footerTemplate?.let { renderTemplate(it, velocityContext).take(2048) }

        return DiscordRenderResult(title, titleUrl, description, fields, footer)
    }

    fun getCurrentTemplates(): DiscordStringTemplates {
        val defaultTitleTemplate =
            DiscordVelocityTemplates::class.java.getResource("/${titleFileName}")?.readText()
        val defaultDescriptionTemplate =
            DiscordVelocityTemplates::class.java.getResource("/${descriptionFileName}")?.readText()
        if (discordDirectory.notExists())
            return DiscordStringTemplates(
                titleTemplate = defaultTitleTemplate,
                descriptionTemplate = defaultDescriptionTemplate
            )

        val titleTemplate = discordDirectory.resolve(titleFileName).let {
            if (it.exists()) it.readText() else null
        }
        val titleUrlTemplate = discordDirectory.resolve(titleUrlFileName).let {
            if (it.exists()) it.readText() else null
        }
        val descriptionTemplate = discordDirectory.resolve(descriptionFileName).let {
            if (it.exists()) it.readText() else null
        }
        val footerTemplate = discordDirectory.resolve(footerFileName).let {
            if (it.exists()) it.readText() else null
        }
        val fieldTemplates = getFieldTemplateFiles().map {
            FieldStringTemplates(
                nameTemplate = it.nameTemplate.readText(),
                valueTemplate = it.valueTemplate.readText(),
                inline = it.inline
            )
        }

        return DiscordStringTemplates(
            titleTemplate = titleTemplate ?: defaultTitleTemplate,
            titleUrlTemplate = titleUrlTemplate,
            descriptionTemplate = descriptionTemplate ?: defaultDescriptionTemplate,
            fieldTemplates = fieldTemplates,
            footerTemplate = footerTemplate
        )

    }

    suspend fun updateTemplates(templates: DiscordStringTemplates) {
        templateWriteMutex.withLock {
            discordDirectory.createDirectories()

            val titleTemplate = velocityEngine.templateWriteAndGet(
                templates.titleTemplate,
                discordDirectory.resolve(titleFileName)
            ) ?: velocityEngine.loadTemplateByName(titleFileName)

            val titleUrlTemplate = velocityEngine.templateWriteAndGet(
                templates.titleUrlTemplate,
                discordDirectory.resolve(titleUrlFileName)
            ) ?: velocityEngine.loadTemplateByName(descriptionFileName)

            val descriptionTemplate = velocityEngine.templateWriteAndGet(
                templates.descriptionTemplate,
                discordDirectory.resolve(descriptionFileName)
            ) ?: velocityEngine.loadTemplateByName(descriptionFileName)

            val footerTemplate = velocityEngine.templateWriteAndGet(
                templates.footerTemplate,
                discordDirectory.resolve(footerFileName)
            )

            val fieldTemplates = templates.fieldTemplates.let { fieldTemplates ->
                discordDirectory.listDirectoryEntries()
                    .filter { it.name.startsWith("field_") && it.extension == "vm" }
                    .forEach { it.deleteExisting() }

                fieldTemplates.mapIndexed { index, value ->
                    val inline = if (value.inline) "_inline" else ""
                    discordDirectory.resolve("field_${index + 1}_name${inline}.vm").createFile()
                        .writeBytes(value.nameTemplate.toByteArray(Charsets.UTF_8))
                    discordDirectory.resolve("field_${index + 1}_value.vm").createFile()
                        .writeBytes(value.valueTemplate.toByteArray(Charsets.UTF_8))

                    FieldTemplates(
                        name = velocityEngine.templateFromString(value.nameTemplate),
                        value = velocityEngine.templateFromString(value.valueTemplate),
                        inline = value.inline
                    )
                }
            }

            this.titleTemplate.value = titleTemplate
            this.titleUrlTemplate.value = titleUrlTemplate
            this.descriptionTemplate.value = descriptionTemplate
            this.footerTemplate.value = footerTemplate
            this.fieldTemplates.value = fieldTemplates
        }
    }

    private fun getFieldTemplateFiles(): List<FieldTemplateFiles> {
        if (discordDirectory.notExists()) return emptyList()

        val nameMap = mutableMapOf<Int, Pair<Path, Boolean>>()
        val valueMap = mutableMapOf<Int, Path>()

        for (entry in discordDirectory.listDirectoryEntries()) {
            if (!entry.name.startsWith("field_") || entry.extension != "vm") continue
            val split = entry.nameWithoutExtension.split("_")
            if (split.size < 3) continue
            val number = split[1].toIntOrNull() ?: continue

            when (split[2]) {
                "name" -> {
                    val inline = split.size > 3 && split[3] == "inline"
                    nameMap[number] = entry to inline
                }

                "value" -> valueMap[number] = entry

                else -> continue
            }
        }

        nameMap.forEach { (key, value) ->
            require(valueMap.contains(key)) { "Found field name template but no corresponding value template for file ${value.first}" }
        }
        valueMap.forEach { (key, value) ->
            require(valueMap.contains(key)) { "Found field value template but no corresponding name template for file $value" }
        }

        return nameMap.entries.sortedBy { it.key }.map { (key, value) ->
            val (fieldNamePath, inline) = value
            val fieldValuePath = requireNotNull(valueMap[key])

            FieldTemplateFiles(
                nameTemplate = fieldNamePath,
                valueTemplate = fieldValuePath,
                inline = inline
            )
        }
    }
}

