package com.kevin.glasslauncher.home

import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.edit.TileHandle
import com.kevin.glasslauncher.motion.DirectionTracker
import com.kevin.glasslauncher.ui.TestTags

/** Which row a tile is in; [tag] is its test tag. */
enum class TilePlace(val tag: String) { Dock(TestTags.DOCK_TILE), Grid(TestTags.GRID_TILE) }

/**
 * What every home tile needs: art, the D-pad tracker for tilt, what Select and a long press do,
 * and where to note that it has focus.
 */
class TileEnv(
    val art: TileArtSource,
    val tracker: DirectionTracker,
    val onLaunch: (AppEntry) -> Unit,
    val onLongPress: (AppEntry, TilePlace, TileHandle) -> Unit,
    val focusMemory: FocusMemory = FocusMemory(),
)
