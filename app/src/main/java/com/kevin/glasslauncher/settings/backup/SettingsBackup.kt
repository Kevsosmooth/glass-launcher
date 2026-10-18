package com.kevin.glasslauncher.settings.backup

import com.kevin.glasslauncher.data.AppOrder
import com.kevin.glasslauncher.data.Look
import com.kevin.glasslauncher.data.MAX_DOCK_SIZE
import com.kevin.glasslauncher.data.MIN_DOCK_SIZE
import com.kevin.glasslauncher.data.PrefsPatch
import com.kevin.glasslauncher.data.Rotation
import com.kevin.glasslauncher.glass.WallpaperCatalog
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

const val BACKUP_FORMAT = "glass-launcher-settings"
const val BACKUP_VERSION = 1
const val MAX_BACKUP_BYTES = 64 * 1024
const val MAX_BACKUP_LIST = 500

// A real backup has four brackets (file, settings, dock, hidden). org.json parses recursively, so
// a file of nothing but "[[[[" could overflow the stack; capping every bracket bounds the depth.
private const val MAX_BRACKETS = 64
private const val MAX_NAME_LENGTH = 255

private val PackageName = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
private val ClassName = Regex("^[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*$")

private object Key {
    const val FORMAT = "format"
    const val VERSION = "version"
    const val SETTINGS = "settings"
    const val LOOK = "look"
    const val WALLPAPER = "wallpaper"
    const val ROTATION = "rotation"
    const val APP_BACKDROP = "appBackdrop"
    const val TOP_SHELF = "topShelf"
    const val CLOCK_24 = "clock24"
    const val DOCK_SIZE = "dockSize"
    const val DOCK = "dock"
    const val HIDDEN = "hidden"
    const val HUD = "hud"
    const val APP_ORDER = "appOrder"
}

/** Why a file was not imported at all. */
enum class BackupError { TooLarge, Malformed, WrongFormat, NewerVersion, NoSettings }

sealed interface DecodedBackup {
    /** [patch] holds every valid setting; [skipped] counts known settings with a bad value. */
    data class Valid(val patch: PrefsPatch, val skipped: Int) : DecodedBackup

    data class Rejected(val error: BackupError) : DecodedBackup
}

/** Suggested name for a backup made on [date], e.g. glass-settings-2026-10-17.json. */
fun backupFileName(date: LocalDate): String = "glass-settings-$date.json"

/** [patch] as a backup file (pretty-printed JSON, UTF-8 when written). */
fun encodeBackup(patch: PrefsPatch): String {
    val settings = JSONObject()
    patch.look?.let { settings.put(Key.LOOK, it.name) }
    patch.wallpaperId?.let { settings.put(Key.WALLPAPER, it) }
    patch.rotation?.let { settings.put(Key.ROTATION, it.name) }
    patch.appBackdrop?.let { settings.put(Key.APP_BACKDROP, it) }
    patch.topShelf?.let { settings.put(Key.TOP_SHELF, it) }
    patch.clock24?.let { settings.put(Key.CLOCK_24, it) }
    patch.dockSize?.let { settings.put(Key.DOCK_SIZE, it) }
    patch.favorites?.let { settings.put(Key.DOCK, JSONArray(it)) }
    patch.hidden?.let { settings.put(Key.HIDDEN, JSONArray(it.sorted())) }
    patch.hud?.let { settings.put(Key.HUD, it) }
    patch.appOrder?.let { settings.put(Key.APP_ORDER, it.name) }
    return JSONObject()
        .put(Key.FORMAT, BACKUP_FORMAT)
        .put(Key.VERSION, BACKUP_VERSION)
        .put(Key.SETTINGS, settings)
        .toString(2)
}

/**
 * Reads a backup file. The file is untrusted: the envelope must match exactly, then each setting
 * is type- and range-checked on its own; a bad one is skipped (and counted), unknown keys are ignored.
 */
fun decodeBackup(bytes: ByteArray): DecodedBackup {
    if (bytes.size > MAX_BACKUP_BYTES) return DecodedBackup.Rejected(BackupError.TooLarge)
    val text = bytes.toString(Charsets.UTF_8)
    if (text.count { it == '{' || it == '[' } > MAX_BRACKETS) return DecodedBackup.Rejected(BackupError.Malformed)
    val root = try {
        JSONObject(text)
    } catch (e: JSONException) {
        return DecodedBackup.Rejected(BackupError.Malformed)
    }
    if (root.opt(Key.FORMAT) != BACKUP_FORMAT) return DecodedBackup.Rejected(BackupError.WrongFormat)
    val version = root.opt(Key.VERSION) as? Int
    if (version == null || version < 1) return DecodedBackup.Rejected(BackupError.WrongFormat)
    if (version > BACKUP_VERSION) return DecodedBackup.Rejected(BackupError.NewerVersion)
    val settings = root.opt(Key.SETTINGS) as? JSONObject ?: return DecodedBackup.Rejected(BackupError.NoSettings)
    val reader = FieldReader(settings)
    val patch = PrefsPatch(
        look = reader.read(Key.LOOK) { enumByName(it, Look.entries) },
        wallpaperId = reader.read(Key.WALLPAPER) { v -> (v as? String)?.takeIf { id -> WallpaperCatalog.all.any { it.id == id } } },
        rotation = reader.read(Key.ROTATION) { enumByName(it, Rotation.entries) },
        appBackdrop = reader.read(Key.APP_BACKDROP) { it as? Boolean },
        topShelf = reader.read(Key.TOP_SHELF) { it as? Boolean },
        clock24 = reader.read(Key.CLOCK_24) { it as? Boolean },
        dockSize = reader.read(Key.DOCK_SIZE) { v -> (v as? Int)?.takeIf { it in MIN_DOCK_SIZE..MAX_DOCK_SIZE } },
        favorites = reader.read(Key.DOCK) { appKeys(it)?.distinct() },
        hidden = reader.read(Key.HIDDEN) { appKeys(it)?.toSet() },
        hud = reader.read(Key.HUD) { it as? Boolean },
        appOrder = reader.read(Key.APP_ORDER) { enumByName(it, AppOrder.entries) },
    )
    if (patch.size == 0) return DecodedBackup.Rejected(BackupError.NoSettings)
    return DecodedBackup.Valid(patch, reader.skipped)
}

/** True for an app key "package/activity" with a valid package name and activity class name. */
fun isAppKey(key: String): Boolean {
    val slash = key.indexOf('/')
    if (slash < 0) return false
    val pkg = key.substring(0, slash)
    val activity = key.substring(slash + 1)
    return pkg.length <= MAX_NAME_LENGTH && activity.length <= MAX_NAME_LENGTH &&
        PackageName.matches(pkg) && ClassName.matches(activity)
}

/** Reads optional settings; a present key whose value [read] rejects counts as skipped. */
private class FieldReader(private val json: JSONObject) {
    var skipped = 0
        private set

    fun <T : Any> read(key: String, parse: (Any) -> T?): T? {
        if (!json.has(key)) return null
        return parse(json.get(key)).also { if (it == null) skipped++ }
    }
}

private fun <E : Enum<E>> enumByName(value: Any, entries: List<E>): E? = entries.firstOrNull { it.name == value }

/** Every entry of a JSON array of app keys, or null if it is too long or any entry is not one. */
private fun appKeys(value: Any): List<String>? {
    val array = value as? JSONArray ?: return null
    if (array.length() > MAX_BACKUP_LIST) return null
    val keys = (0 until array.length()).map { array.get(it) as? String ?: return null }
    return keys.takeIf { it.all(::isAppKey) }
}
