package com.example

import com.example.data.ai.PayloadParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PayloadParserTest {

    @Test
    fun `sanitizeFileName rejects traversal and absolute paths`() {
        assertNull(PayloadParser.sanitizeFileName("../../etc/passwd"))
        assertNull(PayloadParser.sanitizeFileName("/abs/path.kt"))
        assertNull(PayloadParser.sanitizeFileName("a/../b.kt"))
        assertNull(PayloadParser.sanitizeFileName("   "))
    }

    @Test
    fun `sanitizeFileName flattens nested paths`() {
        assertEquals("MainActivity.kt", PayloadParser.sanitizeFileName("app/src/MainActivity.kt"))
        assertEquals("styles.css", PayloadParser.sanitizeFileName("css\\styles.css"))
    }

    @Test
    fun `stripCodeFence unwraps fenced content and leaves plain code alone`() {
        assertEquals("val x = 1", PayloadParser.stripCodeFence("```kotlin\nval x = 1\n```"))
        assertEquals("<p>hi</p>", PayloadParser.stripCodeFence("<p>hi</p>"))
    }

    @Test
    fun `parse returns sanitized unique files`() {
        val json = """
            {"projectName":"Demo","summary":"ok","files":[
              {"name":"index.html","content":"<html></html>"},
              {"name":"../evil.js","content":"x"},
              {"name":"index.html","content":"<html>v2</html>"},
              {"name":"app.js","content":"```js\nconsole.log(1)\n```"}
            ]}
        """.trimIndent()
        val payload = PayloadParser.parse(json, "fallback", "fallback summary").getOrThrow()
        assertEquals("Demo", payload.projectName)
        assertEquals(listOf("index.html", "app.js"), payload.files.map { it.name })
        assertEquals("<html>v2</html>", payload.files[0].content)
        assertEquals("console.log(1)", payload.files[1].content)
    }

    @Test
    fun `parse fails cleanly on malformed json or empty file list`() {
        assertTrue(PayloadParser.parse("not json", "a", "b").isFailure)
        assertTrue(PayloadParser.parse("""{"files":[]}""", "a", "b").isFailure)
        assertTrue(PayloadParser.parse("""{"files":[{"name":"../x","content":"y"}]}""", "a", "b").isFailure)
    }

    @Test
    fun `parse falls back to defaults when name or summary missing`() {
        val payload = PayloadParser.parse(
            """{"files":[{"name":"a.kt","content":"x"}]}""", "Fallback", "Fallback summary"
        ).getOrThrow()
        assertEquals("Fallback", payload.projectName)
        assertEquals("Fallback summary", payload.summary)
    }
}
