package com.example.ui.preview

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.export.ProjectPackager
import com.example.data.model.CodeFile
import com.example.data.model.ConsoleEntry
import com.example.data.model.ConsoleLevel
import com.example.data.model.ViewportSize
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.IndigoGlow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WebsitePreviewPane(
    files: List<CodeFile>,
    viewportSize: ViewportSize,
    refreshNonce: Long,
    consoleEntries: List<ConsoleEntry>,
    isConsoleExpanded: Boolean,
    isFixingError: Boolean,
    onSelectViewport: (ViewportSize) -> Unit,
    onRefresh: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onToggleConsole: () -> Unit,
    onClearConsole: () -> Unit,
    onFixErrorWithAi: () -> Unit,
    onConsoleMessage: (ConsoleLevel, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val combinedHtml = remember(files, refreshNonce) {
        ProjectPackager.buildCombinedWebsiteHtml(files)
    }

    Column(
        modifier = modifier
            .background(ObsidianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianPanel)
                .border(1.dp, ObsidianBorder)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewportSize.entries.forEach { size ->
                    val selected = size == viewportSize
                    val icon = when (size) {
                        ViewportSize.DESKTOP -> Icons.Default.DesktopWindows
                        ViewportSize.TABLET -> Icons.Default.TabletAndroid
                        ViewportSize.MOBILE -> Icons.Default.PhoneAndroid
                    }
                    Surface(
                        color = if (selected) CyanElectric.copy(alpha = 0.16f) else ObsidianElevated,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            if (selected) CyanElectric else ObsidianBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectViewport(size) }
                            .testTag("viewport_${size.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = size.label,
                                tint = if (selected) CyanElectric else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = size.shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) CyanElectric else TextSecondary
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("preview_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload preview",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = onOpenFullscreen,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("open_new_tab_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open fullscreen preview",
                        tint = CyanElectric,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ObsidianVoid)
                .padding(8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val targetWidth = viewportSize.widthDp?.dp?.coerceAtMost(maxWidth) ?: maxWidth
            Box(
                modifier = Modifier
                    .width(targetWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
            ) {
                SandboxedWebView(
                    htmlContent = combinedHtml,
                    refreshNonce = refreshNonce,
                    onConsoleMessage = onConsoleMessage,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        LiveConsoleDrawer(
            entries = consoleEntries,
            isExpanded = isConsoleExpanded,
            isFixingError = isFixingError,
            onToggle = onToggleConsole,
            onClear = onClearConsole,
            onFixErrorWithAi = onFixErrorWithAi
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SandboxedWebView(
    htmlContent: String,
    refreshNonce: Long,
    onConsoleMessage: (ConsoleLevel, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lastLoadedSignature = remember { arrayOf("") }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(android.graphics.Color.parseColor("#080C14"))

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun log(level: String, message: String) {
                            val mapped = when (level.uppercase()) {
                                "INFO" -> ConsoleLevel.INFO
                                "WARN" -> ConsoleLevel.WARN
                                "ERROR" -> ConsoleLevel.ERROR
                                else -> ConsoleLevel.LOG
                            }
                            post {
                                onConsoleMessage(mapped, message, "app.js")
                            }
                        }
                    },
                    "ForgeConsole"
                )

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        if (consoleMessage != null) {
                            val level = when (consoleMessage.messageLevel()) {
                                ConsoleMessage.MessageLevel.ERROR -> ConsoleLevel.ERROR
                                ConsoleMessage.MessageLevel.WARNING -> ConsoleLevel.WARN
                                ConsoleMessage.MessageLevel.TIP -> ConsoleLevel.INFO
                                else -> ConsoleLevel.LOG
                            }
                            onConsoleMessage(
                                level,
                                consoleMessage.message(),
                                "line ${consoleMessage.lineNumber()}"
                            )
                        }
                        return true
                    }
                }
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            val signature = "$refreshNonce::${htmlContent.hashCode()}"
            if (lastLoadedSignature[0] != signature) {
                lastLoadedSignature[0] = signature
                webView.loadDataWithBaseURL(
                    "https://forgeide.local/",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun LiveConsoleDrawer(
    entries: List<ConsoleEntry>,
    isExpanded: Boolean,
    isFixingError: Boolean,
    onToggle: () -> Unit,
    onClear: () -> Unit,
    onFixErrorWithAi: () -> Unit
) {
    val errorCount = entries.count { it.level == ConsoleLevel.ERROR }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianPanel)
            .border(1.dp, ObsidianBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("console_toggle_row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = CyanElectric,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Live Console (${entries.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                if (errorCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(RoseError.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "$errorCount err",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoseError
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (errorCount > 0) {
                    Button(
                        onClick = onFixErrorWithAi,
                        enabled = !isFixingError,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoGlow,
                            contentColor = ObsidianVoid
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(26.dp)
                            .testTag("web_fix_error_ai_button")
                    ) {
                        if (isFixingError) {
                            CircularProgressIndicator(
                                color = ObsidianVoid,
                                strokeWidth = 1.8.dp,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isFixingError) "Fixing..." else "Fix Error with AI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (isExpanded && entries.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("clear_console_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear console",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = if (isExpanded) "Collapse console" else "Expand console",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .background(ObsidianVoid)
            ) {
                if (entries.isEmpty()) {
                    Text(
                        text = "Console ready. console.log() and runtime errors appear here.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            val color = when (entry.level) {
                                ConsoleLevel.ERROR -> RoseError
                                ConsoleLevel.WARN -> AmberWarn
                                ConsoleLevel.INFO -> CyanElectric
                                ConsoleLevel.LOG -> TextPrimary
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "[${entry.timestamp}] ${entry.level.tag}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = entry.message,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = color,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
