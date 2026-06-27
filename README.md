# AIO-Files
All-In-One Modular File Opener for Android

A modular, view-only file viewer for Android with a Material You (Material 3) interface. The base app discovers and routes to modules, each handling specific file types — starting with text and image viewers.

**Zero GMS dependency** — fully functional on de-googled devices (LineageOS, GrapheneOS, etc.).

## Features

### Text Viewer
- Monospace rendering with configurable line numbers and line wrapping
- Code editor-style line number gutter that stays fixed during horizontal scroll
- Shared horizontal scrolling — all lines scroll together in unwrapped mode
- Settings persist across files (line numbers, wrapping)

### Image Viewer
- High-quality image loading via Coil 2
- Pinch-to-zoom, pan, double-tap zoom
- Nearest-neighbor (pixel-perfect) rendering toggle for pixel art
- Settings persist across files

### Per-Module Settings Panel
- Gear icon in the top app bar opens a `ModalBottomSheet` with module-specific settings
- Settings survive across file changes (singleton state)

## Modules

| Module | Extensions | Features |
|--------|-----------|----------|
| **Text** | `.txt`, `.md`, `.json`, `.xml`, `.log`, `.csv`, `.yaml`, `.yml`, `.ini`, `.cfg`, `.conf`, `.java`, `.kt`, `.py`, `.js`, `.ts`, `.html`, `.css`, `.sh`, `.sql`, `.diff`, `.patch`, … | Monospace rendering, line numbers, line wrapping, horizontal scroll, settings panel |
| **Image** | `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp`, `.bmp`, `.svg`, `.heic`, `.heif`, `.ico`, `.tiff`, `.tif` | Pinch-to-zoom, pan, double-tap zoom, nearest-neighbor toggle, settings panel |

## Architecture

- **`core/`** — Shared contract (`FileViewerModule`, `ModuleRegistry`, `FileRef`, `ViewerContext`)
- **`app/`** — Base application (file picker, navigation, Material 3 theme, settings panel)
- **`text/`** — Text viewer module with persistent settings (`TextSettings`)
- **`image/`** — Image viewer module with persistent settings (`ImageSettings`)

### `FileViewerModule` Interface

```kotlin
interface FileViewerModule {
    val id: String
    val name: String
    val supportedExtensions: List<String>
    val supportedMimeTypes: List<String>

    @Composable
    fun viewerContent(file: FileRef, context: ViewerContext)

    /** Optional settings panel shown in a ModalBottomSheet */
    val settingsContent: (@Composable () -> Unit)? get() = null
}
```

Modules are standard Android library modules registered at runtime via `ModuleRegistry`. Adding a new viewer requires implementing `FileViewerModule` and one line of registration.

### Settings Architecture

Each module exposes an optional `settingsContent` composable. The `ViewerScreen` shows a gear icon in the `TopAppBar` that opens a `ModalBottomSheet` with that content. Settings state is held in singleton `object`s with `mutableStateOf`, persisting across file changes:

```kotlin
object TextSettings {
    var showLineNumbers by mutableStateOf(false)
    var wrapLines by mutableStateOf(true)
}
```

## Building

```bash
./gradlew assembleDebug
```

Requires Android SDK 34+ and JDK 17.

---

> **Disclaimer:** This project is shamelessly vibecoded with the Pi Coding Agent and Qwen3.6-27B. If anything looks weird, it's probably the AI's fault.
