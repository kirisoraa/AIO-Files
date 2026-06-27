package com.aiofiles.core

import androidx.compose.runtime.Composable

/**
 * Type alias for a module's viewer UI.
 *
 * This is a Composable function that takes a FileRef and ViewerContext
 * and renders the file content.
 */
@Composable
fun FileViewerModule.renderFile(file: FileRef, context: ViewerContext) {
    this.viewerContent(file, context)
}

/**
 * Interface that every file viewer module must implement.
 *
 * A module declares which file types it supports and provides a
 * composable for rendering file content.
 *
 * The base app uses this interface to:
 * 1. Discover which modules can handle a given file
 * 2. Render the file using the module's viewerContent composable
 *
 * Implementations should be lightweight - no heavy initialization
 * in the constructor, as the base app instantiates all modules at startup.
 */
interface FileViewerModule {

    /**
     * Unique identifier for this module (e.g., "text", "image", "pdf").
     * Used for registration and lookup. Must be lowercase, no spaces.
     */
    val id: String

    /**
     * Human-readable display name (e.g., "Text Viewer", "Image Viewer").
     * Shown to the user in module lists and file type pickers.
     */
    val name: String

    /**
     * File extensions this module handles, without dots and lowercase.
     */
    val supportedExtensions: List<String>

    /**
     * MIME types this module handles.
     */
    val supportedMimeTypes: List<String>

    /**
     * Composable that renders the file content for this module.
     *
     * The module is responsible for:
     * - Reading the file content via ViewerContext.context ContentResolver
     * - Rendering the content in a scrollable, themed layout
     * - Handling errors via ViewerContext.onError
     *
     * The base app wraps this in appropriate scaffolding (app bar, back button).
     */
    @Composable
    fun viewerContent(file: FileRef, context: ViewerContext)

    /**
     * Optional composable that renders a settings panel for this module.
     * Return null if this module has no user-facing settings.
     *
     * The returned Composable is displayed in a ModalBottomSheet from the
     * ViewerScreen settings icon. Settings state should be managed in a
     * singleton object so preferences persist across files.
     */
    val settingsContent: (@Composable () -> Unit)? get() = null

    /**
     * Returns true if this module can handle the given file.
     *
     * Default implementation checks extension and MIME type against
     * supportedExtensions and supportedMimeTypes. Override for
     * custom logic (e.g., content sniffing).
     */
    fun canHandle(file: FileRef): Boolean {
        val ext = file.extension
        if (ext != null && supportedExtensions.contains(ext)) {
            return true
        }
        val mime = file.mimeType
        if (mime != null && supportedMimeTypes.contains(mime)) {
            return true
        }
        if (mime != null) {
            for (pattern in supportedMimeTypes) {
                if (pattern.endsWith("/*")) {
                    val typePrefix = pattern.substringBefore("/")
                    if (mime.startsWith("$typePrefix/")) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /** Module metadata derived from this instance. */
    fun toModuleInfo(): ModuleInfo = ModuleInfo(
        id = id,
        name = name,
        supportedExtensions = supportedExtensions,
        supportedMimeTypes = supportedMimeTypes
    )
}
