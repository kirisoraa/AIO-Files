package com.aiofiles.app.screen

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aiofiles.core.FileRef
import com.aiofiles.core.FileViewerModule
import com.aiofiles.core.ModuleRegistry
import com.aiofiles.core.ViewerContext
import kotlinx.coroutines.launch

private const val TAG = "ViewerScreen"

/**
 * Screen that displays file content using the appropriate module.
 *
 * Flow:
 * 1. Receives a file URI
 * 2. Resolves it to a FileRef with metadata
 * 3. Queries ModuleRegistry for handlers
 * 4. Renders the module's viewer UI, or shows an error if no handler found
 * 5. Shows module settings in a bottom sheet when available
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    fileUri: Uri,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fileRef by remember { mutableStateOf<FileRef?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    // Resolve file metadata
    LaunchedEffect(fileUri) {
        try {
            Log.d(TAG, "Resolving metadata for: $fileUri")

            val name = context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) else fileUri.lastPathSegment ?: "Unknown"
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else -1L
                    Pair(displayName, size)
                } else {
                    Pair(fileUri.lastPathSegment ?: "Unknown", -1L)
                }
            } ?: Pair(fileUri.lastPathSegment ?: "Unknown", -1L)

            val mimeType = context.contentResolver.getType(fileUri)
            val extension = name.first.substringAfterLast('.', "").lowercase()
                .takeIf { it.isNotEmpty() && it != name.first }

            Log.d(TAG, "Resolved: name=${name.first}, mime=$mimeType, ext=$extension")

            fileRef = FileRef(
                uri = fileUri,
                name = name.first,
                extension = extension,
                mimeType = mimeType,
                size = name.second
            )
            isLoading = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve file metadata", e)
            error = "Failed to read file: ${e.message}"
            isLoading = false
        }
    }

    val handlers = fileRef?.let { ModuleRegistry.findHandlers(it) }.orEmpty()
    val activeModule = handlers.firstOrNull()
    val hasSettings = activeModule?.settingsContent != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileRef?.name ?: "Loading...",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_revert),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (hasSettings) {
                        IconButton(
                            onClick = { showSettings = true }
                        ) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_preferences),
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        FileContentArea(
            isLoading = isLoading,
            error = error,
            fileRef = fileRef,
            handlers = handlers,
            context = context,
            scope = scope,
            snackbarHostState = snackbarHostState,
            onBack = onBack,
            modifier = modifier.padding(paddingValues)
        )
    }

    // Settings bottom sheet
    if (showSettings && hasSettings) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState
        ) {
            activeModule?.settingsContent?.invoke()
        }
    }
}

/**
 * Renders the main content area of the viewer — loading, error, file content, or no-handler state.
 */
@Composable
private fun FileContentArea(
    isLoading: Boolean,
    error: String?,
    fileRef: FileRef?,
    handlers: List<FileViewerModule>,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        error != null -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_dialog_alert),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        fileRef != null -> {
            val viewerContext = ViewerContext(
                context = context,
                resources = context.resources,
                onBack = onBack,
                onError = { message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            )

            when {
                handlers.isEmpty() -> {
                    NoHandlerFoundScreen(
                        fileRef = fileRef,
                        modifier = modifier.fillMaxSize()
                    )
                }

                else -> {
                    Box(modifier = modifier.fillMaxSize()) {
                        handlers[0].viewerContent(fileRef, viewerContext)
                    }
                }
            }
        }
    }
}

/**
 * Screen shown when no module can handle the selected file.
 */
@Composable
private fun NoHandlerFoundScreen(
    fileRef: FileRef,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(android.R.drawable.ic_dialog_alert),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "No Viewer Available",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "No installed module can handle this file type.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // File info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                text = "File: ${fileRef.name}",
                style = MaterialTheme.typography.bodyMedium
            )
            fileRef.extension?.let { ext ->
                Text(
                    text = "Extension: .$ext",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            fileRef.mimeType?.let { mime ->
                Text(
                    text = "MIME Type: $mime",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (fileRef.size >= 0) {
                Text(
                    text = "Size: ${formatBytes(fileRef.size)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Formats bytes into a human-readable string.
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
