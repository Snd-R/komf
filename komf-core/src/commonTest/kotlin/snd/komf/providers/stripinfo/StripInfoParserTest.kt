package snd.komf.providers.stripinfo

import snd.komf.providers.stripinfo.model.StripInfoAlbumId
import snd.komf.providers.stripinfo.model.StripInfoRole
import snd.komf.providers.stripinfo.model.StripInfoSeriesId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StripInfoParserTest {
    private val parser = StripInfoParser()

    // --- parseSearchResults ---

    @Test
    fun `parseSearchResults returns exact and overige matches`() {
        val results = parser.parseSearchResults(SEARCH_HTML)

        assertEquals(3, results.size)
        assertEquals(StripInfoSeriesId(1857), results[0].id)
        assertEquals("Suske en Wiske", results[0].title)
        assertEquals("https://www.stripinfo.be/reeks/index/1857_Suske_en_Wiske", results[0].url)
        assertEquals(StripInfoSeriesId(13444), results[1].id)
        assertEquals("Junior Suske en Wiske", results[1].title)
        assertEquals(StripInfoSeriesId(2046), results[2].id)
    }

    @Test
    fun `parseSearchResults ignores links outside tables`() {
        val html = """
            <html><body>
              <nav><a href="/reeks/index/999_nav_link">Nav Link</a></nav>
              <section>
                <table>
                  <tr><td><a href="https://www.stripinfo.be/reeks/index/1857_Suske_en_Wiske">Suske en Wiske</a></td></tr>
                </table>
              </section>
            </body></html>
        """.trimIndent()

        val results = parser.parseSearchResults(html)
        assertEquals(1, results.size)
        assertEquals(StripInfoSeriesId(1857), results[0].id)
    }

    @Test
    fun `parseSearchResults deduplicates by series ID`() {
        val html = """
            <html><body>
              <table>
                <tr><td><a href="/reeks/index/1857_Suske_en_Wiske">Suske en Wiske</a></td></tr>
                <tr><td><a href="/reeks/index/1857_Suske_en_Wiske">Suske en Wiske duplicate</a></td></tr>
              </table>
            </body></html>
        """.trimIndent()

        val results = parser.parseSearchResults(html)
        assertEquals(1, results.size)
    }

    @Test
    fun `parseSearchResults returns empty list when no results`() {
        val html = "<html><body><p>Geen resultaten gevonden.</p></body></html>"
        assertEquals(emptyList(), parser.parseSearchResults(html))
    }

    // --- parseSeries ---

    @Test
    fun `parseSeries parses title startYear endYear and rating from microdata`() {
        val seriesId = StripInfoSeriesId(1857)
        val series = parser.parseSeries(SUSKE_SERIES_HTML, seriesId)

        assertEquals("Suske en Wiske", series.title)
        assertEquals(1946, series.startYear)
        assertEquals(2026, series.endYear)
        assertEquals(7.04, series.rating)
        assertEquals(12323, series.ratingCount)
        assertEquals(seriesId, series.id)
    }

    @Test
    fun `parseSeries parses album list with number title and yearRange`() {
        val series = parser.parseSeries(SUSKE_SERIES_HTML, StripInfoSeriesId(1857))

        assertEquals(3, series.albums.size)

        val album0 = series.albums[0]
        assertEquals(StripInfoAlbumId(20822), album0.id)
        assertEquals("0", album0.number)
        assertEquals("Rikki en Wiske", album0.title)
        assertEquals("1946-2015", album0.yearRange)

        val album1 = series.albums[1]
        assertEquals(StripInfoAlbumId(10479), album1.id)
        assertEquals("1", album1.number)
        assertEquals("Op het eiland Amoras", album1.title)

        val album2 = series.albums[2]
        assertEquals(StripInfoAlbumId(10480), album2.id)
        assertEquals("2", album2.number)
    }

    @Test
    fun `parseSeries parses sidebar authors publishers and language`() {
        val series = parser.parseSeries(SUSKE_SERIES_HTML, StripInfoSeriesId(1857))

        assertEquals(listOf("Willy Vandersteen"), series.authors)
        assertEquals(listOf("Standaard Uitgeverij"), series.publishers)
        assertEquals("Nederlands", series.language)
    }

    @Test
    fun `parseSeries parses tags`() {
        val series = parser.parseSeries(SUSKE_SERIES_HTML, StripInfoSeriesId(1857))
        assertEquals(listOf("Humor", "Avontuur"), series.tags)
    }

    @Test
    fun `parseSeries parses pageLogo image URL`() {
        val series = parser.parseSeries(SUSKE_SERIES_HTML, StripInfoSeriesId(1857))
        assertEquals("https://www.stripinfo.be/images/images/1857.jpg", series.imageUrl)
    }

    @Test
    fun `parseSeries handles series without endDate`() {
        val html = SUSKE_SERIES_HTML.replace(
            """<span itemprop="endDate" content="2026-12-31"></span>""", ""
        )
        val series = parser.parseSeries(html, StripInfoSeriesId(1857))
        assertNull(series.endYear)
        assertEquals(1946, series.startYear)
    }

    // --- parseAlbum ---

    @Test
    fun `parseAlbum parses number title pageCount isbn and publisher from microdata`() {
        val album = parser.parseAlbum(PLANKGAS_ALBUM_HTML, StripInfoAlbumId(16803))

        assertEquals(StripInfoAlbumId(16803), album.id)
        assertEquals("1", album.number)
        assertEquals("Deel 1", album.title)
        assertEquals(32, album.pageCount)
        assertEquals("9071762971", album.isbn)
        assertEquals("Concentra Media", album.publisher)
    }

    @Test
    fun `parseAlbum parses scenario and tekeningen credits with authorIds`() {
        val album = parser.parseAlbum(PLANKGAS_ALBUM_HTML, StripInfoAlbumId(16803))

        assertEquals(2, album.credits.size)

        val scenario = album.credits.first { it.role == StripInfoRole.SCENARIO }
        assertEquals("Urbanus", scenario.name)
        assertEquals(1926, scenario.authorId)

        val tekeningen = album.credits.first { it.role == StripInfoRole.TEKENINGEN }
        assertEquals("Dirk Stallaert", tekeningen.name)
        assertEquals(1256, tekeningen.authorId)
    }

    @Test
    fun `parseAlbum parses year language binding and barcode from detail table`() {
        val album = parser.parseAlbum(PLANKGAS_ALBUM_HTML, StripInfoAlbumId(16803))

        assertEquals(2006, album.year)
        assertEquals("Nederlands", album.language)
        assertEquals("Softcover", album.binding)
        assertEquals("9789071762970", album.barcode)
    }

    @Test
    fun `parseAlbum extracts series ID from breadcrumb link`() {
        val album = parser.parseAlbum(PLANKGAS_ALBUM_HTML, StripInfoAlbumId(16803))
        assertEquals(StripInfoSeriesId(1820), album.seriesId)
    }

    @Test
    fun `parseAlbum extracts cover image URL`() {
        val album = parser.parseAlbum(PLANKGAS_ALBUM_HTML, StripInfoAlbumId(16803))
        assertEquals("https://www.stripinfo.be/image.php?i=494603&s=16803", album.imageUrl)
    }

    @Test
    fun `parseAlbum handles missing optional fields gracefully`() {
        val minimalHtml = """
            <html><body>
              <div itemtype="https://schema.org/ComicIssue">
                <span itemprop="issueNumber">5</span>
                <span itemprop="name">Test Album</span>
              </div>
            </body></html>
        """.trimIndent()

        val album = parser.parseAlbum(minimalHtml, StripInfoAlbumId(999))

        assertEquals("5", album.number)
        assertEquals("Test Album", album.title)
        assertNull(album.year)
        assertNull(album.isbn)
        assertNull(album.seriesId)
        assertNull(album.imageUrl)
        assertEquals(emptyList(), album.credits)
    }

    companion object {
        private val SEARCH_HTML = """
            <!DOCTYPE html><html><body>
            <div class="listcleanblock">
              <h2 class="title" id="Reeksen">Reeksen</h2>
              <section class="row">
                <section class="c6">
                  <h4 class="title">Exacte resultaten</h4>
                  <table>
                    <tr><td><a href="https://www.stripinfo.be/reeks/index/1857_Suske_en_Wiske">Suske en Wiske</a></td></tr>
                  </table>
                </section>
                <section class="c6">
                  <h4 class="title">Overige resultaten</h4>
                  <table>
                    <tr><td><a href="https://www.stripinfo.be/reeks/index/13444_Junior_Suske_en_Wiske">Junior Suske en Wiske</a></td></tr>
                    <tr><td><a href="https://www.stripinfo.be/reeks/index/2046_Klein_Suske_en_Wiske">Klein Suske en Wiske</a></td></tr>
                  </table>
                </section>
              </section>
            </div>
            </body></html>
        """.trimIndent()

        private val SUSKE_SERIES_HTML = """
            <!DOCTYPE html><html><body>
            <h1 class="c12">
              <a href="https://www.stripinfo.be/reeks/index/1857_Suske_en_Wiske">
                <img src="https://www.stripinfo.be/images/images/1857.jpg" alt="Suske en Wiske" class="pageLogo">
              </a>
            </h1>
            <section id="ComicSeries" itemscope itemtype="https://schema.org/ComicSeries">
              <section class="c10" itemprop="name" content="Suske en Wiske">
                <span itemprop="url" content="https://www.stripinfo.be/reeks/index/1857_Suske_en_Wiske"></span>
                <span itemprop="startDate" content="1946-01-01"></span>
                <span itemprop="endDate" content="2026-12-31"></span>
                <span itemprop="aggregateRating" itemscope itemtype="https://schema.org/AggregateRating">
                  <span itemprop="reviewCount" content="12323"></span>
                  <span itemprop="ratingValue" content="7.04"></span>
                </span>
                <table class="lijst">
                  <tr itemscope itemprop="isPartOf" itemtype="https://schema.org/ComicIssue">
                    <td itemprop="issueNumber">0</td>
                    <td class="firstcol"><a href="https://www.stripinfo.be/reeks/strip/20822_Suske_en_Wiske_0" itemprop="url"><span itemprop="name">Rikki en Wiske</span></a></td>
                    <td class="secondcol">1946-2015</td>
                  </tr>
                  <tr itemscope itemprop="isPartOf" itemtype="https://schema.org/ComicIssue">
                    <td itemprop="issueNumber">1</td>
                    <td class="firstcol"><a href="https://www.stripinfo.be/reeks/strip/10479_Suske_en_Wiske_1" itemprop="url"><span itemprop="name">Op het eiland Amoras</span></a></td>
                    <td class="secondcol">1947-2025</td>
                  </tr>
                  <tr itemscope itemprop="isPartOf" itemtype="https://schema.org/ComicIssue">
                    <td itemprop="issueNumber">2</td>
                    <td class="firstcol"><a href="https://www.stripinfo.be/reeks/strip/10480_Suske_en_Wiske_2" itemprop="url"><span itemprop="name">De vliegende aap</span></a></td>
                    <td class="secondcol">1948-2018</td>
                  </tr>
                </table>
              </section>
              <aside class="c2">
                <h4>Auteur(s)</h4>
                <ul>
                  <li><a href="https://www.stripinfo.be/auteur/index/42_Willy_Vandersteen">Willy Vandersteen</a></li>
                </ul>
                <h4>Uitgever(s)</h4>
                <ul>
                  <li><a href="https://www.stripinfo.be/uitgever/index/5_Standaard">Standaard Uitgeverij</a></li>
                </ul>
                <h4>Oorspronkelijke taal</h4>
                <ul>
                  <li>Nederlands</li>
                </ul>
                <div id="reeksTags">
                  <div><a href="/zoek/zoek?tag=Humor">Humor</a></div>
                  <div><a href="/zoek/zoek?tag=Avontuur">Avontuur</a></div>
                </div>
              </aside>
            </section>
            </body></html>
        """.trimIndent()

        // Based on actual HTML from https://www.stripinfo.be/reeks/strip/16803
        // Series: Plankgas en Plastronneke (ID 1820), Album: Deel 1
        private val PLANKGAS_ALBUM_HTML = """
            <!DOCTYPE html><html><body>
            <section id="ComicSeries" class="row" itemscope itemtype="https://schema.org/ComicSeries">
              <section class="c10" itemprop="name" content="Plankgas en Plastronneke">
                <div itemprop="hasPart" itemscope itemtype="https://schema.org/ComicIssue">
                  <h2 class="title">
                    <span itemprop="issueNumber">1</span>
                    <a href="https://www.stripinfo.be/reeks/strip/16803_Plankgas_1" itemprop="url">
                      <span itemprop="name">Deel 1</span>
                    </a>
                  </h2>
                  <a href="https://www.stripinfo.be/reeks/index/1820_Plankgas_en_Plastronneke">Plankgas en Plastronneke</a>
                  <img src="https://www.stripinfo.be/image.php?i=494603&amp;s=16803" alt="Cover">
                  <table class="innerDividers">
                    <tr>
                      <td>Scenario</td>
                      <td><a href="https://www.stripinfo.be/auteur/index/1926_Urbanus">Urbanus</a></td>
                    </tr>
                    <tr>
                      <td>Tekeningen</td>
                      <td><a href="https://www.stripinfo.be/auteur/index/1256_Stallaert_Dirk">Dirk Stallaert</a></td>
                    </tr>
                    <tr>
                      <td>Uitgever(s)</td>
                      <td itemprop="publisher" itemscope itemtype="https://schema.org/Organization">
                        <a href="https://www.stripinfo.be/uitgever/index/173_Concentra_Media" itemprop="url">
                          <span itemprop="name">Concentra Media</span>
                        </a>
                      </td>
                    </tr>
                  </table>
                  <table class="innerDividers">
                    <tr><td>Jaar</td><td>2006</td></tr>
                    <tr><td>Pagina's</td><td itemprop="pageEnd">32</td></tr>
                    <tr><td>ISBN</td><td itemprop="identifier">9071762971</td></tr>
                    <tr><td>Barcode</td><td>9789071762970</td></tr>
                    <tr><td>Kaft</td><td>Softcover</td></tr>
                    <tr><td>&nbsp;</td><td>Kleur</td></tr>
                    <tr><td>Taal</td><td>Nederlands</td></tr>
                  </table>
                </div>
              </section>
            </section>
            </body></html>
        """.trimIndent()
    }
}
