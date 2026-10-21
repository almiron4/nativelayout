# nativelayout - Specification

Status: draft v0.4. v0.4 adds `goRoot`, `goBack`, `canGoBack` and `back()` (section 8.6) and the `animation` property (section 4.6). It replaces the draft where the markup lived inside Kotlin code.

License: this specification is licensed under [CC BY-SA 4.0](LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael. The code examples in this document are also available under the [MPL 2.0](LICENSE).

---

## Contents

The specification is split by subject, in the order of a first reading. The sections keep their numbers, so "section 5.3" is the same wherever it is mentioned.

| Section | Subject | File |
|---|---|---|
| 1 | Idea | [SPEC.md](SPEC.md) |
| 2, 3 | The file, how it works | [file.md](docs/spec/file.md) |
| 4 | Style (selectors, variables, properties, states, media) | [style.md](docs/spec/style.md) |
| 5.1, 5.2 | Tags and attributes | [body.md](docs/spec/body.md) |
| 5.3 | Lists: `key` and `virtual` | [lists.md](docs/spec/lists.md) |
| 5.4, 5.5 | Layers (`dialog`) and groups (`fieldset`, `legend`) | [layers-and-groups.md](docs/spec/layers-and-groups.md) |
| 5.6, 5.7 | Components (`include`) and identity inside a `for` | [components.md](docs/spec/components.md) |
| 5.8, 5.9 | Header (`header`) and built-in icons (`icon:name`) | [header-and-icons.md](docs/spec/header-and-icons.md) |
| 6, 7 | Values, control lines, conditions, data | [values-and-data.md](docs/spec/values-and-data.md) |
| 8, 9 | The program side and `render` | [program-side.md](docs/spec/program-side.md) |
| 10, 11 | Errors and targets | [errors-and-targets.md](docs/spec/errors-and-targets.md) |
| 12 | Examples | [examples.md](docs/spec/examples.md) |
| 13 | Good practices | [good-practices.md](docs/spec/good-practices.md) |

## Reading guide

- **To write a screen:** sections 2, 4, 5, 6 and the examples (12).
- **To write the program side of a screen:** sections 7, 8 and 9.
- **To build a target (a new language or platform):** sections 3, 8, 10 and 11, then the rest of the file.

## Designed, not built yet

Nothing: every part of the specification is built in the Kotlin engine.

---

## 1. Idea

A screen is written once, in a plain text file that looks like HTML. The same file works in any language: each language only supplies the program side and the part that draws native components.

- The file is HTML you already know, plus three additions: `{value}`, `if` / `for` lines, and a `<style>` with simple selectors.
- It has no functions, no classes and no variables. The logic stays outside, in each language.
- It is not a canvas that only draws: the file has conditions and repetition.
- The rule for every decision is the same: simple, nothing ambiguous.
