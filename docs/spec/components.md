# nativelayout - Specification: Components and identity

[Index](../../SPEC.md) | Previous: [Layers and groups](layers-and-groups.md) | Next: [Values, control and data](values-and-data.md)

---

*Chapter 5, continued.*

### 5.6 Components: `<include>`

*Designed in v0.3. Not built yet.*

An `<include>` puts another `.nl` file where the tag is written, so the same block can be shared by several screens or repeated in a list.

```html
for p in products:
    <include src="card.nl" key="{p.id}" name="{p.name}" price="{p.price}">
end
```

`card.nl`:

```html
<nativelayout>
<link rel="stylesheet" href="card.css">

<body>
    <div class="card">
        <p class="name">{name}</p>
        <p class="price">{price}</p>
        <input id="qty" type="number" placeholder="Qty">
        <button id="buy">Buy</button>
    </div>
</body>
</nativelayout>
```

- `<include>` has no content and no closing tag, like `<img>`.
- `src` is required. It is the path of the component, relative to the file that has the tag. The loader finds it as it finds a `.css`.
- The component is a normal `.nl` (section 2): `<nativelayout>`, zero or more `<link>` and exactly one `<body>`. The content of its `<body>` takes the place of the tag.
- **Parameters.** Every attribute except `src` and `key` becomes a name inside the component: `name="{p.name}"` gives it the name `name`, and `title="Home"` gives it the text `Home`. In the attributes of an `<include>`, `{ }` accepts any value (text, number, bool, list or map) and hands it over as it is.
- The component sees only its parameters. It does not see the data of the screen nor the variable of the `for` that contains it, so every component can be read on its own.
- A name used in the component and not given is an error that names the component and the line of the `<include>`. A parameter that is not used is not an error.
- `if`, `elif`, `else` and `for` work inside the component, with its parameters.
- **Style.** The `<link>` of the component join the cascade before the links of the screen, so the screen can override the component.
- **Ids.** An `id` in a component counts as if the element was written in place (section 5.7). In a `for`, each instance has its own.
- **Key.** `key` on the `<include>` is the identity of the instance, as for any item of a list (sections 5.3 and 5.7).
- **Where.** Wherever a `div` can go. Not inside text-only tags (`p`, `span`, `h1`, `h2`, `h3`, `button`, `textarea`).
- **Nesting.** Up to 8 levels. A file that includes itself, directly or through another, is an error.
- Not in v1: content handed to the component (slots), and a default value for a parameter.

### 5.7 Identity inside a `for`

*Designed in v0.3. Not built yet.*

What is inside a `for`, or inside an `<include>` that is in a `for`, is repeated. The engine identifies each repetition by itself, so the program works with the items and never with positions.

#### Ids in a `for`

- An `id` inside a `for` names one element per item. A `<button id="buy">` in a list of 50 products is 50 buttons, all named `buy`.
- Outside a `for`, an `id` is unique in the screen, as before. Two `<include>` outside a `for` that hold the same `id` are a `duplicate id` error.
- A `class` works as before.

#### The key of an item

The engine gives each repetition a key, in this order:

1. the `key="..."` written on the repeated element or on the `<include>`;
2. the field `key` of the item, when the item is a map and has it;
3. the field `id` of the item, when the item is a map and has it;
4. the position (0, 1, 2, ...), with a warning when the repeated block has an `id` or a field, because what the user types would follow the position and not the item.

The author writes `key` only when the data has no `id`. The key is applied on every `render`.

#### The program side

```kotlin
fun NativeView.ShopScreen(): Page {
    val s = state { mutableMapOf<String, Any?>("products" to products) }

    if (click("buy")) {
        val p = clicked("p")        // the item of the 'for p in products' where the tap happened
        buy(p, value("qty", p))     // the text of the field 'qty' of that item
    }

    return view("shop.nl", s)
}
```

- `clicked(name)`: once `click(...)` is true, the item of the `for name in ...` that contains the tapped element. `name` is the variable written in the `for`. In nested loops each variable gives its own item. If the tap was not inside that `for`, it is an error.
- `value(id, item...)`, `checked(id, item...)` and `setValue(id, item..., text)` address an element with a repeated id: one item for each enclosing `for`, from the outermost to the innermost. Giving no item for a repeated id is an error that says which item is needed.
- The engine finds the repetition of an item by its key (the rule above). The item can be the same object that was handed in the data, or another one with the same key.
- `click(".class")` and `clickIndex()` keep working for classes.
- The engine reads and returns. It never writes into the data: what to keep is the decision of the program.
- What the user typed in an item follows its key. Reordering a list that has keys keeps each text with its item.
- In a `virtual` list the same rules hold. For an item that is not on screen there is no field to read: `value(id, item)` gives `""`, so keep the text in the data (section 5.3).

---

[Index](../../SPEC.md) | Previous: [Layers and groups](layers-and-groups.md) | Next: [Values, control and data](values-and-data.md)

License: [CC BY-SA 4.0](../../LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael.
