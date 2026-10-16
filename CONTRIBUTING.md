# Contributing to nativelayout

<p align="justify">
Thank you for wanting to help. This guide says exactly what to do, in order, for each kind of contribution. If something here is unclear, that is a bug in this guide: open an issue and say which step confused you.
</p>

## Contents

1. [Ways to contribute](#1-ways-to-contribute)
2. [The one rule behind every decision](#2-the-one-rule-behind-every-decision)
3. [Repository layout](#3-repository-layout)
4. [Set up the Android implementation](#4-set-up-the-android-implementation)
5. [Report a bug](#5-report-a-bug)
6. [Propose a feature or a change to the spec](#6-propose-a-feature-or-a-change-to-the-spec)
7. [Send a pull request](#7-send-a-pull-request)
8. [Code and file conventions](#8-code-and-file-conventions)
9. [Add a new language](#9-add-a-new-language)
10. [Add or change an example](#10-add-or-change-an-example)
11. [Continuous integration](#11-continuous-integration)
12. [Releases (maintainers)](#12-releases-maintainers)
13. [License](#13-license)

---

## 1. Ways to contribute

| You want to | Do this |
|---|---|
| Report something that does not work | [Section 5](#5-report-a-bug) |
| Ask for a new tag, property or verb | [Section 6](#6-propose-a-feature-or-a-change-to-the-spec) |
| Fix a bug or improve the Android code | [Sections 4 and 7](#4-set-up-the-android-implementation) |
| Fix a typo or improve a guide | Edit the file on GitHub and open a pull request ([section 7](#7-send-a-pull-request)) |
| Write an implementation for another language | [Section 9](#9-add-a-new-language) |
| Add an example screen | [Section 10](#10-add-or-change-an-example) |

---

## 2. The one rule behind every decision

<p align="justify">
The <code>.nl</code> file is the same for every language, and the specification says: <em>simple, nothing ambiguous</em>. A change is accepted only if it can be implemented with the same meaning in every target. If a feature works on Android but cannot be drawn on another platform, it does not go into the spec as a requirement: it becomes a warning on the targets that cannot draw it, never an error (see <a href="SPEC.md">SPEC.md</a>, section 11).
</p>

Consequences you should expect in review:

- No new syntax in the `.nl` file when the program side can do the job (state and logic live in the language, not in the file).
- No arithmetic, functions or variables in the file.
- Unsupported things are errors with a clear message and a line number, not silent behavior.

---

## 3. Repository layout

```
README.md          overview, install, API
SPEC.md            the file format shared by every language (the source of truth)
CONTRIBUTING.md    this file
LICENSE            MIT
jitpack.yml        tells JitPack how to build the Android library
img/               images used by the documentation
.github/workflows/ continuous integration
android/           Kotlin implementation
  core/              pure JVM: parser, CSS reader, Expander, tree, diff, host interface
  android/           AndroidLayoutHost and NativeView
  app/               editor with live preview and a demo activity
  examples/          example screens, one folder each
  README.md          Android guide
```

<p align="justify">
Each language lives in its own folder at the repository root (<code>android/</code>, and later <code>python/</code>, <code>ios/</code>, and so on). A language folder is self-contained: its own build files, its own <code>README.md</code> and its own <code>examples/</code>. Nothing in one language folder may depend on another.
</p>

---

## 4. Set up the Android implementation

### Requirements

| Tool | Version |
|---|---|
| JDK | 21 |
| Gradle | 8.13 (the repository has no `gradlew`, install Gradle or use your IDE's) |
| Android Gradle Plugin | 8.13.0 (set in `android/build.gradle.kts`) |
| Kotlin | 2.4.0 (set in `android/build.gradle.kts`) |
| Android SDK | platform 36, minimum SDK of the library is 21 |

### Get the code

```
git clone https://github.com/getnativelayout/nativelayout.git
cd nativelayout/android
```

To contribute through a fork, fork the repository on GitHub first and clone your fork instead.

### Build the editor app

```
gradle :app:assembleDebug
```

The APK is in `android/app/build/outputs/apk/debug/`.

### Build and install the library locally

```
gradle :core:publishToMavenLocal :android:publishToMavenLocal
```

<p align="justify">
This publishes <code>io.github.getnativelayout:nativelayout-core</code> and <code>nativelayout-android</code> to your <code>~/.m2</code>. To try your change in another project, add <code>mavenLocal()</code> to its repositories and depend on those coordinates with the version from <code>android/android/build.gradle.kts</code>.
</p>

### No Android SDK on your computer

<p align="justify">
You can contribute from a phone or any machine without the SDK: push your branch to your fork and let GitHub Actions build and test it (section 11). The workflow runs on every push and uploads the APKs as artifacts.
</p>

---

## 5. Report a bug

Open an issue and fill in every item below. Reports missing the first four items will be asked to add them.

1. **What you expected** to see or happen.
2. **What happened instead**, with the exact error text if the screen showed a red error message.
3. **The smallest `.nl` file** that shows the problem, in a code block. Remove everything that is not needed. Include the linked `.css` if the style is involved.
4. **The data** you passed to `render`, as a Kotlin `mapOf(...)` or as JSON-like text.
5. **The taps or steps** that lead to the problem (for example: tap Add, then tap the second card).
6. **Version**: the tag you depend on (for example `v0.1.2`), or the commit if you build from source.
7. **Device**: model, Android version, and font scale if you changed it.
8. **A screenshot**, when the problem is visual.

<p align="justify">
A problem that only happens in one state of the data usually means the Expander received something unexpected. Say which names the data had and their types.
</p>

---

## 6. Propose a feature or a change to the spec

1. Search the existing issues first.
2. Open an issue titled `Proposal: <short name>`.
3. In the issue, write:
   - the problem you cannot solve today, with a real example screen;
   - the smallest change to the `.nl` format or to the verbs that solves it;
   - how it behaves on other targets (what a platform without that capability does: warning, fallback);
   - at least one example `.nl` and the matching program side.
4. Wait for agreement before writing the code. A proposal that changes the format is decided in the issue, not in the pull request.

Changes to `SPEC.md` follow these rules:

- The spec changes in the same pull request as the code that implements it.
- Bump the version at the top of `SPEC.md` when the format changes: patch for clarifications, minor for additions, major for anything that breaks an existing `.nl` file.
- Update the error table (section 10) when you add or change an error.
- Update the examples if the change makes one of them wrong.

---

## 7. Send a pull request

### Steps

1. Fork the repository and create a branch from `main`. Name it after the change: `fix-percent-width`, `add-python-expander`.
2. Make **one** change per pull request. A bug fix and an unrelated cleanup are two pull requests.
3. Build and test locally if you can (section 4), or push to your fork and wait for the green check (section 11).
4. Open the pull request against `main` and fill in:
   - what changed and why;
   - which issue it closes (`Closes #12`);
   - how you tested it;
   - screenshots for visual changes.
5. Answer review comments by pushing new commits to the same branch.

### Checklist (all must be true before you ask for review)

- [ ] The change does one thing.
- [ ] The CI workflow passes, including the job `Test and build example (tasks)`.
- [ ] If behavior changed, `SPEC.md` is updated in the same pull request.
- [ ] If an error message changed, the table in `SPEC.md` section 10 matches.
- [ ] If the API changed, the API tables in `README.md` and `android/README.md` match.
- [ ] If you added a tag, property or verb, there is an example that uses it.
- [ ] New files carry the header described in section 8.
- [ ] No unrelated formatting changes.

### Commit messages

Short, in English, in the imperative: `Fix percent width inside nested rows`, `Add checkbox taps to the host`. One line is enough; add a second paragraph when the reason is not obvious.

---

## 8. Code and file conventions

### Header

Source files start with a header with the author and the date of creation:

```kotlin
/*
 * Author: Your Name (github-handle)
 * Date: 2026-10-16
 */
```

Use the comment syntax of the language (`#` for YAML and Python).

### Kotlin

- Official Kotlin style (`kotlin.code.style=official`).
- `core` is pure JVM: it must not import anything from `android.*`. Anything that touches the platform belongs in the `android` module behind the `LayoutHost` interface.
- Errors that come from the user's `.nl` file are `NativeLayoutError`s with a message and a line number. Do not throw a bare exception for a bad `.nl`.
- A public function that other languages must also have goes in the verb table of `SPEC.md` section 8.

### Documentation

- English.
- Text paragraphs may use `<p align="justify">`; lists and tables stay in plain Markdown.
- No emojis in code, comments or docs.
- Code blocks have a language (`kotlin`, `html`, `yaml`, `groovy`).

---

## 9. Add a new language

<p align="justify">
A new implementation is complete when the same <code>.nl</code> files that work on Android work on it, unchanged, with the same meaning. Follow these steps in order.
</p>

### Step 1: open an issue first

Title it `Language: <language> (<target>)`. Say which toolkit draws the components (for example Kivy, Tkinter, UIKit) and who is working on it. Wait for the maintainers to confirm there is no overlap.

### Step 2: create the folder

```
<language>/
  README.md        install, quick start, API, troubleshooting (same sections as android/README.md)
  examples/
    tasks/         the same tasks example, adapted to the language
  <build files>    whatever the language needs to build and publish
```

### Step 3: build the three parts

| Part | What it must do | Spec sections |
|---|---|---|
| **Expander** | Read the `.nl`, resolve `<link>` files, evaluate `if` / `elif` / `else` / `for` / `end` and `{values}` with the data, apply selectors and variables, write the result into the `style` of each element, output plain HTML with inline styles. | 2, 4, 5, 6, 7 |
| **Engine** | Parse that HTML, read the styles, build the neutral tree, diff the new tree against the previous one and produce a list of changes. | 3, 5, 9 |
| **Host** | Create and update the native components from the changes, and report taps. | 5.1, 9 |

Then add the program side:

| Verbs to provide | Spec section |
|---|---|
| `screen`, `render`, `click`, `clickIndex`, `value`, `checked`, `setValue`, `save`, `load`, `exists`, `delete`, `close` | 8 |

<p align="justify">
Names follow the idiom of the language (the spec shows <code>click_index</code> and <code>set_value</code> for Python). The behavior does not change: <code>render</code> shows the screen, waits for a tap on an element with an <code>id</code> or a class, and returns <code>true</code>; it returns <code>false</code> when the window closes.
</p>

### Step 4: the behavior that must match exactly

1. The tags of section 5.1 map to native components exactly as listed.
2. A tag outside the list is an error with the message from section 10.
3. The properties of section 4.3 are supported; an unknown property is a warning, never an error.
4. A property the platform cannot draw (for example rounded corners or gap) is a warning, never an error.
5. Percentages are accepted only in `width` and `height`.
6. Taps go into a queue and each `render` consumes one, in order.
7. Text typed in a field with an `id` is kept between renders; `value` and `checked` in the file apply only on creation or when the data value changes.
8. Every error in section 10 has the same message text and reports the line of the `.nl` file.

### Step 5: prove it

1. Run `android/examples/tasks/tasks.nl` and `tasks.css` **unchanged** on your implementation.
2. Write the same tests as the Android CI job: expand `tasks.nl` with the five data states (full list, empty list, each of the three filters, everything done), build the tree, and check that the expected `id`s exist (`title`, `add`, `show-all`, `show-pending`, `show-done`, and `clear` only when something is done).
3. Make a bad `.nl` for each row of the error table and check the message.

### Step 6: wire it into the repository

- Add a CI job for the language in `.github/workflows/` that builds it and runs the tests on every push.
- Update the table in `README.md` (section "Implementations") from `planned` to `implemented`, with the folder link.
- Update the table in `SPEC.md` section 11 the same way.
- Add the language's install instructions to its own `README.md`, and a short pointer in the root `README.md`.

### Definition of done

- [ ] Expander, Engine and Host implemented.
- [ ] All verbs of section 8 provided.
- [ ] `tasks.nl` and `tasks.css` run unchanged.
- [ ] The five data states pass in CI.
- [ ] Each error of section 10 gives the same message.
- [ ] `README.md` and `examples/` exist in the language folder.
- [ ] Both tables (`README.md`, `SPEC.md` section 11) are updated.

---

## 10. Add or change an example

Examples live in `<language>/examples/<name>/`. Each folder contains:

| File | Purpose |
|---|---|
| `<name>.nl` | the screen |
| `<name>.css` | the style, linked from the `.nl` |
| the program file | `MainActivity.kt` for Android |
| `README.md` | where each file goes, how to run it, what to try, what it covers |

Rules:

- The example must run in an empty project after following only its README.
- Keep the files minimal. Do not include Gradle files, manifests or resources that every project already has.
- If you change `tasks`, update the CI test that expands it (`TasksScreenTest` in `.github/workflows/build.yml`), because it checks the ids and the data it uses.
- A new example should cover something `tasks` does not. List what it covers in its README.

---

## 11. Continuous integration

The workflow `.github/workflows/build.yml` runs on every push and pull request.

| Job | What it does |
|---|---|
| `Build editor APK` | builds `android/app` and uploads the APK |
| `Test and build example (tasks)` | publishes the library from your commit to Maven local, creates a temporary project from the files in `android/examples/tasks/`, runs the screen tests, builds the APK and uploads it |

If a job fails:

1. Open the run in the Actions tab and open the failed step.
2. Read the first error, not the last line.
3. If the example test failed, download the artifact `tasks-example-test-report` for the full report.
4. Fix, push to the same branch, wait for the new run.

---

## 12. Releases (maintainers)

1. Make sure `main` is green.
2. Raise the version in `android/core/build.gradle.kts` and `android/android/build.gradle.kts` when the library changes.
3. Create a GitHub release with a new tag, for example `v0.1.3`, targeting `main`. The tag must point to a commit that already contains `jitpack.yml`.
4. Open `https://jitpack.io/#getnativelayout/nativelayout`, go to Releases and tap Get it on the new tag.
5. When the log icon is green, the version is available as `com.github.getnativelayout.nativelayout:nativelayout-android:<tag>`.

A tag is never moved or reused. A fix is a new tag.

---

## 13. License

<p align="justify">
By contributing, you agree that your contribution is licensed under the <a href="LICENSE">MIT License</a>, the same as the rest of the project.
</p>
