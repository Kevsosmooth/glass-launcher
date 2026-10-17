# Glass Launcher

An Android TV home screen with the look of Apple tvOS 26 "Liquid Glass": a frosted glass dock,
large banner tiles, springy focus motion, and a Quick Controls panel. Written in Kotlin with
Jetpack Compose (foundation only), built to be smoother and lighter than the stock launcher
without turning the effects off. No ads, no recommendations, no analytics.

Status: early (v0.1.0). Developed and measured on an NVIDIA Shield (Android 11); Google TV and
Fire TV devices are secondary targets.

| Home | Edit mode |
| --- | --- |
| ![Home screen](docs/images/home.jpg) | ![Edit mode](docs/images/edit-mode.jpg) |
| **Quick Controls** | **Settings** |
| ![Quick Controls](docs/images/quick-controls.jpg) | ![Settings](docs/images/settings.jpg) |
| **Wallpapers** | |
| ![Wallpaper picker](docs/images/wallpapers.jpg) | |

## Features

- **Glass dock** with up to six favorites as 16:9 banner tiles, and an **All apps** grid below it.
  Apps without a banner get their icon on a tile tinted from the icon's colour.
- **Top shelf** above the dock that follows the focused app.
- **Edit mode**: long-press Select on a dock tile, then move, unpin, hide, open App info or
  uninstall. Everything works with the D-pad alone.
- **Quick Controls** (Menu key, or the status pill): Sleep, Sleep timer, Wi-Fi, Inputs, and shortcuts
  to Display, Sound, Android Settings and Launcher Settings. The focused control turns solid white
  with dark text so it reads from across the room.
- **Look**: Glass, or Solid (opaque panels, like tvOS "Reduce Transparency").
- **Background**: a backdrop that follows the focused app, and/or rotating built-in wallpapers
  (hourly, daily, or off). Each is switchable in Settings.
- **Wallpapers**: soft colour fields plus free-licence photos grouped by category (Nature, Ocean,
  Night, Mountains, Space), one left/right row per category. Sources and licences:
  [docs/wallpaper-credits.md](docs/wallpaper-credits.md).
- Settings: look, wallpaper, background mode, dock size (4 to 6), hidden apps, 12/24-hour clock.
- **Device pages** in the same glass style: Wi-Fi (connection, signal, band, nearby networks),
  Bluetooth (paired devices), Sound (volume slider and mute, or your TV's volume over HDMI-CEC;
  audio formats), Display (resolution, refresh rate, HDR, supported modes), Storage and About.
  Android only lets apps read most of this, so joining networks, pairing and changing the
  resolution open Android's own page, one row away.
- Debug builds add a frame-time HUD for spotting jank by eye.

## Install

Requires Android TV or Google TV, Android 9 (API 28) or newer.

**On the TV, no computer needed.**

1. Install **Downloader** by AFTVnews from the Play Store.
2. Let it install apps: Settings, Device Preferences, Security & restrictions, Unknown sources,
   Downloader (menu names vary a little between TVs).
3. Open Downloader and enter the code **9798602**, or the full address
   `https://github.com/Kevsosmooth/glass-launcher/releases/latest/download/glass-launcher.apk`.
4. Install, then open Glass Launcher from your apps.

The code always points to the newest release. One APK runs on every TV (ARM and x86).

**With adb.** Download `glass-launcher.apk` from the [Releases](../../releases) page, then:

```
adb connect <tv-ip>:5555
adb -s <tv-ip>:5555 install -r glass-launcher.apk
```

**From source.** Needs JDK 17 and the Android SDK:

```
./gradlew :app:assembleRelease          # app/build/outputs/apk/release/app-release.apk
```

Release builds are signed with your debug key unless a `keystore.properties` (storeFile,
storePassword, keyAlias, keyPassword) sits in the repo root; it is git-ignored.

or use the helper, which can build on another machine over ssh and copies the APK to `out/`:

```
BUILD_HOST=my-build-box tools/build.sh release      # leave BUILD_HOST unset to build here
DEVICE=<tv-ip>:5555 tools/install.sh release
```

