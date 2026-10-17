package com.kevin.glasslauncher.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

/** Install / uninstall / update of any app: drop its cached art and reload the list. */
class PackageChangeReceiver(
    private val apps: AppRepository,
    private val banners: BannerCache,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        intent.data?.schemeSpecificPart?.let(banners::evict)
        apps.refresh()
    }

    companion object {
        fun register(context: Context, apps: AppRepository, banners: BannerCache) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            val receiver = PackageChangeReceiver(apps, banners)
            // System broadcasts still arrive with NOT_EXPORTED; only other apps are shut out.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        }
    }
}
