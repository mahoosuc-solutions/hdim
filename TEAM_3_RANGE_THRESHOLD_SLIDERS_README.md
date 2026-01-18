# Team 3: Range & Threshold Sliders

**Phase:** 3 (Week 2)
**Duration:** 5 hours
**Target Tests:** 30+ unit tests
**Coverage Goal:** ≥85%
**Status:** ✅ Phase 2 Complete (SVG + Drag) | Phase 3 Ready

---

## 🎯 Team 3 Mission

Implement **Range and Threshold Sliders** for measure algorithm configuration. Enable users to specify:

1. **Range Sliders** (dual values) - Age ranges, BMI bounds, score ranges
2. **Threshold Sliders** (single value) - HbA1c targets, blood pressure limits, cholesterol thresholds

### Deliverables

- ✅ `RangeThresholdSliderComponent` - Dual-slider/single-slider component
- ✅ 30+ unit tests covering both slider types
- ✅ Grid-aligned dual-slider inputs (range)
- ✅ Single-slider with warning/critical zones (threshold)
- ✅ Preset values for common thresholds
- ✅ Real-time CQL generation
- ✅ Visual feedback (color coding, indicators)
- ✅ Accessibility support

---

## 🏗️ Architecture

### Slider Configuration Union Type

```typescript
// From measure-builder.model.ts
type SliderConfig = RangeSliderConfig | ThresholdSliderConfig

// Range Slider: Two values
RangeSliderConfig {
  type: 'range'
  currentMin: number      // e.g., 40
  currentMax: number      // e.g., 75
  minValue: number        // e.g., 18
  maxValue: number        // e.g., 120
  step: number            // e.g., 1
  unit: string            // e.g., 'years'
}

// Threshold Slider: Single value with zones
ThresholdSliderConfig {
  type: 'threshold'
  currentValue: number             // e.g., 9.0
  minValue: number                 // e.g., 4.0
  maxValue: number                 // e.g., 13.0
  warningThreshold: number         // e.g., 10.0
  criticalThreshold: number        // e.g., 11.0
  preset: string                   // e.g., 'hba1c_control'
  step: number                     // e.g., 0.1
  unit: string                     // e.g., '%'
}
```

### Component Methods

**Range Slider Methods:**
- `onRangeMinChange()` - Handle min slider input
- `onRangeMaxChange()` - Handle max slider input
- `getTrackFillPercentage()` - Calculate track fill position/width

**Threshold Slider Methods:**
- `onThresholdChange()` - Handle slider input
- `applyPreset()` - Apply preset value
- `isInWarningZone()` - Check if value exceeds warning
- `isInCriticalZone()` - Check if value exceeds critical
- `getWarningIndicatorPosition()` - Visual indicator positioning
- `getCriticalIndicatorPosition()` - Visual indicator positioning

**Shared Methods:**
- `generateCQL()` - Generate CQL for current config
- `generateRangeCQL()` - CQL for range slider
- `generateThresholdCQL()` - CQL for threshold slider
- `isRangeSlider()` - Type guard
- `isThresholdSlider()` - Type guard

---

## 📋 Test Suite (30+ Tests)

### Range Slider Tests (13 tests)
1. **Basic Rendering (5 tests)**
   - ✅ Create component
   - ✅ Render label and description
   - ✅ Render two sliders
   - ✅ Set min/max attributes correctly
   - ✅ Display values with unit

2. **Dual Value Interaction (8 tests)**
   - ✅ Update min value on change
   - ✅ Update max value on change
   - ✅ Prevent min > max
   - ✅ Prevent max < min
   - ✅ Emit change event
   - ✅ Real-time display update
   - ✅ Track fill updates
   - ✅ Boundary enforcement

### Threshold Slider Tests (12 tests)
3. **Basic Rendering (4 tests)**
   - ✅ Create component
   - ✅ Render label and description
   - ✅ Render single slider
   - ✅ Display current value with unit

4. **Warning/Critical Zones (5 tests)**
   - ✅ Render warning indicator
   - ✅ Render critical indicator
   - ✅ Position indicators correctly
   - ✅ Show warning tooltip
   - ✅ Show critical tooltip

