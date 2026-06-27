package com.blockschedule.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockschedule.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the updater, shared by the Today banner and the Settings screen. */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val progress: Float) : UpdateUiState
    data object Installing : UpdateUiState
    data class NeedsPermission(val info: UpdateInfo) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = UpdatePrefs(app)

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private val _autoCheck = MutableStateFlow(prefs.autoCheck)
    val autoCheck: StateFlow<Boolean> = _autoCheck.asStateFlow()

    val currentVersionName: String = BuildConfig.VERSION_NAME
    val currentVersionCode: Int = BuildConfig.VERSION_CODE

    /** Called once per app start; only checks if the user left auto-update on. */
    fun checkOnLaunch() {
        if (_autoCheck.value && _state.value is UpdateUiState.Idle) checkNow(silentIfUpToDate = true)
    }

    fun setAutoCheck(enabled: Boolean) {
        prefs.autoCheck = enabled
        _autoCheck.value = enabled
    }

    fun checkNow(silentIfUpToDate: Boolean = false) {
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            val info = UpdateManager.fetchLatest()
            _state.value = when {
                info == null -> UpdateUiState.Error("Couldn't check for updates")
                UpdateManager.isNewer(info, currentVersionCode) -> UpdateUiState.Available(info)
                else -> if (silentIfUpToDate) UpdateUiState.Idle else UpdateUiState.UpToDate
            }
        }
    }

    /** Download + install the available update. Routes through permission if needed. */
    fun startUpdate() {
        val current = _state.value
        val info = (current as? UpdateUiState.Available)?.info
            ?: (current as? UpdateUiState.NeedsPermission)?.info
            ?: return

        if (!ApkInstaller.canInstall(getApplication())) {
            _state.value = UpdateUiState.NeedsPermission(info)
            ApkInstaller.openInstallPermissionSettings(getApplication())
            return
        }

        _state.value = UpdateUiState.Downloading(0f)
        viewModelScope.launch {
            runCatching {
                val apk = UpdateManager.downloadApk(getApplication(), info.apkUrl) { p ->
                    _state.value = UpdateUiState.Downloading(p.coerceAtLeast(0f))
                }
                _state.value = UpdateUiState.Installing
                ApkInstaller.install(getApplication(), apk)
            }.onFailure {
                _state.value = UpdateUiState.Error(it.message ?: "Update failed")
            }
        }
    }

    fun dismiss() { _state.value = UpdateUiState.Idle }
}
