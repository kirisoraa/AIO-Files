package com.aiofiles.core

import android.content.Context
import android.content.res.Resources

/**
 * Context provided by the base app to a module's viewer UI.
 *
 * This encapsulates everything a module needs from the host app:
 * - Android [Context] for accessing system services (ContentResolver, etc.)
 * - [Resources] for string/dimension lookups
 * - Navigation callbacks for back action
 * - Error reporting callback
 *
 * @property context The Android application context.
 * @property resources Android resources for lookups.
 * @property onBack Callback to navigate back / dismiss the viewer.
 * @property onError Callback to report an error to the host app.
 */
data class ViewerContext(
    val context: Context,
    val resources: Resources,
    val onBack: () -> Unit,
    val onError: (String) -> Unit
)
