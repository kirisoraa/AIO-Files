package com.aiofiles.app

import android.app.Application
import android.util.Log
import com.aiofiles.core.ModuleRegistry
import com.aiofiles.image.ImageModule
import com.aiofiles.markdown.MarkdownModule
import com.aiofiles.text.TextModule

private const val TAG = "AioFilesApp"

/**
 * Application class that initializes the module registry.
 *
 * All file viewer modules are registered here at startup.
 * To add a new module:
 * 1. Create a module class implementing FileViewerModule
 * 2. Add it as a dependency in app/build.gradle.kts
 * 3. Register it below with ModuleRegistry.register()
 */
class AioFilesApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Register all available modules
        ModuleRegistry.register(ImageModule())
        ModuleRegistry.register(MarkdownModule())
        ModuleRegistry.register(TextModule())

        Log.d(TAG, "Registered ${ModuleRegistry.size()} module(s)")
        ModuleRegistry.modules.forEach { module ->
            Log.d(TAG, "  - ${module.id}: ${module.name} (${module.supportedExtensions.size} extensions)")
        }
    }
}
