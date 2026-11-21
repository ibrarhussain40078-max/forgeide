package com.example.data.export

import com.example.data.model.BuildLogEntry
import com.example.data.model.CodeFile
import com.example.data.model.FileLanguage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject

object ProjectPackager {

    fun createZipBytes(files: List<CodeFile>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            files.forEach { file ->
                zos.putNextEntry(ZipEntry(file.name))
                zos.write(file.content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    fun buildCombinedWebsiteHtml(files: List<CodeFile>): String {
        val htmlFile = files.firstOrNull { it.name.equals("index.html", ignoreCase = true) }
            ?: files.firstOrNull { it.language == FileLanguage.HTML }

        val rawHtml = htmlFile?.content ?: """
            <!DOCTYPE html>
            <html><body style="background:#080c14;color:#f1f5f9;font-family:sans-serif;padding:24px;">
            <h2>No index.html found</h2><p>Create an index.html file in the Explorer to preview your website.</p>
            </body></html>
        """.trimIndent()

        val cssFiles = files.filter { it.language == FileLanguage.CSS }
        val jsFiles = files.filter { it.language == FileLanguage.JS }

        var processed = rawHtml

        // Inline matching <link href="...css"> tags or append all CSS into <head>
        cssFiles.forEach { css ->
            val linkRegex = Regex(
                """<link[^>]*href=["'](?:\./)?${Regex.escape(css.name)}["'][^>]*/?>""",
                RegexOption.IGNORE_CASE
            )
            val styleBlock = "<style data-file=\"${css.name}\">\n${css.content}\n</style>"
            if (linkRegex.containsMatchIn(processed)) {
                processed = linkRegex.replace(processed) { styleBlock }
            } else if (processed.contains("</head>", ignoreCase = true)) {
                processed = processed.replaceFirst("</head>", "$styleBlock\n</head>", ignoreCase = true)
            } else {
                processed = "$styleBlock\n$processed"
            }
        }

        val consoleBridgeScript = """
            <script>
            (function() {
              if (window.__forgeConsolePatched) return;
              window.__forgeConsolePatched = true;
              const send = (level, args) => {
                try {
                  const text = Array.from(args).map(a => {
                    if (typeof a === 'object' && a !== null) {
                      try { return JSON.stringify(a); } catch(_) { return String(a); }
                    }
                    return String(a);
                  }).join(' ');
                  if (window.ForgeConsole) window.ForgeConsole.log(level, text);
                } catch(_) {}
              };
              const origLog = console.log;
              const origInfo = console.info;
              const origWarn = console.warn;
              const origErr = console.error;
              console.log = function() { send('LOG', arguments); origLog.apply(console, arguments); };
              console.info = function() { send('INFO', arguments); origInfo.apply(console, arguments); };
              console.warn = function() { send('WARN', arguments); origWarn.apply(console, arguments); };
              console.error = function() { send('ERROR', arguments); origErr.apply(console, arguments); };
              window.addEventListener('error', function(e) {
                if (window.ForgeConsole) {
                  window.ForgeConsole.log('ERROR', (e.message || 'Script error') + ' (line ' + (e.lineno || 0) + ')');
                }
              });
            })();
            </script>
        """.trimIndent()

        processed = if (processed.contains("<head>", ignoreCase = true)) {
            processed.replaceFirst("<head>", "<head>\n$consoleBridgeScript", ignoreCase = true)
        } else {
            "$consoleBridgeScript\n$processed"
        }

        // Inline matching <script src="...js"></script> tags or append before </body>
        jsFiles.forEach { js ->
            val scriptRegex = Regex(
                """<script[^>]*src=["'](?:\./)?${Regex.escape(js.name)}["'][^>]*>\s*</script>""",
                RegexOption.IGNORE_CASE
            )
            val safeJs = js.content.replace("</script>", "<\\/script>", ignoreCase = true)
            val scriptBlock = "<script data-file=\"${js.name}\">\n$safeJs\n</script>"
            if (scriptRegex.containsMatchIn(processed)) {
                processed = scriptRegex.replace(processed) { scriptBlock }
            } else if (processed.contains("</body>", ignoreCase = true)) {
                processed = processed.replaceFirst("</body>", "$scriptBlock\n</body>", ignoreCase = true)
            } else {
                processed = "$processed\n$scriptBlock"
            }
        }

        return processed
    }

    data class ValidationReport(
        val logs: List<BuildLogEntry>,
        val hasErrors: Boolean,
        val warningsCount: Int,
        val errorSummary: String
    )

    /**
     * Fast local pre-flight checks before a real build. Reports only genuine findings; it never
     * pretends to run Gradle tasks. The real compiler runs on GitHub Actions.
     */
    fun validateAndroidBuild(files: List<CodeFile>): ValidationReport {
        val logs = mutableListOf<BuildLogEntry>()
        var idCounter = 1L
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

        fun add(text: String, isError: Boolean = false, isWarning: Boolean = false, isSuccess: Boolean = false) {
            logs.add(BuildLogEntry(idCounter++, fmt.format(Date()), text, isError, isWarning, isSuccess))
        }

        var errors = 0
        var warnings = 0
        var lastError = ""

        val ktFiles = files.filter { it.language == FileLanguage.KOTLIN }
        val manifest = files.firstOrNull { it.name.equals("AndroidManifest.xml", ignoreCase = true) }
        val previewJson = files.firstOrNull { it.name.equals("ui-preview.json", ignoreCase = true) }

        add("Pre-flight checks (${files.size} files)")

        if (ktFiles.isEmpty()) {
            errors++
            lastError = "No Kotlin (.kt) source files found."
            add("e: $lastError", isError = true)
        } else if (ktFiles.none { it.content.contains("MainActivity") }) {
            errors++
            lastError = "No MainActivity found; the app needs an entry-point activity."
            add("e: $lastError", isError = true)
        }

        ktFiles.forEach { kt ->
            val open = kt.content.count { it == '{' }
            val close = kt.content.count { it == '}' }
            if (open != close) {
                warnings++
                add("w: ${kt.name}: brace count differs (open=$open, close=$close); the compiler will confirm.", isWarning = true)
            }
        }

        if (manifest == null) {
            warnings++
            add("w: AndroidManifest.xml missing; a default one will be generated.", isWarning = true)
        } else if (!manifest.content.contains("<application")) {
            warnings++
            add("w: AndroidManifest.xml has no <application>; a default one will be generated.", isWarning = true)
        }

        if (files.any { it.name.endsWith(".kts", ignoreCase = true) }) {
            add("Note: build scripts are managed by ForgeIDE; your build.gradle.kts is not used.")
        }

        if (previewJson != null) {
            try {
                JSONObject(previewJson.content)
            } catch (e: Exception) {
                warnings++
                add("w: ui-preview.json is not valid JSON (only affects the preview): ${e.localizedMessage}", isWarning = true)
            }
        }

        if (errors == 0) add("Pre-flight passed ($warnings warning(s)). Starting the real build on GitHub Actions...")

        return ValidationReport(
            logs = logs,
            hasErrors = errors > 0,
            warningsCount = warnings,
            errorSummary = lastError
        )
    }
}
