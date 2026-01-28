package snd.komf.util

import io.ktor.http.parseUrl
import snd.komf.model.WebLink

/**
 * Parse and validate a URL string for use as a web link.
 *
 * Policy:
 * - Only allow http/https URLs.
 * - Require a non-blank host.
 * - Return a normalized, properly-encoded URL string (see [toStingEncoded]).
 */
fun String.toValidHttpUrlOrNull(): String? {
    val raw = trim()
    if (raw.isBlank()) return null

    // Guard against empty authority like "https:///path" which some parsers may interpret oddly.
    val lower = raw.lowercase()
    if (lower.startsWith("http:///") || lower.startsWith("https:///")) return null

    val url = parseUrl(raw) ?: return null
    val scheme = url.protocol.name.lowercase()
    if (scheme != "http" && scheme != "https") return null
    if (url.host.isBlank()) return null

    return url.toStingEncoded()
}

/**
 * Filter out invalid/unsupported links and normalize URLs for valid entries.
 *
 * This helper is intentionally logging-free so callers can decide how/where to report drops.
 */
fun Iterable<WebLink>.sanitizeHttpLinks(): List<WebLink> {
    return mapNotNull { link ->
        val sanitizedUrl = link.url.toValidHttpUrlOrNull() ?: return@mapNotNull null
        if (sanitizedUrl == link.url) link else WebLink(link.label, sanitizedUrl)
    }
}
