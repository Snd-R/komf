package snd.komf.providers.mangabaka

import kotlinx.serialization.json.Json
import snd.komf.providers.mangabaka.api.MangaBakaSearchResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests to verify that MangaBaka series can be deserialized correctly
 * with various combinations of source fields.
 *
 * In particular:
 * - `mangadex` is optional and may be absent.
 * - Other source fields are always present as keys, with NULL value when
 *   there is no data for that provider, per the intended schema.
 */
class MangaBakaSeriesDeserializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `mangabaka series without mangadex source deserializes successfully`() {
        val payload = """
            {
              "status": 200,
              "data": [
                {
                  "id": 1,
                  "state": "active",
                  "title": "Test Series",
                  "cover": {
                    "raw": null,
                    "x150": null,
                    "x250": null,
                    "x350": null
                  },
                  "status": "completed",
                  "is_licensed": false,
                  "content_rating": "safe",
                  "type": "manga",
                  "source": {
                    "manga_updates": { 
                      "id": "12345",
                      "rating": 8.5
                    },
                    "anilist": {
                      "id": 67890,
                      "rating": 9.0
                    },
                    "anime_news_network": null,
                    "kitsu": null,
                    "mangadex": null,
                    "my_anime_list": null
                  }
                }
              ]
            }
        """.trimIndent()

        val result = json.decodeFromString<MangaBakaSearchResponse>(payload)
        val series = result.data.first()

        // Basic sanity checks
        assertNotNull(series)
        assertEquals(1, series.id.value)
        assertEquals("Test Series", series.title)

        // Mangadex is effectively "absent" (null)
        assertNull(series.source.mangadex)

        // Present sources are not null and have expected values
        assertNotNull(series.source.mangaUpdates)
        assertEquals("12345", series.source.mangaUpdates?.id)
        assertEquals(8.5, series.source.mangaUpdates?.rating)

        assertNotNull(series.source.anilist)
        assertEquals(67890, series.source.anilist?.id)
        assertEquals(9.0, series.source.anilist?.rating)

        // Providers with explicit null values
        assertNull(series.source.kitsu)
        assertNull(series.source.myAnimeList)
        assertNull(series.source.animeNewsNetwork)
    }

    @Test
    fun `mangabaka series with all non-mangadex sources null deserializes successfully`() {
        val payload = """
            {
              "status": 200,
              "data": [
                {
                  "id": 2,
                  "state": "active",
                  "title": "All Null Sources Series",
                  "cover": {
                    "raw": null,
                    "x150": null,
                    "x250": null,
                    "x350": null
                  },
                  "status": "releasing",
                  "is_licensed": true,
                  "content_rating": "suggestive",
                  "type": "manhwa",
                  "source": {
                    "manga_updates": null,
                    "anilist": null,
                    "anime_news_network": null,
                    "kitsu": null,
                    "my_anime_list": null
                    // mangadex omitted entirely to reflect it being removed
                  }
                }
              ]
            }
        """.trimIndent()

        val result = json.decodeFromString<MangaBakaSearchResponse>(payload)
        val series = result.data.first()

        // Basic sanity checks
        assertNotNull(series)
        assertEquals(2, series.id.value)
        assertEquals("All Null Sources Series", series.title)

        // All non-mangadex sources are explicitly null
        assertNull(series.source.mangaUpdates)
        assertNull(series.source.anilist)
        assertNull(series.source.kitsu)
        assertNull(series.source.myAnimeList)
        assertNull(series.source.animeNewsNetwork)

        // Mangadex may be absent; property should still be null
        assertNull(series.source.mangadex)
    }

    @Test
    fun `mangabaka series with only mangadex populated deserializes successfully`() {
        val payload = """
            {
              "status": 200,
              "data": [
                {
                  "id": 3,
                  "state": "active",
                  "title": "MangaDex Only Series",
                  "cover": {
                    "raw": null,
                    "x150": null,
                    "x250": null,
                    "x350": null
                  },
                  "status": "completed",
                  "is_licensed": false,
                  "content_rating": "safe",
                  "type": "manga",
                  "source": {
                    "mangadex": {
                      "id": "abc-123-def",
                      "rating": 7.8
                    },
                    "manga_updates": null,
                    "anilist": null,
                    "anime_news_network": null,
                    "kitsu": null,
                    "my_anime_list": null
                  }
                }
              ]
            }
        """.trimIndent()

        val result = json.decodeFromString<MangaBakaSearchResponse>(payload)
        val series = result.data.first()

        // Basic sanity checks
        assertNotNull(series)
        assertEquals(3, series.id.value)
        assertEquals("MangaDex Only Series", series.title)

        // Mangadex is populated and correctly deserialized
        assertNotNull(series.source.mangadex)
        assertEquals("abc-123-def", series.source.mangadex?.id)
        assertEquals(7.8, series.source.mangadex?.rating)

        // Other sources are present as keys but null
        assertNull(series.source.mangaUpdates)
        assertNull(series.source.anilist)
        assertNull(series.source.kitsu)
        assertNull(series.source.myAnimeList)
        assertNull(series.source.animeNewsNetwork)
    }
}
