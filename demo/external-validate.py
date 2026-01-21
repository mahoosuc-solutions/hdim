#!/usr/bin/env python3
import argparse
import json
import sys
import urllib.error
import urllib.request


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="External validator for HDIM demo data (counts + values)."
    )
    parser.add_argument(
        "--expected",
        default="demo/validation/expected-demo-data.json",
        help="Path to expected demo data JSON",
    )
    parser.add_argument("--tenant-id", default="acme-health")
    parser.add_argument("--fhir-base", default="http://localhost:8085/fhir")
    parser.add_argument("--care-gap-base", default="http://localhost:8086/care-gap")
    parser.add_argument("--auth-user-id", default="550e8400-e29b-41d4-a716-446655440000")
    parser.add_argument("--auth-username", default="demo_user")
    parser.add_argument("--auth-roles", default="ADMIN")
    parser.add_argument("--auth-validated", default="gateway-dev")
    parser.add_argument(
        "--mode",
        choices=("strict", "lenient"),
        default="strict",
        help="strict: exact counts; lenient: allow extras",
    )
    parser.add_argument(
        "--timeout-seconds",
        type=int,
        default=5,
        help="HTTP timeout for validation calls",
    )
    return parser.parse_args()


def load_expected(path: str) -> dict:
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)


def build_headers(args: argparse.Namespace) -> dict:
    return {
        "X-Auth-User-Id": args.auth_user_id,
        "X-Auth-Username": args.auth_username,
        "X-Auth-Tenant-Ids": args.tenant_id,
        "X-Auth-Roles": args.auth_roles,
        "X-Auth-Validated": args.auth_validated,
        "X-Tenant-ID": args.tenant_id,
    }


def request_json_with_status(url: str, headers: dict, timeout: int):
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            payload = response.read().decode("utf-8")
            data = json.loads(payload) if payload else {}
            return response.getcode(), data
    except urllib.error.HTTPError as exc:
        payload = exc.read().decode("utf-8")
        data = json.loads(payload) if payload else {}
        return exc.code, data


def request_status(url: str, headers: dict, timeout: int) -> int:
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            return response.getcode()
    except urllib.error.HTTPError as exc:
        return exc.code


def normalize_measures(measures):
    return sorted({m for m in measures if m})


def check_count(label: str, actual: int, expected: int, mode: str) -> dict:
    if mode == "lenient":
        ok = actual >= expected
        return {
            "ok": ok,
            "actual": actual,
            "expected": expected,
            "delta": actual - expected,
        }
    ok = actual == expected
    return {"ok": ok, "actual": actual, "expected": expected, "delta": actual - expected}


