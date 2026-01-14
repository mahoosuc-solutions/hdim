#!/usr/bin/env python3
"""
Enhanced converter for AI Solutioning Journey markdown documents to HTML web pages.
Includes: Better markdown parsing, tables, social meta tags, PDF support, analytics, related content.
"""

import os
import re
from pathlib import Path
from urllib.parse import quote

# Document mapping with related content
DOCUMENTS = {
    'ai-solutioning-journey.md': {
        'title': 'The AI Solutioning Journey',
        'subtitle': 'From Node.js Prototype to Production Platform',
        'icon': '📖',
        'related': [
            ('Origin Story', '../origin-story.md'),
            ('Java Rebuild Deep-Dive', 'java-rebuild-deep-dive.html'),
            ('Spec-Driven Development', 'spec-driven-development-analysis.html'),
            ('Architecture Evolution', 'architecture-evolution-timeline.html')
        ]
    },
    'spec-driven-development-analysis.md': {
        'title': 'Spec-Driven Development Analysis',
        'subtitle': 'How Specifications Enabled AI Solutioning',
        'icon': '📋',
        'related': [
            ('AI Solutioning Journey', 'ai-solutioning-journey.html'),
            ('AI-Native vs Non-AI-Native', 'ai-native-vs-non-ai-native-comparison.html'),
            ('Traditional vs AI Solutioning', 'traditional-vs-ai-solutioning-comparison.html'),
            ('AI Solutioning Whitepaper', '../ai-solutioning-whitepaper.md')
        ]
    },
    'architecture-evolution-timeline.md': {
        'title': 'Architecture Evolution Timeline',
        'subtitle': 'From Node.js Prototype to Production Platform',
        'icon': '🏗️',
        'related': [
            ('AI Solutioning Journey', 'ai-solutioning-journey.html'),
            ('Java Rebuild Deep-Dive', 'java-rebuild-deep-dive.html'),
            ('Metrics & Statistics', 'ai-solutioning-metrics.html'),
            ('Traditional vs AI Solutioning', 'traditional-vs-ai-solutioning-comparison.html')
        ]
    },
    'traditional-vs-ai-solutioning-comparison.md': {
        'title': 'Traditional vs AI Solutioning',
        'subtitle': 'A Comprehensive Comparison',
        'icon': '⚖️',
        'related': [
            ('AI Solutioning Journey', 'ai-solutioning-journey.html'),
            ('AI-Native vs Non-AI-Native', 'ai-native-vs-non-ai-native-comparison.html'),
            ('Metrics & Statistics', 'ai-solutioning-metrics.html'),
            ('Spec-Driven Development', 'spec-driven-development-analysis.html')
        ]
    },
    'ai-native-vs-non-ai-native-comparison.md': {
        'title': 'AI-Native vs Non-AI-Native Developers',
        'subtitle': 'How AI-Native Architects Use AI Tools Differently',
        'icon': '👥',
        'related': [
            ('Spec-Driven Development', 'spec-driven-development-analysis.html'),
            ('Traditional vs AI Solutioning', 'traditional-vs-ai-solutioning-comparison.html'),
            ('AI Solutioning Journey', 'ai-solutioning-journey.html'),
            ('Sales Narrative', '../sales-narrative.md')
        ]
    },
    'java-rebuild-deep-dive.md': {
        'title': 'The Java Rebuild',
        'subtitle': 'Technical Deep-Dive',
        'icon': '☕',
        'related': [
            ('AI Solutioning Journey', 'ai-solutioning-journey.html'),
            ('Architecture Evolution', 'architecture-evolution-timeline.html'),
            ('Metrics & Statistics', 'ai-solutioning-metrics.html'),
            ('Solution Architect Presentation', '../solution-architect-presentation.md')
        ]
    },
    'ai-solutioning-metrics.md': {
        'title': 'AI Solutioning Metrics',
        'subtitle': 'Quantifying the Achievement',
        'icon': '📊',
        'related': [
            ('Traditional vs AI Solutioning', 'traditional-vs-ai-solutioning-comparison.html'),
            ('AI Solutioning Journey', 'ai-solutioning-journey.html'),
            ('Architecture Evolution', 'architecture-evolution-timeline.html'),
            ('Executive Summary', 'executive-summary.html')
        ]
    }
}

