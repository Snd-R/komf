// adapted to kotlin from https://github.com/gpanther/java-nat-sort
package snd.komf.util

/**
 * Compares Strings (or any other CharSequence subclass) using the
 * [natural sort](http://blog.codinghorror.com/sorting-for-humans-natural-sort-order/) /
 * [alphanum algorithm](http://www.davekoelle.com/alphanum.html) which gives a more
 * "natural" ordering when presenting the sorted list of strings to humans.
 *
 *
 *  * Does not play nice with Unicode, especially characters which are outside of the BMP (ie.
 * codepoints with values larger than [Character.MAX_VALUE]).
 *  * Does not handle fractions or grouping characters properly.
 *  * Only understands integer values up to 2^64-1.
 *
 */
fun <T : CharSequence?> natSortComparator(): Comparator<T> {
    return SimpleNaturalComparator()
}

fun <T : CharSequence?> caseInsensitiveNatSortComparator(): Comparator<T> {
    return CaseInsensitiveSimpleNaturalComparator()
}

private class CaseInsensitiveSimpleNaturalComparator<T : CharSequence?> : AbstractSimpleNaturalComparator<T>() {
    override fun compareChars(c1: Char, c2: Char): Int {
        return c1.lowercaseChar() - c2.lowercaseChar()
    }
}

private class SimpleNaturalComparator<T : CharSequence?> : AbstractSimpleNaturalComparator<T>() {
    override fun compareChars(c1: Char, c2: Char): Int {
        return c1.code - c2.code
    }
}

private abstract class AbstractSimpleNaturalComparator<T : CharSequence?> : Comparator<T> {
    override fun compare(sequence1: T, sequence2: T): Int {
        val len1 = sequence1!!.length
        val len2 = sequence2!!.length
        var idx1 = 0
        var idx2 = 0

        while (idx1 < len1 && idx2 < len2) {
            val c1 = sequence1[idx1++]
            val c2 = sequence2[idx2++]

            val isDigit1 = isDigit(c1)
            val isDigit2 = isDigit(c2)

            when {
                isDigit1 && !isDigit2 -> return -1
                !isDigit1 && isDigit2 -> return 1
                !isDigit1 && !isDigit2 -> {
                    val c = compareChars(c1, c2)
                    if (c != 0) {
                        return c
                    }
                }

                else -> {
                    var num1 = parse(c1)
                    while (idx1 < len1) {
                        val digit = sequence1[idx1++]
                        if (isDigit(digit)) {
                            num1 = num1 * 10 + parse(digit)
                        } else {
                            idx1--
                            break
                        }
                    }

                    var num2 = parse(c2)
                    while (idx2 < len2) {
                        val digit = sequence2[idx2++]
                        if (isDigit(digit)) {
                            num2 = num2 * 10 + parse(digit)
                        } else {
                            idx2--
                            break
                        }
                    }

                    if (num1 != num2) {
                        return compareUnsigned(num1, num2)
                    }

                }
            }
        }

        return when {
            idx1 < len1 -> 1
            idx2 < len2 -> -1
            else -> 0
        }
    }

    abstract fun compareChars(c1: Char, c2: Char): Int

    companion object {
        private fun compareUnsigned(num1: Long, num2: Long): Int {
            return compare(num1 + Long.MIN_VALUE, num2 + Long.MIN_VALUE)
        }

        private fun compare(x: Long, y: Long): Int {
            return if ((x < y)) -1 else (if ((x == y)) 0 else 1)
        }

        private fun parse(c1: Char): Long {
            return (c1.code - '0'.code).toLong()
        }

        private fun isDigit(c: Char): Boolean {
            return ('0' <= c) and (c <= '9')
        }
    }
}