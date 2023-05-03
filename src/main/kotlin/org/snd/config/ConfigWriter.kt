package org.snd.config

import com.charleskorn.kaml.Yaml
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isWritable
import kotlin.io.path.writeText
import kotlin.text.Charsets.UTF_8

class ConfigWriter(
    private val yaml: Yaml
) {

    @Synchronized
    fun writeConfig(config: AppConfig, path: Path) {
        checkWriteAccess(path)
        if (path.isDirectory()) {
            path.resolve("application.yml")
                .writeText(yaml.encodeToString(AppConfig.serializer(), removeDeprecatedOptions(config)), UTF_8)
        } else {
            path.writeText(yaml.encodeToString(AppConfig.serializer(), removeDeprecatedOptions(config)), UTF_8)
        }
    }

    @Synchronized
    fun writeConfigToDefaultPath(config: AppConfig) {
        val filePath = Path.of(".").toAbsolutePath().normalize().resolve("application.yml")
        if (filePath.exists())
            checkWriteAccess(filePath)

        filePath.writeText(yaml.encodeToString(AppConfig.serializer(), removeDeprecatedOptions(config)), UTF_8)
    }

    private fun checkWriteAccess(path: Path) {
        if (path.isWritable().not()) throw AccessDeniedException(file = path.toFile(), reason = "No write access to config file")
    }

    private fun removeDeprecatedOptions(config: AppConfig): AppConfig {
        return config.copy(
            komga = config.komga.copy(
                metadataUpdate = removeDeprecatedOptions(config.komga.metadataUpdate),
                aggregateMetadata = null
            ),
            kavita = config.kavita.copy(
                metadataUpdate = removeDeprecatedOptions(config.kavita.metadataUpdate),
                aggregateMetadata = null
            ),
            metadataProviders = config.metadataProviders.copy(
                mangaUpdates = null,
                mal = null,
                nautiljon = null,
                aniList = null,
                yenPress = null,
                kodansha = null,
                viz = null,
                bookWalker = null
            )
        )
    }

    private fun removeDeprecatedOptions(config: MetadataUpdateConfig): MetadataUpdateConfig {
        return config.copy(
            bookThumbnails = null,
            seriesThumbnails = null,
            seriesTitle = null,
            titleType = null,
            readingDirectionValue = null,
            languageValue = null,
            orderBooks = null,
            modes = null,
            default = config.default.copy(
                postProcessing = config.default.postProcessing.copy(
                    titleType = null
                )
            ),
            library = config.library.map { (libraryId, libraryConfig) ->
                libraryId to libraryConfig.copy(
                    postProcessing = libraryConfig.postProcessing.copy(
                        titleType = null
                    )
                )
            }.toMap()
        )
    }
}