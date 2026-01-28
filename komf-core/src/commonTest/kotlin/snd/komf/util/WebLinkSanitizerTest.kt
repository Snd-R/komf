package snd.komf.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import snd.komf.model.WebLink

class WebLinkSanitizerTest {

    @Test
    fun toValidHttpUrlOrNull_returnsEncodedUrl() {
        val sanitized = "https://example.com/a b?x=y".toValidHttpUrlOrNull()
        assertEquals("https://example.com/a%20b?x=y", sanitized)
    }

    @Test
    fun toValidHttpUrlOrNull_trimsWhitespace() {
        val sanitized = "  https://example.com  ".toValidHttpUrlOrNull()
        assertEquals("https://example.com", sanitized)
    }

    @Test
    fun toValidHttpUrlOrNull_rejectsInvalidScheme() {
        val sanitized = "htttps://www.alpha-manga.com/manga/121000501".toValidHttpUrlOrNull()
        assertNull(sanitized)
    }

    @Test
    fun toValidHttpUrlOrNull_rejectsMissingHost() {
        val sanitized = "https:///path".toValidHttpUrlOrNull()
        assertNull(sanitized)
    }

    @Test
    fun sanitizeHttpLinks_filtersAndNormalizes() {
        val links = listOf(
            WebLink("Good", "https://example.com/a b"),
            WebLink("Bad", "htttps://example.com"),
        )

        val sanitized = links.sanitizeHttpLinks()
        assertEquals(1, sanitized.size)
        assertEquals("Good", sanitized.first().label)
        assertNotNull(sanitized.first().url)
        assertEquals("https://example.com/a%20b", sanitized.first().url)
    }
}
