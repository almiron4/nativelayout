# nativelayout - Specification: Examples

[Index](../../SPEC.md) | Previous: [Errors and targets](errors-and-targets.md) | Next: [Good practices](good-practices.md)

---

## 12. Examples

### 12.1 Counter (basics)

```html
<nativelayout>

<style>
    body { flex: 1; justify-content: center; align-items: center; gap: 1rem; }
    .big { font-size: 3rem; font-weight: bold; }
    .row { flex-direction: row; gap: 0.75rem; }
</style>

<body>
    <p class="big">{n}</p>
    <p>twice: {twice}</p>
    <div class="row">
        <button id="minus">-</button>
        <button id="plus">+</button>
    </div>
</body>

</nativelayout>
```

```kotlin
val screen = screen("counter.nl")
var n = 0

while (render(screen, mapOf("n" to n, "twice" to n * 2))) {
    if (click("plus")) n++
    if (click("minus")) n--
}
```

### 12.2 Login (forms)

```html
<nativelayout>

<style>
    :root  { --line: #D0CCC0; --ok: #238636; --bad: #CF222E; }
    body   { flex: 1; justify-content: center; padding: 1.75rem; gap: 0.75rem; background: #F0EEE8; }
    input  { padding: 0.875rem; border: 1px solid var(--line); border-radius: 0.75rem; background: #FFFFFF; }
    #login { background: var(--ok); color: #FFFFFF; font-weight: bold; border-radius: 0.75rem; padding: 0.875rem; }
    .error { color: var(--bad); }
</style>

<body>
    if signed:
        <h1>Welcome, {user}</h1>
        <button id="logout">Sign out</button>
    else:
        <h1>Sign in</h1>
        <input id="user" placeholder="Username">
        <input id="pass" type="password" placeholder="Password">
        <button id="login">Sign in</button>
        if failed:
            <p class="error">Invalid username or password.</p>
        end
    end
</body>

</nativelayout>
```

```kotlin
var signed = false
var failed = false
var user = ""

while (render(login, mapOf("signed" to signed, "failed" to failed, "user" to user))) {
    if (click("login")) {
        user = value("user")
        signed = user.isNotBlank() && value("pass") == "1234"
        failed = !signed
    }
    if (click("logout")) { signed = false; failed = false }
}
```

### 12.3 Task list (lists and class taps)

```html
<nativelayout>

<style>
    :root { --bg: #F2F2F2; --card: #EDE9FE; --accent: #2F81F7; }

    body  { background: var(--bg); padding: 1.25rem; gap: 1rem; }
    .card { background: var(--card); border-radius: 0.75rem; padding: 0.875rem; }
    .bold { font-weight: bold; }
    #add  { background: var(--accent); color: #FFFFFF; font-weight: bold; }
</style>

<body>
    <h1>Tasks</h1>

    if pending == 0:
        <p>Nothing to do.</p>
    else:
        <p>{pending} pending</p>
    end

    <input id="title" placeholder="New task">
    <button id="add">Add</button>

    for task in tasks:
        <div class="card">
            if task.done:
                <p>{task.title} (done)</p>
            else:
                <p class="bold">{task.title}</p>
            end
        </div>
    end
</body>

</nativelayout>
```

```kotlin
val tasks = mutableListOf<MutableMap<String, Any>>(
    mutableMapOf("title" to "Test nativelayout", "done" to true),
    mutableMapOf("title" to "Write the documentation", "done" to false)
)

while (render(panel, mapOf("pending" to tasks.count { it["done"] == false }, "tasks" to tasks))) {
    if (click("add")) {
        val title = value("title")
        if (title.isNotBlank()) {
            tasks.add(mutableMapOf("title" to title, "done" to false))
            setValue("title", "")
        }
    }
    if (click(".card")) {
        val task = tasks[clickIndex()]
        task["done"] = task["done"] != true
    }
}
```

### 12.4 Quiz (education)

```html
<nativelayout>

<style>
    body    { flex: 1; padding: 1.25rem; gap: 1rem; }
    .muted  { color: #6B7280; }
    .option { background: #E5E7EB; padding: 0.875rem; border-radius: 0.75rem; }
    .right  { background: #86EFAC; padding: 0.875rem; border-radius: 0.75rem; }
    .wrong  { background: #FCA5A5; padding: 0.875rem; border-radius: 0.75rem; }
    #next   { background: #2F81F7; color: #FFFFFF; font-weight: bold; padding: 0.875rem; border-radius: 0.75rem; }
</style>

<body>
    if finished:
        <h1>Result</h1>
        <p>{score} / {total}</p>
        <button id="restart">Try again</button>
    else:
        <p class="muted">Question {number} of {total}</p>
        <h2>{question}</h2>
        for opt in options:
            if opt.state == "right":
                <button class="right">{opt.label}</button>
            elif opt.state == "wrong":
                <button class="wrong">{opt.label}</button>
            else:
                <button class="option">{opt.label}</button>
            end
        end
        if answered:
            <button id="next">Next</button>
        end
    end
</body>

</nativelayout>
```

