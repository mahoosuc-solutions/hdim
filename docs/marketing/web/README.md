# AI Solutioning Journey - Web Content

This directory contains web-ready HTML versions of the AI Solutioning Journey documentation.

## Files

### Main Index
- **`ai-solutioning-index.html`** - Main landing page with overview, timeline, document cards, and metrics

### Document Pages
- **`ai-solutioning-journey.html`** - The complete development journey narrative
- **`spec-driven-development-analysis.html`** - Spec-driven development methodology deep-dive
- **`architecture-evolution-timeline.html`** - Architecture evolution through 7 phases
- **`traditional-vs-ai-solutioning-comparison.html`** - Comprehensive comparison analysis
- **`ai-native-vs-non-ai-native-comparison.html`** - Developer comparison and usage patterns
- **`java-rebuild-deep-dive.html`** - Technical analysis of the Java rebuild
- **`ai-solutioning-metrics.html`** - Quantified achievements and statistics

## Usage

### Local Viewing
1. Open `ai-solutioning-index.html` in a web browser
2. Navigate between documents using the links
3. All pages are fully responsive (desktop, tablet, mobile)

### Deployment
These HTML files can be deployed to:
- **Vercel** - Static site hosting
- **GitHub Pages** - Free hosting
- **Netlify** - Static site hosting
- **Any web server** - Standard HTML files

### Customization
- All styling is in `<style>` tags within each HTML file
- Colors can be customized via CSS variables in `:root`
- Font is Inter (Google Fonts) - can be changed in the `<link>` tag

## Regenerating HTML

If you update the markdown source files, regenerate the HTML:

```bash
cd docs/marketing/web
python3 convert-to-html.py
```

The script will:
1. Read markdown files from `../` (parent directory)
2. Convert markdown to HTML
3. Apply the template with navigation and styling
4. Write HTML files to this directory

## Design

- **Color Scheme:**
  - Primary: #1E3A5F (Dark Blue)
  - Secondary: #00A9A5 (Teal)
  - Accent: #2E7D32 (Green)
  - Text: #2C3E50 (Dark Gray)

- **Typography:**
  - Font: Inter (Google Fonts)
  - Headings: 700-800 weight
  - Body: 400 weight, 1.8 line height

- **Layout:**
  - Max width: 900px (content), 1200px (index)
  - Responsive breakpoint: 768px
  - Fixed navigation bar
  - Hero sections with gradients

## Features

- ✅ Fully responsive design
- ✅ Consistent navigation
- ✅ Print-friendly (can be saved as PDF)
- ✅ SEO-friendly meta tags
- ✅ Accessible markup
- ✅ Fast loading (no external dependencies except fonts)

## Notes

- The markdown to HTML conversion is basic - complex markdown features may need manual adjustment
- Tables, code blocks, and lists are supported
- Links to external resources work as-is
- Internal links between documents work via relative paths
