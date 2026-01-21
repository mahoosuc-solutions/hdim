#!/usr/bin/env python3
"""Verify direct dependency licenses for npm and Maven artifacts.

This script resolves licenses from upstream registries and writes a
summary block into docs/compliance/THIRD_PARTY_NOTICES.md.
"""

from __future__ import annotations

import json
import pathlib
import re
import sys
import time
import xml.etree.ElementTree as ET
from typing import Dict, List, Optional, Tuple

import requests

REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
NOTICES_PATH = REPO_ROOT / "docs" / "compliance" / "THIRD_PARTY_NOTICES.md"
PACKAGE_JSON = REPO_ROOT / "package.json"
PACKAGE_LOCK = REPO_ROOT / "package-lock.json"
GRADLE_VERSIONS = REPO_ROOT / "backend" / "gradle" / "libs.versions.toml"
ALLOWLIST_PATH = REPO_ROOT / "docs" / "compliance" / "LICENSE_ALLOWLIST.txt"

LICENSE_MARKERS = ("<!-- BEGIN: LICENSE_VERIFICATION -->", "<!-- END: LICENSE_VERIFICATION -->")

NPM_REGISTRY = "https://registry.npmjs.org/"
MAVEN_CENTRAL = "https://repo1.maven.org/maven2/"


LICENSE_NORMALIZATION = {
    "apache": "Apache-2.0",
    "mit": "MIT",
    "isc": "ISC",
    "bsd-2-clause": "BSD-2-Clause",
    "bsd 2-clause": "BSD-2-Clause",
    "bsd-3-clause": "BSD-3-Clause",
    "bsd 3-clause": "BSD-3-Clause",
    "0bsd": "0BSD",
    "eclipse public license v2.0": "EPL-2.0",
    "eclipse public license 2.0": "EPL-2.0",
    "eclipse public license - version 2.0": "EPL-2.0",
    "eclipse public license - v 1.0": "EPL-1.0",
    "eclipse public license v 1.0": "EPL-1.0",
    "eclipse public license 1.0": "EPL-1.0",
    "bouncy castle": "Bouncy Castle License",
    "standard 'no charge' license": "GSAP Standard License",
}


class LicenseResult:
    def __init__(self, name: str, version: str, license_name: str, source: str, note: str = ""):
        self.name = name
        self.version = version
        self.license_name = license_name
        self.source = source
        self.note = note

    def format(self) -> str:
        version = f" {self.version}" if self.version else ""
        note = f"; note: {self.note}" if self.note else ""
        return f"- {self.name}{version} (license: {self.license_name}; source: {self.source}{note})"


