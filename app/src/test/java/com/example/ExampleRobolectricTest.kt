package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.export.ProjectPackager
import com.example.data.model.CodeFile
import com.example.data.model.FileLanguage
import com.example.data.remote.GitHubBuildService
import com.example.data.templates.StarterTemplates
import com.example.data.templates.TemplateGallery
import com.example.ui.components.SimpleQrEncoder
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches ForgeIDE`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ForgeIDE", appName)
  }

  @Test
  fun `zip export packages all starter files`() {
    val files = StarterTemplates.websiteStarterFiles()
    val zipBytes = ProjectPackager.createZipBytes(files)
    val entryNames = mutableListOf<String>()

    ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        entryNames.add(entry.name)
        entry = zis.nextEntry
      }
    }

    assertEquals(listOf("index.html", "styles.css", "app.js"), entryNames)
  }

  @Test
  fun `website bundler inlines css js and console bridge`() {
    val files = StarterTemplates.websiteStarterFiles()
    val html = ProjectPackager.buildCombinedWebsiteHtml(files)
    assertTrue(html.contains("window.ForgeConsole"))
    assertTrue(html.contains("data-file=\"styles.css\""))
    assertTrue(html.contains("data-file=\"app.js\""))
  }

  @Test
  fun `android validator passes starter and catches unbalanced braces`() {
    val starter = StarterTemplates.androidStarterFiles()
    val passReport = ProjectPackager.validateAndroidBuild(starter)
    assertFalse(passReport.hasErrors)

    val brokenFiles = starter.map {
      if (it.name == "MainActivity.kt") {
        CodeFile("MainActivity.kt", "package com.test\nclass Broken {", FileLanguage.KOTLIN)
      } else {
        it
      }
    }
    val failReport = ProjectPackager.validateAndroidBuild(brokenFiles)
    assertTrue(failReport.hasErrors)
    assertTrue(failReport.errorSummary.contains("Unbalanced braces"))
  }

  @Test
  fun `template gallery provides 4 starter templates`() {
    val templates = TemplateGallery.allTemplates
    assertEquals(4, templates.size)
    assertTrue(templates.all { it.files.isNotEmpty() })
  }

  @Test
  fun `workflow yaml contains jdk 17 gradle and artifact upload`() {
    val yaml = GitHubBuildService.WORKFLOW_YAML_TEMPLATE
    assertTrue(yaml.contains("actions/setup-java@v4"))
    assertTrue(yaml.contains("java-version: '17'"))
    assertTrue(yaml.contains("gradle/actions/setup-gradle@v4"))
    assertTrue(yaml.contains("gradle assembleDebug"))
    assertTrue(yaml.contains("actions/upload-artifact@v4"))
  }

  @Test
  fun `qr encoder produces valid square finder pattern matrix`() {
    val matrix = SimpleQrEncoder.encode("https://github.com/octocat/app/actions/runs/123")
    assertTrue(matrix.isNotEmpty())
    assertEquals(matrix.size, matrix[0].size)
    assertTrue(matrix[0][0]) // top-left finder corner
    assertTrue(matrix[0][matrix.size - 1]) // top-right finder corner
    assertTrue(matrix[matrix.size - 1][0]) // bottom-left finder corner
  }
}
