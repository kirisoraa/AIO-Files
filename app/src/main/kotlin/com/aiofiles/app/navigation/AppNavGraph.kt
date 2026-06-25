package com.aiofiles.app.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aiofiles.app.screen.FilePickerScreen
import com.aiofiles.app.screen.ModulesScreen
import com.aiofiles.app.screen.ViewerScreen

private const val TAG = "AppNavGraph"

/**
 * Top-level navigation destinations.
 */
sealed class Screen(val route: String) {
    object FilePicker : Screen("file_picker")
    object Viewer : Screen("viewer") {
        fun createRoute(uri: Uri): String {
            // Store URI in a shared holder instead of passing through route
            FileUriHolder.set(uri)
            return "viewer"
        }
    }
    object Modules : Screen("modules")
}

/**
 * Simple holder for the currently selected file URI.
 * Used to pass URIs between screens without Navigation encoding issues.
 */
object FileUriHolder {
    @Volatile
    private var uri: Uri? = null

    fun set(uri: Uri) {
        this.uri = uri
    }

    fun get(): Uri? = uri

    fun clear() {
        uri = null
    }
}

/**
 * Root navigation graph for the app.
 *
 * Routes:
 * - FilePicker: Home screen for picking files
 * - Viewer: Shows file content via the appropriate module
 * - Modules: Lists all installed modules
 */
@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.FilePicker.route,
        modifier = modifier
    ) {
        // File picker - home screen
        composable(Screen.FilePicker.route) {
            FilePickerScreen(
                onFileSelected = { uri ->
                    // Remove any stale viewer/modules entries, then navigate fresh
                    navController.navigate(Screen.Viewer.createRoute(uri)) {
                        popUpTo(Screen.FilePicker.route) { inclusive = false }
                    }
                },
                onModulesClick = {
                    navController.navigate(Screen.Modules.route) {
                        popUpTo(Screen.FilePicker.route) { inclusive = false }
                    }
                }
            )
        }

        // Viewer screen - gets URI from shared holder
        composable(Screen.Viewer.route) {
            val fileUri = FileUriHolder.get()
            if (fileUri == null) {
                Log.e(TAG, "No file URI in holder")
                navController.popBackStack()
                return@composable
            }

            Log.d(TAG, "Navigating to viewer with URI: $fileUri")

            ViewerScreen(
                fileUri = fileUri,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Modules screen - lists installed modules
        composable(Screen.Modules.route) {
            ModulesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
