package com.kevin.glasslauncher.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kevin.glasslauncher.AppContainer
import com.kevin.glasslauncher.BuildConfig
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.apps.BannerCache
import com.kevin.glasslauncher.debug.FrameHud
import com.kevin.glasslauncher.edit.SelectGuard
import com.kevin.glasslauncher.edit.moveKeys
import com.kevin.glasslauncher.glass.GlassContext
import com.kevin.glasslauncher.glass.LocalGlass
import com.kevin.glasslauncher.motion.DirectionTracker
import com.kevin.glasslauncher.motion.trackDirection
import com.kevin.glasslauncher.shelf.AppBackdrop
import com.kevin.glasslauncher.shelf.BackdropUnderlay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Home content (wallpaper and glass context come from LauncherRoot). */
@Composable
fun HomeRoute(
    container: AppContainer,
    homeSignals: Flow<Unit>,
    focusMemory: FocusMemory,
    onLaunch: (AppEntry) -> Unit,
    onOpenControls: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    state?.let { HomeScreen(it, container, homeSignals, focusMemory, onLaunch, onOpenControls) }
}

/**
 * Backdrop, then one lazy grid: top area (top shelf, status pill), the glass dock, all apps; the
 * tile menu on top. Focusing the dock scrolls back to the top; Home/Back close the menu, finish a
 * move, scroll up and focus the first dock tile.
 */
@Composable
private fun HomeScreen(
    state: HomeState,
    container: AppContainer,
    homeSignals: Flow<Unit>,
    focusMemory: FocusMemory,
    onLaunch: (AppEntry) -> Unit,
    onOpenControls: () -> Unit,
) {
    val grid = rememberLazyGridState()
    val tracker = remember { DirectionTracker() }
    val dockFocus = remember { DockFocus() }
    val edit = rememberHomeEditHost(container, dockFocus)
    val launch by rememberUpdatedState(onLaunch)
    val env = remember(container.banners, tracker, edit, focusMemory) {
        TileEnv(BannerArtSource(container.banners), tracker, { launch(it) }, edit::openMenu, focusMemory)
    }
    val dock = edit.editor.arrange(state.dock)
    val dockKeys = dock.map { it.key }
    HomeEffects(state, grid, dockFocus, edit, homeSignals)
    val underlay = remember { BackdropUnderlay() }
    val outer = LocalGlass.current
    val glass = remember(outer, state.appBackdrop) {
        if (state.appBackdrop) GlassContext(outer.wallpaper, outer.look, underlay) else outer
    }
    Box(Modifier.fillMaxSize().trackDirection(tracker)) {
        if (state.appBackdrop) Backdrop(grid, dockFocus, container.banners, state.topShelf, underlay)
        CompositionLocalProvider(LocalGlass provides glass) {
            HomeGrid(
                state.grid, env, grid,
                top = { HomeTop(state, dockFocus, container.banners, tracker, onLaunch, onOpenControls) },
                dock = { MovableDock(dock, env, dockFocus, edit, grid) },
                modifier = Modifier.fillMaxSize(),
            )
            edit.Menu(dockKeys, state.dockSize)
        }
        if (BuildConfig.DEBUG && state.hud) FrameHud(Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun HomeEffects(
    state: HomeState,
    grid: LazyGridState,
    dockFocus: DockFocus,
    edit: HomeEditHost,
    homeSignals: Flow<Unit>,
) {
    val savedDock = state.dock.map { it.key }
    LaunchedEffect(dockFocus) { dockFocus.debounce() }
    LaunchedEffect(savedDock) { edit.editor.sync(savedDock) }
    LaunchedEffect(edit.restore) {
        edit.restore?.let {
            it.requestFocus()
            edit.restored()
        }
    }
    LaunchedEffect(homeSignals) {
        homeSignals.collect {
            edit.reset()
            grid.scrollToItem(0)
            dockFocus.wantFirst = true
        }
    }
    BackHandler(enabled = edit.editor.moving != null) { edit.drop() }
}

/** The dock; while a tile is moving, Left/Right move it and Select drops it. */
@Composable
private fun MovableDock(dock: List<AppEntry>, env: TileEnv, dockFocus: DockFocus, edit: HomeEditHost, grid: LazyGridState) {
    val scope = rememberCoroutineScope()
    val moving = edit.editor.moving
    val guard = remember(moving) { SelectGuard() }
    Dock(
        dock, env, dockFocus, moving,
        onFocusEnter = { scope.launch { if (!grid.isAtTop()) grid.animateScrollToItem(0) } },
        modifier = Modifier.moveKeys(moving != null, guard, edit.editor::step, edit::drop),
    )
}

/** The focused dock app's backdrop, down to the dock slab, while the list is at the top. */
@Composable
private fun Backdrop(grid: LazyGridState, dockFocus: DockFocus, banners: BannerCache, topShelf: Boolean, underlay: BackdropUnderlay) {
    val atTop by remember(grid) { derivedStateOf { grid.isAtTop() } }
    AppBackdrop(
        if (atTop) dockFocus.shelfApp else null,
        banners,
        underlay,
        Modifier.fillMaxWidth().height(topAreaHeight(topShelf) + HomeLayout.RowGap),
    )
}
