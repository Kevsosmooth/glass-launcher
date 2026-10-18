package com.kevin.glasslauncher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.debug.DebugTools
import com.kevin.glasslauncher.settings.SettingsPage
import com.kevin.glasslauncher.ui.LauncherRoot
import com.kevin.glasslauncher.ui.Overlay
import com.kevin.glasslauncher.ui.RootController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

private const val EXTRA_OPEN = "open"

/** `--es open wifi` etc. opens that Settings page (debug builds, for screenshots). */
private val DEBUG_PAGES = mapOf(
    "wifi" to SettingsPage.Wifi,
    "bluetooth" to SettingsPage.Bluetooth,
    "sound" to SettingsPage.Sound,
    "display" to SettingsPage.Display,
    "storage" to SettingsPage.Storage,
    "about" to SettingsPage.About,
    "backup" to SettingsPage.Backup,
    "homehelper" to SettingsPage.HomeHelper,
)

/**
 * HOME + LEANBACK_LAUNCHER activity (singleTask). Back and a repeated Home return to the top;
 * Menu toggles Quick Controls. `--es open settings|controls` opens a panel (used by tools/).
 */
class MainActivity : ComponentActivity() {
    private val homeSignals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val root = RootController()
    private val container get() = (application as LauncherApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleExtras(intent)
        // A launcher never finishes on Back.
        onBackPressedDispatcher.addCallback(this) { homeSignals.tryEmit(Unit) }
        setContent { LauncherRoot(container, root, homeSignals, onLaunch = ::launch) }
    }

    override fun onNewIntent(intent: Intent) {
        // Home while already on the home screen; coming back from an app keeps the user's place.
        // Android pauses the activity before a new intent, so check window focus (as Launcher3 does).
        val broughtToFront = intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0
        val alreadyHome = hasWindowFocus() && !broughtToFront
        super.onNewIntent(intent)
        if (alreadyHome && intent.hasCategory(Intent.CATEGORY_HOME)) homeSignals.tryEmit(Unit)
        handleExtras(intent)
    }

    override fun onStart() {
        super.onStart()
        container.apps.refresh()
    }

    private fun handleExtras(intent: Intent) {
        DebugTools.applyLaunchExtras(intent, container)
        // Debug only: in a release build another app could pop a panel up over playback.
        if (!BuildConfig.DEBUG) return
        when (intent.getStringExtra(EXTRA_OPEN)) {
            "settings" -> root.openSettings()
            "controls" -> root.overlay = Overlay.QuickControls
            else -> DEBUG_PAGES[intent.getStringExtra(EXTRA_OPEN)]?.let(root::openSettings)
        }
    }

    private fun launch(app: AppEntry) {
        if (container.apps.launch(this, app)) {
            lifecycleScope.launch { container.prefs.recordLaunch(app.key) }
        } else {
            Toast.makeText(this, getString(R.string.cannot_open, app.label), Toast.LENGTH_SHORT).show()
        }
    }
}
