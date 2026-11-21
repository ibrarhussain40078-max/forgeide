package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiCodeGenerator
import com.example.data.export.ProjectPackager
import com.example.data.local.GitHubBuildConfig
import com.example.data.local.IdeDatabase
import com.example.data.local.IdeRepository
import com.example.data.local.SecureGitHubPrefs
import com.example.data.model.AiGenerationState
import com.example.data.model.AndroidPreviewTab
import com.example.data.model.ApkBuildState
import com.example.data.model.BuildLogEntry
import com.example.data.model.CodeFile
import com.example.data.model.CompactWorkspaceTab
import com.example.data.model.ConsoleEntry
import com.example.data.model.ConsoleLevel
import com.example.data.model.FileLanguage
import com.example.data.model.IdeMode
import com.example.data.model.ViewportSize
import com.example.data.remote.GitHubBuildService
import com.example.data.templates.IdeTemplateItem
import com.example.data.templates.StarterTemplates
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Workspace state captured right before an AI rewrite so the user can revert it. */
data class WorkspaceSnapshot(
    val projectName: String,
    val files: List<CodeFile>,
    val activeFileName: String
)

data class IdeUiState(
    val mode: IdeMode = IdeMode.WEBSITE,
    val projectName: String = StarterTemplates.defaultProjectName(IdeMode.WEBSITE),
    val files: List<CodeFile> = StarterTemplates.websiteStarterFiles(),
    val activeFileName: String = "index.html",
    val aiPrompt: String = "",
    val aiState: AiGenerationState = AiGenerationState.Idle,
    val isFixingError: Boolean = false,
    val viewportSize: ViewportSize = ViewportSize.DESKTOP,
    val consoleEntries: List<ConsoleEntry> = emptyList(),
    val isConsoleExpanded: Boolean = true,
    val androidPreviewTab: AndroidPreviewTab = AndroidPreviewTab.SIMULATOR,
    val apkBuildState: ApkBuildState = ApkBuildState.Idle,
    val buildLogs: List<BuildLogEntry> = emptyList(),
    val gitHubConfig: GitHubBuildConfig = GitHubBuildConfig(),
    val compactTab: CompactWorkspaceTab = CompactWorkspaceTab.CODE,
    val isFileSidebarVisible: Boolean = true,
    val isSaved: Boolean = true,
    val refreshNonce: Long = 1L,
    val statusMessage: String? = null,
    val undoSnapshot: WorkspaceSnapshot? = null,
    val geminiApiKey: String = "",
    val geminiModel: String = ""
) {
    val activeFile: CodeFile?
        get() = files.firstOrNull { it.name == activeFileName } ?: files.firstOrNull()
}

class IdeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IdeRepository by lazy {
        IdeRepository(IdeDatabase.getInstance(application).ideDao())
    }
    private val secureGitHubPrefs: SecureGitHubPrefs by lazy {
        SecureGitHubPrefs(application)
    }
    private val geminiGenerator = GeminiCodeGenerator { secureGitHubPrefs.loadGemini() }
    private val gitHubBuildService = GitHubBuildService()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val _uiState = MutableStateFlow(
        secureGitHubPrefs.loadGemini().let { (gemKey, gemModel) ->
            IdeUiState(gitHubConfig = secureGitHubPrefs.loadConfig(), geminiApiKey = gemKey, geminiModel = gemModel)
        }
    )
    val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var buildJob: Job? = null
    private var consoleCounter = 1L
    private var buildLogCounter = 1L
    private var cloudCompiledApkBytes: ByteArray? = null

    init {
        loadWorkspaceForMode(IdeMode.WEBSITE)
    }

    fun switchMode(newMode: IdeMode) {
        if (_uiState.value.mode == newMode) return
        persistCurrentWorkspaceSync()
        loadWorkspaceForMode(newMode)
    }

    private fun loadWorkspaceForMode(mode: IdeMode) {
        viewModelScope.launch {
            val saved = repository.loadWorkspace(mode)
            val files = saved?.files ?: when (mode) {
                IdeMode.WEBSITE -> StarterTemplates.websiteStarterFiles()
                IdeMode.ANDROID -> StarterTemplates.androidStarterFiles()
            }
            val projectName = saved?.projectName ?: StarterTemplates.defaultProjectName(mode)
            val activeFile = saved?.activeFileName?.takeIf { name -> files.any { it.name == name } }
                ?: files.firstOrNull()?.name.orEmpty()

            cloudCompiledApkBytes = null
            _uiState.update {
                it.copy(
                    mode = mode,
                    projectName = projectName,
                    files = files,
                    activeFileName = activeFile,
                    consoleEntries = emptyList(),
                    apkBuildState = ApkBuildState.Idle,
                    buildLogs = emptyList(),
                    androidPreviewTab = AndroidPreviewTab.SIMULATOR,
                    undoSnapshot = null,
                    isSaved = true,
                    refreshNonce = it.refreshNonce + 1L
                )
            }
            if (saved == null) {
                repository.saveWorkspace(mode, projectName, activeFile, files)
            }
        }
    }

    fun loadTemplate(template: IdeTemplateItem) {
        viewModelScope.launch {
            cloudCompiledApkBytes = null
            val activeFile = template.files.first().name
            repository.saveWorkspace(
                mode = template.mode,
                projectName = template.projectName,
                activeFileName = activeFile,
                files = template.files
            )
            _uiState.update {
                it.copy(
                    mode = template.mode,
                    projectName = template.projectName,
                    files = template.files,
                    activeFileName = activeFile,
                    consoleEntries = emptyList(),
                    apkBuildState = ApkBuildState.Idle,
                    buildLogs = emptyList(),
                    androidPreviewTab = AndroidPreviewTab.SIMULATOR,
                    compactTab = CompactWorkspaceTab.PREVIEW,
                    isSaved = true,
                    refreshNonce = it.refreshNonce + 1L,
                    statusMessage = "Loaded template: ${template.title}"
                )
            }
        }
    }

    fun saveGitHubConfig(config: GitHubBuildConfig) {
        val tokenStored = secureGitHubPrefs.saveConfig(config)
        if (!tokenStored) {
            _uiState.update {
                it.copy(
                    gitHubConfig = config.copy(token = ""),
                    statusMessage = "Secure storage unavailable on this device: token was NOT saved."
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                gitHubConfig = config,
                statusMessage = if (config.isConfigured) {
                    "Cloud APK build connected to ${config.owner}/${config.repo}"
                } else {
                    "GitHub settings saved (add token, owner and repo to build APKs)."
                }
            )
        }
    }

    fun saveGeminiSettings(apiKey: String, model: String) {
        val ok = secureGitHubPrefs.saveGemini(apiKey, model)
        _uiState.update {
            it.copy(
                geminiApiKey = if (ok) apiKey.trim() else "",
                geminiModel = model.trim(),
                statusMessage = when {
                    !ok -> "Secure storage unavailable: Gemini key was NOT saved."
                    apiKey.isBlank() -> "Gemini API key removed."
                    else -> "Gemini API key saved."
                }
            )
        }
    }

    fun clearGitHubSecrets() {
        secureGitHubPrefs.clearSecrets()
        _uiState.update {
            it.copy(
                gitHubConfig = GitHubBuildConfig(),
                geminiApiKey = "",
                geminiModel = "",
                statusMessage = "Cleared saved secrets."
            )
        }
    }

    fun pushWorkflowToGitHub() {
        val cfg = _uiState.value.gitHubConfig
        viewModelScope.launch {
            val res = gitHubBuildService.pushWorkflowYaml(cfg)
            res.fold(
                onSuccess = { msg -> setStatusMessage(msg) },
                onFailure = { err -> setStatusMessage("Workflow push failed: ${err.localizedMessage}") }
            )
        }
    }

    fun updateProjectName(name: String) {
        _uiState.update { it.copy(projectName = name, isSaved = false) }
        scheduleAutoSave()
    }

    fun selectFile(fileName: String) {
        _uiState.update {
            it.copy(
                activeFileName = fileName,
                compactTab = CompactWorkspaceTab.CODE
            )
        }
        scheduleAutoSave()
    }

    fun updateActiveFileContent(newContent: String) {
        val currentName = _uiState.value.activeFile?.name ?: return
        _uiState.update { state ->
            val updatedFiles = state.files.map { file ->
                if (file.name == currentName) file.copy(content = newContent) else file
            }
            state.copy(files = updatedFiles, isSaved = false)
        }
        scheduleAutoSave(triggerPreviewRefresh = true)
    }

    fun createFile(rawName: String) {
        val name = rawName.trim()
        if (name.isBlank()) return
        if (_uiState.value.files.any { it.name.equals(name, ignoreCase = true) }) {
            setStatusMessage("File '$name' already exists.")
            return
        }
        val lang = FileLanguage.fromFileName(name)
        val starter = when (lang) {
            FileLanguage.HTML -> "<section>\n  <h2>New Section</h2>\n</section>\n"
            FileLanguage.CSS -> "/* $name */\n"
            FileLanguage.JS -> "// $name\nconsole.log('Loaded $name');\n"
            FileLanguage.KOTLIN -> "package com.forgeide.app\n\n// $name\n"
            FileLanguage.XML -> "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>\n"
            FileLanguage.JSON -> "{\n}\n"
            else -> ""
        }
        val newFile = CodeFile(name = name, content = starter, language = lang)
        _uiState.update {
            it.copy(
                files = it.files + newFile,
                activeFileName = newFile.name,
                compactTab = CompactWorkspaceTab.CODE,
                isSaved = false,
                refreshNonce = it.refreshNonce + 1L
            )
        }
        scheduleAutoSave()
    }

    fun renameFile(oldName: String, rawNewName: String) {
        val newName = rawNewName.trim()
        if (newName.isBlank() || oldName == newName) return
        if (_uiState.value.files.any { it.name.equals(newName, ignoreCase = true) }) {
            setStatusMessage("A file named '$newName' already exists.")
            return
        }
        _uiState.update { state ->
            val updated = state.files.map { file ->
                if (file.name == oldName) {
                    file.copy(name = newName, language = FileLanguage.fromFileName(newName))
                } else {
                    file
                }
            }
            val nextActive = if (state.activeFileName == oldName) newName else state.activeFileName
            state.copy(
                files = updated,
                activeFileName = nextActive,
                isSaved = false,
                refreshNonce = state.refreshNonce + 1L
            )
        }
        scheduleAutoSave()
    }

    fun deleteFile(fileName: String) {
        val currentFiles = _uiState.value.files
        if (currentFiles.size <= 1) {
            setStatusMessage("Cannot delete the last file in the project.")
            return
        }
        val updated = currentFiles.filterNot { it.name == fileName }
        val nextActive = if (_uiState.value.activeFileName == fileName) {
            updated.first().name
        } else {
            _uiState.value.activeFileName
        }
        _uiState.update {
            it.copy(
                files = updated,
                activeFileName = nextActive,
                isSaved = false,
                refreshNonce = it.refreshNonce + 1L
            )
        }
        scheduleAutoSave()
    }

    fun resetToStarterTemplate() {
        val mode = _uiState.value.mode
        val files = when (mode) {
            IdeMode.WEBSITE -> StarterTemplates.websiteStarterFiles()
            IdeMode.ANDROID -> StarterTemplates.androidStarterFiles()
        }
        val name = StarterTemplates.defaultProjectName(mode)
        cloudCompiledApkBytes = null
        _uiState.update {
            it.copy(
                projectName = name,
                files = files,
                activeFileName = files.first().name,
                consoleEntries = emptyList(),
                apkBuildState = ApkBuildState.Idle,
                buildLogs = emptyList(),
                isSaved = false,
                refreshNonce = it.refreshNonce + 1L,
                statusMessage = "Reset ${mode.label} starter template."
            )
        }
        scheduleAutoSave()
    }

    fun updateAiPrompt(prompt: String) {
        _uiState.update { it.copy(aiPrompt = prompt) }
    }

    fun dismissAiStatus() {
        _uiState.update { it.copy(aiState = AiGenerationState.Idle) }
    }

    fun generateWithGemini(overridePrompt: String? = null) {
        val prompt = (overridePrompt ?: _uiState.value.aiPrompt).trim()
        if (prompt.isBlank()) {
            _uiState.update {
                it.copy(aiState = AiGenerationState.Error("Describe what you want Gemini to build first."))
            }
            return
        }
        if (overridePrompt != null) {
            _uiState.update { it.copy(aiPrompt = prompt) }
        }

        val targetMode = _uiState.value.mode
        val currentFiles = _uiState.value.files

        viewModelScope.launch {
            _uiState.update { it.copy(aiState = AiGenerationState.Loading(prompt)) }
            val result = geminiGenerator.generateProject(targetMode, prompt, currentFiles)
            result.fold(
                onSuccess = { payload ->
                    val preferredActive = when (targetMode) {
                        IdeMode.WEBSITE -> payload.files.firstOrNull { it.name == "index.html" }?.name
                        IdeMode.ANDROID -> payload.files.firstOrNull { it.name == "MainActivity.kt" }?.name
                    } ?: payload.files.first().name

                    repository.saveWorkspace(
                        mode = targetMode,
                        projectName = payload.projectName,
                        activeFileName = preferredActive,
                        files = payload.files
                    )

                    cloudCompiledApkBytes = null
                    _uiState.update {
                        it.copy(
                            undoSnapshot = WorkspaceSnapshot(it.projectName, it.files, it.activeFileName),
                            projectName = payload.projectName,
                            files = payload.files,
                            activeFileName = preferredActive,
                            aiState = AiGenerationState.Success(payload.summary),
                            consoleEntries = emptyList(),
                            apkBuildState = ApkBuildState.Idle,
                            buildLogs = emptyList(),
                            androidPreviewTab = AndroidPreviewTab.SIMULATOR,
                            compactTab = CompactWorkspaceTab.PREVIEW,
                            isSaved = true,
                            refreshNonce = it.refreshNonce + 1L,
                            statusMessage = "Gemini generated ${payload.files.size} files."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            aiState = AiGenerationState.Error(
                                err.localizedMessage ?: "Gemini generation failed. Check connection or try again."
                            )
                        )
                    }
                }
            )
        }
    }

    fun fixErrorWithAi() {
        val state = _uiState.value
        if (state.isFixingError) return

        val errorText = when (state.mode) {
            IdeMode.WEBSITE -> {
                state.consoleEntries
                    .filter { it.level == ConsoleLevel.ERROR }
                    .joinToString("\n") { it.message }
                    .ifBlank { "Runtime JavaScript or layout error in Website preview." }
            }
            IdeMode.ANDROID -> {
                val failedMsg = (state.apkBuildState as? ApkBuildState.Failed)?.errorSummary.orEmpty()
                val logErrs = state.buildLogs.filter { it.isError }.joinToString("\n") { it.text }
                listOf(failedMsg, logErrs).filter { it.isNotBlank() }.joinToString("\n")
                    .ifBlank { "Android Gradle compilation error." }
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFixingError = true,
                    aiState = AiGenerationState.Loading("Fixing error with Gemini...")
                )
            }
            val result = geminiGenerator.fixCodeError(
                mode = state.mode,
                projectName = state.projectName,
                files = state.files,
                errorMessage = errorText
            )
            result.fold(
                onSuccess = { payload ->
                    val active = payload.files.firstOrNull { it.name == state.activeFileName }?.name
                        ?: payload.files.first().name
                    repository.saveWorkspace(state.mode, payload.projectName, active, payload.files)
                    _uiState.update {
                        it.copy(
                            isFixingError = false,
                            undoSnapshot = WorkspaceSnapshot(it.projectName, it.files, it.activeFileName),
                            projectName = payload.projectName,
                            files = payload.files,
                            activeFileName = active,
                            aiState = AiGenerationState.Success("AI Fix Applied: ${payload.summary}"),
                            consoleEntries = emptyList(),
                            isSaved = true,
                            refreshNonce = it.refreshNonce + 1L,
                            statusMessage = "Fixed with AI: ${payload.summary}"
                        )
                    }
                    if (state.mode == IdeMode.ANDROID) {
                        triggerAndroidApkBuild()
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isFixingError = false,
                            aiState = AiGenerationState.Error(
                                "AI Error Fixer failed: ${err.localizedMessage ?: "Unknown error"}"
                            )
                        )
                    }
                }
            )
        }
    }

    /** Reverts the last AI generation/fix. */
    fun undoLastAiChange() {
        val snap = _uiState.value.undoSnapshot ?: return
        val mode = _uiState.value.mode
        viewModelScope.launch {
            repository.saveWorkspace(mode, snap.projectName, snap.activeFileName, snap.files)
            cloudCompiledApkBytes = null
            _uiState.update {
                it.copy(
                    projectName = snap.projectName,
                    files = snap.files,
                    activeFileName = snap.activeFileName,
                    undoSnapshot = null,
                    aiState = AiGenerationState.Idle,
                    consoleEntries = emptyList(),
                    isSaved = true,
                    refreshNonce = it.refreshNonce + 1L,
                    statusMessage = "Reverted last AI change."
                )
            }
        }
    }

    fun refreshPreview() {
        _uiState.update {
            it.copy(
                refreshNonce = it.refreshNonce + 1L,
                statusMessage = "Preview refreshed."
            )
        }
    }

    fun setViewportSize(size: ViewportSize) {
        _uiState.update { it.copy(viewportSize = size) }
    }

    fun toggleConsoleExpanded() {
        _uiState.update { it.copy(isConsoleExpanded = !it.isConsoleExpanded) }
    }

    fun clearConsole() {
        _uiState.update { it.copy(consoleEntries = emptyList()) }
    }

    fun appendConsoleLog(level: ConsoleLevel, message: String, source: String = "") {
        val entry = ConsoleEntry(
            id = consoleCounter++,
            timestamp = timeFormat.format(Date()),
            level = level,
            message = message,
            source = source
        )
        _uiState.update { state ->
            val nextList = (state.consoleEntries + entry).takeLast(120)
            state.copy(consoleEntries = nextList)
        }
    }

    fun setAndroidPreviewTab(tab: AndroidPreviewTab) {
        _uiState.update { it.copy(androidPreviewTab = tab) }
    }

    fun setCompactTab(tab: CompactWorkspaceTab) {
        _uiState.update { it.copy(compactTab = tab) }
    }

    fun toggleFileSidebar() {
        _uiState.update { it.copy(isFileSidebarVisible = !it.isFileSidebarVisible) }
    }

    fun triggerAndroidApkBuild() {
        buildJob?.cancel()
        buildJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val filesSnapshot = _uiState.value.files
            val projectTitle = _uiState.value.projectName
            val ghConfig = _uiState.value.gitHubConfig
            val useCloud = ghConfig.isConfigured
            cloudCompiledApkBytes = null

            _uiState.update {
                it.copy(
                    androidPreviewTab = AndroidPreviewTab.BUILD_CONSOLE,
                    compactTab = CompactWorkspaceTab.PREVIEW,
                    buildLogs = emptyList(),
                    apkBuildState = ApkBuildState.Building(
                        progress = 0.05f,
                        currentTask = "Validating sources...",
                        stageLabel = "Pushing Source",
                        isCloudBuild = useCloud
                    )
                )
            }

            // First run static local pre-flight validation so syntax bugs are caught fast
            val report = ProjectPackager.validateAndroidBuild(filesSnapshot)
            // Re-key entries so log ids never collide with the build console's own counter.
            val preflightLogs = report.logs.map { it.copy(id = buildLogCounter++) }
            _uiState.update { state -> state.copy(buildLogs = state.buildLogs + preflightLogs) }
            if (report.hasErrors) {
                val duration = (System.currentTimeMillis() - startTime).coerceAtLeast(350L)
                _uiState.update {
                    it.copy(
                        apkBuildState = ApkBuildState.Failed(
                            errorSummary = report.errorSummary,
                            durationMs = duration,
                            isCloudBuild = useCloud
                        )
                    )
                }
                return@launch
            }

            if (useCloud) {
                val cloudResult = gitHubBuildService.pushSourcesAndBuildApk(
                    config = ghConfig,
                    projectName = projectTitle,
                    files = filesSnapshot
                ) { text, progress, stageLabel, isWarning ->
                    val logEntry = BuildLogEntry(
                        id = buildLogCounter++,
                        timestamp = timeFormat.format(Date()),
                        text = text,
                        isWarning = isWarning,
                        isSuccess = progress >= 1.0f
                    )
                    _uiState.update { state ->
                        state.copy(
                            buildLogs = state.buildLogs + logEntry,
                            apkBuildState = ApkBuildState.Building(
                                progress = progress,
                                currentTask = text,
                                stageLabel = stageLabel,
                                isCloudBuild = true
                            )
                        )
                    }
                }

                val duration = (System.currentTimeMillis() - startTime).coerceAtLeast(800L)
                cloudResult.fold(
                    onSuccess = { res ->
                        cloudCompiledApkBytes = res.apkBytes
                        _uiState.update {
                            it.copy(
                                apkBuildState = ApkBuildState.Success(
                                    apkFileName = res.apkFileName,
                                    sizeKb = (res.apkBytes.size / 1024).coerceAtLeast(1),
                                    durationMs = duration,
                                    warningsCount = res.warningsCount,
                                    isCloudBuild = true,
                                    qrPayloadUrl = res.downloadUrl
                                )
                            )
                        }
                    },
                    onFailure = { err ->
                        val errMsg = err.localizedMessage ?: "Cloud APK build failed."
                        val errEntry = BuildLogEntry(
                            id = buildLogCounter++,
                            timestamp = timeFormat.format(Date()),
                            text = "e: $errMsg",
                            isError = true
                        )
                        _uiState.update { state ->
                            state.copy(
                                buildLogs = state.buildLogs + errEntry,
                                apkBuildState = ApkBuildState.Failed(
                                    errorSummary = errMsg,
                                    durationMs = duration,
                                    isCloudBuild = true
                                )
                            )
                        }
                    }
                )
            } else {
                // No simulated builds: a real APK needs a real Gradle/Android SDK toolchain.
                val msg = "GitHub is not configured. Open Settings, add a GitHub token, owner and repo " +
                    "to compile a real APK with GitHub Actions."
                val entry = BuildLogEntry(
                    id = buildLogCounter++,
                    timestamp = timeFormat.format(Date()),
                    text = "e: $msg",
                    isError = true
                )
                _uiState.update {
                    it.copy(
                        buildLogs = it.buildLogs + entry,
                        apkBuildState = ApkBuildState.Failed(
                            errorSummary = msg,
                            durationMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L),
                            isCloudBuild = false
                        )
                    )
                }
            }
        }
    }

    fun buildZipBytes(): ByteArray = ProjectPackager.createZipBytes(_uiState.value.files)

    /** Real APK bytes downloaded from GitHub Actions, or empty if no real build exists yet. */
    fun buildApkBytes(): ByteArray = cloudCompiledApkBytes ?: ByteArray(0)

    fun suggestedZipFileName(): String {
        val slug = _uiState.value.projectName.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "forge-project" }
        return "$slug.zip"
    }

    fun suggestedApkFileName(): String {
        val fromSuccess = (_uiState.value.apkBuildState as? ApkBuildState.Success)?.apkFileName
        if (!fromSuccess.isNullOrBlank()) return fromSuccess
        val slug = _uiState.value.projectName.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "forge-app" }
        return "$slug-debug.apk"
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun setStatusMessage(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    private fun scheduleAutoSave(triggerPreviewRefresh: Boolean = false) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(400L)
            val state = _uiState.value
            repository.saveWorkspace(
                mode = state.mode,
                projectName = state.projectName,
                activeFileName = state.activeFileName,
                files = state.files
            )
            _uiState.update {
                it.copy(
                    isSaved = true,
                    refreshNonce = if (triggerPreviewRefresh) it.refreshNonce + 1L else it.refreshNonce
                )
            }
        }
    }

    private fun persistCurrentWorkspaceSync() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveWorkspace(
                mode = state.mode,
                projectName = state.projectName,
                activeFileName = state.activeFileName,
                files = state.files
            )
        }
    }
}
