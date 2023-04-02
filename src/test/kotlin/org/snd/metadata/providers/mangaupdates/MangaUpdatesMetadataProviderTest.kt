package org.snd.metadata.providers.mangaupdates

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.snd.common.testUtils.Utils
import org.snd.common.testUtils.prettyPrint
import org.snd.config.ProviderConfig
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.NameMatchingMode
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.module.JsonModule

class MangaUpdatesMetadataProviderTest {

    private val config: ProviderConfig = ProviderConfig()
    private val title = "钢之炼金术士"
    private val seriesId = "19001585632"
    private val jsonModule = JsonModule()
    private lateinit var client: MangaUpdatesClient
    private lateinit var provider: MangaUpdatesMetadataProvider

    @BeforeEach
    fun setUp() {
        val httpClient = Utils.createHttpClient("MangaUpdates")
        client = MangaUpdatesClient(httpClient, jsonModule.moshi)
        val mangaUpdatesMetadataMapper = MangaUpdatesMetadataMapper(
            metadataConfig = config.seriesMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val nameSimilarityMatcher = NameSimilarityMatcher.getInstance(NameMatchingMode.CLOSEST_MATCH)
        val mangaUpdatesSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let {
                NameSimilarityMatcher.getInstance(it)
            } ?: nameSimilarityMatcher
        provider = MangaUpdatesMetadataProvider(
            client,
            mangaUpdatesMetadataMapper,
            mangaUpdatesSimilarityMatcher,
            config.seriesMetadata.thumbnail,
            config.mediaType,
        )
    }

    @Test
    fun getSeriesMetadata() {
        val seriesMetadata = provider.getSeriesMetadata(ProviderSeriesId(seriesId))
        println(seriesMetadata.prettyPrint())
    }

    @Test
    fun searchSeries() {
        val searchResults = provider.searchSeries(title)
        println(searchResults.prettyPrint())
    }

    @Test
    fun matchSeriesMetadata() {
        val matchResult = provider.matchSeriesMetadata(title)
        if (matchResult != null) {
            println(matchResult.prettyPrint())
        }
    }
}