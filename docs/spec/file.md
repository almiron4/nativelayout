# nativelayout - Specification: The file

[Index](../../SPEC.md) | Next: [Style](style.md)

---

## 2. The file

```html
<nativelayout>

<link rel="stylesheet" href="panel.css">

<style>
    /* style rules */
</style>

<body>
    <!-- the screen -->
</body>

</nativelayout>
```

1. The first thing in the file is `<nativelayout>` and the last is `</nativelayout>`.
2. Between them: zero or more `<link rel="stylesheet" href="...">`, then exactly one `<body>`. Nothing else.
3. `<style>` is not allowed in the `.nl`. All style lives in `.css` files, with the rules of section 4.
4. `<body>` is the root of the screen. Everything visible goes inside it.
5. Comments are `<!-- ... -->` in the `.nl`, and `/* ... */` in the `.css`.
6. `<script>` is not allowed.
7. The path in `href` is relative to the `.nl`. The cascade follows the order of the `<link>` tags: at the same weight, the later rule wins.
8. Files are UTF-8.
9. One screen is one `.nl`. The extension is `.nl` (proposal).

---

## 3. How it works

```
panel.nl + data  ->  Expander  ->  HTML with inline styles  ->  Engine  ->  native views
                     (if, for,                                  (parse,      (host)
                      {value}, selectors)                        diff, style)
```

- **Expander:** reads the file once. On each `render` it evaluates the control lines and the `{values}` with the data, writes the result of the selectors into the `style` of each element, and turns `<body>` into the root container. Its output is plain HTML with inline styles and no control.
- **Engine:** HTML parser, style reader, neutral tree and diff. In Kotlin it already exists and does not change.
- **Host:** creates the native components (Android `View` objects).

For each language there is an expander, an engine, a host and the program verbs (section 8). The `.nl` file is the same for all of them.

---

[Index](../../SPEC.md) | Next: [Style](style.md)

License: [CC BY-SA 4.0](../../LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael.
