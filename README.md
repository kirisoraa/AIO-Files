# AIO-Files
All-In-One Modular File Opener for Android

A modular, view-only file viewer for Android with a Material You (Material 3) interface. The base app discovers and routes to modules, each handling specific file types — starting with text and image viewers.

**Zero GMS dependency** — fully functional on de-googled devices (LineageOS, GrapheneOS, etc.).

## Modules

| Module | Extensions | Features |
|--------|-----------|----------|
| **Text** | `.txt`, `.md`, `.json`, `.xml`, `.log`, `.csv`, `.yaml`, `.yml`, `.ini`, `.cfg`, `.conf` | Monospace rendering, scrollable content |
| **Image** | `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp`, `.bmp`, `.svg`, `.heic`, `.heif`, `.ico`, `.tiff`, `.tif` | Pinch-to-zoom, pan, double-tap zoom, nearest-neighbor toggle |

## Architecture

- **`core/`** — Shared contract (`FileViewerModule`, `ModuleRegistry`, `FileRef`, `ViewerContext`)
- **`app/`** — Base application (file picker, navigation, Material 3 theme)
- **`text/`** — Text viewer module
- **`image/`** — Image viewer module (Coil 2, pinch-to-zoom)

Modules are standard Android library modules registered at runtime via `ModuleRegistry`. Adding a new viewer requires implementing `FileViewerModule` and one line of registration.

## Building

```bash
./gradlew assembleDebug
```

Requires Android SDK 34+ and JDK 17-21.

---

> **Disclaimer:** This project is shamelessly vibecoded with the Pi Coding Agent and Qwen3.6-27B. If anything looks weird, it's probably the AI's fault.
