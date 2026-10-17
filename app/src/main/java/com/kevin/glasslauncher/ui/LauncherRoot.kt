package com.kevin.glasslauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.glasslauncher.AppContainer
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.controls.QuickControlsPanel
import com.kevin.glasslauncher.data.Look
import com.kevin.glasslauncher.glass.GlassContext
import com.kevin.glasslauncher.glass.LocalGlass
import com.kevin.glasslauncher.glass.WallpaperBackground
import com.kevin.glasslauncher.home.FocusMemory
import com.kevin.glasslauncher.home.HomeRoute
import com.kevin.glasslauncher.settings.SettingsScreen
import kotlinx.coroutines.flow.Flow

/** Panels drawn over home. */
enum class Overlay { Settings, QuickControls }

/** Which overlay is open; the activity flips it from the Menu key and launch extras. */
class RootController {
    var overlay by mutableStateOf<Overlay?>(null)

    fun toggle(target: Overlay) {
        overlay = if (overlay == target) null else target
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
        Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
            WallpaperBackground(glass)
            Box(Modifier.fillMaxSize().focusRequester(homeFocus).onFocusChanged { homeHasFocus = it.hasFocus }.focusRestorer()) {
                HomeRoute(container, homeSignals, focusMemory, onLaunch, onOpenControls = { root.overlay = Overlay.QuickControls })
            }
            when (root.overlay) {
                Overlay.Settings -> SettingsScreen(container, onClose = { root.overlay = null })
                Overlay.QuickControls -> QuickControlsPanel(
                    container,
                    onClose = { root.overlay = null },
                    onOpenSettings = { root.overlay = Overlay.Settings },
                )
                null -> Unit
            }
        }
    }
}
