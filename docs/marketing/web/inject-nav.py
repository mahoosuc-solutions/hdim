#!/usr/bin/env python3
"""
Inject shared navigation into all HTML pages in the marketing web directory.
Run from the docs/marketing/web directory.
"""

import os
import re

# Read the shared navigation component
with open('shared-nav.html', 'r') as f:
    nav_content = f.read()

# List of files to update (excludes shared-nav.html itself)
html_files = [
    'ai-native-vs-non-ai-native-comparison.html',
    'ai-solutioning-index.html',
    'ai-solutioning-journey.html',
    'ai-solutioning-metrics.html',
    'architecture-evolution-timeline.html',
    'cms-vision.html',
    'executive-summary.html',
    'java-rebuild-deep-dive.html',
    'performance-benchmarking.html',
    'platform-architecture.html',
    'spec-driven-development-analysis.html',
    'traditional-vs-ai-solutioning-comparison.html',
    'vision-deck.html',
]

for filename in html_files:
    if not os.path.exists(filename):
        print(f"⚠️  Skipping {filename} (not found)")
        continue

    with open(filename, 'r') as f:
        content = f.read()

    # Check if already has the new nav
    if 'hdim-nav' in content:
        print(f"✓ {filename} already has new navigation")
        continue

    # Find <body> tag and insert nav after it
    body_match = re.search(r'<body[^>]*>', content)
    if not body_match:
        print(f"⚠️  Skipping {filename} (no <body> tag found)")
        continue

    # Remove old nav if present (simple nav with class="nav-links")
    # Pattern to match old nav blocks
    old_nav_patterns = [
        r'<nav>\s*<a[^>]*class="logo"[^>]*>.*?</nav>',  # Old simple nav
        r'<nav class="nav"[^>]*>.*?</nav>',  # Another pattern
    ]

    for pattern in old_nav_patterns:
        content = re.sub(pattern, '', content, flags=re.DOTALL)

    # Insert new nav after <body>
    insert_pos = body_match.end()
    new_content = content[:insert_pos] + '\n' + nav_content + '\n' + content[insert_pos:]

    # Adjust hero padding if present (to account for fixed nav)
    # Only for pages that don't already have the spacer
    if 'hdim-nav-spacer' not in content:
        # The spacer is included in the nav component, but we may need to adjust hero sections
        pass

    with open(filename, 'w') as f:
        f.write(new_content)

    print(f"✓ Updated {filename}")

print("\n✅ Navigation injection complete!")
print("Note: You may need to adjust hero section padding on some pages.")
