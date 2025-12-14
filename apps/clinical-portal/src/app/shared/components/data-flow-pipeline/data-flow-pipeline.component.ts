import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil, interval } from 'rxjs';
import { SystemEventsService } from '../../../services/system-events.service';
import { PipelineState, PipelineNode, PipelineConnection, getNodeStatusColor } from '../../../models/system-event.model';
import { StatusIndicatorComponent, IndicatorStatusType } from '../status-indicator/status-indicator.component';

/**
 * DataFlowPipeline Component
 *
 * Animated SVG diagram showing data flowing through the healthcare platform services.
 * Displays nodes for each service with status indicators and animated connections.
 *
 * Features:
 * - SVG-based animated pipeline
 * - Nodes: FHIR -> CQL Engine -> Quality Measures -> Care Gaps
 * - Animated particles flowing between nodes
 * - Node status indicators (active/idle/error)
 * - Throughput counters on connections
 * - Click node to see details
 *
 * @example
 * <app-data-flow-pipeline></app-data-flow-pipeline>
 */
@Component({
  selector: 'app-data-flow-pipeline',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
    StatusIndicatorComponent,
  ],
  template: `
    <mat-card class="pipeline-card">
      <mat-card-header class="pipeline-header">
        <mat-card-title class="pipeline-title">
          <mat-icon class="title-icon">account_tree</mat-icon>
          Data Flow Pipeline
        </mat-card-title>
      </mat-card-header>

      <mat-card-content class="pipeline-content">
        <div class="pipeline-diagram" #pipelineDiagram>
          <!-- SVG Pipeline -->
          <svg class="pipeline-svg" viewBox="0 0 800 200" preserveAspectRatio="xMidYMid meet">
            <!-- Definitions for gradients and filters -->
            <defs>
              <!-- Glow filter for active nodes -->
              <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
                <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
                <feMerge>
                  <feMergeNode in="coloredBlur"/>
                  <feMergeNode in="SourceGraphic"/>
                </feMerge>
              </filter>

              <!-- Arrow marker for connections -->
              <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                <polygon points="0 0, 10 3.5, 0 7" fill="#90a4ae"/>
              </marker>

              <!-- Animated particle -->
              <circle id="particle" r="4" fill="#4caf50">
                <animate attributeName="opacity" values="1;0.6;1" dur="1s" repeatCount="indefinite"/>
              </circle>
            </defs>

            <!-- Connection lines -->
            <g class="connections">
              <g *ngFor="let conn of pipeline.connections; let i = index">
                <!-- Connection path -->
                <path
                  [attr.d]="getConnectionPath(conn)"
                  class="connection-line"
                  [class.active]="conn.isActive"
                  stroke="#90a4ae"
                  stroke-width="2"
                  fill="none"
                  marker-end="url(#arrowhead)">
                </path>

                <!-- Animated particles on active connections -->
                <g *ngIf="conn.isActive">
                  <circle
                    *ngFor="let p of getParticles(i)"
                    r="4"
                    [attr.fill]="getParticleColor(conn)"
                    class="flow-particle">
                    <animateMotion
                      [attr.dur]="getParticleDuration(conn)"
                      repeatCount="indefinite"
                      [attr.begin]="p.delay + 's'">
                      <mpath [attr.href]="'#conn-path-' + i"/>
                    </animateMotion>
                  </circle>
                </g>

                <!-- Throughput label -->
                <text
                  *ngIf="conn.throughput > 0"
                  [attr.x]="getConnectionMidpoint(conn).x"
                  [attr.y]="getConnectionMidpoint(conn).y - 10"
                  class="throughput-label"
                  text-anchor="middle">
                  {{ conn.throughput.toFixed(1) }}/s
                </text>

                <!-- Hidden path for animation reference -->
                <path
                  [id]="'conn-path-' + i"
                  [attr.d]="getConnectionPath(conn)"
                  fill="none"
                  stroke="none">
                </path>
              </g>
            </g>

            <!-- Nodes -->
            <g class="nodes">
              <g *ngFor="let node of pipeline.nodes; let i = index"
                 [attr.transform]="'translate(' + getNodePosition(i).x + ',' + getNodePosition(i).y + ')'"
                 class="node-group"
                 [class.active]="node.status === 'active' || node.status === 'processing'"
                 (click)="selectNode(node)">

                <!-- Node background -->
                <rect
                  x="-60" y="-35"
                  width="120" height="70"
                  rx="8" ry="8"
                  class="node-bg"
                  [class]="'status-' + node.status"
                  [attr.filter]="node.status === 'processing' ? 'url(#glow)' : null">
                </rect>

                <!-- Node icon -->
                <g [attr.transform]="'translate(0, -8)'">
                  <circle r="18" class="icon-bg" [class]="'icon-' + node.id"/>
                  <text
                    text-anchor="middle"
                    dominant-baseline="central"
                    class="node-icon-text"
                    fill="white"
                    font-family="Material Icons"
                    font-size="18">
                    {{ getNodeIconChar(node.id) }}
                  </text>
                </g>

                <!-- Node name -->
                <text
                  y="22"
                  text-anchor="middle"
                  class="node-name">
                  {{ node.name }}
                </text>

                <!-- Status indicator -->
                <circle
                  cx="50" cy="-25"
                  r="6"
                  class="status-dot"
                  [attr.fill]="getStatusColor(node.status)">
                  <animate
                    *ngIf="node.status === 'processing'"
                    attributeName="opacity"
                    values="1;0.5;1"
                    dur="1s"
                    repeatCount="indefinite"/>
                </circle>

                <!-- Processing animation ring -->
                <circle
                  *ngIf="node.status === 'processing'"
                  r="38"
                  fill="none"
                  stroke="#2196f3"
                  stroke-width="2"
                  stroke-dasharray="20,10"
                  class="processing-ring">
                  <animateTransform
                    attributeName="transform"
                    type="rotate"
                    from="0"
                    to="360"
                    dur="3s"
                    repeatCount="indefinite"/>
                </circle>
              </g>
            </g>
          </svg>

          <!-- Node Details Panel -->
          <div class="node-details" *ngIf="selectedNode">
            <div class="details-header">
              <span class="details-title">{{ selectedNode.name }}</span>
              <button class="close-btn" (click)="selectedNode = null">
                <mat-icon>close</mat-icon>
              </button>
            </div>
            <div class="details-content">
              <div class="detail-row">
                <span class="detail-label">Status:</span>
                <app-status-indicator
                  [status]="getStatusType(selectedNode.status)"
                  size="small"
                  [showLabel]="true">
                </app-status-indicator>
              </div>
              <div class="detail-row">
                <span class="detail-label">Description:</span>
                <span class="detail-value">{{ selectedNode.description }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">Throughput:</span>
                <span class="detail-value">{{ selectedNode.throughput.toFixed(1) }} items/sec</span>
              </div>
              <div class="detail-row" *ngIf="selectedNode.errorCount > 0">
                <span class="detail-label">Errors:</span>
                <span class="detail-value error">{{ selectedNode.errorCount }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">Last Activity:</span>
                <span class="detail-value">{{ formatTime(selectedNode.lastActivity) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Legend -->
        <div class="pipeline-legend">
          <div class="legend-item">
            <span class="legend-dot active"></span>
            <span>Active</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot processing"></span>
            <span>Processing</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot idle"></span>
            <span>Idle</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot error"></span>
            <span>Error</span>
          </div>
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .pipeline-card {
      height: 100%;
      display: flex;
      flex-direction: column;
    }

    .pipeline-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.08);
    }

    .pipeline-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 1rem;
      margin: 0;
    }

    .title-icon {
      color: #1976d2;
    }

    .pipeline-content {
      flex: 1;
      padding: 16px;
      display: flex;
      flex-direction: column;
    }

    .pipeline-diagram {
      flex: 1;
      position: relative;
      min-height: 200px;
    }

    .pipeline-svg {
      width: 100%;
      height: 100%;
    }

    /* Node styles */
    .node-group {
      cursor: pointer;
      transition: transform 0.2s ease;
    }

    .node-group:hover {
      transform: scale(1.02);
    }

    .node-bg {
      fill: #fafafa;
      stroke: #e0e0e0;
      stroke-width: 2;
      transition: all 0.3s ease;
    }

    .node-bg.status-active {
      fill: #e8f5e9;
      stroke: #4caf50;
    }

    .node-bg.status-processing {
      fill: #e3f2fd;
      stroke: #2196f3;
    }

    .node-bg.status-error {
      fill: #ffebee;
      stroke: #f44336;
    }

    .node-bg.status-idle {
      fill: #fafafa;
      stroke: #bdbdbd;
    }

    .icon-bg {
      fill: #1976d2;
    }

    .icon-fhir { fill: #1976d2; }
    .icon-cql { fill: #7b1fa2; }
    .icon-quality { fill: #388e3c; }
    .icon-caregap { fill: #f57c00; }

    .node-icon-text {
      font-size: 16px;
    }

    .node-name {
      font-size: 11px;
      fill: #424242;
      font-weight: 500;
    }

    .status-dot {
      transition: fill 0.3s ease;
    }

    .processing-ring {
      opacity: 0.5;
    }

    /* Connection styles */
    .connection-line {
      transition: stroke 0.3s ease, stroke-width 0.3s ease;
    }

    .connection-line.active {
      stroke: #4caf50;
      stroke-width: 3;
    }

    .flow-particle {
      filter: drop-shadow(0 0 2px rgba(76, 175, 80, 0.5));
    }

    .throughput-label {
      font-size: 10px;
      fill: #666;
      font-weight: 500;
    }

    /* Node details panel */
    .node-details {
      position: absolute;
      bottom: 10px;
      right: 10px;
      width: 250px;
      background: white;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      z-index: 10;
    }

    .details-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 12px;
      border-bottom: 1px solid #eee;
    }

    .details-title {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .close-btn {
      background: none;
      border: none;
      cursor: pointer;
      padding: 2px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .close-btn mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: #666;
    }

    .details-content {
      padding: 12px;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      font-size: 0.8rem;
    }

    .detail-row:last-child {
      margin-bottom: 0;
    }

    .detail-label {
      color: #666;
    }

    .detail-value {
      color: #333;
      font-weight: 500;
    }

    .detail-value.error {
      color: #f44336;
    }

    /* Legend */
    .pipeline-legend {
      display: flex;
      justify-content: center;
      gap: 20px;
      padding-top: 12px;
      border-top: 1px solid rgba(0, 0, 0, 0.08);
      margin-top: auto;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.75rem;
      color: #666;
    }

    .legend-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
    }

    .legend-dot.active {
      background-color: #4caf50;
    }

    .legend-dot.processing {
      background-color: #2196f3;
      animation: pulse 1.5s ease-in-out infinite;
    }

    .legend-dot.idle {
      background-color: #9e9e9e;
    }

    .legend-dot.error {
      background-color: #f44336;
    }

    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataFlowPipelineComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('pipelineDiagram') pipelineDiagram!: ElementRef;

  pipeline: PipelineState = {
    nodes: [],
    connections: [],
    lastUpdated: new Date().toISOString(),
  };

  selectedNode: PipelineNode | null = null;

  // Node positions (x, y coordinates in SVG viewBox)
  private nodePositions = [
    { x: 100, y: 100 },   // FHIR
    { x: 300, y: 100 },   // CQL Engine
    { x: 500, y: 100 },   // Quality Measures
    { x: 700, y: 100 },   // Care Gap
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private eventsService: SystemEventsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Subscribe to pipeline state updates
    this.eventsService.pipeline$
      .pipe(takeUntil(this.destroy$))
      .subscribe(pipeline => {
        this.pipeline = pipeline;
        this.cdr.markForCheck();
      });
  }

  ngAfterViewInit(): void {
    // Any post-init setup
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get position for a node by index
   */
  getNodePosition(index: number): { x: number; y: number } {
    return this.nodePositions[index] || { x: 0, y: 0 };
  }

  /**
   * Get SVG path for a connection
   */
  getConnectionPath(conn: PipelineConnection): string {
    const fromIdx = this.pipeline.nodes.findIndex(n => n.id === conn.from);
    const toIdx = this.pipeline.nodes.findIndex(n => n.id === conn.to);

    if (fromIdx === -1 || toIdx === -1) return '';

    const from = this.nodePositions[fromIdx];
    const to = this.nodePositions[toIdx];

    // Curved path with control points
    const startX = from.x + 60; // Right edge of node
    const endX = to.x - 60;     // Left edge of node
    const midX = (startX + endX) / 2;

    return `M ${startX} ${from.y} C ${midX} ${from.y}, ${midX} ${to.y}, ${endX} ${to.y}`;
  }

  /**
   * Get midpoint of a connection for label placement
   */
  getConnectionMidpoint(conn: PipelineConnection): { x: number; y: number } {
    const fromIdx = this.pipeline.nodes.findIndex(n => n.id === conn.from);
    const toIdx = this.pipeline.nodes.findIndex(n => n.id === conn.to);

    if (fromIdx === -1 || toIdx === -1) return { x: 0, y: 0 };

    const from = this.nodePositions[fromIdx];
    const to = this.nodePositions[toIdx];

    return {
      x: (from.x + to.x) / 2,
      y: from.y,
    };
  }

  /**
   * Get particles for animated connection
   */
  getParticles(connectionIndex: number): { delay: number }[] {
    const conn = this.pipeline.connections[connectionIndex];
    if (!conn || !conn.isActive) return [];

    // Number of particles based on throughput
    const count = Math.min(5, Math.max(2, Math.ceil(conn.throughput / 2)));
    return Array.from({ length: count }, (_, i) => ({
      delay: (i / count) * 2, // Stagger start times
    }));
  }

  /**
   * Get particle color based on connection state
   */
  getParticleColor(conn: PipelineConnection): string {
    return conn.isActive ? '#4caf50' : '#9e9e9e';
  }

  /**
   * Get animation duration based on throughput
   */
  getParticleDuration(conn: PipelineConnection): string {
    // Faster animation for higher throughput
    const duration = Math.max(1, 3 - conn.throughput / 5);
    return duration + 's';
  }

  /**
   * Get node icon character (Material Icons)
   */
  getNodeIconChar(nodeId: string): string {
    const icons: Record<string, string> = {
      fhir: '\uef63',      // storage
      cql: '\ue86f',       // code
      quality: '\ue8e8',   // verified
      caregap: '\ue002',   // warning
    };
    return icons[nodeId] || '\ue88e'; // info
  }

  /**
   * Get status color
   */
  getStatusColor(status: string): string {
    return getNodeStatusColor(status as any);
  }

  /**
   * Convert pipeline status to StatusIndicator status type
   */
  getStatusType(status: string): IndicatorStatusType {
    const mapping: Record<string, IndicatorStatusType> = {
      active: 'active',
      idle: 'idle',
      processing: 'processing',
      error: 'error',
    };
    return mapping[status] || 'idle';
  }

  /**
   * Select a node to show details
   */
  selectNode(node: PipelineNode): void {
    this.selectedNode = this.selectedNode?.id === node.id ? null : node;
    this.cdr.markForCheck();
  }

  /**
   * Format timestamp
   */
  formatTime(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  }
}
