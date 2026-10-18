package com.kevin.glasslauncher.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesTest {
    private val bullet = "\u2022 "

    @Test
    fun headingsLoseTheirMarks() {
        assertEquals("What's new\n\nFixes", releaseNotesText("## What's new\n\n### Fixes ###"))
    }

    @Test
    fun listItemsGetABullet() {
        assertEquals("${bullet}One\n${bullet}Two\n  ${bullet}Nested\n1. Numbered", releaseNotesText("- One\n* Two\n  + Nested\n1. Numbered"))
    }

    @Test
    fun linksAndImagesKeepOnlyTheirText() {
        assertEquals(
            "See the install guide and logo.",
            releaseNotesText("See the [install guide](https://example.com/x?a=1) and ![logo](https://e.com/l.png)."),
        )
        assertEquals("Ref link", releaseNotesText("[Ref link][1]\n\n[1]: https://example.com"))
    }

    @Test
    fun emphasisCodeQuotesAndHtmlAreStripped() {
        val md = "**Bold** _it_ `code` ~~old~~ snake_case\n> quoted\n<b>tag</b><!-- hidden -->\n```kotlin\nval x = 1\n```\n---"
        assertEquals("Bold it code old snake_case\nquoted\ntag\nval x = 1", releaseNotesText(md))
    }

    @Test
    fun escapedMarkersStayAsText() {
        assertEquals("2 * 3 = 6 #1", releaseNotesText("2 \\* 3 = 6 \\#1"))
    }

    @Test
    fun crlfAndBlankRunsAreTidied() {
        assertEquals("A\n\nB", releaseNotesText("A\r\n\r\n\r\n\r\nB\r\n"))
    }

    @Test
    fun controlAndBidiCharactersAreRemoved() {
        assertEquals("abc", releaseNotesText("a\u0007b\u202Ec"))
    }

    @Test
    fun longNotesAreCappedWithAnEllipsis() {
        val text = releaseNotesText("word ".repeat(2000))
        assertTrue(text.length <= MAX_NOTES_CHARS)
        assertTrue(text.endsWith("\u2026"))
        assertEquals("short", releaseNotesText("short"))
        assertFalse(releaseNotesText("x".repeat(MAX_NOTES_CHARS)).endsWith("\u2026"))
    }

    @Test
    fun onlyTheStartOfAHugeBodyIsRead() {
        val hidden = "<!--" + "x".repeat(MAX_NOTES_SOURCE_CHARS) + "-->visible"
        assertFalse(releaseNotesText(hidden).contains("visible"))
    }

    @Test(timeout = 10_000)
    fun unterminatedCommentsAndTagsStayFast() {
        releaseNotesText("<!--".repeat(100_000))
        releaseNotesText("<a".repeat(100_000))
    }
}
