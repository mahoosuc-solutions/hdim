# On-Screen Help System - Implementation Guide

**Status**: ✅ Components Created
**Date**: 2025-11-19
**Version**: 1.0.0

---

## Overview

The Clinical Portal now includes a comprehensive on-screen help system with three main components:

1. **Help Tooltips** - Inline contextual help next to form fields
2. **Help Panel** - Sliding panel with detailed help, FAQs, and quick links
3. **Help Content Service** - Centralized help content management

---

## Components Created

### 1. HelpTooltipComponent

**Location**: `apps/clinical-portal/src/app/shared/components/help-tooltip/`

**Purpose**: Display small contextual help tips next to UI elements

**Features**:
- ✅ Question mark icon with hover/click tooltip
- ✅ Customizable position (top, right, bottom, left)
- ✅ Optional "Learn more" links
- ✅ Keyboard accessible (ARIA compliant)
- ✅ Mobile responsive
- ✅ Smooth animations

**Usage Example**:
```typescript
import { HelpTooltipComponent } from './shared/components/help-tooltip/help-tooltip.component';

@Component({
  selector: 'app-measure-form',
  standalone: true,
  imports: [CommonModule, HelpTooltipComponent],
  template: `
    <div class="form-field">
      <label>
        Measure Name
        <app-help-tooltip
          text="Enter a descriptive name like 'CDC-A1C - Diabetes HbA1c Control'"
          [position]="'right'">
        </app-help-tooltip>
      </label>
      <input type="text" [(ngModel)]="measureName">
    </div>

    <div class="form-field">
      <label>
        CQL Text
        <app-help-tooltip
          text="Clinical Quality Language defines measure logic"
          link="https://cql.hl7.org/"
          [position]="'top'">
        </app-help-tooltip>
      </label>
      <textarea [(ngModel)]="cqlText"></textarea>
    </div>
  `
})
```

---

### 2. HelpPanelComponent

**Location**: `apps/clinical-portal/src/app/shared/components/help-panel/`

**Purpose**: Display comprehensive help content in a sliding side panel

**Features**:
- ✅ Slide-in animation from right side
- ✅ Searchable help sections
- ✅ Expandable/collapsible sections
- ✅ Quick links to documentation
- ✅ Keyboard shortcuts reference
- ✅ Contact support button
- ✅ Fully keyboard navigable
- ✅ Mobile responsive (full screen on mobile)

**Usage Example**:
```typescript
import { HelpPanelComponent, HelpSection } from './shared/components/help-panel/help-panel.component';
import { HelpContentService } from './services/help-content.service';

@Component({
  selector: 'app-measure-builder',
  standalone: true,
  imports: [CommonModule, HelpPanelComponent],
  template: `
    <!-- Help Button in Header -->
    <header>
      <h1>Measure Builder</h1>
      <button (click)="showHelp = true" class="help-button">
        <span>Help</span>
        <kbd>?</kbd>
      </button>
    </header>

    <!-- Main Content -->
    <main>
      <!-- Your page content -->
    </main>

    <!-- Help Panel -->
    <app-help-panel
      [isOpen]="showHelp"
      [title]="'Measure Builder Help'"
      [sections]="helpSections"
      [quickLinks]="quickLinks"
      [searchable]="true"
      [showKeyboardShortcuts]="true"
      (close)="showHelp = false"
      (contactSupport)="onContactSupport()">
    </app-help-panel>
  `
})
export class MeasureBuilderComponent {
  showHelp = false;
  helpSections: HelpSection[] = [];
  quickLinks = [];

  constructor(private helpContent: HelpContentService) {
    this.helpSections = this.helpContent.getMeasureBuilderHelp();
    this.quickLinks = this.helpContent.getQuickLinks();
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboard(event: KeyboardEvent) {
    // Toggle help panel with "?" key
    if (event.key === '?' && !this.isInputFocused()) {
      event.preventDefault();
      this.showHelp = !this.showHelp;
    }
  }

  private isInputFocused(): boolean {
    const activeElement = document.activeElement;
    return activeElement?.tagName === 'INPUT' ||
           activeElement?.tagName === 'TEXTAREA';
  }

  onContactSupport() {
    // Open support dialog or mailto link
    window.location.href = 'mailto:support@healthdata.com?subject=Help Request';
  }
}
```

