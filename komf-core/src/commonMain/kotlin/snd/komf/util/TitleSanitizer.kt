package snd.komf.util

import snd.komf.api.config.TitleSanitizationConfig

fun sanitizeTitle(raw: String, config: TitleSanitizationConfig): String {
    if (!config.enabled) return raw
    
    // Trim first to catch trailing whitespace before suffix removal
    var result = raw.trim()
    
    // 1) Strip explicit suffixes
    config.stripSuffixes.forEach { suffix ->
        if (suffix.isNotBlank() && result.endsWith(suffix, ignoreCase = true)) {
            result = result.removeSuffix(suffix)
        }
    }
    
    // 2) Apply regex patterns (ignore empty patterns)
    config.stripPatterns.forEach { pattern ->
        if (pattern.isNotBlank()) {
            result = result.replace(Regex(pattern), "")
        }
    }
    
    return result.trim()
}

