package com.kevin.glasslauncher.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.ui.TestTags

/**
 * The one scrolling list: [top] (status pill, top shelf), [dock], then every app in [apps].
 * Columns, gaps and side padding come from [HomeLayout], so dock tiles share the grid's columns.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeGrid(
    apps: List<AppEntry>,
    env: TileEnv,
    grid: LazyGridState,
    top: @Composable () -> Unit,
    dock: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeMarginSpec) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(HomeLayout.COLUMNS),
            state = grid,
            modifier = modifier.testTag(TestTags.HOME),
            contentPadding = PaddingValues(
                start = HomeLayout.SidePadding, end = HomeLayout.SidePadding, bottom = HomeLayout.BottomPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(HomeLayout.TileGap),
            verticalArrangement = Arrangement.spacedBy(HomeLayout.RowGap),
        ) {
            item(key = "top", span = { GridItemSpan(maxLineSpan) }, contentType = "top") { top() }
            item(key = "dock", span = { GridItemSpan(maxLineSpan) }, contentType = "dock") { dock() }
            items(apps, key = { it.key }, contentType = { "app" }) { app ->
                AppTile(app, env, TilePlace.Grid)
            }
        }
    }
}
