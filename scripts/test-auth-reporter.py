#!/usr/bin/env python3

"""
HDIM Clinical Portal - Authentication Test Reporter
Generates detailed test reports and tracks test history

Usage:
    python3 test-auth-reporter.py --shell-output results.txt
    python3 test-auth-reporter.py --json-output results.json
    python3 test-auth-reporter.py --html-output results.html
    python3 test-auth-reporter.py --all-outputs
"""

import json
import sys
import subprocess
import argparse
import os
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Tuple


class AuthTestReporter:
    """Generate authentication test reports in multiple formats"""

    def __init__(self):
        self.timestamp = datetime.utcnow().isoformat() + "Z"
        self.project_root = Path(__file__).parent.parent
        self.results: Dict = {
            "timestamp": self.timestamp,
            "summary": {"passed": 0, "failed": 0, "skipped": 0},
            "tests": {},
            "status": "UNKNOWN"
        }

    def run_tests(self, verbose: bool = False) -> bool:
        """Execute the authentication test suite"""
        script_path = self.project_root / "scripts" / "test-authentication-flow.sh"

        if not script_path.exists():
            print(f"Error: Test script not found at {script_path}")
            return False

        print(f"Running authentication tests from {script_path}...")
        print()

        try:
            env = os.environ.copy()
            if verbose:
                env["VERBOSE"] = "1"

            result = subprocess.run(
                [str(script_path)],
                env=env,
                capture_output=False,
                timeout=300  # 5 minute timeout
            )

            return result.returncode == 0

        except subprocess.TimeoutExpired:
            print("Error: Tests timed out after 5 minutes")
            return False
        except Exception as e:
            print(f"Error running tests: {e}")
            return False

    def parse_test_output(self, output: str) -> Dict:
        """Parse test output and extract results"""
        # This is a simplified parser - in production, use the JSON output
        results = {
            "passed": 0,
            "failed": 0,
            "skipped": 0,
            "tests": {}
        }

        for line in output.split('\n'):
            if "✓" in line and "PASS" in line:
                results["passed"] += 1
            elif "✗" in line and "FAIL" in line:
                results["failed"] += 1
            elif "⊘" in line and "SKIP" in line:
                results["skipped"] += 1

        return results

    def generate_json_report(self, output_file: str) -> bool:
        """Generate JSON format report"""
        try:
            # Run shell script with json output
            script_path = self.project_root / "scripts" / "test-authentication-flow.sh"
            temp_json = "/tmp/test_report_temp.json"

            subprocess.run(
                [str(script_path), temp_json],
                timeout=300,
                capture_output=True
            )

            if Path(temp_json).exists():
                with open(temp_json, 'r') as f:
                    data = json.load(f)

                # Add metadata
                data["metadata"] = {
                    "generator": "test-auth-reporter.py",
                    "version": "1.0",
                    "generated_at": self.timestamp
                }

                with open(output_file, 'w') as f:
                    json.dump(data, f, indent=2)

                print(f"✓ JSON report saved to: {output_file}")
                return True
            else:
                print(f"✗ Failed to generate JSON report")
                return False

        except Exception as e:
            print(f"✗ Error generating JSON report: {e}")
            return False

    def generate_html_report(self, output_file: str) -> bool:
        """Generate HTML format report"""
        try:
            script_path = self.project_root / "scripts" / "test-authentication-flow.sh"
            temp_json = "/tmp/test_report_temp.json"

            subprocess.run(
                [str(script_path), temp_json],
                timeout=300,
                capture_output=True
            )

            if not Path(temp_json).exists():
                print("✗ Failed to generate HTML report: JSON data missing")
                return False

            with open(temp_json, 'r') as f:
                data = json.load(f)

            status_class = "success" if data["status"] == "PASSED" else "failure"
            status_icon = "✅" if data["status"] == "PASSED" else "❌"

            html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HDIM Authentication Test Report</title>
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}

        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }}

        .container {{
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 8px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            overflow: hidden;
        }}

        .header {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }}

        .header h1 {{
            font-size: 2.5em;
            margin-bottom: 10px;
        }}

        .header p {{
            font-size: 1.1em;
            opacity: 0.9;
        }}

        .content {{
            padding: 40px;
        }}

        .summary {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 40px;
        }}

        .summary-card {{
            background: #f8f9fa;
            border-left: 4px solid #667eea;
            padding: 20px;
            border-radius: 4px;
        }}

        .summary-card.success {{
            border-left-color: #10b981;
        }}

        .summary-card.failure {{
            border-left-color: #ef4444;
        }}

        .summary-card.warning {{
            border-left-color: #f59e0b;
        }}

        .summary-card h3 {{
            font-size: 0.9em;
            color: #6b7280;
            margin-bottom: 10px;
            text-transform: uppercase;
        }}

        .summary-card .number {{
            font-size: 2.5em;
            font-weight: bold;
            color: #1f2937;
        }}

        .status {{
            text-align: center;
            margin: 30px 0;
            padding: 20px;
            border-radius: 8px;
            font-size: 1.5em;
            font-weight: bold;
        }}

        .status.success {{
            background: #ecfdf5;
            color: #065f46;
            border: 2px solid #10b981;
        }}

        .status.failure {{
            background: #fef2f2;
            color: #7f1d1d;
            border: 2px solid #ef4444;
        }}

        .tests-section h2 {{
            font-size: 1.5em;
            margin: 30px 0 20px 0;
            color: #1f2937;
            border-bottom: 2px solid #e5e7eb;
            padding-bottom: 10px;
        }}

        .test-item {{
            display: flex;
            align-items: flex-start;
            padding: 15px;
            margin: 10px 0;
            background: #f9fafb;
            border-radius: 4px;
            border-left: 4px solid #d1d5db;
        }}

        .test-item.pass {{
            border-left-color: #10b981;
            background: #f0fdf4;
        }}

        .test-item.fail {{
            border-left-color: #ef4444;
            background: #fef2f2;
        }}

        .test-item.skip {{
            border-left-color: #f59e0b;
            background: #fffbeb;
        }}

        .test-icon {{
            font-size: 1.5em;
            margin-right: 15px;
            min-width: 24px;
        }}

        .test-content {{
            flex: 1;
        }}

        .test-name {{
            font-weight: 600;
            color: #1f2937;
            margin-bottom: 5px;
        }}

        .test-message {{
            font-size: 0.9em;
            color: #6b7280;
        }}

        .test-result {{
            font-size: 0.85em;
            font-weight: 600;
            padding: 4px 12px;
            border-radius: 20px;
            white-space: nowrap;
        }}

        .test-result.pass {{
            background: #10b981;
            color: white;
        }}

        .test-result.fail {{
            background: #ef4444;
            color: white;
        }}

        .test-result.skip {{
            background: #f59e0b;
            color: white;
        }}

        .footer {{
            background: #f3f4f6;
            padding: 20px;
            text-align: center;
            color: #6b7280;
            font-size: 0.9em;
            border-top: 1px solid #e5e7eb;
        }}

        .metadata {{
            font-size: 0.85em;
            color: #9ca3af;
            margin-top: 10px;
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>HDIM Clinical Portal</h1>
            <p>Authentication Test Report</p>
        </div>

        <div class="content">
            <div class="summary">
                <div class="summary-card success">
                    <h3>Tests Passed</h3>
                    <div class="number">{data["summary"]["passed"]}</div>
                </div>
                <div class="summary-card failure">
                    <h3>Tests Failed</h3>
                    <div class="number">{data["summary"]["failed"]}</div>
                </div>
                <div class="summary-card warning">
                    <h3>Tests Skipped</h3>
                    <div class="number">{data["summary"]["skipped"]}</div>
                </div>
                <div class="summary-card">
                    <h3>Total Tests</h3>
                    <div class="number">{data["summary"]["passed"] + data["summary"]["failed"] + data["summary"]["skipped"]}</div>
                </div>
            </div>

            <div class="status {status_class}">
                {status_icon} {data["status"]}
            </div>

            <div class="tests-section">
                <h2>Test Results</h2>
                {''.join(self._generate_test_items(data))}
            </div>
        </div>

        <div class="footer">
            <p><strong>Generated:</strong> {self.timestamp}</p>
            <p class="metadata">HDIM Authentication Test Suite v1.0</p>
        </div>
    </div>
</body>
</html>
"""

            with open(output_file, 'w') as f:
                f.write(html_content)

            print(f"✓ HTML report saved to: {output_file}")
            return True

        except Exception as e:
            print(f"✗ Error generating HTML report: {e}")
            return False

    @staticmethod
    def _generate_test_items(data: Dict) -> str:
        """Generate HTML test item elements"""
        items = []

        for test_name, test_data in data.get("tests", {}).items():
            result = test_data.get("result", "UNKNOWN").lower()
            message = test_data.get("message", "")

            icon_map = {
                "pass": "✓",
                "fail": "✗",
                "skip": "⊘"
            }
            icon = icon_map.get(result, "?")

            items.append(f"""
                <div class="test-item {result}">
                    <div class="test-icon">{icon}</div>
                    <div class="test-content">
                        <div class="test-name">{test_name}</div>
                        <div class="test-message">{message}</div>
                    </div>
                    <div class="test-result {result}">{result.upper()}</div>
                </div>
            """)

        return ''.join(items)

    def generate_markdown_report(self, output_file: str) -> bool:
        """Generate Markdown format report"""
        try:
            script_path = self.project_root / "scripts" / "test-authentication-flow.sh"
            temp_json = "/tmp/test_report_temp.json"

            subprocess.run(
                [str(script_path), temp_json],
                timeout=300,
                capture_output=True
            )

            if not Path(temp_json).exists():
                print("✗ Failed to generate Markdown report: JSON data missing")
                return False

            with open(temp_json, 'r') as f:
                data = json.load(f)

            status_emoji = "✅" if data["status"] == "PASSED" else "❌"

            md_content = f"""# HDIM Clinical Portal - Authentication Test Report

**Generated:** {self.timestamp}

## Summary

{status_emoji} **Status: {data["status"]}**

| Metric | Value |
|--------|-------|
| Tests Passed | {data["summary"]["passed"]} |
| Tests Failed | {data["summary"]["failed"]} |
| Tests Skipped | {data["summary"]["skipped"]} |
| **Total Tests** | **{data["summary"]["passed"] + data["summary"]["failed"] + data["summary"]["skipped"]}** |

## Test Results

"""
            for test_name, test_data in data.get("tests", {}).items():
                result = test_data.get("result", "UNKNOWN")
                message = test_data.get("message", "")

                icon_map = {
                    "PASS": "✅",
                    "FAIL": "❌",
                    "SKIP": "⚠️"
                }
                icon = icon_map.get(result, "❓")

                md_content += f"- {icon} **{test_name}** - {result}\n"
                if message:
                    md_content += f"  - {message}\n"

            md_content += f"""
## Next Steps

"""
            if data["status"] == "PASSED":
                md_content += """
- ✅ All authentication tests passed
- Access Clinical Portal: http://localhost:4200
- Login with demo credentials: demo_admin@hdim.ai / demo123
- Verify patient data loads without 401 errors
- Check DevTools for proper cookie Path=/

"""
            else:
                md_content += """
- ❌ Some tests failed - review failures above
- Check Docker services: `docker compose -f docker-compose.demo.yml ps`
- Review gateway logs: `docker logs hdim-demo-gateway`
- Verify nginx cookie configuration in `apps/clinical-portal/nginx.conf`

"""

            with open(output_file, 'w') as f:
                f.write(md_content)

            print(f"✓ Markdown report saved to: {output_file}")
            return True

        except Exception as e:
            print(f"✗ Error generating Markdown report: {e}")
            return False


def main():
    parser = argparse.ArgumentParser(
        description="HDIM Authentication Test Reporter"
    )
    parser.add_argument(
        "--verbose", "-v",
        action="store_true",
        help="Enable verbose output"
    )
    parser.add_argument(
        "--shell-output",
        help="Run tests and save shell output"
    )
    parser.add_argument(
        "--json-output",
        help="Generate JSON format report"
    )
    parser.add_argument(
        "--html-output",
        help="Generate HTML format report"
    )
    parser.add_argument(
        "--markdown-output",
        help="Generate Markdown format report"
    )
    parser.add_argument(
        "--all-outputs",
        action="store_true",
        help="Generate all report formats"
    )

    args = parser.parse_args()

    reporter = AuthTestReporter()

    # Run tests
    print("=" * 70)
    print("HDIM Authentication Test Reporter")
    print("=" * 70)
    print()

    success = reporter.run_tests(verbose=args.verbose)
    print()

    if not success:
        print("⚠️  Tests completed with warnings or failures")
        print()

    # Generate reports
    print("=" * 70)
    print("Generating Reports")
    print("=" * 70)
    print()

    generated_any = False

    if args.shell_output or args.all_outputs:
        if args.shell_output:
            reporter.generate_json_report(args.shell_output)
            generated_any = True

    if args.json_output or args.all_outputs:
        json_file = args.json_output or "test-report.json"
        reporter.generate_json_report(json_file)
        generated_any = True

    if args.html_output or args.all_outputs:
        html_file = args.html_output or "test-report.html"
        reporter.generate_html_report(html_file)
        generated_any = True

    if args.markdown_output or args.all_outputs:
        md_file = args.markdown_output or "test-report.md"
        reporter.generate_markdown_report(md_file)
        generated_any = True

    if not generated_any and not args.all_outputs:
        # If no output format specified, just run tests
        pass

    print()
    print("=" * 70)
    if success:
        print("✅ All operations completed successfully")
        sys.exit(0)
    else:
        print("❌ Some operations failed")
        sys.exit(1)


if __name__ == "__main__":
    main()
