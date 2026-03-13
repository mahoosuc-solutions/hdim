#!/usr/bin/env python3
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
GENERATOR = REPO_ROOT / "scripts" / "docs" / "generate_docs.py"
PRIMARY_DOCS = [
    REPO_ROOT / "docs" / "README.md",
    REPO_ROOT / "docs" / "architecture" / "ARCHITECTURE.md",
    REPO_ROOT / "docs" / "architecture" / "SYSTEM_ARCHITECTURE.md",
    REPO_ROOT / "docs" / "architecture" / "GATEWAY_ARCHITECTURE.md",
    REPO_ROOT / "docs" / "architecture" / "EVENT_SOURCING_ARCHITECTURE.md",
    REPO_ROOT / "docs" / "architecture" / "SHARED_MODULE_CATALOG.md",
    REPO_ROOT / "docs" / "services" / "SERVICE_CATALOG.md",
    REPO_ROOT / "docs" / "services" / "PORT_REFERENCE.md",
    REPO_ROOT / "docs" / "services" / "DEPENDENCY_MAP.md",
    REPO_ROOT / "backend" / "README.md",
]


def fail(message: str) -> None:
    print(f"[docs:validate] ERROR: {message}")
    sys.exit(1)


def check_file_exists() -> None:
    for path in PRIMARY_DOCS:
        if not path.exists():
            fail(f"Missing required documentation file: {path.relative_to(REPO_ROOT)}")


def check_generated_files_clean() -> None:
    tracked = [
        REPO_ROOT / "docs" / "services" / "SERVICE_CATALOG.md",
        REPO_ROOT / "docs" / "services" / "PORT_REFERENCE.md",
        REPO_ROOT / "docs" / "services" / "DEPENDENCY_MAP.md",
        REPO_ROOT / "docs" / "architecture" / "SHARED_MODULE_CATALOG.md",
    ]
    before = {path: path.read_text(encoding="utf-8") if path.exists() else "" for path in tracked}
    subprocess.run([sys.executable, str(GENERATOR)], cwd=REPO_ROOT, check=True)
    changed = [path.relative_to(REPO_ROOT).as_posix() for path in tracked if path.read_text(encoding="utf-8") != before[path]]
    if changed:
        fail("Generated documentation changed during validation; run docs:generate and commit the results:\n" + "\n".join(changed))


def check_missing_readmes() -> None:
    for path in sorted((REPO_ROOT / "backend" / "modules" / "services").iterdir()):
        if path.is_dir() and path.name not in {".settings", "build"}:
            if not (path / "README.md").exists():
                fail(f"Missing service README: {path.relative_to(REPO_ROOT)}")
    for path in sorted((REPO_ROOT / "backend" / "modules" / "shared").rglob("build.gradle.kts")):
        if not (path.parent / "README.md").exists():
            fail(f"Missing shared module README: {path.parent.relative_to(REPO_ROOT)}")


def check_internal_links() -> None:
    link_pattern = re.compile(r"\[[^\]]+\]\(([^)]+)\)")
    for doc in PRIMARY_DOCS:
        text = doc.read_text(encoding="utf-8")
        for target in link_pattern.findall(text):
            if target.startswith(("http://", "https://", "mailto:", "#")):
                continue
            resolved = (doc.parent / target).resolve()
            if not resolved.exists():
                fail(f"Broken link in {doc.relative_to(REPO_ROOT)} -> {target}")


def main() -> None:
    check_file_exists()
    check_generated_files_clean()
    check_missing_readmes()
    check_internal_links()
    print("[docs:validate] PASS")


if __name__ == "__main__":
    main()
