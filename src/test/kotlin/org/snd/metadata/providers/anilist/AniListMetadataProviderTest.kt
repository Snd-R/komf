package org.snd.metadata.providers.anilist

import okhttp3.OkHttpClient
import org.snd.config.AniListConfig
import org.snd.config.ProviderConfig
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.NameMatchingMode
import org.snd.metadata.model.Provider
import org.snd.metadata.providers.MetadataProviderTest

class AniListMetadataProviderTest: MetadataProviderTest {
    override val config = ProviderConfig()
    override val title = "Fullmetal Alchemist"
    override val seriesId = "30025"
    override val providerName = Provider.ANILIST.toString()

    override val provider: MetadataProvider
        get() {
            val aniListConfig = AniListConfig()
            val okHttpClient = OkHttpClient.Builder().build()
            val client = AniListClient(okHttpClient, providerName)
            val anilistMetadataMapper = AniListMetadataMapper(
                metadataConfig = aniListConfig.seriesMetadata,
                authorRoles = aniListConfig.authorRoles,
                artistRoles = aniListConfig.artistRoles,
                tagsSizeLimit = aniListConfig.tagsSizeLimit,
                tagsScoreThreshold = aniListConfig.tagsScoreThreshold
            )
            val nameSimilarityMatcher = NameSimilarityMatcher.getInstance(NameMatchingMode.CLOSEST_MATCH)
            val similarityMatcher: NameSimilarityMatcher =
                aniListConfig.nameMatchingMode?.let {
                    NameSimilarityMatcher.getInstance(it)
                } ?: nameSimilarityMatcher
            return AniListMetadataProvider(
                client,
                anilistMetadataMapper,
                similarityMatcher,
                aniListConfig.seriesMetadata.thumbnail,
                aniListConfig.mediaType,
            )
        }
}