# nativelayout

**Create native interfaces from HTML, in any language.**

<a href="https://jitpack.io/#getnativelayout/nativelayout-android"><img src="https://jitpack.io/v/getnativelayout/nativelayout-android.svg" alt="JitPack"></a> <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License: MIT"></a>

A screen is written once, in a plain text file that looks like HTML. The same file works in any language: each language only supplies the Expander, the Engine and the native host. No WebView, no canvas: the file becomes real native views.

The file describes the screen. The logic (state, taps) stays in your language, in a plain loop.

## Why nativelayout

- **HTML you already know**, plus three additions: `{value}`, `if` / `for` lines, and a `<style>` with simple selectors.
- **Native output.** Buttons, fields, lists and images are real platform views.
- **No functions, classes or variables in the file.** State and taps live in your code.
- **Conditions and repetition**, so it is more than a static canvas.
- **Efficient updates.** Each `render` is diffed against the previous screen and only the changes are applied. Text typed in a field is kept.
- **Clear errors** with the line of the `.nl` file.

## Implementations

The `.nl` file is the same everywhere. Each language only needs an Expander, an Engine and a native host.

| Language | Target | Status | Folder |
|---|---|---|---|
| Kotlin | Android (native views) | implemented | [`android/`](android/) |
| Java | Android (native views) | planned | - |
| Python | Kivy | planned | - |
| Python | Tkinter | planned | - |
| Swift | iOS (UIKit) | planned | - |
| JavaScript / TypeScript | React Native | planned | - |
| Dart | Flutter | planned | - |
| C# | .NET MAUI | planned | - |
| Java | Swing / JavaFX (desktop) | planned | - |
| C++ | Qt | planned | - |
| Rust | desktop | planned | - |
| Go | desktop | planned | - |

Each implementation lives in its own folder and is published as a library. Usage guides and examples live in each folder. The Android folder also contains an editor app (`android/app`) with live preview, used to test screens on-device.

Want to add a language? See [Contributing](#contributing).

## Install (Android)

Published through [JitPack](https://jitpack.io/#getnativelayout/nativelayout-android).

Add the JitPack repository (`https://jitpack.io`) to your project:

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

Then add the dependency:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.getnativelayout.nativelayout-android:nativelayout-android:v0.1.1")
}
```

`nativelayout-android` already includes `nativelayout-core`. To use only the parser and Expander (pure JVM), depend on `nativelayout-core` instead. Minimum SDK: 21.

Quick start and examples: [`android/README.md`](android/README.md) and [`android/examples/`](android/examples/).

## API

The same verbs exist in every language.

| Verb | Does |
|---|---|
| `screen(name)` | loads a `.nl` file and returns a screen |
| `render(screen, data)` | shows the screen with the data, waits for a tap, returns `true`; `false` when the window closes |
| `click(target)` | `true` if the `id` (or `".class"`) was tapped |
| `clickIndex()` | position, from 0, of the tapped element of the class |
| `value(id)` | text of a field, `""` if the id does not exist |
| `checked(id)` | state of a checkbox, `false` if it does not exist |
| `setValue(id, text)` | fills or clears a field (`"1"` / `"0"` for a checkbox) |
| `save(key, text)` | stores text on the device |
| `load(key)` | reads stored text |
| `exists(key)` | `true` if the key is stored |
| `delete(key)` | removes a stored key |
| `close()` | releases the host |

Data passed to `render` is plain: text, number, bool, list and map.

## The file format

```
<nativelayout>
    <link rel="stylesheet" href="theme.css">   (zero or more)
    <style> ... </style>                       (optional)
    <body> ... </body>                         (exactly one)
</nativelayout>
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

Values (`{name}`), control lines (`if`, `elif`, `else`, `for`, `end`), the cascade rules, error messages and complete examples are in [`SPEC.md`](SPEC.md).

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
android/     Kotlin implementation (core, android, app), with its own README and examples
SPEC.md      the file format, shared by every language
```

## Contributing

Issues and pull requests are welcome. A new language implementation needs an Expander, an Engine and a native host that follow `SPEC.md`; the `.nl` files must work unchanged. Put it in its own folder, with its own README and examples.

## License

[MIT](LICENSE)