package com.kevin.glasslauncher.shelf

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.apps.BannerCache
import com.kevin.glasslauncher.apps.LargeArtSize
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(16.dp)
private val CardMaxHeight = LargeArtSize.HEIGHT_DP.dp
private val CardEdge = Color(0x80FFFFFF)
private val CardElevation = 12.dp
private val Placeholder = Color(0x33FFFFFF)
private val TitleStyle = TextStyle(
    color = Color.White,
    fontSize = 26.sp,
    fontWeight = FontWeight.SemiBold,
    shadow = Shadow(Color(0x99000000), Offset(0f, 1f), 6f),
)

/**
 * The featured app, tvOS-style: its banner as a large rounded card with a bright glass edge and
 * its name beside it. The card fills the available height up to the art's native size.
 */
@Composable
fun ShelfCard(app: AppEntry, banners: BannerCache, modifier: Modifier = Modifier) {
    val art by produceState(banners.peekLarge(app), app.key) { value = loadLargeArt(app, banners) }
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            Modifier
                .heightIn(max = CardMaxHeight)
                .fillMaxHeight()
                .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true)
                .shadow(CardElevation, CardShape)
                .clip(CardShape)
                .drawBehind {
                    val image = art?.image
                    if (image == null) {
                        drawRect(Placeholder)
                    } else {
                        drawImage(
                            image,
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                            filterQuality = FilterQuality.Low,
                        )
                    }
                }
                .border(1.dp, CardEdge, CardShape),
        )
        BasicText(
            app.label,
            Modifier.padding(bottom = 4.dp),
            style = TitleStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
