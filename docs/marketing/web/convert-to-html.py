#!/usr/bin/env python3
"""
Convert AI Solutioning Journey markdown documents to HTML web pages.
"""

import os
import re
from pathlib import Path

# Document mapping
DOCUMENTS = {
    'ai-solutioning-journey.md': {
        'title': 'The AI Solutioning Journey',
        'subtitle': 'From Node.js Prototype to Production Platform',
        'icon': '📖'
    },
    'spec-driven-development-analysis.md': {
        'title': 'Spec-Driven Development Analysis',
        'subtitle': 'How Specifications Enabled AI Solutioning',
        'icon': '📋'
    },
    'architecture-evolution-timeline.md': {
        'title': 'Architecture Evolution Timeline',
        'subtitle': 'From Node.js Prototype to Production Platform',
        'icon': '🏗️'
    },
    'traditional-vs-ai-solutioning-comparison.md': {
        'title': 'Traditional vs AI Solutioning',
        'subtitle': 'A Comprehensive Comparison',
        'icon': '⚖️'
    },
    'ai-native-vs-non-ai-native-comparison.md': {
        'title': 'AI-Native vs Non-AI-Native Developers',
        'subtitle': 'How AI-Native Architects Use AI Tools Differently',
        'icon': '👥'
    },
    'java-rebuild-deep-dive.md': {
        'title': 'The Java Rebuild',
        'subtitle': 'Technical Deep-Dive',
        'icon': '☕'
    },
    'ai-solutioning-metrics.md': {
        'title': 'AI Solutioning Metrics',
        'subtitle': 'Quantifying the Achievement',
        'icon': '📊'
    },
    'performance-benchmarking.md': {
        'title': 'Performance Benchmarking',
        'subtitle': 'CQL/FHIR vs Traditional SQL - 2-4x Faster',
        'icon': '⚡'
    }
}

