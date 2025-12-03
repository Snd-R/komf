package snd.komf.mediaserver.metadata

import snd.komf.api.config.TitleSanitizationConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class TitleSanitizerTest {

    private val cfg = TitleSanitizationConfig(
        enabled = true,
        stripSuffixes = listOf("(Volumes)"),
        stripPatterns = listOf("\\s*\\(Chapters\\)$")
    )

    @Test
    fun `strips configured suffixes`() {
        val result = sanitizeTitle("My Series (Volumes)", cfg)
        assertEquals("My Series", result)
    }

    @Test
    fun `applies regex patterns`() {
        val result = sanitizeTitle("My Series (Chapters)", cfg)
        assertEquals("My Series", result)
    }

    @Test
    fun `returns raw title when disabled`() {
        val disabled = cfg.copy(enabled = false)
        val result = sanitizeTitle("My Series (Volumes)", disabled)
        assertEquals("My Series (Volumes)", result)
    }
}

