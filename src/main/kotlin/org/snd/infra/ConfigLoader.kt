package org.snd.infra

import com.charleskorn.kaml.Yaml
import org.snd.config.AppConfig
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isReadable

class ConfigLoader {

    fun loadConfig(path: Path?): AppConfig {
        val configRaw = loadFromEnv() ?: loadFromArgs(path) ?: loadDefault()
        return configRaw?.let {
            val config = Yaml.default.decodeFromString(AppConfig.serializer(), it)
            overrideFromEnvVariables(config)
        } ?: overrideFromEnvVariables(AppConfig())
    }

    private fun loadFromEnv(): String? {
        val confEnv = System.getenv("KOMF_CONFIG_DIR")
        return confEnv?.let {
            val path = Path.of(it).resolve("application.yml")
            if (path.isReadable()) Files.readString(path)
            else null
        }
    }

    private fun loadFromArgs(path: Path?): String? {
        return path?.let {
            Files.readString(it.toRealPath())
        }
    }

    private fun loadDefault(): String? {
        return AppConfig::class.java.getResource("/application.yml")?.readText()
    }

    private fun overrideFromEnvVariables(config: AppConfig): AppConfig {
        val komgaConfig = config.komga
        val komgaBaseUri = System.getenv("KOMF_KOMGA_BASE_URI") ?: komgaConfig.baseUri
        val komgaUser = System.getenv("KOMF_KOMGA_USER") ?: komgaConfig.komgaUser
        val komgaPassword = System.getenv("KOMF_KOMGA_PASSWORD") ?: komgaConfig.komgaPassword

        val serverConfig = config.server
        val serverPort = System.getenv("KOMF_SERVER_PORT")?.toInt() ?: serverConfig.port
        val logLevel = System.getenv("KOMF_LOG_LEVEL") ?: config.logLevel

        return config.copy(
            komga = komgaConfig.copy(
                baseUri = komgaBaseUri,
                komgaUser = komgaUser,
                komgaPassword = komgaPassword
            ),
            server = serverConfig.copy(port = serverPort),
            logLevel = logLevel
        )
    }
}