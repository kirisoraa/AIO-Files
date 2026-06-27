package com.aiofiles.markdown

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiofiles.core.FileRef
import com.aiofiles.core.FileViewerModule
import com.aiofiles.core.ViewerContext
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "MarkdownModule"

/**
 * Persistent settings for the markdown viewer module.
 * These survive across file changes.
 */
object MarkdownSettings {
    var fontSize by mutableStateOf(16f)
    var fontFamily by mutableStateOf(FontFamily.Default)
    var lineHeightMultiplier by mutableStateOf(1.5f)
    var renderLaTeX by mutableStateOf(false)
    var showCodeBlockLineNumbers by mutableStateOf(false)
    var tableWordWrap by mutableStateOf(true)
    var renderInlineImages by mutableStateOf(true)
}

/**
 * Markdown file viewer module.
 */
class MarkdownModule : FileViewerModule {

    override val id = "markdown"

    override val name = "Markdown Viewer"

    override val supportedExtensions = listOf("md", "markdown", "mdx")

    override val supportedMimeTypes = listOf(
        "text/markdown",
        "text/x-markdown"
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
                Log.e(TAG, "Failed to read file: ${file.name}", e)
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
                    MarkdownContent(
                        content = content!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override val settingsContent: @Composable () -> Unit = {
        MarkdownSettingsPanel()
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
        Text(
            "Warning",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "(empty file)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Markdown Rendering ─────────────────────────────────────────────────────

@Composable
private fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val processedContent = remember(content, MarkdownSettings.renderLaTeX) {
        if (MarkdownSettings.renderLaTeX) {
            preprocessLaTeX(content)
        } else {
            content
        }
    }

    Box(modifier.fillMaxSize()) {
        MarkdownText(
            markdown = processedContent,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = MarkdownSettings.fontSize.sp,
                fontFamily = MarkdownSettings.fontFamily,
                lineHeight = (MarkdownSettings.fontSize * MarkdownSettings.lineHeightMultiplier).sp
            ),
            linkColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            isTextSelectable = true,
            onLinkClicked = { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                ctx.startActivity(intent)
            }
        )
    }
}

/**
 * Preprocess markdown content to handle LaTeX blocks.
 * Extracts $$...$$ and \[...\] blocks and replaces them with placeholders.
 * Note: Full LaTeX rendering via MathView requires AndroidView + bitmap generation,
 * which is complex. For MVP, we strip LaTeX blocks and show the raw text.
 * This is a placeholder implementation that can be enhanced later.
 */
private fun preprocessLaTeX(content: String): String {
    // Display math: $$...$$ or \[...\]
    var processed = content.replace(Regex("\\$\\$([\\s\\S]*?)\\$\\$")) { match ->
        "\n\n" + match.value + "\n\n"
    }
    processed = processed.replace(Regex("\\\\\\[([\\s\\S]*?)\\\\\\]")) { match ->
        "\n\n" + match.value + "\n\n"
    }

    // Inline math: $...$ or \(...\)
    processed = processed.replace(Regex("\\$([^$]+)\\$")) { match ->
        "`${match.groupValues[1]}`"
    }
    processed = processed.replace(Regex("\\\\\\(([\\s\\S]*?)\\\\\\)")) { match ->
        "`${match.groupValues[1]}`"
    }

    return processed
}

// ─── Settings Panel ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkdownSettingsPanel() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Markdown Viewer Settings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Font Size slider
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Font Size", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${MarkdownSettings.fontSize.toInt()}sp",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = MarkdownSettings.fontSize,
                onValueChange = { MarkdownSettings.fontSize = it },
                valueRange = 12f..24f,
                steps = 11
            )
        }

        // Font Family segmented button
        Column(Modifier.padding(vertical = 8.dp)) {
            Text("Font Family", style = MaterialTheme.typography.bodyLarge)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    "Sans" to FontFamily.SansSerif,
                    "Serif" to FontFamily.Serif,
                    "Mono" to FontFamily.Monospace
                ).forEach { (label, family) ->
                    val isSelected = MarkdownSettings.fontFamily == family
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = { MarkdownSettings.fontFamily = family },
                        label = { Text(label) },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        // Line Height segmented button
        Column(Modifier.padding(vertical = 8.dp)) {
            Text("Line Height", style = MaterialTheme.typography.bodyLarge)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    "Compact" to 1.2f,
                    "Comfortable" to 1.5f,
                    "Spacious" to 1.8f
                ).forEach { (label, multiplier) ->
                    val isSelected = MarkdownSettings.lineHeightMultiplier == multiplier
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = { MarkdownSettings.lineHeightMultiplier = multiplier },
                        label = { Text(label) },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        // Render LaTeX toggle
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Render LaTeX", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = MarkdownSettings.renderLaTeX,
                onCheckedChange = { MarkdownSettings.renderLaTeX = it }
            )
        }

        // Code Block Line Numbers toggle
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Code Block Line Numbers", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = MarkdownSettings.showCodeBlockLineNumbers,
                onCheckedChange = { MarkdownSettings.showCodeBlockLineNumbers = it }
            )
        }

        // Table Word Wrap toggle
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Table Word Wrap", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = MarkdownSettings.tableWordWrap,
                onCheckedChange = { MarkdownSettings.tableWordWrap = it }
            )
        }

        // Render Inline Images toggle
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Render Inline Images", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = MarkdownSettings.renderInlineImages,
                onCheckedChange = { MarkdownSettings.renderInlineImages = it }
            )
        }
    }
}
