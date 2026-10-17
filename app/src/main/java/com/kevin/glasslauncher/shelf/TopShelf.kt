package com.kevin.glasslauncher.shelf

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.apps.BannerCache
import com.kevin.glasslauncher.home.AppTile
import com.kevin.glasslauncher.home.HomeLayout
import com.kevin.glasslauncher.motion.DirectionTracker
import com.kevin.glasslauncher.motion.Motion
import com.kevin.glasslauncher.ui.TestTags

private val ShelfTopPadding = 16.dp
private val CardToRow = 14.dp
private val LabelToRow = 8.dp

// Room below the row for a focused tile's 1.15x scale before the dock's glass slab.
private val ShelfBottomPadding = 8.dp

private val RowLabelStyle = TextStyle(
    color = Color(0xD9FFFFFF),
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium,
    shadow = Shadow(Color(0x99000000), Offset(0f, 1f), 4f),
)

/**
 * Top shelf above the dock (spec "Top shelf", M0 result: no channel data). The featured app
 * ([focused] dock app, else the focused Recent tile) shows as a large card; below it a Recent row
 * of up to [MAX_SHELF_RECENTS] tiles is always there when [recents] (most recent first) is not
 * empty, so Up from the dock reaches it. No recents and no focused app: nothing focusable.
 * Fills [modifier]'s bounds; tiles tagged TestTags.SHELF_TILE, root TestTags.TOP_SHELF.
 */
@Composable
fun TopShelf(
    focused: AppEntry?,
    recents: List<AppEntry>,
    banners: BannerCache,
    tracker: DirectionTracker,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shelfFocus by remember { mutableStateOf<AppEntry?>(null) }
    val shown = shelfRecents(recents)
    val featured = featuredApp(focused, shelfFocus, shown)
    Column(
        modifier
            .testTag(TestTags.TOP_SHELF)
            .padding(top = ShelfTopPadding, bottom = ShelfBottomPadding),
    ) {
        Crossfade(featured, Modifier.weight(1f).fillMaxWidth(), Motion.crossfadeSpring, label = "shelf card") { app ->
            if (app != null) ShelfCard(app, banners, Modifier.fillMaxSize())
        }
        if (shown.isNotEmpty()) {
            Spacer(Modifier.height(CardToRow))
            RecentRow(shown, banners, tracker, onLaunch) { app, hasFocus ->
                if (hasFocus) shelfFocus = app else if (shelfFocus == app) shelfFocus = null
            }
        }
    }
}

@Composable
private fun RecentRow(
    apps: List<AppEntry>,
    banners: BannerCache,
    tracker: DirectionTracker,
    onLaunch: (AppEntry) -> Unit,
    onFocus: (AppEntry, Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        BasicText(stringResource(R.string.look_recent), Modifier.padding(bottom = LabelToRow), style = RowLabelStyle)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(HomeLayout.TileGap)) {
            for (app in apps) {
                key(app.key) {
                    AppTile(
                        app, banners, tracker, onClick = { onLaunch(app) },
                        Modifier.weight(1f).testTag(TestTags.SHELF_TILE).onFocusChanged { onFocus(app, it.isFocused) },
                    )
                }
            }
            // Empty slots keep tiles on the grid's column widths.
            repeat(MAX_SHELF_RECENTS - apps.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}
