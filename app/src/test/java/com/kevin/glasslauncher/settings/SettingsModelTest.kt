package com.kevin.glasslauncher.settings

import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.data.AppOrder
import com.kevin.glasslauncher.data.City
import com.kevin.glasslauncher.data.Look
import com.kevin.glasslauncher.data.Prefs
import com.kevin.glasslauncher.data.Rotation
import com.kevin.glasslauncher.data.TempUnit
import com.kevin.glasslauncher.system.NativePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsModelTest {
    private val prefs = Prefs()

    private fun ids(sections: List<SettingsSection>) = sections.flatMap { s -> s.rows.map { it.id } }

    private fun row(p: Prefs, id: SettingId) =
        settingsSections(p, confirmingReset = false, versionName = "1.0").flatMap { it.rows }.first { it.id == id }

    @Test
    fun cycleWrapsAndStartsOverOnUnknown() {
        assertEquals(2, cycle(listOf(1, 2, 3), 1))
        assertEquals(1, cycle(listOf(1, 2, 3), 3))
        assertEquals(1, cycle(listOf(1, 2, 3), 9))
    }

    @Test
    fun choiceRowsCycleTheirValues() {
        assertEquals(SettingsAction.Change(PrefChange.SetLook(Look.Solid)), onSelect(SettingId.Look, prefs))
        assertEquals(SettingsAction.Change(PrefChange.SetLook(Look.Glass)), onSelect(SettingId.Look, prefs.copy(look = Look.Solid)))
        assertEquals(SettingsAction.Change(PrefChange.SetRotation(Rotation.Off)), onSelect(SettingId.Rotation, prefs))
        assertEquals(SettingsAction.Change(PrefChange.SetRotation(Rotation.Hourly)), onSelect(SettingId.Rotation, prefs.copy(rotation = Rotation.Off)))
        assertEquals(SettingsAction.Change(PrefChange.SetDockSize(4)), onSelect(SettingId.DockSize, prefs.copy(dockSize = 6)))
        assertEquals(SettingsAction.Change(PrefChange.SetDockSize(5)), onSelect(SettingId.DockSize, prefs.copy(dockSize = 4)))
        assertEquals(SettingsAction.Change(PrefChange.SetClock24(true)), onSelect(SettingId.Clock, prefs))
        assertEquals(SettingsAction.Change(PrefChange.SetAppOrder(AppOrder.Alphabetical)), onSelect(SettingId.AppOrder, prefs))
    }

    @Test
    fun togglesFlip() {
        assertEquals(SettingsAction.Change(PrefChange.SetAppBackdrop(false)), onSelect(SettingId.AppBackdrop, prefs))
        assertEquals(SettingsAction.Change(PrefChange.SetTopShelf(false)), onSelect(SettingId.TopShelf, prefs))
        assertEquals(SettingsAction.Change(PrefChange.SetHud(true)), onSelect(SettingId.Hud, prefs))
    }

    @Test
    fun linksOpenPagesAndResetNeedsConfirm() {
        assertEquals(SettingsAction.Open(SettingsPage.Wallpaper), onSelect(SettingId.Wallpaper, prefs))
        assertEquals(SettingsAction.Open(SettingsPage.HiddenApps), onSelect(SettingId.HiddenApps, prefs))
        assertEquals(SettingsAction.Open(SettingsPage.MakeDefault), onSelect(SettingId.MakeDefault, prefs))
        assertEquals(SettingsAction.Open(SettingsPage.HomeHelper), onSelect(SettingId.HomeHelper, prefs))
        assertEquals(SettingsAction.ToggleResetConfirm, onSelect(SettingId.ResetDock, prefs))
        assertEquals(SettingsAction.Change(PrefChange.ResetDock), onSelect(SettingId.ConfirmResetDock, prefs))
        assertEquals(SettingsAction.None, onSelect(SettingId.Source, prefs))
    }

    @Test
    fun deviceRowsOpenOurPagesAndAndroidSettings() {
        assertEquals(SettingsAction.Open(SettingsPage.Wifi), onSelect(SettingId.Wifi, prefs))
        assertEquals(SettingsAction.Open(SettingsPage.Bluetooth), onSelect(SettingId.Bluetooth, prefs))
        assertEquals(SettingsAction.Open(SettingsPage.Sound), onSelect(SettingId.Sound, prefs))
        assertEquals(SettingsAction.Open(SettingsPage.Display), onSelect(SettingId.Display, prefs))
        assertEquals(SettingsAction.Open(SettingsPage.Storage), onSelect(SettingId.Storage, prefs))
        assertEquals(SettingsAction.Open(SettingsPage.About), onSelect(SettingId.AboutDevice, prefs))
        assertEquals(SettingsAction.OpenNative(NativePage.All), onSelect(SettingId.AndroidSettings, prefs))
    }

    @Test
    fun deviceSectionFollowsHomeWithLiveValues() {
        val device = DeviceSummary(wifi = UiText.Raw("Home"), display = UiText.Raw("4K 60 Hz"))
        val sections = settingsSections(prefs, false, versionName = "1", device = device)
        assertEquals(
            listOf(R.string.panels_section_appearance, R.string.panels_section_home, R.string.system_section),
            sections.take(3).map { it.title },
        )
        val rows = sections[2].rows
        assertEquals(
            listOf(
                SettingId.Wifi, SettingId.Bluetooth, SettingId.Sound, SettingId.Display,
                SettingId.Storage, SettingId.AboutDevice, SettingId.AndroidSettings,
            ),
            rows.map { it.id },
        )
        assertEquals(RowKind.Link(UiText.Raw("Home")), rows.first { it.id == SettingId.Wifi }.kind)
        assertEquals(RowKind.Link(UiText.Raw("4K 60 Hz")), rows.first { it.id == SettingId.Display }.kind)
        assertEquals(RowKind.Link(null), rows.first { it.id == SettingId.Sound }.kind)
    }

    @Test
    fun backupSectionSitsAfterDeviceAndBeforeHelp() {
        val sections = settingsSections(prefs, false, versionName = "1")
        val titles = sections.map { it.title }
        assertEquals(titles.indexOf(R.string.system_section) + 1, titles.indexOf(R.string.backup_section))
        assertEquals(titles.indexOf(R.string.backup_section) + 1, titles.indexOf(R.string.panels_section_help))
        val rows = sections.first { it.title == R.string.backup_section }.rows
        assertEquals(listOf(SettingId.ExportSettings, SettingId.ImportSettings), rows.map { it.id })
        assertEquals(listOf(R.string.backup_export, R.string.backup_import), rows.map { it.title })
        assertTrue(rows.all { it.kind == RowKind.Link(null) })
    }

    @Test
    fun backupRowsOpenTheBackupPage() {
        assertEquals(SettingsAction.Open(SettingsPage.Backup), onSelect(SettingId.ExportSettings, prefs))
        assertEquals(SettingsAction.Open(SettingsPage.Backup), onSelect(SettingId.ImportSettings, prefs))
    }

    @Test
    fun confirmRowOnlyWhileConfirming() {
        val off = ids(settingsSections(prefs, confirmingReset = false, versionName = "1"))
        val on = ids(settingsSections(prefs, confirmingReset = true, versionName = "1"))
        assertFalse(SettingId.ConfirmResetDock in off)
        assertEquals(on.indexOf(SettingId.ResetDock) + 1, on.indexOf(SettingId.ConfirmResetDock))
    }

    @Test
    fun statsOverlayRowIsInReleaseBuildsUnderAbout() {
        val about = settingsSections(prefs, false, versionName = "1").last()
        assertEquals(R.string.panels_section_about, about.title)
        val row = about.rows.last()
        assertEquals(SettingId.Hud, row.id)
        assertEquals(R.string.panels_hud, row.title)
        assertEquals(RowKind.Toggle(false), row.kind)
    }

    @Test
    fun checkForUpdatesRowShowsTheStatusAndChecks() {
        val about = settingsSections(prefs, false, versionName = "1", updateStatus = "Up to date").last()
        val row = about.rows.first { it.id == SettingId.CheckUpdates }
        assertEquals(R.string.update_check, row.title)
        assertEquals(RowKind.Link(UiText.Raw("Up to date")), row.kind)
        assertEquals(SettingsAction.CheckUpdates, onSelect(SettingId.CheckUpdates, prefs))
    }

    @Test
    fun firstRowIsLookThenWallpaper() {
        val all = ids(settingsSections(prefs, false, versionName = "1"))
        assertEquals(listOf(SettingId.Look, SettingId.Wallpaper), all.take(2))
    }

    @Test
    fun rowValuesFollowPrefs() {
        assertEquals(RowKind.Choice(UiText.Res(R.string.panels_look_solid)), row(prefs.copy(look = Look.Solid), SettingId.Look).kind)
        assertEquals(RowKind.Choice(UiText.Res(R.string.panels_clock_24)), row(prefs.copy(clock24 = true), SettingId.Clock).kind)
        assertEquals(RowKind.Choice(UiText.Raw("5")), row(prefs.copy(dockSize = 5), SettingId.DockSize).kind)
        assertEquals(RowKind.Link(UiText.Raw("Ocean")), row(prefs.copy(wallpaperId = "ocean"), SettingId.Wallpaper).kind)
        assertEquals(RowKind.Link(UiText.Res(R.string.panels_hidden_none)), row(prefs, SettingId.HiddenApps).kind)
        assertEquals(
            RowKind.Link(UiText.Res(R.string.panels_hidden_count, listOf(2))),
            row(prefs.copy(hidden = setOf("a/b", "c/d")), SettingId.HiddenApps).kind,
        )
        assertEquals(RowKind.Toggle(false), row(prefs.copy(topShelf = false), SettingId.TopShelf).kind)
        assertEquals(RowKind.Info(UiText.Raw(SOURCE_URL)), row(prefs, SettingId.Source).kind)
    }

    @Test
    fun weatherRowsFollowClockWithDefaults() {
        val appearance = settingsSections(prefs.copy(tempUnit = TempUnit.Fahrenheit), false, versionName = "1").first().rows
        val ids = appearance.map { it.id }
        assertEquals(
            listOf(SettingId.Clock, SettingId.Weather, SettingId.TemperatureUnit, SettingId.WeatherLocation),
            ids.subList(ids.indexOf(SettingId.Clock), ids.size),
        )
        assertEquals(RowKind.Toggle(true), appearance.first { it.id == SettingId.Weather }.kind)
        assertEquals(RowKind.Choice(UiText.Res(R.string.weather_unit_f)), appearance.first { it.id == SettingId.TemperatureUnit }.kind)
        assertEquals(RowKind.Link(UiText.Res(R.string.weather_location_auto)), appearance.first { it.id == SettingId.WeatherLocation }.kind)
    }

    @Test
    fun weatherLocationShowsPickedOrAutomaticCity() {
        val auto = settingsSections(prefs, false, "1", weatherAutoName = "Brooklyn").flatMap { it.rows }
        assertEquals(
            RowKind.Link(UiText.Res(R.string.weather_location_auto_named, listOf("Brooklyn"))),
            auto.first { it.id == SettingId.WeatherLocation }.kind,
        )
        val picked = prefs.copy(weatherCity = City("Oslo, Oslo, Norway", 59.91, 10.75))
        assertEquals(RowKind.Link(UiText.Raw("Oslo, Oslo, Norway")), row(picked, SettingId.WeatherLocation).kind)
        assertEquals(RowKind.Choice(UiText.Res(R.string.weather_unit_c)), row(prefs.copy(tempUnit = TempUnit.Celsius), SettingId.TemperatureUnit).kind)
    }

    @Test
    fun weatherRowsActions() {
        assertEquals(SettingsAction.Change(PrefChange.SetWeatherOn(false)), onSelect(SettingId.Weather, prefs.copy(weatherOn = true)))
        assertEquals(
            SettingsAction.Change(PrefChange.SetTempUnit(TempUnit.Celsius)),
            onSelect(SettingId.TemperatureUnit, prefs.copy(tempUnit = TempUnit.Fahrenheit)),
        )
        assertEquals(
            SettingsAction.Change(PrefChange.SetTempUnit(TempUnit.Fahrenheit)),
            onSelect(SettingId.TemperatureUnit, prefs.copy(tempUnit = TempUnit.Celsius)),
        )
        assertEquals(SettingsAction.Open(SettingsPage.WeatherLocation), onSelect(SettingId.WeatherLocation, prefs))
    }

    @Test
    fun toggleHiddenAddsAndRemoves() {
        assertEquals(setOf("a/b"), toggleHidden(emptySet(), "a/b"))
        assertEquals(setOf("c/d"), toggleHidden(setOf("a/b", "c/d"), "a/b"))
    }

    @Test
    fun hideAllButDockKeepsOnlyTheDock() {
        val apps = listOf("a", "b", "c", "d").map { AppEntry("$it.pkg", "$it.Main", it, false) }
        val p = prefs.copy(favorites = listOf("b.pkg/b.Main", "d.pkg/d.Main"), hidden = setOf("d.pkg/d.Main"))
        // d is hidden, so it is not in the dock either: everything except b gets hidden.
        assertEquals(setOf("a.pkg/a.Main", "c.pkg/c.Main", "d.pkg/d.Main"), hideAllButDock(apps, p))
        assertEquals(setOf("a.pkg/a.Main", "c.pkg/c.Main"), hideAllButDock(apps, p.copy(hidden = emptySet())))
    }
}
