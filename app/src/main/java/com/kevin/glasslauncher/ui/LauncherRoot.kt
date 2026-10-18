package com.kevin.glasslauncher.ui

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.glasslauncher.AppContainer
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.controls.QuickControlsPanel
import com.kevin.glasslauncher.data.Look
import com.kevin.glasslauncher.debug.StatsOverlay
import com.kevin.glasslauncher.glass.GlassContext
import com.kevin.glasslauncher.glass.LocalGlass
import com.kevin.glasslauncher.glass.WallpaperBackground
import com.kevin.glasslauncher.home.FocusMemory
import com.kevin.glasslauncher.home.HomeRoute
import com.kevin.glasslauncher.homebutton.homeOpensGlass
import com.kevin.glasslauncher.settings.SettingsPage
import com.kevin.glasslauncher.settings.SettingsScreen
import com.kevin.glasslauncher.weather.WeatherOverlay
import kotlinx.coroutines.flow.Flow

/** Panels drawn over home. */
private const val HOME_BEHIND_CONTROLS = 0.2f

enum class Overlay { Settings, QuickControls, HomeSetup, Weather }

/** Which overlay is open; Menu (via [handleMenu]) and the activity's launch extras flip it. */
class RootController {
    var overlay by mutableStateOf<Overlay?>(null)

    /** The page Settings opens on; Quick Controls sets it for Wi-Fi, Display and Sound. */
    var settingsPage by mutableStateOf(SettingsPage.Main)
        private set

    /** Bumped on every openSettings so asking for the same page again re-opens it. */
    var settingsRequest by mutableIntStateOf(0)
        private set

    fun toggle(target: Overlay) {
        settingsPage = SettingsPage.Main
        overlay = if (overlay == target) null else target
    }

    /**
     * Menu toggles Quick Controls; true when [event] was Menu. Called from LauncherRoot's preview
     * pass, before a focused row (which would consume it) sees the key.
     */
    fun handleMenu(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_MENU) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) toggle(Overlay.QuickControls)
        return true
    }

    fun openSettings(page: SettingsPage = SettingsPage.Main) {
        settingsPage = page
        settingsRequest++
        overlay = Overlay.Settings
    }
}

/** Wallpaper + glass context, home, and the open overlay. Back or Home closes the overlay. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LauncherRoot(
    container: AppContainer,
    root: RootController,
    homeSignals: Flow<Unit>,
    onLaunch: (AppEntry) -> Unit,
) {
    val prefs by container.prefs.prefs.collectAsStateWithLifecycle(initialValue = null)
    val wallpaper by container.wallpaper.wallpaper.collectAsStateWithLifecycle()
    val look = prefs?.look ?: Look.Glass
    val glass = remember(wallpaper, look) { GlassContext(wallpaper, look) }
    val focusMemory = remember { FocusMemory() }
    LaunchedEffect(homeSignals) {
        homeSignals.collect {
            focusMemory.discard()
            root.overlay = null
        }
    }
    // First run: offer to make Glass the Home screen, once, unless Home already opens it.
    val context = LocalContext.current
    LaunchedEffect(prefs?.homeSetupSeen) {
        if (prefs?.homeSetupSeen == false && root.overlay == null && !homeOpensGlass(context)) {
            root.overlay = Overlay.HomeSetup
            container.prefs.setHomeSetupSeen()
        }
    }
    BackHandler(enabled = root.overlay != null) { root.overlay = null }
    val homeFocus = remember { FocusRequester() }
    var overlayWasOpen by remember { mutableStateOf(false) }
    var homeHasFocus by remember { mutableStateOf(false) }
    // Closing a panel removes the focused node; put focus back on the home tile it came from,
    // unless Home already moved it to the dock.
    LaunchedEffect(root.overlay) {
        if (root.overlay != null) {
            if (!overlayWasOpen) focusMemory.save()
            overlayWasOpen = true
        } else if (overlayWasOpen) {
            overlayWasOpen = false
            withFrameNanos { }
            if (!focusMemory.restore() && !homeHasFocus) homeFocus.requestFocus()
        }
    }
    CompositionLocalProvider(LocalGlass provides glass) {
        Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }.onPreviewKeyEvent { root.handleMenu(it.nativeKeyEvent) }) {
            WallpaperBackground(glass)
            // Quick Controls float on the wallpaper with no panel, and the weather card sits in the
            // middle; home steps back so tiles behind them do not compete with them.
            val homeDims = root.overlay == Overlay.QuickControls || root.overlay == Overlay.Weather
            val homeAlpha by animateFloatAsState(if (homeDims) HOME_BEHIND_CONTROLS else 1f, label = "home")
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = homeAlpha }
                    .focusRequester(homeFocus).onFocusChanged { homeHasFocus = it.hasFocus }.focusRestorer(),
            ) {
                HomeRoute(
                    container, homeSignals, focusMemory, onLaunch,
                    controlsOpen = root.overlay == Overlay.QuickControls,
                    onOpenControls = { root.overlay = Overlay.QuickControls },
                    onOpenWeather = { root.overlay = Overlay.Weather },
                )
            }
            when (root.overlay) {
                Overlay.Settings -> key(root.settingsRequest) {
                    SettingsScreen(container, onClose = { root.overlay = null }, startPage = root.settingsPage)
                }
                Overlay.QuickControls -> QuickControlsPanel(
                    container,
                    onClose = { root.overlay = null },
                    onOpenSettings = root::openSettings,
                )
                Overlay.HomeSetup -> HomeSetupPanel(
                    onSetUp = { root.openSettings(SettingsPage.HomeHelper) },
                    onClose = { root.overlay = null },
                )
                Overlay.Weather -> WeatherOverlay(container, onClose = { root.overlay = null })
                null -> Unit
            }
            if (prefs?.hud == true) StatsOverlay(Modifier.align(Alignment.TopStart))
        }
    }
}
