package org.snd.metadata.providers.bangumi

import org.junit.jupiter.api.Test
import org.snd.common.testUtils.Utils
import org.snd.config.ProviderConfig
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.NameMatchingMode
import org.snd.metadata.model.Provider
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.SeriesStatus
import org.snd.metadata.providers.MetadataProviderTest
import org.snd.module.JsonModule
import kotlin.test.assertEquals

class BangumiMetadataProviderTest: MetadataProviderTest {

    override val config = ProviderConfig()
    override val title = "钢之炼金术士"
    override val seriesId = "8165"
    override val providerName = Provider.BANGUMI.toString()
    override val provider: MetadataProvider
        get() {
            val httpClient = Utils.createHttpClient(providerName)
            val jsonModule = JsonModule()
            val client = BangumiClient(httpClient, jsonModule.moshi)
            val bangumiMetadataMapper = BangumiMetadataMapper(
                metadataConfig = config.seriesMetadata,
                authorRoles = config.authorRoles,
                artistRoles = config.artistRoles,
            )
            val nameSimilarityMatcher = NameSimilarityMatcher.getInstance(NameMatchingMode.CLOSEST_MATCH)
            val similarityMatcher: NameSimilarityMatcher =
                config.nameMatchingMode?.let {
                    NameSimilarityMatcher.getInstance(it)
                } ?: nameSimilarityMatcher
            return BangumiMetadataProvider(
                client,
                bangumiMetadataMapper,
                similarityMatcher,
                config.seriesMetadata.thumbnail,
                config.mediaType,
            )
        }

    @Test
    fun seriesEndStatus() {
        val endedSeriesId = "8165"
        val endedSeriesMetadata = provider.getSeriesMetadata(ProviderSeriesId(endedSeriesId))
        assertEquals(SeriesStatus.ENDED, endedSeriesMetadata.metadata.status)

        // Hunter x Hunter
        val hiatusSeriesId = "24596"
        val hiatusSeriesMetadata = provider.getSeriesMetadata(ProviderSeriesId(hiatusSeriesId))
        assertEquals(SeriesStatus.HIATUS, hiatusSeriesMetadata.metadata.status)

        // Ongoing
        val ongoingSeriesId = "105206"
        val ongoingSeriesMetadata = provider.getSeriesMetadata(ProviderSeriesId(ongoingSeriesId))
        assertEquals(SeriesStatus.ONGOING, ongoingSeriesMetadata.metadata.status)
    }
}