package com.kevin.glasslauncher.update

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

/** Who an APK claims to be: package, version code, and the SHA-256 (hex) of each signing certificate. */
data class ApkIdentity(val packageName: String, val versionCode: Long, val certSha256: Set<String>)

/**
 * Whether Android can read a downloaded APK's signing certificates. Android 9 and 10 only collect
 * them for `getPackageArchiveInfo` with the deprecated GET_SIGNATURES flag (GET_SIGNING_CERTIFICATES
 * leaves signingInfo null), so there Glass checks package and version only and relies on
 * PackageInstaller, which refuses an update signed with a different key ([installFailure] reports
 * that as [FailReason.NotTrusted]). Android 11+ gets the full certificate check before installing.
 */
fun archiveCertificatesReadable(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.R

/**
 * Null when [candidate] may replace [installed]: same package, a higher version code, and (when
 * [checkCertificates]) exactly the same signing certificates. Otherwise the reason it must never
 * reach the installer. A null [candidate] (Android could not read the file) is [FailReason.Corrupt].
 */
fun upgradeProblem(candidate: ApkIdentity?, installed: ApkIdentity, checkCertificates: Boolean): FailReason? = when {
    candidate == null -> FailReason.Corrupt
    candidate.packageName != installed.packageName -> FailReason.NotTrusted
    checkCertificates && (installed.certSha256.isEmpty() || candidate.certSha256 != installed.certSha256) -> FailReason.NotTrusted
    candidate.versionCode <= installed.versionCode -> FailReason.NotNewer
    else -> null
}

/**
 * The downloaded APK at [path] as Android parses it, or null when it is not a readable APK. Without
 * [withCertificates] the identity has no certificates (see [archiveCertificatesReadable]).
 */
fun PackageManager.archiveIdentity(path: String, withCertificates: Boolean): ApkIdentity? {
    val flags = if (withCertificates) PackageManager.GET_SIGNING_CERTIFICATES else 0
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        getPackageArchiveInfo(path, flags)
    } ?: return null
    return if (withCertificates) identityOf(info) else ApkIdentity(info.packageName, info.longVersionCode, emptySet())
}

/** The installed [packageName]; its certificates are empty if Android withholds them (never trusted). */
fun PackageManager.installedIdentity(packageName: String): ApkIdentity {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
    } else {
        getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    }
    return identityOf(info) ?: ApkIdentity(packageName, info.longVersionCode, emptySet())
}

/**
 * Multiple signers: all current signers. One signer: its certificate history (key rotation), so a
 * rotated key differs from the installed one and is refused until Glass is reinstalled by hand.
 */
private fun identityOf(info: PackageInfo): ApkIdentity? {
    val signing = info.signingInfo ?: return null
    val certs: Array<Signature> = (if (signing.hasMultipleSigners()) signing.apkContentsSigners else signing.signingCertificateHistory)
        ?: return null
    return ApkIdentity(info.packageName, info.longVersionCode, certs.map { sha256Hex(it.toByteArray()) }.toSet())
}

internal fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

/** Lowercase hex, two digits per byte. */
internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
