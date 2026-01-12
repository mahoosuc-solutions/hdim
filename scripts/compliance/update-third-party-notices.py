#!/usr/bin/env python3
"""Update third-party notices dependency inventory.

Reads package.json and backend/gradle/libs.versions.toml to refresh
sections in docs/compliance/THIRD_PARTY_NOTICES.md.
"""

from __future__ import annotations

import json
import pathlib
import re
from typing import Dict, List, Tuple

REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
NOTICES_PATH = REPO_ROOT / "docs" / "compliance" / "THIRD_PARTY_NOTICES.md"
PACKAGE_JSON = REPO_ROOT / "package.json"
GRADLE_VERSIONS = REPO_ROOT / "backend" / "gradle" / "libs.versions.toml"

MARKERS = {
    "FRONTEND_DEPENDENCIES": ("<!-- BEGIN: FRONTEND_DEPENDENCIES -->", "<!-- END: FRONTEND_DEPENDENCIES -->"),
    "FRONTEND_DEV_DEPENDENCIES": (
        "<!-- BEGIN: FRONTEND_DEV_DEPENDENCIES -->",
        "<!-- END: FRONTEND_DEV_DEPENDENCIES -->",
    ),
    "BACKEND_LIBRARIES": ("<!-- BEGIN: BACKEND_LIBRARIES -->", "<!-- END: BACKEND_LIBRARIES -->"),
}


def load_package_deps(path: pathlib.Path) -> Tuple[Dict[str, str], Dict[str, str]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return data.get("dependencies", {}), data.get("devDependencies", {})


def parse_gradle_catalog(path: pathlib.Path) -> List[str]:
    versions: Dict[str, str] = {}
    libraries: List[Tuple[str, str]] = []
    section = None

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("[") and line.endswith("]"):
            section = line.strip("[]")
            continue
        if section == "versions":
            match = re.match(r"([A-Za-z0-9_.-]+)\s*=\s*\"([^\"]+)\"", line)
            if match:
                versions[match.group(1)] = match.group(2)
            continue
        if section == "libraries":
            module_match = re.search(r"module\s*=\s*\"([^\"]+)\"", line)
            if not module_match:
                continue
            module = module_match.group(1)
            version_ref_match = re.search(r"version\.ref\s*=\s*\"([^\"]+)\"", line)
            version_match = re.search(r"version\s*=\s*\"([^\"]+)\"", line)
            version = None
            if version_ref_match:
                version = versions.get(version_ref_match.group(1))
            elif version_match:
                version = version_match.group(1)
            if version:
                libraries.append((module, version))
            else:
                libraries.append((module, ""))
    formatted = []
    for module, version in sorted(libraries, key=lambda item: item[0].lower()):
        if version:
            formatted.append(f"- {module} {version}")
        else:
            formatted.append(f"- {module}")
    return formatted


def format_package_list(items: Dict[str, str]) -> List[str]:
    lines = []
    for name in sorted(items.keys(), key=str.lower):
        lines.append(f"- {name} {items[name]}")
    return lines


def replace_block(text: str, begin: str, end: str, new_lines: List[str]) -> str:
    if begin not in text or end not in text:
        raise ValueError(f"Missing markers: {begin} / {end}")
    pre, rest = text.split(begin, 1)
    _, post = rest.split(end, 1)
    content = "\n" + ("\n".join(new_lines) if new_lines else "") + "\n"
    return f"{pre}{begin}{content}{end}{post}"


def main() -> None:
    dependencies, dev_dependencies = load_package_deps(PACKAGE_JSON)
    frontend = format_package_list(dependencies)
    frontend_dev = format_package_list(dev_dependencies)
    backend = parse_gradle_catalog(GRADLE_VERSIONS)

    notices = NOTICES_PATH.read_text(encoding="utf-8")
    notices = replace_block(notices, *MARKERS["FRONTEND_DEPENDENCIES"], frontend)
    notices = replace_block(notices, *MARKERS["FRONTEND_DEV_DEPENDENCIES"], frontend_dev)
    notices = replace_block(notices, *MARKERS["BACKEND_LIBRARIES"], backend)

    NOTICES_PATH.write_text(notices, encoding="utf-8")
    print(f"Updated {NOTICES_PATH}")


if __name__ == "__main__":
    main()
