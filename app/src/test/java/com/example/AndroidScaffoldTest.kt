package com.example

import com.example.data.model.CodeFile
import com.example.data.model.FileLanguage
import com.example.data.remote.AndroidScaffold
import com.example.data.remote.GitHubBuildService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidScaffoldTest {

    private fun kt(name: String, content: String) = CodeFile(name, content, FileLanguage.KOTLIN)
    private fun other(name: String, content: String) = CodeFile(name, content, FileLanguage.fromFileName(name))

    @Test
    fun `kotlin package is forced to the app namespace`() {
        assertTrue(AndroidScaffold.normalizeKotlin("package com.example.x\n\nclass A").startsWith("package com.forgeide.app"))
        assertTrue(AndroidScaffold.normalizeKotlin("class A").startsWith("package com.forgeide.app"))
    }

    @Test
    fun `manifest drops unresolvable references and exports the launcher`() {
        val raw = """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.example.app">
              <application android:icon="@mipmap/ic_launcher" android:theme="@style/Theme.Custom" android:label="@string/app_name">
                <activity android:name="com.example.app.MainActivity">
                  <intent-filter><action android:name="android.intent.action.MAIN"/></intent-filter>
                </activity>
              </application>
            </manifest>
        """.trimIndent()
        val m = AndroidScaffold.normalizeManifest(raw, "My App")
        assertFalse(m.contains("package="))
        assertFalse(m.contains("@mipmap"))
        assertFalse(m.contains("@style/"))
        assertFalse(m.contains("@string/"))
        assertTrue(m.contains("android:name=\".MainActivity\""))
        assertTrue(m.contains("android:exported=\"true\""))
        assertTrue(m.contains("android:label=\"My App\""))
    }

    @Test
    fun `missing manifest gets a valid default`() {
        val m = AndroidScaffold.normalizeManifest(null, "App & Co")
        assertTrue(m.contains("<application"))
        assertTrue(m.contains("App &amp; Co"))
        assertTrue(m.contains("android.intent.action.MAIN"))
    }

    @Test
    fun `repo files form a complete project and ignore AI build scripts`() {
        val files = listOf(
            kt("MainActivity.kt", "package foo\nclass MainActivity"),
            other("build.gradle.kts", "BROKEN"),
            other("ui-preview.json", "{}"),
        )
        val out = AndroidScaffold.repoFiles("Demo", files)
        assertTrue(out.keys.containsAll(listOf("settings.gradle.kts", "build.gradle.kts", "app/build.gradle.kts", "gradle.properties", "app/src/main/AndroidManifest.xml")))
        assertTrue(out.containsKey("app/src/main/java/com/forgeide/app/MainActivity.kt"))
        assertTrue(out.containsKey("app/src/main/assets/ui-preview.json"))
        assertFalse(out.values.any { it == "BROKEN" })
        assertTrue(out["app/build.gradle.kts"]!!.contains("namespace = \"com.forgeide.app\""))
    }

    @Test
    fun `workflow builds without a wrapper and fails if no apk`() {
        val y = AndroidScaffold.WORKFLOW_YAML
        assertTrue(y.contains("gradle-version: '8.9'"))
        assertTrue(y.contains("gradle assembleDebug"))
        assertTrue(y.contains("if-no-files-found: error"))
    }

    @Test
    fun `error extraction keeps compiler errors and strips timestamps`() {
        val log = """
            2026-11-21T10:00:00.1234567Z > Task :app:compileDebugKotlin FAILED
            2026-11-21T10:00:01.1234567Z e: file:///x/MainActivity.kt:12:5 Unresolved reference: foo
            2026-11-21T10:00:01.2234567Z some noise
            2026-11-21T10:00:02.1234567Z FAILURE: Build failed with an exception.
        """.trimIndent()
        val out = GitHubBuildService.extractErrorLines(log)
        assertTrue(out.contains("e: file:///x/MainActivity.kt:12:5 Unresolved reference: foo"))
        assertTrue(out.contains("FAILURE: Build failed"))
        assertFalse(out.contains("noise"))
        assertFalse(out.contains("2026-11-21"))
        assertEquals(2, out.lines().size)
    }
}
