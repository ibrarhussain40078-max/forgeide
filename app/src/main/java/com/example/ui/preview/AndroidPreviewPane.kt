package com.example.ui.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AndroidPreviewTab
import com.example.data.model.ApkBuildState
import com.example.data.model.BuildLogEntry
import com.example.data.model.CodeFile
import com.example.ui.components.QrCodeView
import com.example.ui.simulator.AndroidUiSimulatorScreen
import com.example.ui.simulator.SimulatorController
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldPass
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.IndigoGlow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoseSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AndroidPreviewPane(
    files: List<CodeFile>,
    refreshNonce: Long,
    activeTab: AndroidPreviewTab,
    apkBuildState: ApkBuildState,
    buildLogs: List<BuildLogEntry>,
    isCloudBuildConfigured: Boolean,
    isFixingError: Boolean,
    onSelectTab: (AndroidPreviewTab) -> Unit,
    onTriggerBuild: () -> Unit,
    onDownloadApk: () -> Unit,
    onFixErrorWithAi: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val simulatorController = remember { SimulatorController() }

    Column(
        modifier = modifier
            .background(ObsidianSurface)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dual-Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianPanel)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AndroidPreviewTab.entries.forEach { tab ->
                val selected = tab == activeTab
                Surface(
                    color = if (selected) ObsidianElevated else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (selected) BorderStroke(1.dp, CyanElectric.copy(alpha = 0.5f)) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectTab(tab) }
                        .testTag("android_tab_${tab.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (tab == AndroidPreviewTab.SIMULATOR) {
                                Icons.Default.PhoneAndroid
                            } else {
                                Icons.Default.Build
                            },
                            contentDescription = null,
                            tint = if (selected) CyanElectric else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) TextPrimary else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Realistic Smartphone Bezel Frame
        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(34.dp))
                .background(Color(0xFF05070B))
                .border(3.dp, Color(0xFF2E3B55), RoundedCornerShape(34.dp))
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ObsidianVoid)
            ) {
                // Realistic Android Status Bar with Punch-Hole Camera
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurface)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "10:42",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF020408))
                            .border(1.dp, Color(0xFF283652), CircleShape)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = EmeraldPass,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Active Tab Content inside Phone Frame
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        AndroidPreviewTab.SIMULATOR -> {
                            AndroidUiSimulatorScreen(
                                files = files,
                                refreshNonce = refreshNonce,
                                controller = simulatorController,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        AndroidPreviewTab.BUILD_CONSOLE -> {
                            ApkBuildConsoleView(
                                apkBuildState = apkBuildState,
                                buildLogs = buildLogs,
                                isCloudBuildConfigured = isCloudBuildConfigured,
                                isFixingError = isFixingError,
                                onTriggerBuild = onTriggerBuild,
                                onDownloadApk = onDownloadApk,
                                onFixErrorWithAi = onFixErrorWithAi,
                                onOpenSettings = onOpenSettings,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Android System Navigation Bar (Back / Home / Reset)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF070B12))
                        .border(1.dp, ObsidianBorder.copy(alpha = 0.4f))
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (activeTab == AndroidPreviewTab.SIMULATOR) {
                                    if (!simulatorController.navigateBack()) {
                                        simulatorController.showToast("At root screen")
                                    }
                                } else {
                                    onSelectTab(AndroidPreviewTab.SIMULATOR)
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                            .testTag("phone_nav_back"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Simulated Back",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectTab(AndroidPreviewTab.SIMULATOR)
                                simulatorController.navigateHome("home")
                            }
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                            .testTag("phone_nav_home"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = "Simulated Home",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                simulatorController.resetSignal++
                                simulatorController.showToast("App state reset")
                            }
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                            .testTag("phone_nav_recents"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropSquare,
                            contentDescription = "Reset Simulated App",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkBuildConsoleView(
    apkBuildState: ApkBuildState,
    buildLogs: List<BuildLogEntry>,
    isCloudBuildConfigured: Boolean,
    isFixingError: Boolean,
    onTriggerBuild: () -> Unit,
    onDownloadApk: () -> Unit,
    onFixErrorWithAi: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(ObsidianVoid)
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCloudBuildConfigured) "GitHub Cloud Runner" else "GitHub not connected",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = if (isCloudBuildConfigured) {
                        "Real JDK 17 + assembleDebug pipeline"
                    } else {
                        "Demo mode • Tap Settings for GitHub APK"
                    },
                    fontSize = 10.sp,
                    color = if (isCloudBuildConfigured) EmeraldPass else AmberWarn,
                    modifier = Modifier.clickable(onClick = onOpenSettings)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isCloudBuildConfigured) {
                    Surface(
                        color = ObsidianElevated,
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .clickable(onClick = onOpenSettings)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure GitHub Cloud Build",
                            tint = TextSecondary,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(15.dp)
                        )
                    }
                }
                Button(
                    onClick = onTriggerBuild,
                    enabled = apkBuildState !is ApkBuildState.Building,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoGlow,
                        contentColor = ObsidianVoid
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("run_gradle_build_button")
                ) {
                    Text(
                        text = if (apkBuildState is ApkBuildState.Building) "Building..." else "Run Build",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live 4-Stage Pipeline Tracker
        if (apkBuildState is ApkBuildState.Building) {
            val stages = listOf("Pushing Source", "Preparing Runner", "Running Gradle assembleDebug", "Uploading APK")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianPanel)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LinearProgressIndicator(
                    progress = { apkBuildState.progress },
                    color = CyanElectric,
                    trackColor = ObsidianElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                )
                Text(
                    text = apkBuildState.stageLabel + ": " + apkBuildState.currentTask,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = CyanElectric,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeIdx = stages.indexOfFirst { it == apkBuildState.stageLabel }.coerceAtLeast(0)
                    stages.forEachIndexed { idx, label ->
                        val active = idx <= activeIdx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (active) CyanElectric.copy(alpha = 0.2f) else ObsidianVoid)
                                .padding(vertical = 3.dp, horizontal = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label.split(" ").last(),
                                fontSize = 8.sp,
                                color = if (active) CyanElectric else TextMuted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Terminal Logs
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF05080F))
                .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            if (buildLogs.isEmpty()) {
                Text(
                    text = "Click 'Build & Download APK' or 'Run Build' to push Kotlin sources to GitHub Actions and compile a real debug APK (requires GitHub settings).",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(buildLogs, key = { it.id }) { log ->
                        val color = when {
                            log.isError -> RoseError
                            log.isWarning -> AmberWarn
                            log.isSuccess -> EmeraldPass
                            else -> TextSecondary
                        }
                        Text(
                            text = "[${log.timestamp}] ${log.text}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = color
                        )
                    }
                }
            }
        }

        // Result Card with Download .APK File + QR Code or Fix Error with AI
        when (apkBuildState) {
            is ApkBuildState.Success -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
                    border = BorderStroke(1.dp, EmeraldPass.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (apkBuildState.isCloudBuild) Icons.Default.CloudDone else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldPass,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "APK Ready",
                                        color = EmeraldPass,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${apkBuildState.apkFileName}\n${apkBuildState.sizeKb} KB • ${apkBuildState.durationMs}ms",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }

                            if (apkBuildState.qrPayloadUrl.isNotBlank()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    QrCodeView(
                                        content = apkBuildState.qrPayloadUrl,
                                        size = 76.dp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Scan QR",
                                        fontSize = 9.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onDownloadApk,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPass,
                                contentColor = ObsidianVoid
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("download_apk_card_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Download .APK File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is ApkBuildState.Failed -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoseSurface),
                    border = BorderStroke(1.dp, RoseError.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = RoseError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Build Failed",
                                    color = RoseError,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = apkBuildState.errorSummary,
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = onFixErrorWithAi,
                            enabled = !isFixingError,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoGlow,
                                contentColor = ObsidianVoid
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("android_fix_error_ai_button")
                        ) {
                            if (isFixingError) {
                                CircularProgressIndicator(
                                    color = ObsidianVoid,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Patching with Gemini...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Fix Error with AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}
