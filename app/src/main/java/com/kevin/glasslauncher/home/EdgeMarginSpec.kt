package com.kevin.glasslauncher.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.lazy.grid.LazyGridState

internal fun LazyGridState.isAtTop() = firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

/** Scrolls just enough to keep the focused tile, at full focus scale and shadow, on screen. */
@OptIn(ExperimentalFoundationApi::class)
internal object EdgeMarginSpec : BringIntoViewSpec {
    private const val MARGIN_OF_SIZE = 0.5f

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val margin = size * MARGIN_OF_SIZE
        val overTop = offset - margin
        val overBottom = offset + size + margin - containerSize
        return when {
            overTop < 0f -> overTop
            overBottom > 0f -> overBottom
            else -> 0f
        }
    }
}
