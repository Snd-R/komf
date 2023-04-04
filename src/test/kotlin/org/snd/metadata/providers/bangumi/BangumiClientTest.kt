package org.snd.metadata.providers.bangumi

import com.squareup.moshi.adapter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.snd.common.testUtils.Utils
import org.snd.common.testUtils.prettyPrint
import org.snd.metadata.providers.bangumi.model.Images
import org.snd.metadata.providers.bangumi.model.Rating
import org.snd.metadata.providers.bangumi.model.SubjectType
import org.snd.module.JsonModule

class BangumiClientTest {

    private lateinit var client: BangumiClient
    private val jsonModule = JsonModule()

    @BeforeEach
    fun setUp() {
        val httpClient = Utils.createHttpClient("Bangumi")

        client = BangumiClient(httpClient, jsonModule.moshi)
    }

    @Test
    fun searchSeries() {
        val keyword = "钢之炼金术士"
        val results = client.searchSeries(keyword, listOf(SubjectType.Book))
        println(results.prettyPrint())
    }

    @Test
    fun getSeries() {
        val results = client.getSeries(6881)
        println(results.prettyPrint())
    }

    @Test
    fun moshiAdapters() {
        // Images
        val imagesJsonString = "{\n" +
                "      \"small\":\"https://lain.bgm.tv/r/200/pic/cover/l/4f/b8/4923_rg33v.jpg\",\n" +
                "      \"grid\":\"https://lain.bgm.tv/r/100/pic/cover/l/4f/b8/4923_rg33v.jpg\",\n" +
                "      \"large\":\"https://lain.bgm.tv/pic/cover/l/4f/b8/4923_rg33v.jpg\",\n" +
                "      \"medium\":\"https://lain.bgm.tv/r/800/pic/cover/l/4f/b8/4923_rg33v.jpg\",\n" +
                "      \"common\":\"https://lain.bgm.tv/r/400/pic/cover/l/4f/b8/4923_rg33v.jpg\"\n" +
                "   }"

        val imagesAdapter = jsonModule.moshi.adapter<Images>()
        val imagesJson = imagesAdapter.fromJson(imagesJsonString)
        println(imagesJson?.prettyPrint())

        // Rating
        val ratingJsonString = "{\n" +
                "      \"rank\":22,\n" +
                "      \"total\":1463,\n" +
                "      \"count\":{\n" +
                "         \"1\":30,\n" +
                "         \"2\":2,\n" +
                "         \"3\":3,\n" +
                "         \"4\":0,\n" +
                "         \"5\":9,\n" +
                "         \"6\":22,\n" +
                "         \"7\":80,\n" +
                "         \"8\":271,\n" +
                "         \"9\":506,\n" +
                "         \"10\":540\n" +
                "      },\n" +
                "      \"score\":8.8\n" +
                "   }"

        val ratingAdapter = jsonModule.moshi.adapter<Rating>()
        val ratingJson = ratingAdapter.fromJson(ratingJsonString)
        println(ratingJson?.prettyPrint())

        val altTitlesString = "[{\"v\":\"Full Metal Alchemist\"},{\"v\":\"鋼之練金術師\"},{\"v\":\"钢之炼金术士\"}]"
        val altTitlesAdapter = jsonModule.moshi.adapter<List<Map<String, String>>>()
        val altTitlesJson = altTitlesAdapter.fromJson(altTitlesString)
        println(altTitlesString)
        println(altTitlesJson?.prettyPrint())
        println(altTitlesJson?.flatMap { map -> map.values }?.toList())
    }
}