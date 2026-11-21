package com.example.data.remote

import android.util.Base64
import com.example.data.local.GitHubBuildConfig
import com.example.data.model.CodeFile
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class CloudApkBuildResult(
    val apkFileName: String,
    val apkBytes: ByteArray,
    val downloadUrl: String,
    val runHtmlUrl: String,
    val warningsCount: Int
)

/** Builds a real debug APK on GitHub Actions: one commit, one dispatch, real step status, real logs. */
class GitHubBuildService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun pushSourcesAndBuildApk(
        config: GitHubBuildConfig,
        projectName: String,
        files: List<CodeFile>,
        onLog: (text: String, progress: Float, stepLabel: String, isWarning: Boolean) -> Unit
    ): Result<CloudApkBuildResult> = withContext(Dispatchers.IO) {
        try {
            val token = config.token.trim()
            val owner = config.owner.trim()
            val repo = config.repo.trim()
            val branch = config.branch.trim().ifBlank { "main" }
            val workflow = config.workflowFile.trim().ifBlank { "build-apk.yml" }
            val api = "https://api.github.com/repos/$owner/$repo"

            onLog("Connecting to $owner/$repo ($branch)...", 0.05f, STAGE_PUSH, false)

            // Remember the newest existing run so we never mistake an old one for ours.
            val previousLatestRunId = runCatching {
                fetchWorkflowRuns(token, api, workflow, branch).optJSONObject(0)?.getLong("id") ?: 0L
            }.getOrDefault(0L)

            // 1. Commit the complete project (+ workflow if missing/outdated) in ONE commit.
            val repoFiles = AndroidScaffold.repoFiles(projectName, files).toMutableMap()
            val workflowPath = ".github/workflows/$workflow"
            if (fetchFileContent(token, "$api/contents/$workflowPath?ref=$branch")?.trim() !=
                AndroidScaffold.WORKFLOW_YAML.trim()
            ) {
                repoFiles[workflowPath] = AndroidScaffold.WORKFLOW_YAML
                onLog("Installing build workflow $workflowPath", 0.08f, STAGE_PUSH, false)
            }
            onLog("Committing ${repoFiles.size} files in a single commit...", 0.12f, STAGE_PUSH, false)
            val commitSha = commitFiles(token, api, branch, repoFiles, "ForgeIDE: update ${projectName.ifBlank { "app" }}")
            onLog("Pushed commit ${commitSha.take(7)}.", 0.2f, STAGE_PUSH, false)

            // 2. Dispatch the workflow on the branch we just committed to.
            dispatch(token, api, workflow, branch)
            onLog("Dispatched workflow '$workflow'.", 0.22f, STAGE_PUSH, false)

            // 3. Find OUR run (same head commit), then follow its real steps.
            var runId: Long? = null
            var runHtmlUrl = "https://github.com/$owner/$repo/actions"
            var lastLine = ""
            val startedAt = System.currentTimeMillis()

            repeat(MAX_POLLS) { attempt ->
                delay(POLL_MS)
                val elapsed = (System.currentTimeMillis() - startedAt) / 1000

                if (runId == null) {
                    val runs = fetchWorkflowRuns(token, api, workflow, branch)
                    val mine = (0 until runs.length()).map { runs.getJSONObject(it) }
                        .firstOrNull { it.getLong("id") > previousLatestRunId && it.optString("head_sha") == commitSha }
                    if (mine == null) {
                        if (attempt % 3 == 0) onLog("Waiting for GitHub to start the run (${elapsed}s)...", 0.24f, STAGE_PUSH, false)
                        return@repeat
                    }
                    runId = mine.getLong("id")
                    runHtmlUrl = mine.optString("html_url", runHtmlUrl)
                    onLog("Run #$runId started: $runHtmlUrl", 0.26f, STAGE_RUNNER, false)
                }

                val id = runId ?: return@repeat
                val run = jsonGet(token, "$api/actions/runs/$id")
                runHtmlUrl = run.optString("html_url", runHtmlUrl)
                val job = jsonGet(token, "$api/actions/runs/$id/jobs").optJSONArray("jobs")?.optJSONObject(0)
                val steps = job?.optJSONArray("steps") ?: JSONArray()
                val progress = stepProgress(steps)
                val current = currentStep(steps)
                val line = "${current.first} (${elapsed}s)"
                if (line != lastLine) {
                    lastLine = line
                    onLog(line, progress, stageForStep(current.first), false)
                }

                if (run.optString("status") == "completed") {
                    val conclusion = run.optString("conclusion")
                    if (conclusion != "success") {
                        val detail = failureDetail(token, api, job)
                        return@withContext Result.failure(
                            IOException("Build ${conclusion.ifBlank { "failed" }} on GitHub Actions.\n$detail\nRun: $runHtmlUrl")
                        )
                    }
                    onLog("Build succeeded. Downloading APK...", 0.95f, STAGE_UPLOAD, false)
                    val artifact = fetchApkArtifact(token, api, id)
                        ?: return@withContext Result.failure(IOException("Build succeeded but no APK artifact was uploaded."))
                    val zipBytes = download(token, "$api/actions/artifacts/${artifact.getLong("id")}/zip")
                    val apk = extractFirstApkFromZip(zipBytes)
                        ?: return@withContext Result.failure(IOException("Artifact contained no .apk file."))
                    val slug = projectName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "forge-app" }
                    val name = if (apk.first.isBlank()) "$slug-debug.apk" else apk.first
                    onLog("Downloaded $name (${apk.second.size / 1024} KB).", 1.0f, STAGE_UPLOAD, false)
                    return@withContext Result.success(
                        CloudApkBuildResult(name, apk.second, "https://github.com/$owner/$repo/actions/runs/$id", runHtmlUrl, 0)
                    )
                }
            }
            Result.failure(IOException("Timed out after ${MAX_POLLS * POLL_MS / 60000} min waiting for the build. See $runHtmlUrl"))
        } catch (e: IOException) {
            Result.failure(IOException(explain(e.message), e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Installs/updates the workflow file on its own (Settings button). */
    suspend fun pushWorkflowYaml(config: GitHubBuildConfig): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!config.isConfigured) {
                return@withContext Result.failure(IllegalStateException("Enter GitHub token, owner, and repo first."))
            }
            val api = "https://api.github.com/repos/${config.owner.trim()}/${config.repo.trim()}"
            val branch = config.branch.trim().ifBlank { "main" }
            val path = ".github/workflows/${config.workflowFile.trim().ifBlank { "build-apk.yml" }}"
            commitFiles(config.token.trim(), api, branch, mapOf(path to AndroidScaffold.WORKFLOW_YAML), "ForgeIDE: install APK build workflow")
            Result.success("Installed $path on ${config.owner}/${config.repo}")
        } catch (e: Exception) {
            Result.failure(IOException(explain(e.message), e))
        }
    }

    // ---- Git Data API: one atomic commit, stale managed files removed ----

    private fun commitFiles(token: String, api: String, branch: String, files: Map<String, String>, message: String): String {
        val headSha = try {
            jsonGet(token, "$api/git/ref/heads/$branch").getJSONObject("object").getString("sha")
        } catch (e: IOException) {
            throw IOException("Branch '$branch' not found. The repo needs at least one commit on '$branch' (add a README).", e)
        }
        val baseTree = jsonGet(token, "$api/git/commits/$headSha").getJSONObject("tree").getString("sha")

        val entries = JSONArray()
        files.forEach { (path, content) ->
            entries.put(JSONObject().put("path", path).put("mode", "100644").put("type", "blob").put("content", content))
        }
        // Delete files from earlier generations that no longer exist, so they can't break the build.
        val existing = jsonGet(token, "$api/git/trees/$baseTree?recursive=1").optJSONArray("tree") ?: JSONArray()
        for (i in 0 until existing.length()) {
            val node = existing.getJSONObject(i)
            val path = node.optString("path")
            if (node.optString("type") == "blob" && path !in files &&
                AndroidScaffold.managedPrefixes.any { path.startsWith(it) }
            ) {
                entries.put(JSONObject().put("path", path).put("mode", "100644").put("type", "blob").put("sha", JSONObject.NULL))
            }
        }

        val newTree = jsonSend(token, "POST", "$api/git/trees", JSONObject().put("base_tree", baseTree).put("tree", entries)).getString("sha")
        val commit = jsonSend(
            token, "POST", "$api/git/commits",
            JSONObject().put("message", message).put("tree", newTree).put("parents", JSONArray().put(headSha))
        ).getString("sha")
        jsonSend(token, "PATCH", "$api/git/refs/heads/$branch", JSONObject().put("sha", commit))
        return commit
    }

    private fun dispatch(token: String, api: String, workflow: String, branch: String) {
        jsonSend(token, "POST", "$api/actions/workflows/$workflow/dispatches", JSONObject().put("ref", branch), allowEmpty = true)
    }

    // ---- Progress derived from the run's REAL steps ----

    private fun stepProgress(steps: JSONArray): Float {
        if (steps.length() == 0) return 0.28f
        var done = 0
        for (i in 0 until steps.length()) if (steps.getJSONObject(i).optString("status") == "completed") done++
        return 0.28f + 0.62f * done / steps.length()
    }

    private fun currentStep(steps: JSONArray): Pair<String, String> {
        for (i in 0 until steps.length()) {
            val s = steps.getJSONObject(i)
            if (s.optString("status") == "in_progress") return s.optString("name", "Running") to "in_progress"
        }
        for (i in steps.length() - 1 downTo 0) {
            val s = steps.getJSONObject(i)
            if (s.optString("status") == "completed") return s.optString("name", "Step") + " done" to "completed"
        }
        return "Waiting for runner" to "queued"
    }

    private fun stageForStep(name: String): String = when {
        name.contains("Build Debug", true) || name.contains("Gradle", true) && !name.contains("Setup", true) -> STAGE_GRADLE
        name.contains("Upload", true) -> STAGE_UPLOAD
        else -> STAGE_RUNNER
    }

    private fun failureDetail(token: String, api: String, job: JSONObject?): String {
        if (job == null) return "No job details available."
        val steps = job.optJSONArray("steps") ?: JSONArray()
        var failedStep = ""
        for (i in 0 until steps.length()) {
            val s = steps.getJSONObject(i)
            if (s.optString("conclusion") == "failure") { failedStep = s.optString("name"); break }
        }
        val log = runCatching { String(download(token, "$api/actions/jobs/${job.getLong("id")}/logs")) }.getOrDefault("")
        val tail = extractErrorLines(log)
        return buildString {
            if (failedStep.isNotBlank()) append("Failed step: $failedStep\n")
            append(tail.ifBlank { "Open the run on GitHub for the full log." })
        }
    }

    // ---- HTTP helpers ----

    private fun req(token: String, url: String) = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")

    private fun jsonGet(token: String, url: String): JSONObject =
        client.newCall(req(token, url).get().build()).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("GitHub ${r.code} for ${url.substringAfter("github.com")}: ${body.take(300)}")
            JSONObject(body)
        }

    private fun jsonSend(token: String, method: String, url: String, json: JSONObject, allowEmpty: Boolean = false): JSONObject {
        val body = json.toString().toRequestBody(jsonMediaType)
        return client.newCall(req(token, url).method(method, body).build()).execute().use { r ->
            val text = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("GitHub ${r.code} on $method ${url.substringAfter("github.com")}: ${text.take(300)}")
            if (allowEmpty && text.isBlank()) JSONObject() else JSONObject(text)
        }
    }

    private fun download(token: String, url: String): ByteArray =
        client.newCall(req(token, url).get().build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("Download failed: HTTP ${r.code}")
            r.body?.bytes() ?: throw IOException("Empty response body.")
        }

    private fun fetchFileContent(token: String, url: String): String? =
        runCatching {
            val b64 = jsonGet(token, url).optString("content").replace("\n", "")
            if (b64.isBlank()) null else String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
        }.getOrNull()

    private fun fetchWorkflowRuns(token: String, api: String, workflow: String, branch: String): JSONArray =
        jsonGet(token, "$api/actions/workflows/$workflow/runs?event=workflow_dispatch&branch=$branch&per_page=10")
            .optJSONArray("workflow_runs") ?: JSONArray()

    private fun fetchApkArtifact(token: String, api: String, runId: Long): JSONObject? {
        val arr = jsonGet(token, "$api/actions/runs/$runId/artifacts").optJSONArray("artifacts") ?: return null
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return list.firstOrNull { it.optString("name").contains("apk", true) } ?: list.firstOrNull()
    }

    private fun extractFirstApkFromZip(zipBytes: ByteArray): Pair<String, ByteArray>? {
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".apk", true)) {
                    return entry.name.substringAfterLast('/') to zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }
        return null
    }

    private fun explain(message: String?): String {
        val m = message.orEmpty()
        return when {
            " 401 " in m -> "GitHub rejected the token (401). Create a new token and save it in Settings."
            "workflow" in m && (" 403 " in m || " 404 " in m) ->
                "GitHub refused to write the workflow file. The token needs the 'workflow' scope (classic) or 'Workflows: write' (fine-grained). $m"
            " 404 " in m -> "Repository or branch not found, or the token has no access to it. $m"
            else -> m.ifBlank { "GitHub request failed." }
        }
    }

    companion object {
        const val STAGE_PUSH = "Pushing Source"
        const val STAGE_RUNNER = "Preparing Runner"
        const val STAGE_GRADLE = "Running Gradle assembleDebug"
        const val STAGE_UPLOAD = "Uploading APK"
        private const val POLL_MS = 5_000L
        private const val MAX_POLLS = 180 // 15 minutes

        val WORKFLOW_YAML_TEMPLATE: String get() = AndroidScaffold.WORKFLOW_YAML

        /** Keeps the compiler/Gradle error lines from a raw Actions log (strips timestamps). Pure, tested. */
        fun extractErrorLines(log: String, max: Int = 25): String =
            log.lineSequence()
                .map { it.replace(Regex("^\\d{4}-\\d{2}-\\d{2}T[\\d:.]+Z\\s*"), "").trim() }
                .filter { it.startsWith("e: ") || it.startsWith("error:", true) || it.startsWith("FAILURE:") ||
                    it.startsWith("* What went wrong") || it.contains("Execution failed for task") ||
                    it.contains("> Could not") || it.contains("Unresolved reference") }
                .distinct().take(max).joinToString("\n")
    }
}
