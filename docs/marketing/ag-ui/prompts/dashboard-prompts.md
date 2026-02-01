# Dashboard UI Generation Prompts

## Clinical Dashboard - Light Mode

### DALL-E 3 Prompt

```
Create a modern healthcare analytics dashboard UI mockup for a clinical care management platform.

LAYOUT:
- Top navigation bar (white background, subtle shadow) with HDIM logo on the left, user menu on the right
- Left sidebar navigation (240px wide, light gray background #F5F7FA) with icons: Dashboard, Patients, Care Gaps, Quality Measures, Analytics, Settings
- Main content area (white background) showing:
  - Summary statistics row at top: 4 cards showing "Total Patients: 12,847", "Open Care Gaps: 2,340", "Closed This Month: +847", "HEDIS Score: 78.4%"
  - Large bar chart showing care gap closure trend over 10 weeks (ascending blue bars, teal accent #00A9A5)
  - Data table below showing priority patients with columns: Name, MRN, Risk Score, Care Gaps, Status
  - Map visualization on right side showing geographic distribution of patients

STYLE:
- Clean, modern SaaS aesthetic (similar to Stripe, Linear, or Vercel dashboards)
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Background: White #FFFFFF with subtle gray cards #F5F7FA
- Typography: Clean sans-serif (Inter or similar), clear hierarchy
- Generous whitespace, minimal borders
- Subtle shadows on cards for depth
- Professional, trustworthy, modern

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- No text artifacts or distortions
- Realistic UI elements, not wireframes
- Professional healthcare software appearance

AVOID:
- Generic stock photography
- Cluttered interfaces
- Cold/clinical aesthetics
- Unrealistic data displays
- Text rendering issues
```

### Midjourney Prompt

```
modern healthcare analytics dashboard UI, clinical care management platform, top navigation bar with logo, left sidebar navigation with icons, main content area with summary statistics cards, bar chart showing care gap closure trend, data table with patient information, map visualization, clean modern SaaS aesthetic, primary color deep blue #1E3A5F, accent color teal #00A9A5, white background with subtle gray cards, clean typography, generous whitespace, professional healthcare software, 1920x1080, high quality, realistic UI elements --ar 16:9 --style raw --v 6
```

### Stable Diffusion Prompt

```
modern healthcare analytics dashboard UI, clinical care management platform, top navigation bar, left sidebar navigation, summary statistics cards, bar chart, data table, map visualization, clean modern SaaS aesthetic, deep blue #1E3A5F, teal #00A9A5, white background, professional healthcare software, high quality, realistic UI elements

Negative prompt: generic stock photo, cluttered interface, wireframe, low quality, distorted text, artifacts
```

---

## Clinical Dashboard - Dark Mode

### DALL-E 3 Prompt

```
Create a modern healthcare analytics dashboard UI mockup in dark mode for a clinical care management platform.

LAYOUT:
- Top navigation bar (dark gray background #1A1A1A, subtle border) with HDIM logo on the left, user menu on the right
- Left sidebar navigation (240px wide, dark background #2C2C2C) with icons: Dashboard, Patients, Care Gaps, Quality Measures, Analytics, Settings
- Main content area (dark background #1A1A1A) showing:
  - Summary statistics row at top: 4 cards showing "Total Patients: 12,847", "Open Care Gaps: 2,340", "Closed This Month: +847", "HEDIS Score: 78.4%" (cards with dark gray background #2C2C2C)
  - Large bar chart showing care gap closure trend over 10 weeks (ascending blue bars, teal accent #00A9A5, dark background)
  - Data table below showing priority patients with columns: Name, MRN, Risk Score, Care Gaps, Status (dark rows with hover effects)
  - Map visualization on right side showing geographic distribution (dark theme)

STYLE:
- Clean, modern dark mode SaaS aesthetic
- Primary color: Light Blue #2C5282 (for highlights)
- Accent color: Teal #00A9A5
- Background: Dark #1A1A1A with dark gray cards #2C2C2C
- Text: Light gray #E0E0E0 for readability
- Typography: Clean sans-serif, clear hierarchy
- Generous whitespace, subtle borders
- Professional, modern, easy on the eyes

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- No text artifacts
- Realistic dark mode UI
- Professional healthcare software appearance

AVOID:
- Too dark (unreadable)
- Harsh contrasts
- Generic dark themes
- Text rendering issues
```

