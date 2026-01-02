# Shared Components Library - Quick Reference

Quick reference guide for all shared components. For detailed examples, see `SHARED_COMPONENTS_EXAMPLES.md`.

---

## Import Statement
```typescript
import {
  StatCardComponent,
  EmptyStateComponent,
  ErrorBannerComponent,
  FilterPanelComponent,
  DateRangePickerComponent,
  StatusBadgeComponent,
  PageHeaderComponent,
  LoadingButtonComponent,
  LoadingOverlayComponent
} from '@app/shared/components';
```

---

## Components at a Glance

| Component | Purpose | Key Props | Events |
|-----------|---------|-----------|--------|
| **StatCardComponent** | Display statistics with trends | `title`, `value`, `icon`, `color`, `trend` | - |
| **EmptyStateComponent** | Show empty states | `icon`, `title`, `message`, `actionText` | `action` |
| **ErrorBannerComponent** | Display messages/alerts | `message`, `type`, `retryButton`, `autoDismissTimeout` | `retry`, `dismiss` |
| **FilterPanelComponent** | Config-driven filters | `filters`, `expanded`, `title` | `filterChange` |
| **DateRangePickerComponent** | Date range selection | `startDate`, `endDate`, `showPresets` | `rangeChange` |
| **StatusBadgeComponent** | Status indicators | `status`, `type`, `icon`, `compact` | - |
| **PageHeaderComponent** | Page headers | `title`, `subtitle`, `breadcrumbs`, `showBackButton` | - |
| **LoadingButtonComponent** | Buttons with loading states | `text`, `loading`, `success`, `icon` | `buttonClick` |
| **LoadingOverlayComponent** | Loading overlays | `isLoading`, `message`, `fullscreen` | - |

---

## Quick Start Templates

### Dashboard Header
```html
<app-page-header
  title="Dashboard"
  [breadcrumbs]="[{ label: 'Home', icon: 'home' }]">
  <button mat-raised-button color="primary">Action</button>
</app-page-header>
```

### Stats Grid
```html
<div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 16px;">
  <app-stat-card title="Label" value="1,234" icon="people" color="primary"></app-stat-card>
</div>
```

### Filter Panel
```typescript
filterConfig: FilterConfig[] = [
  { key: 'name', label: 'Name', type: 'text' },
  { key: 'status', label: 'Status', type: 'select', options: [...] }
];
```
```html
<app-filter-panel [filters]="filterConfig" (filterChange)="onFilter($event)"></app-filter-panel>
```

### Empty State
```html
<app-empty-state
  *ngIf="items.length === 0"
  icon="inbox"
  title="No Items"
  message="Get started by adding your first item."
  actionText="Add Item"
  (action)="addItem()">
</app-empty-state>
```

### Error Banner
```html
<app-error-banner
  *ngIf="error"
  [message]="error"
  type="error"
  [retryButton]="true"
  (retry)="retry()"
  (dismiss)="clearError()">
</app-error-banner>
```

### Status Badge
```html
<app-status-badge status="Active" type="success"></app-status-badge>
<app-status-badge status="Pending" type="warning" [compact]="true"></app-status-badge>
```

### Date Range Picker
```html
<app-date-range-picker
  [startDate]="start"
  [endDate]="end"
  (rangeChange)="onDateChange($event)">
</app-date-range-picker>
```

---

## Common Patterns

### Standard Page Layout
```html
<!-- Header -->
<app-page-header title="Page Title" [breadcrumbs]="breadcrumbs">
  <button mat-raised-button color="primary">Action</button>
</app-page-header>

<!-- Error -->
<app-error-banner *ngIf="error" [message]="error" type="error"></app-error-banner>

<!-- Loading -->
<app-loading-overlay [isLoading]="loading"></app-loading-overlay>

<!-- Empty -->
<app-empty-state *ngIf="!loading && items.length === 0"></app-empty-state>

<!-- Content -->
<div *ngIf="!loading && items.length > 0">
  <!-- Your content -->
</div>
```

