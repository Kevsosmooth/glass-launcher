package com.kevin.glasslauncher.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.data.City
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.PageTitle
import com.kevin.glasslauncher.settings.ui.PanelRow
import com.kevin.glasslauncher.settings.ui.PanelStyle
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.ToggleRow
import com.kevin.glasslauncher.settings.ui.focusOnShow
import com.kevin.glasslauncher.system.rememberPermissionRequest
import com.kevin.glasslauncher.weather.LocationSource
import com.kevin.glasslauncher.weather.LocationState
import kotlinx.coroutines.delay

private const val MIN_QUERY_LENGTH = 2
// Wait for a pause in typing so each keystroke does not send a search.
private const val SEARCH_DELAY_MS = 500L
private val FieldHeight = 48.dp
private val FieldSurface = Color(0x26FFFFFF)

private sealed interface CitySearch {
    data object Idle : CitySearch
    data class Found(val cities: List<City>) : CitySearch
    data object Failed : CitySearch
}

/** What the page shows: the picked city (null for Automatic), the device location opt-in, what Automatic found. */
data class WeatherLocationChoice(val picked: City?, val useDeviceLocation: Boolean, val place: LocationState)

/**
 * Where the weather is for: Automatic (with what was found, or a VPN warning), the picked city
 * if any, the "Use this TV's location" opt-in, then a city search. Selecting Automatic or a result
 * calls [onPick] (null for Automatic); the caller saves it and goes back. [onSearch] returns null
 * when the search failed. Turning the opt-in on asks for coarse location first when needed and
 * only saves it once Android grants it.
 */
@Composable
fun ColumnScope.WeatherLocationPage(
    choice: WeatherLocationChoice,
    onSearch: suspend (String) -> List<City>?,
    onUseDeviceLocation: (Boolean) -> Unit,
    onPick: (City?) -> Unit,
) {
    val picked = choice.picked
    var query by remember { mutableStateOf("") }
    var search by remember { mutableStateOf<CitySearch>(CitySearch.Idle) }
    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            search = CitySearch.Idle
            return@LaunchedEffect
        }
        delay(SEARCH_DELAY_MS)
        search = onSearch(trimmed)?.let { CitySearch.Found(it) } ?: CitySearch.Failed
    }
    PageTitle(stringResource(R.string.weather_location))
    Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) {
        PlaceRow(
            stringResource(R.string.weather_location_auto), automaticDetail(choice.place), picked == null,
            { onPick(null) }, Modifier.focusOnShow(),
        )
        if (picked != null) {
            PlaceRow(picked.name, stringResource(R.string.weather_location_current), selected = true, { onPick(picked) })
        }
        DeviceLocationToggle(choice.useDeviceLocation, onUseDeviceLocation)
        CitySearchField(query) { query = it }
        when (val s = search) {
            CitySearch.Idle -> Unit
            CitySearch.Failed -> TextBlock(stringResource(R.string.weather_search_failed))
            is CitySearch.Found -> {
                if (s.cities.isEmpty()) TextBlock(stringResource(R.string.weather_search_none))
                s.cities.forEach { city -> ListRow(city.name, null, { onPick(city) }) }
            }
        }
    }
}

/** The opt-in switch and what it means; turning it on asks for coarse location when not granted yet. */
@Composable
private fun DeviceLocationToggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val ask = rememberPermissionRequest(Manifest.permission.ACCESS_COARSE_LOCATION) { granted -> if (granted) onChange(true) }
    ToggleRow(stringResource(R.string.weather_device_location), on, {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        when {
            on -> onChange(false)
            granted -> onChange(true)
            else -> ask()
        }
    })
    TextBlock(stringResource(R.string.weather_device_location_note))
}

@Composable
private fun automaticDetail(place: LocationState): String {
    if (place.looksWrong) return stringResource(R.string.weather_location_wrong)
    val located = place.located ?: return stringResource(R.string.weather_location_unknown)
    val name = located.name
    return when (located.source) {
        LocationSource.Network -> stringResource(R.string.weather_location_from_wifi)
        LocationSource.Internet -> if (name != null) {
            stringResource(R.string.weather_location_from_internet, name)
        } else {
            stringResource(R.string.weather_location_from_internet_unnamed)
        }
        // A city that was just cleared; the automatic lookup has not run yet.
        LocationSource.Picked -> stringResource(R.string.weather_location_unknown)
    }
}

/** A choice with a detail line under its title and a tick when [selected]. */
@Composable
private fun PlaceRow(title: String, detail: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PanelRow(onClick, modifier) { focused ->
        val primary = if (focused) PanelStyle.FocusedText else PanelStyle.Text
        Column(Modifier.weight(1f)) {
            BasicText(title, style = PanelStyle.on(PanelStyle.Row, primary, focused))
            BasicText(detail, style = PanelStyle.on(PanelStyle.Value, if (focused) primary else PanelStyle.SecondaryText, focused))
        }
        if (selected) BasicText("✓", style = PanelStyle.on(PanelStyle.Row, primary, focused))
    }
}

/**
 * A one-line text field styled like a row: filled when idle, white when focused. Select opens the
 * TV keyboard; its Search key closes the keyboard (results already follow the typing).
 */
@Composable
private fun CitySearchField(value: String, onChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val label = stringResource(R.string.weather_search_label)
    val hint = stringResource(R.string.weather_search_hint)
    val color = if (focused) PanelStyle.FocusedText else PanelStyle.Text
    BasicTextField(
        value,
        onChange,
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        singleLine = true,
        textStyle = PanelStyle.on(PanelStyle.Row, color, focused),
        cursorBrush = SolidColor(color),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        decorationBox = { field ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = FieldHeight)
                    .background(if (focused) PanelStyle.FocusedSurface else FieldSurface, PanelStyle.RowShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    val hintColor = if (focused) PanelStyle.FocusedText.copy(alpha = 0.5f) else PanelStyle.SecondaryText
                    BasicText(hint, style = PanelStyle.on(PanelStyle.Row, hintColor, focused))
                }
                field()
            }
        },
    )
}