5. **Presets (3 tests)**
   - ✅ Render preset buttons
   - ✅ Apply preset on click
   - ✅ Highlight active preset

### Integration Tests (5+ tests)
6. **CQL Generation**
   - ✅ Generate range CQL
   - ✅ Generate threshold CQL
   - ✅ Format CQL correctly

7. **Accessibility**
   - ✅ ARIA labels on sliders
   - ✅ ARIA value attributes

---

## 🚀 Getting Started

### Step 1: Verify Worktree

```bash
cd /home/webemo-aaron/projects/measure-builder-sliders
git status
# Should be on: feature/interactive-sliders
```

### Step 2: Install Dependencies

```bash
npm install --legacy-peer-deps
```

### Step 3: Review Files

All files created for Team 3:
```bash
ls apps/clinical-portal/src/app/pages/measure-builder/components/measure-config-slider/
# range-threshold-slider.component.ts (327 lines)
# range-threshold-slider.component.html (92 lines)
# range-threshold-slider.component.scss (320 lines)
# range-threshold-slider.component.spec.ts (580+ lines, 30+ tests)
```

### Step 4: Run Tests

```bash
npm run test:watch -- --include='**/range-threshold-slider.component.spec.ts'
```

All 30+ tests should be defined and ready to pass!

---

## 🧪 Test Categories Explained

### Range Slider: Dual Value Testing

Tests verify that both sliders work independently:

```typescript
// Min slider test
it('should update min value when min slider changes', () => {
  const minSlider = fixture.debugElement.query(...);
  minSlider?.nativeElement.value = '50';
  minSlider?.nativeElement.dispatchEvent(new Event('input'));

  expect(component.config.currentMin).toBe(50);
});

// Cross-validation
it('should prevent min value from exceeding max value', () => {
  // Try to set min=85 when max=75
  // Should maintain min <= max invariant
});
```

### Threshold Slider: Warning/Critical Zones

Tests verify visual feedback at different value zones:

```typescript
// Zone detection
it('should change slider appearance when value exceeds warning', () => {
  slider.value = '10.5'; // > warning of 10.0

  expect(slider.classList.contains('warning-level')).toBeTruthy();
});

// Indicator positioning
it('should position warning indicator at correct percentage', () => {
  const position = ((warningThreshold - minValue) / range) * 100;
  const indicator = fixture.debugElement.query(...);

  expect(parseInt(indicator.style.left)).toBeCloseTo(position, 5);
});
```

---

## 🔧 Key Implementation Details

### 1. Dual Slider Logic (Range)

Keep min and max synchronized:

```typescript
onRangeMinChange(event: Event): void {
  const newMin = parseInt(event.target.value, 10);
  const config = this.getRangeConfig();

  // Enforce min <= max invariant
  if (newMin <= config.currentMax) {
    config.currentMin = newMin;
    this.emitChange();
  }
}
```

### 2. Warning/Critical Zones (Threshold)

Check value against thresholds:

```typescript
isInWarningZone(): boolean {
  const config = this.getThresholdConfig();
  return (config.currentValue > config.warningThreshold &&
          config.currentValue <= config.criticalThreshold);
}
```

### 3. CQL Generation

Convert slider values to CQL:

```typescript
private generateRangeCQL(): string {
  const config = this.getRangeConfig();
  return `age >= ${config.currentMin} and age <= ${config.currentMax}`;
}

private generateThresholdCQL(): string {
  const config = this.getThresholdConfig();
  return `hba1c <= ${config.currentValue} %`;
}
```

### 4. Preset Application

Store and apply preset values:

```typescript
private readonly presets = {
  'hba1c_control': { value: 7.0, label: 'HbA1c Control (≤7%)' },
  'hba1c_target': { value: 8.0, label: 'HbA1c Target (≤8%)' },
  // ... more presets
};

applyPreset(presetKey: string): void {
  const preset = this.presets[presetKey];
  const config = this.getThresholdConfig();
  config.currentValue = preset.value;
  this.emitChange();
}
```

---

