package com.kevin.glasslauncher.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address

enum class Connection { Wifi, Ethernet, Other, None }

/** The active network. Wi-Fi details are null on Ethernet, and [ssid] is null without location access. */
data class NetworkStatus(
    val connection: Connection,
    val wifiEnabled: Boolean,
    val ssid: String? = null,
    val rssi: Int? = null,
    val frequencyMhz: Int? = null,
    val linkMbps: Int? = null,
    val ip: String? = null,
)

data class NearbyNetwork(val ssid: String, val rssi: Int, val frequencyMhz: Int, val secure: Boolean)

/**
 * Nearby networks for the list: one entry per name (its strongest signal), hidden and already
 * connected networks left out, strongest first.
 */
fun nearbyNetworks(scan: List<NearbyNetwork>, connectedSsid: String?): List<NearbyNetwork> =
    scan.filter { it.ssid.isNotBlank() && it.ssid != connectedSsid }
        .groupBy { it.ssid }
        .map { (_, same) -> same.maxBy { it.rssi } }
        .sortedWith(compareByDescending<NearbyNetwork> { it.rssi }.thenBy { it.ssid.lowercase() })

/** Whether a scan result's capability string ("[WPA2-PSK-CCMP][ESS]") needs a password. */
fun isSecure(capabilities: String): Boolean =
    listOf("WEP", "PSK", "EAP", "SAE", "OWE").any { capabilities.contains(it) }

/**
 * Reads Wi-Fi and network state. Since Android 10 apps cannot turn Wi-Fi on or off or join a
 * network themselves, so those stay in Android Settings; names (ours and nearby) need location
 * permission and Location switched on.
 */
class WifiSource(context: Context) {
    private val app = context.applicationContext
    private val wifi = app.getSystemService(WifiManager::class.java)
    private val connectivity = app.getSystemService(ConnectivityManager::class.java)
    private val location = app.getSystemService(LocationManager::class.java)

    val hasLocationPermission: Boolean
        get() = app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val locationOn: Boolean get() = location?.isLocationEnabled == true

    fun status(): NetworkStatus {
        val enabled = wifi?.isWifiEnabled == true
        val network = connectivity?.activeNetwork ?: return NetworkStatus(Connection.None, enabled)
        val caps = connectivity.getNetworkCapabilities(network) ?: return NetworkStatus(Connection.None, enabled)
        val ip = connectivity.getLinkProperties(network)?.linkAddresses
            ?.map { it.address }?.firstOrNull { it is Inet4Address }?.hostAddress
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkStatus(Connection.Ethernet, enabled, ip = ip)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> wifiStatus(enabled, ip)
            else -> NetworkStatus(Connection.Other, enabled, ip = ip)
        }
    }

    // WifiManager.connectionInfo is the only way to read the current network without registering a
    // callback; deprecated in API 31 but still answered on every TV we support.
    private fun wifiStatus(enabled: Boolean, ip: String?): NetworkStatus {
        val info = wifi?.connectionInfo
        return NetworkStatus(
            Connection.Wifi,
            enabled,
            ssid = cleanSsid(info?.ssid),
            rssi = info?.rssi,
            frequencyMhz = info?.frequency?.takeIf { it > 0 },
            linkMbps = info?.linkSpeed?.takeIf { it > 0 },
            ip = ip,
        )
    }

    /** Asks for fresh results (Android throttles this; old results are still returned). */
    // startScan is deprecated for third-party apps but still works under the foreground throttle.
    fun requestScan() {
        if (hasLocationPermission) wifi?.startScan()
    }

    /** Latest scan results; empty without location permission or with Location off. */
    fun scanResults(): List<NearbyNetwork> {
        if (!hasLocationPermission) return emptyList()
        val results = try {
            wifi?.scanResults.orEmpty()
        } catch (e: SecurityException) {
            emptyList()
        }
        return results.map {
            // ScanResult.SSID is deprecated, but wifiSsid needs API 33.
            val name = cleanSsid(it.SSID).orEmpty()
            NearbyNetwork(name, it.level, it.frequency, isSecure(it.capabilities.orEmpty()))
        }
    }
}
