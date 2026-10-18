package com.kevin.glasslauncher.update

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest

/** Largest APK Glass will download (today's is a few MB). */
const val MAX_APK_BYTES = 64L * 1024 * 1024

private const val APK_ACCEPT = "application/octet-stream"
private const val BUFFER_BYTES = 64 * 1024
private const val PROGRESS_STEPS = 100

/**
 * Downloads [release]'s APK into [target] and checks it against the release: only from
 * [RELEASE_DOWNLOAD_PREFIX], never more than [maxBytes], exactly [ReleaseInfo.size] bytes when
 * GitHub gave one, and the same SHA-256 when GitHub gave one. On any failure [target] is deleted
 * and an [UpdateException] is thrown ([FailReason.Storage] when the file cannot be written).
 * [onProgress] gets 0..1 in whole percent steps.
 */
fun downloadApk(
    http: UpdateHttp,
    release: ReleaseInfo,
    target: File,
    maxBytes: Long = MAX_APK_BYTES,
    onProgress: (Float) -> Unit = {},
) {
    if (!isReleaseApkUrl(release.apkUrl)) throw UpdateException(FailReason.BadRelease)
    val expected = release.size
    if (expected != null && expected > maxBytes) throw UpdateException(FailReason.TooLarge)
    try {
        target.parentFile?.mkdirs()
        val sha256 = target.outputStream().use { out ->
            http.get(release.apkUrl, APK_ACCEPT) { body -> save(body, out, expected, maxBytes, onProgress) }
        }
        if (release.sha256 != null && release.sha256 != sha256) throw UpdateException(FailReason.Corrupt)
    } catch (e: UpdateException) {
        target.delete()
        throw e
    } catch (e: IOException) {
        target.delete()
        throw UpdateException(FailReason.Storage)
    }
}

/**
 * Streams [body] into [out], enforcing the size rules; returns the lowercase hex SHA-256. A failed
 * write is [FailReason.Storage] (UpdateHttp would report a bare IOException as a network failure).
 */
internal fun save(body: Body, out: OutputStream, expected: Long?, maxBytes: Long, onProgress: (Float) -> Unit): String {
    val announced = body.contentLength
    if (announced > maxBytes) throw UpdateException(FailReason.TooLarge)
    if (announced >= 0 && expected != null && announced != expected) throw UpdateException(FailReason.Corrupt)
    val total = expected ?: announced.takeIf { it > 0 }
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(BUFFER_BYTES)
    var written = 0L
    var lastStep = -1
    while (true) {
        val n = body.stream.read(buffer)
        if (n < 0) break
        written += n
        if (written > maxBytes) throw UpdateException(FailReason.TooLarge)
        digest.update(buffer, 0, n)
        try {
            out.write(buffer, 0, n)
        } catch (e: IOException) {
            throw UpdateException(FailReason.Storage)
        }
        val step = if (total == null) -1 else (written * PROGRESS_STEPS / total).toInt().coerceAtMost(PROGRESS_STEPS)
        if (step > lastStep) {
            lastStep = step
            onProgress(step / PROGRESS_STEPS.toFloat())
        }
    }
    if (expected != null && written != expected) throw UpdateException(FailReason.Corrupt)
    return digest.digest().toHex()
}
