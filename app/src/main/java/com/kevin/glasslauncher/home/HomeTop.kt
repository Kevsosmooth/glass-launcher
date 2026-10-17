package com.kevin.glasslauncher.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.apps.BannerCache
import com.kevin.glasslauncher.motion.DirectionTracker
import com.kevin.glasslauncher.shelf.TopShelf
import com.kevin.glasslauncher.ui.TestTags

/** Height of the list's top item: with the top shelf, or just the status pill. */
fun topAreaHeight(topShelf: Boolean): Dp = if (topShelf) HomeLayout.TopArea else HomeLayout.TopAreaCompact

/** Top of the list: the top shelf (when on) filling the area, the status pill top right. */
@Composable
fun HomeTop(
    state: HomeState,
    dockFocus: DockFocus,
    banners: BannerCache,
    tracker: DirectionTracker,
    onLaunch: (AppEntry) -> Unit,
    onOpenControls: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(topAreaHeight(state.topShelf))) {
        if (state.topShelf) {
            TopShelf(dockFocus.shelfApp, state.recents, banners, tracker, onLaunch, Modifier.fillMaxSize())
        }
        StatusPill(
            state.clock24, onOpenControls,
            Modifier.align(Alignment.TopEnd).padding(top = HomeLayout.StatusInset).testTag(TestTags.STATUS_PILL),
        )
    }
}
