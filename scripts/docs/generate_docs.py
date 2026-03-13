#!/usr/bin/env python3
from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Iterable


REPO_ROOT = Path(__file__).resolve().parents[2]
BACKEND_ROOT = REPO_ROOT / "backend"
SERVICES_ROOT = BACKEND_ROOT / "modules" / "services"
SHARED_ROOT = BACKEND_ROOT / "modules" / "shared"


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.rstrip() + "\n", encoding="utf-8")


def parse_settings_services() -> list[str]:
    text = read_text(BACKEND_ROOT / "settings.gradle.kts")
    inside = False
    services: list[str] = []
    for line in text.splitlines():
        if "// Microservices" in line:
            inside = True
            continue
        if inside and line.strip() == ")":
            break
        if inside:
            match = re.search(r'"modules:services:([^"]+)"', line)
            if match:
                services.append(match.group(1))
    return services


def parse_settings_shared_modules() -> list[str]:
    text = read_text(BACKEND_ROOT / "settings.gradle.kts")
    results: list[str] = []
    for line in text.splitlines():
        match = re.search(r'"modules:shared:([^"]+)"', line)
        if match:
            results.append(match.group(1))
    return results


def find_prop_block(text: str, start_key: str, next_keys: Iterable[str]) -> str:
    lines = text.splitlines()
    start = None
    indent = None
    for idx, line in enumerate(lines):
        if re.match(rf"^\s*{re.escape(start_key)}:\s*$", line):
            start = idx + 1
            indent = len(line) - len(line.lstrip()) + 2
            break
    if start is None:
        return ""
    collected = []
    for line in lines[start:]:
        if not line.strip():
            collected.append(line)
            continue
        current_indent = len(line) - len(line.lstrip())
        if current_indent < indent and any(re.match(rf"^\s*{re.escape(k)}:", line) for k in next_keys):
            break
        if current_indent < indent and re.match(r"^[A-Za-z0-9_-]+:", line):
            break
        collected.append(line)
    return "\n".join(collected)


def simple_yaml_value(block: str, key: str) -> str | None:
    match = re.search(rf"(?m)^\s*{re.escape(key)}:\s*(.+?)\s*$", block)
    return match.group(1).strip() if match else None


@dataclass
class ServiceInfo:
    name: str
    managed: bool
    path: Path
    build_path: Path | None
    readme_path: Path
    app_yml_path: Path | None
    dockerfile_path: Path | None
    python_service: bool
    deployable: bool
    support_module: bool
    app_name: str
    port: str
    context_path: str
    domain: str
    summary: str
    service_deps: list[str]
    shared_deps: list[str]
    uses_feign: bool
    uses_kafka: bool


@dataclass
class SharedModuleInfo:
    name: str
    group: str
    path: Path
    readme_path: Path
    summary: str


def service_domain(name: str) -> str:
    if name in {
        "patient-service",
        "fhir-service",
        "cql-engine-service",
        "quality-measure-service",
        "care-gap-service",
        "consent-service",
        "clinical-workflow-service",
        "nurse-workflow-service",
        "hcc-service",
        "prior-auth-service",
        "sdoh-service",
        "ecr-service",
        "qrda-export-service",
    }:
        return "Clinical and care management"
    if name.endswith("-event-service") or name.endswith("-event-handler-service") or name in {
        "event-store-service",
        "event-replay-service",
        "fhir-event-bridge-service",
    }:
        return "Event-sourced services and handlers"
    if name in {
        "event-processing-service",
        "event-router-service",
        "ehr-connector-service",
        "cms-connector-service",
        "cdr-processor-service",
        "data-enrichment-service",
        "data-ingestion-service",
        "documentation-service",
        "corehive-adapter-service",
        "healthix-adapter-service",
        "hedis-adapter-service",
        "ihe-gateway-service",
    }:
        return "Integration, ingestion, and adapters"
    if name in {
        "analytics-service",
        "predictive-analytics-service",
        "cost-analysis-service",
        "payer-workflows-service",
        "migration-workflow-service",
        "audit-query-service",
        "query-api-service",
        "cqrs-query-service",
    }:
        return "Analytics, payer, and query services"
    if name in {
        "gateway-admin-service",
        "gateway-clinical-service",
        "gateway-fhir-service",
        "admin-service",
        "approval-service",
        "notification-service",
        "demo-orchestrator-service",
        "demo-seeding-service",
    }:
        return "Platform, admin, and notifications"
    return "AI, agents, and business-facing services"


