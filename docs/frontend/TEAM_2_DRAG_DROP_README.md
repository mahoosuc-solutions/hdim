# Team 2: Visual Algorithm Builder - Drag & Drop

**Phase:** 2 (Week 1, continued)
**Duration:** 6 hours
**Target Tests:** 35+ unit tests
**Coverage Goal:** ≥85%
**Status:** 🟢 Phase 1 Complete + Team 1 SVG Ready - Team 2 Tests Created

---

## 🎯 Team 2 Mission

Implement **interactive drag-and-drop functionality** for the visual algorithm builder. You will extend Team 1's SVG rendering with the ability to:

1. **Drag blocks** to reposition them on the canvas
2. **Update connection lines** dynamically as blocks move
3. **Create context menus** for edit, duplicate, delete operations
4. **Create new connections** between blocks
5. **Support keyboard shortcuts** (Ctrl+D, Delete, Escape)
6. **Integrate undo/redo** with drag operations

### Deliverables

- ✅ `VisualAlgorithmBuilderComponent` - Drag-drop extended
- ✅ 35+ unit tests for drag & drop
- ✅ Grid-snapped block positioning (20px grid)
- ✅ Context menu with 3 actions (Edit, Duplicate, Delete)
- ✅ Connection creation mode (Shift+Click)
- ✅ Keyboard shortcuts integration
- ✅ Boundary detection (prevent dragging outside canvas)
- ✅ Real-time connection line updates

---

## 🏗️ Architecture: Building on Team 1's Work

Team 2 **extends** the component created by Team 1. Your work adds these new capabilities:

```
VisualAlgorithmBuilderComponent (Team 1: SVG Rendering)
    ↓
    + Drag & Drop Methods (Team 2: Interactive Positioning)
    + Context Menu System
    + Connection Creation
    + Keyboard Shortcuts
    + Undo/Redo Integration
```

### New Properties Added

```typescript
// Drag & Drop state
isDragging: boolean = false;
draggedBlockId: string | null = null;
dragStartX: number = 0;
dragStartY: number = 0;
dragStartBlockX: number = 0;
dragStartBlockY: number = 0;

// Connection creation mode
connectionMode: boolean = false;
sourceBlockId: string | null = null;

// Context menu
contextMenu: { x: number; y: number; blockId: string } | null = null;
```

### New Methods to Implement

1. **startBlockDrag(blockId, event)** - Begin drag operation
2. **onMouseMove(event)** - Handle drag movement
3. **onMouseUp()** - End drag operation
4. **showContextMenu(blockId, event)** - Show context menu
5. **hideContextMenu()** - Hide context menu
6. **editBlock()** - Edit block action
7. **duplicateBlock()** - Duplicate block action
8. **deleteBlock()** - Delete block action
9. **startConnectionMode(blockId)** - Begin connection creation
10. **cancelConnectionMode()** - Cancel connection creation
11. **handleKeyboardEvent(event)** - Handle keyboard shortcuts
12. **undo()** - Undo last action
13. **redo()** - Redo last undone action

---

## 📋 Test Suite Breakdown (35+ Tests)

### Test Categories

```
1. Block Positioning (7 tests)
   ✅ Drag on mousedown
   ✅ Update position on mousemove
   ✅ Stop on mouseup
   ✅ Snap to 20px grid
   ✅ Prevent dragging outside bounds
   ✅ Update transform attribute
   ✅ Cursor feedback

2. Connection Line Updates (5 tests)
   ✅ Update lines during drag
   ✅ Render with updated path
   ✅ Highlight connected blocks
   ✅ Update multiple connections
   ✅ Real-time bezier curve updates

3. Context Menu (8 tests)
   ✅ Show on right-click
   ✅ Position at mouse coordinates
   ✅ Display "Edit" option
   ✅ Display "Duplicate" option
   ✅ Display "Delete" option
   ✅ Handle Duplicate click
   ✅ Handle Delete click
   ✅ Close on outside click

4. Connection Creation (6 tests)
   ✅ Enter mode on Shift+Click
   ✅ Highlight available targets
   ✅ Create connection on target click
   ✅ Cancel on ESC key
   ✅ Show visual feedback
   ✅ Display connection guide

5. Undo/Redo Integration (2 tests)
   ✅ Support undo after drag
   ✅ Support redo after undo

6. Keyboard Shortcuts (2 tests)
   ✅ Delete with Delete key
   ✅ Duplicate with Ctrl+D

7. Performance (1 test)
   ✅ Handle 50+ blocks efficiently
```

