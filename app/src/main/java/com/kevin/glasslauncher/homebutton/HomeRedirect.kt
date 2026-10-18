package com.kevin.glasslauncher.homebutton

import android.view.KeyEvent

private fun isHomeNamed(simpleName: String) = simpleName.startsWith("Home") && "Activity" in simpleName

/**
 * Stock home screens the Home button helper covers up, each with a test for its home activity's
 * simple name: Fire TV's (ui.HomeActivity_vNext), Google TV's, and the Shield / Android TV one
 * (MainActivity; the Shield does not hand the Home key to accessibility services). Keep the
 * packages in step with android:packageNames in res/xml/home_button_service.xml, which limits
 * the events the service receives to these apps.
 */
private val STOCK_HOMES: Map<String, (String) -> Boolean> = mapOf(
    "com.amazon.tv.launcher" to ::isHomeNamed,
    "com.google.android.apps.tv.launcherx" to ::isHomeNamed,
    "com.google.android.tvlauncher" to { it == "MainActivity" },
)

val STOCK_HOME_PACKAGES: Set<String> = STOCK_HOMES.keys

/**
 * True when a window change means a stock home screen just came up. The launchers' other
 * screens, such as Fire TV's app library and search, stay reachable.
 */
fun isStockHome(packageName: CharSequence?, className: CharSequence?): Boolean {
    val isHome = STOCK_HOMES[packageName?.toString()] ?: return false
    val simpleName = className?.toString()?.substringAfterLast('.') ?: return false
    return isHome(simpleName)
}

/** What the helper does with a key the system lets it see. */
enum class KeyVerdict { PassThrough, Swallow, OpenGlass }

/**
 * Home is taken whole, so the stock screen never sees the press: the down event is swallowed and
 * the up event opens Glass. Every other key passes through untouched.
 */
fun homeKeyVerdict(keyCode: Int, action: Int): KeyVerdict = when {
    keyCode != KeyEvent.KEYCODE_HOME -> KeyVerdict.PassThrough
    action == KeyEvent.ACTION_UP -> KeyVerdict.OpenGlass
    else -> KeyVerdict.Swallow
}

/**
 * True when [component] (package/full.ClassName) is in Android's colon-separated list of enabled
 * accessibility services, where entries may use the short form package/.ClassName.
 */
fun isServiceEnabled(enabledServices: String?, component: String): Boolean =
    enabledServices.orEmpty().split(':').any { entry ->
        val pkg = entry.substringBefore('/', missingDelimiterValue = "")
        val cls = entry.substringAfter('/', missingDelimiterValue = "")
        pkg.isNotEmpty() && "$pkg/${if (cls.startsWith('.')) pkg + cls else cls}" == component
    }
