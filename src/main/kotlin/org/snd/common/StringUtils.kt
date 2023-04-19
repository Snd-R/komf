package org.snd.common

import org.apache.commons.lang3.StringUtils


object StringUtils {
    private val fullwidthRegex = "[\uff01-\uff5e]".toRegex()

    fun replaceFullwidthChars(input: String) = input.replace(fullwidthRegex) { match ->
        Character.toString(match.value.codePointAt(0) - 0xfee0)
    }

    fun stripAccents(input: String): String = StringUtils.stripAccents(input)

}