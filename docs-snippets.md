# Documentation snippets for v0.1.8

## README.md, table of tags

Replace the `img` row and add the `header` row:

| Tag | Native component |
|---|---|
| `img` | image (`data:` URI, a built-in icon `icon:<name>`, or a name resolved by the host) |
| `header` | bar fixed at the top of the screen, outside the scroll; direct child of the root, one per screen |

## README.md, "Why nativelayout"

- **Fixed header and built-in icons.** `<header>` stays at the top while the screen scrolls, and `<img src="icon:arrow-left">` draws a built-in icon in the text color, with no image files.

## SPEC.md, new section: `<header>`

`<header>` must be a direct child of the root tag, and a screen can have only one. It is laid out above
the scrolling content and does not scroll. In the neutral tree it is always the first child of the root,
wherever it is written, and it is never collapsed into a text node. Dialogs still come last.
A `<header>` placed anywhere else is an error: "<header> must be a direct child of the root tag".

## SPEC.md, new section: icons

`<img src="icon:name">` draws a built-in icon. Icons are drawn on a 24 x 24 grid with a 2-unit stroke,
round caps and joins, in the `color` of the element, at 24 dp by default. `width` and `height` scale them.
Use `id` or `class` on the `<img>` to read taps, and `alt` for accessibility. An unknown name is an error
that lists the available names. The shapes are defined in the core (`IconSet`), so every host draws the same icons.

Available icons (30): `arrow-left`, `arrow-right`, `arrow-up`, `arrow-down`, `chevron-left`, `chevron-right`, `chevron-up`, `chevron-down`, `close`, `check`, `plus`, `minus`, `menu`, `search`, `more-vertical`, `more-horizontal`, `home`, `user`, `trash`, `edit`, `star`, `lock`, `eye`, `mail`, `info`, `alert`, `calendar`, `logout`, `bookmark`, `clock`.
