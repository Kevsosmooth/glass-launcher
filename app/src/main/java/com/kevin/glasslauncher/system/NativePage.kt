package com.kevin.glasslauncher.system

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.kevin.glasslauncher.controls.tryStart

/**
 * Android's own settings screens, for what an app is not allowed to change itself. Each lists the
 * actions to try in order: TV builds differ (the Shield has no ACTION_DISPLAY_SETTINGS or
 * ACTION_BLUETOOTH_SETTINGS, so Display opens "Display & Sound" and Bluetooth opens the Android TV
 * accessory pairing screen).
 */
enum class NativePage(private val actions: List<String>) {
    Wifi(listOf(Settings.ACTION_WIFI_SETTINGS)),
    Bluetooth(listOf(ACTION_TV_CONNECT_INPUT, Settings.ACTION_BLUETOOTH_SETTINGS)),
    Sound(listOf(Settings.ACTION_SOUND_SETTINGS, Settings.ACTION_DISPLAY_SETTINGS)),
    Display(listOf(Settings.ACTION_DISPLAY_SETTINGS, Settings.ACTION_SOUND_SETTINGS)),
    Storage(listOf(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)),
    About(listOf(Settings.ACTION_DEVICE_INFO_SETTINGS)),
    DateTime(listOf(Settings.ACTION_DATE_SETTINGS)),
    Language(listOf(Settings.ACTION_LOCALE_SETTINGS)),
    Location(listOf(Settings.ACTION_LOCATION_SOURCE_SETTINGS)),
    Update(listOf(ACTION_SYSTEM_UPDATE)),
    All(emptyList()),
    ;

    /** Opens the first screen this TV has, else the main Android Settings; false if nothing opened. */
    fun open(context: Context): Boolean =
        (actions + Settings.ACTION_SETTINGS).any { context.tryStart(Intent(it)) }
}

/** Android TV's "Pair accessory" screen (Settings > Remotes & Accessories > Add accessory). */
private const val ACTION_TV_CONNECT_INPUT = "com.google.android.intent.action.CONNECT_INPUT"

/** Not in the public SDK, but handled on Android TV builds (the Shield opens its OTA updater). */
private const val ACTION_SYSTEM_UPDATE = "android.settings.SYSTEM_UPDATE_SETTINGS"