# Base URL for social sharing (update when deployed)
BASE_URL = "https://your-domain.com/ai-solutioning"  # Update this when deployed

def generate_social_meta_tags(doc_info, filename):
    """Generate Open Graph and Twitter Card meta tags."""
    title = doc_info['title']
    description = doc_info['subtitle']
    url = f"{BASE_URL}/{filename}"
    image_url = f"{BASE_URL}/og-image-{filename.replace('.html', '')}.jpg"  # Placeholder
    
    return f'''    <!-- Open Graph / Facebook -->
    <meta property="og:type" content="article">
    <meta property="og:url" content="{url}">
    <meta property="og:title" content="{title}">
    <meta property="og:description" content="{description}">
    <meta property="og:image" content="{image_url}">
    
    <!-- Twitter -->
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:url" content="{url}">
    <meta name="twitter:title" content="{title}">
    <meta name="twitter:description" content="{description}">
    <meta name="twitter:image" content="{image_url}">'''

def generate_related_content(related):
    """Generate related content section HTML."""
    if not related:
        return ""
    
    items = []
    for title, link in related:
        items.append(f'            <li><a href="{link}">{title}</a></li>')
    
    return f'''
        <div class="related-content">
            <h3>Related Content</h3>
            <ul>
{chr(10).join(items)}
            </ul>
        </div>'''

def generate_social_sharing_buttons(title, url):
    """Generate social sharing buttons HTML."""
    encoded_title = quote(title)
    encoded_url = quote(url)
    
    return f'''
        <div class="social-sharing">
            <h4>Share this page</h4>
            <div class="share-buttons">
                <a href="https://twitter.com/intent/tweet?text={encoded_title}&url={encoded_url}" target="_blank" class="share-btn twitter" title="Share on Twitter">
                    Twitter
                </a>
                <a href="https://www.linkedin.com/sharing/share-offsite/?url={encoded_url}" target="_blank" class="share-btn linkedin" title="Share on LinkedIn">
                    LinkedIn
                </a>
                <a href="https://www.facebook.com/sharer/sharer.php?u={encoded_url}" target="_blank" class="share-btn facebook" title="Share on Facebook">
                    Facebook
                </a>
                <a href="mailto:?subject={encoded_title}&body={encoded_url}" class="share-btn email" title="Share via Email">
                    Email
                </a>
            </div>
        </div>'''

