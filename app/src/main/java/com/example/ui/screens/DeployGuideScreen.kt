package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.SectionHeader

private data class DeployStep(
    val id: String,
    val title: String,
    val summary: String,
    val detail: String
)

private data class TroubleshootItem(
    val id: String,
    val symptom: String,
    val resolution: String
)

private val mobileDeploySteps = listOf(
    DeployStep(
        id = "push_github",
        title = "1. Push Project to GitHub from Chrome",
        summary = "No laptop needed — use AI Studio Export / Push to GitHub in mobile Chrome.",
        detail = "Open AI Studio in Chrome on your Realme P4 Power, tap Settings -> Push to GitHub, and authorize your free GitHub account. The bundled .github/workflows/android-apk.yml runs automatically in the cloud."
    ),
    DeployStep(
        id = "download_chrome",
        title = "2. Download APK in Android Chrome",
        summary = "Fetch the cloud-built APK artifact or GitHub Release directly on your phone.",
        detail = "In Chrome, open your GitHub repository -> Actions -> latest Android APK Build run (or Releases). Tap the APK asset to download app-debug.apk or signed release APK into Chrome Downloads."
    ),
    DeployStep(
        id = "allow_chrome_install",
        title = "3. Allow Chrome Unknown App Installs",
        summary = "Grant Chrome permission to hand off APKs to the Android Package Installer.",
        detail = "Go to Settings -> Security & privacy -> More security settings -> Install unknown apps -> Google Chrome -> Enable. You only need to grant this once."
    ),
    DeployStep(
        id = "install_verify",
        title = "4. Install & Verify Signature",
        summary = "Open Chrome Downloads (Ctrl+J / 3-dot menu -> Downloads) and tap the APK.",
        detail = "If Play Protect prompts for a test/debug build, tap More details -> Install anyway. Launch PowerForge and inspect the SHA-256 certificate on the System tab."
    ),
    DeployStep(
        id = "realme_battery",
        title = "5. Configure Realme P4 Power Battery",
        summary = "Prevent ColorOS from freezing background telemetry or notifications.",
        detail = "Settings -> Battery -> More battery settings -> Optimize battery use -> PowerForge -> Don't optimize. Also disable 'Pause app activity if unused' in App Info."
    )
)

private val troubleshootingItems = listOf(
    TroubleshootItem(
        id = "zip_artifact",
        symptom = "GitHub Actions downloads a .zip file instead of .apk",
        resolution = "GitHub workflow Artifacts wrap the APK in a .zip archive. On your Realme phone, open 'My Files' or 'Files by Google', tap the downloaded .zip to extract app-debug.apk, then tap the extracted .apk to install. Or tag a release (v1.0.0) in GitHub mobile web to download the raw .apk directly from Releases."
    ),
    TroubleshootItem(
        id = "package_conflict",
        symptom = "'App not installed as package conflicts with an existing package'",
        resolution = "You are installing a Release-signed APK over a Debug-signed APK (or vice versa) with the same applicationId. Uninstall the previous PowerForge build first from Settings -> Apps, then install the new APK."
    ),
    TroubleshootItem(
        id = "play_protect",
        symptom = "Google Play Protect says 'Unsafe app blocked' or 'Unrecognized developer'",
        resolution = "New self-signed or debug APKs do not yet have Play Store reputation. Tap 'More details' -> 'Install anyway' to test your own build."
    ),
    TroubleshootItem(
        id = "parse_error",
        symptom = "'There was a problem parsing the package'",
        resolution = "The Chrome download was interrupted before finishing. Delete the partial file from Downloads, ensure stable Wi-Fi/5G, and re-download the APK completely."
    ),
    TroubleshootItem(
        id = "coloros_kill",
        symptom = "Realme UI kills background work or delays notifications",
        resolution = "Open App Info -> Battery usage -> turn ON 'Allow background activity' and 'Allow auto-launch'. Disable 'Sleep standby optimization' under Battery -> More battery settings."
    )
)

@Composable
fun DeployGuideScreen(
    completedStepIds: Set<String>,
    onToggleStep: (String) -> Unit,
    onResetChecklist: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedTroubleId by remember { mutableStateOf<String?>(troubleshootingItems.first().id) }
    val progress = completedStepIds.size.toFloat() / mobileDeploySteps.size.toFloat()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Mobile-Only Cloud Build & Install",
                icon = Icons.Filled.CloudDownload,
                trailingBadge = "${completedStepIds.size}/${mobileDeploySteps.size} Done"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Realme P4 Power Setup Progress",
                            style = MaterialTheme.typography.titleSmall
                        )
                        TextButton(
                            onClick = onResetChecklist,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reset_checklist_button")
                        ) {
                            Text(stringResource(R.string.action_reset_checklist))
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                    )
                    Text(
                        text = "Included workflow: .github/workflows/android-apk.yml builds both a ready-to-test Debug APK and a Release APK (when keystore secrets are added) directly in GitHub cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(mobileDeploySteps, key = { it.id }) { step ->
            val checked = step.id in completedStepIds
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onToggleStep(step.id) }
                    .testTag("deploy_step_${step.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (checked) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleStep(step.id) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = step.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = step.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(
                title = "Mobile-Only Troubleshooting",
                icon = Icons.AutoMirrored.Filled.HelpOutline
            )
        }

        items(troubleshootingItems, key = { it.id }) { item ->
            val expanded = expandedTroubleId == item.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        expandedTroubleId = if (expanded) null else item.id
                    }
                    .testTag("troubleshoot_${item.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.symptom,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        Text(
                            text = item.resolution,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Tip: If you create a Release tag (e.g. v1.0.0) in GitHub Mobile Chrome, GitHub Actions attaches the raw .apk directly to the Releases page so no zip extraction is needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
