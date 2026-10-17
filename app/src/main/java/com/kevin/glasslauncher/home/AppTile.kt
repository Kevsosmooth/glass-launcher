package com.kevin.glasslauncher.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.apps.BannerCache
import com.kevin.glasslauncher.edit.TileHandle
import com.kevin.glasslauncher.motion.DirectionTracker
import com.kevin.glasslauncher.motion.focusMotion
import kotlin.math.roundToInt

private val Placeholder = Color(0x33FFFFFF)
private val LabelStyle = TextStyle(
    color = Color.White,
    fontSize = 11.sp,
    shadow = Shadow(Color(0x99000000), Offset(0f, 1f), 4f),
)

/** Where a tile sits on screen; a plain holder (not state) read only when its menu opens. */
private class TileAnchor {
    var coordinates: LayoutCoordinates? = null

    fun bounds(): Rect = coordinates?.takeIf { it.isAttached }?.boundsInRoot() ?: Rect.Zero
}

/**
 * Home tile (dock or grid). Select launches; a long press (held Select) reports the tile to
 * [TileEnv.onLongPress]. The [place] tag marks the unscaled bounds (outside the focus layer).
 */
@Composable
fun AppTile(app: AppEntry, env: TileEnv, place: TilePlace, modifier: Modifier = Modifier) {
    val anchor = remember { TileAnchor() }
    val focus = remember { FocusRequester() }
    DisposableEffect(env.focusMemory, focus) { onDispose { env.focusMemory.gone(focus) } }
    TileFrame(
        app, env.art, env.tracker,
        onClick = { env.onLaunch(app) },
        onLongClick = { env.onLongPress(app, place, TileHandle(anchor.bounds(), focus)) },
        modifier = modifier
            .aspectRatio(16f / 9f)
            .testTag(place.tag)
            .onPlaced { anchor.coordinates = it }
            .onFocusChanged { if (it.isFocused) env.focusMemory.focused(focus) }
            .focusRequester(focus),
    )
}

/** Plain tile for other rows (top shelf): banner art, Select calls [onClick], no edit menu. */
@Composable
fun AppTile(app: AppEntry, banners: BannerCache, tracker: DirectionTracker, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val art = remember(banners) { BannerArtSource(banners) }
    TileFrame(app, art, tracker, onClick, onLongClick = null, modifier = modifier.aspectRatio(16f / 9f))
}

/** 16:9 art (banner, or icon on a tint with the label) with the focus lift. */
@Composable
private fun TileFrame(
    app: AppEntry,
    art: TileArtSource,
    tracker: DirectionTracker,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier,
) {
    val picture by produceState(art.peek(app), app.key) {
        if (value == null) value = art.load(app)
    }
    Box(
        modifier
            // One merged node outside the focus layer, so tests and layout checks see unscaled bounds.
            .semantics(mergeDescendants = true) {}
            .focusMotion(tracker, HomeLayout.TileShape)
            .then(if (onLongClick != null) Modifier.selectPress(onClick, onLongClick) else Modifier)
            .combinedClickable(interactionSource = null, indication = null, onLongClick = onLongClick, onClick = onClick)
            .drawBehind {
                val image = picture?.image
                if (image == null) {
                    drawRect(Placeholder)
                } else {
                    drawImage(
                        image,
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        filterQuality = FilterQuality.Low,
                    )
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (picture?.showLabel == true) {
            BasicText(
                app.label,
                Modifier.padding(start = 8.dp, end = 8.dp, bottom = 6.dp),
                style = LabelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
