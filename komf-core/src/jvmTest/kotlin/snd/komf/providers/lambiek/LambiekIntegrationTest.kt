package snd.komf.providers.lambiek

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import kotlinx.coroutines.runBlocking
import snd.komf.providers.lambiek.model.LambiekSeriesId
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Live integration test for the Lambiek.net provider.
 *
 * Requires an active internet connection and is excluded from regular CI builds.
 * Run manually to verify the scraping still works after site changes.
 *
 * Usage: ./gradlew :komf-core:jvmTest --tests "*.LambiekIntegrationTest"
 */
class LambiekIntegrationTest {

    private val client = LambiekClient(
        HttpClient(OkHttp) {
            install(UserAgent) { agent = "Komf/test" }
            followRedirects = true
        }
    )

    // Suske en Wiske — stable well-known Belgian series with 300+ volumes
    private val suskeSeriesId = LambiekSeriesId("suske-en-wiske")

    @Test
    fun `searchSeries returns results for Suske en Wiske`() = runBlocking {
        val results = client.searchSeries("Suske en Wiske")

        assertTrue(results.isNotEmpty(), "Expected at least one search result")
        val suske = results.find { it.id == suskeSeriesId }
        assertNotNull(suske, "Expected series slug 'suske-en-wiske' in results")
        assertTrue(suske.seriesName.contains("Suske", ignoreCase = true), "Series name should contain 'Suske'")
    }

    @Test
    fun `getSeriesPage returns valid series metadata`() = runBlocking {
        val series = client.getSeriesPage(suskeSeriesId)

        assertTrue(series.title.isNotBlank(), "Title should not be blank")
        assertTrue(series.title.contains("Suske", ignoreCase = true), "Title should contain 'Suske'")
        assertNotNull(series.publisher, "Should have a publisher")
    }

    @Test
    fun `getCollections returns books with volumes`() = runBlocking {
        val books = client.getCollections(suskeSeriesId)

        assertTrue(books.isNotEmpty(), "Suske en Wiske should have books in the shop")
        val numbered = books.filter { it.volume != null }
        assertTrue(numbered.isNotEmpty(), "Should have numbered volumes")

        // All books should have valid IDs and slugs
        assertTrue(books.all { it.id.id > 0 }, "All books should have positive IDs")
        assertTrue(books.all { it.slug.isNotBlank() }, "All books should have slugs")
    }

    @Test
    fun `getBook returns valid book metadata`() = runBlocking {
        val books = client.getCollections(suskeSeriesId)
        assertTrue(books.isNotEmpty(), "Need at least one book to test getBook")

        val firstBook = books.first()
        val book = client.getBook(suskeSeriesId, firstBook)

        assertTrue(book.title?.isNotBlank() == true, "Book title should not be blank")
        assertNotNull(book.publisher, "Book should have a publisher")
    }
}
