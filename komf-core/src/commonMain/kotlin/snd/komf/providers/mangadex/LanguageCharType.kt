package snd.komf.providers.mangadex

import java.util.TreeMap

enum class LanguageCharType(
    private val code: String,
    private val ranges: Set<UnicodeRange>,
    private val includedLct: Set<LanguageCharType>,
) {
    Unknown(
        "unknown",
        setOf(),
        setOf(),
    ),
    Special(
        "any",
        setOf(
            UnicodeRange(0x0.toChar(), 0x2f.toChar()),
            UnicodeRange(0x3a.toChar(), 0x40.toChar()),
            UnicodeRange(0x5b.toChar(), 0x60.toChar()),
            UnicodeRange(0x7b.toChar(), 0x7f.toChar()),
        ),
        setOf(),
    ),
    Numbers(
        "any",
        setOf(
            UnicodeRange(0x30.toChar(), 0x39.toChar()),
        ),
        setOf(),
    ),
    English(
        "en",
        setOf(
            UnicodeRange(0x41.toChar(), 0x5A.toChar()), // upper case
            UnicodeRange(0x61.toChar(), 0x7A.toChar()), // lower case
        ),
        setOf(Special, Numbers),
    ),
    Chinese(
        "zh",
        setOf(
            UnicodeRange(0x4e00.toChar(), 0x9fa0.toChar()),
        ),
        setOf(English),
    ),
    Japanese(
        "ja",
        setOf(
            UnicodeRange(0x3040.toChar(), 0x309f.toChar()), // hiragana
            UnicodeRange(0x30A0.toChar(), 0x30FF.toChar()), // katakana
            UnicodeRange(0x31F0.toChar(), 0x31FF.toChar()), // katakana extension
            UnicodeRange(0x3200.toChar(), 0x33FF.toChar()), // Compatibility
            UnicodeRange(0xFF00.toChar(), 0xFF0F.toChar()), // half width
        ),
        setOf(Chinese, English), // include chinese for kanji
    ),
    Korean(
        "ko",
        setOf(
            UnicodeRange(0x1100.toChar(), 0x11FF.toChar()), // 자모
            UnicodeRange(0x3130.toChar(), 0x318F.toChar()), // Compatibility Elements
            UnicodeRange(0xA960.toChar(), 0xA97F.toChar()), // 초성
            UnicodeRange(0xAC00.toChar(), 0xD7AF.toChar()), // Syllables
            UnicodeRange(0xD7B0.toChar(), 0xD7FF.toChar()), // 중성, 종성
        ),
        setOf(English),
    );

    fun getCode(): String {
        return code
    }

    fun primary(c: Char): Boolean {
        for (r in ranges) {
            if ((r.start <= c) && (r.end >= c)) {
                return true
            }
        }

        return false
    }

    fun secondary(c: Char): Boolean {
        for (i in includedLct) {
            if (i.includes(c)) {
                return true
            }
        }

        return false
    }

    fun includes(c: Char): Boolean {
        return primary(c) || secondary(c)
    }

    fun includes(lct: LanguageCharType): Boolean {
        if (this == lct) {
            return true
        } else {
            for (iLct in includedLct) {
                if (iLct.includes(lct)) {
                    return true
                }
            }
        }
        return false
    }

    data class UnicodeRange(val start: Char, val end: Char)
    data class UnicodeRangeEntry(val range: UnicodeRange, val lct: LanguageCharType)

    companion object {
        private val lookup: TreeMap<Char, UnicodeRangeEntry> = TreeMap<Char, UnicodeRangeEntry>()

        init {
            for (lct in entries) {
                for (range in lct.ranges) {
                    lookup[range.start] = UnicodeRangeEntry(range, lct)
                }
            }
        }

        fun detect(str: String, default: String? = null): Set<String> {
            val candidates = mutableSetOf<LanguageCharType>()

            for (c in str) {
                // look for non-"en" characters
                val candidate = lookup.floorEntry(c)
                if ((candidate != null) && (c <= candidate.value.range.end)) {
                    var foundCandidate = false
                    var replaceCandidate: LanguageCharType? = null
                    for (prevCandidate in candidates) {
                        if (prevCandidate.includes(candidate.value.lct)) {
                            foundCandidate = true
                        } else if (candidate.value.lct.includes(prevCandidate)) {
                            replaceCandidate = prevCandidate
                            break
                        }
                    }

                    if (replaceCandidate != null) {
                        candidates.remove(replaceCandidate)
                    }

                    if (!foundCandidate) {
                        candidates.add(candidate.value.lct)
                    }
                } else {
                    // TODO: handle this.  Not in the currently defined languages (only CJK have
                    //  been defined properly).  need more language definitions as well as valid
                    //  inclusions.
                    //  i.e. if we want to allow mixing japanese with french and treat it as
                    //  japanese, need to define french range + include french in japanese
                    //  inclusion.  For now, will add in the "unknown" language and let the
                    //  caller decide what to do with this.
                    candidates.add(Unknown)
                }
            }

            val codes = candidates.map { it.getCode() }.toMutableSet()

            if (default != null && codes.contains("any")) {
                // if default language specified and only specials/numbers were found,
                // treat it as the default language.  i.e. !111!! would return "any" without
                // default specified, but will return "en" if "en" was the value of default.
                codes.remove("any")
                codes.add(default)
            }

            return codes.toSet()
        }
    }

}


