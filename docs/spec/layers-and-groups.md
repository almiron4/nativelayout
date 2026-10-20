# nativelayout - Specification: Layers and groups

[Index](../../SPEC.md) | Previous: [Lists](lists.md) | Next: [Components and identity](components.md)

---

### 5.4 Layers: `<dialog>`

A `<dialog>` is a layer drawn over the whole screen. It is how a dialog, a menu or a bottom sheet is written, with the same tools as the rest of the screen.

`confirm.nl`:

```html
<nativelayout>
<link rel="stylesheet" href="confirm.css">

<body>
    <h1>Tasks</h1>

    if confirm:
        <dialog id="layer" style="justify-content: flex-end; align-items: stretch">
            <div class="sheet">
                <p>Clear the finished tasks?</p>
                <button id="yes">Clear</button>
                <button id="no">Cancel</button>
            </div>
        </dialog>
    end
</body>
</nativelayout>
```

`confirm.css`:

```css
.sheet { background: white; padding: 1rem; border-radius: 16px 16px 0 0; gap: 0.5rem; }
```

- The `<dialog>` element is the layer itself. Its style is the style of the layer: the default is a dimmed background (`rgba(0,0,0,0.5)`) with the content centered, and you change it as you like. `justify-content` and `align-items` place the content: `center` and `center` make a dialog, `flex-end` and `stretch` make a bottom sheet, `flex-start` and `stretch` a top bar.
- The content is a normal block. Its size, background, border and corners are written in its own style (for the sheet above, `border-radius: 16px 16px 0 0`).
- The layer takes every tap, so nothing under it is touched. If the `<dialog>` has an `id` or a class, a tap on the layer outside the content wakes the program like any other tap. That is how a tap outside closes the dialog.
- Whether the dialog is shown is the program's data, with `if`. The file does not open or close anything.
- A `<dialog>` goes at the first level of the screen, next to the other blocks. Inside another block it is an error. It cannot be inside a `virtual` container.
- Several dialogs are drawn in the order they are written, over the other blocks, which keep their order. A dialog is always placed after them in the tree.
- The back button of the system reaches the program as `back()` (section 8.6): the program closes the dialog by changing its data. The entrance animation is in section 4.6. Dragging the sheet is the job of the host and is not in the file.
- What the operating system asks the user (permissions, choosing a file or a date, sharing) is not a layer: it is a verb of the program's API, not part of the `.nl`.

### 5.5 Groups: `<fieldset>` and `<legend>`

A `<fieldset>` groups related fields. By default it has a thin gray border, rounded corners and padding. A `<legend>` is its title, bold and first inside the group.

```
<fieldset>
    <legend>Delivery</legend>
    <input id="street" type="text" placeholder="Street">
    <input id="city" type="text" placeholder="City">
</fieldset>
```

- The group is a container like a `div`; the border, padding and corners are a default style you can change.
- With the `disabled` attribute, the group blocks every button, field and checkbox inside it, as in HTML.
- The `<legend>` is shown as the first line inside the border, and not over the border line as in a browser.
- A `<legend>` outside a `<fieldset>` is an error.
- To put the legend on the border line, as a browser does, pull it up with a negative margin and give it the background of the screen. `align-self: flex-start` makes it as wide as its text, so the line stays visible on both sides:

```css
fieldset { padding-top: 1.25rem; margin-top: 0.75rem; }
legend   { align-self: flex-start; margin-top: -2rem; margin-left: 0.5rem; padding: 0.25rem;
           background: var(--bg); }
```

---

[Index](../../SPEC.md) | Previous: [Lists](lists.md) | Next: [Components and identity](components.md)

License: [CC BY-SA 4.0](../../LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael.
