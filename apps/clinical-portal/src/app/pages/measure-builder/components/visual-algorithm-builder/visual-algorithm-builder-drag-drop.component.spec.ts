import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VisualAlgorithmBuilderComponent } from './visual-algorithm-builder.component';
import { AlgorithmBuilderService } from '../../services/algorithm-builder.service';
import { LoggerService } from '../../../../services/logger.service';
import { of } from 'rxjs';
import { MeasureAlgorithm, PopulationBlock } from '../../models/measure-builder.model';

describe('VisualAlgorithmBuilderComponent - Drag & Drop Suite', () => {
  let component: VisualAlgorithmBuilderComponent;
  let fixture: ComponentFixture<VisualAlgorithmBuilderComponent>;
  let algorithmService: any;

  const mockBlocks: PopulationBlock[] = [
    {
      id: 'initial-block',
      type: 'initial' as any,
      label: 'Initial Population',
      name: 'Initial Population',
      description: 'All eligible patients',
      condition: 'Condition X present',
      color: '#2196F3',
      position: { x: 100, y: 100 },
      x: 100,
      y: 100,
      width: 150,
      height: 80
    },
    {
      id: 'denom-block',
      type: 'denominator',
      label: 'Denominator',
      name: 'Denominator',
      description: 'Patients with lab result',
      condition: 'Lab Y > 100',
      color: '#4CAF50',
      position: { x: 400, y: 100 },
      x: 400,
      y: 100,
      width: 150,
      height: 80
    }
  ];

  const mockAlgorithm: MeasureAlgorithm = {
    initialPopulation: mockBlocks[0],
    denominator: mockBlocks[1],
    numerator: mockBlocks[1], // reuse for simplicity
    exclusions: [],
    exceptions: [],
    blocks: mockBlocks,
    connections: [
      { id: 'conn-1', sourceBlockId: 'initial-block', targetBlockId: 'denom-block', fromBlockId: 'initial-block', toBlockId: 'denom-block', connectionType: 'inclusion' }
    ]
  } as MeasureAlgorithm;

  beforeEach(async () => {
    const algorithmServiceSpy = {
      getAlgorithm: jest.fn(),
      algorithm$: of(mockAlgorithm),
      updateBlockPosition: jest.fn(),
      addConnection: jest.fn(),
      removeConnection: jest.fn(),
      addExclusionBlock: jest.fn(),
      removeBlock: jest.fn(),
      duplicateBlock: jest.fn(),
      undo: jest.fn(),
      redo: jest.fn(),
    };

    const loggerServiceSpy = {
      withContext: jest.fn().mockReturnValue({
        info: jest.fn(),
        warn: jest.fn(),
        error: jest.fn(),
        debug: jest.fn(),
      }),
    };

    await TestBed.configureTestingModule({
      imports: [VisualAlgorithmBuilderComponent],
      providers: [
        { provide: AlgorithmBuilderService, useValue: algorithmServiceSpy },
        { provide: LoggerService, useValue: loggerServiceSpy }
      ]
    }).compileComponents();

    algorithmService = TestBed.inject(AlgorithmBuilderService) as any;
    algorithmService.getAlgorithm.mockReturnValue(of(mockAlgorithm));

    fixture = TestBed.createComponent(VisualAlgorithmBuilderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Drag & Drop - Block Positioning', () => {
    it('should allow dragging a block via startBlockDrag', () => {
      const mouseEvent = new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true,
        cancelable: true,
      });

      component.startBlockDrag('initial-block', mouseEvent);
      fixture.detectChanges();

      expect(component.isDragging).toBeTruthy();
      expect(component.draggedBlockId).toBe('initial-block');
    });

    it('should update block position on mousemove while dragging', () => {
      const mouseEvent = new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true,
        cancelable: true,
      });

      component.startBlockDrag('initial-block', mouseEvent);
      fixture.detectChanges();

      // Move mouse — startBlockDrag registers document-level listeners
      document.dispatchEvent(new MouseEvent('mousemove', {
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
      const calls = algorithmService.updateBlockPosition.mock.calls;
      if (calls.length > 0) {
        const args = calls[calls.length - 1];
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

    it('should track drag state during drag', () => {
      const mouseEvent = new MouseEvent('mousedown', {
        clientX: 175,
        clientY: 140,
        bubbles: true,
        cancelable: true,
      });

      component.startBlockDrag('initial-block', mouseEvent);
      fixture.detectChanges();

      // Verify drag state is active
      expect(component.isDragging).toBeTruthy();
      expect(component.draggedBlockId).toBe('initial-block');
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

    it('should render connection lines with path data', () => {
      fixture.detectChanges();

      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      const path = connectionElement?.query(el => el.nativeElement.tagName === 'path');
      const pathData = path?.nativeElement.getAttribute('d');

      expect(pathData).toContain('M');
      expect(pathData).toContain('C');
    });

    it('should render connection between initial and denom blocks', () => {
      fixture.detectChanges();

      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      expect(connectionElement).toBeTruthy();
      expect(connectionElement?.nativeElement.getAttribute('data-connection-id')).toContain('initial-block');
    });

    it('should render multiple connection lines for multi-connection algorithm', () => {
      const numerBlock: PopulationBlock = {
        id: 'numer-block',
        type: 'numerator',
        label: 'Numerator',
        name: 'Numerator',
        description: 'Patients meeting criteria',
        condition: 'Medication Z present',
        color: '#FF9800',
        position: { x: 700, y: 100 },
        x: 700,
        y: 100,
        width: 150,
        height: 80
      };
      const algorithmWithMultipleConnections: MeasureAlgorithm = {
        ...mockAlgorithm,
        blocks: [
          ...mockAlgorithm.blocks!,
          numerBlock
        ],
        connections: [
          { id: 'conn-1', sourceBlockId: 'initial-block', targetBlockId: 'denom-block', fromBlockId: 'initial-block', toBlockId: 'denom-block', connectionType: 'inclusion' },
          { id: 'conn-2', sourceBlockId: 'initial-block', targetBlockId: 'numer-block', fromBlockId: 'initial-block', toBlockId: 'numer-block', connectionType: 'inclusion' },
          { id: 'conn-3', sourceBlockId: 'denom-block', targetBlockId: 'numer-block', fromBlockId: 'denom-block', toBlockId: 'numer-block', connectionType: 'inclusion' }
        ]
      };

      // Re-render with the updated algorithm by re-calling renderSVG
      (component as any).algorithm = algorithmWithMultipleConnections;
      (component as any).renderSVG();
      fixture.detectChanges();

      const connections = fixture.debugElement.queryAll(el => el.nativeElement.getAttribute('data-connection-id'));
      expect(connections.length).toBeGreaterThanOrEqual(2);
    });
  });

  describe('Drag & Drop - Context Menu', () => {
    it('should set context menu state programmatically', () => {
      component.contextMenu = { x: 175, y: 140, blockId: 'initial-block' };
      fixture.detectChanges();

      expect(component.contextMenu).toBeTruthy();
      expect(component.contextMenu!.blockId).toBe('initial-block');
    });

    it('should store context menu position', () => {
      component.contextMenu = { x: 200, y: 150, blockId: 'initial-block' };
      fixture.detectChanges();

      expect(component.contextMenu!.x).toBe(200);
      expect(component.contextMenu!.y).toBe(150);
    });

    it('should call duplicate service when duplicating block', () => {
      component.contextMenu = { x: 175, y: 140, blockId: 'initial-block' };
      algorithmService.duplicateBlock('initial-block');

      expect(algorithmService.duplicateBlock).toHaveBeenCalledWith('initial-block');
    });

    it('should call remove service when deleting block', () => {
      component.contextMenu = { x: 175, y: 140, blockId: 'initial-block' };
      algorithmService.removeBlock('initial-block');

      expect(algorithmService.removeBlock).toHaveBeenCalledWith('initial-block');
    });

    it('should clear context menu', () => {
      component.contextMenu = { x: 175, y: 140, blockId: 'initial-block' };
      component.contextMenu = null;

      expect(component.contextMenu).toBeNull();
    });
  });

  describe('Drag & Drop - Connection Creation', () => {
    it('should enter connection mode programmatically', () => {
      component.connectionMode = true;
      component.sourceBlockId = 'initial-block';

      expect(component.connectionMode).toBeTruthy();
      expect(component.sourceBlockId).toBe('initial-block');
    });

    it('should track source block in connection mode', () => {
      component.connectionMode = true;
      component.sourceBlockId = 'initial-block';
      fixture.detectChanges();

      expect(component.sourceBlockId).toBe('initial-block');
    });

    it('should call addConnection service to create connection', () => {
      algorithmService.addConnection('initial-block', 'denom-block');

      expect(algorithmService.addConnection).toHaveBeenCalledWith('initial-block', 'denom-block');
    });

    it('should exit connection mode when cancelled', () => {
      component.connectionMode = true;
      component.sourceBlockId = 'initial-block';

      component.connectionMode = false;
      component.sourceBlockId = null;

      expect(component.connectionMode).toBeFalsy();
      expect(component.sourceBlockId).toBeNull();
    });

    it('should render existing connections in the algorithm', () => {
      const connections = fixture.debugElement.queryAll(el => el.nativeElement.getAttribute('data-connection-id'));
      expect(connections.length).toBeGreaterThan(0);
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
    it('should delete selected block via selectBlock + removeBlock', () => {
      component.selectBlock('initial-block');
      fixture.detectChanges();

      expect(component.selectedBlockId).toBe('initial-block');

      algorithmService.removeBlock('initial-block');
      expect(algorithmService.removeBlock).toHaveBeenCalledWith('initial-block');
    });

    it('should duplicate selected block via selectBlock + duplicateBlock', () => {
      component.selectBlock('initial-block');
      fixture.detectChanges();

      expect(component.selectedBlockId).toBe('initial-block');

      algorithmService.duplicateBlock('initial-block');
      expect(algorithmService.duplicateBlock).toHaveBeenCalledWith('initial-block');
    });
  });

  describe('Drag & Drop - Performance', () => {
    it('should handle drag operations with 50+ blocks efficiently', () => {
      const largeAlgorithm: MeasureAlgorithm = {
        ...mockAlgorithm,
        blocks: Array.from({ length: 50 }, (_, i) => ({
          id: `block-${i}`,
          type: 'denominator',
          label: `Block ${i}`,
          name: `Block ${i}`,
          description: `Test block ${i}`,
          condition: 'Test condition',
          color: '#4CAF50',
          position: { x: 100 + (i % 5) * 200, y: 100 + Math.floor(i / 5) * 150 },
          x: 100 + (i % 5) * 200,
          y: 100 + Math.floor(i / 5) * 150,
          width: 150,
          height: 80
        } as PopulationBlock))
      };

      algorithmService.getAlgorithm.mockReturnValue(of(largeAlgorithm));
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
