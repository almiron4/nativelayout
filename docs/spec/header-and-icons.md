# nativelayout - Specification: Header and icons

[Index](../../SPEC.md) | Previous: [Components and identity](components.md) | Next: [Values, control and data](values-and-data.md)

---

*Chapter 5, continued.*

### 5.8 Header: `<header>`

A `<header>` is a bar fixed at the top of the screen. It does not scroll with the rest.

```html
<body>
    <header>
        <img id="back" src="icon:arrow-left">
        <h1>Tasks</h1>
    </header>

    ...
</body>
```

- It must be a direct child of the root tag, and a screen can have only one. Anywhere else it is an error: "`<header>` must be a direct child of the root tag".
- It is laid out above the scrolling content. In the neutral tree it is always the first child of the root, wherever it is written, and it is never collapsed into a text node. The dialogs still come last.
- Its style is that of a block: size, background, padding, `flex-direction` and the rest of section 4 apply.

### 5.9 Icons: `<img src="icon:name">`

An `<img>` whose `src` starts with `icon:` draws a built-in icon, so no image file is needed.

- Icons are drawn on a 24 x 24 grid with a 2-unit stroke, round caps and joins, in the `color` of the element, at 24 dp by default. `width` and `height` scale them.
- Use `id` or `class` on the `<img>` to read taps, and `alt` for accessibility.
- An unknown name is an error that lists the available names.
- The shapes are defined in the core (`IconSet`), so every host draws the same icons.

Available icons (30): `arrow-left`, `arrow-right`, `arrow-up`, `arrow-down`, `chevron-left`, `chevron-right`, `chevron-up`, `chevron-down`, `close`, `check`, `plus`, `minus`, `menu`, `search`, `more-vertical`, `more-horizontal`, `home`, `user`, `trash`, `edit`, `star`, `lock`, `eye`, `mail`, `info`, `alert`, `calendar`, `logout`, `bookmark`, `clock`.

---

[Index](../../SPEC.md) | Previous: [Components and identity](components.md) | Next: [Values, control and data](values-and-data.md)

License: [CC BY-SA 4.0](../../LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael.