Open it from the app list. To use it as the Home screen, see below.

## Make it the default Home screen

Android TV picks Home from the launchers installed. The reliable way to make Glass the only
one is to disable the stock launcher for your user (reversible, no root):

```
DEVICE=<tv-ip>:5555 tools/make-default.sh            # dry run: prints what it would do
DEVICE=<tv-ip>:5555 tools/make-default.sh --apply    # disables the stock launcher(s)
DEVICE=<tv-ip>:5555 tools/make-default.sh --undo     # brings the stock launcher back
```

The script refuses to disable anything unless Glass is installed first. If Projectivy Launcher is
present it asks before uninstalling it (`--remove-projectivy` answers yes; an uninstall cannot be
undone). Equivalent by hand:
`adb shell pm disable-user --user 0 com.google.android.tvlauncher` (undo with `pm enable`).

If a TV ever ends up without a Home screen, `--undo` (or the `pm enable` line) fixes it.

**Fire TV / Fire OS.** Fire OS does not let apps replace the Home screen, and its launcher cannot
be disabled without root. Glass Launcher runs there as a normal app (find it under Your Apps),
but the Home button still opens Fire TV's own launcher. `make-default.sh` explains this and exits.

## Settings

Open Launcher Settings from Quick Controls. Look (Glass or Solid), wallpaper and wallpaper
rotation, the focused-app backdrop, the top shelf, dock size, hidden apps and the 12/24-hour clock.
The Device section lists Wi-Fi, Bluetooth, Sound, Display, Storage and About this TV with their
current values, plus Android Settings. Wi-Fi network names need the location permission (Android's
rule; Glass never reads your location) and Location turned on.

## Performance budgets

Measured on the Shield with a release build, Glass look and all motion on:

| Metric | Budget |
| --- | --- |
| Frame time while moving through the dock or grid | p50 < 8 ms, p99 < 16.7 ms |
| Janky frames while navigating | < 1% |
| Idle RAM (PSS) on the home screen | < 70 MB |
| CPU while idle on the home screen | about 0% (nothing animates when nothing moves) |
| Cold start to first frame | < 500 ms |

Measure it yourself on a TV you own:

```
tools/build.sh release && tools/install.sh release
DEVICE=<tv-ip>:5555 tools/measure.sh my-tv       # writes reports/<date>-my-tv/summary.md
DEVICE=<tv-ip>:5555 tools/bench.sh               # macrobenchmarks (startup, frame timing)
```

`measure.sh` compares frame times, jank, RAM and idle CPU against the table above and exits
non-zero when a budget is missed.

Latest Shield run (v0.1.0, warm, second run after install): frame times and idle CPU are within
budget (dock p50 7.8 ms / p99 15.9 ms, grid p50 6.6 ms / p99 10.2 ms, idle CPU 0%). Two budgets
are still missed: slow dock navigation had 1.5% janky frames (2 of 120), and idle RAM is about
105 MB PSS, of which about 49 MB is GPU memory for the wallpaper and glass layers. Cold start
has not been measured yet. See [docs/testing.md](docs/testing.md) for the full test session.

## Contributing

Issues and pull requests are welcome.

- Keep files small and focused: components under 400 lines, other modules under 500, functions under 80.
- Pure logic gets JVM unit tests (JUnit 4). Run `./gradlew :app:testDebugUnitTest`, and for the
  scripts: `tools/test_scripts.sh`, `python3 tools/test_check_layout.py`,
  `python3 tools/test_summarize_report.py`.
- Everything must work with the D-pad alone; motion uses springs from `Motion.kt`.
- Do not commit secrets, device serials or personal IP addresses; scripts take the device from
  `DEVICE` or `-d`.

## Feedback

Bugs, ideas and device reports (model, Android version, what looked wrong) go in
[Issues](../../issues). A screenshot from `tools/screenshots.sh` helps for layout problems.

## License

[MIT](LICENSE). Copyright (c) 2026 Kevin Suriel.
