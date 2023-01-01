package org.snd.config

import com.charleskorn.kaml.Yaml
import java.nio.file.Path
import kotlin.io.path.isWritable
import kotlin.io.path.writeText
import kotlin.text.Charsets.UTF_8

class ConfigWriter(
    private val yaml: Yaml
) {

    fun writeConfig(config: AppConfig, path: Path) {
        checkWriteAccess(path)
        path.writeText(yaml.encodeToString(AppConfig.serializer(), config), UTF_8)
    }

    fun writeConfigToDefaultPath(config: AppConfig) {
        val filePath = Path.of(".").toAbsolutePath().normalize().resolve("application.yml")
        checkWriteAccess(filePath)
        filePath.writeText(yaml.encodeToString(AppConfig.serializer(), config), UTF_8)
    }

    private fun checkWriteAccess(path: Path) {
        if (path.isWritable().not()) throw AccessDeniedException(file = path.toFile(), reason = "No write access to config file")
    }
}