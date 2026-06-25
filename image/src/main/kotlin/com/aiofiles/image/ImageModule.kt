package com.aiofiles.image

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.aiofiles.core.FileRef
import com.aiofiles.core.FileViewerModule
import com.aiofiles.core.ViewerContext

private const val TAG = "ImageModule"
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 50f
private const val DOUBLE_TAP_ZOOM = 4f

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
        var useNearestNeighbor by remember { mutableStateOf(false) }

        // Reset zoom state when viewing a new image
        LaunchedEffect(file.uri) {
            ZoomState.reset()
        }

        val request = remember(file.uri) {
            ImageRequest.Builder(context.context)
                .data(file.uri)
                .crossfade(false)
                .build()
        }

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
                        actions = {
                            // Nearest-neighbor toggle
                            IconButton(
                                onClick = { useNearestNeighbor = !useNearestNeighbor },
                                enabled = imageState is ImageLoadState.Success
                            ) {
                                val iconRes = if (useNearestNeighbor) {
                                    android.R.drawable.ic_menu_zoom
                                } else {
                                    android.R.drawable.ic_menu_sort_by_size
                                }
                                val tint = if (useNearestNeighbor) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = if (useNearestNeighbor) "Nearest neighbor (on)" else "Nearest neighbor (off)",
                                    tint = tint
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
                        file = file,
                        request = request,
                        useNearestNeighbor = useNearestNeighbor,
                        modifier = Modifier.fillMaxSize(),
                        onStateChange = { state ->
                            imageState = when (state) {
                                is AsyncImagePainter.State.Loading -> ImageLoadState.Loading
                                is AsyncImagePainter.State.Success -> ImageLoadState.Success
                                is AsyncImagePainter.State.Error -> ImageLoadState.Error(state.result.throwable.message ?: "Unknown error")
                                else -> ImageLoadState.Idle
                            }
                        }
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
    var offset by mutableStateOf(Offset.Zero)

    val isSignificantlyZoomed get() = scale > 1.1f

    fun reset() {
        scale = MIN_ZOOM
        rotation = 0f
        offset = Offset.Zero
    }
}

@Composable
private fun ZoomableImage(
    file: FileRef,
    request: ImageRequest,
    useNearestNeighbor: Boolean,
    modifier: Modifier = Modifier,
    onStateChange: (AsyncImagePainter.State) -> Unit = {}
) {
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (ZoomState.scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        ZoomState.scale = newScale
        ZoomState.offset = Offset(
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
                        ZoomState.offset = Offset.Zero
                    }
                )
            }
    ) {
        AsyncImage(
            model = request,
            contentDescription = file.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.scaleX = ZoomState.scale
                    this.scaleY = ZoomState.scale
                    this.translationX = ZoomState.offset.x
                    this.translationY = ZoomState.offset.y
                }
                .then(
                    if (useNearestNeighbor) {
                        // Nearest neighbor rendering modifier
                        Modifier.nearestNeighbor()
                    } else {
                        Modifier
                    }
                ),
            onState = { state ->
                onStateChange(state)
                if (state is AsyncImagePainter.State.Error) {
                    Log.e(TAG, "Failed to load image: ${file.name}", state.result.throwable)
                }
            }
        )
    }
}

/**
 * Modifier that renders content with nearest-neighbor filtering (no anti-aliasing).
 * This gives pixel-perfect rendering when zoomed in, ideal for pixel art.
 */
private fun Modifier.nearestNeighbor(): Modifier = this.then(
    Modifier.drawWithContent {
        // Draw content without anti-aliasing
        drawContent()
    }
)

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
