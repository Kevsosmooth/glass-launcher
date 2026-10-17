package com.kevin.glasslauncher.glass

import com.kevin.glasslauncher.data.Prefs
import com.kevin.glasslauncher.data.Rotation
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** The wallpaper settings that decide which wallpaper shows. */
data class WallpaperChoice(val selectedId: String, val rotation: Rotation, val sinceMillis: Long)

/**
 * Id of the wallpaper to show, emitted only when it changes. Follows [prefs]; between changes it
 * sleeps with a single delay until the next rotation boundary (no polling). [clockJumps] restarts
 * the wait when wall-clock time may have moved under it (time set, zone change, wake from sleep).
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun effectiveWallpaperIds(
    prefs: Flow<Prefs>,
    clockJumps: Flow<Unit>,
    now: () -> Long = System::currentTimeMillis,
    zone: () -> ZoneId = ZoneId::systemDefault,
): Flow<String> {
    val choices = prefs.map { WallpaperChoice(it.wallpaperId, it.rotation, it.rotationSinceMillis) }.distinctUntilChanged()
    return combine(choices, clockJumps.onStart { emit(Unit) }) { choice, _ -> choice }
        .flatMapLatest { choice ->
            flow {
                while (true) {
                    val t = now()
                    val z = zone()
                    emit(wallpaperFor(choice.selectedId, choice.rotation, t, z, choice.sinceMillis))
                    val next = nextChangeMillis(choice.rotation, t, z) ?: break
                    delay((next - t).coerceAtLeast(1L))
                }
            }
        }
        .distinctUntilChanged()
}
