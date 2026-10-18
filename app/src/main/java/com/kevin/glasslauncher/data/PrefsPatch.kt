package com.kevin.glasslauncher.data

/**
 * Some of the user's settings, e.g. read from a backup file. A null field is left as it is when
 * the patch is applied. [favorites] is the dock order. Recents, the sleep timer, the rotation
 * start time and the first-run Home card are not settings (usage history, running timers, one-off
 * hints) and are never part of a patch.
 */
data class PrefsPatch(
    val look: Look? = null,
    val wallpaperId: String? = null,
    val rotation: Rotation? = null,
    val appBackdrop: Boolean? = null,
    val topShelf: Boolean? = null,
    val clock24: Boolean? = null,
    val dockSize: Int? = null,
    val favorites: List<String>? = null,
    val hidden: Set<String>? = null,
    val hud: Boolean? = null,
    val appOrder: AppOrder? = null,
) {
    /** How many settings this patch changes. */
    val size: Int
        get() = listOf(look, wallpaperId, rotation, appBackdrop, topShelf, clock24, dockSize, favorites, hidden, hud, appOrder)
            .count { it != null }
}

/** Every setting in these prefs; the dock is left out while it is still the default one. */
fun Prefs.toPatch(): PrefsPatch = PrefsPatch(
    look = look,
    wallpaperId = wallpaperId,
    rotation = rotation,
    appBackdrop = appBackdrop,
    topShelf = topShelf,
    clock24 = clock24,
    dockSize = dockSize,
    favorites = favorites,
    hidden = hidden,
    hud = hud,
    appOrder = appOrder,
)
