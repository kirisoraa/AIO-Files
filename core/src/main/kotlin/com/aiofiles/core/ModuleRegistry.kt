package com.aiofiles.core

/**
 * Central registry for file viewer modules.
 *
 * This singleton manages the lifecycle of all registered modules:
 * - Modules are registered at app startup via [register]
 * - The base app queries [findHandlers] to discover which modules
 *   can handle a given file
 * - [getAllModules] returns metadata for all registered modules
 *
 * Thread-safe for the typical usage pattern (register at startup,
 * read-only queries thereafter).
 */
object ModuleRegistry {

    @Suppress("ObjectPropertyName")
    private val _modules: MutableList<FileViewerModule> = mutableListOf()

    /**
     * All registered modules. Read-only view.
     */
    val modules: List<FileViewerModule>
        get() = _modules.toList()

    /**
     * Register a module with the registry.
     *
     * @param module The module to register.
     * @throws IllegalArgumentException if a module with the same ID is already registered.
     */
    fun register(module: FileViewerModule) {
        if (_modules.any { it.id == module.id }) {
            throw IllegalArgumentException(
                "Module with id '${module.id}' is already registered"
            )
        }
        _modules.add(module)
    }

    /**
     * Find all modules that can handle the given file.
     *
     * @param file The file reference to check.
     * @return List of modules that can handle this file, ordered by registration.
     */
    fun findHandlers(file: FileRef): List<FileViewerModule> {
        return _modules.filter { it.canHandle(file) }
    }

    /**
     * Get metadata for all registered modules.
     *
     * Useful for displaying a list of installed modules in the UI.
     */
    fun getAllModules(): List<ModuleInfo> {
        return _modules.map { it.toModuleInfo() }
    }

    /**
     * Get a specific module by its ID.
     *
     * @param id The module ID to look up.
     * @return The module, or null if not found.
     */
    fun getModule(id: String): FileViewerModule? {
        return _modules.find { it.id == id }
    }

    /**
     * Check if any modules are registered.
     */
    fun isEmpty(): Boolean = _modules.isEmpty()

    /**
     * Get the count of registered modules.
     */
    fun size(): Int = _modules.size
}
