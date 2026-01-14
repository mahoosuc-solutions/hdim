# Feature UI Generation Prompts

## Care Gap Detection Feature

### DALL-E 3 Prompt

```
Create a modern healthcare feature UI mockup showing a care gap detection interface.

LAYOUT:
- Top section: Feature header with "Care Gap Detection" title and description
- Main content area showing:
  - Patient search bar at top with filters (Name, MRN, Risk Score)
  - Care gap list showing:
    - Patient name and MRN
    - Care gap type (e.g., "Diabetes Screening Due", "Mammography Overdue")
    - Priority level (High/Medium/Low) with color indicators
    - Days overdue
    - Action buttons (Schedule, Dismiss, View Details)
  - Side panel (when patient selected) showing:
    - Patient details
    - Care gap history
    - Recommended actions
    - Schedule appointment button

STYLE:
- Clean, modern SaaS feature interface
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Priority colors: Red for high, Yellow for medium, Green for low
- White background with subtle gray cards
- Clear typography, actionable design
- Professional healthcare software appearance

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Realistic UI elements
- Clear data presentation
- Professional feature interface

AVOID:
- Cluttered interface
- Unclear priority indicators
- Generic feature screens
- Text rendering issues
```

---

## Quality Measure Evaluation Feature

### DALL-E 3 Prompt

```
Create a modern healthcare feature UI mockup showing a quality measure evaluation interface.

LAYOUT:
- Top section: Feature header with "Quality Measure Evaluation" title
- Main content area showing:
  - Measure selection dropdown at top (showing HEDIS measures)
  - 3-step wizard interface:
    - Step 1: "Select Measure" - showing measure categories and available measures
    - Step 2: "Configure Evaluation" - showing patient population filters, date range, evaluation options
    - Step 3: "View Results" - showing:
      - Summary statistics (Total Patients, Compliant, Non-Compliant, Compliance Rate)
      - Bar chart showing compliance by measure criteria
      - Data table showing patient-level results with compliance status
      - Export button for results
  - Progress indicator showing current step

STYLE:
- Clean, modern wizard interface
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Success color: Green for compliant
- Warning color: Yellow for non-compliant
- White background with subtle gray cards
- Clear step indicators
- Professional healthcare software appearance

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Realistic wizard interface
- Clear step progression
- Professional feature interface

AVOID:
- Confusing wizard flow
- Unclear step indicators
- Generic evaluation screens
- Text rendering issues
```

---

## AI Assistant Interface Feature

### DALL-E 3 Prompt

```
Create a modern healthcare AI assistant interface UI mockup.

LAYOUT:
- Split screen design:
  - Left side (60%): Chat interface showing:
    - Chat header with "AI Assistant" title and model indicator
    - Chat history showing:
      - User message: "What care gaps does patient John Doe have?"
      - AI response with reasoning explanation
      - Tool execution indicators (FHIR query, CQL evaluation)
      - Confidence scores
      - Guardrail notifications (if applicable)
    - Input area at bottom with text field and send button
  - Right side (40%): Context panel showing:
    - Patient information (if patient query)
    - Tool execution history
    - Audit trail
    - Related information

STYLE:
- Clean, modern chat interface
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Chat bubbles: Light blue for user, white for AI
- Tool execution: Subtle badges with icons
- Confidence scores: Color-coded (green/yellow/red)
- White background with subtle gray panels
- Clear typography, conversational design
- Professional healthcare AI interface

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Realistic chat interface
- Clear tool execution indicators
- Professional AI assistant interface

AVOID:
- Generic chat interfaces
- Unclear tool execution
- Confusing confidence indicators
- Text rendering issues
```

---

## Patient Search Feature

### DALL-E 3 Prompt

```
Create a modern healthcare patient search interface UI mockup.

LAYOUT:
- Top section: Search bar with:
  - Search input field (prominent, large)
  - Search filters (Name, MRN, DOB, Phone)
  - Advanced search toggle
- Main content area showing:
  - Search results list with:
    - Patient cards showing:
      - Patient name and MRN
      - Date of birth and age
      - Risk score with color indicator
      - Care gaps count
      - Last visit date
      - Quick action buttons (View, Edit, Schedule)
  - Selected patient detail panel (on right) showing:
    - Patient demographics
    - Recent visits
    - Active care gaps
    - Medications
    - Allergies

STYLE:
- Clean, modern search interface
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Search bar: Prominent, easy to find
- Patient cards: Clear hierarchy, easy to scan
- White background with subtle gray cards
- Clear typography, search-focused design
- Professional healthcare search interface

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Realistic search interface
- Clear search results
- Professional patient search interface

AVOID:
- Cluttered search results
- Unclear patient information
- Generic search interfaces
- Text rendering issues
```

---

## Usage Notes

### Customization

**Replace Placeholders:**
- `{feature_name}` → Actual feature name
- `{primary_color}` → Brand primary color
- `{accent_color}` → Brand accent color
- `{metrics}` → Actual metrics to display

**Add Context:**
- Include specific use cases
- Mention target audience
- Add workflow details
- Include interaction patterns

### Quality Tips

**For Best Results:**
1. Be specific about feature functionality
2. Include exact UI elements
3. Specify interaction patterns
4. Mention user workflows
5. Use negative prompts to avoid unwanted elements

---

**Feature UI Generation Prompts**

*Ready-to-use prompts for generating feature UI mockups with AI tools.*
