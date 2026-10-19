# nativelayout - Specification: Lists

[Index](../../SPEC.md) | Previous: [The body](body.md) | Next: [Layers and groups](layers-and-groups.md)

---

### 5.3 Lists: `key` and `virtual`

Two attributes help with lists written with `for`. They are independent: `key` helps every list that changes, `virtual` is for very long lists. A long list usually uses both.

#### `key`: the identity of an item

`key` gives an element a stable name among its siblings. The engine uses it to know that, between two renders, an item is still the same item.

```html
for task in tasks:
    <div class="card" key="{task.id}">
        <p>{task.title}</p>
    </div>
end
```

- The value is text. Use something that identifies the item and never changes: an id from the data, never the position. `{ }` is allowed.
- It must be unique among siblings. A repeated key is an error.
- Without a written `key`, the engine chooses one by itself (section 5.7): the `key` or the `id` of the item when it has one, and the position otherwise. Position means first with first, second with second.
- With `key`, an item is matched by its key, wherever it is. Adding or removing an item creates or removes only that item and leaves the others alone. Without `key`, adding an item at the start looks like every item changed.
- What the user typed in a field inside an item stays with that item, because it is the same item.
- Changing the order of items that already exist rebuilds that container, so text typed in its fields is lost.
- An element with an `id` outside a `for` is already identified by it. Inside a `for` the identity comes from `key`, or from the automatic key of section 5.7.
- A normal container may mix children with and without `key`.

#### `virtual`: long lists

`virtual` marks a container as a long list. Without it, every item becomes a native view, even the ones far off screen, and with thousands of items that is heavy. With it, only the items on screen (plus about one screen above and one below) have a native view. The others are created when you scroll near them and discarded when you scroll away.

```html
<div virtual style="gap: 1rem">
    for p in products:
        <div class="item" key="{p.id}">
            <p class="name">{p.name}</p>
            <p class="price">{p.price}</p>
        </div>
    end
</div>
```

Everything else works as in any list. A tap on an item, `click(".item")` and `clickIndex()` behave the same, and the index counts over the whole list, not only over the items on screen. In a `virtual` list the `key` also decides which views are kept when the data changes: an item that did not change keeps its view.

Rules:

| Rule | Why |
|---|---|
| `virtual` goes on a container (`div`) that holds only the `for` | the items are the whole content of the container |
| Every item (direct child) has a key: written, or automatic from the `id` of the item (section 5.7) | the engine needs it to keep the views of the items that did not change |
| No loose text and no other elements next to the `for` | they would not be items of the list |
| Without `virtual`, nothing changes | the list is a normal container |
| Prefer no `id` and no `class` on the container; use `style="..."` | a tap on an element with an `id` or a class wakes the program, also a tap on the empty space of the list |

The items are stacked in a column: a `virtual` list is always vertical, and `flex-direction` on it is ignored. `gap`, `padding`, `margin` and `background` of the container work as usual.

Limits:

- A `virtual` list does not scroll by itself: it takes the full height of its items and the screen scrolls (section 5.1). The height of items not shown yet is estimated from the ones already measured, so the scroll position can adjust a little while you scroll.
- The views of items off screen are discarded, so what the user typed in a field (or the state of a checkbox) inside an item is lost when it scrolls away. For items with fields, keep the value in the program's data and give it back with `value="{x.text}"` or `checked="{x.done}"`.
- `virtual` saves native views, not the work of the Expander and the Engine: they still read every item on each `render`. That work runs off the UI thread, so the screen does not freeze, but its cost grows with the list. For tens of thousands of items, hand the program one page of the data at a time.
- On a short list `virtual` brings no gain: leave it out.
- Only containers use `virtual`. On other tags it has no effect.

---

[Index](../../SPEC.md) | Previous: [The body](body.md) | Next: [Layers and groups](layers-and-groups.md)

License: [CC BY-SA 4.0](../../LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael.