SERVICE_SUMMARIES = {
    "care-gap-event-service": "Event-sourced care-gap service with projections, snapshots, and Stars aggregation.",
    "patient-event-service": "Event-sourced patient domain service and projection API.",
    "quality-measure-event-service": "Event-sourced quality-measure service and projection API.",
    "clinical-workflow-event-service": "Event-sourced clinical workflow service and projection API.",
    "care-gap-event-handler-service": "Care-gap event handler library for projection and side-effect logic.",
    "patient-event-handler-service": "Patient event handler library for projection and side-effect logic.",
    "quality-measure-event-handler-service": "Quality-measure event handler library for projection and side-effect logic.",
    "clinical-workflow-event-handler-service": "Clinical-workflow event handler library for projection and side-effect logic.",
    "gateway-admin-service": "Administrative gateway built on gateway-core.",
    "gateway-clinical-service": "Clinical API gateway built on gateway-core.",
    "gateway-fhir-service": "FHIR API gateway built on gateway-core.",
    "event-store-service": "Immutable event store for event-sourced domains.",
    "event-replay-service": "Replay and projection rebuild support for event-sourced domains.",
    "fhir-event-bridge-service": "Bridge between FHIR-side changes and the event pipeline.",
    "star-ratings": "Reusable Medicare Advantage Stars calculation models and cut-point logic.",
}


def default_summary(name: str, domain: str, support_module: bool, python_service: bool) -> str:
    if support_module:
        return f"{name} is an internal support module in the {domain.lower()} domain."
    if python_service:
        return f"{name} is a Python-based service in the {domain.lower()} domain."
    return f"{name} is a service in the {domain.lower()} domain."


def parse_build_deps(build_text: str) -> tuple[list[str], list[str], bool, bool]:
    deps = re.findall(r'project\(":([^"]+)"\)', build_text)
    service_deps = [d.split(":")[-1] for d in deps if d.startswith("modules:services:")]
    shared_deps = [d.replace("modules:shared:", "shared:") for d in deps if d.startswith("modules:shared:")]
    uses_feign = "openfeign" in build_text.lower()
    uses_kafka = "kafka" in build_text.lower()
    return sorted(service_deps), sorted(shared_deps), uses_feign, uses_kafka


def collect_services() -> list[ServiceInfo]:
    managed_services = set(parse_settings_services())
    services: list[ServiceInfo] = []
    for path in sorted(p for p in SERVICES_ROOT.iterdir() if p.is_dir() and p.name not in {".settings", "build"}):
        build_path = path / "build.gradle.kts"
        app_yml_path = path / "src" / "main" / "resources" / "application.yml"
        readme_path = path / "README.md"
        dockerfile_path = path / "Dockerfile"
        pyproject_path = path / "pyproject.toml"
        package_json_path = path / "package.json"
        python_service = pyproject_path.exists() or (path / "src" / "main.py").exists()
        build_text = read_text(build_path)
        app_text = read_text(app_yml_path)
        server_block = find_prop_block(app_text, "server", {"spring", "healthdata", "management", "logging", "event-store", "audit", "jwt", "gateway", "backend", "stars"})
        spring_block = find_prop_block(app_text, "spring", {"server", "healthdata", "management", "logging", "event-store", "audit", "jwt", "gateway", "backend", "stars"})
        port = simple_yaml_value(server_block, "port") or "n/a"
        context_path = "n/a"
        servlet_match = re.search(r"(?ms)^\s*servlet:\s*$([\s\S]+?)(?:^\S|\Z)", server_block)
        if servlet_match:
            context_path = simple_yaml_value(servlet_match.group(1), "context-path") or "n/a"
        app_name = simple_yaml_value(spring_block, "name") or path.name
        service_deps, shared_deps, uses_feign, uses_kafka = parse_build_deps(build_text)
        support_module = path.name.endswith("-event-handler-service")
        deployable = (
            python_service
            or ('spring.boot' in build_text.lower())
            or dockerfile_path.exists()
        ) and not support_module
        domain = service_domain(path.name)
        summary = SERVICE_SUMMARIES.get(path.name) or default_summary(path.name, domain, support_module, python_service)
        services.append(
            ServiceInfo(
                name=path.name,
                managed=path.name in managed_services,
                path=path,
                build_path=build_path if build_path.exists() else None,
                readme_path=readme_path,
                app_yml_path=app_yml_path if app_yml_path.exists() else None,
                dockerfile_path=dockerfile_path if dockerfile_path.exists() else None,
                python_service=python_service,
                deployable=deployable,
                support_module=support_module,
                app_name=app_name,
                port=port,
                context_path=context_path,
                domain=domain,
                summary=summary,
                service_deps=service_deps,
                shared_deps=shared_deps,
                uses_feign=uses_feign,
                uses_kafka=uses_kafka,
            )
        )
    return services


