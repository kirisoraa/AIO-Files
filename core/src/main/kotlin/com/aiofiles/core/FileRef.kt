package com.aiofiles.core

import android.net.Uri

/**
 * Immutable reference to a file to be viewed.
 *
 * @property uri The content URI for the file (typically from SAF).
 * @property name Display name of the file.
 * @property extension File extension without the leading dot, lowercase (e.g., "txt").
 * @property mimeType MIME type of the file, or null if unknown.
 * @property size File size in bytes, or -1 if unknown.
 */
data class FileRef(
    val uri: Uri,
    val name: String,
    val extension: String?,
    val mimeType: String?,
    val size: Long
) {
    companion object {
        /**
         * Extracts the extension from a filename.
         * Returns the extension without the dot, lowercase, or null if no extension.
         */
        fun extractExtension(fileName: String): String? {
            val dotIndex = fileName.lastIndexOf('.')
            return if (dotIndex >= 0 && dotIndex < fileName.length - 1) {
                fileName.substring(dotIndex + 1).lowercase()
            } else {
                null
            }
        }
    }
}
