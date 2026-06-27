# Plan: Rewrite TextModule

## Problem

The text module has a fundamental architectural conflict:

- **LazyColumn + Row(gutter | content)**: Line numbers align perfectly with content, but `horizontalScroll` on individual `Text` elements means short lines don't scroll and gestures only work on overflow lines.
- **Column + Row(gutter column | content box)**: One `horizontalScroll` wrapper works for all lines, but gutter numbers don't align with content because separate `Column`s don't share row heights.

Every iterative fix broke something else. The module needs a clean rewrite.

## Approach

Use `LazyColumn` where each item is a `Row(gutter | content)` for perfect alignment. For horizontal scrolling, **capture drag gestures on the Row itself** (not individual Texts) and apply a **shared horizontal offset** to ALL content Texts via `Modifier.offset`.

### Structure

```
LazyColumn (vertical scroll + padding)
  itemsIndexed { index, line ->
    Row (captures horizontal drag gestures)
      ├── Text(line number)     -- fixed width, NOT shifted
      └── Text(line content)    -- shifted via Modifier.offset(sharedOffset)
  }
```

### Key insight

`Modifier.pointerInput { detectHorizontalDragGestures { ... } }` on the Row captures gestures from ANY line (short or long). The shared `horizontalOffset` state is applied to every content `Text` via `Modifier.offset { IntOffset(offset, 0) }`. All lines shift together.

When wrapping is ON, no horizontal scroll — the gesture modifier is skipped entirely.

## Files to Modify

- `text/src/main/kotlin/com/aiofiles/text/TextModule.kt` — full rewrite

## Changes

1. **Remove** `TextWithLineNumbers` and `TextPlain` — replace with single `TextContent` composable
2. **`TextContent`**: Takes `content`, `showLineNumbers`, `wrapLines` parameters
3. **Horizontal scroll**: `var horizontalOffset by remember { mutableStateOf(0f) }` shared across all lines
4. **Gesture capture**: `Modifier.pointerInput` on each Row with `detectHorizontalDragGestures` (only when unwrapped)
5. **Content shift**: `Modifier.offset { IntOffset(horizontalOffset.toInt(), 0) }` on each content Text (only when unwrapped)
6. **Gutter**: Only rendered when `showLineNumbers` is true

## Verification

- Build succeeds
- Line numbers align with content start (even when wrapped)
- All lines scroll together horizontally (including short lines)
- Horizontal gesture works from any line
- Wrapping mode: no horizontal scroll, content wraps naturally
- No crashes when toggling settings
