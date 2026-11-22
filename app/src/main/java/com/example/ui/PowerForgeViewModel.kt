package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.BatterySnapshot
import com.example.data.ChecklistPreferencesRepository
import com.example.data.DeviceIdentity
import com.example.data.DeviceTelemetryRepository
import com.example.data.MemoryStorageSnapshot
import com.example.data.NetworkSnapshot
import com.example.data.SystemInstallStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface TelemetryUiState {
    data object Loading : TelemetryUiState
    data class Success(
        val identity: DeviceIdentity,
        val battery: BatterySnapshot,
        val memoryStorage: MemoryStorageSnapshot,
        val network: NetworkSnapshot,
        val systemStatus: SystemInstallStatus,
        val completedStepIds: Set<String>,
        val useDynamicColor: Boolean,
        val lastRefreshedEpochMs: Long
    ) : TelemetryUiState
    data class Error(val message: String) : TelemetryUiState
}

class PowerForgeViewModel(application: Application) : AndroidViewModel(application) {

    private val telemetryRepo = DeviceTelemetryRepository(application)
    private val prefsRepo = ChecklistPreferencesRepository(application)

    private data class SnapshotBundle(
        val identity: DeviceIdentity,
        val memoryStorage: MemoryStorageSnapshot,
        val network: NetworkSnapshot,
        val systemStatus: SystemInstallStatus,
        val timestampMs: Long
    )

    private val snapshotBundle = MutableStateFlow<SnapshotBundle?>(null)
    private val loadError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TelemetryUiState> = combine(
        snapshotBundle,
        telemetryRepo.observeBattery(),
        prefsRepo.completedStepIds,
        prefsRepo.useDynamicColor,
        loadError
    ) { bundle, battery, completedSteps, dynamicColor, error ->
        when {
            error != null -> TelemetryUiState.Error(error)
            bundle == null -> TelemetryUiState.Loading
            else -> TelemetryUiState.Success(
                identity = bundle.identity,
                battery = battery,
                memoryStorage = bundle.memoryStorage,
                network = bundle.network,
                systemStatus = bundle.systemStatus,
                completedStepIds = completedSteps,
                useDynamicColor = dynamicColor,
                lastRefreshedEpochMs = bundle.timestampMs
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TelemetryUiState.Loading
    )

    init {
        refreshTelemetry()
    }

    fun refreshTelemetry() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    SnapshotBundle(
                        identity = telemetryRepo.getDeviceIdentity(),
                        memoryStorage = telemetryRepo.readMemoryAndStorage(),
                        network = telemetryRepo.readNetworkSnapshot(),
                        systemStatus = telemetryRepo.readSystemInstallStatus(),
                        timestampMs = System.currentTimeMillis()
                    )
                }
            }.onSuccess { bundle ->
                loadError.value = null
                snapshotBundle.value = bundle
            }.onFailure { throwable ->
                loadError.value = throwable.message ?: "Unable to read device telemetry."
            }
        }
    }

    fun toggleDeployStep(stepId: String) {
        viewModelScope.launch {
            prefsRepo.toggleStep(stepId)
        }
    }

    fun resetDeployChecklist() {
        viewModelScope.launch {
            prefsRepo.resetSteps()
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setUseDynamicColor(enabled)
        }
    }

    fun sendDiagnosticNotification(): Boolean {
        val ctx = getApplication<Application>()
        val channelId = "powerforge_diagnostics"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                ctx.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ctx.getString(R.string.notif_channel_desc)
            }
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
            return false
        }
        val notification = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(ctx.getString(R.string.notif_test_title))
            .setContentText(ctx.getString(R.string.notif_test_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        return runCatching {
            NotificationManagerCompat.from(ctx).notify(1042, notification)
            true
        }.getOrDefault(false)
    }
}
