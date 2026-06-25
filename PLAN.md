# Plan: Module Settings System

## Context

Each module (text, image, etc.) needs a settings panel that lets the user change how file content is displayed. Settings should be accessible from the file display screen and update the view immediately.

**Specific requirements:**
- **Text module**: Toggle line wrapping and line numbers
- **Image module**: Toggle image aliasing (nearest-neighbor rendering for pixel art) — already partially implemented as a top bar button

## Approach

1. Add an optional `settingsContent` property to `FileViewerModule` — a composable factory that produces the settings UI for a module.
2. `ViewerScreen` shows a settings icon in its TopAppBar when the active module provides settings. Tapping it opens a `ModalBottomSheet` with the module's settings panel.
3. Each module manages its own settings state in a singleton `object` (e.g., `TextSettings`, `ImageSettings`) using `mutableStateOf` — this persists across files.
4. Update `TextModule` with line numbers (code editor gutter style) and line wrapping toggles.
5. Move `ImageModule`'s nearest-neighbor toggle from the top bar into the settings panel AND fix the broken nearest-neighbor modifier (use `graphicsLayer { interpolation = Interpolation.Nearest }` instead of no-op `drawWithContent { drawContent() }`).

## Files to Modify

### Core Module
- **`core/src/main/kotlin/com/aiofiles/core/FileViewerModule.kt`** — Add optional `settingsContent` property

### App Module
- **`app/src/main/kotlin/com/aiofiles/app/screen/ViewerScreen.kt`** — Add settings icon + ModalBottomSheet

### Text Module
- **`text/src/main/kotlin/com/aiofiles/text/TextModule.kt`** — Add line numbers, line wrapping, and settings UI

### Image Module
- **`image/src/main/kotlin/com/aiofiles/image/ImageModule.kt`** — Move nearest-neighbor toggle into settings panel

## Reuse

- **`ModalBottomSheet` / `ModalBottomSheetLayout`** from Material 3 — for the settings panel
- **`IconButton`** with settings icon — for the top bar action
- **`Switch`** from Material 3 — for toggle settings
- Existing pattern: `ZoomState` object in ImageModule shows how shared mutable state works across composables

## Steps

### 1. Update `FileViewerModule` interface

Add an optional settings property:

```kotlin
interface FileViewerModule {
    // ... existing properties ...

    /**
     * Optional composable that renders a settings panel for this module.
     * Return null if this module has no user-facing settings.
     *
     * The returned Composable is displayed in a ModalBottomSheet from the
     * ViewerScreen settings icon.
     */
    val settingsContent: (@Composable () -> Unit)? get() = null
}
```

### 2. Update `ViewerScreen` to show module settings

- Add a settings `IconButton` to the TopAppBar `actions` slot (only when the active module has `settingsContent != null`)
- Use `ModalBottomSheetLayout` (or `ModalBottomSheet` as an overlay) to show the settings panel
- Pass the module's `settingsContent` composable into the sheet

Key changes:
- Track `showSettings` state
- Wrap content in `ModalBottomSheetLayout` when module has settings
- Add settings gear icon to TopAppBar actions

### 3. Update `TextModule` with settings

Add persistent settings state (singleton object — persists across files):
```kotlin
object TextSettings {
    var showLineNumbers by mutableStateOf(false)
    var wrapLines by mutableStateOf(true)
}
```

Update the text display:
- **Line numbers**: When enabled, split content into lines and render each line in a `Row` with a monospaced line number gutter on the left (like a code editor). Use a `LazyColumn` for performance with large files.
- **Line wrapping**: When disabled, wrap the text `Text` in `Modifier.horizontalScroll(rememberScrollState())` to allow horizontal scrolling instead of wrapping.

Add settings panel:
- Two `Switch` rows: "Line Numbers" and "Line Wrapping"
- Wire them to `TextSettings.showLineNumbers` and `TextSettings.wrapLines`

### 4. Update `ImageModule` settings

- Move the nearest-neighbor toggle from the TopAppBar `actions` entirely into a settings panel
- Create an `ImageSettings` singleton object with `useNearestNeighbor` state (persists across files)
- Add settings panel with a `Switch` for "Nearest Neighbor (Pixel Art)"
- **Fix the broken nearest-neighbor modifier**: Replace `drawWithContent { drawContent() }` (no-op) with `graphicsLayer { interpolation = Interpolation.Nearest }` which actually disables anti-aliasing for crisp pixel rendering

### 5. Settings persist across files, `ZoomState` resets

Settings (`TextSettings`, `ImageSettings`) are singleton objects — they persist across files by design. Only `ZoomState` (zoom level/offset) should be reset per file via `LaunchedEffect(file.uri)`.

## Verification

- **Text module**: Open a text file → tap settings icon → toggle line numbers on/off → verify line numbers appear/disappear → toggle line wrapping → verify text wraps/scrolls horizontally
- **Image module**: Open a pixel art image → tap settings icon → toggle nearest neighbor → verify crisp pixels vs smooth interpolation
- **Settings panel**: Should slide up as a bottom sheet, dismiss on back tap or tapping outside
- **No settings module**: If a future module has no settings, the settings icon should not appear
- **Nearest neighbor fix**: Verify that toggling nearest neighbor actually produces crisp pixel rendering (not just smooth interpolation)
