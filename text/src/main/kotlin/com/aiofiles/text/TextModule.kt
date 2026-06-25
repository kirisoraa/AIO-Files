package com.aiofiles.text

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aiofiles.core.FileRef
import com.aiofiles.core.FileViewerModule
import com.aiofiles.core.ViewerContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Text file viewer module.
 *
 * Handles plain text files, source code, configuration files, and other
 * text-based formats. Renders content in a monospace font with proper
 * scrolling and Material 3 theming.
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
            color = MaterialTheme.colorScheme.surface
        ) {
            when {
                content == null && loadError == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                loadError != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Warning",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = loadError!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                content == "" -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "(empty file)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    Text(
                        text = content!!,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
