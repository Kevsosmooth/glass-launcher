package com.kevin.glasslauncher.edit

/** Whether a grid app can be added to the dock. */
enum class DockSlot { Free, InDock, Full }

/** [key] moved [delta] places (clamped to the ends); unchanged if [key] is not in [dock]. */
fun moveInDock(dock: List<String>, key: String, delta: Int): List<String> {
    val from = dock.indexOf(key)
    if (from < 0) return dock
    val to = (from + delta).coerceIn(0, dock.lastIndex)
    if (to == from) return dock
    return dock.toMutableList().apply { add(to, removeAt(from)) }
}

fun removeFromDock(dock: List<String>, key: String): List<String> = dock.filter { it != key }

fun dockSlot(dock: List<String>, key: String, dockSize: Int): DockSlot = when {
    key in dock -> DockSlot.InDock
    dock.size >= dockSize -> DockSlot.Full
    else -> DockSlot.Free
}

/** [key] appended at the end; unchanged if it is already docked or the dock is full. */
fun addToDock(dock: List<String>, key: String, dockSize: Int): List<String> =
    if (dockSlot(dock, key, dockSize) == DockSlot.Free) dock + key else dock