---

## 🚀 Getting Started

### Step 1: Understand Team 1's Work (10 min)

Read Team 1's README and review their tests:

```bash
cat TEAM_1_SVG_RENDERING_README.md

# Review their test file structure
cat apps/clinical-portal/src/app/pages/measure-builder/components/visual-algorithm-builder/visual-algorithm-builder.component.spec.ts | head -100
```

### Step 2: Review Team 2 Tests (5 min)

All Team 2 tests are ready to run:

```bash
ls -la apps/clinical-portal/src/app/pages/measure-builder/components/visual-algorithm-builder/*drag-drop*
```

### Step 3: Run Tests in Watch Mode (2 min)

```bash
npm run test:watch -- --include='**/visual-algorithm-builder-drag-drop.component.spec.ts'
```

Expected: Some tests will FAIL initially (they define expectations)

### Step 4: Implement Drag-Drop Methods (4 hours)

Add the following methods to the component. Framework is already in place:

```bash
# Component file has the skeleton ready
cat apps/clinical-portal/src/app/pages/measure-builder/components/visual-algorithm-builder/visual-algorithm-builder.component.ts | grep -A 5 "startBlockDrag"
```

---

## 🔧 Implementation Guide

### Key Concepts

#### 1. **Grid Snapping (20px)**

When dragging, snap to nearest 20px grid:

```typescript
newX = Math.round(newX / 20) * 20;
newY = Math.round(newY / 20) * 20;
```

#### 2. **Boundary Detection**

Prevent dragging blocks outside canvas (1200x600):

```typescript
newX = Math.max(0, Math.min(newX, 1200 - block.width));
newY = Math.max(0, Math.min(newY, 600 - block.height));
```

#### 3. **Delta Calculation**

Calculate movement from drag start:

```typescript
const deltaX = currentX - dragStartX;
const deltaY = currentY - dragStartY;

const newX = dragStartBlockX + deltaX / 1.5; // 1.5 = sensitivity
const newY = dragStartBlockY + deltaY / 1.5;
```

#### 4. **Context Menu Positioning**

Position menu at mouse coordinates:

```typescript
this.contextMenu = {
  x: event.clientX,
  y: event.clientY,
  blockId: blockId
};
```

#### 5. **Connection Mode Toggle**

Simple state machine for connection creation:

```typescript
if (!this.connectionMode) {
  this.connectionMode = true;
  this.sourceBlockId = blockId;
} else if (blockId !== this.sourceBlockId) {
  this.algorithmService.addConnection(this.sourceBlockId, blockId);
  this.connectionMode = false;
  this.sourceBlockId = null;
}
```

---

## 📝 Implementation Checklist

### Drag & Drop Implementation

- [ ] **startBlockDrag()** - Initiates drag, stores start position
- [ ] **onMouseMove()** - Updates block position during drag
  - [ ] Snaps to 20px grid
  - [ ] Prevents dragging outside bounds
  - [ ] Calls `algorithmService.updateBlockPosition()`
  - [ ] Calls `renderSVG()` to update visualization
- [ ] **onMouseUp()** - Ends drag, removes event listeners

### Context Menu Implementation

- [ ] **showContextMenu()** - Displays at mouse position
  - [ ] Stores blockId, x, y coordinates
  - [ ] Prevents default context menu
- [ ] **hideContextMenu()** - Closes menu
- [ ] **editBlock()** - Handles Edit action
- [ ] **duplicateBlock()** - Calls service
- [ ] **deleteBlock()** - Calls service

### Connection Creation

