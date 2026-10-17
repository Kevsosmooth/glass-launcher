package com.kevin.glasslauncher.controls

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.glasslauncher.home.rememberClockText
import com.kevin.glasslauncher.settings.ui.PanelStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Clock and today's date on one line at the top of Quick Controls; ticks once a minute. */
@Composable
fun ControlsHeader(clock24: Boolean, modifier: Modifier = Modifier) {
    val time by rememberClockText(clock24)
    // Re-read the date whenever the clock text changes, so it rolls over at midnight.
    val date = remember(time) { todayText() }
    Row(
        modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicText(time, style = PanelStyle.Title.copy(color = PanelStyle.Text, fontSize = 24.sp))
        BasicText(date, Modifier.padding(bottom = 3.dp), style = PanelStyle.Value.copy(color = PanelStyle.SecondaryText))
    }
}

private fun todayText(): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd")
    return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern, locale))
}
