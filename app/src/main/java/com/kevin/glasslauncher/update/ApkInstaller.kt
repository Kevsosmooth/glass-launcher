package com.kevin.glasslauncher.update

import android.content.Context
import android.os.Build
import java.io.File

/** Android's part of an update: the identity check, the install session, and giving up on it. Tests fake it. */
interface ApkInstaller {
    /** Null when [apk] may replace the installed Glass; otherwise why it must not be installed. */
    fun problem(apk: File): FailReason?

    /** Hands [apk] to PackageInstaller; the result arrives at [UpdateReceiver]. Throws [UpdateException]. */
    fun install(apk: File)

    /** Abandons Glass's open install sessions. */
    fun abandon()
}

/** The real [ApkInstaller]: PackageManager for the check, PackageInstaller for the install. */
class SystemApkInstaller(context: Context) : ApkInstaller {
    private val app = context.applicationContext

    override fun problem(apk: File): FailReason? {
        val pm = app.packageManager
        val certificates = archiveCertificatesReadable(Build.VERSION.SDK_INT)
        return upgradeProblem(pm.archiveIdentity(apk.path, certificates), pm.installedIdentity(app.packageName), certificates)
    }

    override fun install(apk: File) = installApk(app, apk)

    override fun abandon() = abandonUpdateSessions(app)
}
