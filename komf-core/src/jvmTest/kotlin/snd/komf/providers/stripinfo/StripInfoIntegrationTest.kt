package snd.komf.providers.stripinfo

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import kotlinx.coroutines.runBlocking
import snd.komf.providers.stripinfo.model.StripInfoSeriesId
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Live integration test for the stripINFO.be provider.
 *
 * Requires an active internet connection and is excluded from regular CI builds.
 * Run manually to verify the scraping still works after site changes.
 *
 * Usage: ./gradlew :komf-core:jvmTest --tests "*.StripInfoIntegrationTest"
 */
class StripInfoIntegrationTest {

    private val client = StripInfoClient(
        HttpClient(OkHttp) {
            install(UserAgent) { agent = "Komf/test" }
            followRedirects = true
        }
    )

    // Suske en Wiske — series ID 1857, well-known Belgian series, stable fixture
    private val suskeSeries = StripInfoSeriesId(1857)

    @Test
    fun `searchSeries returns results for Suske en Wiske`() = runBlocking {
        val results = client.searchSeries("Suske en Wiske")

        assertTrue(results.isNotEmpty(), "Expected at least one search result")
        val exact = results.find { it.id == suskeSeries }
        assertNotNull(exact, "Expected series ID $suskeSeries in results")
        assertTrue(exact.title.contains("Suske", ignoreCase = true), "Title should contain 'Suske'")
    }

    @Test
    fun `getSeries returns valid metadata for Suske en Wiske`() = runBlocking {
        val series = client.getSeries(suskeSeries)

        assertTrue(series.title.isNotBlank(), "Title should not be blank")
        assertTrue(series.albums.isNotEmpty(), "Suske en Wiske should have albums")
        assertNotNull(series.startYear, "Should have a start year")
        assertTrue((series.startYear ?: 0) < 1950, "Suske en Wiske started before 1950")
        assertTrue(series.authors.isNotEmpty(), "Should have at least one author")
        assertTrue(series.publishers.isNotEmpty(), "Should have at least one publisher")
    }

    @Test
    fun `getSeries album list contains correctly numbered entries`() = runBlocking {
        val series = client.getSeries(suskeSeries)

        val numbered = series.albums.filter { it.number != null }
        assertTrue(numbered.isNotEmpty(), "Should have numbered albums")

        // Check that album 1 is present
        val album1 = series.albums.find { it.number == "1" }
        assertNotNull(album1, "Album number 1 should exist")
        assertTrue(album1.title?.isNotBlank() == true, "Album 1 should have a title")
    }
}
