package com.kevin.glasslauncher.settings

import androidx.annotation.StringRes
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.data.Look
import com.kevin.glasslauncher.data.MAX_DOCK_SIZE
import com.kevin.glasslauncher.data.MIN_DOCK_SIZE
import com.kevin.glasslauncher.data.Prefs
import com.kevin.glasslauncher.data.Rotation
import com.kevin.glasslauncher.glass.WallpaperCatalog
import com.kevin.glasslauncher.home.buildHomeState
import com.kevin.glasslauncher.system.NativePage

const val LICENSE_NAME = "MIT"
const val SOURCE_URL = "https://github.com/Kevsosmooth/glass-launcher"

/** Pages of the Settings panel; Back from any of them returns to [Main]. */
enum class SettingsPage { Main, Wallpaper, HiddenApps, MakeDefault, Wifi, Bluetooth, Sound, Display, Storage, About }

enum class SettingId {
    Look, Wallpaper, Rotation, AppBackdrop, TopShelf, Clock,
    DockSize, HiddenApps, ResetDock, ConfirmResetDock,
    Wifi, Bluetooth, Sound, Display, Storage, AboutDevice, AndroidSettings,
    MakeDefault, Hud, Version, License, Source,
}

/** Live values shown beside the Device rows; null shows no value. */
data class DeviceSummary(
    val wifi: UiText? = null,
    val bluetooth: UiText? = null,
    val sound: UiText? = null,
    val display: UiText? = null,
    val storage: UiText? = null,
)

/** Text that is either a string resource (with format args) or a literal. */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val text: String) : UiText
}

/** How a row looks and what Select does with it. */
sealed interface RowKind {
    data class Toggle(val on: Boolean) : RowKind
    data class Choice(val value: UiText) : RowKind
    data class Link(val value: UiText?) : RowKind
    data object Action : RowKind
    data class Info(val value: UiText) : RowKind
}

data class SettingsRow(val id: SettingId, @StringRes val title: Int, val kind: RowKind)

data class SettingsSection(@StringRes val title: Int, val rows: List<SettingsRow>)

/** A preference write; [applyTo] in SettingsActions performs it. */
sealed interface PrefChange {
    data class SetLook(val look: Look) : PrefChange
    data class SetRotation(val rotation: Rotation) : PrefChange
    data class SetAppBackdrop(val on: Boolean) : PrefChange
    data class SetTopShelf(val on: Boolean) : PrefChange
    data class SetClock24(val on: Boolean) : PrefChange
    data class SetDockSize(val size: Int) : PrefChange
    data class SetHud(val on: Boolean) : PrefChange
    data class SetWallpaper(val id: String) : PrefChange
    data class SetHidden(val keys: Set<String>) : PrefChange
    data object ResetDock : PrefChange
}

/** What Select on a row means. */
sealed interface SettingsAction {
    data class Change(val change: PrefChange) : SettingsAction
    data class Open(val page: SettingsPage) : SettingsAction
    data class OpenNative(val page: NativePage) : SettingsAction
    data object ToggleResetConfirm : SettingsAction
    data object None : SettingsAction
}

/** The value after [current] in [values], wrapping around; the first value if [current] is unknown. */
fun <T> cycle(values: List<T>, current: T): T = values[(values.indexOf(current) + 1).mod(values.size)]

val DockSizes: List<Int> = (MIN_DOCK_SIZE..MAX_DOCK_SIZE).toList()

/** [hidden] with [key] added, or removed if it was already there. */
fun toggleHidden(hidden: Set<String>, key: String): Set<String> = if (key in hidden) hidden - key else hidden + key

/** Hidden set for a minimal home: every app except the ones in the dock right now. */
fun hideAllButDock(apps: List<AppEntry>, prefs: Prefs): Set<String> {
    val dock = buildHomeState(apps, prefs).dock.mapTo(HashSet()) { it.key }
    return apps.map { it.key }.filterNotTo(HashSet()) { it in dock }
}

/** Rows of the main page, grouped. [debug] adds the Developer section. */
fun settingsSections(
    prefs: Prefs,
    confirmingReset: Boolean,
    debug: Boolean,
    versionName: String,
    device: DeviceSummary = DeviceSummary(),
): List<SettingsSection> =
    listOfNotNull(
        SettingsSection(R.string.panels_section_appearance, appearanceRows(prefs)),
        SettingsSection(R.string.panels_section_home, homeRows(prefs, confirmingReset)),
        SettingsSection(R.string.system_section, deviceRows(device)),
        SettingsSection(
            R.string.panels_section_help,
            listOf(SettingsRow(SettingId.MakeDefault, R.string.panels_make_default, RowKind.Link(null))),
        ),
        if (debug) {
            SettingsSection(
                R.string.panels_section_developer,
                listOf(SettingsRow(SettingId.Hud, R.string.panels_hud, RowKind.Toggle(prefs.hud))),
            )
        } else {
            null
        },
        SettingsSection(
            R.string.panels_section_about,
            listOf(
                SettingsRow(SettingId.Version, R.string.panels_version, RowKind.Info(UiText.Raw(versionName))),
                SettingsRow(SettingId.License, R.string.panels_license, RowKind.Info(UiText.Raw(LICENSE_NAME))),
                SettingsRow(SettingId.Source, R.string.panels_source, RowKind.Info(UiText.Raw(SOURCE_URL))),
            ),
        ),
    )

