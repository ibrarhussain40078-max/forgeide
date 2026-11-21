package com.example.data.remote

import com.example.data.model.CodeFile

/**
 * Turns the IDE's loose files into a complete, buildable Gradle project.
 * ForgeIDE owns the build scripts (so builds are reproducible); AI-written code is normalized
 * to the fixed package/namespace so the manifest, sources and Gradle config always agree.
 */
object AndroidScaffold {

    const val APP_PACKAGE = "com.forgeide.app"
    const val GRADLE_VERSION = "8.9"
    private const val SRC_DIR = "app/src/main/java/com/forgeide/app/"
    private const val ASSETS_DIR = "app/src/main/assets/"

    /** Repo path prefixes ForgeIDE fully manages; stale files under them are deleted on push. */
    val managedPrefixes = listOf(SRC_DIR, ASSETS_DIR, "app/src/main/res/values/")

    fun repoFiles(projectName: String, files: List<CodeFile>): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        out["settings.gradle.kts"] = SETTINGS
        out["build.gradle.kts"] = ROOT_BUILD
        out["gradle.properties"] = GRADLE_PROPERTIES
        out["app/build.gradle.kts"] = APP_BUILD
        out["app/src/main/AndroidManifest.xml"] = normalizeManifest(
            files.firstOrNull { it.name.equals("AndroidManifest.xml", ignoreCase = true) }?.content,
            projectName
        )
        for (f in files) {
            val n = f.name
            when {
                n.equals("AndroidManifest.xml", true) || n.endsWith(".kts", true) -> Unit // ForgeIDE-owned
                n.endsWith(".kt") -> out[SRC_DIR + n] = normalizeKotlin(f.content)
                n.endsWith(".xml") -> out["app/src/main/res/values/$n"] = f.content
                else -> out[ASSETS_DIR + n] = f.content
            }
        }
        return out
    }

    fun normalizeKotlin(source: String): String {
        val pkg = Regex("(?m)^\\s*package\\s+[\\w.`]+")
        return if (pkg.containsMatchIn(source)) {
            pkg.replaceFirst(source, "package $APP_PACKAGE")
        } else {
            "package $APP_PACKAGE\n\n$source"
        }
    }

    fun normalizeManifest(raw: String?, projectName: String): String {
        val label = projectName.ifBlank { "Forge App" }
            .replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;")
        if (raw.isNullOrBlank() || !raw.contains("<application")) return defaultManifest(label)

        var m: String = raw
        // AGP reads the namespace from Gradle; drop the legacy package attribute and any references
        // to resources the scaffold does not provide (icons, custom themes, string labels).
        m = Regex("\\s+package=\"[^\"]*\"").replace(m, "")
        for (attr in listOf("android:icon", "android:roundIcon", "android:theme", "android:label")) {
            m = Regex("\\s+" + Regex.escape(attr) + "=\"[^\"]*\"").replace(m, "")
        }
        m = m.replaceFirst(
            "<application",
            "<application android:label=\"$label\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\""
        )
        // Activities with intent filters must declare exported on targetSdk 31+; point at our class.
        m = Regex("<activity\\b([^>]*?)(/?)>").replace(m) { r ->
            var attrs = r.groupValues[1]
            attrs = Regex("android:name=\"[^\"]*MainActivity\"").replace(attrs, "android:name=\".MainActivity\"")
            if (!attrs.contains("android:exported")) attrs += " android:exported=\"true\""
            "<activity$attrs${r.groupValues[2]}>"
        }
        return m
    }

    private fun defaultManifest(label: String) = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="$label"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""".trimIndent()

    private const val SETTINGS = """pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "ForgeApp"
include(":app")
"""

    private const val ROOT_BUILD = """plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
"""

    private const val GRADLE_PROPERTIES = """org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
"""

    private const val APP_BUILD = """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.forgeide.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.forgeide.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
"""

    val WORKFLOW_YAML = """
name: Build Debug APK

on:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '$GRADLE_VERSION'

      - name: Build Debug APK
        run: gradle assembleDebug --no-daemon --stacktrace

      - name: Upload Debug APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: app/build/outputs/apk/debug/*.apk
          if-no-files-found: error
""".trimIndent()
}
