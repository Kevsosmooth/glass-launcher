package com.kevin.glasslauncher.system

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.ui.DetailRow
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.SectionHeader
import com.kevin.glasslauncher.settings.ui.SliderRow
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.ToggleRow
import com.kevin.glasslauncher.settings.ui.focusOnShow

private const val POLL_MS = 2_000L

/**
 * Media volume (Left/Right on the slider, sent to the TV when HDMI-CEC owns it), mute, and what each audio output supports. Output
 * choice, surround mode and passthrough are Android's, one row away.
 */
@Composable
fun ColumnScope.SoundPage() {
    val context = LocalContext.current
    val source = remember(context) { SoundSource(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val state by rememberPolled(refresh, POLL_MS, null) { source.state() }

    SystemPage(stringResource(R.string.system_sound)) {
        val sound = state
        if (sound == null) {
            TextBlock(stringResource(R.string.system_loading), Modifier.focusOnShow())
            return@SystemPage
        }
        val value = when {
            sound.onTv -> stringResource(R.string.system_volume_on_tv)
            sound.muted -> stringResource(R.string.system_muted)
            else -> stringResource(R.string.system_volume_value, sound.percent)
        }
        SliderRow(
            stringResource(R.string.system_volume),
            value,
            if (sound.onTv) null else if (sound.max > 0) sound.volume.toFloat() / sound.max else 0f,
            onStep = if (sound.fixed && !sound.onTv) {
                null
            } else {
                { step ->
                    source.step(step)
                    refresh++
                }
            },
            Modifier.focusOnShow(),
        )
        when {
            sound.onTv -> {
                ListRow(stringResource(R.string.system_mute_tv), null, { source.toggleMute() })
                TextBlock(stringResource(R.string.system_volume_tv_hint))
            }
            sound.fixed -> TextBlock(stringResource(R.string.system_volume_fixed))
            else -> ToggleRow(stringResource(R.string.system_mute), sound.muted, {
                source.toggleMute()
                refresh++
            })
        }
        if (sound.outputs.isNotEmpty()) {
            SectionHeader(stringResource(R.string.system_outputs))
            sound.outputs.forEach { output ->
                val detail = (listOf(output.type) + listOfNotNull(output.channels) + output.formats).distinct().joinToString(" · ")
                DetailRow(output.name, detail)
            }
        }
        PageGap()
        NativeLinkRow(stringResource(R.string.system_sound_more), NativePage.Sound)
    }
}