### Filterable List
```html
<app-filter-panel [filters]="filterConfig" (filterChange)="onFilter($event)"></app-filter-panel>

<mat-table [dataSource]="filteredItems">
  <ng-container matColumnDef="status">
    <th mat-header-cell *matHeaderCellDef>Status</th>
    <td mat-cell *matCellDef="let item">
      <app-status-badge [status]="item.status" [type]="getType(item.status)"></app-status-badge>
    </td>
  </ng-container>
</mat-table>
```

### Dashboard Stats
```html
<div class="stats-grid">
  <app-stat-card
    *ngFor="let stat of statistics"
    [title]="stat.label"
    [value]="stat.value"
    [icon]="stat.icon"
    [color]="stat.color"
    [trend]="stat.trend">
  </app-stat-card>
</div>
```

---

## Color Types

### StatCard Colors
- `primary` - Blue
- `accent` - Pink
- `warn` - Red
- `success` - Green

### ErrorBanner Types
- `error` - Red (critical errors)
- `warning` - Orange (warnings)
- `info` - Blue (information)
- `success` - Green (success messages)

### StatusBadge Types
- `success` - Green (active, completed)
- `warning` - Orange (pending, attention)
- `error` - Red (failed, inactive)
- `info` - Blue (in progress, informational)
- `neutral` - Gray (default, draft)

---

## TypeScript Interfaces

### FilterConfig
```typescript
interface FilterConfig {
  key: string;
  label: string;
  type: 'text' | 'select' | 'date-range' | 'number-range';
  options?: Array<{ label: string; value: any }>;
  placeholder?: string;
  defaultValue?: any;
}
```

### FilterValues
```typescript
interface FilterValues {
  [key: string]: any;
}
```

### DateRange
```typescript
interface DateRange {
  start: Date | null;
  end: Date | null;
}
```

### Breadcrumb
```typescript
interface Breadcrumb {
  label: string;
  route?: string;
  icon?: string;
}
```

---

## Accessibility Checklist

All components include:
- ✅ ARIA labels on interactive elements
- ✅ Keyboard navigation support
- ✅ Focus indicators
- ✅ Screen reader friendly
- ✅ Semantic HTML
- ✅ Role attributes
- ✅ aria-live regions for dynamic content

---

## Responsive Breakpoints

All components are responsive with these breakpoints:
- **Mobile:** < 480px
- **Tablet (Portrait):** 480px - 768px
- **Tablet (Landscape):** 768px - 1024px
- **Desktop:** > 1024px

---

## Testing

Run component tests:
```bash
npm test -- stat-card.component.spec.ts
npm test -- empty-state.component.spec.ts
npm test -- error-banner.component.spec.ts
npm test -- filter-panel.component.spec.ts
npm test -- date-range-picker.component.spec.ts
npm test -- status-badge.component.spec.ts
npm test -- page-header.component.spec.ts
```

---

## File Locations

```
apps/clinical-portal/src/app/shared/components/
├── stat-card/
├── empty-state/
├── error-banner/
├── filter-panel/
├── date-range-picker/
├── status-badge/
├── page-header/
├── loading-button/
├── loading-overlay/
└── index.ts
```

---

## Documentation Files

1. **PHASE_4_SHARED_COMPONENTS_LIBRARY.md** - Complete implementation guide
2. **SHARED_COMPONENTS_EXAMPLES.md** - Detailed usage examples
3. **SHARED_COMPONENTS_QUICK_REFERENCE.md** - This quick reference

---

## Support

For detailed examples and advanced usage, see:
- Component JSDoc comments in TypeScript files
- Test files (*.spec.ts) for usage patterns
- SHARED_COMPONENTS_EXAMPLES.md for real-world scenarios

---

**Last Updated:** November 18, 2024
**Version:** 1.0.0
**Total Components:** 9 (7 new + 2 existing)
**Total Lines:** 5,128 lines of code
