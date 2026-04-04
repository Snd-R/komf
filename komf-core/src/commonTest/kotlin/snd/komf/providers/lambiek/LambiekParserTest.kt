package snd.komf.providers.lambiek

import snd.komf.providers.lambiek.model.LambiekBookId
import snd.komf.providers.lambiek.model.LambiekSeriesId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LambiekParserTest {
    private val parser = LambiekParser()

    // --- parseSearchResults ---

    @Test
    fun `parseSearchResults returns series from book table rows`() {
        val results = parser.parseSearchResults(SEARCH_HTML)

        assertEquals(2, results.size)
        assertEquals(LambiekSeriesId("suske-en-wiske"), results[0].id)
        assertEquals("Suske en Wiske", results[0].seriesName)
        assertEquals("Willy Vandersteen", results[0].artistName)
        assertEquals(LambiekSeriesId("asterix"), results[1].id)
        assertEquals("Asterix", results[1].seriesName)
    }

    @Test
    fun `parseSearchResults strips format suffix from series label`() {
        val html = """
            <html><body>
              <table><tbody>
                <tr>
                  <td><a href="/shop/series/suske-en-wiske/12345/het-wonderwater.html">Het wonderwater</a></td>
                  <td>Suske en Wiske sc</td>
                  <td>Vandersteen</td>
                </tr>
              </tbody></table>
            </body></html>
        """.trimIndent()

        val results = parser.parseSearchResults(html)
        assertEquals(1, results.size)
        assertEquals("Suske en Wiske", results[0].seriesName)
    }

    @Test
    fun `parseSearchResults deduplicates by series slug`() {
        val html = """
            <html><body>
              <table><tbody>
                <tr>
                  <td><a href="/shop/series/suske-en-wiske/11111/album-1.html">Album 1</a></td>
                  <td>Suske en Wiske</td>
                  <td>Vandersteen</td>
                </tr>
                <tr>
                  <td><a href="/shop/series/suske-en-wiske/22222/album-2.html">Album 2</a></td>
                  <td>Suske en Wiske</td>
                  <td>Vandersteen</td>
                </tr>
              </tbody></table>
            </body></html>
        """.trimIndent()

        val results = parser.parseSearchResults(html)
        assertEquals(1, results.size)
        assertEquals(LambiekSeriesId("suske-en-wiske"), results[0].id)
    }

    @Test
    fun `parseSearchResults returns empty list when no table found`() {
        val html = "<html><body><p>Geen resultaten gevonden.</p></body></html>"
        assertEquals(emptyList(), parser.parseSearchResults(html))
    }

    @Test
    fun `parseSearchResults ignores rows missing book link`() {
        val html = """
            <html><body>
              <table><tbody>
                <tr>
                  <td>No link here</td>
                  <td>Some series</td>
                </tr>
                <tr>
                  <td><a href="/shop/series/asterix/80911/asterix-de-gallie.html">Asterix de Galliër</a></td>
                  <td>Asterix</td>
                  <td>Uderzo</td>
                </tr>
              </tbody></table>
            </body></html>
        """.trimIndent()

        val results = parser.parseSearchResults(html)
        assertEquals(1, results.size)
        assertEquals(LambiekSeriesId("asterix"), results[0].id)
    }

    // --- parseSeriesPage ---

    @Test
    fun `parseSeriesPage extracts title from breadcrumb`() {
        val series = parser.parseSeriesPage(SERIES_PAGE_HTML, LambiekSeriesId("suske-en-wiske"))
        assertEquals("Suske en Wiske", series.title)
    }

    @Test
    fun `parseSeriesPage extracts publisher language and genres`() {
        val series = parser.parseSeriesPage(SERIES_PAGE_HTML, LambiekSeriesId("suske-en-wiske"))
        assertEquals("Standaard Uitgeverij", series.publisher)
        assertEquals("Dutch", series.language)
        assertEquals(listOf("adventure", "humour"), series.genres)
    }

    @Test
    fun `parseSeriesPage returns empty books list`() {
        val series = parser.parseSeriesPage(SERIES_PAGE_HTML, LambiekSeriesId("suske-en-wiske"))
        assertEquals(emptyList(), series.books)
    }

    @Test
    fun `parseSeriesPage falls back to slug-derived title when no breadcrumb`() {
        val html = "<html><body><p>No breadcrumb here</p></body></html>"
        val series = parser.parseSeriesPage(html, LambiekSeriesId("suske-en-wiske"))
        assertEquals("Suske-en-wiske", series.title)
    }

    // --- parseCollections ---

    @Test
    fun `parseCollections extracts books with id slug volume and title`() {
        val books = parser.parseCollections(COLLECTIONS_HTML, LambiekSeriesId("suske-en-wiske"))

        assertEquals(2, books.size)

        val book1 = books[0]
        assertEquals(LambiekBookId(80911), book1.id)
        assertEquals("het-wonderwater", book1.slug)
        assertEquals("382", book1.volume)
        assertEquals("Het wonderwater", book1.title)

        val book2 = books[1]
        assertEquals(LambiekBookId(75432), book2.id)
        assertEquals("de-lachende-mummie", book2.slug)
        assertEquals("381", book2.volume)
    }

    @Test
    fun `parseCollections deduplicates books by id`() {
        val html = """
            <html><body>
              <a href="/shop/series/suske-en-wiske/80911/het-wonderwater.html">
                <h2>382 Het wonderwater</h2>
              </a>
              <a href="/shop/series/suske-en-wiske/80911/het-wonderwater.html">
                <h2>382 Het wonderwater (duplicate)</h2>
              </a>
            </body></html>
        """.trimIndent()

        val books = parser.parseCollections(html, LambiekSeriesId("suske-en-wiske"))
        assertEquals(1, books.size)
    }

    @Test
    fun `parseCollections ignores links to other series`() {
        val html = """
            <html><body>
              <a href="/shop/series/suske-en-wiske/80911/het-wonderwater.html">
                <h2>382 Het wonderwater</h2>
              </a>
              <a href="/shop/series/asterix/12345/asterix-de-gallie.html">
                <h2>1 Asterix de Galliër</h2>
              </a>
            </body></html>
        """.trimIndent()

        val books = parser.parseCollections(html, LambiekSeriesId("suske-en-wiske"))
        assertEquals(1, books.size)
        assertEquals(LambiekBookId(80911), books[0].id)
    }

    @Test
    fun `parseCollections returns unnumbered title when heading has no leading number`() {
        val html = """
            <html><body>
              <a href="/shop/series/suske-en-wiske/90000/special-edition.html">
                <h2>Special Edition</h2>
              </a>
            </body></html>
        """.trimIndent()

        val books = parser.parseCollections(html, LambiekSeriesId("suske-en-wiske"))
        assertEquals(1, books.size)
        assertNull(books[0].volume)
        assertEquals("Special Edition", books[0].title)
    }

    // --- parseBookPage ---

    @Test
    fun `parseBookPage extracts all schema org fields`() {
        val book = parser.parseBookPage(BOOK_PAGE_HTML, LambiekBookId(80911))

        assertEquals(LambiekBookId(80911), book.id)
        assertEquals("Het wonderwater", book.title)
        assertEquals("382", book.volume)
        assertEquals("Wout Schoonis", book.illustrator)
        assertEquals("Charel Cambré", book.writer)
        assertEquals("Standaard Uitgeverij", book.publisher)
        assertEquals("12-02-2026", book.releaseDate)
        assertEquals("Dutch", book.language)
        assertEquals(40, book.pageCount)
        assertEquals("9789002288487", book.isbn)
        assertEquals(listOf("adventure"), book.genres)
        assertNotNull(book.description)
        assertEquals("https://www.lambiek.net/share/image.php/cover.jpg", book.imageUrl)
    }

    @Test
    fun `parseBookPage extracts series ID from specifics link`() {
        val book = parser.parseBookPage(BOOK_PAGE_HTML, LambiekBookId(80911))
        assertEquals(LambiekSeriesId("suske-en-wiske"), book.seriesId)
    }

    @Test
    fun `parseBookPage handles missing optional fields gracefully`() {
        val minimalHtml = """
            <html><body>
              <div itemscope itemtype="http://schema.org/Book">
                <h2 itemprop="name">Het wonderwater</h2>
              </div>
            </body></html>
        """.trimIndent()

        val book = parser.parseBookPage(minimalHtml, LambiekBookId(999))

        assertEquals("Het wonderwater", book.title)
        assertNull(book.volume)
        assertNull(book.illustrator)
        assertNull(book.writer)
        assertNull(book.isbn)
        assertNull(book.releaseDate)
        assertNull(book.seriesId)
        assertNull(book.imageUrl)
        assertEquals(emptyList(), book.genres)
    }

    companion object {
        private val SEARCH_HTML = """
            <!DOCTYPE html><html><body>
              <table>
                <thead><tr><th>Title</th><th>Series</th><th>Artist</th></tr></thead>
                <tbody>
                  <tr>
                    <td><a href="/shop/series/suske-en-wiske/80911/het-wonderwater.html">Het wonderwater</a></td>
                    <td>Suske en Wiske</td>
                    <td>Willy Vandersteen</td>
                  </tr>
                  <tr>
                    <td><a href="/shop/series/asterix/12345/asterix-de-gallie.html">Asterix de Galliër</a></td>
                    <td>Asterix</td>
                    <td>Uderzo</td>
                  </tr>
                </tbody>
              </table>
            </body></html>
        """.trimIndent()

        private val SERIES_PAGE_HTML = """
            <!DOCTYPE html><html><body>
              <div class="broodkruimel">
                <span itemprop="name"><a href="/">Home</a></span>
                <span itemprop="name"><a href="/shop/">Shop</a></span>
                <span itemprop="name">Suske en Wiske</span>
              </div>
              <div itemscope itemtype="http://schema.org/Book">
                <span itemprop="publisher"><a href="/shop/publishers/standaard/">Standaard Uitgeverij</a></span>
                <span itemprop="inLanguage">Dutch</span>
                <span itemprop="genre">adventure</span>
                <span itemprop="genre">humour</span>
              </div>
            </body></html>
        """.trimIndent()

        private val COLLECTIONS_HTML = """
            <!DOCTYPE html><html><body>
              <a href="/shop/series/suske-en-wiske/80911/het-wonderwater.html">
                <h2>382 Het wonderwater</h2>
                <img src="/share/image.php/80911.jpg">
              </a>
              <a href="/shop/series/suske-en-wiske/75432/de-lachende-mummie.html">
                <h2>381 De lachende mummie</h2>
                <img src="/share/image.php/75432.jpg">
              </a>
            </body></html>
        """.trimIndent()

        private val BOOK_PAGE_HTML = """
            <!DOCTYPE html><html><body>
              <div itemscope itemtype="http://schema.org/Book">
                <h2 itemprop="name">Het wonderwater</h2>
                <span itemprop="illustrator"><a href="/shop/artist/schoonis">Wout Schoonis</a></span>
                <span itemprop="author">Charel Cambré</span>
                <span itemprop="publisher"><a href="/shop/publishers/standaard/">Standaard Uitgeverij</a></span>
                <span itemprop="datePublished">12-02-2026</span>
                <span itemprop="inLanguage">Dutch</span>
                <span itemprop="numberOfPages">40</span>
                <span itemprop="genre">adventure</span>
                <meta itemprop="isbn" content="9789002288487">
                <div itemprop="description">A story about water.</div>
                <img itemprop="image" src="https://www.lambiek.net/share/image.php/cover.jpg">
                <div class="specifics">volume: <span>382</span></div>
                <div class="specifics">series: <span><a href="/shop/series/suske-en-wiske/">Suske en Wiske</a></span></div>
              </div>
            </body></html>
        """.trimIndent()
    }
}
