package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CompactWorkspaceTab
import com.example.data.model.IdeMode
import com.example.ui.components.ConfirmDeleteFileDialog
import com.example.ui.components.FileNameDialog
import com.example.ui.components.FullscreenWebPreviewDialog
import com.example.ui.components.GitHubCloudSettingsDialog
import com.example.ui.components.IdeTopToolbarAndPromptBar
import com.example.ui.components.TemplateGallerySheet
import com.example.ui.components.WorkflowYamlPreviewDialog
import com.example.ui.editor.CodeEditorPane
import com.example.ui.editor.FileExplorerPane
import com.example.ui.preview.AndroidPreviewPane
import com.example.ui.preview.WebsitePreviewPane
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun IdeWorkspaceScreen(
    viewModel: IdeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateFileDialog by remember { mutableStateOf(false) }
    var renameTargetFile by remember { mutableStateOf<String?>(null) }
    var deleteTargetFile by remember { mutableStateOf<String?>(null) }
    var showFullscreenWebPreview by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showCloudSettingsDialog by remember { mutableStateOf(false) }
    var showWorkflowYamlDialog by remember { mutableStateOf(false) }

    val zipExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(viewModel.buildZipBytes())
                }
            }.onSuccess {
                Toast.makeText(context, "Project ZIP exported!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                viewModel.setStatusMessage("ZIP export failed: ${err.localizedMessage}")
            }
        }
    }

    val apkExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(viewModel.buildApkBytes())
                }
            }.onSuccess {
                Toast.makeText(context, "APK package saved!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                viewModel.setStatusMessage("APK export failed: ${err.localizedMessage}")
            }
        }
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        containerColor = ObsidianVoid,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWide = maxWidth >= 720.dp

            BackHandler(enabled = !isWide && state.compactTab != CompactWorkspaceTab.CODE) {
                viewModel.setCompactTab(CompactWorkspaceTab.CODE)
            }

            Column(modifier = Modifier.fillMaxSize()) {
                IdeTopToolbarAndPromptBar(
                    projectName = state.projectName,
                    mode = state.mode,
                    aiPrompt = state.aiPrompt,
                    aiState = state.aiState,
                    isWideLayout = isWide,
                    isSidebarVisible = state.isFileSidebarVisible,
                    isCloudBuildConfigured = state.gitHubConfig.isConfigured,
                    onProjectNameChange = viewModel::updateProjectName,
                    onSelectMode = viewModel::switchMode,
                    onAiPromptChange = viewModel::updateAiPrompt,
                    onGenerateWithGemini = viewModel::generateWithGemini,
                    onDismissAiBanner = viewModel::dismissAiStatus,
                    canUndoAi = state.undoSnapshot != null,
                    onUndoAi = viewModel::undoLastAiChange,
                    onRunRefresh = {
                        viewModel.refreshPreview()
                        if (!isWide) viewModel.setCompactTab(CompactWorkspaceTab.PREVIEW)
                    },
                    onDownloadZip = {
                        zipExportLauncher.launch(viewModel.suggestedZipFileName())
                    },
                    onBuildApk = {
                        viewModel.triggerAndroidApkBuild()
                    },
                    onOpenTemplates = { showTemplatesSheet = true },
                    onOpenSettings = { showCloudSettingsDialog = true },
                    onToggleSidebar = viewModel::toggleFileSidebar
                )

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (state.isFileSidebarVisible) {
                            FileExplorerPane(
                                files = state.files,
                                activeFileName = state.activeFile?.name.orEmpty(),
                                onSelectFile = viewModel::selectFile,
                                onRequestNewFile = { showCreateFileDialog = true },
                                onRequestRenameFile = { renameTargetFile = it },
                                onRequestDeleteFile = { deleteTargetFile = it },
                                onResetTemplate = viewModel::resetToStarterTemplate,
                                modifier = Modifier
                                    .width(210.dp)
                                    .fillMaxHeight()
                                    .border(1.dp, ObsidianBorder)
                            )
                        }

                        CodeEditorPane(
                            files = state.files,
                            activeFile = state.activeFile,
                            isSaved = state.isSaved,
                            onSelectFile = viewModel::selectFile,
                            onContentChange = viewModel::updateActiveFileContent,
                            onRequestNewFile = { showCreateFileDialog = true },
                            modifier = Modifier
                                .weight(1.05f)
                                .fillMaxHeight()
                                .border(1.dp, ObsidianBorder)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(1.dp, ObsidianBorder)
                        ) {
                            PreviewEngineHost(
                                state = state,
                                viewModel = viewModel,
                                onOpenFullscreen = { showFullscreenWebPreview = true },
                                onOpenSettings = { showCloudSettingsDialog = true },
                                onDownloadApk = { apkExportLauncher.launch(viewModel.suggestedApkFileName()) }
                            )
                        }
                    }
                } else {
                    CompactWorkspaceSwitcher(
                        activeTab = state.compactTab,
                        onSelectTab = viewModel::setCompactTab
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (state.compactTab) {
                            CompactWorkspaceTab.FILES -> {
                                FileExplorerPane(
                                    files = state.files,
                                    activeFileName = state.activeFile?.name.orEmpty(),
                                    onSelectFile = viewModel::selectFile,
                                    onRequestNewFile = { showCreateFileDialog = true },
                                    onRequestRenameFile = { renameTargetFile = it },
                                    onRequestDeleteFile = { deleteTargetFile = it },
                                    onResetTemplate = viewModel::resetToStarterTemplate,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            CompactWorkspaceTab.CODE -> {
                                CodeEditorPane(
                                    files = state.files,
                                    activeFile = state.activeFile,
                                    isSaved = state.isSaved,
                                    onSelectFile = viewModel::selectFile,
                                    onContentChange = viewModel::updateActiveFileContent,
                                    onRequestNewFile = { showCreateFileDialog = true },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            CompactWorkspaceTab.PREVIEW -> {
                                PreviewEngineHost(
                                    state = state,
                                    viewModel = viewModel,
                                    onOpenFullscreen = { showFullscreenWebPreview = true },
                                    onOpenSettings = { showCloudSettingsDialog = true },
                                    onDownloadApk = { apkExportLauncher.launch(viewModel.suggestedApkFileName()) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateFileDialog) {
        FileNameDialog(
            title = "Create New File",
            initialValue = "",
            confirmLabel = "Create",
            onDismiss = { showCreateFileDialog = false },
            onConfirm = { newName ->
                showCreateFileDialog = false
                viewModel.createFile(newName)
            }
        )
    }

    renameTargetFile?.let { target ->
        FileNameDialog(
            title = "Rename File",
            initialValue = target,
            confirmLabel = "Rename",
            onDismiss = { renameTargetFile = null },
            onConfirm = { newName ->
                renameTargetFile = null
                viewModel.renameFile(target, newName)
            }
        )
    }

    deleteTargetFile?.let { target ->
        ConfirmDeleteFileDialog(
            fileName = target,
            onDismiss = { deleteTargetFile = null },
            onConfirm = {
                deleteTargetFile = null
                viewModel.deleteFile(target)
            }
        )
    }

    if (showFullscreenWebPreview) {
        FullscreenWebPreviewDialog(
            projectName = state.projectName,
            files = state.files,
            refreshNonce = state.refreshNonce,
            onRefresh = viewModel::refreshPreview,
            onConsoleMessage = viewModel::appendConsoleLog,
            onDismiss = { showFullscreenWebPreview = false }
        )
    }

    if (showTemplatesSheet) {
        TemplateGallerySheet(
            onSelectTemplate = { template ->
                showTemplatesSheet = false
                viewModel.loadTemplate(template)
            },
            onDismiss = { showTemplatesSheet = false }
        )
    }

    if (showCloudSettingsDialog) {
        GitHubCloudSettingsDialog(
            currentConfig = state.gitHubConfig,
            onSave = { config ->
                showCloudSettingsDialog = false
                viewModel.saveGitHubConfig(config)
            },
            onClearSecrets = {
                viewModel.clearGitHubSecrets()
            },
            onOpenWorkflowYaml = {
                showWorkflowYamlDialog = true
            },
            onDismiss = { showCloudSettingsDialog = false }
        )
    }

    if (showWorkflowYamlDialog) {
        WorkflowYamlPreviewDialog(
            isGitHubConfigured = state.gitHubConfig.isConfigured,
            onPushToRepo = viewModel::pushWorkflowToGitHub,
            onDismiss = { showWorkflowYamlDialog = false }
        )
    }
}

@Composable
private fun PreviewEngineHost(
    state: IdeUiState,
    viewModel: IdeViewModel,
    onOpenFullscreen: () -> Unit,
    onOpenSettings: () -> Unit,
    onDownloadApk: () -> Unit
) {
    when (state.mode) {
        IdeMode.WEBSITE -> {
            WebsitePreviewPane(
                files = state.files,
                viewportSize = state.viewportSize,
                refreshNonce = state.refreshNonce,
                consoleEntries = state.consoleEntries,
                isConsoleExpanded = state.isConsoleExpanded,
                isFixingError = state.isFixingError,
                onSelectViewport = viewModel::setViewportSize,
                onRefresh = viewModel::refreshPreview,
                onOpenFullscreen = onOpenFullscreen,
                onToggleConsole = viewModel::toggleConsoleExpanded,
                onClearConsole = viewModel::clearConsole,
                onFixErrorWithAi = viewModel::fixErrorWithAi,
                onConsoleMessage = viewModel::appendConsoleLog,
                modifier = Modifier.fillMaxSize()
            )
        }
        IdeMode.ANDROID -> {
            AndroidPreviewPane(
                files = state.files,
                refreshNonce = state.refreshNonce,
                activeTab = state.androidPreviewTab,
                apkBuildState = state.apkBuildState,
                buildLogs = state.buildLogs,
                isCloudBuildConfigured = state.gitHubConfig.isConfigured,
                isFixingError = state.isFixingError,
                onSelectTab = viewModel::setAndroidPreviewTab,
                onTriggerBuild = viewModel::triggerAndroidApkBuild,
                onDownloadApk = onDownloadApk,
                onFixErrorWithAi = viewModel::fixErrorWithAi,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CompactWorkspaceSwitcher(
    activeTab: CompactWorkspaceTab,
    onSelectTab: (CompactWorkspaceTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianPanel)
            .border(1.dp, ObsidianBorder)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CompactWorkspaceTab.entries.forEach { tab ->
            val selected = tab == activeTab
            val icon = when (tab) {
                CompactWorkspaceTab.FILES -> Icons.Default.FolderOpen
                CompactWorkspaceTab.CODE -> Icons.Default.Code
                CompactWorkspaceTab.PREVIEW -> Icons.Default.Visibility
            }
            Surface(
                color = if (selected) ObsidianElevated else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
                border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, CyanElectric.copy(alpha = 0.5f)) else null,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectTab(tab) }
                    .testTag("compact_tab_${tab.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) CyanElectric else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) TextPrimary else TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
