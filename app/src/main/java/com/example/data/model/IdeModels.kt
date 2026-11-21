package com.example.data.model

enum class IdeMode(val label: String) {
    WEBSITE("Website Builder"),
    ANDROID("Android App Builder")
}

enum class ViewportSize(val label: String, val shortLabel: String, val widthDp: Int?) {
    DESKTOP("Desktop 100%", "Desktop", null),
    TABLET("Tablet 768px", "768px", 768),
    MOBILE("Mobile 375px", "375px", 375)
}

enum class AndroidPreviewTab(val label: String) {
    SIMULATOR("Interactive UI Simulator"),
    BUILD_CONSOLE("APK Build Console")
}

enum class CompactWorkspaceTab(val label: String) {
    FILES("Files"),
    CODE("Code"),
    PREVIEW("Preview")
}

enum class ConsoleLevel(val tag: String) {
    LOG("LOG"),
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERR")
}

data class ConsoleEntry(
    val id: Long,
    val timestamp: String,
    val level: ConsoleLevel,
    val message: String,
    val source: String = ""
)

enum class FileLanguage(val badge: String) {
    HTML("HTML"),
    CSS("CSS"),
    JS("JS"),
    KOTLIN("KT"),
    XML("XML"),
    GRADLE("KTS"),
    JSON("JSON"),
    TEXT("TXT");

    companion object {
        fun fromFileName(fileName: String): FileLanguage {
            val lower = fileName.lowercase()
            return when {
                lower.endsWith(".html") || lower.endsWith(".htm") -> HTML
                lower.endsWith(".css") -> CSS
                lower.endsWith(".js") || lower.endsWith(".mjs") -> JS
                lower.endsWith(".kt") -> KOTLIN
                lower.endsWith(".xml") -> XML
                lower.endsWith(".kts") || lower.endsWith(".gradle") -> GRADLE
                lower.endsWith(".json") -> JSON
                else -> TEXT
            }
        }
    }
}

data class CodeFile(
    val name: String,
    val content: String,
    val language: FileLanguage = FileLanguage.fromFileName(name)
)

data class BuildLogEntry(
    val id: Long,
    val timestamp: String,
    val text: String,
    val isError: Boolean = false,
    val isWarning: Boolean = false,
    val isSuccess: Boolean = false
)

sealed interface AiGenerationState {
    data object Idle : AiGenerationState
    data class Loading(val prompt: String) : AiGenerationState
    data class Success(val summary: String) : AiGenerationState
    data class Error(val message: String) : AiGenerationState
}

sealed interface ApkBuildState {
    data object Idle : ApkBuildState
    data class Building(
        val progress: Float,
        val currentTask: String,
        val stageLabel: String = "Preparing",
        val isCloudBuild: Boolean = false
    ) : ApkBuildState
    data class Success(
        val apkFileName: String,
        val sizeKb: Int,
        val durationMs: Long,
        val warningsCount: Int,
        val isCloudBuild: Boolean = false,
        val qrPayloadUrl: String = ""
    ) : ApkBuildState
    data class Failed(
        val errorSummary: String,
        val durationMs: Long,
        val isCloudBuild: Boolean = false
    ) : ApkBuildState
}
