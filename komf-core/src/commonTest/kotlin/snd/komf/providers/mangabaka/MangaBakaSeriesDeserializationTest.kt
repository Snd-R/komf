package snd.komf.providers.mangabaka

import kotlinx.serialization.json.Json
import snd.komf.providers.mangabaka.api.MangaBakaSearchResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Test to verify that MangaBaka series can be deserialized even when
 * some source fields are missing from the API response.
 * 
 * This addresses the issue where series like 'Tis Time for 'Torture,' Princess
 * would fail to deserialize because the mangadex field was missing.
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
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val result = json.decodeFromString<MangaBakaSearchResponse>(payload)
        val series = result.data.first()

        // Verify the series was deserialized successfully
        assertNotNull(series)
        assertEquals(1, series.id.value)
        assertEquals("Test Series", series.title)
        
        // Verify that missing mangadex field is null
        assertNull(series.source.mangadex)
        
        // Verify that present sources are not null
        assertNotNull(series.source.mangaUpdates)
        assertEquals("12345", series.source.mangaUpdates?.id)
        assertNotNull(series.source.anilist)
        assertEquals(67890, series.source.anilist?.id)
    }

    @Test
    fun `mangabaka series with all source fields missing deserializes successfully`() {
        val payload = """
            {
              "status": 200,
              "data": [
                {
                  "id": 2,
                  "state": "active",
                  "title": "Another Test Series",
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
                  "source": {}
                }
              ]
            }
        """.trimIndent()

        val result = json.decodeFromString<MangaBakaSearchResponse>(payload)
        val series = result.data.first()

        // Verify the series was deserialized successfully
        assertNotNull(series)
        assertEquals(2, series.id.value)
        assertEquals("Another Test Series", series.title)
        
        // Verify all source fields are null
        assertNull(series.source.mangadex)
        assertNull(series.source.mangaUpdates)
        assertNull(series.source.anilist)
        assertNull(series.source.kitsu)
        assertNull(series.source.myAnimeList)
        assertNull(series.source.animeNewsNetwork)
    }

    @Test
    fun `mangabaka series with only mangadex source deserializes successfully`() {
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
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val result = json.decodeFromString<MangaBakaSearchResponse>(payload)
        val series = result.data.first()

        // Verify the series was deserialized successfully
        assertNotNull(series)
        assertEquals(3, series.id.value)
        
        // Verify mangadex is present
        assertNotNull(series.source.mangadex)
        assertEquals("abc-123-def", series.source.mangadex?.id)
        assertEquals(7.8, series.source.mangadex?.rating)
        
        // Verify other sources are null
        assertNull(series.source.mangaUpdates)
        assertNull(series.source.anilist)
    }
}

