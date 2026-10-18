package com.kevin.glasslauncher.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.io.File
import java.io.IOException

/** Broadcast action PackageInstaller reports the session result with ([UpdateReceiver]). */
const val ACTION_INSTALL_STATUS = "com.kevin.glasslauncher.update.INSTALL_STATUS"

/**
 * Hands a verified [apk] to Android's PackageInstaller as a full install of Glass itself. The result
 * (including "show the confirmation screen") arrives at [UpdateReceiver]. Throws [UpdateException]
 * when the session cannot be written; the session is abandoned then.
 */
fun installApk(context: Context, apk: File) {
    val installer = context.packageManager.packageInstaller
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
        setAppPackageName(context.packageName)
        setSize(apk.length())
    }
    val sessionId = try {
        installer.createSession(params)
    } catch (e: IOException) {
        throw UpdateException(FailReason.Storage)
    } catch (e: SecurityException) {
        throw UpdateException(FailReason.InstallFailed)
    }
    try {
        installer.openSession(sessionId).use { session ->
            session.openWrite(APK_ASSET_NAME, 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            session.commit(statusReceiver(context, sessionId).intentSender)
        }
    } catch (e: IOException) {
        installer.abandonSession(sessionId)
        throw UpdateException(FailReason.Storage)
    } catch (e: SecurityException) {
        installer.abandonSession(sessionId)
        throw UpdateException(FailReason.InstallFailed)
    }
}

/**
 * Explicit intent to our unexported receiver. Mutable because PackageInstaller fills in the status
 * extras; an explicit target keeps that safe.
 */
private fun statusReceiver(context: Context, sessionId: Int): PendingIntent {
    val intent = Intent(context, UpdateReceiver::class.java).setAction(ACTION_INSTALL_STATUS)
    return PendingIntent.getBroadcast(
        context,
        sessionId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
}

/**
 * Abandons every install session Glass still has open (it only ever opens update sessions). Android
 * reports each as aborted to [UpdateReceiver]; a session already being installed is left alone.
 */
fun abandonUpdateSessions(context: Context) {
    val installer = context.packageManager.packageInstaller
    installer.mySessions.forEach { session ->
        try {
            installer.abandonSession(session.sessionId)
        } catch (e: SecurityException) {
            // Already finished or gone: nothing left to abandon.
        }
    }
}

/**
 * What a final PackageInstaller status means for the user; null for success. CONFLICT and
 * INCOMPATIBLE are how Android refuses an update signed with a different key, which is the only
 * signature check on Android 9 and 10 ([archiveCertificatesReadable]).
 */
fun installFailure(status: Int): FailReason? = when (status) {
    PackageInstaller.STATUS_SUCCESS -> null
    PackageInstaller.STATUS_FAILURE_ABORTED -> FailReason.Cancelled
    PackageInstaller.STATUS_FAILURE_STORAGE -> FailReason.Storage
    PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> FailReason.NotTrusted
    else -> FailReason.InstallFailed
}
