package snd.komf.app.config

import com.charleskorn.kaml.Yaml
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isWritable
import kotlin.io.path.writeText
import kotlin.text.Charsets.UTF_8

class ConfigWriter(private val yaml: Yaml) {

    @Synchronized
    fun writeConfig(config: AppConfig, path: Path) {
        checkWriteAccess(path)
        if (path.isDirectory()) {
            path.resolve("application.yml")
                .writeText(yaml.encodeToString(AppConfig.serializer(), config), UTF_8)
        } else {
            path.writeText(yaml.encodeToString(AppConfig.serializer(), config), UTF_8)
        }
    }

    @Synchronized
    fun writeConfigToDefaultPath(config: AppConfig) {
        val filePath = Path.of(".").toAbsolutePath().normalize().resolve("application.yml")
        if (filePath.exists())
            checkWriteAccess(filePath)

        filePath.writeText(yaml.encodeToString(AppConfig.serializer(), config), UTF_8)
    }

    private fun checkWriteAccess(path: Path) {
        if (path.isWritable().not()) throw AccessDeniedException(file = path.toFile(), reason = "No write access to config file")
    }
}