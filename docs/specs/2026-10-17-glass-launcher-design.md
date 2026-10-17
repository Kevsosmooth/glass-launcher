# Glass Launcher - design spec

Date: 2026-10-17. Status: approved; the MVP implements it.

## Goal

An Android TV home screen that looks and feels like Apple tvOS 26 (Liquid Glass),
written in Kotlin, that is visibly smoother and lighter than the stock launcher and
Projectivy, without giving up the look. Primary device: NVIDIA Shield (Android 11,
API 30, 3 GB RAM, Tegra X1, UI at 1920x1080). Secondary: onn Google TV boxes and
Fire TV devices on Fire OS 7/8. Vega OS Fire Sticks cannot run Android apps and are
out of scope.

## Non-goals (v1)

- Accessibility-service Home-button override (Fire TV / onn). Possible v2.
- Widgets, icon packs, animated/video wallpapers, profiles, parental controls.
- Ads, recommendations, analytics. None, ever.

## Home screen

Layout at 1920x1080 (960x540 dp):

1. **Status pill** (top right): glass pill with time and a settings glyph. D-pad Up
   from the dock reaches it; Select or the Menu key opens Quick Controls.
2. **Top shelf** (top ~55%): content for the app focused in the dock, as on tvOS.
   - If the app publishes Android TV preview channels, show its first channel as a
     row of large rounded cards.
   - Otherwise show the app's banner large over a blurred backdrop.
   - Default when focus first lands: Continue Watching (Watch Next), if readable.
   - Fallback if channel/Watch Next data is not readable (spike decides): "Recent
     apps" row.
3. **Dock**: frosted glass slab with up to 6 favorites as 16:9 banner tiles
   (rounded corners, about 16 dp). Apps without a banner get their icon centred on a
   tile tinted from the icon's dominant colour.
4. **All apps grid** below the dock, 6 columns. Scrolling down slides the top shelf
   up and away; scrolling back up restores it.

Sources: `Intent.CATEGORY_LEANBACK_LAUNCHER` apps (plus `CATEGORY_LAUNCHER` apps
that have no leanback entry, shown with icons). Package add/remove/change updates
the list live.

## Motion

All motion uses springs (no fixed-duration tweens), drawn in the graphics layer so
nothing re-lays out during focus changes.

- **Focus**: scale 1.0 -> ~1.15, shadow lifts (elevation and blur grow), a specular
  sheen sweeps once across the top edge. Neighbours never get covered: tile spacing
  reserves room for the focused size.
- **Directional tilt**: each D-pad press tilts the newly focused tile 3-4 degrees
  toward the direction of travel, then springs back (stands in for tvOS touchpad
  parallax).
- **Row/shelf transitions**: shelf content crossfades and slides with a spring when
  the focused dock app changes; debounce so fast scrolling does not load every app.
- **Edit mode**: long-press Select -> tiles wiggle; Left/Right moves the held tile,
  Select drops it; a small menu offers Unpin / Hide / App info / Uninstall.
- **Quick Controls**: panel slides in from the right with a spring.

All values (scale, spring stiffness/damping, tilt, shadow) live in one `Motion`
tokens file so they can be tuned by eye on the Shield.

## Quick Controls (tvOS Control Center)

Glass panel on the right, opened from the status pill or the Menu key.

| Control | Kind | How it works |
| --- | --- | --- |
| Sleep | large tile | `DevicePolicyManager.lockNow()` (device admin, one-time user grant) |
| Sleep timer | large tile | 15/30/60/90 min; scheduled with `AlarmManager`, then `lockNow()` |
| Wi-Fi | large tile | Opens `Settings.ACTION_WIFI_SETTINGS`; shows current SSID if allowed |
| Inputs | large tile | Lists HDMI inputs via `TvInputManager`; launches the passthrough channel |
| Display, Sound, Android Settings, Launcher Settings | round buttons | Open the matching settings screens |

The focused control turns solid white with dark text (Apple's pattern; readable
from across the room).

## Look

- **Glass**: the wallpaper is decoded at 1/8 size, blurred once (stack blur in
  Kotlin), and kept as one small bitmap. Each glass surface draws the matching
  region of that bitmap (aligned to its on-screen position), then a tint, a 1 dp
  bright top edge, and a soft shadow. Cost is about one bitmap draw per surface, so
  it runs on API 28+ with no `RenderEffect`.
