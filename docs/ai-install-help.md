# Get help from an AI assistant

Stuck on a step? Copy everything in the box below into ChatGPT, Claude, Gemini or any AI
assistant, then add what your TV is and where you got stuck. It has every fact the assistant
needs, so it can walk you through one step at a time. The same text works for AI agents that
read this repo.

```text
Help me install Glass Launcher on my TV, one step at a time. Ask me which TV I have and what I
see on screen before each step. Use only the facts below; if something on my screen does not
match, ask me to describe it instead of guessing.

ABOUT GLASS LAUNCHER
- Free, open-source (MIT) home screen for Android TV, Google TV and Fire TV, styled like Apple TV.
- Source and releases: https://github.com/Kevsosmooth/glass-launcher
- Package name: com.kevin.glasslauncher. Needs Android 9 or newer.
- Installing it does NOT make it the home screen by itself. That is a separate last step.
- Updating: install the new version the same way; settings are kept.

STEP 1: INSTALL THE DOWNLOADER APP (by AFTVnews)
- Android TV / Google TV / NVIDIA Shield: search "Downloader" in the Google Play Store.
- Fire TV: search "Downloader" in the Appstore (or say "Downloader" to Alexa) and choose Get.

STEP 2: ALLOW DOWNLOADER TO INSTALL APPS
- Shield / Android TV: Settings > Device Preferences > Security & restrictions > Unknown sources >
  turn on Downloader.
- Google TV: Settings > Apps > Security & restrictions > Unknown sources > turn on Downloader.
  If that switch is grayed out, first turn on Developer options: Settings > System > About,
  press "Android TV OS build" 7 times. On newer Google TV software the switch may not be in
  the menu at all: Android asks when Downloader first installs something; choose Settings there
  and turn on "Allow from this source".
- Fire TV: Settings > My Fire TV > Developer options > Install unknown apps > turn on Downloader.
  If Developer options is missing: Settings > My Fire TV > About, press Select on the device name
  7 times, then go back.

STEP 3: DOWNLOAD AND INSTALL
- Open Downloader. Press the # button at the top (or select the address bar), type the code
  9798602 and choose Load. A page counts down about 6 seconds, then the download starts.
- Choose Install (or "Install update" if Glass is already there), then Done. Deleting the
  downloaded file afterwards is safe.
- Glass now appears in the app list, but the Home button still opens the TV's own home screen.
  The first time Glass opens it offers "Show me how" for the next step.

STEP 4: MAKE GLASS THE HOME SCREEN
- NVIDIA Shield, Google TV, Android TV (no computer): open Glass, press the Menu key (or select
  the clock at the top right) to open Quick Controls, then Launcher settings > Home button helper
  > Open Accessibility settings. If Settings opens on its main page, go to Device Preferences >
  Accessibility. Under Services, select "Glass Launcher Home button" and turn it on, then accept
  Android's warning. The Home button now opens Glass. Turn it off there to undo.
- Some Android TV boxes instead ask "Select a Home app" the first time Home is pressed after
  installing: pick Glass Launcher and choose Always. Then nothing else is needed.
- Fire TV: Fire OS hides the helper in its Accessibility menu, so it needs a computer with adb
  once (see "With a computer" below).

WITH A COMPUTER (adb; optional)
- On the TV turn on debugging: Settings > Device Preferences > About, press Build 7 times, then
  Developer options > Network debugging (Google TV: Settings > System > About, press "Android TV
  OS build" 7 times; Fire TV: Developer options > ADB debugging). Note the
  TV's IP address from its network settings. Then on the computer:
    adb connect <tv-ip>:5555   (accept "Allow debugging" on the TV)
- Fully native home screen (no helper needed): switch the stock launcher off. Afterwards the
  Home button opens Glass, or Android asks which Home app to use: pick Glass, Always.
    Shield / Android TV:  adb shell pm disable-user --user 0 com.google.android.tvlauncher
    Google TV:            adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
  Undo: the same line with "pm enable <package>" instead. If the TV ever has no home screen,
  run the undo line.
- Fire TV Home button helper on:
    adb shell settings put secure enabled_accessibility_services com.kevin.glasslauncher/com.kevin.glasslauncher.homebutton.HomeButtonService
    adb shell settings put secure accessibility_enabled 1
  Note: the first line replaces any other accessibility service turned on the same way. The
  repo's tools/home-helper.sh keeps them. Undo: turn the helper off, or run
    adb shell settings delete secure enabled_accessibility_services

TROUBLESHOOTING
- "App not installed": an older Glass signed with a different key is installed. Uninstall it
  first (settings are lost), then install again.
- Downloader shows a web page instead of downloading: check the code is 9798602 and try Load
  again.
- Home still opens the old screen: step 4 is not done, or the helper was turned off.
- Glass settings: press Menu on the remote, or select the clock, then Launcher settings.
```

Picture version of the same steps: [install guide](install-guide.md).
