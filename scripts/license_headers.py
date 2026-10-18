#!/usr/bin/env python3
# Author: Adilson C. Rafael (adilson889)
# Date: 2026-10-18
"""
Makes sure every Kotlin file (.kt, .kts) carries the MPL 2.0 notice (Exhibit A).

  python3 scripts/license_headers.py --check   list the files without the notice, exit 1 if any
  python3 scripts/license_headers.py           add the notice to those files

Rules:
- A file that already has a header comment with "Author:" keeps it; the notice is added inside
  the same comment, after a blank line.
- A file without a header gets a new comment with only the notice (no author is invented).
- A file that already mentions the Mozilla Public License near the top is left alone.
"""
import pathlib
import subprocess
import sys

NOTICE = [
    "This Source Code Form is subject to the terms of the Mozilla Public",
    "License, v. 2.0. If a copy of the MPL was not distributed with this",
    "file, You can obtain one at https://mozilla.org/MPL/2.0/.",
]
EXTENSIONS = (".kt", ".kts")
SKIP_PARTS = {"build", ".gradle", ".git", "node_modules"}


def tracked_files():
    root = pathlib.Path(__file__).resolve().parent.parent
    out = subprocess.run(["git", "ls-files"], cwd=root, capture_output=True, text=True, check=True).stdout
    for line in out.splitlines():
        p = root / line
        if p.suffix in EXTENSIONS and not (SKIP_PARTS & set(p.relative_to(root).parts)):
            yield root, p


def has_notice(text):
    return "Mozilla Public License" in "\n".join(text.splitlines()[:20])


def add_notice(text):
    bom = "\ufeff" if text.startswith("\ufeff") else ""
    body = text[len(bom):]
    block = ["/*"] + [" * " + l for l in NOTICE] + [" */"]
    if body.lstrip().startswith("/*"):
        start = body.index("/*")
        end = body.find("*/", start)
        head = body[start:end]
        if end != -1 and "Author:" in head:
            before = body[:end].rstrip(" \t")
            if not before.endswith("\n"):
                before += "\n"
            lines = [" *"] + [" * " + l for l in NOTICE]
            return bom + before + "\n".join(lines) + "\n " + body[end:]
    return bom + "\n".join(block) + "\n\n" + body


def main():
    check = "--check" in sys.argv[1:]
    missing = []
    for root, p in tracked_files():
        text = p.read_text(encoding="utf-8")
        if has_notice(text):
            continue
        missing.append(p.relative_to(root))
        if not check:
            p.write_text(add_notice(text), encoding="utf-8")
    if check:
        if missing:
            print("These files have no MPL 2.0 notice:")
            for m in missing:
                print("  " + str(m))
            print("Run the workflow 'License headers' with mode 'fix', or: python3 scripts/license_headers.py")
            sys.exit(1)
        print("All files have the notice.")
    else:
        print(f"Notice added to {len(missing)} file(s).")


if __name__ == "__main__":
    main()
