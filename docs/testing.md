# Test session

Every script in `tools/`, in the order to run them. All of them take the TV from `DEVICE=<ip:port>`
(or `-d <ip:port>`) and adb from `ADB` (default: `adb` on PATH). Example device used below:
`DEVICE=192.168.1.50:5555`.

```
export DEVICE=192.168.1.50:5555      # your TV
export ADB=$HOME/Android/Sdk/platform-tools/adb   # only if adb is not on PATH
```

## Ground rules

- The TV must be idle and nobody watching: the scripts press D-pad keys and open Glass.
- Every script that presses keys first checks what is in front and stops if a video app is on
  screen, or if the TV is not on a launcher or Glass. A sleeping screen is woken. Set
  `GL_BUSY_PACKAGES` to the player and live-TV apps you use (the default covers YouTube, Netflix,
  VLC and Plex), `GL_ALLOW_ANY_TOP=1` to skip the "must be on Home" check. Per-machine settings
  (`DEVICE`, `ADB`, `GL_BUSY_PACKAGES`) can live in `tools/local.conf`, which is git-ignored.
- No script sends Select (OK) except the screenshots run, which opens menus inside Glass.
- Numbers only count for a **release** build with the Glass look and all motion on.

## 1. Offline checks (no TV needed)

```
python3 tools/test_check_layout.py        # layout checker against hand-written dumps
python3 tools/test_summarize_report.py    # measurement summary parsing and budgets
tools/test_scripts.sh                     # lib.sh, build/install, screenshots, measure, make-default with a fake adb
./gradlew :app:testDebugUnitTest          # app unit tests
```

All of them must pass before touching a TV.

## 2. Build and install

```
tools/build.sh debug        # out/app-debug.apk
tools/build.sh release      # out/app-release.apk
tools/install.sh debug      # adb install -r, prints versionName / versionCode
```

Build on another machine with `BUILD_HOST=<ssh-host>` (sources are rsynced to `BUILD_DIR`, default
`projects/glass-launcher` under that host's home). `BUILD_PREFIX` puts a command in front of
gradle, for example a memory cap or a lock when several builds share one machine. `bench`
builds the release app plus the macrobenchmark APK.

## 3. Baseline of another launcher (optional)

```
tools/baseline-launcher.sh out/baseline stock com.google.android.tvlauncher/.MainActivity
```

Records RAM, warm start, frame stats while navigating and idle CPU for the stock launcher, so
Glass has something to be compared with. Use the same script with Glass
(`com.kevin.glasslauncher/.MainActivity`) to get matching files.

## 4. Screenshots and layout check (debug build)

```
tools/screenshots.sh                    # shots/<timestamp>/
tools/screenshots.sh -d 192.168.1.60:5555 out/shots-fire
```

For each state it saves `NN-name.png` and the `uiautomator dump` `NN-name.xml`: home, dock tile 3,
grid row 2, edit menu, move mode, Settings, wallpaper picker, Quick Controls, Solid look, every
built-in wallpaper, 200 fake apps (plus scrolled), HUD on. Look, wallpaper, fake apps and HUD are
reset at the end. If the key path to a screen differs, override `EDIT_MOVE_KEYS`,
`EDIT_UNDO_KEYS` or `WALLPAPER_PICKER_KEYS`.

To watch the captures arrive from a phone or laptop, run the live gallery in another terminal and
open `http://<this machine's LAN IP>:8765/` (it refreshes every 3 seconds and serves only PNGs):

```
python3 tools/shots_server.py --dir shots --host 0.0.0.0     # default 127.0.0.1 = this machine only
```

After capturing, the script runs `tools/check_layout.py` on the plain home states. To rerun by hand:

```
python3 tools/check_layout.py shots/<run>/01-home.xml
```

It reads the bounds of the Compose test tags `dock`, `dock_tile`, `grid_tile`, `shelf_tile` and
`status_pill` and prints PASS/FAIL per rule: equal tile sizes, equal gaps, dock columns aligned
with grid columns, gap at least `width * (scale - 1) + 2 * margin`, scaled dock tiles inside the dock
slab, nothing off-screen, no overlaps. With Pillow it also writes `01-home.annotated.png`.
Options: `--scale 1.15 --margin-dp 6 --density auto`.

Look at the pictures, too: the checker proves geometry, not taste.

### Manual pass (D-pad only)

1. Home: focus travels dock to grid and back; Back/Home returns to the first dock tile.
2. Edit mode: long-press Select, move a tile with Left/Right, drop with Select; unpin and hide work.
3. Menu key opens and closes Quick Controls; Sleep timer and Inputs respond; Back closes it and
   focus returns to the tile it was opened from. (On the Shield, Menu sent by `input keyevent`
   never reaches apps; open the panel from the status pill or with `--es open controls`.)
4. Settings: change Look, wallpaper, dock size, clock; each takes effect without a restart.
5. Device pages (`--es open wifi|bluetooth|sound|display|storage|about` on debug builds): values
   match Android's own pages; on Sound, Left/Right on Volume changes the level (or the TV's
   volume when HDMI-CEC owns it) and focus stays on the row; each last row opens Android's page
   and Back returns to ours.
6. Text on glass stays readable on every wallpaper.

## 5. Performance (release build)

```
tools/build.sh release && tools/install.sh release
tools/measure.sh my-tv
```

Writes `reports/<date>-my-tv/`: gfxinfo and framestats per sweep (`dock-slow`, `dock-burst`,
`grid`), meminfo, idle CPU, and `summary.md` with each metric against its budget:

| Metric | Budget |
| --- | --- |
| Frame time p50 / p99 | < 8 ms / < 16.7 ms |
| Janky frames | < 1% |
| Idle RAM (PSS) | < 70 MB |
| Idle CPU | about 0% (under 1% passes) |

The script refuses debug builds (`ALLOW_DEBUG=1` overrides, numbers will not match). Exit status 1
means a budget was missed.

Measure twice after installing a new build. On the first run the GPU driver compiles and caches
the glass shaders, which can add about 60 MB of native heap on an NVIDIA Shield; later launches
load them from its cache. The second run is the one that counts.

To narrow down a memory or frame problem on a debug build, switch parts off with launch extras:
`--es look Solid` (no glass), `--ez backdrop false` (no app backdrop), `--ei fake_apps 200`
(placeholder apps instead of the installed ones), `--es only_packages <pkg,pkg>` (only these apps;
an empty value shows all again), `--ez reset_dock true` (dock back to its first-run default).

## 6. Macrobenchmarks

```
BUILD_HOST=<ssh-host> tools/bench.sh      # build host needs the SDK and adb
```

Runs `:benchmark:connectedBenchmarkReleaseAndroidTest` against the TV (startup and frame timing).
Without `BUILD_HOST` it runs in this checkout. Results land in `out/benchmark/`.

## 7. Make it the default Home

```
tools/make-default.sh            # dry run
tools/make-default.sh --apply    # needs Glass installed; disables the stock launcher(s)
tools/make-default.sh --undo     # re-enables them
```

Check that Home opens Glass, that apps launch and return to it, then try `--undo` once so you know
the way back works. On Fire TV the script only explains the limitation.

## Troubleshooting

- `no device`: set `DEVICE` or pass `-d`.
- `TV is in use` / `not on Home`: the safety check stopped the run; leave the video app or press Home.
- Layout check says `no dock_tile/grid_tile/shelf_tile nodes`: the dump is not of the home screen,
  or the build predates the test tags.
- `not a debug build`: `screenshots.sh` needs `tools/build.sh debug` and `tools/install.sh debug`.
