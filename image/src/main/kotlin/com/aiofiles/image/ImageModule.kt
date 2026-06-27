package com.aiofiles.image

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.aiofiles.core.FileRef
import com.aiofiles.core.FileViewerModule
import com.aiofiles.core.ViewerContext

private const val TAG = "ImageModule"
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 50f
private const val DOUBLE_TAP_ZOOM = 4f

/**
 * Persistent settings for the image viewer module.
 * These survive across file changes.
 */
object ImageSettings {
    var useNearestNeighbor by mutableStateOf(false)
}

/**
 * Image viewer module.
 *
 * Handles common image formats with pinch-to-zoom, pan, and nearest-neighbor
 * rendering toggle for pixel-perfect viewing.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class ImageModule : FileViewerModule {

    override val id = "image"

    override val name = "Image Viewer"

    override val supportedExtensions = listOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg",
        "heic", "heif", "ico", "tiff", "tif"
    )

    override val supportedMimeTypes = listOf(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
        "image/bmp",
        "image/svg+xml",
        "image/x-icon",
        "image/tiff",
        "image/heic",
        "image/heif"
    )

    @Composable
    override fun viewerContent(file: FileRef, context: ViewerContext) {
        var imageState by remember { mutableStateOf<ImageLoadState>(ImageLoadState.Idle) }

        // Reset zoom state when viewing a new image
        LaunchedEffect(file.uri) {
            ZoomState.reset()
        }

        // Use FilterQuality.Low for nearest-neighbor (pixel art) rendering
        val filterQuality = if (ImageSettings.useNearestNeighbor) {
            FilterQuality.Low
        } else {
            FilterQuality.High
        }

        // Include filter quality in the request key so Coil treats it as a different model
        // This forces the painter to be recreated when the setting changes
        val request = remember(file.uri, filterQuality) {
            ImageRequest.Builder(context.context)
                .data(file.uri)
                .crossfade(false)
                .build()
        }

        val painter = rememberAsyncImagePainter(
            model = request,
            contentScale = ContentScale.Fit,
            filterQuality = filterQuality,
            onSuccess = { state ->
                imageState = ImageLoadState.Success
            },
            onError = { state ->
                imageState = ImageLoadState.Error(state.result.throwable.message ?: "Unknown error")
                Log.e(TAG, "Failed to load image: ${file.name}", state.result.throwable)
            },
            onLoading = {
                imageState = ImageLoadState.Loading
            }
        )

        Surface {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = file.name,
                                maxLines = 1
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = context.onBack) {
                                Icon(
                                    painter = painterResource(android.R.drawable.ic_menu_revert),
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                floatingActionButton = {
                    // Reset zoom button - only show when zoomed in
                    if (imageState is ImageLoadState.Success && ZoomState.isSignificantlyZoomed) {
                        FloatingActionButton(
                            onClick = { ZoomState.reset() },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_zoom),
                                contentDescription = "Reset zoom"
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    ZoomableImage(
                        painter = painter,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay loading indicator while image loads
                    if (imageState is ImageLoadState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Show error state overlay
                    if (imageState is ImageLoadState.Error) {
                        ErrorState(error = (imageState as ImageLoadState.Error).message)
                    }
                }
            }
        }
    }

    override val settingsContent: @Composable () -> Unit = {
        ImageSettingsPanel()
    }
}

/**
 * Simplified image load state for UI display logic.
 */
sealed interface ImageLoadState {
    object Idle : ImageLoadState
    object Loading : ImageLoadState
    object Success : ImageLoadState
    data class Error(val message: String) : ImageLoadState
}

/**
 * Mutable zoom state shared across recompositions.
 */
object ZoomState {
    var scale by mutableStateOf(MIN_ZOOM)
    var rotation by mutableStateOf(0f)
    var offset by androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)

    val isSignificantlyZoomed get() = scale > 1.1f

    fun reset() {
        scale = MIN_ZOOM
        rotation = 0f
        offset = androidx.compose.ui.geometry.Offset.Zero
    }
}

@Composable
private fun ZoomableImage(
    painter: androidx.compose.ui.graphics.painter.Painter,
    modifier: Modifier = Modifier
) {
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (ZoomState.scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        ZoomState.scale = newScale
        ZoomState.offset = androidx.compose.ui.geometry.Offset(
            x = ZoomState.offset.x + panChange.x,
            y = ZoomState.offset.y + panChange.y
        )
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .transformable(transformableState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Double-tap zoom: toggle between current and target zoom
                        val targetScale = if (ZoomState.scale > DOUBLE_TAP_ZOOM) MIN_ZOOM else DOUBLE_TAP_ZOOM
                        ZoomState.scale = targetScale
                        ZoomState.offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                )
            }
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.scaleX = ZoomState.scale
                    this.scaleY = ZoomState.scale
                    this.translationX = ZoomState.offset.x
                    this.translationY = ZoomState.offset.y
                }
        )
    }
}

/**
 * Settings panel for the image viewer module.
 */
@Composable
private fun ImageSettingsPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Image Viewer Settings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Nearest Neighbor toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearest Neighbor (Pixel Art)",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = ImageSettings.useNearestNeighbor,
                onCheckedChange = { ImageSettings.useNearestNeighbor = it }
            )
        }
    }
}

@Composable
private fun ErrorState(error: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(android.R.drawable.ic_menu_report_image),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Failed to Load Image",
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
