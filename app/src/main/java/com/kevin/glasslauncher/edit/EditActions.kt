package com.kevin.glasslauncher.edit

import android.content.Context
import android.widget.Toast
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.data.LauncherPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Carries out edit-mode choices. Dock and hidden changes are saved on the app-wide [scope], so
 * they finish even if home leaves the screen; system pages open from [context].
 */
class EditActions(
    private val editor: HomeEditor,
    private val prefs: LauncherPrefs,
    private val scope: CoroutineScope,
    private val context: Context,
) {
    /**
     * Runs [action] for [target] against the shown [dock] (keys, in order). Returns false when the
     * tile leaves its row (Remove from dock, Hide), so the caller must put focus somewhere else.
     */
    fun pick(action: MenuAction, target: MenuTarget, dock: List<String>, dockSize: Int): Boolean {
        val app = target.app
        when (action) {
            MenuAction.Move -> editor.startMove(app.key, dock)
            MenuAction.RemoveFromDock -> saveDock(removeFromDock(dock, app.key))
            MenuAction.AddToDock -> saveDock(addToDock(dock, app.key, dockSize))
            MenuAction.Hide -> scope.launch { prefs.setHidden(prefs.prefs.first().hidden + app.key) }
            MenuAction.AppInfo -> openAppInfo(context, app.packageName).orComplain()
            MenuAction.Uninstall -> requestUninstall(context, app.packageName).orComplain()
        }
        return action != MenuAction.RemoveFromDock && action != MenuAction.Hide
    }

    fun saveDock(keys: List<String>) {
        scope.launch { prefs.setFavorites(keys) }
    }

    private fun Boolean.orComplain() {
        if (!this) Toast.makeText(context, R.string.home_no_system_page, Toast.LENGTH_SHORT).show()
    }
}
