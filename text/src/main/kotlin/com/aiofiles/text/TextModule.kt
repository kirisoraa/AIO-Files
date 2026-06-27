package com.aiofiles.text

import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aiofiles.core.FileRef
import com.aiofiles.core.FileViewerModule
import com.aiofiles.core.ViewerContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "TextModule"

/**
 * Persistent settings for the text viewer module.
 * These survive across file changes.
 */
object TextSettings {
    var showLineNumbers by mutableStateOf(false)
    var wrapLines by mutableStateOf(true)
}

/**
 * Text file viewer module.
 */
class TextModule : FileViewerModule {

    override val id = "text"

    override val name = "Text Viewer"

    override val supportedExtensions = listOf(
        "txt", "md", "markdown", "text",
        "json", "xml", "yaml", "yml", "toml", "csv", "tsv", "ini",
        "cfg", "conf", "config", "properties", "env", "rc",
        "java", "kt", "kts", "py", "js", "ts", "jsx", "tsx",
        "html", "htm", "css", "scss", "less",
        "sh", "bash", "zsh", "bat", "ps1",
        "sql", "graphql",
        "log", "out",
        "diff", "patch", "gitignore", "gitattributes", "editorconfig"
    )

    override val supportedMimeTypes = listOf(
        "text/plain",
        "text/html",
        "text/xml",
        "text/css",
        "text/csv",
        "text/markdown",
        "application/json",
        "application/xml",
        "application/javascript",
        "application/x-yaml"
    )

    @Composable
    override fun viewerContent(file: FileRef, context: ViewerContext) {
        var content by remember { mutableStateOf<String?>(null) }
        var loadError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(file.uri) {
            try {
                content = withContext(Dispatchers.IO) {
                    context.context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                        val maxSize = 50 * 1024 * 1024
                        val buffer = ByteArray(8192)
                        val sb = StringBuilder()
                        var total = 0
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            total += bytesRead
                            if (total > maxSize) {
                                return@withContext null
                            }
                            sb.append(buffer.decodeToString())
                        }
                        sb.toString()
                    } ?: ""
                }

                if (content == null) {
                    loadError = "File is too large to display (>50MB)"
                }
            } catch (e: Exception) {
                Log.e("TextModule", "Failed to read file: ${file.name}", e)
                loadError = "Failed to read file: ${e.message}"
                context.onError("Failed to read file")
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            when {
                content == null && loadError == null -> LoadingState()
                loadError != null -> ErrorState(loadError!!)
                content == "" -> EmptyState()
                else -> {
                    TextContent(
                        content = content!!,
                        showLineNumbers = TextSettings.showLineNumbers,
                        wrapLines = TextSettings.wrapLines,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override val settingsContent: @Composable () -> Unit = {
        TextSettingsPanel()
    }
}

// ─── States ──────────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Warning", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("(empty file)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Text Rendering ──────────────────────────────────────────────────────────

/**
 * Shared text styling for consistent line heights between gutter and content.
 */
@Composable
private fun TextLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

/**
 * Main text rendering composable.
 *
 * Two layout modes:
 *
 * WRAPPED: LazyColumn of Row(gutter | content). Each Row is one logical line.
 *   Content wraps naturally. No horizontal scroll needed.
 *
 * UNWRAPPED: LazyColumn with a single item containing Row(gutterColumn | scrollBox).
 *   gutterColumn: one Text per line number, outside the scroll container.
 *   scrollBox: horizontalScroll wrapping a Column of all content Texts.
 *   Since softWrap=false, each content Text is exactly one visual row,
 *   so gutter numbers align perfectly.
 */
@Composable
private fun TextContent(
    content: String,
    showLineNumbers: Boolean,
    wrapLines: Boolean,
    modifier: Modifier = Modifier
) {
    val lines = content.lines()

    if (wrapLines) {
        TextWrapped(lines = lines, showLineNumbers = showLineNumbers, modifier = modifier)
    } else {
        TextUnwrapped(lines = lines, showLineNumbers = showLineNumbers, modifier = modifier)
    }
}

/**
 * Wrapped mode: each logical line is a Row with optional gutter + wrapping content.
 */
@Composable
private fun TextWrapped(
    lines: List<String>,
    showLineNumbers: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxSize().padding(16.dp)) {
        itemsIndexed(lines, key = { i, _ -> i }) { index, line ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (showLineNumbers) {
                    Text(
                        text = "${index + 1}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(48.dp).padding(end = 16.dp),
                        textAlign = TextAlign.End
                    )
                }
                TextLine(text = line, Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Unwrapped mode: gutter column outside horizontal scroll, all content inside.
 * One horizontal scroll container wraps ALL lines so they scroll together.
 */
@Composable
private fun TextUnwrapped(
    lines: List<String>,
    showLineNumbers: Boolean,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Gutter column — outside horizontal scroll, stays fixed
            if (showLineNumbers) {
                Column(
                    Modifier.width(48.dp).padding(end = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    lines.forEachIndexed { index, _ ->
                        Text(
                            text = "${index + 1}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Content inside horizontal scroll — all lines scroll together
            Box(Modifier.horizontalScroll(horizontalScrollState)) {
                Column {
                    lines.forEach { line ->
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

// ─── Settings Panel ──────────────────────────────────────────────────────────

@Composable
private fun TextSettingsPanel() {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Text Viewer Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Line Numbers", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = TextSettings.showLineNumbers,
                onCheckedChange = { TextSettings.showLineNumbers = it }
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Line Wrapping", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = TextSettings.wrapLines,
                onCheckedChange = { TextSettings.wrapLines = it }
            )
        }
    }
}
