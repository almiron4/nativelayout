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
- A file that already mentions the Mozilla Public License near the top is left alone, and a notice repeated in the header is reduced to one copy.
"""
import pathlib
import re
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
    # The notice is wrapped over several comment lines, so join them (without the " * ") before looking.
    head = " ".join(line.strip().lstrip("*/ ").strip() for line in text.splitlines()[:20])
    return "Mozilla Public License" in head


# One copy of the notice, with the blank comment line that may come before it.
NOTICE_COPY = re.compile(
    r"(?:\n \*)?\n \* " + r"\n \* ".join(re.escape(l) for l in NOTICE)
)


# A comment that holds only the notice (what an earlier version of this script added on each run).
NOTICE_BLOCK = "/*\n * " + "\n * ".join(NOTICE) + "\n */\n\n"


def remove_repeated(text):
    """Keeps one copy of the notice in the header and drops the others."""
    # a notice-only comment followed by another comment that already has the notice
    while text.startswith(NOTICE_BLOCK):
        rest = text[len(NOTICE_BLOCK):]
        close = rest.find("*/")
        if not (rest.startswith("/*") and close != -1 and NOTICE[0] in rest[:close]):
            break
        text = rest
    end = text.find("*/")
    if end == -1:
        return text
    head, rest = text[:end], text[end:]
    found = list(NOTICE_COPY.finditer(head))
    if len(found) < 2:
        return text
    for m in reversed(found[1:]):
        head = head[:m.start()] + head[m.end():]
    return head + rest


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
    repeated = []
    for root, p in tracked_files():
        text = p.read_text(encoding="utf-8")
        fixed = remove_repeated(text)
        if fixed != text:
            repeated.append(p.relative_to(root))
        if not has_notice(fixed):
            missing.append(p.relative_to(root))
            fixed = add_notice(fixed)
        if not check and fixed != text:
            p.write_text(fixed, encoding="utf-8")
    if check:
        if missing or repeated:
            if missing:
                print("These files have no MPL 2.0 notice:")
                for m in missing:
                    print("  " + str(m))
            if repeated:
                print("These files repeat the MPL 2.0 notice:")
                for m in repeated:
                    print("  " + str(m))
            print("Run the workflow 'License headers' with mode 'fix', or: python3 scripts/license_headers.py")
            sys.exit(1)
        print("All files have the notice, once.")
    else:
        print(f"Notice added to {len(missing)} file(s); repeated copies removed from {len(repeated)} file(s).")


if __name__ == "__main__":
    main()
