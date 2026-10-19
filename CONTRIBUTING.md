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
13. [License and ownership](#13-license-and-ownership)

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
| Ask a question or show what you built | [Join our Community on Discord](https://discord.gg/638USuPnpk) |

---
<p align="justify">
Not sure where something belongs? Ask on <a href="https://discord.gg/638USuPnpk">Discord</a> first. Use it for questions and discussion; use issues for bugs and proposals, so decisions stay on record.
</p>


## 2. The one rule behind every decision

<p align="justify">
The <code>.nl</code> file is the same for every language, and the specification says: <em>simple, nothing ambiguous</em>. A change is accepted only if it can be implemented with the same meaning in every target. If a feature works on Android but cannot be drawn on another platform, it does not go into the spec as a requirement: it becomes a warning on the targets that cannot draw it, never an error (see <a href="SPEC.md">SPEC.md</a>, section 11).
</p>

<p align="justify">
The syntax of the <code>.nl</code> file, the verbs of the API and the content of <code>SPEC.md</code> are decided by the maintainer, so that the project keeps one clear idea. Proposals are welcome and are read carefully, but an idea being interesting is not enough: it has to fit the rule above. A proposal that is declined is declined for that reason, and the reason is written in the issue.
</p>

Consequences you should expect in review:

- No new syntax in the `.nl` file when the program side can do the job (state and logic live in the language, not in the file).
- No arithmetic, functions or variables in the file.
- Unsupported things are errors with a clear message and a line number, not silent behavior.

---

## 3. Repository layout

```
README.md          overview, install, API
SPEC.md            the specification, index and overview (the source of truth)
docs/spec/         the specification, split by subject; the section numbers do not change
CONTRIBUTING.md    this file
LICENSE            MPL 2.0 (the code)
LICENSE-SPEC       CC BY-SA 4.0 (SPEC.md, docs/spec/ and the documentation)
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

<p align="justify">
The editor app uses the <code>core</code> and <code>android</code> modules of the same commit (<code>implementation(project(...))</code> in <code>android/app/build.gradle.kts</code>), so a change to the engine shows in the editor without copying any file. To try a change on a phone, push your branch and install the editor APK that the workflow builds (section 11).
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

### Test the core

```
gradle :core:test
```

The tests in `android/core/src/test/` expand and build small screens in plain JVM, so they need no device.

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
4. **The data** you passed to `view` (or `render`), as a Kotlin `mapOf(...)` or as JSON-like text.
5. **The taps or steps** that lead to the problem (for example: tap Add, then tap the second card).
6. **Version**: the tag you depend on (for example `v0.1.7`), or the commit if you build from source.
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

Changes to `SPEC.md` and `docs/spec/` follow these rules:

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
- [ ] The CI workflow passes, including the job `Build example (testlayout) against this commit`.
- [ ] The core tests pass (`gradle :core:test`), and a new CSS property has a case in `CssFeaturesTest`.
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

### Adding a CSS property

1. Read it in `applyDeclaration` of `LayoutEngine.kt`. An invalid value gives a warning and is ignored, never an exception.
2. Add a field with a default value to `NativeStyle` in `NativeTree.kt`. The diff compares the whole style, so it picks the new field up by itself. If the property is inherited, also copy it in `inherit()` of `LayoutEngine.kt`.
3. Apply it in `AndroidLayoutHost.kt` (`applyStyle` and the functions it calls).
4. Document it in `SPEC.md` section 4.3 and in the style list of `README.md`.
5. Add a case to `CssFeaturesTest` for the valid value, an invalid value and, if it is inherited, the inheritance.

A selector state (`:pressed`, `:disabled`, `:focus`) or an `@media` condition is read in `Expander.kt`, not in the engine: the expander writes the result into the `style` attributes (`style-pressed`, `style-disabled`, `style-focus`).

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
| `state`, `view`, `go`, `screen`, `render`, `click`, `clickIndex`, `value`, `checked`, `setValue`, `save`, `load`, `exists`, `delete`, `close` | 8 |

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
2. Write tests for the same five data states: expand `tasks.nl` with them (full list, empty list, each of the three filters, everything done), build the tree, and check that the expected `id`s exist (`title`, `add`, `show-all`, `show-pending`, `show-done`, and `clear` only when something is done).
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
- If you change an example, make sure it still builds in CI (job `Build example (testlayout) against this commit`).
- A new example should cover something `tasks` does not. List what it covers in its README.

---

## 11. Continuous integration

The workflow `.github/workflows/build.yml` runs on every push and pull request.

| Job | What it does |
|---|---|
| `Build editor APK` | runs the core tests (`gradle :core:test`), builds `android/app` with the engine of this commit and uploads the APK (`nativelayout-debug`) |
| `Build example (testlayout) against this commit` | publishes the library from your commit to Maven local, points the project in `android/examples/testlayout/` at it, builds the APK and uploads it |

If a job fails:

1. Open the run in the Actions tab and open the failed step.
2. Read the first error, not the last line.
3. If the example build failed, open the failed Gradle step for the full message.
4. Fix, push to the same branch, wait for the new run.

---

## 12. Releases (maintainers)

1. Make sure `main` is green.
2. Raise the version in `android/core/build.gradle.kts` and `android/android/build.gradle.kts` when the library changes.
3. Create a GitHub release with a new tag, for example `v0.1.7`, targeting `main`. The tag must point to a commit that already contains `jitpack.yml`.
4. Open `https://jitpack.io/#getnativelayout/nativelayout`, go to Releases and tap Get it on the new tag.
5. When the log icon is green, the version is available as `com.github.getnativelayout.nativelayout:nativelayout-android:<tag>`.

A tag is never moved or reused. A fix is a new tag.

---

## 13. License and ownership

**Licenses.** The code and the examples are under the <a href="LICENSE">Mozilla Public License 2.0</a>. `SPEC.md` and the written documentation are under <a href="LICENSE-SPEC">CC BY-SA 4.0</a>.

**Your contribution.** By opening a pull request you confirm that:

- you wrote it, or you have the right to submit it;
- you agree that it is distributed under the license of the file you changed: MPL 2.0 for code and examples, CC BY-SA 4.0 for the specification and the documentation;
- you keep the copyright of what you wrote, and your name stays in the history of the repository.

**What the MPL 2.0 means for a modified copy.** Files you change must stay under the MPL 2.0 and their source must be published when you distribute them. Your own files, and apps that only use the library, can have any license.

**The name and the logo.** *nativelayout*, *getnativelayout* and the logo are not licensed to anyone. A fork or a modified version must use another name.

This is a summary to help you decide, not legal advice. The license files are the text that applies.
