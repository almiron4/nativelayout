# nativelayout - Specification: Style

[Index](../../SPEC.md) | Previous: [The file](file.md) | Next: [The body](body.md)

---

## 4. Style

### 4.1 Selectors

| Selector | Matches |
|---|---|
| `:root` | declares variables (`--name: value;`) |
| `body` | the root of the screen |
| `p`, `button`, `input`, ... | every element with that tag |
| `.name` | every element with that class |
| `#name` | the element with that id |
| `h1, h2` | a list, separated by commas |
| `.name:pressed`, `button:disabled`, `#id:focus` | the element while it is pressed, disabled or focused (section 4.4) |

Not supported in v1: `a b`, `a > b`, `:hover` and the other pseudo-classes, attribute selectors, and any `@media` condition other than the theme (section 4.5). They are an error. `!important` is ignored.

**Cascade.** The weight is: tag, then class, then id, then the `style="..."` attribute, each overriding the one before. At the same weight, the later rule wins.

### 4.2 Variables

```css
:root { --bg: #F2F2F2; --accent: #2F81F7; }
body  { background: var(--bg); }
```

A variable that is not declared in `:root` is an error.

### 4.3 Properties

| Area | Properties |
|---|---|
| Layout | `display` (`flex` or `block`), `flex-direction`, `flex`, `flex-wrap`, `justify-content`, `align-items`, `align-self`, `gap`, `width`, `height`, `min-width`, `max-width`, `min-height`, `max-height`, `overflow`, `opacity` |
| Box | `padding`, `margin` (and `-top`, `-right`, `-bottom`, `-left` of each), `border` (and `border-width`, `border-color`, `border-style`), `border-radius` (one to four values) and `border-top-left-radius`, `border-top-right-radius`, `border-bottom-right-radius`, `border-bottom-left-radius`, `box-shadow` |
| Background | `color`, `background` (and `background-color`, `background-image`) with a color or a `linear-gradient` |
| Text | `font-size`, `font-weight`, `font-style`, `font-family`, `text-align`, `text-decoration`, `text-transform`, `line-height`, `letter-spacing` |

- Units: `rem`, `px` and `0`. `px` means dp (density-independent). `1rem = 16dp` times the font scale of the system. A percentage is accepted in `width`, `height`, `opacity` and `line-height`. `opacity` also takes a number from 0 to 1, and `line-height` a number that multiplies the font size.
- Colors: `#RGB`, `#RGBA`, `#RRGGBB`, `#RRGGBBAA`, color names (`red`, `teal`, ...), `rgb()`, `rgba()`, `transparent` and `var(--name)`.
- Inherited by the children: `color`, `font-size`, `font-weight`, `font-style`, `font-family`, `text-align`, `text-decoration`, `text-transform`, `line-height` and `letter-spacing`.
- The default direction is column and the default alignment is stretch.
- Tags carry default styles: headings are bold with vertical margins, `p` has a vertical margin of 1rem, a `button` is gray with padding, a text field has a border. Write `margin: 0` when you do not want the margin.
- An unknown property gives a warning and is ignored.

Details of the properties added after v0.1:

- `justify-content`: `flex-start`, `center`, `flex-end`, `space-between`, `space-around`, `space-evenly`. With one item, `space-between` behaves like `flex-start`.
- `flex-wrap: wrap` works in rows (`flex-direction: row`). Items break to a new line when they do not fit, and `gap` is used between items and between lines. `flex` does not grow items inside a wrapped row. In a column it is ignored.
- `align-self` (`auto`, `flex-start`, `center`, `flex-end`, `stretch`) overrides `align-items` for one element.
- `min-width`, `min-height`, `max-width` and `max-height` take a length. `max-width: none` clears a limit.
- `margin` accepts negative lengths. A box does not clip its children: what a negative margin pushes outside the box is drawn, unless the box has `overflow: hidden`.
- `border-radius` takes one to four lengths in the CSS order: top-left, top-right, bottom-right, bottom-left. With two values the first is for top-left and bottom-right and the second for top-right and bottom-left. With three, the second is for top-right and bottom-left. `border-radius: 16px 16px 0 0` rounds only the top, the shape of a bottom sheet. The slash form (`10px / 20px`) and percentages are not supported and give a warning. The four `border-*-radius` properties change one corner.
- `overflow: hidden` clips the content to the rounded corners (`border-radius`). `overflow: visible` is the default. `scroll` and `auto` are not supported and give a warning.
- `opacity` from 0 to 1. A disabled element is drawn at 0.5 unless `:disabled` sets its own `opacity`.
- `text-align`: `left`, `center`, `right` (and `start`, `end`). `justify` gives a warning and is treated as `left`.
- `text-decoration`: `underline`, `line-through` or `none`. A line style or color is accepted and ignored.
- `font-family`: `sans-serif`, `serif` or `monospace` (a few common names map to them). It takes the first one it knows from the list.
- `text-transform`: `none`, `uppercase`, `lowercase`, `capitalize`.
- `line-height`: `normal`, a number (multiple of the font size), a length or a percentage. `letter-spacing`: `normal` or a length.
- `box-shadow` becomes an elevation: half of the blur (or of the vertical offset when there is no blur). Color, offset and spread are ignored, `inset` is not supported, and only the first shadow counts. The element needs a background to cast a shadow.
- `linear-gradient([angle | to <side or corner>,] color, color, ...)`. The angle is rounded to the nearest 45 degrees and the stop positions are ignored. A later `background` color replaces the gradient.
- `border-style: none` removes the border. The other styles are accepted and the border is always solid. Borders are the same on the four sides.

### 4.4 States

```css
.btn          { background: #2F81F7; color: #FFFFFF; }
.btn:pressed  { background: #1F6FE0; }
button:disabled { background: #CCCCCC; opacity: 0.6; }
#name:focus   { border-color: #2F81F7; }
```

- `:pressed` (and `:active`, the same thing), `:disabled` and `:focus`, after a tag, a class or an id.
- Inside a state only these properties count: `background`, `color` and `border-color`. `opacity` counts only in `:disabled`. Any other property gives a warning.
- A state rule weighs the same as the plain rule of the same selector kind. At the same weight, the later one wins.
- `:pressed` applies to elements that react to a tap (they have an `id` or a class). A pressed container also presses its children on Android.
- `:hover` is an error: a touch screen has no hover.

### 4.5 Media

```css
:root { --bg: #FFFFFF; --text: #111827; }

@media (prefers-color-scheme: dark) {
    :root { --bg: #0F172A; --text: #F1F5F9; }
}
```

- The only conditions are `(prefers-color-scheme: dark)` and `(prefers-color-scheme: light)`. Any other `@media` is an error.
- Inside the block go the same rules as outside, including `:root` variables. The block is part of the file in the place where it is written, so it overrides what comes before it at the same weight.
- The host tells the Expander which theme is active. On Android, `NativeView` reads the system theme on every render. A host that has no theme treats it as light.

---

[Index](../../SPEC.md) | Previous: [The file](file.md) | Next: [The body](body.md)

License: [CC BY-SA 4.0](../../LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael.
