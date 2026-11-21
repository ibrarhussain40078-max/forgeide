package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CodeFile
import com.example.data.model.FileLanguage
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldPass
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
import kotlinx.coroutines.delay

@Composable
fun FileExplorerPane(
    files: List<CodeFile>,
    activeFileName: String,
    onSelectFile: (String) -> Unit,
    onRequestNewFile: () -> Unit,
    onRequestRenameFile: (String) -> Unit,
    onRequestDeleteFile: (String) -> Unit,
    onResetTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(ObsidianSurface)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPLORER",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(
                    onClick = onRequestNewFile,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("new_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create new file",
                        tint = CyanElectric,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onResetTemplate,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("reset_template_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset starter template",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(files, key = { it.name }) { file ->
                val isSelected = file.name == activeFileName
                FileExplorerItem(
                    file = file,
                    isSelected = isSelected,
                    canDelete = files.size > 1,
                    onClick = { onSelectFile(file.name) },
                    onRename = { onRequestRenameFile(file.name) },
                    onDelete = { onRequestDeleteFile(file.name) }
                )
            }
        }
    }
}

@Composable
private fun FileExplorerItem(
    file: CodeFile,
    isSelected: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = if (isSelected) ObsidianElevated else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("explorer_file_${file.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguageBadge(language = file.language)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onRename,
            modifier = Modifier
                .size(32.dp)
                .testTag("rename_file_${file.name}")
        ) {
            Icon(
                imageVector = Icons.Default.DriveFileRenameOutline,
                contentDescription = "Rename ${file.name}",
                tint = TextMuted,
                modifier = Modifier.size(15.dp)
            )
        }
        if (canDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("delete_file_${file.name}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete ${file.name}",
                    tint = TextMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun CodeEditorPane(
    files: List<CodeFile>,
    activeFile: CodeFile?,
    isSaved: Boolean,
    onSelectFile: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onRequestNewFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var copiedJustNow by remember { mutableStateOf(false) }

    LaunchedEffect(copiedJustNow) {
        if (copiedJustNow) {
            delay(1600L)
            copiedJustNow = false
        }
    }

    Column(
        modifier = modifier
            .background(ObsidianVoid)
    ) {
        // Open File Tabs & Copy / Save Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianPanel)
                .border(width = 1.dp, color = ObsidianBorder)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(files, key = { it.name }) { file ->
                    val selected = file.name == activeFile?.name
                    Surface(
                        color = if (selected) ObsidianVoid else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectFile(file.name) }
                            .testTag("editor_tab_${file.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LanguageBadge(language = file.language)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) TextPrimary else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
                item {
                    IconButton(
                        onClick = onRequestNewFile,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New file",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isSaved) "Saved" else "Saving...",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSaved) EmeraldPass else AmberWarn,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(
                onClick = {
                    val code = activeFile?.content.orEmpty()
                    clipboard.setText(AnnotatedString(code))
                    copiedJustNow = true
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(36.dp)
                    .testTag("copy_code_button")
            ) {
                Icon(
                    imageVector = if (copiedJustNow) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = if (copiedJustNow) EmeraldPass else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Line-Numbered Syntax-Highlighted Editor
        if (activeFile == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No file selected", color = TextSecondary)
                }
            }
        } else {
            val verticalScroll = rememberScrollState()
            val horizontalScroll = rememberScrollState()
            val lineCount = remember(activeFile.content) {
                activeFile.content.count { it == '\n' } + 1
            }
            val lineNumbers = remember(lineCount) {
                (1..lineCount).joinToString("\n")
            }
            val codeTextStyle = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = TextPrimary
            )
            val visualTransformation = remember(activeFile.language) {
                SyntaxVisualTransformation(activeFile.language)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(verticalScroll)
                    .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = lineNumbers,
                    style = codeTextStyle.copy(color = TextMuted),
                    modifier = Modifier
                        .width(44.dp)
                        .padding(start = 8.dp, end = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(ObsidianBorder)
                )
                BasicTextField(
                    value = activeFile.content,
                    onValueChange = onContentChange,
                    textStyle = codeTextStyle,
                    cursorBrush = SolidColor(CyanElectric),
                    visualTransformation = visualTransformation,
                    minLines = 22,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScroll)
                        .padding(horizontal = 12.dp)
                        .testTag("code_editor_input")
                )
            }
        }
    }
}

@Composable
fun LanguageBadge(language: FileLanguage) {
    val badgeColor = when (language) {
        FileLanguage.HTML -> Color(0xFFFB923C)
        FileLanguage.CSS -> CyanElectric
        FileLanguage.JS -> AmberWarn
        FileLanguage.KOTLIN -> IndigoGlow
        FileLanguage.XML -> EmeraldPass
        FileLanguage.GRADLE -> Color(0xFF2DD4BF)
        FileLanguage.JSON -> RoseError
        FileLanguage.TEXT -> TextSecondary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeColor.copy(alpha = 0.16f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = language.badge,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = badgeColor
        )
    }
}
