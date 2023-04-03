package org.snd.metadata.providers

import org.junit.jupiter.api.Test
import org.snd.common.testUtils.prettyPrint
import org.snd.config.ProviderConfig
import org.snd.metadata.MetadataProvider
import org.snd.metadata.model.metadata.ProviderSeriesId

interface MetadataProviderTest {
    
    val config: ProviderConfig
    val provider: MetadataProvider
    val providerName: String
    val title: String
    val seriesId: String

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