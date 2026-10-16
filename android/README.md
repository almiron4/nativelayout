# nativelayout — android

Kotlin implementation. Turns an `.nl` file into real Android `View` objects, no WebView.

## Modules

- `core`: pure JVM. HTML parser, CSS reader, `<link>` resolver, Expander (`if`/`for`/`{value}`), neutral tree, diff, host interface.
- `android`: `AndroidLayoutHost` (tree -> real Views) and `NativeView` (the `screen()/render()/click()/...` API from the spec, section 8).
- `app`: the editor (code, data, preview, file tree) and `DemoActivity`, a worked example built on `NativeView`.

## Publish core and android as a library

Both modules carry `maven-publish`.

Local test:
```
./gradlew :core:publishToMavenLocal :android:publishToMavenLocal
```

Via JitPack (repository `nativelayout`, module path `android`): push this repository
to GitHub and create a tag (e.g. `v0.1.0`). Consumers add:
```kotlin
implementation("com.github.getnativelayout.nativelayout:nativelayout-core:v0.1.0")
implementation("com.github.getnativelayout.nativelayout:nativelayout-android:v0.1.0")
```
Each new tag is a new version; nothing is overwritten. Consumers upgrade by
changing the tag they depend on.

## Build the APK from the phone

Push the repository to GitHub. The workflow at `.github/workflows/build.yml`
(repository root) runs on every push and publishes `nativelayout-debug.apk` as
an artifact of the run (Actions tab).
