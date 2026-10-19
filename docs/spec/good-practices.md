# nativelayout - Specification: Good practices

[Index](../../SPEC.md) | Previous: [Examples](examples.md)

---

## 13. Good practices

- Keep the logic out of the file. A name in `{ }` should already be calculated by the program.
- Use `id` for single elements (buttons, fields) and `class` for repeated ones (cards).
- Put colors in `:root` and use `var(--name)`, so a theme change is one edit.
- Write one variant per state (`.right`, `.wrong`) instead of computing a class.
- Give a field a `value="{name}"` to open it with a stored text.
- Put a `key` on every repeated element, with something that never changes (an id), never the position.
- Use `virtual` only for long lists, hundreds of items or more. A short list is simpler as a normal container and keeps what the user typed.
- Keep `id` and `class` off a `virtual` container. Style it with `style="..."`.

---

[Index](../../SPEC.md) | Previous: [Examples](examples.md)

License: [CC BY-SA 4.0](../../LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael.