---

## Admin Dashboard

### DALL-E 3 Prompt

```
Create a modern healthcare system administration dashboard UI mockup.

LAYOUT:
- Top navigation bar with HDIM logo, notifications icon, user menu
- Left sidebar with admin navigation: Dashboard, Users, Roles, Audit Logs, Integrations, System Health, Settings
- Main content area showing:
  - System health overview: 6 service status cards (all green/healthy) showing service names and uptime percentages
  - Recent activity feed showing audit events, user actions, system events
  - Integration status panel showing connected systems (Epic, Cerner, AllScripts) with connection status indicators
  - Performance metrics: API response times, event throughput, database performance

STYLE:
- Clean, modern admin interface
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Status colors: Green for healthy, Yellow for warning, Red for error
- White background with subtle gray cards
- Clear typography, data-focused design
- Professional, technical, trustworthy

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Realistic admin dashboard appearance
- Clear status indicators
- Professional healthcare software

AVOID:
- Cluttered interface
- Unclear status indicators
- Generic admin panels
```

---

## Analytics Dashboard

### DALL-E 3 Prompt

```
Create a modern healthcare analytics dashboard UI mockup focused on data visualization and reporting.

LAYOUT:
- Top navigation bar with HDIM logo, date range selector, export button
- Left sidebar with analytics navigation: Overview, Quality Metrics, Population Health, Financial Analytics, Custom Reports
- Main content area showing:
  - Large line chart showing quality measure trends over 12 months (multiple lines for different measures)
  - Pie chart showing care gap distribution by category
  - Heat map showing geographic performance metrics
  - Summary statistics: Total members, Quality score, Cost per member, Risk score average
  - Data table with drill-down capabilities

STYLE:
- Data-focused, analytical interface
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Chart colors: Blue gradient, teal accent, professional color palette
- White background with subtle gray cards
- Clear data visualization, professional charts
- Clean typography, data-first design

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Professional data visualization
- Clear charts and graphs
- Realistic analytics dashboard

AVOID:
- Cluttered charts
- Unclear data visualization
- Generic analytics dashboards
```

---

## Mobile Dashboard

### DALL-E 3 Prompt

```
Create a modern mobile healthcare dashboard UI mockup for a clinical care management platform.

LAYOUT:
- Top bar with hamburger menu icon, HDIM logo, notifications icon
- Collapsed navigation (hamburger menu accessible)
- Main content area showing:
  - Summary statistics cards (stacked vertically): Total Patients, Open Care Gaps, Closed This Month, HEDIS Score
  - Simplified line chart showing care gap closure trend (mobile-optimized)
  - Swipeable patient cards showing: Name, MRN, Risk Score, Care Gaps count
  - Bottom navigation bar with icons: Home, Patients, Gaps, More

STYLE:
- Mobile-first, touch-friendly design
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- White background with subtle gray cards
- Large touch targets, readable text
- Clean, modern mobile UI
- Professional healthcare app appearance

TECHNICAL:
- Resolution: 375x812 pixels (iPhone X size)
- Format: High-quality PNG
- Mobile-optimized layout
- Touch-friendly interface
- Realistic mobile app UI

AVOID:
- Desktop layout on mobile
- Too small text
- Unclear navigation
- Generic mobile apps
```

---

## Usage Notes

### Prompt Customization

**Replace Placeholders:**
- `{primary_color}` → Actual brand color (#1E3A5F)
- `{accent_color}` → Actual accent color (#00A9A5)
- `{logo}` → HDIM logo description
- `{metrics}` → Actual metrics to display

**Add Context:**
- Include specific use cases
- Mention target audience
- Add brand guidelines
- Include design system references

### Quality Tips

**For Best Results:**
1. Be specific about layout and elements
2. Include exact color codes
3. Specify dimensions clearly
4. Mention style references (e.g., "similar to Stripe")
5. Use negative prompts to avoid unwanted elements

**Iteration:**
- Start with base prompt
- Refine based on results
- Save successful variations
- Build prompt library

---

**Dashboard UI Generation Prompts**

*Ready-to-use prompts for generating dashboard UI mockups with AI tools.*
