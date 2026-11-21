package com.example.data.ai

import com.example.data.model.CodeFile
import com.example.data.model.FileLanguage
import org.json.JSONObject

/** Parses and sanitizes Gemini's structured JSON response. Pure logic, unit-testable. */
object PayloadParser {

    private val fenceRegex = Regex("^```[a-zA-Z0-9]*\\s*\\n([\\s\\S]*?)\\n?```\\s*$")

    /**
     * Returns a safe, flat file name or null if the name is unusable.
     * Rejects absolute paths and traversal; flattens nested paths to the last segment.
     */
    fun sanitizeFileName(raw: String): String? {
        val normalized = raw.trim().replace('\\', '/')
        if (normalized.isEmpty() || normalized.startsWith("/")) return null
        if (normalized.split('/').any { it == ".." }) return null
        val last = normalized.substringAfterLast('/').trim()
        if (last.isEmpty() || last == "." || last.any { it.isISOControl() }) return null
        return last.take(120)
    }

    /** Models sometimes wrap file bodies in markdown fences despite instructions; unwrap them. */
    fun stripCodeFence(content: String): String {
        val match = fenceRegex.matchEntire(content.trim()) ?: return content
        return match.groupValues[1]
    }

    fun parse(rawJson: String, fallbackName: String, fallbackSummary: String): Result<GeneratedProjectPayload> {
        return try {
            val root = JSONObject(rawJson)
            val projectName = root.optString("projectName", fallbackName).ifBlank { fallbackName }
            val summary = root.optString("summary", fallbackSummary).ifBlank { fallbackSummary }
            val filesJson = root.getJSONArray("files")

            val byName = LinkedHashMap<String, CodeFile>()
            for (i in 0 until filesJson.length()) {
                val obj = filesJson.getJSONObject(i)
                val name = sanitizeFileName(obj.optString("name")) ?: continue
                val content = stripCodeFence(obj.optString("content"))
                byName[name] = CodeFile(name = name, content = content, language = FileLanguage.fromFileName(name))
            }

            if (byName.isEmpty()) {
                Result.failure(IllegalStateException("Gemini returned no usable project files."))
            } else {
                Result.success(GeneratedProjectPayload(projectName, summary, byName.values.toList()))
            }
        } catch (e: org.json.JSONException) {
            Result.failure(IllegalStateException("Gemini returned malformed JSON. Please retry.", e))
        }
    }
}
