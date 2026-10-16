# nativelayout - android

Kotlin implementation of nativelayout. It turns an `.nl` file into real Android `View` objects, with no WebView.

## Modules

| Module | Type | Contents |
|---|---|---|
| `core` | pure JVM | HTML parser, CSS reader, `<link>` resolver, Expander (`if` / `for` / `{value}`), neutral tree, diff, host interface |
| `android` | Android library | `AndroidLayoutHost` (tree to real Views) and `NativeView` (the `screen()` / `render()` / `click()` API from the spec, section 8) |
| `app` | Android app | the editor (code, data, preview, file tree) and `DemoActivity`, a worked example built on `NativeView` |

## Install

Published through [JitPack](https://jitpack.io/#getnativelayout/nativelayout).

**1. Add the JitPack repository**

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

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

If your project has no `dependencyResolutionManagement`, put the same line in `allprojects { repositories { ... } }` of the root `build.gradle`.

**2. Add the dependency**

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.getnativelayout.nativelayout:nativelayout-android:v0.1.2")
}
```

```groovy
// app/build.gradle
dependencies {
    implementation 'com.github.getnativelayout.nativelayout:nativelayout-android:v0.1.2'
}
```

| Part | Value |
|---|---|
| group | `com.github.getnativelayout.nativelayout` |
| artifact | `nativelayout-android` or `nativelayout-core` |
| version | a tag of the repository, for example `v0.1.2` |

`nativelayout-android` already includes `nativelayout-core`. Depend on `nativelayout-core` alone only if you want the parser and Expander on a plain JVM. Minimum SDK: 21.

## Quick start

Put `counter.nl` in `app/src/main/assets/`:

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

Then the activity:

```kotlin
import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import co.adilson889.nativelayout.android.NativeView

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

`NativeView(context, area, loader) { ... }` takes:

| Parameter | Meaning |
|---|---|
| `context` | the Android context |
| `area` | the `FrameLayout` where the screen is drawn |
| `loader` | returns the text of a file by name, or `null`; used for `.nl` files and linked `.css` |
| block | the program: a suspend lambda, where `render` suspends until the next tap |

Always call `ui.close()` in `onDestroy`.

## API

| Function | Does |
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
| `close()` | releases the host |

Data passed to `render` is plain: `String`, numbers, `Boolean`, `List` and `Map`.

Errors in a `.nl` file are shown on screen in red, with the line of the file.

## Examples

| Example | Covers |
|---|---|
| [`examples/tasks/`](examples/tasks/) | forms, lists, conditions, filters, class taps, progress bar, on-device storage, linked stylesheet |

Each example folder has its own README with the steps to run it.

## Troubleshooting

| Symptom | Cause |
|---|---|
| `Couldn't find ... in the configured repositories` | the JitPack repository is missing |
| `screen not found: name.nl` | the file is not in `app/src/main/assets/` or the loader does not find it |
| style does not apply | a linked `.css` is missing from the assets or its `href` is wrong |
| red error text on screen | an error in the `.nl` file; the message includes the line |

## The editor app

`app` is an editor with live preview, used to test screens on the device: code, data, preview and file tree. `DemoActivity` is a worked example built on `NativeView`.

### Build the APK from the phone

Push the repository to GitHub. The workflow at `.github/workflows/build.yml` (repository root) runs on every push and publishes `nativelayout-debug.apk` as an artifact of the run (Actions tab).

## Publish the library (maintainers)

Both library modules carry `maven-publish`.

Local test, from this folder:

```
./gradlew :core:publishToMavenLocal :android:publishToMavenLocal
```

Through JitPack: the Gradle project lives in `android/`, so the repository root has a `jitpack.yml` that builds from there:

```yaml
jdk:
  - openjdk21
install:
  - source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk install gradle 8.13 < /dev/null && cd android && gradle :core:publishToMavenLocal :android:publishToMavenLocal
```

To release:

1. Commit to `main`.
2. Create a GitHub release with a new tag (for example `v0.1.3`).
3. Open `https://jitpack.io/#getnativelayout/nativelayout`, go to Releases and tap Get it on the new tag.
4. When the log icon turns green, the version is available.

Each tag is a new version and nothing is overwritten. Consumers upgrade by changing the tag they depend on. The tag must point to a commit that already contains `jitpack.yml`.