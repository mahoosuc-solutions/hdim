import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  AfterViewInit,
  ChangeDetectorRef,
  HostListener,
  Input
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AlgorithmBuilderService } from '../../services/algorithm-builder.service';
import { MeasureAlgorithm, PopulationBlock } from '../../models/measure-builder.model';

/**
 * VisualAlgorithmBuilderComponent renders a visual SVG representation of a measure algorithm
 * with population blocks (initial, denominator, numerator, exclusion, exception) and
 * connection lines between them.
 *
 * Features:
 * - SVG-based rendering for scalability and performance
 * - Color-coded population blocks for easy identification
 * - Connection lines with arrow heads
 * - Interactive hover effects
 * - Tooltips with block information
 * - Responsive design with viewBox
 * - Accessibility support with ARIA labels and titles
 * - Handles large algorithms (50+ blocks) efficiently
 */
@Component({
  selector: 'app-visual-algorithm-builder',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './visual-algorithm-builder.component.html',
  styleUrl: './visual-algorithm-builder.component.scss'
})
export class VisualAlgorithmBuilderComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('svgCanvas') svgCanvas?: ElementRef<SVGSVGElement>;

  @Input() algorithm: MeasureAlgorithm | null = null;
  hoveredBlockId: string | null = null;
  selectedBlockId: string | null = null;

  // Drag & Drop properties
  isDragging = false;
  draggedBlockId: string | null = null;
  dragStartX = 0;
  dragStartY = 0;
  dragStartBlockX = 0;
  dragStartBlockY = 0;

  // Connection mode properties
  connectionMode = false;
  sourceBlockId: string | null = null;

  // Context menu properties
  contextMenu: { x: number; y: number; blockId: string } | null = null;

  // Color mapping for population block types
  private readonly blockColors: Map<string, string> = new Map([
    ['initial_population', '#2196F3'],  // Blue
    ['denominator', '#4CAF50'],         // Green
    ['numerator', '#FF9800'],           // Orange
    ['exclusion', '#F44336'],           // Red
    ['exception', '#9C27B0']            // Purple
  ]);

  private destroy$ = new Subject<void>();

  constructor(
    private algorithmService: AlgorithmBuilderService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAlgorithm();
    this.subscribeToAlgorithmChanges();
  }

  ngAfterViewInit(): void {
    this.renderSVG();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load the algorithm from the service
   */
  private loadAlgorithm(): void {
    this.algorithmService
      .algorithm$
      .pipe(takeUntil(this.destroy$))
      .subscribe(algorithm => {
        this.algorithm = algorithm;
        this.cdr.detectChanges();
        this.renderSVG();
      });
  }

  /**
   * Subscribe to algorithm changes and re-render
   */
  private subscribeToAlgorithmChanges(): void {
    // In a real implementation, this would subscribe to state changes
    // For now, we just load once
  }

  /**
   * Render the complete SVG visualization
   */
  private renderSVG(): void {
    if (!this.svgCanvas || !this.algorithm) return;

    const svg = this.svgCanvas.nativeElement;
    // Clear existing content
    while (svg.firstChild) {
      svg.removeChild(svg.firstChild);
    }

    // Add grid background and definitions
    this.renderDefinitions(svg);

    // Add grid background
    this.renderGrid(svg);

    // Render connection lines first (so they appear behind blocks)
    this.renderConnections(svg);

    // Render blocks
    this.algorithm.blocks.forEach(block => {
      this.renderBlock(svg, block);
    });
  }

  /**
   * Render SVG definitions (patterns, markers, etc.)
   */
  private renderDefinitions(svg: SVGSVGElement): void {
    const defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');

    // Grid pattern
    const pattern = document.createElementNS('http://www.w3.org/2000/svg', 'pattern');
    pattern.setAttribute('id', 'grid');
    pattern.setAttribute('width', '20');
    pattern.setAttribute('height', '20');
    pattern.setAttribute('patternUnits', 'userSpaceOnUse');

    const gridLine1 = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    gridLine1.setAttribute('d', 'M 20 0 L 0 0 0 20');
    gridLine1.setAttribute('fill', 'none');
    gridLine1.setAttribute('stroke', '#e0e0e0');
    gridLine1.setAttribute('stroke-width', '0.5');

    pattern.appendChild(gridLine1);
    defs.appendChild(pattern);

    // Arrow marker for connection lines
    const marker = document.createElementNS('http://www.w3.org/2000/svg', 'marker');
    marker.setAttribute('id', 'arrowhead');
    marker.setAttribute('markerWidth', '10');
    marker.setAttribute('markerHeight', '10');
    marker.setAttribute('refX', '9');
    marker.setAttribute('refY', '3');
    marker.setAttribute('orient', 'auto');

    const arrowPath = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
    arrowPath.setAttribute('points', '0 0, 10 3, 0 6');
    arrowPath.setAttribute('fill', '#999');

    marker.appendChild(arrowPath);
    defs.appendChild(marker);

    svg.appendChild(defs);
  }

  /**
   * Render grid background
   */
  private renderGrid(svg: SVGSVGElement): void {
    const backgroundRect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
    backgroundRect.setAttribute('width', '1200');
    backgroundRect.setAttribute('height', '600');
    backgroundRect.setAttribute('fill', 'url(#grid)');
    svg.appendChild(backgroundRect);
  }

  /**
   * Render a single population block
   */
  private renderBlock(svg: SVGSVGElement, block: PopulationBlock): void {
    const group = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    group.setAttribute('data-block-id', block.id);
    group.setAttribute('data-block-type', block.type);
    group.setAttribute('class', 'block');
    group.setAttribute('transform', `translate(${block.x},${block.y})`);
    group.setAttribute('aria-label', `${block.name}: ${block.description}`);

    // Add title for accessibility
    const title = document.createElementNS('http://www.w3.org/2000/svg', 'title');
    title.textContent = block.name;
    group.appendChild(title);

    // Draw rectangle
    const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
    rect.setAttribute('width', block.width.toString());
    rect.setAttribute('height', block.height.toString());
    rect.setAttribute('fill', block.color);
    rect.setAttribute('stroke', '#333');
    rect.setAttribute('stroke-width', '1');
    rect.setAttribute('rx', '4');
    rect.setAttribute('ry', '4');
    group.appendChild(rect);

    // Draw text label
    const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    text.setAttribute('x', (block.width / 2).toString());
    text.setAttribute('y', (block.height / 2 + 4).toString());
    text.setAttribute('text-anchor', 'middle');
    text.setAttribute('dominant-baseline', 'middle');
    text.setAttribute('font-size', '12');
    text.setAttribute('font-weight', '500');
    text.setAttribute('fill', 'white');
    text.textContent = block.name;
    group.appendChild(text);

    // Add tooltip
    const tooltip = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    tooltip.setAttribute('class', 'tooltip');
    tooltip.setAttribute('data-tooltip', 'true');
    tooltip.setAttribute('visibility', 'hidden');

    const tooltipBg = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
    tooltipBg.setAttribute('width', '200');
    tooltipBg.setAttribute('height', '60');
    tooltipBg.setAttribute('fill', '#333');
    tooltipBg.setAttribute('stroke', '#999');
    tooltipBg.setAttribute('rx', '4');
    tooltipBg.setAttribute('x', String(block.width! / 2 - 100));
    tooltipBg.setAttribute('y', String(-(block.height! + 10)));
    tooltip.appendChild(tooltipBg);

    const tooltipText = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    tooltipText.setAttribute('x', String(block.width! / 2));
    tooltipText.setAttribute('y', String(-(block.height! + 50)));
    tooltipText.setAttribute('text-anchor', 'middle');
    tooltipText.setAttribute('font-size', '11');
    tooltipText.setAttribute('fill', 'white');
    tooltipText.textContent = `${block.name}: ${block.description}`;
    tooltip.appendChild(tooltipText);

    group.appendChild(tooltip);

    // Add event listeners for hover
    group.addEventListener('mouseenter', () => this.onBlockHover(group));
    group.addEventListener('mouseleave', () => this.onBlockLeave(group));

    svg.appendChild(group);
  }

  /**
   * Render all connection lines between blocks
   */
  private renderConnections(svg: SVGSVGElement): void {
    if (!this.algorithm?.connections) return;

    this.algorithm.connections.forEach(connection => {
      const fromBlock = this.algorithm!.blocks.find(b => b.id === connection.fromBlockId);
      const toBlock = this.algorithm!.blocks.find(b => b.id === connection.toBlockId);

      if (fromBlock && toBlock) {
        this.renderConnectionLine(svg, fromBlock, toBlock, connection.fromBlockId, connection.toBlockId);
      }
    });
  }

  /**
   * Render a single connection line between two blocks
   */
  private renderConnectionLine(
    svg: SVGSVGElement,
    fromBlock: PopulationBlock,
    toBlock: PopulationBlock,
    fromId: string,
    toId: string
  ): void {
    const group = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    group.setAttribute('data-connection-id', `${fromId}-to-${toId}`);
    group.setAttribute('class', 'connection');

    // Calculate start and end points (centers of blocks)
    const fromX = fromBlock.x + fromBlock.width / 2;
    const fromY = fromBlock.y + fromBlock.height / 2;
    const toX = toBlock.x + toBlock.width / 2;
    const toY = toBlock.y + toBlock.height / 2;

    // Create Bezier curve path
    const midX = (fromX + toX) / 2;
    const pathData = `M ${fromX} ${fromY} C ${midX} ${fromY}, ${midX} ${toY}, ${toX} ${toY}`;

    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', pathData);
    path.setAttribute('stroke', '#999');
    path.setAttribute('stroke-width', '2');
    path.setAttribute('fill', 'none');
    path.setAttribute('marker-end', 'url(#arrowhead)');
    group.appendChild(path);

    svg.appendChild(group);
  }

  /**
   * Handle block hover
   */
  private onBlockHover(blockElement: SVGGElement): void {
    this.hoveredBlockId = blockElement.getAttribute('data-block-id');
    blockElement.classList.add('hover');

    // Show tooltip
    const tooltip = blockElement.querySelector('.tooltip') as SVGElement;
    if (tooltip) {
      tooltip.setAttribute('visibility', 'visible');
    }
  }

  /**
   * Handle block leave
   */
  private onBlockLeave(blockElement: SVGGElement): void {
    this.hoveredBlockId = null;
    blockElement.classList.remove('hover');

    // Hide tooltip
    const tooltip = blockElement.querySelector('.tooltip') as SVGElement;
    if (tooltip) {
      tooltip.setAttribute('visibility', 'hidden');
    }
  }

  /**
   * Get color for a block type
   */
  getBlockColor(blockType: string): string {
    const color = this.blockColors.get(blockType);
    if (!color) {
      throw new Error(`Invalid block type: ${blockType}`);
    }
    return color;
  }

  /**
   * Handle block click to select
   */
  selectBlock(blockId: string): void {
    this.selectedBlockId = blockId;
  }

  /**
   * Start dragging a block
   */
  startBlockDrag(blockId: string, event: MouseEvent): void {
    event.preventDefault();
    this.isDragging = true;
    this.draggedBlockId = blockId;
    this.dragStartX = event.clientX;
    this.dragStartY = event.clientY;

    const block = this.algorithm?.blocks.find(b => b.id === blockId);
    if (block) {
      this.dragStartBlockX = block.x;
      this.dragStartBlockY = block.y;
    }

    document.addEventListener('mousemove', this.onMouseMove.bind(this));
    document.addEventListener('mouseup', this.onMouseUp.bind(this));
  }

  /**
   * Handle mouse move during drag
   */
  private onMouseMove = (event: MouseEvent): void => {
    if (!this.isDragging || !this.draggedBlockId || !this.algorithm) return;

    const deltaX = event.clientX - this.dragStartX;
    const deltaY = event.clientY - this.dragStartY;

    let newX = this.dragStartBlockX + deltaX / 1.5;
    let newY = this.dragStartBlockY + deltaY / 1.5;

    // Snap to 20px grid
    newX = Math.round(newX / 20) * 20;
    newY = Math.round(newY / 20) * 20;

    // Prevent dragging outside canvas
    const block = this.algorithm.blocks.find(b => b.id === this.draggedBlockId);
    if (block) {
      newX = Math.max(0, Math.min(newX, 1200 - block.width));
      newY = Math.max(0, Math.min(newY, 600 - block.height));

      // Update block position
      block.x = newX;
      block.y = newY;

      // Notify service
      this.algorithmService.updateBlockPosition(this.draggedBlockId, newX, newY);

      // Re-render
      this.renderSVG();
    }
  };

  /**
   * End dragging
   */
  private onMouseUp = (): void => {
    this.isDragging = false;
    this.draggedBlockId = null;

    document.removeEventListener('mousemove', this.onMouseMove.bind(this));
    document.removeEventListener('mouseup', this.onMouseUp.bind(this));
  };

  /**
   * Show context menu on right-click
   */
  showContextMenu(blockId: string, event: MouseEvent): void {
    event.preventDefault();
    this.contextMenu = {
      x: event.clientX,
      y: event.clientY,
      blockId
    };
    this.selectedBlockId = blockId;
    this.cdr.detectChanges();
  }

  /**
   * Hide context menu
   */
  hideContextMenu(): void {
    this.contextMenu = null;
  }

  /**
   * Edit block from context menu
   */
  editBlock(): void {
    if (this.contextMenu) {
      // Emit event or call service
      console.log('Edit block:', this.contextMenu.blockId);
    }
    this.hideContextMenu();
  }

  /**
   * Duplicate block from context menu
   */
  duplicateBlock(): void {
    if (this.contextMenu) {
      this.algorithmService.duplicateBlock(this.contextMenu.blockId);
    }
    this.hideContextMenu();
  }

  /**
   * Delete block from context menu
   */
  deleteBlock(): void {
    if (this.contextMenu) {
      this.algorithmService.removeBlock(this.contextMenu.blockId);
    }
    this.hideContextMenu();
  }

  /**
   * Enter connection creation mode (Shift+Click on block)
   */
  startConnectionMode(blockId: string): void {
    if (!this.connectionMode) {
      this.connectionMode = true;
      this.sourceBlockId = blockId;
    } else if (blockId !== this.sourceBlockId) {
      // Create connection
      if (this.sourceBlockId) {
        this.algorithmService.addConnection(this.sourceBlockId, blockId);
      }
      this.connectionMode = false;
      this.sourceBlockId = null;
    }
    this.cdr.detectChanges();
  }

  /**
   * Cancel connection mode
   */
  cancelConnectionMode(): void {
    this.connectionMode = false;
    this.sourceBlockId = null;
  }

  /**
   * Handle keyboard events
   */
  @HostListener('window:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.cancelConnectionMode();
      this.hideContextMenu();
    }

    if (event.key === 'Delete' && this.selectedBlockId) {
      this.algorithmService.removeBlock(this.selectedBlockId);
    }

    if (event.ctrlKey && event.key === 'd' && this.selectedBlockId) {
      event.preventDefault();
      this.algorithmService.duplicateBlock(this.selectedBlockId);
    }
  }

  /**
   * Undo last action
   */
  undo(): void {
    this.algorithmService.undo();
  }

  /**
   * Redo last undone action
   */
  redo(): void {
    this.algorithmService.redo();
  }

  /**
   * Get all blocks
   */
  getBlocks(): PopulationBlock[] {
    return this.algorithm?.blocks || [];
  }
}
