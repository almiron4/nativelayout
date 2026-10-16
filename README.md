# nativelayout

**Create native interfaces from HTML, in any language.**

<a href="https://jitpack.io/#getnativelayout/nativelayout-android"><img src="https://jitpack.io/v/getnativelayout/nativelayout-android.svg" alt="JitPack"></a> <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License: MIT"></a>

A screen is written once, in a plain text file that looks like HTML. The same file works in any language: each language only supplies the Expander, the Engine and the native host. No WebView, no canvas: the file becomes real native views.

```html
<nativelayout>

<style>
    body { flex: 1; justify-content: center; align-items: center; gap: 1rem; }
    .big { font-size: 3rem; font-weight: bold; }
    .row { flex-direction: row; gap: 0.75rem; }
</style>

<body>
    <p class="big">{n}</p>
    <div class="row">
        <button id="minus">-</button>
        <button id="plus">+</button>
    </div>
</body>

</nativelayout>
```

```kotlin
val counter = screen("counter.nl")
var n = 0

while (render(counter, mapOf("n" to n))) {
    if (click("plus")) n++
    if (click("minus")) n--
}
```

The file describes the screen. The logic stays in your language, in a plain loop.

## Why nativelayout

- **HTML you already know**, plus three additions: `{value}`, `if` / `for` lines, and a `<style>` with simple selectors.
- **Native output.** Buttons, fields, lists and images are real platform views.
- **No functions, classes or variables in the file.** State and taps live in your code.
- **Conditions and repetition**, so it is more than a static canvas.
- **Efficient updates.** Each `render` is diffed against the previous screen and only the changes are applied. Text typed in a field is kept.
- **Clear errors** with the line of the `.nl` file.

## Implementations

| Language | Status | Folder |
|---|---|---|
| Kotlin (Android) | implemented | [`android/`](android/) |
| Python (Kivy, Tkinter) | planned | - |

Each implementation lives in its own folder and is published as a library. The Android folder also contains an editor app (`android/app`) with live preview, used to test screens on-device.

## Install (Android)

Published through [JitPack](https://jitpack.io/#getnativelayout/nativelayout-android).

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.getnativelayout.nativelayout-android:nativelayout-android:v0.1.1")
}
```

`nativelayout-android` already includes `nativelayout-core`. To use only the parser and Expander (pure JVM):

```kotlin
implementation("com.github.getnativelayout.nativelayout-android:nativelayout-core:v0.1.1")
```

Minimum SDK: 21.

## Quick start (Android)

Put `counter.nl` in `src/main/assets/`, then:

```kotlin
class MainActivity : Activity() {
    private lateinit var ui: NativeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val area = FrameLayout(this)
        setContentView(area)

        ui = NativeView(
            this,
            area,
            { name -> runCatching { assets.open(name).bufferedReader().use { it.readText() } }.getOrNull() }
        ) {
            val counter = screen("counter.nl")
            var n = 0

            while (render(counter, mapOf("n" to n))) {
                if (click("plus")) n++
                if (click("minus")) n--
            }
        }
    }

    override fun onDestroy() {
        ui.close()
        super.onDestroy()
    }
}
```

`NativeView(context, area, loader) { ... }` takes the Android context, the `FrameLayout` where the screen is drawn, a loader that returns the text of a file by name (used for `.nl` files and linked `.css`), and the program block. The block is a suspend lambda: `render` suspends until the next tap.

## API

| Verb | Does |
|---|---|
| `screen(name)` | loads a `.nl` file through the loader and returns a `Screen` |
| `render(screen, data)` | shows the screen with the data, waits for a tap, returns `true`; `false` when the window closes |
| `click(target)` | `true` if the `id` (or `".class"`) was tapped |
| `clickIndex()` | position, from 0, of the tapped element of the class |
| `value(id)` | text of a field, `""` if the id does not exist |
| `checked(id)` | state of a checkbox, `false` if it does not exist |
| `setValue(id, text)` | fills or clears a field (`"1"` / `"0"` for a checkbox) |
| `save(key, text)` | stores text on the device |
| `load(key)` | reads stored text, `null` if missing |
| `exists(key)` | `true` if the key is stored |
| `delete(key)` | removes a stored key |
| `close()` | releases the host; call it in `onDestroy` |

Data passed to `render` is plain: text, number, bool, `List` and `Map`.

## The file format

```
<nativelayout>
    <link rel="stylesheet" href="theme.css">   (zero or more)
    <style> ... </style>                       (optional)
    <body> ... </body>                         (exactly one)
</nativelayout>
```

**Values and control**

```html
<p>{task.title} - {pending} pending</p>

if pending == 0:
    <p>Nothing to do.</p>
elif pending < 5:
    <p>Almost there.</p>
else:
    <p>Busy day.</p>
end

for task in tasks:
    <div class="card">{task.title}</div>
end
```

**Tags**

| Tag | Native component |
|---|---|
| `div` | container |
| `span`, `p`, `h1`, `h2`, `h3` | text |
| `button` | button |
| `input` (`text`, `password`, `number`, `checkbox`) | field or checkbox |
| `textarea` | multi-line field |
| `img` | image (`data:` URI or a name resolved by the host) |
| `ul`, `li` | list with bullets |

**Style**

- Selectors: `:root`, `body`, tag, `.class`, `#id`, and comma lists. Variables with `var(--name)`.
- Properties: `color`, `background`, `font-size`, `font-weight`, `padding`, `margin`, `border-radius`, `border`, `display`, `flex-direction`, `flex`, `justify-content`, `align-items`, `gap`, `width`, `height`.
- Units: `rem`, `px` (dp) and `0`. Percentages only in `width` and `height`.
- Not supported in v1: descendant and child selectors, pseudo-classes, attribute selectors, `@media`, `<script>`.

The full format, cascade rules, error messages and five complete examples (counter, login, task list, quiz, settings) are in [`SPEC.md`](SPEC.md).

## How it works

```
panel.nl + data  ->  Expander  ->  HTML with inline styles  ->  Engine  ->  native views
                     (if, for,                                  (parse,      (host)
                      {value}, selectors)                        diff, style)
```

- **Expander:** evaluates control lines and `{values}` with your data and applies the selectors.
- **Engine:** HTML parser, style reader, neutral tree and diff.
- **Host:** creates and updates the native components.

## Repository layout

```
android/
  core/      pure JVM: parser, CSS reader, Expander, neutral tree, diff, host interface
  android/   AndroidLayoutHost and NativeView
  app/       editor with live preview and a demo activity
SPEC.md      the file format, shared by every language
```

Build and publishing details for Android are in [`android/README.md`](android/README.md).

## Contributing

Issues and pull requests are welcome. A new language implementation needs an Expander, an Engine and a native host that follow `SPEC.md`; the `.nl` files must work unchanged.

## License

[MIT](LICENSE)