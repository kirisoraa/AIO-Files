# Markdown Viewer Module

## Context
The app currently has text and image modules. The user wants a dedicated markdown module that renders `.md` files with full formatting: tables, lists, text formatting, code blocks, images, and LaTeX math.

Currently, `.md` files are handled by the Text module (plain text). A dedicated markdown module would provide rich rendering.

## Library Choice
**`compose-markdown`** by jeziellago (v0.7.2 on JitPack)
- Group: `com.github.jeziellago`
- Artifact: `compose-markdown`
- License: MIT
- Supports: CommonMark, tables, task lists, code blocks, images, links, HTML, text highlighting, text selection, auto-size, link click handling
- TextView-based renderer wrapped in Compose — well-maintained, 456+ stars

## LaTeX Math Rendering
`compose-markdown` doesn't support LaTeX natively. Two approaches:

**Option A — Preprocessing + MathView (recommended for MVP)**
- Use `Nishant-Pathak/MathView` (JitPack, v1.1, KaTeX-based) to render LaTeX blocks as bitmaps
- Preprocess markdown: extract `$$...$$` and `\[...\]` blocks, render as images, replace with inline markdown images
- Pros: offline, fast, no WebView needed
- Cons: preprocessing complexity, MathView is XML-based (needs `AndroidView` wrapper for bitmap generation)

**Option B — WebView fallback**
- Render entire markdown in a WebView with Goldmark + KaTeX JS library
- Pros: full LaTeX support, no preprocessing
- Cons: loses Compose integration, heavier

**Recommendation**: Start with Option A. Preprocess markdown to extract LaTeX blocks, render them as PNG bitmaps using MathView, and replace with inline images. Users toggle via settings.

## Settings Ideas

| Setting | Description | Default |
|---------|-------------|---------|
| **Font Size** | Body text size (12sp–24sp, slider) | 16sp |
| **Font Family** | Serif / Sans-serif / Monospace | Sans-serif |
| **Line Height** | Compact / Comfortable / Spacious (1.2x / 1.5x / 1.8x) | Comfortable |
| **Render LaTeX** | Toggle LaTeX math rendering ($$...$$, \[...\]) | Off |
| **Code Block Line Numbers** | Show line numbers in fenced code blocks | Off |
| **Table Word Wrap** | Enable word wrapping inside markdown tables | On |
| **Render Inline Images** | Show/hide inline images embedded in markdown | On |

## Files to Create/Modify

### New files
- `markdown/build.gradle.kts` — Module build config
- `markdown/src/main/kotlin/com/aiofiles/markdown/MarkdownModule.kt` — Module implementation
- `markdown/src/main/AndroidManifest.xml` — Module manifest (empty, library module)

### Modified files
- `settings.gradle.kts` — Add `:markdown` module + JitPack repo
- `app/build.gradle.kts` — Add `implementation(project(":markdown"))`
- `app/src/main/kotlin/com/aiofiles/app/AioFilesApplication.kt` — Register MarkdownModule
- `README.md` — Document the new module
- `gradle/libs.versions.toml` — Add JitPack repo reference (or add in settings.gradle.kts)

## Steps

### 1. Create markdown module structure
- [ ] Create `markdown/` directory with `build.gradle.kts` and `src/main/AndroidManifest.xml`
- [ ] Use same pattern as `text/` module (library module, Compose enabled, minSdk 26)
- [ ] Add JitPack repository in `settings.gradle.kts`
- [ ] Add `compose-markdown` dependency: `com.github.jeziellago:compose-markdown:0.7.2`
- [ ] Add `MathView` dependency for LaTeX: `com.github.Nishant-Pathak:MathView:v1.1`

### 2. Implement MarkdownModule
- [ ] Create `MarkdownModule` class implementing `FileViewerModule`
- [ ] ID: `"markdown"`, Name: `"Markdown Viewer"`
- [ ] Extensions: `["md", "markdown", "mdx"]`
- [ ] MIME types: `["text/markdown", "text/x-markdown", "text/plain"]`
- [ ] Read file content via `ViewerContext` (same pattern as TextModule)
- [ ] Render using `MarkdownText` composable with settings-driven `TextStyle`
- [ ] Handle links via `onLinkClicked` — open in browser via Intent
- [ ] Handle loading/error/empty states (same pattern as TextModule)

### 3. Implement MarkdownSettings singleton
- [ ] `fontSize: Float` (12–24sp, default 16)
- [ ] `fontFamily: FontFamily` (Sans-serif default)
- [ ] `lineHeightMultiplier: Float` (1.2, 1.5, 1.8, default 1.5)
- [ ] `renderLaTeX: Boolean` (default false)
- [ ] `showCodeBlockLineNumbers: Boolean` (default false)
- [ ] `tableWordWrap: Boolean` (default true)
- [ ] `renderInlineImages: Boolean` (default true)
- [ ] Links always use accent color (hardcoded, no setting)

### 4. Implement settings panel
- [ ] Font size slider (12–24sp)
- [ ] Font family segmented button (Serif / Sans / Mono)
- [ ] Line height segmented button (Compact / Comfortable / Spacious)
- [ ] Render LaTeX toggle
- [ ] Code block line numbers toggle
- [ ] Table word wrap toggle
- [ ] Render inline images toggle
- [ ] Links always use accent color (no toggle needed)

### 5. Implement LaTeX preprocessing (behind toggle)
- [ ] Regex to extract `$$...$$` and `\[...\]` blocks
- [ ] Use MathView to render each block as a bitmap
- [ ] Replace LaTeX blocks with inline markdown image syntax
- [ ] Handle inline LaTeX `$...$` and `\(...\)` similarly
- [ ] Cache rendered LaTeX bitmaps to avoid re-rendering

### 6. Wire up module registration
- [ ] Add `:markdown` to `settings.gradle.kts`
- [ ] Add `implementation(project(":markdown"))` to `app/build.gradle.kts`
- [ ] Register `MarkdownModule()` in `AioFilesApplication.onCreate()`
- [ ] Import and register alongside existing modules

### 7. Update documentation
- [ ] Update `README.md` with markdown module details
- [ ] Update module table with features

## Verification
- [ ] `./gradlew assembleDebug` builds successfully
- [ ] Opening a `.md` file renders formatted markdown (headings, bold, lists, tables)
- [ ] Settings panel shows all toggles and controls
- [ ] Font size slider changes text size immediately
- [ ] Font family switch changes font immediately
- [ ] LaTeX toggle renders math blocks when enabled
- [ ] Links open in browser
- [ ] Code blocks render with optional line numbers
- [ ] Tables render correctly with optional word wrap
- [ ] Settings persist across file changes

## Risks & Tradeoffs
- **MathView is XML-based**: Need `AndroidView` wrapper for bitmap generation. May need to run on background thread.
- **JitPack dependencies**: `compose-markdown` and `MathView` are on JitPack, not Maven Central. JitPack builds on-demand which can be slow on first fetch.
- **LaTeX preprocessing**: Regex-based extraction may miss edge cases (nested delimiters, escaped characters). Good enough for MVP but may need a proper parser later.
- **compose-markdown is TextView-based**: Can't do custom Compose rendering inside markdown. Limited control over code block styling (no syntax highlighting out of the box).
