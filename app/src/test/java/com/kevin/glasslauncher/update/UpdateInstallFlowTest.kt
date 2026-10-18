package com.kevin.glasslauncher.update

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageInstaller
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.security.MessageDigest

/** Download, check and install with a fake installer; Unconfined coroutines run each step right away. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdateInstallFlowTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val start = 1_800_000_000_000L
    private var now = start
    private val apk = ByteArray(1234) { (it % 251).toByte() }
    private val apkSha = MessageDigest.getInstance("SHA-256").digest(apk).toHex()
    private val routes = mapOf(
        LATEST_RELEASE_URL to FakeResponse.ok(releaseJson(digest = "sha256:$apkSha", size = apk.size.toLong())),
        FIXTURE_APK_URL to FakeResponse.ok(apk),
    )
    private val installer = FakeInstaller()
    private val installerScreen = ComponentName(INSTALLER_PACKAGE, INSTALLER_ACTIVITY)

    @Before
    fun setUp() {
        app.getSharedPreferences("updates", Context.MODE_PRIVATE).edit().clear().commit()
        shadowOf(app.packageManager).setCanRequestPackageInstalls(true)
    }

    private fun startedUpdate(): UpdateController {
        val env = UpdateEnv(UpdateHttp("Test/1", FakeConnections(routes)), { now }, "0.2.0", Dispatchers.Unconfined) { installer }
        val updates = UpdateController(app, CoroutineScope(Dispatchers.Unconfined), env)
        updates.checkNow()
        updates.startUpdate()
        return updates
    }

    private fun installing(): UpdateController =
        startedUpdate().also { assertTrue(it.state.value is UpdateState.Installing) }

    private fun release(updates: UpdateController): ReleaseInfo = checkNotNull(updates.state.value.cardRelease())

    @Test
    fun theVerifiedDownloadIsInstalledAndConfirmed() {
        registerConfirmScreen(app, system = true)
        val updates = installing()
        assertArrayEquals(apk, installer.installed.single())

        updates.onConfirmNeeded(confirmIntent())
        assertEquals(installerScreen, shadowOf(app).nextStartedActivity?.component)
    }

    @Test
    fun returningToGlassShowsTheConfirmationAgain() {
        registerConfirmScreen(app, system = true)
        val updates = installing()
        updates.onConfirmNeeded(confirmIntent())
        shadowOf(app).nextStartedActivity

        now += 60_000
        updates.checkIfDue()
        assertEquals(installerScreen, shadowOf(app).nextStartedActivity?.component)
        assertTrue(updates.state.value is UpdateState.Installing)
    }

    @Test
    fun noAnswerForTenMinutesAbandonsTheSessionAndOffersTryAgain() {
        registerConfirmScreen(app, system = true)
        val updates = installing()
        val release = release(updates)
        updates.onConfirmNeeded(confirmIntent())

        now = start + INSTALL_TIMEOUT_MS + 1
        updates.checkIfDue()
        assertEquals(UpdateState.Failed(FailReason.InstallFailed, release), updates.state.value)
        assertEquals(1, installer.abandoned)

        // The abandoned session's own "aborted" answer arrives late and changes nothing.
        updates.onInstallResult(PackageInstaller.STATUS_FAILURE_ABORTED)
        assertEquals(UpdateState.Failed(FailReason.InstallFailed, release), updates.state.value)
    }

    @Test
    fun aConfirmationScreenFromAnOrdinaryAppIsNeverStarted() {
        registerConfirmScreen(app, system = false)
        val updates = installing()
        updates.onConfirmNeeded(confirmIntent())
        assertNull(shadowOf(app).nextStartedActivity)
        assertEquals(UpdateState.Failed(FailReason.InstallFailed, release(updates)), updates.state.value)
        assertEquals(1, installer.abandoned)
    }

    @Test
    fun androidRefusingADifferentSignerIsNotTrusted() {
        val updates = installing()
        updates.onInstallResult(PackageInstaller.STATUS_FAILURE_CONFLICT)
        assertEquals(UpdateState.Failed(FailReason.NotTrusted, release(updates)), updates.state.value)
    }

    @Test
    fun aRefusedApkNeverReachesTheInstaller() {
        installer.problem = FailReason.NotTrusted
        val updates = startedUpdate()
        assertEquals(FailReason.NotTrusted, (updates.state.value as UpdateState.Failed).reason)
        assertTrue(installer.installed.isEmpty())
    }
}
