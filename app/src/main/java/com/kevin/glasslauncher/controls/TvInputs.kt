package com.kevin.glasslauncher.controls

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager

/** A pass-through (HDMI and similar) input the TV can switch to. */
data class PassthroughInput(val id: String, val label: String)

/** Pass-through inputs from TvInputManager; empty when the device has none or no TV framework. */
fun passthroughInputs(context: Context): List<PassthroughInput> {
    val manager = context.getSystemService(TvInputManager::class.java) ?: return emptyList()
    return manager.tvInputList
        .filter { it.isPassthroughInput }
        .map { PassthroughInput(it.id, it.loadLabel(context).toString()) }
}

/** Opens the pass-through channel of [input] in the system TV app. */
fun inputIntent(input: PassthroughInput): Intent =
    Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(input.id))