HTML_TEMPLATE = '''<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{title} - AI Solutioning Journey</title>
    <meta name="description" content="{subtitle}">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}

        :root {{
            --primary: #1E3A5F;
            --secondary: #00A9A5;
            --accent: #2E7D32;
            --text: #2C3E50;
            --text-light: #6C757D;
            --light: #F5F7FA;
            --white: #FFFFFF;
            --border: #E0E0E0;
        }}

        body {{
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            color: var(--text);
            line-height: 1.8;
            background: var(--light);
        }}

        nav {{
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            background: var(--white);
            padding: 20px 40px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            z-index: 1000;
        }}

        .logo {{
            font-size: 24px;
            font-weight: 700;
            color: var(--primary);
            text-decoration: none;
            letter-spacing: 1px;
        }}

        .nav-links {{
            display: flex;
            gap: 30px;
        }}

        .nav-links a {{
            text-decoration: none;
            color: var(--text);
            font-weight: 500;
            font-size: 14px;
            transition: color 0.2s;
        }}

        .nav-links a:hover {{
            color: var(--secondary);
        }}

        .hero {{
            padding: 180px 40px 80px;
            background: linear-gradient(135deg, var(--primary) 0%, #2C5282 100%);
            color: white;
            text-align: center;
        }}

        .hero h1 {{
            font-size: 48px;
            font-weight: 800;
            margin-bottom: 16px;
        }}

        .hero .subtitle {{
            font-size: 20px;
            font-weight: 400;
            opacity: 0.95;
        }}

        .content {{
            max-width: 900px;
            margin: 0 auto;
            padding: 60px 40px;
            background: var(--white);
        }}

        .content h1 {{
            font-size: 36px;
            font-weight: 700;
            color: var(--primary);
            margin: 40px 0 20px;
            padding-top: 20px;
            border-top: 3px solid var(--secondary);
        }}

        .content h2 {{
            font-size: 28px;
            font-weight: 700;
            color: var(--primary);
            margin: 35px 0 15px;
            padding-top: 15px;
        }}

        .content h3 {{
            font-size: 22px;
            font-weight: 600;
            color: var(--primary);
            margin: 30px 0 12px;
        }}

        .content h4 {{
            font-size: 18px;
            font-weight: 600;
            color: var(--text);
            margin: 25px 0 10px;
        }}

        .content p {{
            margin-bottom: 20px;
            font-size: 16px;
            line-height: 1.8;
        }}

        .content ul, .content ol {{
            margin: 20px 0 20px 30px;
        }}

        .content li {{
            margin-bottom: 10px;
            font-size: 16px;
            line-height: 1.7;
        }}

        .content strong {{
            font-weight: 600;
            color: var(--primary);
        }}

        .content em {{
            font-style: italic;
            color: var(--text-light);
        }}

        .content code {{
            background: var(--light);
            padding: 2px 6px;
            border-radius: 4px;
            font-family: 'Courier New', monospace;
            font-size: 14px;
            color: var(--accent);
        }}

        .content pre {{
            background: var(--light);
            padding: 20px;
            border-radius: 8px;
            overflow-x: auto;
            margin: 20px 0;
            border-left: 4px solid var(--secondary);
        }}

        .content pre code {{
            background: none;
            padding: 0;
            color: var(--text);
        }}

        .content blockquote {{
            border-left: 4px solid var(--secondary);
            padding-left: 20px;
            margin: 20px 0;
            font-style: italic;
            color: var(--text-light);
        }}

        .content table {{
            width: 100%;
            border-collapse: collapse;
            margin: 25px 0;
        }}

        .content th {{
            background: var(--primary);
            color: white;
            padding: 12px;
            text-align: left;
            font-weight: 600;
        }}

        .content td {{
            padding: 12px;
            border-bottom: 1px solid var(--border);
        }}

        .content tr:hover {{
            background: var(--light);
        }}

        .content hr {{
            border: none;
            border-top: 2px solid var(--border);
            margin: 40px 0;
        }}

        .back-link {{
            display: inline-block;
            margin-top: 40px;
            padding: 12px 24px;
            background: var(--secondary);
            color: white;
            text-decoration: none;
            border-radius: 6px;
            font-weight: 600;
            transition: background 0.2s;
        }}

        .back-link:hover {{
            background: #00897B;
        }}

        footer {{
            background: var(--primary);
            color: white;
            padding: 40px;
            text-align: center;
        }}

        footer p {{
            opacity: 0.8;
            font-size: 14px;
        }}

        @media (max-width: 768px) {{
            .hero h1 {{
                font-size: 32px;
            }}

            .hero .subtitle {{
                font-size: 18px;
            }}

            .content {{
                padding: 40px 20px;
            }}

            .content h1 {{
                font-size: 28px;
            }}

            .content h2 {{
                font-size: 24px;
            }}

            nav {{
                padding: 15px 20px;
            }}

            .nav-links {{
                display: none;
            }}
        }}
    </style>
</head>
<body>
    <nav>
        <a href="ai-solutioning-index.html" class="logo">HDIM</a>
        <div class="nav-links">
            <a href="ai-solutioning-index.html">Home</a>
            <a href="ai-solutioning-index.html#documents">Documents</a>
            <a href="ai-solutioning-index.html#metrics">Metrics</a>
        </div>
    </nav>

    <section class="hero">
        <h1>{icon} {title}</h1>
        <p class="subtitle">{subtitle}</p>
    </section>

    <div class="content">
{content}
        <a href="ai-solutioning-index.html" class="back-link">← Back to Index</a>
    </div>

    <footer>
        <p>&copy; 2026 HDIM Platform. The AI Solutioning Journey.</p>
    </footer>
</body>
</html>'''


