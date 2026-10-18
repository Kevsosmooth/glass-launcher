package com.kevin.glasslauncher.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.kevin.glasslauncher.BuildConfig
import com.kevin.glasslauncher.controls.tryStart
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

/** GitHub's newest published (non-draft, non-prerelease) Glass release. */
const val LATEST_RELEASE_URL = "https://api.github.com/repos/Kevsosmooth/glass-launcher/releases/latest"

/** Every Glass APK URL starts here; a release pointing anywhere else is refused ([isReleaseApkUrl]). */
const val RELEASE_DOWNLOAD_PREFIX = "https://github.com/Kevsosmooth/glass-launcher/releases/download/"

/** How often [UpdateController.checkIfDue] asks GitHub. */
const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

private const val API_ACCEPT = "application/vnd.github+json"
private const val UPDATES_DIR = "updates"
private const val MAX_JSON_BYTES = 1024 * 1024

/** What the controller needs from the outside; tests swap the network, clock, version, threads and installer. */
class UpdateEnv(
    val http: UpdateHttp = UpdateHttp("GlassLauncher/${BuildConfig.VERSION_NAME}"),
    val now: () -> Long = System::currentTimeMillis,
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val io: CoroutineDispatcher = Dispatchers.IO,
    val installer: (Context) -> ApkInstaller = ::SystemApkInstaller,
)

/** True when the last check is a day old, never happened, or the clock went backwards since. */
fun isCheckDue(lastCheck: Long, now: Long): Boolean = now - lastCheck !in 0 until CHECK_INTERVAL_MS

/**
 * One-click updates from GitHub releases: a daily check, then download, verify (size, SHA-256,
 * package, version, signing certificate) and Android's install confirmation. All network and disk
 * work runs on [UpdateEnv.io]; the public functions are for the main thread.
 */
class UpdateController(context: Context, private val scope: CoroutineScope, private val env: UpdateEnv = UpdateEnv()) {
    private val app = context.applicationContext
    private val store = UpdateStore(app)
    private val apk = File(File(app.cacheDir, UPDATES_DIR), APK_ASSET_NAME)
    private val installer = env.installer(app)
    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private var job: Job? = null

    @Volatile
    private var pending: PendingInstall? = null

    val state: StateFlow<UpdateState> = mutableState.asStateFlow()

    // A file left by an interrupted update is never reused; the saved release brings the card back.
    private val ready: Job = scope.launch(env.io) {
        apk.parentFile?.listFiles()?.forEach { it.delete() }
        mutableState.value = restore()
    }

    /**
     * Glass came to the front (onStart). Asks GitHub at most once per [CHECK_INTERVAL_MS], quietly (no
     * Checking, no Failed). Also resumes an update the user just allowed, and brings back Android's
     * install confirmation (or gives up on it after [INSTALL_TIMEOUT_MS]) while Installing.
     */
    fun checkIfDue() {
        when (val current = state.value) {
            is UpdateState.Installing -> resumeInstall(current.release)
            is UpdateState.NeedsPermission -> if (canInstall()) startUpdate()
            else -> launchExclusive { if (isCheckDue(store.lastCheck, env.now())) checkGitHub(quiet = true) }
        }
    }

    /** Asks GitHub now (Settings > Check for updates), showing Checking and any failure. */
    fun checkNow() {
        if (!state.value.isBusy) launchExclusive { checkGitHub(quiet = false) }
    }

    /** Downloads, verifies and installs the offered release; NeedsPermission first if Android has not allowed installs. */
    fun startUpdate() {
        val release = state.value.updatableRelease() ?: return
        if (!canInstall()) {
            mutableState.value = UpdateState.NeedsPermission(release)
            return
        }
        launchExclusive { update(release) }
    }

    /**
     * The card's Update button: [startUpdate], and if Android has not allowed installs yet, open its
     * Install unknown apps screen (the update resumes on the next [checkIfDue] once allowed). False
     * when this TV lacks that screen: show `R.string.update_no_permission_screen`.
     */
    fun requestUpdate(): Boolean {
        startUpdate()
        if (state.value !is UpdateState.NeedsPermission) return true
        return app.tryStart(permissionIntent())
    }

    /** Later: hide the card until a newer version shows up. Settings still offers this one. */
    fun dismiss() {
        val release = state.value.updatableRelease() ?: return
        store.dismissedVersion = release.version
        mutableState.value = UpdateState.Available(release, dismissed = true)
    }

