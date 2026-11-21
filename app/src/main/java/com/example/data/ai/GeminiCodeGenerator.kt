package com.example.data.ai

import android.util.Log
import com.example.data.model.CodeFile
import com.example.data.model.IdeMode
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "GeminiCodeGenerator"

data class GeneratedProjectPayload(
    val projectName: String,
    val summary: String,
    val files: List<CodeFile>
)

/**
 * Calls the Gemini API directly with the user's own API key (get one free at aistudio.google.com).
 * No Firebase / App Check involved, so it works in any build of the app.
 */
class GeminiCodeGenerator(
    /** Returns (apiKey, modelName). Read on every call so Settings changes apply immediately. */
    private val settings: () -> Pair<String, String>
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private val systemInstruction = """
        You are ForgeIDE, an expert full-stack web and native Android Jetpack Compose architect.
        Generate complete, production-quality, bug-free multi-file projects with zero placeholder TODOs.
        Never return markdown fences inside JSON string fields; return raw valid source code per file.
    """.trimIndent()

    private fun responseSchema(): JSONObject {
        fun str(desc: String) = JSONObject().put("type", "STRING").put("description", desc)
        val file = JSONObject().put("type", "OBJECT").put(
            "properties",
            JSONObject()
                .put("name", str("File name such as index.html, styles.css, app.js, MainActivity.kt, AndroidManifest.xml or ui-preview.json"))
                .put("content", str("Complete working source code for this file"))
        ).put("required", JSONArray().put("name").put("content"))
        return JSONObject().put("type", "OBJECT").put(
            "properties",
            JSONObject()
                .put("projectName", str("Short 2-4 word project title"))
                .put("summary", str("Concise 1-sentence summary of the generated app or website"))
                .put("files", JSONObject().put("type", "ARRAY").put("items", file).put("description", "All project source files"))
        ).put("required", JSONArray().put("projectName").put("summary").put("files"))
    }

    /** One Gemini REST call. Returns the model's JSON text or a human-readable failure. */
    private suspend fun call(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val (apiKey, modelRaw) = settings()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("No Gemini API key. Open Settings (gear icon), paste a free key from aistudio.google.com, and save.")
            )
        }
        val model = modelRaw.trim().ifBlank { DEFAULT_MODEL }
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", responseSchema())
                    .put("temperature", 0.4)
            )
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", apiKey.trim())
            .post(body.toString().toRequestBody(jsonType))
            .build()
        try {
            client.newCall(request).execute().use { r ->
                val text = r.body?.string().orEmpty()
                if (!r.isSuccessful) {
                    val msg = runCatching { JSONObject(text).getJSONObject("error").getString("message") }.getOrDefault(text.take(200))
                    return@withContext Result.failure(IOException(explain(r.code, msg, model)))
                }
                val root = JSONObject(text)
                val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
                val out = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty()
                if (out.isBlank()) {
                    val reason = candidate?.optString("finishReason").orEmpty().ifBlank { "no content" }
                    Result.failure(IllegalStateException("Gemini returned an empty response ($reason). Try again or simplify the prompt."))
                } else {
                    Result.success(out)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Gemini request failed", e)
            Result.failure(IOException("Network error talking to Gemini: ${e.message}", e))
        }
    }

    private fun explain(code: Int, msg: String, model: String): String = when (code) {
        400 -> "Gemini rejected the request (400): $msg. Check the API key in Settings."
        401, 403 -> "Gemini API key was refused ($code). Create a new key at aistudio.google.com and save it in Settings."
        404 -> "Model '$model' was not found. Change the model name in Settings (try $DEFAULT_MODEL)."
        429 -> "Gemini rate limit or quota reached (429). Wait a minute and retry."
        else -> "Gemini error $code: $msg"
    }

    suspend fun generateProject(
        mode: IdeMode,
        prompt: String,
        existingFiles: List<CodeFile>
    ): Result<GeneratedProjectPayload> =
        call(buildModePrompt(mode, prompt, existingFiles)).fold(
            onSuccess = { PayloadParser.parse(it, "Generated Project", "Generated code loaded into workspace.") },
            onFailure = { Result.failure(it) }
        )

    suspend fun fixCodeError(
        mode: IdeMode,
        projectName: String,
        files: List<CodeFile>,
        errorMessage: String
    ): Result<GeneratedProjectPayload> {
        val filesDump = files.joinToString("\n\n") { "=== FILE: ${it.name} ===\n${it.content}" }
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
        return call(prompt).fold(
            onSuccess = { PayloadParser.parse(it, projectName, "Patched error in project files.") },
            onFailure = { Result.failure(it) }
        )
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
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
