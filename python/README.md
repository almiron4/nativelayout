# nativelayout (Python)

- `core/`     puro Python, sem Kivy (port do core Kotlin v1.1.0)
- `android/`  host Kivy + app Android
- `desktop/`  host Kivy + app desktop
- `examples/`

`nativelayout` e um pacote de namespace (sem `__init__.py` na raiz): o core esta em
`core/nativelayout/core` e cada host acrescenta `nativelayout/host` na sua pasta.
Para usar o core: colocar `python/core` no `sys.path`.