## 📊 Visual Design

### Range Slider
```
Label: "Age Range"
Description: "Patient age range (years)"

Min: [◉────────────────────] Max
40                                75

Track Fill: [====green====]

Display: 40 - 75 years
```

### Threshold Slider
```
Label: "HbA1c Threshold"
Description: "Hemoglobin A1c target"

[◉─────────────────────────]
     ↑warning   ↑critical
     10%        11%

Current: 9.0 %

⚠️  Presets: [HbA1c Control] [HbA1c Target] [Custom]
```

---

## ✅ Acceptance Criteria

Your implementation is complete when:

- [ ] All 30+ tests pass
- [ ] Code coverage ≥85%
- [ ] Range sliders update dual values correctly
- [ ] Min never exceeds max (and vice versa)
- [ ] Threshold sliders detect warning/critical zones
- [ ] Visual indicators positioned correctly
- [ ] Presets apply correctly
- [ ] CQL generated for both slider types
- [ ] ARIA labels present
- [ ] No console errors
- [ ] Responsive design works (mobile/tablet)

---

## 📝 Definition of Done

Before committing:

- [ ] All 30+ tests passing
- [ ] Code coverage ≥85%
- [ ] No ESLint warnings
- [ ] Comments on complex logic
- [ ] Accessibility verified
- [ ] Performance tested (rapid changes < 100ms)
- [ ] Git commit with message

### Pre-commit Commands

```bash
# Run all tests
npm run test -- --include='**/range-threshold-slider.component.spec.ts' --watch=false

# Check coverage
npm run test -- --code-coverage

# Format and lint
npm run lint && npm run format

# Commit
git add .
git commit -m "Team 3: Range & Threshold Sliders - 30+ tests, 85%+ coverage"
```

---

## 🚢 Integration Notes

### With Team 1 (SVG Rendering)

Sliders are displayed **right of the canvas**:
- SVG canvas takes 60% of width
- Slider configuration panel takes 40%
- Responsive stacking on mobile

### With Team 2 (Drag-Drop)

Sliders update when:
- Block dragged to new position
- Block type changed
- Connection updated

### With AlgorithmBuilderService

Sliders emit changes → service updates state → CQL regenerated

```typescript
// Component
valueChanged.emit({ id: 'age-range', currentMin: 40, currentMax: 75 });

// Parent component listens and calls service
algorithmService.updateSliderConfig(config);
```

---

## 💡 Implementation Hints from Tests

Look at test expectations to understand behavior:

```typescript
// Range validation
expect(component.config.currentMin).toBeLessThanOrEqual(component.config.currentMax);

// Warning zone detection
it('should change slider appearance when value exceeds warning', () => {
  component.config.currentValue = 10.5; // > warning of 10.0
  expect(slider.classList.contains('warning-level')).toBeTruthy();
});

// CQL format
const cql = component.generateCQL();
expect(cql).toContain('>=');
expect(cql).toContain('<=');
```

---

## 🎯 Success Metrics

**Week 2 Phase 3 Goals:**
- Team 3 (this team): 30+ tests, 85%+ coverage ← YOU ARE HERE
- Team 4 (next): 30+ tests, 85%+ coverage
- Total: 60+ new tests, 85%+ coverage on new code

---

## 📞 Support

- **Questions:** Review test file for expected behavior
- **Stuck:** Check generateCQL() - tests show CQL format
- **Debugging:** Use console.log with slider values
- **Slack:** #measure-builder-swarm

---

## 🎉 What You're Building

When Team 3 completes sliders:

**Measure Creation becomes interactive:**
- Choose age range visually (not typing "18-65")
- See warning zone for HbA1c (orange at >10%)
- See critical zone for HbA1c (red at >11%)
- One-click presets for common targets
- Real-time CQL generation
- Visual feedback at every step

**Result:** Reduce measure creation time by 30 minutes → 15 minutes

---

**Status:** ✨ Ready for Development ✨

All files created. Tests await implementation.

```bash
npm run test:watch -- --include='**/range-threshold-slider.component.spec.ts'
```

Good luck, Team 3! 🚀
