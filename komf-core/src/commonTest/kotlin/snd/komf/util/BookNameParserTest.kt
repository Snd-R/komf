package snd.komf.util

import snd.komf.model.BookRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookNameParserTest {

    @Test
    fun `getBookNumber parses number at end of string`() {
        assertEquals(BookRange(123.0), BookNameParser.getBookNumber("Batman 123"))
    }

    @Test
    fun `getBookNumber parses hash-prefixed number`() {
        assertEquals(BookRange(5.0), BookNameParser.getBookNumber("Spider-Man #5"))
    }

    @Test
    fun `getBookNumber parses Issue prefix`() {
        assertEquals(BookRange(42.0), BookNameParser.getBookNumber("Issue 42"))
    }

    @Test
    fun `getBookNumber parses Volume prefix`() {
        assertEquals(BookRange(3.0), BookNameParser.getBookNumber("Volume 3"))
    }

    @Test
    fun `getBookNumber parses number with parenthetical suffix`() {
        assertEquals(BookRange(10.0), BookNameParser.getBookNumber("Series 10 (2020)"))
    }

    @Test
    fun `getBookNumber parses dash-delimited number`() {
        assertEquals(
            BookRange(195.0),
            BookNameParser.getBookNumber("Suske En Wiske HQ - 195 - De Hippe Heksen")
        )
    }

    @Test
    fun `getBookNumber parses dash-delimited leading zeros`() {
        assertEquals(
            BookRange(1.0),
            BookNameParser.getBookNumber("The Walking Dead - 001 - Days Gone Bye")
        )
    }

    @Test
    fun `getBookNumber parses dash-delimited decimal number`() {
        assertEquals(
            BookRange(12.5),
            BookNameParser.getBookNumber("Series - 12.5 - Special Edition")
        )
    }

    @Test
    fun `getBookNumber parses dash-delimited range`() {
        assertEquals(
            BookRange(1.0, 5.0),
            BookNameParser.getBookNumber("Series - 1-5 - Omnibus")
        )
    }

    @Test
    fun `getBookNumber prefers existing regex over dash-delimited`() {
        // "Issue 5" should match the Issue regex, not the dash pattern
        assertEquals(BookRange(5.0), BookNameParser.getBookNumber("Series - 2099 - Issue 5"))
    }

    @Test
    fun `getBookNumber returns null for unrecognized format`() {
        assertNull(BookNameParser.getBookNumber("No Number Here"))
    }
}