HTML_TEMPLATE = '''<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{title} - AI Solutioning Journey</title>
    <meta name="description" content="{subtitle}">
{social_meta}
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <!-- Google Analytics -->
    <script async src="https://www.googletagmanager.com/gtag/js?id=GA_MEASUREMENT_ID"></script>
    <script>
        window.dataLayer = window.dataLayer || [];
        function gtag(){{dataLayer.push(arguments);}}
        gtag('js', new Date());
        gtag('config', 'GA_MEASUREMENT_ID');
    </script>
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
            box-shadow: 0 4px 20px rgba(0,0,0,0.08);
            border-radius: 8px;
            overflow: hidden;
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

        .related-content {{
            background: var(--light);
            padding: 30px;
            border-radius: 12px;
            margin: 40px 0;
            border-left: 4px solid var(--secondary);
        }}

        .related-content h3 {{
            font-size: 20px;
            font-weight: 700;
            color: var(--primary);
            margin-bottom: 15px;
        }}

        .related-content ul {{
            list-style: none;
            margin: 0;
        }}

        .related-content li {{
            margin-bottom: 10px;
        }}

        .related-content a {{
            color: var(--secondary);
            text-decoration: none;
            font-weight: 500;
        }}

        .related-content a:hover {{
            text-decoration: underline;
        }}

        .social-sharing {{
            background: var(--light);
            padding: 30px;
            border-radius: 12px;
            margin: 40px 0;
        }}

        .social-sharing h4 {{
            font-size: 18px;
            font-weight: 600;
            color: var(--primary);
            margin-bottom: 15px;
        }}

        .share-buttons {{
            display: flex;
            gap: 15px;
            flex-wrap: wrap;
        }}

        .share-btn {{
            padding: 10px 20px;
            border-radius: 6px;
            text-decoration: none;
            font-weight: 600;
            font-size: 14px;
            transition: transform 0.2s, box-shadow 0.2s;
            color: white;
        }}

        .share-btn.twitter {{
            background: #1DA1F2;
        }}

        .share-btn.linkedin {{
            background: #0077B5;
        }}

        .share-btn.facebook {{
            background: #4267B2;
        }}

        .share-btn.email {{
            background: var(--secondary);
        }}

        .share-btn:hover {{
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0,0,0,0.2);
        }}

        .action-buttons {{
            display: flex;
            gap: 15px;
            margin: 40px 0;
            flex-wrap: wrap;
        }}

        .btn {{
            padding: 12px 24px;
            border-radius: 6px;
            font-weight: 600;
            font-size: 14px;
            text-decoration: none;
            transition: background 0.2s, transform 0.2s;
            display: inline-block;
        }}

        .btn-primary {{
            background: var(--secondary);
            color: white;
        }}

        .btn-primary:hover {{
            background: #00897B;
            transform: translateY(-2px);
        }}

        .btn-secondary {{
            background: var(--primary);
            color: white;
        }}

        .btn-secondary:hover {{
            background: #2C5282;
            transform: translateY(-2px);
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

        @media print {{
            nav, .social-sharing, .action-buttons {{
                display: none;
            }}
            
            .content {{
                max-width: 100%;
                padding: 20px;
            }}
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
        
        <div class="action-buttons">
            <a href="javascript:window.print()" class="btn btn-primary">📄 Download PDF</a>
            <a href="executive-summary.html" class="btn btn-secondary">📊 Executive Summary</a>
        </div>
{related_content}
{social_sharing}
        <a href="ai-solutioning-index.html" class="back-link">← Back to Index</a>
    </div>

    <footer>
        <p>&copy; 2026 HDIM Platform. The AI Solutioning Journey.</p>
    </footer>
</body>
</html>'''


def parse_markdown_table(text):
    """Parse markdown table to HTML table."""
    lines = text.strip().split('\n')
    if len(lines) < 2:
        return text
    
    # Check if it's a table (has | separators)
    if '|' not in lines[0]:
        return text
    
    html = ['<table>']
    
    # Header row
    header = [cell.strip() for cell in lines[0].split('|') if cell.strip()]
    if header:
        html.append('<thead><tr>')
        for cell in header:
            html.append(f'<th>{cell}</th>')
        html.append('</tr></thead>')
    
    # Skip separator row (|---|---|)
    start_idx = 2
    
    # Data rows
    html.append('<tbody>')
    for line in lines[start_idx:]:
        if '|' not in line:
            break
        cells = [cell.strip() for cell in line.split('|') if cell.strip()]
        if cells:
            html.append('<tr>')
            for cell in cells:
                # Check for highlight class
                if cell.startswith('**') and cell.endswith('**'):
                    html.append(f'<td class="highlight"><strong>{cell[2:-2]}</strong></td>')
                else:
                    html.append(f'<td>{cell}</td>')
            html.append('</tr>')
    html.append('</tbody>')
    html.append('</table>')
    
    return '\n'.join(html)


