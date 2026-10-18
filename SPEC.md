# nativelayout - Specification

Status: draft v0.2. It replaces the draft where the markup lived inside Kotlin code.

License: this specification is licensed under [CC BY-SA 4.0](LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael. The code examples in this document are also available under the [MPL 2.0](LICENSE).

---

## 1. Idea

A screen is written once, in a plain text file that looks like HTML. The same file works in any language: each language only supplies the program side and the part that draws native components.

- The file is HTML you already know, plus three additions: `{value}`, `if` / `for` lines, and a `<style>` with simple selectors.
- It has no functions, no classes and no variables. The logic stays outside, in each language.
- It is not a canvas that only draws: the file has conditions and repetition.
- The rule for every decision is the same: simple, nothing ambiguous.

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
| Box | `padding`, `margin` (and `-top`, `-right`, `-bottom`, `-left` of each), `border` (and `border-width`, `border-color`, `border-style`), `border-radius`, `box-shadow` |
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

## 5. Body

### 5.1 Tags

| Tag | Native component |
|---|---|
| `div` | container |
| `span`, `p`, `h1`, `h2`, `h3` | text |
| `button` | button (text only) |
| `input` (`text`, `password`, `number`, `checkbox`) | field or checkbox |
| `textarea` | multi-line field |
| `img` | image |
| `ul`, `li` | list; each `li` gets a bullet |

A tag outside this list is an error.

The whole screen scrolls: the root sits in one vertical scroll container. A long list scrolls the screen (for very long lists, see `virtual` in section 5.3), and a bar pinned to the bottom is not possible in v1.

### 5.2 Attributes

| Attribute | Meaning |
|---|---|
| `id` | unique name; links the element to the program |
| `class` | style group, and also a group for the program |
| `style` | inline style |
| `placeholder` | hint of a field |
| `type` | type of an `input` |
| `value` | initial text of a field |
| `checked` | initial state of a checkbox |
| `disabled`, `readonly` | blocks the element |
| `src`, `alt` | image: a `data:` URI or a name resolved by the host; no network in v1 |
| `rows` | lines of a `textarea` |
| `key` | stable name of an item among its siblings (section 5.3) |
| `virtual` | marks a container as a long list (section 5.3) |

- `checked`, `disabled` and `readonly` count when present, whatever their text. With a value (`checked="{done}"`) the attribute is written only when the value is `true`.
- `id` and `class` cannot have a value: `id="{x}"` is an error. A class changes with `if` and `else`, writing each variant in full.
- An `id` is unique in the screen.
- `virtual` counts when present, whatever its text.
- `key` accepts values: `key="{task.id}"`.
- A tap on an element that has an `id` or a class wakes the program (section 9).

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
- Without `key`, siblings are matched by position (first with first, second with second).
- With `key`, an item is matched by its key, wherever it is. Adding or removing an item creates or removes only that item and leaves the others alone. Without `key`, adding an item at the start looks like every item changed.
- What the user typed in a field inside an item stays with that item, because it is the same item.
- Changing the order of items that already exist rebuilds that container, so text typed in its fields is lost.
- An element with an `id` is already identified by it and does not need a `key`. `key` is for repeated elements, which cannot have an `id`.
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
| Every item (direct child) has a `key` | the engine needs it to keep the views of the items that did not change |
| No loose text and no other elements next to the `for` | they would be items without a `key`, which is an error |
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

## 6. Values and control

### 6.1 Values: `{name}`

Shows the value of a name, as text and escaped. It works in text and in attribute values (not `id` or `class`).

```html
<p>{task.title} - {pending} pending</p>
<input id="title" value="{draft}">
```

Only names with dots are allowed inside `{ }`: `{pending}`, `{task.title}`. A literal brace is written `\{` and `\}`. Inside `<style>` the braces are CSS and are not values.

### 6.2 Control lines

A line is control when, after trimming, it is exactly one of these:

```
if condition:
elif condition:
else:
for item in list:
end
```

- The `:` is the last character of the line, with no space before it: `if pending == 0:`.
- Control is alone on its line, between tags. A line that starts with `if` but does not end with `:` is ordinary text.
- `end` closes the innermost open `if` or `for`. `elif` and `else` continue the `if` and do not need their own `end`.
- `for item in list:` repeats its block once per element. `item` exists only inside the block, and cannot reuse a name that already exists.

### 6.3 Conditions

```
condition := or
or        := and ("or" and)*
and       := not ("and" not)*
not       := "not" not | compare | "(" condition ")"
compare   := operand (("==" | "!=" | "<" | ">" | "<=" | ">=") operand)?
operand   := name | number | "text" | true | false
```

- A name alone must be a `bool`.
- A list or a text has the property `size`: `if tasks.size == 0:`.
- There is no arithmetic. `{n + 1}` is an error: the program calculates and hands over `next`.

---

## 7. Data

The program hands the data to `render`. The data is plain, like JSON: text, number, bool, list and map. A name in the file is looked up in it; a name that is missing is an error.

```
pending = 2
tasks   = [ { title: "Buy bread", done: false }, { title: "Call Ana", done: true } ]
```

Kotlin uses `Map` and `List`, Python uses `dict` and `list`, so the same file reads the same data in any language. A name that holds a list or a map cannot be shown with `{ }`: use `for` or a name inside it.

---

## 8. The program side

The file only describes. The logic (state, taps) is written in each language, with the same verbs. Each screen has its own logic file next to its `.nl`, and the entry point (`MainActivity`, `main.py`) only prepares and starts the first screen.

```
app/
  MainActivity.kt        (only prepares)
  login.nl   login.css   LoginScreen.kt
  panel.nl   panel.css   PanelScreen.kt
```

### 8.1 The screen function

A screen is one function. It holds the state, reads the taps and returns what to show. The engine calls it, shows what it returns, waits for a tap and calls it again, until the window closes.

JS (illustrative):

```js
export default function PanelScreen() {
  const s = state({ pending: 0 })

  if (click("add")) s.pending++

  return view("panel.nl", s)
}
```

Kotlin:

```kotlin
fun NativeView.PanelScreen(): Page {
    val s = state { mutableMapOf<String, Any?>("pending" to 0) }

    if (click("add")) s["pending"] = (s["pending"] as Int) + 1

    return view("panel.nl", s)
}
```

Python (illustrative, not built yet):

```python
def panel_screen():
    s = state({"pending": 0})

    if click("add"):
        s["pending"] += 1

    return view("panel.nl", s)
```

Going to another screen:

```kotlin
fun NativeView.LoginScreen(): Page {
    val s = state { mutableMapOf<String, Any?>("failed" to false) }

    if (click("login")) {
        if (value("pass") == "1234") return go { PanelScreen() }
        s["failed"] = true
    }

    return view("login.nl", s)
}
```

### 8.2 Verbs

| Verb | Python style | Does |
|---|---|---|
| `state(initial)` | `state` | the state of the screen: the same object on every call, a new one after `go` |
| `view(screen, data)` | `view` | returns the screen to show, with its data |
| `go(function)` | `go` | returns a jump to another screen function |
| `click(target)` | `click` | `true` if the `id` (or `".class"`) was tapped |
| `clickIndex()` | `click_index` | position, from 0, of the tapped element of the class |
| `value(id)` | `value` | text of a field, `""` if the id does not exist |
| `checked(id)` | `checked` | state of a checkbox, `false` if it does not exist |
| `setValue(id, text)` | `set_value` | fills or clears a field (`"1"` / `"0"` for a checkbox) |
| `save(key, text)`, `load(key)`, `exists(key)`, `delete(key)` | same | store text on the device |
| `render(screen, data)` | `render` | base layer (section 8.5): shows the screen, waits for a tap, returns `true`; `false` when the window closes |

### 8.3 Rules

- The function is called again after every tap. In the first call there is no tap yet, so `click` is `false`.
- `state(initial)` gives the same object on every call, so what changed survives. Call it at the top, always in the same order, never inside `if` or `for`.
- The data given to `view` is plain, as in section 7.
- `go(function)` leaves the current screen. Its state is dropped, the tree is built from scratch (section 9, point 6) and the new function is called at once, with no tap yet. More than 8 `go` in a row without a `view` is an error.
- A screen file is read once and kept, so `view` is cheap on every call.
- Coming back to a screen starts it again with a new state. To keep something between screens, use `save` and `load`.

Examples of `go`:

A loop that never shows a screen is an error. Here each function jumps to the other, so at the ninth jump in a row the engine stops with `go: too many jumps without showing a screen`. A function that always jumps to itself fails the same way.

```kotlin
fun NativeView.AScreen(): Page = go { BScreen() }
fun NativeView.BScreen(): Page = go { AScreen() }
```

A few jumps in a row are fine. Every `view` resets the count, so up to 8 jumps can come before a screen is shown:

```kotlin
fun NativeView.SplashScreen(): Page {
    if (!exists("token")) return go { LoginScreen() }
    return go { HomeScreen() }
}
```

The usual case is a jump that depends on a tap:

```kotlin
fun NativeView.LoginScreen(): Page {
    val s = state { mutableMapOf<String, Any?>("failed" to false) }

    if (click("login")) {
        if (value("pass") == "1234") return go { HomeScreen() }
        s["failed"] = true
    }

    return view("login.nl", s)
}
```

### 8.4 The entry point

The entry point creates the area and the loader and names the first screen function. It has no logic. The parameter names are provisional; use named arguments.

```kotlin
class MainActivity : Activity() {
    private lateinit var ui: NativeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val area = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        setContentView(area)

        ui = NativeView(
            context = this,
            area = area,
            loader = { name -> runCatching { assets.open(name).bufferedReader().use { it.readText() } }.getOrNull() },
            app = { LoginScreen() }
        )
    }

    override fun onDestroy() {
        ui.close()
        super.onDestroy()
    }
}
```

### 8.5 Base layer: the `render` loop

The screen function runs on top of this loop, which stays available for programs that want to control it themselves.

Kotlin (Android):

```kotlin
class MainActivity : Activity() {
    private lateinit var ui: NativeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = NativeView(this) {
            val panel = screen("panel.nl")
            var pending = 0

            while (render(panel, mapOf("pending" to pending))) {
                if (click("add")) pending++
            }
        }
    }

    override fun onDestroy() {
        ui.close()
        super.onDestroy()
    }
}
```

Python (illustrative, not built yet):

```python
with NativeView(app) as ui:
    panel = screen("panel.nl")
    pending = 0

    while ui.render(panel, {"pending": pending}):
        if ui.click("add"):
            pending += 1
```

---

## 9. render

1. The first call builds the native components.
2. The next calls compare with the previous screen and change only what differs. Elements are matched by `id`, by `key` or by position (section 5.3). The text typed in a field with an `id` is kept.
3. `value` and `checked` in the file are default values: they are applied when the field is created and again only when the value in the data is different from the previous `render`. What the user typed in between is not touched.
4. The program waits inside `render` until there is a tap on an element with an `id` or a class. Typing does not wake it.
5. Each tap goes into a queue and each `render` consumes one, in order.
6. Passing a different screen resets the tree and builds it again.
7. When the window closes, `render` returns `false` and the loop ends.

---

## 10. Errors

Same format as the other messages, with the line of the `.nl` file.

| Situation | Message |
|---|---|
| Identity missing | `the file must start with <nativelayout> and end with </nativelayout>` |
| Unknown selector | `style: the selector 'ul li' is not supported (line 8)` |
| Undefined variable | `style: the variable '--bg' is not defined in ':root'` |
| Unsupported tag | `markup: the tag 'table' is not supported (line 18)` |
| `<script>` | `markup: <script> is not allowed` |
| Repeated id | `markup: duplicate id 'add'` |
| Repeated key | `markup: duplicate key 'p1'` |
| Item without key in a `virtual` container | `markup: every child of a 'virtual' container needs a key (line 12)` |
| Id or class with a value | `markup: 'id' cannot be generated by value` |
| `end` without a block | `'end' has no matching 'if' or 'for' (line 20)` |
| Block not closed | `the 'if' opened on line 12 is never closed with 'end'` |
| `else` or `elif` without `if` | `'else' has no matching 'if' (line 14)` |
| Space before the colon | `write 'if pending == 0:' without a space before ':'` |
| Unknown name | `the name 'pending' was not given by the program (line 12)` |
| List shown with `{ }` | `'{tasks}' is a list; use 'for task in tasks:'` |
| Bad condition | `invalid condition: 'pending ==' (line 12)` |
| Arithmetic | `arithmetic is not supported in '{n + 1}'; give the program's result as a name` |
| `render` with another type | `render: expected a screen` |
| Too many jumps | `go: too many jumps without showing a screen` |

---

## 11. Targets

| Target | Status | Notes (proposal) |
|---|---|---|
| Android, Kotlin | engine and host exist | needs the expander and the verbs |
| Python, Kivy | planned | flex and rounded corners drawn by hand |
| Python, Tkinter | planned | no flex and no rounded corners: gap and radius ignored with a warning |

Each target supports a subset of the style. What a target cannot draw gives a warning, never an error.

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

## 14. Open decisions

1. **`elif`.** Chosen over `else if`, because it is one word, as in Python. Confirm.
2. **Extension.** `.nl` is a proposal.
3. **Reuse.** v1 has no components, so a card cannot be shared between two screens. A tag such as `<include src="card.nl">` is not designed.
4. **Engine per language.** The expander ends in HTML with inline styles, and each language then needs an engine that reads it (parser, style, diff). Either it is ported to each language, or one shared engine is used with bindings.
5. **Loading.** How `screen("panel.nl")` finds the file: Android assets, resources, or embedded at build time.
6. **Index in loops.** `for i, item in list:` is not in v1.
7. **Arithmetic.** Not in v1. The program calculates.
8. **Style classes and taps.** A tap on any element with an id or a class wakes `render`, so a tap on a text that only has a style class redraws the screen for nothing. Harmless, but classes for style and for events could be separated.
9. **Selectors.** Only simple ones in v1. A descendant selector (`.card p`) could come later.
10. **List items.** Settled: the diff matches elements by `id`, by `key` or by position (section 5.3). Reordering existing items still rebuilds the container; a finer move is not designed.
11. **Scrolling.** The root is the only scroll container. A `virtual` list scrolls with the screen; it is not a second one.
12. **Values in attributes.** `{ }` is allowed in attribute values except `id` and `class`. Confirm.
13. **Units.** `rem` scales with the font scale of the system, also in padding and gaps. Confirm.
14. **Virtual lists.** Vertical only, and the Expander still reads every item. A lazy expander that reads only the visible items is not designed, nor is a horizontal `virtual` list. Text typed in an item that scrolls away is not kept.
15. **State slots.** `state` is matched by call order, like hooks, so it cannot be called inside `if` or `for`. A named form, `state("name", initial)`, is not designed.