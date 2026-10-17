package com.kevin.glasslauncher.system

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.PageTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** A scrolling device page under [title]; its first focusable row should use focusOnShow. */
@Composable
fun ColumnScope.SystemPage(title: String, content: @Composable () -> Unit) {
    Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) {
        PageTitle(title)
        content()
    }
}

/** Space before the links to Android's own screens at the bottom of a page. */
@Composable
fun PageGap() {
    Spacer(Modifier.height(12.dp))
}

/** A row that opens one of Android's own settings screens. */
@Composable
fun NativeLinkRow(title: String, page: NativePage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    ListRow(title, null, { page.open(context) }, modifier, chevron = true)
}

/**
 * [read] on a background thread now and every [intervalMillis] while the app is in front (so it
 * stops while an Android settings screen is open, and catches up on return). A new [key] reads
 * again at once, e.g. after the page changed something. While not [active] it keeps the last value.
 */
@Composable
fun <T> rememberPolled(key: Any?, intervalMillis: Long, initial: T, active: Boolean = true, read: () -> T): State<T> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(initial, key, active, lifecycle) {
        if (!active) return@produceState
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = withContext(Dispatchers.IO) { read() }
                delay(intervalMillis)
            }
        }
    }
}

/** Returns a function that asks for [permission] (no-op when null); [onResult] gets the answer. */
@Composable
fun rememberPermissionRequest(permission: String?, onResult: (Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return { permission?.let(launcher::launch) }
}
