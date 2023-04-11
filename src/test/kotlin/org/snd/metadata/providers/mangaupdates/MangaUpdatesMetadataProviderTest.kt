package org.snd.metadata.providers.mangaupdates

import org.snd.common.testUtils.Utils
import org.snd.config.ProviderConfig
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.NameMatchingMode
import org.snd.metadata.model.Provider
import org.snd.metadata.providers.MetadataProviderTest
import org.snd.module.JsonModule

class MangaUpdatesMetadataProviderTest: MetadataProviderTest {

    override val config = ProviderConfig()
    override val title = "钢之炼金术士"
    override val seriesId = "19001585632"
    override val providerName = Provider.MANGA_UPDATES.toString()
    override val provider: MetadataProvider
        get() {
            val httpClient = Utils.createHttpClient(providerName)
            val jsonModule = JsonModule()
            val client = MangaUpdatesClient(httpClient, jsonModule.moshi)
            val mangaUpdatesMetadataMapper = MangaUpdatesMetadataMapper(
                metadataConfig = config.seriesMetadata,
                authorRoles = config.authorRoles,
                artistRoles = config.artistRoles,
            )
            val nameSimilarityMatcher = NameSimilarityMatcher.getInstance(NameMatchingMode.CLOSEST_MATCH)
            val similarityMatcher: NameSimilarityMatcher =
                config.nameMatchingMode?.let {
                    NameSimilarityMatcher.getInstance(it)
                } ?: nameSimilarityMatcher
            return MangaUpdatesMetadataProvider(
                client,
                mangaUpdatesMetadataMapper,
                similarityMatcher,
                config.seriesMetadata.thumbnail,
                config.mediaType,
            )
        }
}