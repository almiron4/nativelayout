# nativelayout - Specification

Status: draft v0.2. It replaces the draft where the markup lived inside Kotlin code.

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

Not supported in v1: `a b`, `a > b`, `:hover` and other pseudo-classes, attribute selectors, `@media`. They are an error. `!important` is ignored.

**Cascade.** The weight is: tag, then class, then id, then the `style="..."` attribute, each overriding the one before. At the same weight, the later rule wins.

### 4.2 Variables

```css
:root { --bg: #F2F2F2; --accent: #2F81F7; }
body  { background: var(--bg); }
```

A variable that is not declared in `:root` is an error.

### 4.3 Properties

`color`, `background` (and `background-color`), `font-size`, `font-weight`, `padding`, `margin`, `border-radius`, `border`, `display` (`flex` or `block`), `flex-direction`, `flex`, `justify-content`, `align-items`, `gap`, `width`, `height`.

- Units: `rem`, `px` and `0`. `px` means dp (density-independent). `1rem = 16dp` times the font scale of the system. A percentage is accepted only in `width` and `height`.
- Colors: `#RGB`, `#RGBA`, `#RRGGBB`, `#RRGGBBAA`, color names (`red`, `teal`, ...), `rgb()`, `rgba()`, `transparent` and `var(--name)`.
- Color, font size and bold are inherited by the children.
- The default direction is column and the default alignment is stretch.
- Tags carry default styles: headings are bold with vertical margins, `p` has a vertical margin of 1rem, a `button` is gray with padding, a text field has a border. Write `margin: 0` when you do not want the margin.
- An unknown property gives a warning and is ignored.

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

The whole screen scrolls: the root sits in one vertical scroll container. A long list scrolls the screen, and a bar pinned to the bottom is not possible in v1.

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

- `checked`, `disabled` and `readonly` count when present, whatever their text. With a value (`checked="{done}"`) the attribute is written only when the value is `true`.
- `id` and `class` cannot have a value: `id="{x}"` is an error. A class changes with `if` and `else`, writing each variant in full.
- An `id` is unique in the screen.
- A tap on an element that has an `id` or a class wakes the program (section 9).

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

The file only describes. The logic (state, taps) is written in each language, with the same verbs:

| Verb | Python style | Does |
|---|---|---|
| `render(screen, data)` | `render` | shows the screen with the data, waits for a tap, returns `true`; `false` when the window closes |
| `click(target)` | `click` | `true` if the `id` (or `".class"`) was tapped |
| `clickIndex()` | `click_index` | position, from 0, of the tapped element of the class |
| `value(id)` | `value` | text of a field, `""` if the id does not exist |
| `checked(id)` | `checked` | state of a checkbox, `false` if it does not exist |
| `setValue(id, text)` | `set_value` | fills or clears a field (`"1"` / `"0"` for a checkbox) |
| `save(key, text)`, `load(key)`, `exists(key)`, `delete(key)` | same | store text on the device |

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
2. The next calls compare with the previous screen and change only what differs. The text typed in a field with an `id` is kept.
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

---

## 13. Good practices

- Keep the logic out of the file. A name in `{ }` should already be calculated by the program.
- Use `id` for single elements (buttons, fields) and `class` for repeated ones (cards).
- Put colors in `:root` and use `var(--name)`, so a theme change is one edit.
- Write one variant per state (`.right`, `.wrong`) instead of computing a class.
- Give a field a `value="{name}"` to open it with a stored text.

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
10. **List items.** The diff matches elements by id, or by position. A `key` attribute is not designed.
11. **Scrolling.** The root is the only scroll container.
12. **Values in attributes.** `{ }` is allowed in attribute values except `id` and `class`. Confirm.
13. **Units.** `rem` scales with the font scale of the system, also in padding and gaps. Confirm.