def markdown_to_html(markdown_text):
    """Convert markdown to HTML with basic formatting."""
    html = markdown_text
    
    # Headers
    html = re.sub(r'^# (.+)$', r'<h1>\1</h1>', html, flags=re.MULTILINE)
    html = re.sub(r'^## (.+)$', r'<h2>\1</h2>', html, flags=re.MULTILINE)
    html = re.sub(r'^### (.+)$', r'<h3>\1</h3>', html, flags=re.MULTILINE)
    html = re.sub(r'^#### (.+)$', r'<h4>\1</h4>', html, flags=re.MULTILINE)
    
    # Bold
    html = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', html)
    
    # Italic
    html = re.sub(r'\*(.+?)\*', r'<em>\1</em>', html)
    
    # Code blocks
    html = re.sub(r'```(\w+)?\n(.*?)```', r'<pre><code>\2</code></pre>', html, flags=re.DOTALL)
    
    # Inline code
    html = re.sub(r'`(.+?)`', r'<code>\1</code>', html)
    
    # Links
    html = re.sub(r'\[(.+?)\]\((.+?)\)', r'<a href="\2">\1</a>', html)
    
    # Horizontal rules
    html = re.sub(r'^---$', r'<hr>', html, flags=re.MULTILINE)
    
    # Paragraphs (lines that aren't headers, lists, or code blocks)
    lines = html.split('\n')
    result = []
    in_paragraph = False
    current_paragraph = []
    
    for line in lines:
        line = line.strip()
        if not line:
            if current_paragraph:
                result.append('<p>' + ' '.join(current_paragraph) + '</p>')
                current_paragraph = []
            in_paragraph = False
            continue
        
        if line.startswith('<') or line.startswith('*') or line.startswith('-') or line.startswith('1.'):
            if current_paragraph:
                result.append('<p>' + ' '.join(current_paragraph) + '</p>')
                current_paragraph = []
            result.append(line)
            in_paragraph = False
        else:
            current_paragraph.append(line)
            in_paragraph = True
    
    if current_paragraph:
        result.append('<p>' + ' '.join(current_paragraph) + '</p>')
    
    # Lists
    html = '\n'.join(result)
    html = re.sub(r'^(\s*)-\s+(.+)$', r'\1<li>\2</li>', html, flags=re.MULTILINE)
    html = re.sub(r'(<li>.*</li>)', r'<ul>\1</ul>', html, flags=re.DOTALL)
    
    # Tables (basic support)
    html = re.sub(r'^\|(.+)\|$', r'<tr><td>\1</td></tr>', html, flags=re.MULTILINE)
    
    return html


def convert_document(md_file, output_dir):
    """Convert a single markdown document to HTML."""
    doc_info = DOCUMENTS.get(md_file.name)
    if not doc_info:
        print(f"Warning: No metadata for {md_file.name}")
        return
    
    # Read markdown
    with open(md_file, 'r', encoding='utf-8') as f:
        markdown_content = f.read()
    
    # Remove frontmatter if present
    if markdown_content.startswith('---'):
        parts = markdown_content.split('---', 2)
        if len(parts) >= 3:
            markdown_content = parts[2].strip()
    
    # Convert to HTML
    html_content = markdown_to_html(markdown_content)
    
    # Generate HTML page
    html_output = HTML_TEMPLATE.format(
        title=doc_info['title'],
        subtitle=doc_info['subtitle'],
        icon=doc_info['icon'],
        content=html_content
    )
    
    # Write HTML file
    output_file = output_dir / f"{md_file.stem}.html"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(html_output)
    
    print(f"✓ Converted {md_file.name} → {output_file.name}")


def main():
    """Main conversion function."""
    # Paths
    script_dir = Path(__file__).parent
    markdown_dir = script_dir.parent
    output_dir = script_dir
    
    # Convert each document
    for md_file in markdown_dir.glob('*.md'):
        if md_file.name in DOCUMENTS:
            convert_document(md_file, output_dir)
        else:
            print(f"Skipping {md_file.name} (not in DOCUMENTS mapping)")


if __name__ == '__main__':
    main()