---

### 3. HelpContentService

**Location**: `apps/clinical-portal/src/app/services/help-content.service.ts`

**Purpose**: Centralize and manage all help content

**Features**:
- ✅ Page-specific help content
- ✅ Structured help sections with types (info, warning, tip, video)
- ✅ Quick links to documentation
- ✅ Easy to maintain and update

**Available Methods**:
```typescript
// Get help for specific pages
helpContent.getMeasureBuilderHelp(): HelpSection[]
helpContent.getPatientsHelp(): HelpSection[]
helpContent.getEvaluationsHelp(): HelpSection[]
helpContent.getReportsHelp(): HelpSection[]
helpContent.getDashboardHelp(): HelpSection[]

// Get common links
helpContent.getQuickLinks(): { text: string; url: string }[]
```

---

## Implementation Examples

### Example 1: Add Help to Form Field

```typescript
// measure-builder.component.html
<div class="form-group">
  <label>
    Measure Category
    <app-help-tooltip
      text="Organize measures by clinical area (Diabetes, Hypertension, etc.)"
      [position]="'right'">
    </app-help-tooltip>
  </label>
  <select [(ngModel)]="measure.category">
    <option value="Diabetes">Diabetes</option>
    <option value="Hypertension">Hypertension</option>
  </select>
</div>
```

### Example 2: Add Help Panel to Page

```typescript
// patients.component.ts
import { Component, HostListener } from '@angular/core';
import { HelpPanelComponent } from '@shared/components/help-panel/help-panel.component';
import { HelpContentService } from '@services/help-content.service';

@Component({
  selector: 'app-patients',
  standalone: true,
  imports: [CommonModule, HelpPanelComponent],
  templateUrl: './patients.component.html'
})
export class PatientsComponent {
  showHelp = false;
  helpSections = this.helpContent.getPatientsHelp();
  quickLinks = this.helpContent.getQuickLinks();

  constructor(private helpContent: HelpContentService) {}

  @HostListener('document:keydown', ['$event'])
  handleKeyboard(event: KeyboardEvent) {
    if (event.key === '?') {
      this.showHelp = !this.showHelp;
    }
  }
}
```

```html
<!-- patients.component.html -->
<div class="page-header">
  <h1>Patients</h1>
  <button (click)="showHelp = true" class="help-button">
    Help <kbd>?</kbd>
  </button>
</div>

<!-- Page content -->

<app-help-panel
  [isOpen]="showHelp"
  [title]="'Patients Help'"
  [sections]="helpSections"
  [quickLinks]="quickLinks"
  (close)="showHelp = false">
</app-help-panel>
```

### Example 3: Custom Help Section

```typescript
// Create custom help content
const customHelp: HelpSection[] = [
  {
    title: 'How to Link Duplicate Patients',
    type: 'tip',
    content: `
      <p><strong>Step 1:</strong> Click "Detect Duplicates" button</p>
      <p><strong>Step 2:</strong> Review the green (master) and orange (duplicate) badges</p>
      <p><strong>Step 3:</strong> Verify the matches are correct</p>
      <p>💡 The system uses an 85% similarity threshold for auto-linking</p>
    `
  },
  {
    title: 'Video Tutorial',
    type: 'video',
    content: `
      <p>Watch our 3-minute tutorial on patient deduplication:</p>
      <iframe width="100%" height="200" src="https://youtube.com/embed/..." frameborder="0"></iframe>
    `,
    link: {
      text: 'Watch full video series',
      url: 'https://docs.example.com/videos'
    }
  },
  {
    title: 'Important: MRN Verification',
    type: 'warning',
    content: `
      <p>⚠️ <strong>Always verify MRN matches before merging records</strong></p>
      <p>Incorrectly merged records can cause:</p>
      <ul>
        <li>Billing errors</li>
        <li>Treatment safety issues</li>
        <li>Compliance violations</li>
      </ul>
    `
  }
];
```