def load_package_deps(path: pathlib.Path) -> Tuple[Dict[str, str], Dict[str, str]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return data.get("dependencies", {}), data.get("devDependencies", {})


def load_package_lock_versions(path: pathlib.Path) -> Dict[str, str]:
    if not path.exists():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    packages = data.get("packages", {})
    versions: Dict[str, str] = {}
    for key, meta in packages.items():
        if not key.startswith("node_modules/"):
            continue
        name = key[len("node_modules/") :]
        version = meta.get("version")
        if version:
            versions[name] = version
    return versions

def parse_gradle_catalog(path: pathlib.Path) -> Tuple[Dict[str, str], List[Tuple[str, str]]]:
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
            libraries.append((module, version or ""))

    return versions, libraries


def normalize_license(value: Optional[str]) -> str:
    if not value:
        return "unknown"
    cleaned = re.sub(r"\s+", " ", value.strip())
    lowered = cleaned.lower()
    for key, normalized in LICENSE_NORMALIZATION.items():
        if key in lowered:
            return normalized
    return cleaned


def fetch_npm_license(package: str, version: str, locked_version: str) -> LicenseResult:
    url = f"{NPM_REGISTRY}{package}"
    resp = requests.get(url, timeout=20)
    resp.raise_for_status()
    data = resp.json()

    license_name = "unknown"
    source = "npm registry (latest tag)"
    note = ""

    versions = data.get("versions", {})
    if locked_version and locked_version in versions:
        license_name = normalize_license(versions[locked_version].get("license"))
        source = "npm registry (package-lock version)"
    elif version in versions:
        license_name = normalize_license(versions[version].get("license"))
        source = "npm registry (version metadata)"
    else:
        latest = data.get("dist-tags", {}).get("latest")
        if latest and latest in versions:
            license_name = normalize_license(versions[latest].get("license"))
            source = "npm registry (latest tag)"
            note = "resolved from latest tag; confirm for pinned version"

    if not license_name or license_name == "unknown":
        license_name = normalize_license(data.get("license"))
        source = "npm registry (package metadata)"

    return LicenseResult(package, version, license_name, source, note=note)


def resolve_gradle_version(module: str, version: str, versions: Dict[str, str]) -> Tuple[str, str]:
    if version:
        return version, ""
    if module.startswith("software.amazon.awssdk:"):
        resolved = versions.get("aws-sdk", "")
        return resolved, "version from aws-sdk BOM"
    if module.startswith("org.springframework.cloud:"):
        resolved = versions.get("spring-cloud", "")
        return resolved, "release train version; may map to BOM"
    if module.startswith("org.springframework.boot:"):
        resolved = versions.get("spring-boot", "")
        return resolved, "version from spring-boot BOM"
    return "", ""


def fetch_maven_metadata(group: str, artifact: str) -> Optional[str]:
    group_path = group.replace(".", "/")
    metadata_url = f"{MAVEN_CENTRAL}{group_path}/{artifact}/maven-metadata.xml"
    resp = requests.get(metadata_url, timeout=20)
    if resp.status_code != 200:
        return None
    try:
        root = ET.fromstring(resp.text)
        release = root.findtext("versioning/release")
        latest = root.findtext("versioning/latest")
        return release or latest
    except ET.ParseError:
        return None


def extract_license_from_pom(xml_text: str) -> Optional[str]:
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return None
    ns = {"m": root.tag.split("}")[0].strip("{")} if "}" in root.tag else {}

    def find(path: str):
        return root.find(path, ns) if ns else root.find(path)

    licenses = find("m:licenses") if ns else find("licenses")
    if licenses is not None:
        license_el = licenses.find("m:license", ns) if ns else licenses.find("license")
        if license_el is not None:
            name_el = license_el.find("m:name", ns) if ns else license_el.find("name")
            if name_el is not None and name_el.text:
                return normalize_license(name_el.text)

    parent = find("m:parent") if ns else find("parent")
    if parent is not None:
        parent_group = parent.find("m:groupId", ns) if ns else parent.find("groupId")
        parent_artifact = parent.find("m:artifactId", ns) if ns else parent.find("artifactId")
        parent_version = parent.find("m:version", ns) if ns else parent.find("version")
        if parent_group is not None and parent_artifact is not None and parent_version is not None:
            pg = parent_group.text
            pa = parent_artifact.text
            pv = parent_version.text
            if pg and pa and pv:
                parent_xml = fetch_maven_pom(pg, pa, pv)
                if parent_xml:
                    return extract_license_from_pom(parent_xml)

    return None


def fetch_maven_pom(group: str, artifact: str, version: str) -> Optional[str]:
    group_path = group.replace(".", "/")
    pom_url = f"{MAVEN_CENTRAL}{group_path}/{artifact}/{version}/{artifact}-{version}.pom"
    resp = requests.get(pom_url, timeout=20)
    if resp.status_code != 200:
        return None
    return resp.text


def fetch_maven_license(module: str, version: str, note: str = "") -> LicenseResult:
    group, artifact = module.split(":", 1)
    pom = fetch_maven_pom(group, artifact, version)
    if pom is None:
        resolved = fetch_maven_metadata(group, artifact)
        if resolved:
            pom = fetch_maven_pom(group, artifact, resolved)
            if pom:
                license_name = extract_license_from_pom(pom) or "unknown"
                return LicenseResult(module, resolved, license_name, "Maven Central (metadata)" , note=f"version resolved from metadata (requested {version})")
        return LicenseResult(module, version, "unknown", "Maven Central", note=f"pom not found for {version}")

    license_name = extract_license_from_pom(pom) or "unknown"
    source = "Maven Central (pom)"
    return LicenseResult(module, version, license_name, source, note=note)


def replace_block(text: str, begin: str, end: str, new_lines: List[str]) -> str:
    if begin not in text or end not in text:
        raise ValueError(f"Missing markers: {begin} / {end}")
    pre, rest = text.split(begin, 1)
    _, post = rest.split(end, 1)
    content = "\n" + ("\n".join(new_lines) if new_lines else "") + "\n"
    return f"{pre}{begin}{content}{end}{post}"


def load_allowlist(path: pathlib.Path) -> List[str]:
    if not path.exists():
        return []
    entries = []
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        entries.append(line)
    return entries


def is_allowlisted(entry: str, allowlist: List[str]) -> bool:
    for allowed in allowlist:
        if entry.startswith(allowed):
            return True
    return False


def main() -> None:
    deps, dev_deps = load_package_deps(PACKAGE_JSON)
    locked_versions = load_package_lock_versions(PACKAGE_LOCK)
    versions, libraries = parse_gradle_catalog(GRADLE_VERSIONS)
    allowlist = load_allowlist(ALLOWLIST_PATH)

    results: List[str] = []
    results.append("### NPM direct dependencies")
    for name, spec in sorted(deps.items(), key=lambda item: item[0].lower()):
        try:
            locked_version = locked_versions.get(name, "")
            result = fetch_npm_license(name, spec, locked_version)
            if result.license_name == "unknown" and is_allowlisted(result.name, allowlist):
                result.license_name = "unknown (allowlisted)"
            results.append(result.format())
        except Exception as exc:
            results.append(f"- {name} {spec} (license: unknown; source: npm registry error: {exc})")
        time.sleep(0.1)

    results.append("")
    results.append("### NPM dev dependencies")
    for name, spec in sorted(dev_deps.items(), key=lambda item: item[0].lower()):
        try:
            locked_version = locked_versions.get(name, "")
            result = fetch_npm_license(name, spec, locked_version)
            if result.license_name == "unknown" and is_allowlisted(result.name, allowlist):
                result.license_name = "unknown (allowlisted)"
            results.append(result.format())
        except Exception as exc:
            results.append(f"- {name} {spec} (license: unknown; source: npm registry error: {exc})")
        time.sleep(0.1)

    results.append("")
    results.append("### Maven/Gradle libraries")
    for module, version in sorted(libraries, key=lambda item: item[0].lower()):
        resolved_version, note = resolve_gradle_version(module, version, versions)
        if not resolved_version:
            results.append(f"- {module} (license: unknown; source: version unresolved)")
            continue
        try:
            result = fetch_maven_license(module, resolved_version, note=note)
            if result.license_name == "unknown" and is_allowlisted(result.name, allowlist):
                result.license_name = "unknown (allowlisted)"
            results.append(result.format())
        except Exception as exc:
            results.append(f"- {module} {resolved_version} (license: unknown; source: maven error: {exc})")
        time.sleep(0.1)

    notices = NOTICES_PATH.read_text(encoding="utf-8")
    notices = replace_block(notices, LICENSE_MARKERS[0], LICENSE_MARKERS[1], results)
    NOTICES_PATH.write_text(notices, encoding="utf-8")
    print(f"Updated {NOTICES_PATH}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(1)
