package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.TelemetryUiState
import com.example.ui.components.BatteryRingGauge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.TelemetryKeyValueRow
import com.example.ui.components.UsageMeterCard
import com.example.ui.components.formatBytesGb
import com.example.ui.theme.HeroOverlayBottom
import com.example.ui.theme.HeroOverlayTop
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusHealthy
import com.example.ui.theme.StatusWarning
import java.util.Locale

@Composable
fun OverviewScreen(
    state: TelemetryUiState.Success,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val identity = state.identity
    val battery = state.battery
    val memStorage = state.memoryStorage
    val network = state.network
    val sys = state.systemStatus

    val batteryAccent = when {
        battery.isCharging -> StatusHealthy
        battery.levelPct <= 20 -> StatusCritical
        battery.levelPct <= 40 -> StatusWarning
        else -> MaterialTheme.colorScheme.primary
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_device_hero),
                        contentDescription = "PowerForge hardware telemetry banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(HeroOverlayTop, HeroOverlayBottom)
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            label = if (sys.isDebuggableBuild) "TEST / DEBUG APK" else "RELEASE APK",
                            dotColor = if (sys.isDebuggableBuild) StatusWarning else StatusHealthy,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                        FilledTonalIconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("refresh_telemetry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.action_refresh)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${identity.brand} ${identity.model}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Android ${identity.androidVersion} (API ${identity.sdkInt}) • ${identity.socModel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.86f)
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.section_battery_power),
                icon = if (battery.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                trailingBadge = battery.statusLabel
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("battery_telemetry_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BatteryRingGauge(
                        percentage = battery.levelPct,
                        accentColor = batteryAccent
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TelemetryKeyValueRow("Source", battery.plugSource)
                        TelemetryKeyValueRow(
                            "Temperature",
                            String.format(Locale.US, "%.1f °C", battery.temperatureC),
                            monospaceValue = true
                        )
                        TelemetryKeyValueRow(
                            "Voltage",
                            "${battery.voltageMv} mV",
                            monospaceValue = true
                        )
                        battery.currentMa?.let { ma ->
                            TelemetryKeyValueRow("Current", "$ma mA", monospaceValue = true)
                        }
                        TelemetryKeyValueRow("Health / Tech", "${battery.healthLabel} • ${battery.technology}")
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.section_memory_storage),
                icon = Icons.Filled.Memory
            )
            Spacer(modifier = Modifier.height(8.dp))
            val usedRam = (memStorage.totalRamBytes - memStorage.availRamBytes).coerceAtLeast(0L)
            UsageMeterCard(
                title = "System RAM",
                icon = Icons.Filled.Memory,
                usedBytes = usedRam,
                totalBytes = memStorage.totalRamBytes,
                accentColor = MaterialTheme.colorScheme.primary,
                subtitle = if (memStorage.lowMemory) "Low Memory Pressure" else "${formatBytesGb(memStorage.availRamBytes)} free"
            )
            Spacer(modifier = Modifier.height(10.dp))
            val usedStorage = (memStorage.totalStorageBytes - memStorage.availStorageBytes).coerceAtLeast(0L)
            UsageMeterCard(
                title = "Internal Storage",
                icon = Icons.Filled.SdStorage,
                usedBytes = usedStorage,
                totalBytes = memStorage.totalStorageBytes,
                accentColor = MaterialTheme.colorScheme.secondary,
                subtitle = "${formatBytesGb(memStorage.availStorageBytes)} available"
            )
        }

        item {
            SectionHeader(
                title = stringResource(R.string.section_network),
                icon = if (network.isConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                trailingBadge = if (network.isConnected) network.transportLabel else "Offline"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TelemetryKeyValueRow(
                        label = "Transport",
                        value = network.transportLabel
                    )
                    TelemetryKeyValueRow(
                        label = "Internet Validated",
                        value = if (network.isValidated) "Yes (Reachable)" else "No"
                    )
                    TelemetryKeyValueRow(
                        label = "Metered Connection",
                        value = if (network.isMetered) "Metered" else "Unmetered"
                    )
                    if (network.downstreamKbps > 0) {
                        TelemetryKeyValueRow(
                            label = "Link Bandwidth",
                            value = "${network.downstreamKbps / 1000} Mbps down / ${network.upstreamKbps / 1000} Mbps up",
                            monospaceValue = true
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.section_device_identity),
                icon = Icons.Filled.PhoneAndroid
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TelemetryKeyValueRow("Manufacturer", identity.manufacturer)
                    TelemetryKeyValueRow("Model / Code", "${identity.model} (${identity.deviceCode})")
                    TelemetryKeyValueRow("SoC / Hardware", "${identity.socModel} / ${identity.hardware}")
                    TelemetryKeyValueRow("Security Patch", identity.securityPatch, monospaceValue = true)
                    TelemetryKeyValueRow("Supported ABIs", identity.abis, monospaceValue = true)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                    TelemetryKeyValueRow("Package ID", identity.packageName, monospaceValue = true)
                    TelemetryKeyValueRow(
                        "App Version",
                        "${identity.versionName} (Build ${identity.versionCode})",
                        monospaceValue = true
                    )
                }
            }
        }
    }
}