---

## HelpSection Type Reference

```typescript
export interface HelpSection {
  title: string;              // Section heading
  content: string;            // HTML content
  type?: 'info' | 'warning' | 'tip' | 'video';  // Visual style
  icon?: string;              // Custom emoji/icon (optional)
  link?: {                    // Optional "Learn more" link
    text: string;
    url: string;
  };
}
```

**Section Types**:
- `'info'` (ℹ️) - Blue border, informational content
- `'warning'` (⚠️) - Orange border, important warnings
- `'tip'` (💡) - Green border, helpful tips
- `'video'` (🎥) - Purple border, video tutorials

---

## Styling Tips

### Custom Help Button Style

```scss
.help-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;

  kbd {
    padding: 2px 6px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 4px;
    font-size: 12px;
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  }
}
```

### Tooltip Custom Colors

```scss
// Override tooltip colors in component
::ng-deep .help-tooltip .tooltip-content {
  background-color: #3b82f6;  // Blue instead of dark gray
}

::ng-deep .help-tooltip .position-right .tooltip-arrow {
  border-color: transparent #3b82f6 transparent transparent;
}
```

---

## Keyboard Shortcuts

The help system supports these keyboard shortcuts:

| Shortcut | Action |
|----------|--------|
| `?` | Toggle help panel |
| `Esc` | Close help panel |
| `Ctrl + K` | Focus search (when panel open) |
| `Tab` | Navigate sections |
| `Enter` | Expand/collapse section |

---

## Accessibility Features

### ARIA Attributes
- ✅ `role="tooltip"` on tooltip content
- ✅ `aria-label` on help icons
- ✅ `aria-describedby` linking tooltips to content
- ✅ `role="complementary"` on help panel
- ✅ Keyboard focus management

### Screen Reader Support
- All help content is readable by screen readers
- Clear labeling of interactive elements
- Logical tab order through help sections

### Visual Accessibility
- High contrast text (WCAG AA compliant)
- Visible focus indicators
- Color-blind friendly section indicators
- Responsive text sizing

---

## Mobile Considerations

### Responsive Behavior
- **Desktop**: Panel slides in from right (480px wide)
- **Tablet**: Panel slides in from right (full viewport width)
- **Mobile**: Panel becomes full-screen modal

### Touch Interactions
- Tap help icon to show tooltip
- Tap outside to close
- Swipe right to close panel (optional enhancement)

---

## Best Practices

### 1. Help Content Writing

✅ **Do**:
- Write in plain language
- Use bullet points for scannability
- Include examples and screenshots
- Keep sections focused and concise
- Use active voice ("Click the button" vs "The button should be clicked")

❌ **Don't**:
- Use jargon without explanation
- Write long paragraphs
- Assume prior knowledge
- Over-explain simple concepts

### 2. Tooltip Usage

✅ **Do**:
- Use for form field clarification
- Keep text brief (1-2 sentences)
- Position to avoid covering important UI
- Provide "Learn more" links for details

❌ **Don't**:
- Use for complex explanations (use Help Panel instead)
- Put tooltips on every element
- Cover clickable elements
- Use for critical information

### 3. Help Panel Organization

✅ **Do**:
- Group related topics in sections
- Put most important content first
- Use section types (info, warning, tip) appropriately
- Include visual examples when helpful
- Provide search functionality

❌ **Don't**:
- Create too many sections (5-8 is ideal)
- Duplicate content from tooltips
- Nest sections too deeply
- Forget to update help content when features change

