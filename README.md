# nativelayout

Create native interfaces from HTML, in any language.

A screen is written once, in a plain text file that looks like HTML: see `SPEC.md`.
The same file works in any language; each language only supplies the Expander, the
Engine and the native host. This repository holds one implementation per language,
each in its own folder.

## Implementations

- [`android/`](android/) — Kotlin, native Android views. Published as a library
  (`core` + `android` modules) and includes an editor app (`android/app`) with
  live preview, used to test screens on-device.

No other language has an implementation yet.

## The file format

```html
<nativelayout>

<link rel="stylesheet" href="panel.css">

<body>
    <!-- the screen -->
</body>

</nativelayout>
```

See `SPEC.md` for the full format and `android/README.md` for how to build or
depend on the Android implementation.