```kotlin
// the options are built before each render, with their state
val options = q.options.mapIndexed { i, label ->
    val state = when {
        picked == -1 -> "idle"
        i == q.answer -> "right"
        i == picked -> "wrong"
        else -> "idle"
    }
    mapOf("label" to label, "state" to state)
}

if (click(".option")) picked = clickIndex()   // only idle options use this class
if (click("next")) { index++; picked = -1 }
```

### 12.5 Settings (device storage)

```html
<nativelayout>

<style>
    body  { flex: 1; padding: 1.25rem; gap: 1rem; }
    .row  { flex-direction: row; align-items: center; gap: 0.5rem; }
    .grow { flex: 1; }
    .ok   { color: #16A34A; }
</style>

<body>
    <h1>Settings</h1>
    <input id="name" placeholder="Display name" value="{name}">
    <div class="row">
        <input type="checkbox" id="dark" checked="{dark}">
        <p class="grow">Dark mode</p>
    </div>
    <div class="row">
        <input type="checkbox" id="notify" checked="{notify}">
        <p class="grow">Notifications</p>
    </div>
    <button id="save">Save</button>
    <button id="reset">Reset</button>
    if saved:
        <p class="ok">Settings saved.</p>
    end
</body>

</nativelayout>
```

```kotlin
var name = load("name")
var dark = load("dark") == "1"
var notify = if (exists("notify")) load("notify") == "1" else true
var saved = false

while (render(settings, mapOf("name" to name, "dark" to dark, "notify" to notify, "saved" to saved))) {
    if (click("save")) {
        name = value("name")
        dark = checked("dark")
        notify = checked("notify")
        save("name", name)
        save("dark", if (dark) "1" else "0")
        save("notify", if (notify) "1" else "0")
        saved = true
    }
    if (click("reset")) {
        delete("name"); delete("dark"); delete("notify")
        name = ""; dark = false; notify = true; saved = false
        setValue("name", ""); setValue("dark", "0"); setValue("notify", "1")
    }
}
```

### 12.6 Large list (`key` and `virtual`)

```html
<nativelayout>

<style>
    body   { flex: 1; padding: 1rem; gap: 1rem; }
    .muted { color: #6B7280; }
    .item  { flex-direction: row; align-items: center; gap: 0.75rem; padding: 0.75rem; border-radius: 1rem; background: #F3EDF7; }
    .grow  { flex: 1; }
    .name  { font-weight: bold; }
</style>

<body>
    <h1>Products</h1>
    <p class="muted">{total} items</p>

    <div virtual style="gap: 0.5rem">
        for p in products:
            <div class="item" key="{p.id}">
                <div class="grow">
                    <p class="name">{p.name}</p>
                    <p class="muted">{p.price}</p>
                </div>
                if p.picked:
                    <p>Picked</p>
                end
            </div>
        end
    </div>
</body>

</nativelayout>
```

```kotlin
fun products(picked: Int) = (1..2000).map { i ->
    mapOf("id" to i, "name" to "Item $i", "price" to "%.2f".format(i * 1.07), "picked" to (i - 1 == picked))
}

var picked = -1
val screen = screen("products.nl")

while (render(screen, mapOf("total" to 2000, "products" to products(picked)))) {
    if (click(".item")) picked = clickIndex()
}
```

Tapping an item changes only that item: the others keep their view.

### 12.7 Components in a list (`include`)

*Designed in v0.3. Not built yet.* A product card written once and repeated, with a button and a field per product.

`card.nl` (from section 5.6) and `shop.nl`:

```html
<nativelayout>
<link rel="stylesheet" href="shop.css">

<body>
    <h1>Shop</h1>

    for p in products:
        <include src="card.nl" name="{p.name}" price="{p.price}">
    end
</body>
</nativelayout>
```

The products have an `id`, so no `key` is written. The program:

```kotlin
fun NativeView.ShopScreen(): Page {
    val s = state { mutableMapOf<String, Any?>("products" to loadProducts(), "added" to 0) }

    if (click("buy")) {
        val p = clicked("p")
        val qty = value("qty", p).ifEmpty { "1" }
        addToCart(p, qty.toInt())
        s["added"] = (s["added"] as Int) + 1
        setValue("qty", p, "")
    }

    return view("shop.nl", s)
}
```

---

[Index](../../SPEC.md) | Previous: [Errors and targets](errors-and-targets.md) | Next: [Good practices](good-practices.md)

License: [CC BY-SA 4.0](../../LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael.
