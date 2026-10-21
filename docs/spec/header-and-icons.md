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

### 5.10 Footer: `<footer>`

A `<footer>` is a region fixed outside the scroll. By default it sits at the bottom of the screen, as the bar of an app (tabs, actions, a message field). Its `dock` and its style place it anywhere.

```html
<body>
    <header>...</header>

    ...

    <footer>
        <button id="tab-list" class="tab on">List</button>
        <button id="tab-summary" class="tab">Summary</button>
    </footer>
</body>
```

- It must be a direct child of the root tag, and a screen can have only one. Anywhere else it is an error: "`<footer>` must be a direct child of the root tag".
- In the neutral tree it comes after the body and before the dialogs, wherever it is written, and it is never collapsed into a text node.
- Default style: `flex-direction: row`, `justify-content: center`, `align-items: center`, no background. All of section 4 applies, so the bar is customised with CSS.
- `dock` says where it goes: `bottom` (default), `top`, `left`, `right` or `center`.
  - `bottom` takes its space: the scroll ends where the footer starts, and nothing is hidden behind it.
  - `top`, `left`, `right` and `center` float over the screen, at that edge or in the middle, and take no space. Use `margin` to move them away from the edge.
- `width`, `height`, `margin` and `border-radius` give a bar that is full width, narrow, or a floating pill. For `top` and `bottom`, `align-self` places a bar that is not full width; for `left` and `right` it places it up, middle or down.
- The active tab is marked with `if`, as with any other state of a screen.
- To repeat the same bar in several screens, write it once as a component (section 5.6) and include it inside the `<footer>`.
- This version does not hide the footer when the keyboard opens, and does not add space for the system navigation bar.

---

[Index](../../SPEC.md) | Previous: [Components and identity](components.md) | Next: [Values, control and data](values-and-data.md)

License: [CC BY-SA 4.0](../../LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael.
