package com.kevin.glasslauncher.settings.backup

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

const val BACKUP_MIME = "application/json"
private const val BACKUP_FOLDER = "GlassLauncher"
private const val MAX_LISTED_FILES = 100

/** Where fallback backups go, as shown to the user and stored in MediaStore. */
val BackupRelativePath = "${Environment.DIRECTORY_DOWNLOADS}/$BACKUP_FOLDER/"

/** A backup file found in Download/GlassLauncher. */
data class BackupFile(val name: String, val uri: Uri)

/**
 * Backup file I/O: any picked document, plus the Download/GlassLauncher fallback for TVs without a
 * document picker (MediaStore on Android 10+, the public Download folder on Android 9). Calls block;
 * run them off the main thread. Failures come back as null/false for the page to explain.
 */
class BackupStorage(context: Context) {
    private val app = context.applicationContext
    private val resolver = app.contentResolver

    /** Android 9 needs storage access to write to Download; later versions use MediaStore. */
    val writePermission: String? =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) Manifest.permission.WRITE_EXTERNAL_STORAGE else null

    /**
     * Lets the list show backups this install did not write (an older install, a USB copy).
     * Android 13+ dropped it: there only this install's own backups are listed.
     */
    val readPermission: String? =
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) Manifest.permission.READ_EXTERNAL_STORAGE else null

    val hasSharedStorage: Boolean get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    fun isGranted(permission: String?): Boolean =
        permission == null || app.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    /** Replaces the contents of [uri] with [text]; false if it could not be written. */
    fun write(uri: Uri, text: String): Boolean = guarded {
        val out = resolver.openOutputStream(uri, "wt") ?: return@guarded false
        out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        true
    } ?: false

    /** At most [MAX_BACKUP_BYTES] + 1 bytes of [uri] (so an oversized file is still detected); null on error. */
    fun read(uri: Uri): ByteArray? = guarded {
        resolver.openInputStream(uri)?.use { readAtMost(it, MAX_BACKUP_BYTES + 1) }
    }

    /** The file name of a picked document, if its provider tells us. */
    fun displayName(uri: Uri): String? = guarded {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    /** Saves [text] as Download/GlassLauncher/[name]; returns the path it got, or null. */
    fun saveToDownloads(name: String, text: String): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveToMediaStore(name, text) else saveToFolder(name, text)

    /** Backups in Download/GlassLauncher, newest first. */
    fun listDownloads(): List<BackupFile> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listMediaStore() else listFolder()

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(name: String, text: String): String? = guarded {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, BACKUP_MIME)
            put(MediaStore.Downloads.RELATIVE_PATH, BackupRelativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@guarded null
        if (!write(uri, text)) {
            resolver.delete(uri, null, null)
            return@guarded null
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        // MediaStore renames on a clash ("... (1).json"), so report the name it really used.
        BackupRelativePath + (displayName(uri) ?: name)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun listMediaStore(): List<BackupFile> = guarded {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} = ? AND ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
        val order = "${MediaStore.Downloads.DATE_MODIFIED} DESC"
        resolver.query(collection, projection, selection, arrayOf(BackupRelativePath, "%.json"), order)?.use { c ->
            buildList {
                while (c.moveToNext() && size < MAX_LISTED_FILES) {
                    add(BackupFile(c.getString(1), ContentUris.withAppendedId(collection, c.getLong(0))))
                }
            }
        }
    }.orEmpty()

    private fun saveToFolder(name: String, text: String): String? = guarded {
        val dir = legacyFolder()
        if (!dir.isDirectory && !dir.mkdirs()) return@guarded null
        File(dir, name).writeText(text, Charsets.UTF_8)
        BackupRelativePath + name
    }

    private fun listFolder(): List<BackupFile> = guarded {
        legacyFolder().listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.take(MAX_LISTED_FILES)
            ?.map { BackupFile(it.name, Uri.fromFile(it)) }
    }.orEmpty()

    // Android 9 only (no MediaStore.Downloads); the call is deprecated from Android 10 on.
    private fun legacyFolder() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), BACKUP_FOLDER)
}

/** Runs [block]; the storage failures a provider or the file system can throw become null. */
private inline fun <T> guarded(block: () -> T?): T? = try {
    block()
} catch (e: IOException) {
    null
} catch (e: SecurityException) {
    null
} catch (e: IllegalArgumentException) {
    null
} catch (e: IllegalStateException) {
    null
}

private fun readAtMost(input: InputStream, limit: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    while (out.size() < limit) {
        val read = input.read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (read < 0) break
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}
