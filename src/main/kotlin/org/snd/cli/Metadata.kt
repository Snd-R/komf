package org.snd.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import org.snd.config.AniListConfig
import org.snd.config.MetadataProvidersConfig
import org.snd.config.ProviderConfig
import org.snd.config.ProvidersConfig
import org.snd.metadata.MetadataProvider
import org.snd.metadata.model.MatchQuery
import org.snd.metadata.model.MediaType
import org.snd.metadata.model.MediaType.MANGA
import org.snd.metadata.model.NameMatchingMode
import org.snd.metadata.model.NameMatchingMode.CLOSEST_MATCH
import org.snd.metadata.model.Provider
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.module.MetadataModule.MetadataProvidersContainer
import org.snd.module.context.CliMetadataContext
import kotlin.system.exitProcess

class Metadata : CliktCommand() {
    private val nameMatchingMode by option().convert { NameMatchingMode.valueOf(it.uppercase()) }.default(CLOSEST_MATCH)
    private val malClientId by option()
    private val mediaType by option().convert { MediaType.valueOf(it.uppercase()) }.default(MANGA)

    override fun run() {
        currentContext.findOrSetObject { CliMetadataContext(createConfig()) }
    }

    private fun createConfig(): MetadataProvidersConfig {
        return MetadataProvidersConfig(
            nameMatchingMode = nameMatchingMode,
            malClientId = malClientId ?: "",
            defaultProviders = ProvidersConfig(
                mangaUpdates = ProviderConfig(enabled = true, mediaType = mediaType),
                mal = ProviderConfig(enabled = true, mediaType = mediaType),
                nautiljon = ProviderConfig(enabled = true, mediaType = mediaType),
                aniList = AniListConfig(enabled = true, mediaType = mediaType),
                yenPress = ProviderConfig(enabled = true, mediaType = mediaType),
                kodansha = ProviderConfig(enabled = true, mediaType = mediaType),
                viz = ProviderConfig(enabled = true, mediaType = mediaType),
                bookWalker = ProviderConfig(enabled = true, mediaType = mediaType),
                mangaDex = ProviderConfig(enabled = true, mediaType = mediaType),
            )
        )
    }

    abstract class MetadataSubCommand : CliktCommand() {
        abstract val moshi: Moshi
        abstract val providers: MetadataProvidersContainer

        protected fun provider(providerName: Provider): MetadataProvider {
            val provider = providers.provider(providerName)
            if (provider == null) {
                echoError("Provider $providerName is not configured")
                exitProcess(1)
            }

            return provider
        }

        protected fun echoError(message: String) {
            echo(toJson(mapOf("message" to message)))
        }

        protected inline fun <reified T : Any> toJson(value: T): String = moshi.adapter<T>().toJson(value)
    }

    class Search : MetadataSubCommand() {
        private val context by requireObject<CliMetadataContext>()
        private val provider by option().convert { Provider.valueOf(it.uppercase()) }.required()
        private val title by option().required()
        override val moshi: Moshi
            get() = context.jsonModule.moshi
        override val providers: MetadataProvidersContainer
            get() = context.metadataModule.metadataProviders.defaultProviders()

        override fun run() {
            val provider = provider(provider)

            try {
                val searchResults = provider.searchSeries(title)
                echo(toJson(searchResults))
            } catch (e: Exception) {
                echoError(e.stackTraceToString())
                exitProcess(1)
            }
            exitProcess(0)
        }
    }

    class Match : MetadataSubCommand() {
        private val context by requireObject<CliMetadataContext>()
        private val provider by option().convert { Provider.valueOf(it.uppercase()) }.required()
        private val title by option().required()
        override val moshi: Moshi
            get() = context.jsonModule.moshi
        override val providers: MetadataProvidersContainer
            get() = context.metadataModule.metadataProviders.defaultProviders()

        override fun run() {
            val provider = provider(provider)

            try {
                val matchResult = provider.matchSeriesMetadata(MatchQuery(title, null, null))
                if (matchResult == null) {
                    echoError("No Match")
                    exitProcess(1)
                }
                echo(toJson(matchResult))
            } catch (e: Exception) {
                echoError(e.stackTraceToString())
                exitProcess(1)
            }

            exitProcess(0)
        }
    }

    class Series : MetadataSubCommand() {
        private val context by requireObject<CliMetadataContext>()
        private val provider by option().convert { Provider.valueOf(it.uppercase()) }.required()
        private val seriesId by option().required()
        override val moshi: Moshi
            get() = context.jsonModule.moshi
        override val providers: MetadataProvidersContainer
            get() = context.metadataModule.metadataProviders.defaultProviders()

        override fun run() {
            val provider = provider(provider)

            try {
                val seriesMetadata = provider.getSeriesMetadata(ProviderSeriesId(seriesId))
                echo(toJson(seriesMetadata))
            } catch (e: Exception) {
                echoError(e.stackTraceToString())
                exitProcess(1)
            }

            exitProcess(0)
        }
    }

    class Book : MetadataSubCommand() {
        private val context by requireObject<CliMetadataContext>()
        private val provider by option().convert { Provider.valueOf(it.uppercase()) }.required()
        private val seriesId by option().required()
        private val bookIds by option().convert { it.split(",") }.required()
        override val moshi: Moshi
            get() = context.jsonModule.moshi
        override val providers: MetadataProvidersContainer
            get() = context.metadataModule.metadataProviders.defaultProviders()

        override fun run() {
            val provider = provider(provider)

            try {
                val books = bookIds.map { bookId ->
                    provider.getBookMetadata(ProviderSeriesId(seriesId), ProviderBookId(bookId))
                }
                echo(toJson(books))
            } catch (e: Exception) {
                echoError(e.stackTraceToString())
                exitProcess(1)
            }

            exitProcess(0)
        }
    }
}