def markdown_to_html(markdown_text):
    """Convert markdown to HTML with enhanced formatting."""
    html = markdown_text
    
    # Remove frontmatter if present
    if html.startswith('---'):
        parts = html.split('---', 2)
        if len(parts) >= 3:
            html = parts[2].strip()
    
    # Tables (process before other conversions)
    table_pattern = r'(\|.+\|\n\|[-\s\|]+\|\n(?:\|.+\|\n?)+)'
    def replace_table(match):
        return parse_markdown_table(match.group(1))
    html = re.sub(table_pattern, replace_table, html, flags=re.MULTILINE)
    
    # Headers
    html = re.sub(r'^# (.+)$', r'<h1>\1</h1>', html, flags=re.MULTILINE)
    html = re.sub(r'^## (.+)$', r'<h2>\1</h2>', html, flags=re.MULTILINE)
    html = re.sub(r'^### (.+)$', r'<h3>\1</h3>', html, flags=re.MULTILINE)
    html = re.sub(r'^#### (.+)$', r'<h4>\1</h4>', html, flags=re.MULTILINE)
    
    # Bold (after headers to avoid conflicts)
    html = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', html)
    
    # Italic (but not if already in strong)
    html = re.sub(r'(?<!\*)\*([^*]+?)\*(?!\*)', r'<em>\1</em>', html)
    
    # Code blocks
    html = re.sub(r'```(\w+)?\n(.*?)```', r'<pre><code>\2</code></pre>', html, flags=re.DOTALL)
    
    # Inline code
    html = re.sub(r'`([^`]+?)`', r'<code>\1</code>', html)
    
    # Links
    html = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'<a href="\2">\1</a>', html)
    
    # Horizontal rules
    html = re.sub(r'^---$', r'<hr>', html, flags=re.MULTILINE)
    
    # Blockquotes
    html = re.sub(r'^> (.+)$', r'<blockquote>\1</blockquote>', html, flags=re.MULTILINE)
    
    # Lists - handle unordered lists
    lines = html.split('\n')
    result = []
    in_list = False
    list_items = []
    
    for line in lines:
        stripped = line.strip()
        
        # Check for list item
        if re.match(r'^[-*]\s+', stripped):
            if not in_list:
                in_list = True
            list_items.append(re.sub(r'^[-*]\s+', '', stripped))
        elif re.match(r'^\d+\.\s+', stripped):
            if not in_list:
                in_list = True
            list_items.append(re.sub(r'^\d+\.\s+', '', stripped))
        else:
            if in_list and list_items:
                result.append('<ul>')
                for item in list_items:
                    result.append(f'<li>{item}</li>')
                result.append('</ul>')
                list_items = []
                in_list = False
            
            if stripped and not stripped.startswith('<'):
                # Regular paragraph
                if not any(stripped.startswith(tag) for tag in ['<h', '<p', '<ul', '<ol', '<table', '<pre', '<blockquote', '<hr']):
                    result.append(f'<p>{stripped}</p>')
                else:
                    result.append(stripped)
            elif not stripped:
                result.append('')
    
    # Close any remaining list
    if in_list and list_items:
        result.append('<ul>')
        for item in list_items:
            result.append(f'<li>{item}</li>')
        result.append('</ul>')
    
    return '\n'.join(result)


def convert_document(md_file, output_dir):
    """Convert a single markdown document to HTML."""
    doc_info = DOCUMENTS.get(md_file.name)
    if not doc_info:
        print(f"Warning: No metadata for {md_file.name}")
        return
    
    # Read markdown
    with open(md_file, 'r', encoding='utf-8') as f:
        markdown_content = f.read()
    
    # Convert to HTML
    html_content = markdown_to_html(markdown_content)
    
    # Generate social meta tags
    filename = f"{md_file.stem}.html"
    social_meta = generate_social_meta_tags(doc_info, filename)
    
    # Generate related content
    related_content = generate_related_content(doc_info.get('related', []))
    
    # Generate social sharing
    url = f"{BASE_URL}/{filename}"
    social_sharing = generate_social_sharing_buttons(doc_info['title'], url)
    
    # Generate HTML page
    html_output = HTML_TEMPLATE.format(
        title=doc_info['title'],
        subtitle=doc_info['subtitle'],
        icon=doc_info['icon'],
        content=html_content,
        social_meta=social_meta,
        related_content=related_content,
        social_sharing=social_sharing
    )
    
    # Write HTML file
    output_file = output_dir / filename
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
