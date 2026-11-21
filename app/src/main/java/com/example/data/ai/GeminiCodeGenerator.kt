package com.example.data.ai

import android.util.Log
import com.example.data.model.CodeFile
import com.example.data.model.IdeMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiCodeGenerator"

data class GeneratedProjectPayload(
    val projectName: String,
    val summary: String,
    val files: List<CodeFile>
)

class GeminiCodeGenerator {

    private val projectResponseSchema = Schema.obj(
        properties = mapOf(
            "projectName" to Schema.string(description = "Short 2-4 word project title"),
            "summary" to Schema.string(description = "Concise 1-sentence summary of the generated app or website"),
            "files" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "File name such as index.html, styles.css, app.js, MainActivity.kt, AndroidManifest.xml, build.gradle.kts, or ui-preview.json"),
                        "content" to Schema.string(description = "Complete working source code for this file")
                    )
                ),
                description = "All project source files"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = projectResponseSchema
                temperature = 0.4f
            },
            systemInstruction = content {
                text(
                    """
                    You are ForgeIDE, an expert full-stack web and native Android Jetpack Compose architect.
                    Generate complete, production-quality, bug-free multi-file projects with zero placeholder TODOs.
                    Never return markdown fences inside JSON string fields; return raw valid source code per file.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun generateProject(
        mode: IdeMode,
        prompt: String,
        existingFiles: List<CodeFile>
    ): Result<GeneratedProjectPayload> {
        return try {
            val modePrompt = buildModePrompt(mode, prompt, existingFiles)
            val response = withTimeout(60_000L) {
                model.generateContent(modePrompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            PayloadParser.parse(rawJson, "Generated Project", "Generated code loaded into workspace.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini generation timed out", e)
            Result.failure(Exception("Generation timed out after 60 seconds. Please try a simpler prompt or retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun fixCodeError(
        mode: IdeMode,
        projectName: String,
        files: List<CodeFile>,
        errorMessage: String
    ): Result<GeneratedProjectPayload> {
        return try {
            val filesDump = files.joinToString("\n\n") { file ->
                "=== FILE: ${file.name} ===\n${file.content}"
            }
            val prompt = """
                TARGET MODE: ${mode.label}
                PROJECT NAME: $projectName
                REPORTED ERROR:
                $errorMessage

                CURRENT PROJECT FILES:
                $filesDump

                TASK:
                Diagnose the error above and return ALL project files with the bug fixed.
                Keep existing features intact and explain the fix concisely in "summary".
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            PayloadParser.parse(rawJson, projectName, "Patched error in project files.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini error fix timed out", e)
            Result.failure(Exception("AI fix timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini error fix failed", e)
            Result.failure(e)
        }
    }

    private fun buildModePrompt(
        mode: IdeMode,
        userPrompt: String,
        existingFiles: List<CodeFile>
    ): String {
        val currentContext = existingFiles.joinToString("\n") { "- ${it.name} (${it.content.length} chars)" }
        return when (mode) {
            IdeMode.WEBSITE -> """
                TARGET MODE: Website Builder (HTML5 + CSS3 + ES6 JavaScript)
                USER REQUEST: "$userPrompt"
                EXISTING FILES:
                $currentContext

                REQUIREMENTS:
                1. Generate complete working files: "index.html", "styles.css", and "app.js" (you may include extra files if helpful).
                2. In index.html, link <link rel="stylesheet" href="styles.css" /> and <script src="app.js"></script>.
                3. Do NOT depend on external CDN scripts that fail offline; write clean modern CSS and vanilla JS so everything works reliably inside a sandboxed WebView.
                4. Make the UI dark-mode or rich modern styled, responsive across mobile (375px), tablet (768px), and desktop, with interactive buttons/forms/logic and helpful console.log() statements so the Live Console captures activity.
            """.trimIndent()

            IdeMode.ANDROID -> """
                TARGET MODE: Android App Builder (Kotlin + Jetpack Compose + Material 3)
                USER REQUEST: "$userPrompt"
                EXISTING FILES:
                $currentContext

                REQUIREMENTS:
                1. Generate these files (the build scripts are provided by the build system; do NOT output build.gradle.kts):
                   - "MainActivity.kt": Complete idiomatic Kotlin + Jetpack Compose Material 3 implementation. Use `package com.forgeide.app`. The launcher activity class must be named MainActivity.
                   - "AndroidManifest.xml": Valid manifest with <application> and a .MainActivity launcher activity (no icon/theme/label resources; none exist).
                   - You may add more .kt files in the same package. Only these libraries are available: Compose BOM (ui, material3, material-icons-extended), activity-compose, lifecycle-viewmodel-compose, navigation-compose, coroutines, core-ktx. No other dependencies, no custom drawables or fonts, minSdk 24.
                   - "ui-preview.json": Complete interactive UI Simulator JSON matching the app you built so the live Android Phone Simulator renders and works interactively!

                2. STRICT SCHEMA FOR "ui-preview.json":
                   {
                     "appName": "App Title",
                     "primaryColor": "#38BDF8",
                     "startScreen": "home",
                     "initialState": { "key1": "string or number or boolean" },
                     "initialLists": { "listKey": ["item 1", "item 2"] },
                     "screens": {
                       "home": {
                         "title": "Screen Title",
                         "elements": [ ... ]
                       }
                     }
                   }

                3. SUPPORTED ELEMENT TYPES IN "elements":
                   - { "type": "heroCard", "badge": "TAG", "title": "{{key}} or text", "subtitle": "..." }
                   - { "type": "text", "text": "{{key}} or text", "style": "headline|title|body|muted" }
                   - { "type": "metricRow", "metrics": [ { "label": "Total", "value": "{{total}}" } ] }
                   - { "type": "calculatorDisplay", "expressionKey": "expr", "resultKey": "result" }
                   - { "type": "keypad", "rows": [ ["7","8","9","/"], ["4","5","6","*"], ["1","2","3","-"], ["C","0","=","+"] ], "exprKey": "expr", "resultKey": "result" }
                   - { "type": "productCard", "title": "Item", "price": "${'$'}49", "subtitle": "Desc", "action": "increment", "key": "cartCount", "step": 1, "buttonLabel": "Add to Cart" }
                   - { "type": "row", "children": [ ...buttons or cards... ] }
                   - { "type": "button", "label": "Label", "action": "increment|decrement|set|toggle|addListItem|navigate|back|toast", "key": "...", "step": 1, "value": "...", "listKey": "...", "inputKey": "...", "target": "screenName", "message": "Toast msg" }
                   - { "type": "outlinedButton", "label": "Label", "action": "...", ... }
                   - { "type": "switchRow", "label": "Label", "key": "boolKey" }
                   - { "type": "slider", "label": "Label", "key": "numKey", "min": 0, "max": 100 }
                   - { "type": "textField", "placeholder": "Enter...", "key": "inputKey" }
                   - { "type": "list", "title": "Section", "listKey": "items" }
                   - { "type": "divider" }

                Make sure all {{key}} placeholders match keys in initialState so the interactive simulator responds immediately to user taps!
            """.trimIndent()
        }
    }
}
