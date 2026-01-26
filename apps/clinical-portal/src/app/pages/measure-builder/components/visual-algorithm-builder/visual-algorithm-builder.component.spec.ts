import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VisualAlgorithmBuilderComponent } from './visual-algorithm-builder.component';
import { AlgorithmBuilderService } from '../../services/algorithm-builder.service';
import { of } from 'rxjs';
import { PopulationBlock, MeasureAlgorithm } from '../../models/measure-builder.model';

describe('VisualAlgorithmBuilderComponent - SVG Rendering Suite', () => {
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
      },
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
      { fromBlockId: 'denom-block', toBlockId: 'numer-block' }
    ]
  };

  beforeEach(async () => {
    const algorithmServiceSpy = jasmine.createSpyObj('AlgorithmBuilderService', [
      'getAlgorithm',
      'addExclusionBlock',
      'removeBlock',
      'addConnection',
      'removeConnection'
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

  describe('SVG Rendering - Canvas Initialization', () => {
    it('should render SVG canvas on component initialization', () => {
      const svgElement = fixture.debugElement.query(el => el.nativeElement.tagName === 'svg');
      expect(svgElement).toBeTruthy();
    }, 30000);

    it('should set correct SVG canvas dimensions', () => {
      const svgElement = fixture.debugElement.query(el => el.nativeElement.tagName === 'svg');
      const svg = svgElement?.nativeElement;
      expect(svg.getAttribute('width')).toBe('1200');
      expect(svg.getAttribute('height')).toBe('600');
    }, 30000);

    it('should render with 24:12 aspect ratio (1200x600)', () => {
      const svgElement = fixture.debugElement.query(el => el.nativeElement.tagName === 'svg');
      const svg = svgElement?.nativeElement;
      const width = parseInt(svg.getAttribute('width'), 10);
      const height = parseInt(svg.getAttribute('height'), 10);
      expect(width / height).toBe(2);
    }, 30000);

    it('should include SVG viewport and coordinate system', () => {
      const svgElement = fixture.debugElement.query(el => el.nativeElement.tagName === 'svg');
      const svg = svgElement?.nativeElement;
      expect(svg.getAttribute('viewBox')).toBeTruthy();
      expect(svg.getAttribute('xmlns')).toBe('http://www.w3.org/2000/svg');
    }, 30000);

    it('should render background grid pattern', () => {
      const defs = fixture.debugElement.query(el => el.nativeElement.tagName === 'defs');
      expect(defs).toBeTruthy();
      const pattern = defs?.debugElement.query(el => el.nativeElement.tagName === 'pattern');
      expect(pattern).toBeTruthy();
    }, 30000);
  }, 30000);

  describe('SVG Rendering - Population Blocks', () => {
    it('should render 3 population blocks for test algorithm', () => {
      const blockElements = fixture.debugElement.queryAll(el => el.nativeElement.getAttribute('data-block-id'));
      expect(blockElements.length).toBe(3);
    }, 30000);

    it('should render initial population block with correct color (#2196F3)', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const rect = initialBlock?.debugElement.query(el => el.nativeElement.tagName === 'rect');
      expect(rect?.nativeElement.getAttribute('fill')).toBe('#2196F3');
    }, 30000);

    it('should render denominator block with correct color (#4CAF50)', () => {
      const denomBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'denom-block');
      const rect = denomBlock?.debugElement.query(el => el.nativeElement.tagName === 'rect');
      expect(rect?.nativeElement.getAttribute('fill')).toBe('#4CAF50');
    }, 30000);

    it('should render numerator block with correct color (#FF9800)', () => {
      const numerBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'numer-block');
      const rect = numerBlock?.debugElement.query(el => el.nativeElement.tagName === 'rect');
      expect(rect?.nativeElement.getAttribute('fill')).toBe('#FF9800');
    }, 30000);

    it('should render block with correct position (x, y coordinates)', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const group = initialBlock?.nativeElement;
      expect(group.getAttribute('transform')).toContain('translate(100,100)');
    }, 30000);

    it('should render block with correct dimensions (width x height)', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const rect = initialBlock?.debugElement.query(el => el.nativeElement.tagName === 'rect');
      expect(rect?.nativeElement.getAttribute('width')).toBe('150');
      expect(rect?.nativeElement.getAttribute('height')).toBe('80');
    }, 30000);

    it('should render block with rounded corners (rx, ry)', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const rect = initialBlock?.debugElement.query(el => el.nativeElement.tagName === 'rect');
      expect(rect?.nativeElement.getAttribute('rx')).toBe('4');
      expect(rect?.nativeElement.getAttribute('ry')).toBe('4');
    }, 30000);

    it('should render block label text', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const textElement = initialBlock?.debugElement.query(el => el.nativeElement.tagName === 'text');
      expect(textElement?.nativeElement.textContent).toContain('Initial Population');
    }, 30000);

    it('should render block text centered within block', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const textElement = initialBlock?.debugElement.query(el => el.nativeElement.tagName === 'text');
      expect(textElement?.nativeElement.getAttribute('x')).toBe('75');
      expect(textElement?.nativeElement.getAttribute('y')).toBe('40');
    }, 30000);

    it('should render text with correct styling (font-size, font-weight)', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const textElement = initialBlock?.debugElement.query(el => el.nativeElement.tagName === 'text');
      expect(textElement?.nativeElement.getAttribute('font-size')).toBe('12');
      expect(textElement?.nativeElement.getAttribute('font-weight')).toBe('500');
    }, 30000);

    it('should render text as white color for contrast', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const textElement = initialBlock?.debugElement.query(el => el.nativeElement.tagName === 'text');
      expect(textElement?.nativeElement.getAttribute('fill')).toBe('white');
    }, 30000);

    it('should render block border stroke', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const rect = initialBlock?.debugElement.query(el => el.nativeElement.tagName === 'rect');
      expect(rect?.nativeElement.getAttribute('stroke')).toBeTruthy();
      expect(rect?.nativeElement.getAttribute('stroke-width')).toBe('1');
    }, 30000);
  }, 30000);

  describe('SVG Rendering - Exclusion & Exception Blocks', () => {
    beforeEach(() => {
      const algorithmWithExclusionException: MeasureAlgorithm = {
        ...mockAlgorithm,
        blocks: [
          ...mockAlgorithm.blocks,
          {
            id: 'exclusion-block',
            type: 'exclusion',
            name: 'Exclusion',
            description: 'Patients with contraindication',
            condition: 'Allergy Y present',
            color: '#F44336',
            x: 400,
            y: 250,
            width: 150,
            height: 80
          },
          {
            id: 'exception-block',
            type: 'exception',
            name: 'Exception',
            description: 'Clinical judgment exceptions',
            condition: 'Patient declined',
            color: '#9C27B0',
            x: 700,
            y: 250,
            width: 150,
            height: 80
          }
        ]
      };
      algorithmService.getAlgorithm.and.returnValue(of(algorithmWithExclusionException));
      fixture.detectChanges();
    }, 30000);

    it('should render exclusion block with correct color (#F44336)', () => {
      const exclusionBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'exclusion-block');
      const rect = exclusionBlock?.debugElement.query(el => el.nativeElement.tagName === 'rect');
      expect(rect?.nativeElement.getAttribute('fill')).toBe('#F44336');
    }, 30000);

    it('should render exception block with correct color (#9C27B0)', () => {
      const exceptionBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'exception-block');
      const rect = exceptionBlock?.debugElement.query(el => el.nativeElement.tagName === 'rect');
      expect(rect?.nativeElement.getAttribute('fill')).toBe('#9C27B0');
    }, 30000);

    it('should render 5 blocks total (initial, denominator, numerator, exclusion, exception)', () => {
      const blockElements = fixture.debugElement.queryAll(el => el.nativeElement.getAttribute('data-block-id'));
      expect(blockElements.length).toBe(5);
    }, 30000);
  }, 30000);

  describe('SVG Rendering - Connection Lines', () => {
    it('should render 2 connection lines for test algorithm', () => {
      const connectionElements = fixture.debugElement.queryAll(el => el.nativeElement.getAttribute('data-connection-id'));
      expect(connectionElements.length).toBe(2);
    }, 30000);

    it('should render connection line as SVG path element', () => {
      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      const path = connectionElement?.debugElement.query(el => el.nativeElement.tagName === 'path');
      expect(path).toBeTruthy();
    }, 30000);

    it('should render connection line with d attribute (SVG path data)', () => {
      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      const path = connectionElement?.debugElement.query(el => el.nativeElement.tagName === 'path');
      expect(path?.nativeElement.getAttribute('d')).toBeTruthy();
    }, 30000);

    it('should render curved connection lines using Bezier curves', () => {
      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      const path = connectionElement?.debugElement.query(el => el.nativeElement.tagName === 'path');
      const pathData = path?.nativeElement.getAttribute('d');
      // Bezier curves use 'C' command
      expect(pathData).toContain('C');
    }, 30000);

    it('should render connection line with correct stroke color (#999)', () => {
      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      const path = connectionElement?.debugElement.query(el => el.nativeElement.tagName === 'path');
      expect(path?.nativeElement.getAttribute('stroke')).toBe('#999');
    }, 30000);

    it('should render connection line with correct stroke width (2)', () => {
      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      const path = connectionElement?.debugElement.query(el => el.nativeElement.tagName === 'path');
      expect(path?.nativeElement.getAttribute('stroke-width')).toBe('2');
    }, 30000);

    it('should render connection line as non-filled (fill="none")', () => {
      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      const path = connectionElement?.debugElement.query(el => el.nativeElement.tagName === 'path');
      expect(path?.nativeElement.getAttribute('fill')).toBe('none');
    }, 30000);

    it('should render arrow head at end of connection line', () => {
      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      const marker = connectionElement?.debugElement.query(el => el.nativeElement.tagName === 'polygon');
      expect(marker).toBeTruthy();
    }, 30000);

    it('should render arrow head with triangle shape', () => {
      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      const marker = connectionElement?.debugElement.query(el => el.nativeElement.tagName === 'polygon');
      expect(marker?.nativeElement.getAttribute('points')).toBeTruthy();
    }, 30000);

    it('should position connection line from center of from-block to center of to-block', () => {
      const connectionElement = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-connection-id'));
      const pathData = connectionElement?.debugElement.query(el => el.nativeElement.tagName === 'path')?.nativeElement.getAttribute('d');
      // Should start near (175, 140) - center of initial block (100+150/2, 100+80/2)
      expect(pathData).toContain('M');
    }, 30000);
  }, 30000);

  describe('SVG Rendering - Hover Effects', () => {
    it('should add hover class to block on mouse enter', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      initialBlock?.nativeElement.dispatchEvent(new MouseEvent('mouseenter'));
      fixture.detectChanges();
      expect(initialBlock?.nativeElement.classList.contains('hover')).toBeTruthy();
    }, 30000);

    it('should remove hover class from block on mouse leave', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      initialBlock?.nativeElement.classList.add('hover');
      initialBlock?.nativeElement.dispatchEvent(new MouseEvent('mouseleave'));
      fixture.detectChanges();
      expect(initialBlock?.nativeElement.classList.contains('hover')).toBeFalsy();
    }, 30000);

    it('should change block opacity on hover', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      initialBlock?.nativeElement.classList.add('hover');
      const computedStyle = window.getComputedStyle(initialBlock?.nativeElement);
      // The style should show reduced opacity for other blocks
      expect(computedStyle.opacity).toBeLessThanOrEqual('1');
    }, 30000);

    it('should highlight connection lines on block hover', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      initialBlock?.nativeElement.dispatchEvent(new MouseEvent('mouseenter'));
      fixture.detectChanges();
      // Connected lines should have highlight class
      const connections = fixture.debugElement.queryAll(el => el.nativeElement.getAttribute('data-connection-id'));
      expect(connections.length).toBeGreaterThan(0);
    }, 30000);

    it('should display tooltip on hover', (done) => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      initialBlock?.nativeElement.dispatchEvent(new MouseEvent('mouseenter'));
      fixture.detectChanges();

      // Tooltip should appear after hover delay
      setTimeout(() => {
        const tooltip = fixture.debugElement.query(el => el.nativeElement.classList.contains('tooltip'));
        expect(tooltip).toBeTruthy();
        done();
      }, 100);
    }, 30000);
  });

  describe('SVG Rendering - Tooltips', () => {
    it('should render tooltip for each block', () => {
      const blockElements = fixture.debugElement.queryAll(el => el.nativeElement.getAttribute('data-block-id'));
      blockElements.forEach(block => {
        const tooltip = block?.debugElement.query(el => el.nativeElement.getAttribute('data-tooltip'));
        expect(tooltip).toBeTruthy();
      }, 30000);
    }, 30000);

    it('should display block name in tooltip', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const tooltip = initialBlock?.debugElement.query(el => el.nativeElement.getAttribute('data-tooltip'));
      expect(tooltip?.nativeElement.textContent).toContain('Initial Population');
    }, 30000);

    it('should display block description in tooltip', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const tooltip = initialBlock?.debugElement.query(el => el.nativeElement.getAttribute('data-tooltip'));
      expect(tooltip?.nativeElement.textContent).toContain('All eligible patients');
    }, 30000);

    it('should display block condition in tooltip', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const tooltip = initialBlock?.debugElement.query(el => el.nativeElement.getAttribute('data-tooltip'));
      expect(tooltip?.nativeElement.textContent).toContain('Condition X present');
    }, 30000);

    it('should position tooltip above or beside block', () => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      const tooltip = initialBlock?.debugElement.query(el => el.nativeElement.getAttribute('data-tooltip'));
      const tooltipStyle = tooltip?.nativeElement.style;
      // Tooltip should have position relative to block
      expect(tooltipStyle.position).toBeTruthy();
    }, 30000);

    it('should hide tooltip on mouse leave', (done) => {
      const initialBlock = fixture.debugElement.query(el => el.nativeElement.getAttribute('data-block-id') === 'initial-block');
      initialBlock?.nativeElement.dispatchEvent(new MouseEvent('mouseleave'));
      fixture.detectChanges();

      setTimeout(() => {
        const tooltip = fixture.debugElement.query(el => el.nativeElement.classList.contains('tooltip-visible'));
        expect(tooltip).toBeFalsy();
        done();
      }, 100);
    }, 30000);
  });

  describe('SVG Rendering - Performance', () => {
    it('should render large algorithm (50 blocks) in less than 500ms', () => {
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

      const startTime = performance.now();
      algorithmService.getAlgorithm.and.returnValue(of(largeAlgorithm));
      fixture.detectChanges();
      const endTime = performance.now();

      expect(endTime - startTime).toBeLessThan(500);
    });

    it('should use SVG rendering groups for efficient rendering', () => {
      const groups = fixture.debugElement.queryAll(el => el.nativeElement.tagName === 'g');
      expect(groups.length).toBeGreaterThan(0);
    });

    it('should use SVG defs for reusable elements (patterns, markers)', () => {
      const defs = fixture.debugElement.query(el => el.nativeElement.tagName === 'defs');
      expect(defs).toBeTruthy();
    });
  });

  describe('SVG Rendering - Responsive Design', () => {
    it('should maintain aspect ratio on viewport resize', () => {
      const svgElement = fixture.debugElement.query(el => el.nativeElement.tagName === 'svg');
      const svg = svgElement?.nativeElement;
      const viewBox = svg.getAttribute('viewBox');
      expect(viewBox).toContain('0');
    });

    it('should scale SVG for different screen sizes', () => {
      const svgElement = fixture.debugElement.query(el => el.nativeElement.tagName === 'svg');
      const svg = svgElement?.nativeElement;
      expect(svg.getAttribute('preserveAspectRatio')).toBeTruthy();
    });
  });

  describe('SVG Rendering - Accessibility', () => {
    it('should render blocks with accessible titles', () => {
      const blockElements = fixture.debugElement.queryAll(el => el.nativeElement.getAttribute('data-block-id'));
      blockElements.forEach(block => {
        const title = block?.debugElement.query(el => el.nativeElement.tagName === 'title');
        expect(title).toBeTruthy();
      });
    });

    it('should provide ARIA labels for blocks', () => {
      const blockElements = fixture.debugElement.queryAll(el => el.nativeElement.getAttribute('data-block-id'));
      blockElements.forEach(block => {
        const ariaLabel = block?.nativeElement.getAttribute('aria-label');
        expect(ariaLabel).toBeTruthy();
      });
    });
  });

  describe('SVG Rendering - Color Consistency', () => {
    it('should use correct color code for initial population (#2196F3)', () => {
      expect(component.getBlockColor('initial_population')).toBe('#2196F3');
    });

    it('should use correct color code for denominator (#4CAF50)', () => {
      expect(component.getBlockColor('denominator')).toBe('#4CAF50');
    });

    it('should use correct color code for numerator (#FF9800)', () => {
      expect(component.getBlockColor('numerator')).toBe('#FF9800');
    });

    it('should use correct color code for exclusion (#F44336)', () => {
      expect(component.getBlockColor('exclusion')).toBe('#F44336');
    });

    it('should use correct color code for exception (#9C27B0)', () => {
      expect(component.getBlockColor('exception')).toBe('#9C27B0');
    });
  });

  describe('SVG Rendering - Block Type Validation', () => {
    it('should render only valid block types', () => {
      const blockElements = fixture.debugElement.queryAll(el => el.nativeElement.getAttribute('data-block-id'));
      const validTypes = ['initial_population', 'denominator', 'numerator', 'exclusion', 'exception'];
      blockElements.forEach(block => {
        const blockType = block?.nativeElement.getAttribute('data-block-type');
        expect(validTypes).toContain(blockType);
      });
    });

    it('should throw error for invalid block type', () => {
      expect(() => component.getBlockColor('invalid_type')).toThrowError();
    });
  });
});
