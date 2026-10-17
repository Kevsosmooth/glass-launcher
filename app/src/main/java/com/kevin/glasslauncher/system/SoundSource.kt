package com.kevin.glasslauncher.system

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.provider.Settings

data class AudioOutput(val name: String, val type: String, val formats: List<String>, val channels: String?)

/** [onTv]: HDMI-CEC hands volume to the TV, so steps go to the TV and the level here means nothing. */
data class SoundState(
    val volume: Int,
    val max: Int,
    val muted: Boolean,
    val fixed: Boolean,
    val outputs: List<AudioOutput>,
    val onTv: Boolean = false,
) {
    val percent: Int get() = volumePercent(volume, max)
}

/** A readable name for an output type; null for types the page leaves out (telephony, buses). */
fun outputTypeName(type: Int): String? = when (type) {
    AudioDeviceInfo.TYPE_HDMI -> "HDMI"
    AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
    TYPE_HDMI_EARC -> "HDMI eARC"
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
    AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB"
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Headphones"
    AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Line out"
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> SPEAKER
    else -> null
}

private const val SPEAKER = "TV speaker"

/** Settings.Global keys behind HDMI-CEC volume control (hidden constants). */
private const val CEC_ENABLED = "hdmi_control_enabled"
private const val CEC_VOLUME = "hdmi_control_volume_control_enabled"

/** AudioDeviceInfo.TYPE_HDMI_EARC, added in API 31. */
private const val TYPE_HDMI_EARC = 29

/**
 * Media volume and audio outputs. Volume and mute are ours to change; the output device, surround
 * mode and passthrough are system settings. On TVs with fixed volume (the TV or receiver owns it)
 * the slider is read-only. With HDMI-CEC volume control on (most TV boxes) Android forwards volume
 * steps to the TV itself.
 */
class SoundSource(context: Context) {
    private val audio = context.applicationContext.getSystemService(AudioManager::class.java)
    private val resolver = context.applicationContext.contentResolver

    fun state(): SoundState {
        val am = audio ?: return SoundState(0, 0, muted = false, fixed = true, outputs = emptyList())
        val outputs = outputs(am)
        return SoundState(
            volume = am.getStreamVolume(AudioManager.STREAM_MUSIC),
            max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            muted = am.isStreamMute(AudioManager.STREAM_MUSIC),
            fixed = am.isVolumeFixed,
            outputs = outputs,
            onTv = outputs.any { it.type.startsWith("HDMI") } && cecVolume(),
        )
    }

    /** One volume step up (+1) or down (-1). Goes to the TV over HDMI-CEC when that's on. */
    fun step(direction: Int) {
        audio?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (direction > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            0,
        )
    }

    /** Mute or unmute whatever owns the volume; with HDMI-CEC that is the TV. */
    fun toggleMute() {
        audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0)
    }

    private fun cecVolume(): Boolean = globalFlag(CEC_ENABLED) && globalFlag(CEC_VOLUME)

    /** Newer Android can refuse reads of these settings; then we treat CEC volume as off. */
    private fun globalFlag(name: String): Boolean = try {
        Settings.Global.getInt(resolver, name, 0) == 1
    } catch (_: SecurityException) {
        false
    }


    private fun outputs(am: AudioManager): List<AudioOutput> =
        am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { device ->
            val type = outputTypeName(device.type) ?: return@mapNotNull null
            val name = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: type
            AudioOutput(name, type, audioFormatNames(device.encodings), channelLayout(device.channelCounts))
        }.distinctBy { it.name to it.type }.let(::withoutPhantomSpeaker)
}

/** TV boxes report a built-in speaker they don't have; hide it whenever HDMI is there too. */
fun withoutPhantomSpeaker(outputs: List<AudioOutput>): List<AudioOutput> =
    if (outputs.any { it.type.startsWith("HDMI") }) outputs.filterNot { it.type == SPEAKER } else outputs