    /** Android's Install unknown apps screen for Glass; start it with `tryStart` (some TVs lack it). */
    fun permissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))

    /** A final PackageInstaller status from [UpdateReceiver]; ignored unless an install is pending. */
    internal fun onInstallResult(status: Int) {
        val release = (state.value as? UpdateState.Installing)?.release ?: return
        pending = null
        discardApk(abandonSession = false)
        val reason = installFailure(status)
        mutableState.value = if (reason == null) UpdateState.UpToDate(env.now()) else UpdateState.Failed(reason, release)
    }

    /**
     * Android wants the user to confirm the install ([UpdateReceiver]). The screen is kept so
     * [checkIfDue] can show it again: Android may block it while Glass is in the background. A
     * missing screen, or one that is not a system app's, abandons the install.
     */
    internal fun onConfirmNeeded(confirm: Intent?) {
        val release = (state.value as? UpdateState.Installing)?.release ?: return
        val safe = confirm?.let { systemConfirmIntent(app.packageManager, it) }
        if (safe == null) {
            giveUpInstall(release)
            return
        }
        pending = (pending ?: PendingInstall(env.now())).copy(confirm = safe)
        if (!app.tryStart(safe)) giveUpInstall(release)
    }

    private fun resumeInstall(release: ReleaseInfo) {
        when (val resume = installResume(pending, env.now())) {
            InstallResume.Wait -> Unit
            is InstallResume.Confirm -> if (!app.tryStart(resume.intent)) giveUpInstall(release)
            InstallResume.GiveUp -> giveUpInstall(release)
        }
    }

    /** Abandons the session (its late "aborted" answer is ignored) so the card offers Try again. */
    private fun giveUpInstall(release: ReleaseInfo) {
        pending = null
        mutableState.value = UpdateState.Failed(FailReason.InstallFailed, release)
        discardApk(abandonSession = true)
    }

    /** Runs as the current job so a quick Try again cannot download while the old file is deleted. */
    private fun discardApk(abandonSession: Boolean) {
        job = scope.launch(env.io) {
            if (abandonSession) installer.abandon()
            apk.delete()
        }
    }

    private fun canInstall(): Boolean = app.packageManager.canRequestPackageInstalls()

    private fun launchExclusive(block: () -> Unit) {
        if (job?.isActive == true) return
        job = scope.launch(env.io) {
            ready.join()
            block()
        }
    }

    private fun checkGitHub(quiet: Boolean) {
        if (!quiet) mutableState.value = UpdateState.Checking
        mutableState.value = try {
            val release = env.http.get(LATEST_RELEASE_URL, API_ACCEPT) { parseRelease(readJson(it)) }
                ?: throw UpdateException(FailReason.BadRelease)
            store.lastCheck = env.now()
            if (isNewer(release.version, env.currentVersion)) {
                store.release = release
                UpdateState.Available(release, dismissed = store.dismissedVersion == release.version)
            } else {
                store.release = null
                UpdateState.UpToDate(env.now())
            }
        } catch (e: UpdateException) {
            // Offline (often right after the TV boots): try again on the next return Home.
            if (e.reason != FailReason.Network) store.lastCheck = env.now()
            if (quiet) restore() else UpdateState.Failed(e.reason, null)
        }
    }

    private fun update(release: ReleaseInfo) {
        mutableState.value = UpdateState.Downloading(release, 0f)
        try {
            downloadApk(env.http, release, apk) { mutableState.value = UpdateState.Downloading(release, it) }
            mutableState.value = UpdateState.Verifying(release)
            installer.problem(apk)?.let { throw UpdateException(it) }
            pending = PendingInstall(env.now())
            mutableState.value = UpdateState.Installing(release)
            installer.install(apk)
        } catch (e: UpdateException) {
            pending = null
            apk.delete()
            mutableState.value = UpdateState.Failed(e.reason, release)
        }
    }

    /** The saved newer release (card after a restart), or Idle; forgets one this Glass already has. */
    private fun restore(): UpdateState {
        val saved = store.release ?: return UpdateState.Idle
        if (!isNewer(saved.version, env.currentVersion)) {
            store.release = null
            return UpdateState.Idle
        }
        return UpdateState.Available(saved, dismissed = store.dismissedVersion == saved.version)
    }
}

/** The release JSON as text; a body past [MAX_JSON_BYTES] (it is about 10 KB) is not a release. */
private fun readJson(body: Body): String {
    if (body.contentLength > MAX_JSON_BYTES) throw UpdateException(FailReason.BadRelease)
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    while (true) {
        val n = body.stream.read(buffer)
        if (n < 0) break
        out.write(buffer, 0, n)
        if (out.size() > MAX_JSON_BYTES) throw UpdateException(FailReason.BadRelease)
    }
    return out.toString(Charsets.UTF_8.name())
}

/** The release Update or Later acts on: offered, waiting for permission, or after a failed try. */
private fun UpdateState.updatableRelease(): ReleaseInfo? = when (this) {
    is UpdateState.Available -> release
    is UpdateState.NeedsPermission -> release
    is UpdateState.Failed -> release
    else -> null
}
