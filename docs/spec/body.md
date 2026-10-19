# nativelayout - Specification: The body

[Index](../../SPEC.md) | Previous: [Style](style.md) | Next: [Lists](lists.md)

---

## 5. Body

### 5.1 Tags

| Tag | Native component |
|---|---|
| `div` | container |
| `span`, `p`, `h1`, `h2`, `h3` | text |
| `button` | button (text only) |
| `input` (`text`, `password`, `number`, `checkbox`) | field or checkbox |
| `textarea` | multi-line field |
| `img` | image |
| `ul`, `li` | list; each `li` gets a bullet |
| `fieldset`, `legend` | bordered group with a title (section 5.5) |
| `dialog` | layer over the screen: dialog or sheet (section 5.4) |
| `include` | a component written in another `.nl` file (section 5.6) |

A tag outside this list is an error.

The whole screen scrolls: the root sits in one vertical scroll container. A long list scrolls the screen (for very long lists, see `virtual` in section 5.3), and a bar pinned to the bottom is not possible in v1.

### 5.2 Attributes

| Attribute | Meaning |
|---|---|
| `id` | unique name; links the element to the program |
| `class` | style group, and also a group for the program |
| `style` | inline style |
| `placeholder` | hint of a field |
| `type` | type of an `input` |
| `value` | initial text of a field |
| `checked` | initial state of a checkbox |
| `disabled`, `readonly` | blocks the element; on a `fieldset`, blocks everything inside it (section 5.5) |
| `src`, `alt` | image: a `data:` URI or a name resolved by the host; no network in v1 |
| `rows` | lines of a `textarea` |
| `key` | stable name of an item among its siblings (section 5.3) |
| `virtual` | marks a container as a long list (section 5.3) |

- `checked`, `disabled` and `readonly` count when present, whatever their text. With a value (`checked="{done}"`) the attribute is written only when the value is `true`.
- `id` and `class` cannot have a value: `id="{x}"` is an error. A class changes with `if` and `else`, writing each variant in full.
- An `id` is unique in the screen. Inside a `for` an `id` names one element per item (section 5.7).
- `virtual` counts when present, whatever its text.
- `key` accepts values: `key="{task.id}"`.
- A tap on an element that has an `id` or a class wakes the program (section 9).

---

[Index](../../SPEC.md) | Previous: [Style](style.md) | Next: [Lists](lists.md)

License: [CC BY-SA 4.0](../../LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael.
