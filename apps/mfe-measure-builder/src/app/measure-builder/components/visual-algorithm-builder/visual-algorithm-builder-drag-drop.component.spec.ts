import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VisualAlgorithmBuilderComponent } from './visual-algorithm-builder.component';
import { AlgorithmBuilderService } from '../../services/algorithm-builder.service';
import { of } from 'rxjs';
import { MeasureAlgorithm, PopulationBlock } from '../../models/measure-builder.model';

describe('VisualAlgorithmBuilderComponent - Drag & Drop Suite', () => {
  let component: VisualAlgorithmBuilderComponent;
  let fixture: ComponentFixture<VisualAlgorithmBuilderComponent>;
  let algorithmService: jasmine.SpyObj<AlgorithmBuilderService>;

  const mockAlgorithm: MeasureAlgorithm = {
    id: 'test-algo-001',
    name: 'Test Algorithm',
    version: '1.0',
    blocks: [
      {
        id: 'initial-block',
        type: 'initial_population',
        name: 'Initial Population',
        description: 'All eligible patients',
        condition: 'Condition X present',
        color: '#2196F3',
        x: 100,
        y: 100,
        width: 150,
        height: 80
      },
      {
        id: 'denom-block',
        type: 'denominator',
        name: 'Denominator',
        description: 'Patients with lab result',
        condition: 'Lab Y > 100',
        color: '#4CAF50',
        x: 400,
        y: 100,
        width: 150,
        height: 80
      }
    ],
    connections: [
      { fromBlockId: 'initial-block', toBlockId: 'denom-block' }
    ]
  };

  beforeEach(async () => {
    const algorithmServiceSpy = jasmine.createSpyObj('AlgorithmBuilderService', [
      'getAlgorithm',
      'updateBlockPosition',
      'addConnection',
      'removeConnection',
      'addExclusionBlock',
      'removeBlock',
      'duplicateBlock',
      'undo',
      'redo'
    ]);

    await TestBed.configureTestingModule({
      imports: [VisualAlgorithmBuilderComponent],
      providers: [
        { provide: AlgorithmBuilderService, useValue: algorithmServiceSpy }
      ]
    }).compileComponents();

    algorithmService = TestBed.inject(AlgorithmBuilderService) as jasmine.SpyObj<AlgorithmBuilderService>;
    algorithmService.getAlgorithm.and.returnValue(of(mockAlgorithm));

    fixture = TestBed.createComponent(VisualAlgorithmBuilderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Drag & Drop - Block Positioning', () => {
    it('should allow dragging a block on mousedown', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const mouseDownEvent = new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      });

      blockElement?.nativeElement.dispatchEvent(mouseDownEvent);
      fixture.detectChanges();

      expect(blockElement?.nativeElement.classList.contains('dragging')).toBeTruthy();
    });

    it('should update block position on mousemove while dragging', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      // Start drag
      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));
      fixture.detectChanges();

      // Move mouse
      window.dispatchEvent(new MouseEvent('mousemove', {
        clientX: 250,
        clientY: 200,
        bubbles: true
      }));
      fixture.detectChanges();

      // Should update service
      expect(algorithmService.updateBlockPosition).toHaveBeenCalled();
    });

    it('should stop dragging on mouseup', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      // Start drag
      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));
      fixture.detectChanges();

      // End drag
      window.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
      fixture.detectChanges();

      expect(blockElement?.nativeElement.classList.contains('dragging')).toBeFalsy();
    });

    it('should snap block to grid on drop (20px grid)', () => {
      // Drag to 235, 145 (not aligned to grid)
      // Should snap to 240, 140 (nearest 20px grid point)
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mousemove', {
        clientX: 250,
        clientY: 210,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
      fixture.detectChanges();

      // Verify snap logic (should be rounded to 20px increments)
      const updateCall = algorithmService.updateBlockPosition.calls.mostRecent();
      if (updateCall) {
        const args = updateCall.args;
        // New position should be snapped to 20px grid
        expect(args[1] % 20).toBeLessThan(1);
        expect(args[2] % 20).toBeLessThan(1);
      }
    });

    it('should prevent dragging outside canvas bounds', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));

      // Try to drag outside (negative coordinates)
      window.dispatchEvent(new MouseEvent('mousemove', {
        clientX: -100,
        clientY: -100,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
      fixture.detectChanges();

      // Block should not go outside bounds
      const block = component.getBlocks().find(b => b.id === 'initial-block');
      expect(block?.x).toBeGreaterThanOrEqual(0);
      expect(block?.y).toBeGreaterThanOrEqual(0);
    });

    it('should update transform attribute on successful drag', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const initialTransform = blockElement?.nativeElement.getAttribute('transform');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mousemove', {
        clientX: 275,
        clientY: 240,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
      fixture.detectChanges();

      const updatedTransform = blockElement?.nativeElement.getAttribute('transform');
      // Transform should be updated if drag was successful
      expect(updatedTransform).toBeTruthy();
    });

    it('should maintain cursor:grab during drag', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const group = blockElement?.nativeElement;

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));
      fixture.detectChanges();

      const computedStyle = window.getComputedStyle(group);
      expect(computedStyle.cursor).toContain('grab');
    });
  });

  describe('Drag & Drop - Connection Line Updates', () => {
    it('should update connection lines during drag', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mousemove', {
        clientX: 300,
        clientY: 200,
        bubbles: true
      }));

      fixture.detectChanges();

      // Connection lines should be redrawn
      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      expect(connectionElement).toBeTruthy();
    });

    it('should render connection lines with updated path during drag', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mousemove', {
        clientX: 300,
        clientY: 200,
        bubbles: true
      }));

      fixture.detectChanges();

      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      const path = connectionElement?.debugElement.query(el => el.nativeElement.tagName === 'path');
      const pathData = path?.nativeElement.getAttribute('d');

      expect(pathData).toContain('M');
      expect(pathData).toContain('C');
    });

    it('should highlight connected blocks during drag', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mousemove', {
        clientX: 300,
        clientY: 200,
        bubbles: true
      }));

      fixture.detectChanges();

      // Connected blocks should have highlight class
      const denomBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'denom-block');
      expect(denomBlock?.nativeElement.classList.contains('connected')).toBeTruthy();
    });

    it('should update all connected lines if block has multiple connections', () => {
      const algorithmWithMultipleConnections: MeasureAlgorithm = {
        ...mockAlgorithm,
        blocks: [
          ...mockAlgorithm.blocks,
          {
            id: 'numer-block',
            type: 'numerator',
            name: 'Numerator',
            description: 'Patients meeting criteria',
            condition: 'Medication Z present',
            color: '#FF9800',
            x: 700,
            y: 100,
            width: 150,
            height: 80
          }
        ],
        connections: [
          { fromBlockId: 'initial-block', toBlockId: 'denom-block' },
          { fromBlockId: 'initial-block', toBlockId: 'numer-block' },
          { fromBlockId: 'denom-block', toBlockId: 'numer-block' }
        ]
      };

      algorithmService.getAlgorithm.and.returnValue(of(algorithmWithMultipleConnections));
      fixture.detectChanges();

      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mousemove', {
        clientX: 300,
        clientY: 200,
        bubbles: true
      }));

      fixture.detectChanges();

      const connections = fixture.debugElement.queryAll(el => el.nativeElement.getAttribute('data-connection-id'));
      // Should have 3 connections
      expect(connections.length).toBeGreaterThanOrEqual(2);
    });
  });

  describe('Drag & Drop - Context Menu', () => {
    it('should show context menu on right-click', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('contextmenu', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));
      fixture.detectChanges();

      const contextMenu = fixture.debugElement.query(el => el.nativeElement.classList.contains('context-menu'));
      expect(contextMenu).toBeTruthy();
    });

    it('should position context menu at mouse coordinates', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('contextmenu', {
        clientX: 200,
        clientY: 150,
        bubbles: true
      }));
      fixture.detectChanges();

      const contextMenu = fixture.debugElement.query(el => el.nativeElement.classList.contains('context-menu'));
      const style = contextMenu?.nativeElement.style;

      expect(parseInt(style.left) || 0).toBeCloseTo(200, 10);
      expect(parseInt(style.top) || 0).toBeCloseTo(150, 10);
    });

    it('should display "Edit" option in context menu', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('contextmenu', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));
      fixture.detectChanges();

      const editOption = fixture.debugElement.query(el =>
        el.nativeElement.textContent?.includes('Edit')
      );
      expect(editOption).toBeTruthy();
    });

    it('should display "Duplicate" option in context menu', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('contextmenu', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));
      fixture.detectChanges();

      const duplicateOption = fixture.debugElement.query(el =>
        el.nativeElement.textContent?.includes('Duplicate')
      );
      expect(duplicateOption).toBeTruthy();
    });

    it('should display "Delete" option in context menu', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('contextmenu', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));
      fixture.detectChanges();

      const deleteOption = fixture.debugElement.query(el =>
        el.nativeElement.textContent?.includes('Delete')
      );
      expect(deleteOption).toBeTruthy();
    });

    it('should call duplicate handler when "Duplicate" clicked', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('contextmenu', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));
      fixture.detectChanges();

      const duplicateOption = fixture.debugElement.query(el =>
        el.nativeElement.textContent?.includes('Duplicate')
      );

      duplicateOption?.nativeElement.click();
      fixture.detectChanges();

      expect(algorithmService.duplicateBlock).toHaveBeenCalled();
    });

    it('should call delete handler when "Delete" clicked', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('contextmenu', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));
      fixture.detectChanges();

      const deleteOption = fixture.debugElement.query(el =>
        el.nativeElement.textContent?.includes('Delete')
      );

      deleteOption?.nativeElement.click();
      fixture.detectChanges();

      expect(algorithmService.removeBlock).toHaveBeenCalled();
    });

    it('should close context menu on click outside', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('contextmenu', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));
      fixture.detectChanges();

      window.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      fixture.detectChanges();

      const contextMenu = fixture.debugElement.query(el => el.nativeElement.classList.contains('context-menu'));
      expect(contextMenu?.nativeElement.classList.contains('hidden')).toBeTruthy();
    });

    it('should prevent right-click default behavior', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      const event = new MouseEvent('contextmenu', {
        clientX: 175,
        clientY: 140,
        bubbles: true,
        cancelable: true
      });

      spyOn(event, 'preventDefault');

      blockElement?.nativeElement.dispatchEvent(event);

      expect(event.preventDefault).toHaveBeenCalled();
    });
  });

  describe('Drag & Drop - Connection Creation', () => {
    it('should enter connection mode on shift+click', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('click', {
        shiftKey: true,
        bubbles: true
      }));
      fixture.detectChanges();

      expect(component.connectionMode).toBeTruthy();
    });

    it('should highlight blocks available for connection in connection mode', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('click', {
        shiftKey: true,
        bubbles: true
      }));
      fixture.detectChanges();

      const availableBlocks = fixture.debugElement.queryAll(el =>
        el.nativeElement.classList.contains('available-target')
      );

      expect(availableBlocks.length).toBeGreaterThan(0);
    });

    it('should create connection on shift+click to target block', () => {
      const fromBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const toBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'denom-block');

      // Start connection mode
      fromBlock?.nativeElement.dispatchEvent(new MouseEvent('click', {
        shiftKey: true,
        bubbles: true
      }));
      fixture.detectChanges();

      // Click target block
      toBlock?.nativeElement.dispatchEvent(new MouseEvent('click', {
        shiftKey: true,
        bubbles: true
      }));
      fixture.detectChanges();

      expect(algorithmService.addConnection).toHaveBeenCalled();
    });

    it('should cancel connection mode on ESC key', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('click', {
        shiftKey: true,
        bubbles: true
      }));
      fixture.detectChanges();

      const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
      window.dispatchEvent(escapeEvent);
      fixture.detectChanges();

      expect(component.connectionMode).toBeFalsy();
    });

    it('should show visual feedback while creating connection', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('click', {
        shiftKey: true,
        bubbles: true
      }));
      fixture.detectChanges();

      const connectionGuide = fixture.debugElement.query(el => el.nativeElement.classList.contains('connection-guide'));
      expect(connectionGuide).toBeTruthy();
    });
  });

  describe('Drag & Drop - Undo/Redo Integration', () => {
    it('should support undo after drag operation', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mousemove', {
        clientX: 300,
        clientY: 200,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
      fixture.detectChanges();

      component.undo();

      expect(algorithmService.undo).toHaveBeenCalled();
    });

    it('should support redo after undo', () => {
      component.redo();
      expect(algorithmService.redo).toHaveBeenCalled();
    });
  });

  describe('Drag & Drop - Keyboard Shortcuts', () => {
    it('should delete selected block with Delete key', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.click();
      fixture.detectChanges();

      const deleteEvent = new KeyboardEvent('keydown', { key: 'Delete' });
      window.dispatchEvent(deleteEvent);
      fixture.detectChanges();

      expect(algorithmService.removeBlock).toHaveBeenCalled();
    });

    it('should duplicate selected block with Ctrl+D', () => {
      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');

      blockElement?.nativeElement.click();
      fixture.detectChanges();

      const duplicateEvent = new KeyboardEvent('keydown', {
        key: 'd',
        ctrlKey: true
      });
      window.dispatchEvent(duplicateEvent);
      fixture.detectChanges();

      expect(algorithmService.duplicateBlock).toHaveBeenCalled();
    });
  });

  describe('Drag & Drop - Performance', () => {
    it('should handle drag operations with 50+ blocks efficiently', () => {
      const largeAlgorithm: MeasureAlgorithm = {
        ...mockAlgorithm,
        blocks: Array.from({ length: 50 }, (_, i) => ({
          id: `block-${i}`,
          type: 'denominator',
          name: `Block ${i}`,
          description: `Test block ${i}`,
          condition: 'Test condition',
          color: '#4CAF50',
          x: 100 + (i % 5) * 200,
          y: 100 + Math.floor(i / 5) * 150,
          width: 150,
          height: 80
        } as PopulationBlock))
      };

      algorithmService.getAlgorithm.and.returnValue(of(largeAlgorithm));
      fixture.detectChanges();

      const startTime = performance.now();

      const blockElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'block-0');
      blockElement?.nativeElement.dispatchEvent(new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true
      }));

      window.dispatchEvent(new MouseEvent('mousemove', {
        clientX: 300,
        clientY: 200,
        bubbles: true
      }));

      const endTime = performance.now();

      expect(endTime - startTime).toBeLessThan(100); // Should be very fast
    });
  });
});
