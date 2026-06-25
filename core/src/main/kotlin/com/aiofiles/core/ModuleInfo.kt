package com.aiofiles.core

/**
 * Metadata describing a file viewer module's capabilities.
 *
 * This is a lightweight data class that can be used to display
 * module information in the UI without needing the full module instance.
 *
 * @property id Unique identifier for this module (e.g., "text", "image").
 * @property name Human-readable display name (e.g., "Text Viewer").
 * @property supportedExtensions File extensions this module handles (without dots, lowercase).
 * @property supportedMimeTypes MIME types this module handles.
 */
data class ModuleInfo(
    val id: String,
    val name: String,
    val supportedExtensions: List<String>,
    val supportedMimeTypes: List<String>
)
