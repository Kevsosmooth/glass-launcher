package com.kevin.glasslauncher.update

/** Longest release-notes text the card shows. */
const val MAX_NOTES_CHARS = 4000

/** Raw markdown read at most; keeps the regexes fast on hostile input (an unterminated `<!--` or `<`). */
const val MAX_NOTES_SOURCE_CHARS = 16_000

private const val BULLET = "\u2022 "
private const val ELLIPSIS = "\u2026"

// Backslash escapes are parked on private-use characters so the marker stripping below skips them.
private const val ESCAPABLE = "\\`*_{}[]()#+-.!>~|<"
private const val PARK_BASE = 0xE000
private val ESCAPE = Regex("""\\([\\`*_{}\[\]()#+\-.!>~|<])""")

private val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
private val AUTOLINK = Regex("""<(https?://[^>\s]+)>""")
private val TAG = Regex("""</?[a-zA-Z][^>]*>""")
private val IMAGE = Regex("""!\[([^\]]*)]\([^)]*\)""")
private val LINK = Regex("""\[([^\]]*)]\([^)]*\)""")
private val REF_LINK = Regex("""\[([^\]]+)]\[[^\]]*]""")
private val FENCE = Regex("""^\s*(```|~~~).*$""")
private val HEADING = Regex("""^\s{0,3}#{1,6}\s+(.*?)(\s+#+)?\s*$""")
private val RULE = Regex("""^\s{0,3}([-*_=])(\s*\1){2,}\s*$""")
private val REF_DEFINITION = Regex("""^\s{0,3}\[[^\]]+]:\s*\S.*$""")
private val QUOTE = Regex("""^\s{0,3}>\s?""")
private val LIST_ITEM = Regex("""^(\s*)[-*+]\s+""")
private val EMPHASIS = Regex("""\*\*|__|~~|`|\*""")
private val LONE_UNDERSCORE = Regex("""(?<![\p{L}\p{N}])_|_(?![\p{L}\p{N}])""")
private val CONTROL = Regex("""[\u0000-\u0008\u000B-\u001F\u007F\u202A-\u202E\u2066-\u2069]""")
private val BLANK_RUN = Regex("""\n{3,}""")

/**
 * GitHub release notes (untrusted markdown) as plain text for a Text: headings, emphasis, code
 * marks and quotes lose their markers, list items get a bullet, links and images keep only their
 * text, HTML is dropped, and the result is capped at [MAX_NOTES_CHARS]. Only the first
 * [MAX_NOTES_SOURCE_CHARS] of [markdown] are read.
 */
fun releaseNotesText(markdown: String): String {
    val cleaned = markdown.take(MAX_NOTES_SOURCE_CHARS).replace("\r\n", "\n").replace('\r', '\n')
        .replace(ESCAPE) { park(it.groupValues[1][0]) }
        .replace(COMMENT, "")
        .replace(AUTOLINK, "$1")
        .replace(TAG, "")
        .replace(CONTROL, "")
    val text = cleaned.lines().mapNotNull(::plainLine).joinToString("\n")
        .replace(BLANK_RUN, "\n\n")
        .trim()
        .let(::unpark)
    return if (text.length <= MAX_NOTES_CHARS) text else text.take(MAX_NOTES_CHARS - 1).trimEnd() + ELLIPSIS
}

/** One markdown line as plain text; null drops it (code fences, rules, link definitions). */
private fun plainLine(line: String): String? {
    if (FENCE.matches(line) || RULE.matches(line) || REF_DEFINITION.matches(line)) return null
    var out = HEADING.matchEntire(line)?.groupValues?.get(1) ?: line
    out = out.replace(QUOTE, "")
    out = LIST_ITEM.find(out)?.let { m -> m.groupValues[1] + BULLET + out.substring(m.range.last + 1) } ?: out
    return out.replace(IMAGE, "$1")
        .replace(LINK, "$1")
        .replace(REF_LINK, "$1")
        .replace(EMPHASIS, "")
        .replace(LONE_UNDERSCORE, "")
        .trimEnd()
}

private fun park(c: Char): String = (PARK_BASE + ESCAPABLE.indexOf(c)).toChar().toString()

private fun unpark(text: String): String = buildString(text.length) {
    for (c in text) {
        val i = c.code - PARK_BASE
        append(if (i in ESCAPABLE.indices) ESCAPABLE[i] else c)
    }
}
