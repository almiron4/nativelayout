# Tasks example (Android)

A task list that exercises most of nativelayout: forms, lists, conditions, filters, taps, a progress bar and on-device storage.

## Files

| File | Goes in | Purpose |
|---|---|---|
| `tasks.nl` | `app/src/main/assets/` | the screen: markup, conditions and loops |
| `tasks.css` | `app/src/main/assets/` | the style, linked from `tasks.nl` with `<link rel="stylesheet">` |
| `MainActivity.kt` | `app/src/main/java/<your package>/` | the logic: state, taps and storage |

`tasks.css` must sit next to `tasks.nl`: the loader passed to `NativeView` resolves both files by name.

## Run it

1. Create an Android project (minimum SDK 21 or higher).
2. Add the JitPack repository and the dependency, as described in the [root README](../../../README.md#install-android).
3. Copy the three files to the locations above.
4. Change the `package` line of `MainActivity.kt` to your project's package.
5. Declare `MainActivity` as the launcher activity in `AndroidManifest.xml`.
6. Build and run.

## What to try

- Tap a card to mark it done or pending. The counters and the progress bar follow.
- Tap `Remove` to delete a task. The tap stays on the button and does not toggle the card.
- Switch between `All`, `Pending` and `Done`. The active chip changes color.
- Type a title and tap `Add`. The field is cleared.
- Tap `Clear completed` to drop every finished task.
- Close and reopen the app. The tasks are still there.

## What it covers

| Feature | Where |
|---|---|
| `{value}` in text and in the `style` attribute | counters, progress bar width |
| `if` / `else`, nested | filter chips, empty state, card states |
| `for` over a list of maps | task cards |
| `.size` on a list | empty state |
| Linked stylesheet and `:root` variables | `tasks.css` |
| Percentage width | progress bar fill |
| Tap by `id` | `add`, `show-all`, `show-pending`, `show-done`, `clear` |
| Tap by class with `clickIndex()` | `.card`, `.remove` |
| `value` and `setValue` | the title field |
| `save`, `load`, `exists` | task persistence |

## How the logic works

The screen is a function of the data. Each loop iteration builds the data from the current state, calls `render`, and then reacts to the tap that woke it:

```kotlin
while (true) {
    val shown = /* tasks filtered by the current filter */
    if (!render(screen, data)) break

    if (click("add")) { /* read the field, add a task, clear the field */ }
    if (click(".card")) { /* toggle shown[clickIndex()] */ }
}
```

Since `clickIndex()` is a position among the tapped class, it always refers to the list that was rendered, which is why the code keeps `shown` instead of indexing `tasks` directly.