---

## Adding New Help Content

### Step 1: Add to HelpContentService

```typescript
// help-content.service.ts
getNewFeatureHelp(): HelpSection[] {
  return [
    {
      title: 'New Feature Overview',
      type: 'info',
      content: `
        <p>Description of the new feature...</p>
      `
    },
    // ... more sections
  ];
}
```

### Step 2: Use in Component

```typescript
// new-feature.component.ts
helpSections = this.helpContent.getNewFeatureHelp();
```

### Step 3: Add Keyboard Shortcut

```typescript
@HostListener('document:keydown', ['$event'])
handleKeyboard(event: KeyboardEvent) {
  if (event.key === '?' && !this.isInputFocused()) {
    event.preventDefault();
    this.showHelp = !this.showHelp;
  }
}
```

---

## Integration Checklist

When adding help to a page:

- [ ] Import `HelpTooltipComponent` for inline help
- [ ] Import `HelpPanelComponent` for detailed help
- [ ] Create help content in `HelpContentService`
- [ ] Add help button to page header
- [ ] Implement keyboard shortcut (`?` key)
- [ ] Test on desktop and mobile
- [ ] Verify accessibility with screen reader
- [ ] Add "Learn more" links to documentation
- [ ] Review help content with stakeholders

---

## Testing

### Manual Testing Checklist

**Tooltips**:
- [ ] Hover shows tooltip
- [ ] Click toggles tooltip
- [ ] Tooltip positioned correctly
- [ ] "Learn more" links work
- [ ] Mobile tap shows tooltip

**Help Panel**:
- [ ] Opens with button click
- [ ] Opens with `?` keyboard shortcut
- [ ] Search filters sections correctly
- [ ] Sections expand/collapse
- [ ] Quick links navigate correctly
- [ ] Contact support button works
- [ ] Closes with Esc key
- [ ] Closes clicking overlay

**Accessibility**:
- [ ] Tab navigation works
- [ ] Screen reader announces content
- [ ] Focus indicators visible
- [ ] ARIA labels present

---

## Future Enhancements

### Planned Features
- [ ] Context-aware help (auto-detect user's current task)
- [ ] Interactive tutorials/walkthroughs
- [ ] Video embedding in help sections
- [ ] User feedback on help usefulness
- [ ] Multi-language support
- [ ] Help content analytics (most viewed topics)
- [ ] AI-powered help chatbot
- [ ] Progressive disclosure (show basic → advanced)

### Video Tutorial Integration

```typescript
{
  title: 'Watch Tutorial: Creating Custom Measures',
  type: 'video',
  content: `
    <div style="position: relative; padding-bottom: 56.25%; height: 0;">
      <iframe
        src="https://www.youtube.com/embed/VIDEO_ID"
        style="position: absolute; top: 0; left: 0; width: 100%; height: 100%;"
        frameborder="0"
        allow="accelerometer; autoplay; encrypted-media; gyroscope"
        allowfullscreen>
      </iframe>
    </div>
  `
}
```

---

## Related Documentation

- **CUSTOM_MEASURES_EXAMPLES.md** - Custom measures usage examples
- **COMPREHENSIVE_FHIR_TEST_DATA.md** - Test data reference
- **PATIENT_DEDUPLICATION_FEATURE.md** - MPI functionality
- **REPORTS_API_DOCUMENTATION.md** - Reports API reference

---

## Support

### Getting Help
- Press `?` on any page to open context-specific help
- Click "Contact Support" in help panel
- Email: support@healthdata.com
- Documentation: https://docs.healthdata.com

### Contributing Help Content
Help content is maintained in `help-content.service.ts`. To suggest improvements:
1. Open an issue describing the improvement
2. Submit PR with updated content
3. Tag with `documentation` label

---

**Last Updated**: 2025-11-19
**Maintainer**: HealthData In Motion Team
**Version**: 1.0.0
