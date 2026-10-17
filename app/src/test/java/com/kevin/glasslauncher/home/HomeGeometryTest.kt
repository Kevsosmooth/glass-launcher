package com.kevin.glasslauncher.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpRect
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.motion.DirectionTracker
import com.kevin.glasslauncher.ui.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The home layout at 960x540 dp, measured from semantics bounds (unscaled tile bounds). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-xhdpi")
class HomeGeometryTest {
    @get:Rule
    val rule = createComposeRule()

    private val margin = HomeLayout.FOCUS_MARGIN_DP
    private val reach = HomeLayout.Footprint

    private fun app(i: Int) = AppEntry("p$i", "p$i.Main", "App $i", isLeanback = true)

    private fun show(dockCount: Int, gridCount: Int) {
        val env = TileEnv(NoArt, DirectionTracker(), onLaunch = {}, onLongPress = { _, _, _ -> })
        rule.setContent {
            HomeGrid(
                apps = List(gridCount, ::app),
                env = env,
                grid = rememberLazyGridState(),
                top = { Spacer(Modifier.fillMaxWidth().height(topAreaHeight(topShelf = false))) },
                dock = { Dock(List(dockCount, ::app), env, DockFocus(), movingKey = null, onFocusEnter = {}) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun bounds(tag: String): List<DpRect> =
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().indices.map {
            rule.onAllNodesWithTag(tag)[it].getUnclippedBoundsInRoot()
        }

    private fun assertNear(expected: Float, actual: Float, what: String) =
        assertEquals(what, expected, actual, 0.01f)

    @Test
    fun allTilesShareOneSizeAndOneGap() {
        show(dockCount = 6, gridCount = 12)
        val tiles = bounds(TestTags.DOCK_TILE) + bounds(TestTags.GRID_TILE)
        assertEquals(18, tiles.size)
        tiles.forEach { assertNear(HomeLayout.Metrics.tileWidth, (it.right - it.left).value, "tile width") }
        tiles.chunked(HomeLayout.COLUMNS).forEach { row ->
            row.zipWithNext { a, b -> assertNear(HomeLayout.TileGap.value, (b.left - a.right).value, "column gap") }
        }
        val rows = bounds(TestTags.GRID_TILE).map { it.top }.distinct().sortedBy { it.value }
        val height = bounds(TestTags.GRID_TILE).first().let { it.bottom - it.top }
        rows.zipWithNext { a, b -> assertNear(HomeLayout.RowGap.value, (b - a - height).value, "row gap") }
    }

    @Test
    fun dockTilesSitOnTheGridColumnsEvenWithEmptySlots() {
        show(dockCount = 4, gridCount = 6)
        val dock = bounds(TestTags.DOCK_TILE)
        val grid = bounds(TestTags.GRID_TILE)
        assertEquals(4, dock.size)
        dock.forEachIndexed { i, tile ->
            assertNear(grid[i].left.value, tile.left.value, "dock column $i x")
            assertNear((grid[i].right - grid[i].left).value, (tile.right - tile.left).value, "dock column $i width")
        }
    }

    @Test
    fun dockSlabHoldsEveryFocusedTileWithMargin() {
        show(dockCount = 6, gridCount = 6)
        val slab = rule.onNodeWithTag(TestTags.DOCK).getUnclippedBoundsInRoot()
        bounds(TestTags.DOCK_TILE).forEach { tile ->
            assertTrue("left", tile.left.value - reach.side - slab.left.value >= margin)
            assertTrue("right", slab.right.value - (tile.right.value + reach.side) >= margin)
            assertTrue("top", tile.top.value - reach.top - slab.top.value >= margin)
            assertTrue("bottom", slab.bottom.value - (tile.bottom.value + reach.bottom) >= margin)
        }
        val firstRow = bounds(TestTags.GRID_TILE).first()
        assertTrue("grid row clears slab", firstRow.top.value - reach.top - slab.bottom.value >= margin)
    }

    @Test
    fun nothingLeavesTheScreen() {
        show(dockCount = 6, gridCount = 12)
        val slab = rule.onNodeWithTag(TestTags.DOCK).getUnclippedBoundsInRoot()
        assertNear(HomeLayout.Metrics.dockInset, slab.left.value, "slab left inset")
        assertNear(960f - HomeLayout.Metrics.dockInset, slab.right.value, "slab right inset")
        (bounds(TestTags.DOCK_TILE) + bounds(TestTags.GRID_TILE)).forEach { tile ->
            assertTrue(tile.left.value - reach.side >= margin)
            assertTrue(tile.right.value + reach.side <= 960f - margin)
        }
    }

    private object NoArt : TileArtSource {
        override fun peek(app: AppEntry) = null

        override suspend fun load(app: AppEntry) = null
    }
}