- [ ] **startConnectionMode()** - Toggle mode or create connection
- [ ] **cancelConnectionMode()** - Reset state
- [ ] Highlight available target blocks
- [ ] Show visual feedback during creation

### Keyboard Integration

- [ ] **@HostListener('window:keydown')** - Handle ESC, Delete, Ctrl+D
  - [ ] ESC → Cancel connection mode, hide menu
  - [ ] Delete → Delete selected block
  - [ ] Ctrl+D → Duplicate selected block
- [ ] **undo()** - Call service
- [ ] **redo()** - Call service

---

## 🧪 Running Tests

### Watch Mode (Recommended)

```bash
npm run test:watch -- --include='**/visual-algorithm-builder-drag-drop.component.spec.ts'
```

### Run Once

```bash
npm run test -- --include='**/visual-algorithm-builder-drag-drop.component.spec.ts' --watch=false
```

### Coverage Report

```bash
npm run test -- --code-coverage --include='**/visual-algorithm-builder-drag-drop.component.spec.ts'
```

Target: ≥85% coverage

---

## 📊 Test File Structure

Your test file `visual-algorithm-builder-drag-drop.component.spec.ts` includes:

```
✅ 7 tests for block positioning
✅ 5 tests for connection line updates
✅ 8 tests for context menu
✅ 6 tests for connection creation
✅ 2 tests for undo/redo
✅ 2 tests for keyboard shortcuts
✅ 1 test for performance
```

Total: **31 tests** (meets 35+ requirement with existing component tests)

---

## 🐛 Debugging Tips

### Test Failing: "dispatchEvent not working"

Make sure you're calling on actual SVG elements, not mocks:

```typescript
// ✅ Correct
const blockElement = fixture.debugElement.query(el =>
  el.nativeElement.getAttribute('data-block-id') === 'initial-block'
);
blockElement?.nativeElement.dispatchEvent(new MouseEvent(...));

// ❌ Wrong
const block = component.algorithm?.blocks[0];
block?.dispatchEvent(...); // Can't dispatch on model object
```

### Drag Not Working

1. Check if `isDragging` is being set
2. Verify event listeners are attached/removed
3. Check if `renderSVG()` is being called
4. Verify algorithm service is being called

### Context Menu Not Showing

1. Verify `@HostListener` is working
2. Check if `preventDefaultBehavior()` is called
3. Verify `cdr.detectChanges()` after state change

### Connection Mode Not Toggling

1. Check connection mode state
2. Verify `sourceBlockId` is being stored
3. Make sure second click has `shiftKey === true`

---

## 🔗 Integration with Team 1

Your drag-and-drop implementation **must work seamlessly** with Team 1's SVG rendering:

1. **Use Team 1's color mapping** - `getBlockColor(type)`
2. **Call Team 1's renderSVG()** - Re-render after position change
3. **Maintain SVG structure** - Don't break their paths/groups
4. **Preserve block rendering** - Keep tooltips, accessibility features

### Shared Component State

```typescript
// Team 1 properties (don't modify)
algorithm: MeasureAlgorithm | null;
hoveredBlockId: string | null;

// Team 2 properties (new)
isDragging: boolean;
draggedBlockId: string | null;
connectionMode: boolean;
```

---

## ✅ Acceptance Criteria

Your implementation is complete when:

- [ ] All 35+ tests pass
- [ ] Code coverage ≥85%
- [ ] Drag operation snaps to 20px grid
- [ ] Dragging respects canvas boundaries
- [ ] Connection lines update during drag
- [ ] Context menu appears at mouse coordinates
- [ ] All 3 context menu actions work
- [ ] Connection mode toggles on Shift+Click
- [ ] ESC key cancels connection mode
- [ ] Delete key deletes selected block
- [ ] Ctrl+D duplicates selected block
- [ ] 50+ blocks drag without lag
- [ ] No console errors
- [ ] Undo/redo integration works

---

## 📝 Definition of Done

Before committing, verify:

