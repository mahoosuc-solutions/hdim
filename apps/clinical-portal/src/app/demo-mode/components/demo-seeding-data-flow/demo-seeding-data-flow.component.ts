import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { Subject, takeUntil } from 'rxjs';
import { DemoModeService } from '../../services/demo-mode.service';
import { DemoSeedingDataFlowService } from '../../services/demo-seeding-data-flow.service';
import { PipelineState, PipelineNode, PipelineConnection } from '../../../models/system-event.model';
import { StatusIndicatorComponent, IndicatorStatusType } from '../../../shared/components/status-indicator/status-indicator.component';

/**
 * Demo Seeding Data Flow Component
 * 
 * Visualizes data flow through services during demo scenario loading.
 * Shows real-time progress as data flows: Demo Seeding -> FHIR -> Patient -> Care Gap -> Quality Measure
 */
@Component({
  selector: 'app-demo-seeding-data-flow',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    MatChipsModule,
    MatTooltipModule,
    MatExpansionModule,
    StatusIndicatorComponent,
  ],
  templateUrl: './demo-seeding-data-flow.component.html',
  styleUrl: './demo-seeding-data-flow.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DemoSeedingDataFlowComponent implements OnInit, OnDestroy {
  pipeline: PipelineState = {
    nodes: [],
    connections: [],
    lastUpdated: new Date().toISOString(),
  };

  selectedNode: PipelineNode | null = null;
  
  // Statistics from progress
  patientsGenerated = 0;
  patientsPersisted = 0;
  careGapsCreated = 0;
  measuresSeeded = 0;
  progressPercent = 0;
  currentStage = '';
  stageMessage = '';

  // Node positions (x, y coordinates in SVG viewBox)
  private nodePositions = [
    { x: 100, y: 100 },   // Demo Seeding
    { x: 250, y: 100 },   // FHIR Service
    { x: 400, y: 100 },   // Patient Service
    { x: 550, y: 100 },   // Care Gap Service
    { x: 700, y: 100 },   // Quality Measure Service
  ];

  private destroy$ = new Subject<void>();

  constructor(
    public demoModeService: DemoModeService,
    private dataFlowService: DemoSeedingDataFlowService,
    private cdr: ChangeDetectorRef
  ) {
    // React to progress changes
    effect(() => {
      const progress = this.demoModeService.progress();
      if (progress) {
        this.updateFromProgress(progress);
      } else {
        // Reset to initial state
        this.pipeline = this.dataFlowService.mapProgressToPipelineState(null);
        this.resetStatistics();
        this.cdr.markForCheck();
      }
    });
  }

  ngOnInit(): void {
    // Initial pipeline state
    const progress = this.demoModeService.progress();
    if (progress) {
      this.updateFromProgress(progress);
    } else {
      this.pipeline = this.dataFlowService.mapProgressToPipelineState(null);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Update component state from progress
   */
  private updateFromProgress(progress: any): void {
    this.pipeline = this.dataFlowService.mapProgressToPipelineState(progress);
    
    this.patientsGenerated = progress.patientsGenerated || 0;
    this.patientsPersisted = progress.patientsPersisted || 0;
    this.careGapsCreated = progress.careGapsCreated || 0;
    this.measuresSeeded = progress.measuresSeeded || 0;
    this.progressPercent = progress.progressPercent || 0;
    this.currentStage = progress.stage || '';
    this.stageMessage = progress.message || '';
    
    this.cdr.markForCheck();
  }

  /**
   * Reset statistics
   */
  private resetStatistics(): void {
    this.patientsGenerated = 0;
    this.patientsPersisted = 0;
    this.careGapsCreated = 0;
    this.measuresSeeded = 0;
    this.progressPercent = 0;
    this.currentStage = '';
    this.stageMessage = '';
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
   * Get particle animation duration
   */
  getParticleDuration(conn: PipelineConnection): string {
    // Faster throughput = faster particles
    const baseDuration = 2; // seconds
    const throughputFactor = Math.max(0.5, Math.min(2, conn.throughput / 10));
    return `${baseDuration / throughputFactor}s`;
  }

  /**
   * Get status color for a node
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'active':
        return '#4caf50';
      case 'processing':
        return '#2196f3';
      case 'error':
        return '#f44336';
      default:
        return '#9e9e9e';
    }
  }

  /**
   * Get status type for status indicator
   */
  getStatusType(status: string): IndicatorStatusType {
    switch (status) {
      case 'active':
        return 'active';
      case 'processing':
        return 'processing';
      case 'error':
        return 'error';
      default:
        return 'idle';
    }
  }

  /**
   * Get node icon character (Material Icons)
   */
  getNodeIconChar(nodeId: string): string {
    const iconMap: Record<string, string> = {
      'demo-seeding': '\ue8b8', // movie_filter
      'fhir': '\ue8b8', // cloud
      'patient': '\ue7ef', // person
      'care-gap': '\ue002', // warning
      'quality-measure': '\ue869', // assessment
    };
    return iconMap[nodeId] || '\ue88e'; // default: settings
  }

  /**
   * Select/deselect a node
   */
  selectNode(node: PipelineNode): void {
    if (this.selectedNode?.id === node.id) {
      this.selectedNode = null;
    } else {
      this.selectedNode = node;
    }
  }

  /**
   * Format duration in milliseconds
   */
  formatDuration(ms: number): string {
    if (ms < 1000) return `${ms}ms`;
    const seconds = Math.floor(ms / 1000);
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}m ${secs}s`;
  }

  /**
   * Format timestamp
   */
  formatTime(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString();
  }

  /**
   * Get stage display name
   */
  getStageDisplayName(stage: string): string {
    const stageNames: Record<string, string> = {
      'INITIALIZING': 'Initializing',
      'RESETTING': 'Resetting Data',
      'GENERATING_PATIENTS': 'Generating Patients',
      'PERSISTING_FHIR': 'Persisting FHIR Resources',
      'CREATING_CARE_GAPS': 'Creating Care Gaps',
      'SEEDING_MEASURES': 'Seeding Quality Measures',
      'COMPLETE': 'Complete',
      'FAILED': 'Failed',
      'CANCELLED': 'Cancelled',
    };
    return stageNames[stage] || stage;
  }
}