private fun appearanceRows(prefs: Prefs) = listOf(
    SettingsRow(SettingId.Look, R.string.panels_look, RowKind.Choice(lookText(prefs.look))),
    SettingsRow(
        SettingId.Wallpaper, R.string.panels_wallpaper,
        RowKind.Link(UiText.Raw(WallpaperCatalog.byId(prefs.wallpaperId).name)),
    ),
    SettingsRow(SettingId.Rotation, R.string.panels_rotation, RowKind.Choice(rotationText(prefs.rotation))),
    SettingsRow(SettingId.AppBackdrop, R.string.panels_app_backdrop, RowKind.Toggle(prefs.appBackdrop)),
    SettingsRow(SettingId.TopShelf, R.string.panels_top_shelf, RowKind.Toggle(prefs.topShelf)),
    SettingsRow(
        SettingId.Clock, R.string.panels_clock,
        RowKind.Choice(UiText.Res(if (prefs.clock24) R.string.panels_clock_24 else R.string.panels_clock_12)),
    ),
)

private fun homeRows(prefs: Prefs, confirmingReset: Boolean) = listOfNotNull(
    SettingsRow(SettingId.DockSize, R.string.panels_dock_size, RowKind.Choice(UiText.Raw(prefs.dockSize.toString()))),
    SettingsRow(
        SettingId.HiddenApps, R.string.panels_hidden_apps,
        RowKind.Link(
            if (prefs.hidden.isEmpty()) {
                UiText.Res(R.string.panels_hidden_none)
            } else {
                UiText.Res(R.string.panels_hidden_count, listOf(prefs.hidden.size))
            },
        ),
    ),
    SettingsRow(SettingId.ResetDock, R.string.panels_reset_dock, RowKind.Action),
    if (confirmingReset) SettingsRow(SettingId.ConfirmResetDock, R.string.panels_reset_dock_confirm, RowKind.Action) else null,
)

private fun deviceRows(device: DeviceSummary) = listOf(
    SettingsRow(SettingId.Wifi, R.string.system_wifi, RowKind.Link(device.wifi)),
    SettingsRow(SettingId.Bluetooth, R.string.system_bluetooth, RowKind.Link(device.bluetooth)),
    SettingsRow(SettingId.Sound, R.string.system_sound, RowKind.Link(device.sound)),
    SettingsRow(SettingId.Display, R.string.system_display, RowKind.Link(device.display)),
    SettingsRow(SettingId.Storage, R.string.system_storage, RowKind.Link(device.storage)),
    SettingsRow(SettingId.AboutDevice, R.string.system_about, RowKind.Link(null)),
    SettingsRow(SettingId.AndroidSettings, R.string.system_android_settings, RowKind.Link(null)),
)

private fun lookText(look: Look) = UiText.Res(
    when (look) {
        Look.Glass -> R.string.panels_look_glass
        Look.Solid -> R.string.panels_look_solid
    },
)

private fun rotationText(rotation: Rotation) = UiText.Res(
    when (rotation) {
        Rotation.Off -> R.string.panels_rotation_off
        Rotation.Hourly -> R.string.panels_rotation_hourly
        Rotation.Daily -> R.string.panels_rotation_daily
    },
)

/** What Select on row [id] does, given the current [prefs]. */
fun onSelect(id: SettingId, prefs: Prefs): SettingsAction = when (id) {
    SettingId.Look -> change(PrefChange.SetLook(cycle(Look.entries, prefs.look)))
    SettingId.Wallpaper -> SettingsAction.Open(SettingsPage.Wallpaper)
    SettingId.Rotation -> change(PrefChange.SetRotation(cycle(Rotation.entries, prefs.rotation)))
    SettingId.AppBackdrop -> change(PrefChange.SetAppBackdrop(!prefs.appBackdrop))
    SettingId.TopShelf -> change(PrefChange.SetTopShelf(!prefs.topShelf))
    SettingId.Clock -> change(PrefChange.SetClock24(!prefs.clock24))
    SettingId.DockSize -> change(PrefChange.SetDockSize(cycle(DockSizes, prefs.dockSize)))
    SettingId.HiddenApps -> SettingsAction.Open(SettingsPage.HiddenApps)
    SettingId.ResetDock -> SettingsAction.ToggleResetConfirm
    SettingId.ConfirmResetDock -> change(PrefChange.ResetDock)
    SettingId.Wifi -> SettingsAction.Open(SettingsPage.Wifi)
    SettingId.Bluetooth -> SettingsAction.Open(SettingsPage.Bluetooth)
    SettingId.Sound -> SettingsAction.Open(SettingsPage.Sound)
    SettingId.Display -> SettingsAction.Open(SettingsPage.Display)
    SettingId.Storage -> SettingsAction.Open(SettingsPage.Storage)
    SettingId.AboutDevice -> SettingsAction.Open(SettingsPage.About)
    SettingId.AndroidSettings -> SettingsAction.OpenNative(NativePage.All)
    SettingId.MakeDefault -> SettingsAction.Open(SettingsPage.MakeDefault)
    SettingId.Hud -> change(PrefChange.SetHud(!prefs.hud))
    SettingId.Version, SettingId.License, SettingId.Source -> SettingsAction.None
}

private fun change(c: PrefChange) = SettingsAction.Change(c)