- **Solid mode** (Apple's Reduce Transparency): opaque dark panels, same layout.
- **Contrast**: text on glass always sits on a tint dark enough for 4.5:1; titles
  use a slight text shadow. This addresses the main Liquid Glass complaint
  (legibility).
- **Wallpaper**: built-in set plus a user image from storage. Re-blur only when the
  wallpaper changes, on a background thread.

## Settings (one short screen)

Wallpaper, Look (Glass/Solid), dock size (4-6), hidden apps, 12/24-hour clock,
"Make default launcher" help page (shows the adb commands for Shield/onn).

## Architecture

Single `:app` module plus `:benchmark` (macrobenchmark + baseline profile).

```
app/src/main/java/.../
  MainActivity.kt            HOME + LEANBACK_LAUNCHER, singleTask
  apps/                      AppRepository (PackageManager), PackageChangeReceiver,
                             BannerCache (LRU at tile size, cleared on onTrimMemory)
  data/                      LauncherPrefs (DataStore): favorites, order, hidden,
                             look, wallpaper, clock
  glass/                     Wallpaper loader, blur, GlassSurface modifier, Solid mode
  motion/                    Motion tokens, focus/tilt/sheen modifiers
  home/                      HomeScreen, HomeViewModel
  shelf/                     TopShelf, channel/Watch Next reader, fallback
  dock/                      Dock, DockTile, edit mode
  grid/                      AllAppsGrid
  controls/                  QuickControls panel, sleep/timer, inputs
  settings/                  SettingsScreen
```

Stack: Kotlin, Compose for TV (`androidx.tv:tv-material`), Compose foundation,
DataStore, AndroidX Lifecycle/ViewModel. No Room, no DI framework, no image
library. R8 full mode. minSdk 28, targetSdk current. Files follow the
`code-organization` limits (components < 400 lines, functions < 80).

Licensing: nielsvanvelzen/tv-launcher (GPL-3.0) is studied for patterns only; no
code copied unless we choose GPL-3.0 for this project.

## Performance and feel: budgets and how they are tested

The point: prove it is fast and smooth with the full look on, not by turning
effects off. Every number below is measured with **Glass mode on, all motion on**.

### Budgets (NVIDIA Shield, release build)

| Metric | Budget | Tool |
| --- | --- | --- |
| Frame time while navigating dock/grid (D-pad held) | p50 < 8 ms, p99 < 16.7 ms | Macrobenchmark `FrameTimingMetric` |
| Janky frames during navigation | < 1% | Macrobenchmark, `dumpsys gfxinfo` |
| Key press -> first frame of focus change | within 1 frame (< 17 ms) | `gfxinfo framestats` (input to frame) |
| Cold start to first frame (TTID) | < 500 ms | Macrobenchmark `StartupTimingMetric` |
| Back to Home from an app (warm) | < 150 ms | Macrobenchmark |
| Idle RAM (PSS) on home screen | < 70 MB | `dumpsys meminfo` |
| CPU while idle on home screen | ~0% (no animation loop when nothing moves) | `top` / `dumpsys cpuinfo` |

Baseline for comparison, measured with the same tools at the start: the stock
`com.google.android.tvlauncher` (RAM, start time, frame stats).

### Feel testing (by eye)

- **Debug HUD** (debug builds only): small overlay with live FPS, a frame-time
  graph, and a red flash on any dropped frame, toggled from Launcher Settings. Lets
  you see jank while using it, not just in reports.
- **Scripted runs**: `tools/` scripts send D-pad sequences to the Shield (dock sweep,
  grid sweep, open/close Quick Controls, edit-mode move) and collect gfxinfo +
  meminfo into a one-page report.
- **Stress case**: a debug toggle that fills the grid with 200 synthetic apps.
- **Side-by-side video**: screen recording of the same scripted run in the stock
  launcher and in Glass Launcher.

### Look is protected too

- **Screenshot tests** (Roborazzi, JVM, no device): home, focused tile, edit mode,
  Quick Controls, Solid mode. A performance change that alters pixels fails the test
  until the new shot is reviewed, so speed fixes cannot quietly downgrade the look.
- **On-device shots** at each milestone via `adb exec-out screencap`.

## Milestones

0. **Spike (throwaway, about a day)**: on the Shield, check (a) whether a
   non-system app can read other apps' preview channels and Watch Next, and (b) a
   bare glass dock + focus animation hits the frame budget. Report before M1.
1. **Skeleton**: HOME activity, app list, dock + grid, focus motion, glass, prefs.
   Benchmarks and HUD land here, not at the end.
2. **Top shelf** (per spike result).
3. **Quick Controls**.
4. **Edit mode + Settings**.
5. **Polish pass**: tune motion tokens on the Shield, screenshot review, final
   benchmark report vs stock launcher.

## Device rules

- New package id, so installing never replaces the stock launcher.
- Make it the only home app on Google TV / Android TV with `tools/make-default.sh`
  (dry run by default; `--undo` turns the stock launcher back on).

## Open questions

- App name and package id: Glass Launcher, `com.kevin.glasslauncher`.
- License: MIT.
