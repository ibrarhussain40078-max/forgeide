package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AiGenerationState
import com.example.data.model.IdeMode
import com.example.data.templates.StarterTemplates
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldPass
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.IndigoGlow
import com.example.ui.theme.IndigoSurface
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
fun IdeTopToolbarAndPromptBar(
    projectName: String,
    mode: IdeMode,
    aiPrompt: String,
    aiState: AiGenerationState,
    isWideLayout: Boolean,
    isSidebarVisible: Boolean,
    isCloudBuildConfigured: Boolean,
    onProjectNameChange: (String) -> Unit,
    onSelectMode: (IdeMode) -> Unit,
    onAiPromptChange: (String) -> Unit,
    onGenerateWithGemini: (String?) -> Unit,
    onDismissAiBanner: () -> Unit,
    canUndoAi: Boolean = false,
    onUndoAi: () -> Unit = {},
    onRunRefresh: () -> Unit,
    onDownloadZip: () -> Unit,
    onBuildApk: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Brand + Project Name + Mode Switcher + Templates + Action Buttons + Cloud Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isWideLayout) {
                IconButton(
                    onClick = onToggleSidebar,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("toggle_sidebar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewSidebar,
                        contentDescription = "Toggle file explorer",
                        tint = if (isSidebarVisible) CyanElectric else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_forge_logo_1795187388175),
                    contentDescription = "ForgeIDE logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(7.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ForgeIDE",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianVoid)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                BasicTextField(
                    value = projectName,
                    onValueChange = onProjectNameChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(CyanElectric),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input")
                )
            }

            // Mode Switcher Tabs
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(ObsidianPanel)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(9.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                IdeMode.entries.forEach { item ->
                    val selected = item == mode
                    val accent = if (item == IdeMode.WEBSITE) CyanElectric else EmeraldPass
                    Surface(
                        color = if (selected) ObsidianElevated else Color.Transparent,
                        shape = RoundedCornerShape(7.dp),
                        border = if (selected) BorderStroke(1.dp, accent.copy(alpha = 0.55f)) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .clickable { onSelectMode(item) }
                            .testTag("mode_tab_${item.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item == IdeMode.WEBSITE) Icons.Default.Language else Icons.Default.Android,
                                contentDescription = null,
                                tint = if (selected) accent else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }

            // Templates Gallery Button
            OutlinedButton(
                onClick = onOpenTemplates,
                border = BorderStroke(1.dp, IndigoGlow.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoGlow),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("templates_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DashboardCustomize,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Templates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            // Action Buttons
            OutlinedButton(
                onClick = onRunRefresh,
                border = BorderStroke(1.dp, ObsidianBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanElectric),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("run_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Run", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onDownloadZip,
                border = BorderStroke(1.dp, ObsidianBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("download_zip_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    tint = CyanElectric,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Download ZIP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            if (mode == IdeMode.ANDROID) {
                Button(
                    onClick = onBuildApk,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPass,
                        contentColor = ObsidianVoid
                    ),
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("build_download_apk_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Build & Download APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // GitHub Cloud Build Settings Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianPanel)
                    .border(
                        1.dp,
                        if (isCloudBuildConfigured) EmeraldPass.copy(alpha = 0.55f) else ObsidianBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("cloud_settings_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isCloudBuildConfigured) EmeraldPass else TextMuted)
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Cloud APK Settings",
                        tint = if (isCloudBuildConfigured) EmeraldPass else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Row 2: AI Prompt Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(ObsidianPanel, IndigoSurface.copy(alpha = 0.55f))
                    )
                )
                .border(1.dp, IndigoGlow.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = IndigoGlow,
                modifier = Modifier.size(18.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (aiPrompt.isEmpty()) {
                    Text(
                        text = if (mode == IdeMode.WEBSITE) {
                            "Ask Gemini: e.g. 'Build a scientific calculator' or 'Create an e-commerce store'..."
                        } else {
                            "Ask Gemini for Android: e.g. 'Build a scientific calculator' or 'Expense tracker'..."
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = aiPrompt,
                    onValueChange = onAiPromptChange,
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(IndigoGlow),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onGenerateWithGemini(null) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_prompt_input")
                )
            }
            val isGenerating = aiState is AiGenerationState.Loading
            Button(
                onClick = { onGenerateWithGemini(null) },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndigoGlow,
                    contentColor = ObsidianVoid
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .height(36.dp)
                    .testTag("generate_gemini_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = ObsidianVoid,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Generating...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Generate with Gemini", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Prompt Chips
        val suggestions = if (mode == IdeMode.WEBSITE) {
            StarterTemplates.promptSuggestionsWebsite
        } else {
            StarterTemplates.promptSuggestionsAndroid
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            suggestions.forEach { chipPrompt ->
                Surface(
                    color = ObsidianPanel,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(enabled = aiState !is AiGenerationState.Loading) {
                            onGenerateWithGemini(chipPrompt)
                        }
                ) {
                    Text(
                        text = chipPrompt,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // AI Generation Status Banner
        AnimatedVisibility(visible = aiState is AiGenerationState.Error || aiState is AiGenerationState.Success) {
            val isError = aiState is AiGenerationState.Error
            val message = when (aiState) {
                is AiGenerationState.Error -> aiState.message
                is AiGenerationState.Success -> aiState.summary
                else -> ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isError) RoseSurface else EmeraldSurface)
                    .border(
                        1.dp,
                        if (isError) RoseError.copy(alpha = 0.5f) else EmeraldPass.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message,
                    color = if (isError) RoseError else EmeraldPass,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                if (!isError && canUndoAi) {
                    TextButton(onClick = onUndoAi) {
                        Text("Undo", color = EmeraldPass, style = MaterialTheme.typography.labelMedium)
                    }
                }
                IconButton(
                    onClick = onDismissAiBanner,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss status",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
