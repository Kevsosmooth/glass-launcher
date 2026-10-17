package com.kevin.glasslauncher.system

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

enum class DeviceKind { Remote, Gamepad, Keyboard, Audio, Other }

data class PairedDevice(val name: String, val address: String, val kind: DeviceKind, val connected: Boolean)

private const val MAJOR_MASK = 0x1F00
private const val MAJOR_AUDIO_VIDEO = 0x0400
private const val MAJOR_PERIPHERAL = 0x0500
private const val PERIPHERAL_KEYBOARD_BIT = 0x40
private const val PERIPHERAL_MINOR_MASK = 0x3C
private const val PERIPHERAL_JOYSTICK = 0x04
private const val PERIPHERAL_GAMEPAD = 0x08
private const val PERIPHERAL_REMOTE = 0x0C

/** What a paired device is, from its Bluetooth class of device and, failing that, its name. */
fun deviceKind(deviceClass: Int, name: String): DeviceKind {
    val lower = name.lowercase()
    val byClass = when (deviceClass and MAJOR_MASK) {
        MAJOR_AUDIO_VIDEO -> DeviceKind.Audio
        MAJOR_PERIPHERAL -> when {
            (deviceClass and PERIPHERAL_MINOR_MASK) == PERIPHERAL_REMOTE -> DeviceKind.Remote
            (deviceClass and PERIPHERAL_MINOR_MASK) in setOf(PERIPHERAL_JOYSTICK, PERIPHERAL_GAMEPAD) -> DeviceKind.Gamepad
            (deviceClass and PERIPHERAL_KEYBOARD_BIT) != 0 -> DeviceKind.Keyboard
            else -> null
        }
        else -> null
    }
    return byClass ?: when {
        "remote" in lower -> DeviceKind.Remote
        listOf("controller", "gamepad", "xbox", "dualsense", "dualshock").any { it in lower } -> DeviceKind.Gamepad
        "keyboard" in lower -> DeviceKind.Keyboard
        listOf("buds", "headphone", "speaker", "soundbar", "airpods").any { it in lower } -> DeviceKind.Audio
        else -> DeviceKind.Other
    }
}

/** Connected first, then by name. */
fun sortDevices(devices: List<PairedDevice>): List<PairedDevice> =
    devices.sortedWith(compareByDescending<PairedDevice> { it.connected }.thenBy { it.name.lowercase() })

/**
 * Paired Bluetooth devices. Pairing, unpairing and turning Bluetooth off are system-only, so those
 * open Android's accessory screen. "Connected" covers Bluetooth LE devices (remotes, most
 * controllers) and audio devices Android is playing to.
 */
class BluetoothSource(context: Context) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(BluetoothManager::class.java)
    private val audio = app.getSystemService(AudioManager::class.java)

    /** Android 12+ asks the user; older versions grant BLUETOOTH at install. */
    val permission: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null

    val hasPermission: Boolean
        get() = permission == null || app.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    val available: Boolean get() = manager?.adapter != null

    val enabled: Boolean get() = hasPermission && manager?.adapter?.isEnabled == true

    fun paired(): List<PairedDevice> {
        val bluetooth = manager ?: return emptyList()
        val adapter = bluetooth.adapter ?: return emptyList()
        if (!hasPermission) return emptyList()
        return try {
            val connected = bluetooth.getConnectedDevices(BluetoothProfile.GATT).mapTo(HashSet()) { it.address }
            val playing = audioOutputNames()
            sortDevices(
                adapter.bondedDevices.orEmpty().map { device ->
                    val name = device.name ?: device.address
                    val kind = deviceKind(device.bluetoothClass?.deviceClass ?: 0, name)
                    PairedDevice(name, device.address, kind, device.address in connected || name in playing)
                },
            )
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    private fun audioOutputNames(): Set<String> =
        audio?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
            .filter { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            .mapTo(HashSet()) { it.productName.toString() }
}
