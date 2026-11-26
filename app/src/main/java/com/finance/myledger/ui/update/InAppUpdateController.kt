package com.finance.myledger.ui.update

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.finance.myledger.domain.update.UpdateMode
import com.finance.myledger.domain.update.UpdatePolicy
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the UI should show about a pending update. */
enum class UpdateUiState { IDLE, DOWNLOADING, READY_TO_INSTALL }

/**
 * Over-the-air updates via Google Play In-App Updates — the only self-update mechanism Play
 * permits for apps it distributes.
 *
 * - Checks once when the activity is created; [UpdatePolicy] picks flexible vs immediate.
 * - Flexible: Play downloads in the background; [state] becomes READY_TO_INSTALL and the UI
 *   offers a restart ([completeUpdate]).
 * - On every resume, re-surfaces a downloaded-but-not-installed update and resumes an
 *   interrupted immediate update (Play requires both).
 * - If the user declines, it doesn't ask again until the next launch.
 *
 * Only works for installs from Play (including internal app sharing / internal testing).
 * Sideloaded or debug builds get an error from Play, which is ignored — the app just never
 * prompts.
 *
 * Must be created in [ComponentActivity.onCreate] (it registers an activity-result launcher).
 */
class InAppUpdateController(
    private val activity: ComponentActivity,
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
) : DefaultLifecycleObserver {

    private val _state = MutableStateFlow(UpdateUiState.IDLE)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var declinedThisSession = false

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                declinedThisSession = true
            }
        }

    private val installListener = InstallStateUpdatedListener { installState ->
        _state.value = when (installState.installStatus()) {
            InstallStatus.PENDING, InstallStatus.DOWNLOADING -> UpdateUiState.DOWNLOADING
            InstallStatus.DOWNLOADED -> UpdateUiState.READY_TO_INSTALL
            else -> UpdateUiState.IDLE
        }
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        manager.registerListener(installListener)
        manager.appUpdateInfo
            .addOnSuccessListener(::maybeStartUpdate)
            .addOnFailureListener { /* Not installed from Play, or Play unavailable — no updates. */ }
    }

    override fun onResume(owner: LifecycleOwner) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _state.value = UpdateUiState.READY_TO_INSTALL
            }
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                start(info, AppUpdateType.IMMEDIATE)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        manager.unregisterListener(installListener)
    }

    /** Restarts the app into the downloaded flexible update. */
    fun completeUpdate() {
        manager.completeUpdate()
    }

    private fun maybeStartUpdate(info: AppUpdateInfo) {
        if (declinedThisSession) return
        if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return
        val mode = UpdatePolicy.decide(
            priority = info.updatePriority(),
            stalenessDays = info.clientVersionStalenessDays(),
            immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
            flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        )
        when (mode) {
            UpdateMode.IMMEDIATE -> start(info, AppUpdateType.IMMEDIATE)
            UpdateMode.FLEXIBLE -> start(info, AppUpdateType.FLEXIBLE)
            UpdateMode.NONE -> Unit
        }
    }

    private fun start(info: AppUpdateInfo, @AppUpdateType type: Int) {
        manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
    }
}
