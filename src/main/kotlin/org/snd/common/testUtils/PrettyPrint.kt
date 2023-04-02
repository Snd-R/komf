package org.snd.common.testUtils

import kotlin.math.max

fun Any.prettyPrint(indentLevel: Int = 0, indentWidth: Int = 4, maxLength: Int = 1000): String {
    var curIndentLevel = indentLevel

    fun padding() = "".padStart(curIndentLevel * indentWidth)

    val toString = toString()
    val stringBuilder = StringBuilder(toString.length)

    var i = 0
    val buffer = StringBuffer()
    while (i < toString.length) {
        when (val char = toString[i]) {
            '(', '[', '{' -> {
                curIndentLevel++
                buffer.appendLine(char).append(padding())
                // stringBuilder.appendLine(char).append(padding())
            }

            ')', ']', '}' -> {
                curIndentLevel--
                buffer.appendLine().append(padding()).append(char)
                // stringBuilder.appendLine().append(padding()).append(char)
                stringBuilder.append(truncateString(buffer.toString(), maxLength, padding()))
                buffer.setLength(0)
            }

            ',' -> {
                buffer.appendLine(char).append(padding())
                // stringBuilder.appendLine(char).append(padding())
                // ignore space after comma as we have added a newline
                val nextChar = toString.getOrElse(i + 1) { char }
                if (nextChar == ' ') i++
            }

            else -> {
                buffer.append(char)
                // stringBuilder.append(char)
            }
        }
        i++
    }

    return stringBuilder.toString()
}

fun truncateString(str: String, maxLength: Int, padding: String): String {
    if (str.length <= maxLength) {
        return str
    }
    val firstHalfLength = maxLength / 2
    val lastHalfLength = maxLength - firstHalfLength
    return str.substring(0, firstHalfLength) +
            "$padding ... \n" +
            str.substring(str.length - lastHalfLength)
}