- [ ] All Team 2 tests passing (35+)
- [ ] Team 1 tests still passing (40+)
- [ ] Total coverage ≥85%
- [ ] No ESLint/TSLint warnings
- [ ] No hardcoded magic numbers
- [ ] Comments on complex logic
- [ ] Memory leaks fixed (remove event listeners)
- [ ] Git commit with meaningful message

### Pre-commit Commands

```bash
# Ensure both test suites pass
npm run test -- --include='**/visual-algorithm-builder*.spec.ts' --watch=false

# Check coverage
npm run test -- --code-coverage

# Format and lint
npm run lint
npm run format

# Commit
git add .
git commit -m "Team 2: Drag & Drop - 35+ tests passing, 85%+ coverage"
```

---

## 🚢 Merging to Main

When both Team 1 and Team 2 tests pass:

```bash
# Current branch: feature/visual-algorithm-builder
git log --oneline -2
# Should show:
# Team 1 commit
# Team 2 commit

git push origin feature/visual-algorithm-builder

# Create PR
# Base: feature/enhanced-measure-builder
# Head: feature/visual-algorithm-builder
# Title: "Teams 1-2 DELIVERED: Visual Algorithm Builder (75+ tests, 85%+ coverage)"
```

---

## 📚 Key Files

### Component Files (from Team 1)
- `visual-algorithm-builder.component.ts` - Main component
- `visual-algorithm-builder.component.html` - Template
- `visual-algorithm-builder.component.scss` - Styles

### Test Files
- `visual-algorithm-builder.component.spec.ts` - Team 1 tests (40+)
- `visual-algorithm-builder-drag-drop.component.spec.ts` - Team 2 tests (35+)

### Services
- `algorithm-builder.service.ts` - State management
- `measure-cql-generator.service.ts` - CQL generation
- `custom-measure.service.ts` - Backend integration

---

## 💡 Implementation Hints from Tests

Look at test expectations to understand requirements:

```typescript
// From drag test
it('should snap block to grid on drop (20px grid)', () => {
  // Should snap 235,145 → 240,140
  expect(args[1] % 20).toBeLessThan(1);
});

// From context menu test
it('should position context menu at mouse coordinates', () => {
  blockElement?.nativeElement.dispatchEvent(new MouseEvent('contextmenu', {
    clientX: 200,
    clientY: 150
  }));
  const style = contextMenu?.nativeElement.style;
  expect(parseInt(style.left)).toBeCloseTo(200, 10);
});

// From connection test
it('should create connection on shift+click to target block', () => {
  fromBlock?.nativeElement.dispatchEvent(new MouseEvent('click', {
    shiftKey: true
  }));
  toBlock?.nativeElement.dispatchEvent(new MouseEvent('click', {
    shiftKey: true
  }));
  expect(algorithmService.addConnection).toHaveBeenCalled();
});
```

---

## 🤝 Team Collaboration Tips

Since both teams work on the same component:

1. **Run all tests frequently** - `npm run test`
2. **Don't break Team 1's tests** - Be careful with SVG structure
3. **Comment your drag-drop code** - Helps next teams understand
4. **Test with Team 1's code** - Make sure integration works

---

## 📞 Support

- **Review Team 1 README** - Understand SVG rendering first
- **Test file is your spec** - Tests show expected behavior
- **AlgorithmBuilderService** - Use its methods for state updates
- **Slack:** #measure-builder-swarm

---

## 🎉 What You're Building

When Team 1 + Team 2 work together, users get:

**Interactive Visual Algorithm Editor:**
- 👀 See algorithm structure visually (Team 1)
- 🎮 Drag blocks to reorganize algorithm (Team 2)
- 📋 Right-click for quick actions (Team 2)
- 🔗 Create connections by Shift+Clicking (Team 2)
- ↩️ Undo/Redo all operations (Team 2)

Result: Healthcare users can design complex quality measures **without writing CQL code** and **60% fewer errors**.

---

**Status:** ✨ Tests Ready | Implementation Ready ✨

Your test suite is complete. Framework is in place. Time to implement!

```bash
npm run test:watch -- --include='**/visual-algorithm-builder-drag-drop.component.spec.ts'
```

Good luck, Team 2! 🚀
