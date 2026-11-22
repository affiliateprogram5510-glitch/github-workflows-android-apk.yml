package com.example.data

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.security.MessageDigest
import kotlin.math.roundToInt

data class DeviceIdentity(
    val brand: String,
    val manufacturer: String,
    val model: String,
    val deviceCode: String,
    val hardware: String,
    val socModel: String,
    val androidVersion: String,
    val sdkInt: Int,
    val securityPatch: String,
    val abis: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long
)

data class BatterySnapshot(
    val levelPct: Int,
    val isCharging: Boolean,
    val plugSource: String,
    val statusLabel: String,
    val healthLabel: String,
    val temperatureC: Float,
    val voltageMv: Int,
    val technology: String,
    val currentMa: Int?,
    val chargeCounterMah: Int?
)

data class MemoryStorageSnapshot(
    val totalRamBytes: Long,
    val availRamBytes: Long,
    val lowMemory: Boolean,
    val totalStorageBytes: Long,
    val availStorageBytes: Long
)

data class NetworkSnapshot(
    val isConnected: Boolean,
    val transportLabel: String,
    val isValidated: Boolean,
    val isMetered: Boolean,
    val downstreamKbps: Int,
    val upstreamKbps: Int
)

data class SystemInstallStatus(
    val isIgnoringBatteryOptimizations: Boolean,
    val canRequestPackageInstalls: Boolean,
    val notificationsEnabled: Boolean,
    val isDebuggableBuild: Boolean,
    val signingCertSha256: String,
    val buildSignatureLabel: String
)

class DeviceTelemetryRepository(private val context: Context) {

    fun getDeviceIdentity(): DeviceIdentity {
        val pkgInfo = getPackageInfoCompat()
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo?.longVersionCode ?: 1L
        } else {
            @Suppress("DEPRECATION")
            (pkgInfo?.versionCode ?: 1).toLong()
        }
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN } ?: Build.HARDWARE
        } else {
            Build.HARDWARE
        }
        val patch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else {
            "N/A"
        }
        return DeviceIdentity(
            brand = Build.BRAND.replaceFirstChar { it.uppercase() },
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            deviceCode = Build.DEVICE,
            hardware = Build.HARDWARE,
            socModel = soc,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = patch,
            abis = Build.SUPPORTED_ABIS.joinToString(", "),
            packageName = context.packageName,
            versionName = pkgInfo?.versionName ?: "1.0",
            versionCode = versionCode
        )
    }

    fun observeBattery(): Flow<BatterySnapshot> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(readBatterySnapshot(intent))
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        trySend(readBatterySnapshot(initialIntent))
        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    fun readBatterySnapshot(intent: Intent? = null): BatterySnapshot {
        val batteryIntent = intent ?: context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt() else 0

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val statusLabel = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val plugSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC / Fast Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "On Battery"
        }

        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val healthLabel = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Degraded"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }

        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tech = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.ifBlank { "Li-ion" } ?: "Li-ion"

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val rawCurrentUa = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = if (rawCurrentUa != null && rawCurrentUa != Int.MIN_VALUE && rawCurrentUa != 0) {
            (rawCurrentUa / 1000f).roundToInt()
        } else {
            null
        }
        val rawCounterUah = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val chargeCounterMah = if (rawCounterUah != null && rawCounterUah > 0) {
            (rawCounterUah / 1000f).roundToInt()
        } else {
            null
        }

        return BatterySnapshot(
            levelPct = pct,
            isCharging = isCharging,
            plugSource = plugSource,
            statusLabel = statusLabel,
            healthLabel = healthLabel,
            temperatureC = tempTenths / 10f,
            voltageMv = voltageMv,
            technology = tech,
            currentMa = currentMa,
            chargeCounterMah = chargeCounterMah
        )
    }

    fun readMemoryAndStorage(): MemoryStorageSnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val dataDir = Environment.getDataDirectory()
        val statFs = StatFs(dataDir.path)
        val totalStorage = statFs.blockCountLong * statFs.blockSizeLong
        val availStorage = statFs.availableBlocksLong * statFs.blockSizeLong

        return MemoryStorageSnapshot(
            totalRamBytes = memInfo.totalMem,
            availRamBytes = memInfo.availMem,
            lowMemory = memInfo.lowMemory,
            totalStorageBytes = totalStorage,
            availStorageBytes = availStorage
        )
    }

    fun readNetworkSnapshot(): NetworkSnapshot {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return NetworkSnapshot(
            isConnected = false,
            transportLabel = "Offline",
            isValidated = false,
            isMetered = false,
            downstreamKbps = 0,
            upstreamKbps = 0
        )
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkSnapshot(
            isConnected = false,
            transportLabel = "No Capabilities",
            isValidated = false,
            isMetered = false,
            downstreamKbps = 0,
            upstreamKbps = 0
        )
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (4G/5G)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Tunnel"
            else -> "Other Network"
        }
        return NetworkSnapshot(
            isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            transportLabel = transport,
            isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            downstreamKbps = caps.linkDownstreamBandwidthKbps,
            upstreamKbps = caps.linkUpstreamBandwidthKbps
        )
    }

    fun readSystemInstallStatus(): SystemInstallStatus {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignoringBatteryOpt = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        val canInstallUnknown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val sha256 = readSigningCertSha256()

        val sigLabel = if (isDebuggable) {
            "Debug / Test APK (Debuggable)"
        } else {
            "Release Build (Signed)"
        }

        return SystemInstallStatus(
            isIgnoringBatteryOptimizations = ignoringBatteryOpt,
            canRequestPackageInstalls = canInstallUnknown,
            notificationsEnabled = notificationsEnabled,
            isDebuggableBuild = isDebuggable,
            signingCertSha256 = sha256,
            buildSignatureLabel = sigLabel
        )
    }

    private fun readSigningCertSha256(): String {
        return runCatching {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = info.signingInfo
                if (signingInfo == null) emptyArray()
                else if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
                else signingInfo.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                ).signatures ?: emptyArray()
            }
            val firstCert = signatures.firstOrNull()?.toByteArray() ?: return "Unavailable"
            val digest = MessageDigest.getInstance("SHA-256").digest(firstCert)
            digest.joinToString(":") { "%02X".format(it) }
        }.getOrElse { "Unavailable" }
    }

    private fun getPackageInfoCompat() = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    }.getOrNull()
}
