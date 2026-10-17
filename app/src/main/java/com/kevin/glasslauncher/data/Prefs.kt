package com.kevin.glasslauncher.data

enum class Look { Glass, Solid }

/** How often the built-in wallpaper changes on its own. */
enum class Rotation { Off, Hourly, Daily }

const val MIN_DOCK_SIZE = 4
const val MAX_DOCK_SIZE = 6
const val DEFAULT_DOCK_SIZE = MAX_DOCK_SIZE
const val DEFAULT_WALLPAPER_ID = "aurora"
const val MAX_RECENTS = 10

/**
 * Everything the user can change. [favorites] is null until the user (or first run) saves a dock;
 * then the dock follows it exactly. [recents] is most recent first. [sleepAtMillis] is the
 * wall-clock time the sleep timer fires, 0 when off. [rotationSinceMillis] is when the user
 * last picked a wallpaper or rotation mode: rotation steps count from there, so a pick shows at once.
 */
data class Prefs(
    val favorites: List<String>? = null,
    val hidden: Set<String> = emptySet(),
    val look: Look = Look.Glass,
    val clock24: Boolean = false,
    val hud: Boolean = false,
    val dockSize: Int = DEFAULT_DOCK_SIZE,
    val wallpaperId: String = DEFAULT_WALLPAPER_ID,
    val rotation: Rotation = Rotation.Daily,
    val rotationSinceMillis: Long = 0L,
    val topShelf: Boolean = true,
    val appBackdrop: Boolean = true,
    val recents: List<String> = emptyList(),
    val sleepAtMillis: Long = 0L,
)

// App keys are "package/activity" and never contain a newline, so a newline-joined string keeps order.
private const val SEPARATOR = "\n"

fun encodeList(list: List<String>): String = list.joinToString(SEPARATOR)

fun decodeList(s: String): List<String> = if (s.isEmpty()) emptyList() else s.split(SEPARATOR)

/** [key] moved to the front of [recents], no duplicates, at most [max] entries. */
fun pushRecent(recents: List<String>, key: String, max: Int = MAX_RECENTS): List<String> =
    (listOf(key) + recents.filter { it != key }).take(max)