def collect_shared_modules() -> list[SharedModuleInfo]:
    modules: list[SharedModuleInfo] = []
    for rel in sorted(parse_settings_shared_modules()):
        path = SHARED_ROOT / rel.replace(":", "/")
        name = f"shared:{rel}"
        group = rel.split(":")[0]
        summary = SERVICE_SUMMARIES.get(path.name) or f"{name} is a shared {group} module used across backend services."
        modules.append(
            SharedModuleInfo(
                name=name,
                group=group,
                path=path,
                readme_path=path / "README.md",
                summary=summary,
            )
        )
    return modules


def generated_banner(source: str) -> str:
    return (
        "<!-- Generated by scripts/docs/generate_docs.py. "
        f"Source: {source}. Do not hand-edit; regenerate instead. -->"
    )


def render_service_catalog(services: list[ServiceInfo], shared_modules: list[SharedModuleInfo]) -> str:
    today = date.today().isoformat()
    lines = [
        generated_banner("backend/settings.gradle.kts, service application.yml, service build.gradle.kts"),
        "# HDIM Service Catalog",
        "",
        f"**Validated Against Code**: {today}",
        "",
        "## Summary",
        "",
        f"- `{len([s for s in services if s.managed])}` Gradle-managed backend service modules",
        f"- `{len(services)}` service directories under `backend/modules/services`",
        f"- `{len([s for s in services if s.readme_path.exists()])}` services with a local `README.md`",
        f"- `{len([s for s in services if not s.readme_path.exists()])}` services currently missing a local `README.md`",
        "",
    ]
    for domain in sorted({s.domain for s in services}):
        lines += [f"## {domain}", "", "| Service | Managed | README | Runtime | Notes |", "|---|---|---|---|---|"]
        for svc in [s for s in services if s.domain == domain]:
            runtime = "Support module" if svc.support_module else ("Deployable" if svc.deployable else "Module")
            lines.append(
                f"| `{svc.name}` | {'Yes' if svc.managed else 'No'} | {'Yes' if svc.readme_path.exists() else 'No'} | {runtime} | {svc.summary} |"
            )
        lines.append("")
    missing = [s.name for s in services if not s.readme_path.exists()]
    lines += ["## Missing Service README Backlog", ""]
    if missing:
        lines += [f"- `{name}`" for name in missing]
    else:
        lines.append("None.")
    lines += ["", "## Shared Modules", ""]
    lines += ["| Module | README | Notes |", "|---|---|---|"]
    for mod in shared_modules:
        lines.append(f"| `{mod.name}` | {'Yes' if mod.readme_path.exists() else 'No'} | {mod.summary} |")
    return "\n".join(lines)


def render_port_reference(services: list[ServiceInfo]) -> str:
    today = date.today().isoformat()
    lines = [
        generated_banner("service application.yml"),
        "# HDIM Port Reference",
        "",
        f"**Validated Against Code**: {today}",
        "",
        "| Service | Port | Context Path | Notes |",
        "|---|---|---|---|",
    ]
    for svc in services:
        note = "Support module" if svc.support_module else ("Deployable" if svc.deployable else "Module")
        lines.append(f"| `{svc.name}` | `{svc.port}` | `{svc.context_path}` | {note} |")
    lines += [
        "",
        "## Operational Notes",
        "",
        "- `n/a` means the value is not explicitly declared in the checked-in `application.yml`.",
        "- Deployment-facing ports can differ from in-app Spring `server.port` values.",
        "- Duplicate defaults should be managed in deployment manifests and validated in CI.",
    ]
    return "\n".join(lines)


def render_dependency_map(services: list[ServiceInfo]) -> str:
    today = date.today().isoformat()
    direct_edges = [(s.name, dep) for s in services for dep in s.service_deps]
    shared_counts: dict[str, int] = {}
    for svc in services:
        for dep in svc.shared_deps:
            shared_counts[dep] = shared_counts.get(dep, 0) + 1
    lines = [
        generated_banner("service build.gradle.kts"),
        "# HDIM Dependency Map",
        "",
        f"**Validated Against Code**: {today}",
        "",
        "## Direct Build-Time Service Dependencies",
        "",
        "| From | To |",
        "|---|---|",
    ]
    for src, dst in sorted(direct_edges):
        lines.append(f"| `{src}` | `{dst}` |")
    lines += ["", "## Shared Module Reuse", "", "| Shared Module | Referenced By |", "|---|---:|"]
    for dep, count in sorted(shared_counts.items()):
        lines.append(f"| `{dep}` | {count} |")
    lines += ["", "## Runtime Integration Signals", "", "| Service | Feign | Kafka |", "|---|---|---|"]
    for svc in services:
        lines.append(f"| `{svc.name}` | {'Yes' if svc.uses_feign else 'No'} | {'Yes' if svc.uses_kafka else 'No'} |")
    return "\n".join(lines)


