package com.kevin.glasslauncher.settings.backup

import com.kevin.glasslauncher.data.AppOrder
import com.kevin.glasslauncher.data.City
import com.kevin.glasslauncher.data.Look
import com.kevin.glasslauncher.data.Prefs
import com.kevin.glasslauncher.data.PrefsPatch
import com.kevin.glasslauncher.data.Rotation
import com.kevin.glasslauncher.data.TempUnit
import com.kevin.glasslauncher.data.toPatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Runs on Robolectric for Android's real org.json (the plain JVM android.jar only has stubs). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsBackupTest {
    private val customised = Prefs(
        favorites = listOf("com.b.app/com.b.app.Main", "com.a.tv/com.a.tv.TvActivity"),
        hidden = setOf("com.c.app/com.c.app.Main\$Inner"),
        look = Look.Solid,
        clock24 = true,
        hud = true,
        dockSize = 5,
        wallpaperId = "carina-nebula",
        rotation = Rotation.Hourly,
        topShelf = false,
        appBackdrop = false,
        appOrder = AppOrder.Alphabetical,
        weatherOn = false,
        tempUnit = TempUnit.Celsius,
        weatherCity = City("Paris, Ile-de-France, France", 48.85, 2.35),
        weatherUseDeviceLocation = true,
    )

    private fun decode(json: String) = decodeBackup(json.toByteArray())

    private fun file(settings: String, version: Int = BACKUP_VERSION) =
        """{"format":"$BACKUP_FORMAT","version":$version,"settings":{$settings}}"""

    @Test
    fun roundTripKeepsEverySetting() {
        val patch = customised.toPatch()
        assertEquals(15, patch.size)
        assertEquals(DecodedBackup.Valid(patch, skipped = 0), decodeBackup(encodeBackup(patch).toByteArray()))
    }

    @Test
    fun defaultDockIsLeftOutAndStaysOut() {
        val patch = Prefs().toPatch()
        assertEquals(null, patch.favorites)
        assertEquals(DecodedBackup.Valid(patch, skipped = 0), decodeBackup(encodeBackup(patch).toByteArray()))
    }

    @Test
    fun rejectsOversizeBeforeParsing() {
        val padded = file("\"look\":\"Solid\"") + " ".repeat(MAX_BACKUP_BYTES)
        assertEquals(DecodedBackup.Rejected(BackupError.TooLarge), decode(padded))
    }

    @Test
    fun rejectsMalformedJson() {
        assertEquals(DecodedBackup.Rejected(BackupError.Malformed), decode("{\"format\": "))
        assertEquals(DecodedBackup.Rejected(BackupError.Malformed), decode("[1, 2]"))
        assertEquals(DecodedBackup.Rejected(BackupError.Malformed), decode(""))
    }

    @Test
    fun rejectsDeepNestingWithoutParsingIt() {
        assertEquals(DecodedBackup.Rejected(BackupError.Malformed), decode("[".repeat(20_000)))
    }

    @Test
    fun rejectsOtherFormatsAndBadVersions() {
        val wrong = DecodedBackup.Rejected(BackupError.WrongFormat)
        assertEquals(wrong, decode("""{"format":"something-else","version":1,"settings":{"look":"Solid"}}"""))
        assertEquals(wrong, decode("""{"version":1,"settings":{"look":"Solid"}}"""))
        assertEquals(wrong, decode("""{"format":"$BACKUP_FORMAT","version":"1","settings":{"look":"Solid"}}"""))
        assertEquals(wrong, decode(file("\"look\":\"Solid\"", version = 0)))
    }

    @Test
    fun rejectsANewerVersion() {
        assertEquals(DecodedBackup.Rejected(BackupError.NewerVersion), decode(file("\"look\":\"Solid\"", version = 2)))
    }

    @Test
    fun rejectsAFileWithNoUsableSettings() {
        val none = DecodedBackup.Rejected(BackupError.NoSettings)
        assertEquals(none, decode("""{"format":"$BACKUP_FORMAT","version":1}"""))
        assertEquals(none, decode(file("")))
        assertEquals(none, decode(file("\"dockSize\":9")))
    }

    @Test
    fun skipsAndCountsBadFieldsAndIgnoresUnknownOnes() {
        val result = decode(
            file(
                """
                "look": "Solid",
                "dockSize": 9,
                "clock24": "yes",
                "topShelf": false,
                "rotation": "Weekly",
                "wallpaper": "not-a-wallpaper",
                "hidden": ["com.ok.app/com.ok.app.Main", "1bad/x.Main"],
                "dock": "com.ok.app/com.ok.app.Main",
                "hud": null,
                "futureSetting": 42
                """,
            ),
        )
        assertEquals(DecodedBackup.Valid(PrefsPatch(look = Look.Solid, topShelf = false), skipped = 7), result)
    }

    @Test
    fun skipsWrongNumberTypesAndOverlongLists() {
        val tooMany = (0..MAX_BACKUP_LIST).joinToString(",") { "\"com.app$it.x/com.app$it.x.Main\"" }
        val result = decode(file("\"dockSize\": 5.0, \"hidden\": [$tooMany], \"appBackdrop\": true"))
        assertEquals(DecodedBackup.Valid(PrefsPatch(appBackdrop = true), skipped = 2), result)
    }

    @Test
    fun weatherCityNeedsANameAndCoordinatesOnTheGlobe() {
        val ok = decode(file("\"weatherCity\": {\"name\": \"Oslo, Norway\", \"latitude\": 59.91, \"longitude\": 10}"))
        assertEquals(DecodedBackup.Valid(PrefsPatch(weatherCity = City("Oslo, Norway", 59.91, 10.0)), 0), ok)
        val bad = decode(
            file(
                """
                "weatherCity": {"name": "", "latitude": 1, "longitude": 1},
                "temperatureUnit": "Kelvin",
                "weather": true
                """,
            ),
        )
        assertEquals(DecodedBackup.Valid(PrefsPatch(weatherOn = true), skipped = 2), bad)
        val offGlobe = decode(file("\"weatherCity\": {\"name\": \"X\", \"latitude\": 91, \"longitude\": 1}, \"weather\": false"))
        assertEquals(DecodedBackup.Valid(PrefsPatch(weatherOn = false), skipped = 1), offGlobe)
    }

    @Test
    fun dockKeepsOrderAndDropsDuplicates() {
        val result = decode(file("\"dock\": [\"b.pkg/b.Main\", \"a.pkg/a.Main\", \"b.pkg/b.Main\"]"))
        assertEquals(DecodedBackup.Valid(PrefsPatch(favorites = listOf("b.pkg/b.Main", "a.pkg/a.Main")), 0), result)
    }

    @Test
    fun appKeysNeedAValidPackageAndActivity() {
        assertTrue(isAppKey("com.netflix.ninja/com.netflix.ninja.MainActivity"))
        assertTrue(isAppKey("fake.app0/fake.app0.Main"))
        assertFalse(isAppKey("com.netflix.ninja"))
        assertFalse(isAppKey("netflix/netflix.Main"))
        assertFalse(isAppKey("com..app/com.app.Main"))
        assertFalse(isAppKey("com.app/com.app/Main"))
        assertFalse(isAppKey("com.app/"))
        assertFalse(isAppKey("com.app/com.app.Main; rm -rf"))
        assertFalse(isAppKey("com." + "a".repeat(300) + "/x.Main"))
    }

    @Test
    fun fileNameCarriesTheDate() {
        assertEquals("glass-settings-2026-10-17.json", backupFileName(LocalDate.of(2026, 10, 17)))
    }
}
