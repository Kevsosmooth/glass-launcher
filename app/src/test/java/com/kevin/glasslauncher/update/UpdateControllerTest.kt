package com.kevin.glasslauncher.update

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Unconfined coroutines run each step right away, so state can be read straight after a call. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdateControllerTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val start = 1_800_000_000_000L
    private var now = start
    private var routes = mapOf(LATEST_RELEASE_URL to FakeResponse.ok(releaseJson()))
    private val opened = mutableListOf<String>()

    @Before
    fun clearStore() {
        app.getSharedPreferences("updates", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun controller(currentVersion: String = "0.2.0"): UpdateController {
        val connections = ConnectionFactory { url -> FakeConnections(routes).open(url).also { opened += url.toString() } }
        val env = UpdateEnv(UpdateHttp("Test/1", connections), { now }, currentVersion, Dispatchers.Unconfined)
        return UpdateController(app, CoroutineScope(Dispatchers.Unconfined), env)
    }

    @Test
    fun checkIfDueAsksAtMostOncePerDay() {
        val updates = controller()
        updates.checkIfDue()
        assertEquals(1, opened.size)
        assertEquals(false, (updates.state.value as UpdateState.Available).dismissed)

        now = start + CHECK_INTERVAL_MS - 1
        updates.checkIfDue()
        assertEquals(1, opened.size)

        now = start + CHECK_INTERVAL_MS
        updates.checkIfDue()
        assertEquals(2, opened.size)
    }

    @Test
    fun theDayIsRememberedAcrossRestarts() {
        controller().checkIfDue()
        now = start + 60_000
        val restarted = controller()
        assertTrue(restarted.state.value is UpdateState.Available)
        restarted.checkIfDue()
        assertEquals(1, opened.size)
    }

    @Test
    fun quietCheckFailureDoesNotShowAnError() {
        routes = mapOf(LATEST_RELEASE_URL to FakeResponse(500))
        val updates = controller()
        updates.checkIfDue()
        assertEquals(UpdateState.Idle, updates.state.value)
    }

    @Test
    fun checkNowReportsFailuresAndUpToDate() {
        routes = mapOf(LATEST_RELEASE_URL to FakeResponse(500))
        val updates = controller()
        updates.checkNow()
        assertEquals(UpdateState.Failed(FailReason.Server, null), updates.state.value)

        routes = mapOf(LATEST_RELEASE_URL to FakeResponse.ok(releaseJson(tag = "v0.2.0")))
        updates.checkNow()
        assertEquals(UpdateState.UpToDate(now), updates.state.value)
    }

    @Test
    fun laterHidesTheCardButKeepsTheUpdateOnOffer() {
        val updates = controller()
        updates.checkNow()
        updates.dismiss()
        val state = updates.state.value as UpdateState.Available
        assertTrue(state.dismissed)
        assertEquals("0.3.0", state.release.version)
        assertNull(state.cardRelease())

        assertTrue((controller().state.value as UpdateState.Available).dismissed)
    }

    @Test
    fun aNewerReleaseBringsTheCardBack() {
        val updates = controller()
        updates.checkNow()
        updates.dismiss()
        routes = mapOf(LATEST_RELEASE_URL to FakeResponse.ok(releaseJson(tag = "v0.4.0")))
        updates.checkNow()
        val state = updates.state.value as UpdateState.Available
        assertEquals("0.4.0", state.release.version)
        assertEquals(false, state.dismissed)
    }

    @Test
    fun savedReleaseIsForgottenOnceInstalled() {
        controller().checkNow()
        assertEquals(UpdateState.Idle, controller(currentVersion = "0.3.0").state.value)
        assertEquals(UpdateState.Idle, controller().state.value)
    }
}
