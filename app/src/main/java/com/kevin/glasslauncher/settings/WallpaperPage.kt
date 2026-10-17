package com.kevin.glasslauncher.settings

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.glass.WallpaperCatalog
import com.kevin.glasslauncher.glass.WallpaperInfo
import com.kevin.glasslauncher.settings.ui.PageTitle
import com.kevin.glasslauncher.settings.ui.PanelStyle
import com.kevin.glasslauncher.settings.ui.SectionHeader
import com.kevin.glasslauncher.settings.ui.focusOnShow
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SWATCH_ASPECT = 16f / 9f
private const val THUMB_SAMPLE = 8
private val SwatchShape = RoundedCornerShape(10.dp)
private val CardShape = RoundedCornerShape(14.dp)
// Three cards and the edge of a fourth fit the settings panel.
private val CardWidth = 92.dp

// Blob centres as fractions of the swatch, one per palette colour (wraps if a palette is longer).
private val BlobSpots = listOf(Offset(0.2f, 0.3f), Offset(0.75f, 0.25f), Offset(0.35f, 0.8f), Offset(0.85f, 0.75f), Offset(0.55f, 0.5f))

/**
 * Built-in wallpapers by category: painted swatches and photo thumbnails. The current one is
 * checked and focused first; the focused photo's credit shows under the title.
 */
@Composable
fun ColumnScope.WallpaperPage(currentId: String, onPick: (String) -> Unit) {
    val current = WallpaperCatalog.byId(currentId).id
    var focusedId by remember { mutableStateOf(current) }
    val credit = (WallpaperCatalog.byId(focusedId) as? WallpaperInfo.Photo)?.credit
    PageTitle(stringResource(R.string.panels_wallpaper))
    BasicText(
        credit?.let { stringResource(R.string.panels_wallpaper_credit, it) }.orEmpty(),
        Modifier.padding(start = 12.dp, bottom = 8.dp),
        style = PanelStyle.Value.copy(color = PanelStyle.SecondaryText),
        maxLines = 1,
    )
    Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) {
        WallpaperCatalog.all.groupBy { it.category }.forEach { (category, items) ->
            SectionHeader(stringResource(category.label))
            CategoryRow(items, current, onPick) { focusedId = it }
        }
    }
}

/**
 * One category as a row that scrolls left and right, so a category can hold any number of
 * wallpapers; the next card peeks in at the edge. Starts scrolled to the current wallpaper.
 */
@Composable
private fun CategoryRow(items: List<WallpaperInfo>, current: String, onPick: (String) -> Unit, onFocus: (String) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, items.indexOfFirst { it.id == current }))
    LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.id }) { info ->
            val focus = if (info.id == current) Modifier.focusOnShow() else Modifier
            SwatchCard(info, info.id == current, { onPick(info.id) }, { onFocus(info.id) }, focus.width(CardWidth))
        }
    }
}

@Composable
private fun SwatchCard(info: WallpaperInfo, selected: Boolean, onClick: () -> Unit, onFocus: () -> Unit, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    val description = if (selected) stringResource(R.string.panels_wallpaper_current, info.name) else info.name
    Column(
        modifier
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .semantics { contentDescription = description }
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .then(if (focused) Modifier.background(PanelStyle.FocusedSurface, CardShape) else Modifier)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val swatch = Modifier.fillMaxWidth().aspectRatio(SWATCH_ASPECT).clip(SwatchShape)
        val thumb = (info as? WallpaperInfo.Photo)?.let { rememberThumbnail(it) }
        Canvas(swatch) {
            when {
                thumb != null -> drawImage(thumb, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
                info is WallpaperInfo.Painted -> paintSwatch(info)
                else -> drawRect(PanelStyle.CodeSurface)
            }
            if (selected) drawCheck()
        }
        BasicText(
            info.name,
            Modifier.padding(start = 4.dp),
            style = PanelStyle.on(PanelStyle.Value, if (focused) PanelStyle.FocusedText else PanelStyle.Text, focused),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The wallpaper's base colour with a soft light blob per palette colour (content, not chrome). */
private fun DrawScope.paintSwatch(info: WallpaperInfo.Painted) {
    drawRect(Color(info.base))
    val radius = size.maxDimension * 0.45f
    info.colors.forEachIndexed { i, argb ->
        val spot = BlobSpots[i % BlobSpots.size]
        val center = Offset(spot.x * size.width, spot.y * size.height)
        val color = Color(argb)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)), center, radius), radius, center)
    }
}

/** The photo at 1/8 size (240x135), decoded off the main thread; null until it is ready. */
@Composable
private fun rememberThumbnail(photo: WallpaperInfo.Photo): ImageBitmap? {
    val assets = LocalContext.current.assets
    val thumb by produceState<ImageBitmap?>(null, photo.asset) {
        value = withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply { inSampleSize = THUMB_SAMPLE }
            runCatching { assets.open(photo.asset).use { BitmapFactory.decodeStream(it, null, options) } }
                .getOrNull()?.asImageBitmap()
        }
    }
    return thumb
}

/** White disc with a dark tick in the top-right corner. */
private fun DrawScope.drawCheck() {
    val r = size.height * 0.16f
    val c = Offset(size.width - r * 1.5f, r * 1.5f)
    drawCircle(PanelStyle.FocusedSurface, r, c)
    val stroke = r * 0.3f
    drawLine(PanelStyle.FocusedText, c + Offset(-r * 0.5f, 0f), c + Offset(-r * 0.12f, r * 0.4f), stroke, StrokeCap.Round)
    drawLine(PanelStyle.FocusedText, c + Offset(-r * 0.12f, r * 0.4f), c + Offset(r * 0.5f, -r * 0.35f), stroke, StrokeCap.Round)
}