def render_shared_catalog(shared_modules: list[SharedModuleInfo]) -> str:
    today = date.today().isoformat()
    lines = [
        generated_banner("backend/settings.gradle.kts"),
        "# HDIM Shared Module Catalog",
        "",
        f"**Validated Against Code**: {today}",
        "",
        "| Module | Group | README | Notes |",
        "|---|---|---|---|",
    ]
    for mod in shared_modules:
        lines.append(f"| `{mod.name}` | `{mod.group}` | {'Yes' if mod.readme_path.exists() else 'No'} | {mod.summary} |")
    return "\n".join(lines)


def render_service_readme(svc: ServiceInfo) -> str:
    today = date.today().isoformat()
    shared = ", ".join(f"`{d}`" for d in svc.shared_deps[:8]) or "None detected"
    service_deps = ", ".join(f"`{d}`" for d in svc.service_deps) or "None detected at build time"
    runtime = (
        "internal support library"
        if svc.support_module
        else "deployable Python service" if svc.python_service else "deployable Spring/Java service" if svc.deployable else "module"
    )
    return "\n".join(
        [
            generated_banner("service build.gradle.kts and application.yml"),
            f"# {svc.name}",
            "",
            f"**Status**: Auto-generated baseline README",
            f"**Last Validated**: {today}",
            f"**Runtime Model**: {runtime}",
            "",
            "## Overview",
            "",
            svc.summary,
            "",
            "## Current Metadata",
            "",
            f"- Managed in `backend/settings.gradle.kts`: {'Yes' if svc.managed else 'No'}",
            f"- Spring application name: `{svc.app_name}`",
            f"- Default app port: `{svc.port}`",
            f"- Servlet context path: `{svc.context_path}`",
            f"- Dockerfile present: {'Yes' if svc.dockerfile_path else 'No'}",
            "",
            "## Architecture Fit",
            "",
            f"- Domain grouping: {svc.domain}",
            f"- Build-time service dependencies: {service_deps}",
            f"- Shared module dependencies: {shared}",
            f"- Uses OpenFeign: {'Yes' if svc.uses_feign else 'No'}",
            f"- Uses Kafka dependencies: {'Yes' if svc.uses_kafka else 'No'}",
            "",
            "## Documentation Notes",
            "",
            "- This is a generated baseline README for coverage and navigation.",
            "- Expand it with API, schema, and operational detail as the service evolves.",
            "- Update generated metadata through code/config changes, then regenerate docs.",
        ]
    )


def render_shared_readme(mod: SharedModuleInfo) -> str:
    today = date.today().isoformat()
    return "\n".join(
        [
            generated_banner("backend/settings.gradle.kts"),
            f"# {mod.name}",
            "",
            f"**Status**: Auto-generated baseline README",
            f"**Last Validated**: {today}",
            "",
            "## Overview",
            "",
            mod.summary,
            "",
            "## Documentation Notes",
            "",
            "- This is a generated baseline README for shared-module coverage.",
            "- Expand it with package-level APIs, extension points, and usage constraints as needed.",
            "- For platform-wide context, see `docs/architecture/SHARED_MODULE_CATALOG.md`.",
        ]
    )


def main() -> None:
    services = collect_services()
    shared_modules = collect_shared_modules()

    for svc in services:
        if not svc.readme_path.exists():
            write_text(svc.readme_path, render_service_readme(svc))

    for mod in shared_modules:
        if not mod.readme_path.exists():
            write_text(mod.readme_path, render_shared_readme(mod))

    services = collect_services()
    shared_modules = collect_shared_modules()
    write_text(REPO_ROOT / "docs" / "services" / "SERVICE_CATALOG.md", render_service_catalog(services, shared_modules))
    write_text(REPO_ROOT / "docs" / "services" / "PORT_REFERENCE.md", render_port_reference(services))
    write_text(REPO_ROOT / "docs" / "services" / "DEPENDENCY_MAP.md", render_dependency_map(services))
    write_text(REPO_ROOT / "docs" / "architecture" / "SHARED_MODULE_CATALOG.md", render_shared_catalog(shared_modules))


if __name__ == "__main__":
    main()
