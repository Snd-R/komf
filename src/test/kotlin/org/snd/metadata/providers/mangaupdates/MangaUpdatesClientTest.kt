package org.snd.metadata.providers.mangaupdates

import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.snd.common.http.HttpClient
import org.snd.common.testUtils.Utils
import org.snd.common.testUtils.prettyPrint
import org.snd.metadata.providers.mangaupdates.model.SeriesType
import org.snd.module.JsonModule


class MangaUpdatesClientTest {
    private lateinit var client: MangaUpdatesClient
    private val mangaTypes = listOf(
        SeriesType.MANGA, SeriesType.MANHWA, SeriesType.MANHUA, SeriesType.ARTBOOK, SeriesType.DOUJINSHI,
        SeriesType.FILIPINO, SeriesType.INDONESIAN, SeriesType.THAI, SeriesType.VIETNAMESE, SeriesType.MALAYSIAN,
        SeriesType.OEL, SeriesType.NORDIC, SeriesType.FRENCH, SeriesType.SPANISH
    )

    @BeforeEach
    fun setUp() {
        val httpClient = Utils.createHttpClient("MangaUpdates")
        val jsonModule = JsonModule()

        client = MangaUpdatesClient(httpClient, jsonModule.moshi)
    }
    @Test
    fun searchSeries() {
        val results = client.searchSeries(
            "Fullmetal",
            mangaTypes,
        )

        println(results.prettyPrint())
    }

    @Test
    fun getSeries() {
        val series = client.getSeries(19001585632)
        println(series.prettyPrint())
    }

}