def main() -> int:
    args = parse_args()
    expected = load_expected(args.expected)
    headers = build_headers(args)

    report = {
        "mode": args.mode,
        "tenantId": expected.get("tenantId"),
        "checks": [],
        "failures": 0,
    }

    # Patient total
    fhir_status, fhir_patients = request_json_with_status(
        f"{args.fhir_base}/Patient?_count=200", headers, args.timeout_seconds
    )
    patient_total = int(fhir_patients.get("total", 0)) if fhir_status == 200 else 0
    expected_patients = int(expected["expectedTotals"]["patients"])
    result = check_count("patients.total", patient_total, expected_patients, args.mode)
    report["checks"].append(
        {"name": "patients.total", "status": fhir_status, **result}
    )
    if not result["ok"]:
        report["failures"] += 1

    # Patient existence + per-patient gaps
    care_gap_base = f"{args.care_gap_base}/api/v1/care-gaps?size=50&patientId="
    stats_base = f"{args.care_gap_base}/care-gap/stats?patient="
    for patient in expected["patients"]:
        patient_id = patient["id"]
        status = request_status(
            f"{args.fhir_base}/Patient/{patient_id}", headers, args.timeout_seconds
        )
        ok = status == 200
        report["checks"].append(
            {
                "name": f"patients.exists.{patient_id}",
                "ok": ok,
                "status": status,
            }
        )
        if not ok:
            report["failures"] += 1
            continue

        gap_status, gap_payload = request_json_with_status(
            f"{care_gap_base}{patient_id}", headers, args.timeout_seconds
        )
        gap_measures = normalize_measures(
            [item.get("measureId") for item in gap_payload.get("content", [])]
        )
        expected_measures = normalize_measures(patient.get("expectedGaps", []))
        gap_count = len(gap_payload.get("content", []))
        expected_count = len(expected_measures)

        count_result = check_count(
            f"careGaps.count.{patient_id}", gap_count, expected_count, args.mode
        )
        report["checks"].append(
            {"name": f"careGaps.count.{patient_id}", "status": gap_status, **count_result}
        )
        if not count_result["ok"]:
            report["failures"] += 1

        missing = [m for m in expected_measures if m not in gap_measures]
        extra = [m for m in gap_measures if m not in expected_measures]
        measures_ok = not missing and (args.mode == "lenient" or not extra)
        report["checks"].append(
            {
                "name": f"careGaps.measures.{patient_id}",
                "ok": measures_ok,
                "status": gap_status,
                "expected": expected_measures,
                "actual": gap_measures,
                "missing": missing,
                "extra": extra,
            }
        )
        if not measures_ok:
            report["failures"] += 1

        stats_status, stats_payload = request_json_with_status(
            f"{stats_base}{patient_id}", headers, args.timeout_seconds
        )
        open_gaps = int(stats_payload.get("openGapsCount", 0)) if stats_status == 200 else 0
        open_result = check_count(
            f"careGaps.stats.open.{patient_id}", open_gaps, expected_count, args.mode
        )
        report["checks"].append(
            {
                "name": f"careGaps.stats.open.{patient_id}",
                "status": stats_status,
                **open_result,
            }
        )
        if not open_result["ok"]:
            report["failures"] += 1

    # Care gap totals and priorities
    all_status, all_gaps = request_json_with_status(
        f"{args.care_gap_base}/api/v1/care-gaps?size=200", headers, args.timeout_seconds
    )
    total_gaps = int(all_gaps.get("totalElements", 0)) if all_status == 200 else 0
    expected_gaps = int(expected["expectedTotals"]["careGaps"])
    total_result = check_count("careGaps.total", total_gaps, expected_gaps, args.mode)
    report["checks"].append(
        {"name": "careGaps.total", "status": all_status, **total_result}
    )
    if not total_result["ok"]:
        report["failures"] += 1

    high_status, high_payload = request_json_with_status(
        f"{args.care_gap_base}/api/v1/care-gaps?size=200&priority=HIGH",
        headers,
        args.timeout_seconds,
    )
    medium_status, medium_payload = request_json_with_status(
        f"{args.care_gap_base}/api/v1/care-gaps?size=200&priority=MEDIUM",
        headers,
        args.timeout_seconds,
    )
    high_count = int(high_payload.get("totalElements", 0)) if high_status == 200 else 0
    medium_count = int(medium_payload.get("totalElements", 0)) if medium_status == 200 else 0
    expected_high = int(expected["expectedTotals"]["priority"]["HIGH"])
    expected_medium = int(expected["expectedTotals"]["priority"]["MEDIUM"])

    high_result = check_count("careGaps.priority.high", high_count, expected_high, args.mode)
    medium_result = check_count(
        "careGaps.priority.medium", medium_count, expected_medium, args.mode
    )
    report["checks"].append(
        {"name": "careGaps.priority.high", "status": high_status, **high_result}
    )
    report["checks"].append(
        {"name": "careGaps.priority.medium", "status": medium_status, **medium_result}
    )
    if not high_result["ok"]:
        report["failures"] += 1
    if not medium_result["ok"]:
        report["failures"] += 1

    print(json.dumps(report, indent=2, sort_keys=True))
    return 1 if report["failures"] else 0


if __name__ == "__main__":
    sys.exit(main())
