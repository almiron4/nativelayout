# nativelayout - Specification: Values, control and data

[Index](../../SPEC.md) | Previous: [Header and icons](header-and-icons.md) | Next: [The program side](program-side.md)

---

## 6. Values and control

### 6.1 Values: `{name}`

Shows the value of a name, as text and escaped. It works in text and in attribute values (not `id` or `class`).

```html
<p>{task.title} - {pending} pending</p>
<input id="title" value="{draft}">
```

Only names with dots are allowed inside `{ }`: `{pending}`, `{task.title}`. The attributes of an `<include>` are the one place where `{ }` may hold a list or a map (section 5.6). A literal brace is written `\{` and `\}`. Inside `<style>` the braces are CSS and are not values.

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

[Index](../../SPEC.md) | Previous: [Header and icons](header-and-icons.md) | Next: [The program side](program-side.md)

License: [CC BY-SA 4.0](../../LICENSE-SPEC). Copyright (c) 2026 Adilson C. Rafael.
