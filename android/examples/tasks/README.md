# Tasks example (Android)

A task list that exercises most of nativelayout: forms, lists, conditions, filters, taps, a progress bar and on-device storage.

## Files

| File | Goes in | Purpose |
|---|---|---|
| `tasks.nl` | `app/src/main/assets/` | the screen: markup, conditions and loops |
| `tasks.css` | `app/src/main/assets/` | the style, linked from `tasks.nl` |
| `MainActivity.kt` | `app/src/main/java/<your package>/` | the logic: state, taps and storage |

`tasks.css` must sit next to `tasks.nl`. `tasks.nl` links it before the body:

```html
<nativelayout>

<link rel="stylesheet" href="tasks.css">

<body>
    ...
</body>

</nativelayout>
```

The loader you pass to `NativeView` resolves both files by name.

## Requirements

- Android project with `minSdk` 21 or higher.
- Kotlin.
- Internet access on the first build, to download the library from JitPack.

## 1. Add the JitPack repository

The library is published on [JitPack](https://jitpack.io/#getnativelayout/nativelayout), so the project must be allowed to resolve from `https://jitpack.io`.

`settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

`settings.gradle` (Groovy):

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

If your project has no `dependencyResolutionManagement`, add the same line to `allprojects { repositories { ... } }` in the root `build.gradle`.

Without this step the build fails with `Couldn't find com.github.getnativelayout... in the configured repositories`.

## 2. Add the dependency

`app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.getnativelayout.nativelayout:nativelayout-android:v0.1.8")
    implementation("com.github.getnativelayout.nativelayout:nativelayout-core:v0.1.8")
}
```

`app/build.gradle` (Groovy):

```groovy
dependencies {
    implementation 'com.github.getnativelayout.nativelayout:nativelayout-android:v0.1.8'
    implementation 'com.github.getnativelayout.nativelayout:nativelayout-core:v0.1.8'
}
```

The coordinates are `group:artifact:version`:

| Part | Value |
|---|---|
| group | `com.github.getnativelayout.nativelayout` |
| artifact | `nativelayout-android` and `nativelayout-core` |
| version | a tag of the repository, for example `v0.1.8` |

Add both artifacts, with the same version. Sync the project after editing.

## 3. Copy the files

1. Copy `tasks.nl` and `tasks.css` to `app/src/main/assets/` (create the folder if it does not exist).
2. Copy `MainActivity.kt` to your package folder and change its `package` line to your project's package.
3. Make sure `AndroidManifest.xml` declares the activity as the launcher:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

## 4. Build and run

Build and install. The screen is created from `tasks.nl` as native views, with three starter tasks on the first run.

## What to try

- Tap a card to mark it done or pending. The counters and the progress bar follow.
- Tap `Remove` to delete a task. The tap stays on the button and does not toggle the card.
- Switch between `All`, `Pending` and `Done`. The active chip changes color.
- Type a title and tap `Add`. The field is cleared.
- Tap `Clear completed` to drop every finished task.
- Close and reopen the app. The tasks are still there.
- Press and hold a card, a chip or a button: the pressed color shows while your finger is down.
- Done tasks are crossed out.
- Switch the phone to the dark theme and reopen the app: the palette changes.

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
| `:pressed` state | `#add`, `.chip`, `.card`, `.remove`, `.ghost` in `tasks.css` |
| `@media (prefers-color-scheme: dark)` | the dark palette at the top of `tasks.css` |
| `linear-gradient` | progress bar fill |
| `box-shadow` | task cards |
| `text-decoration: line-through` | done tasks |

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

`clickIndex()` is a position among the tapped class, so it always refers to the list that was rendered. That is why the code keeps `shown` instead of indexing `tasks` directly.

## Troubleshooting

| Symptom | Cause |
|---|---|
| `Couldn't find ... in the configured repositories` | the JitPack repository is missing (step 1) |
| `screen not found: tasks.nl` | the file is not in `app/src/main/assets/` |
| style does not apply | `tasks.css` is missing from assets or the `<link>` name is wrong |
| red error text on screen | a `.nl` error; the message includes the line of the file |