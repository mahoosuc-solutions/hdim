import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import * as THREE from 'three';
import { Subject, takeUntil } from 'rxjs';

import { ThreeSceneService } from '../core/three-scene.service';
import { WebSocketVisualizationService, WebSocketStatus, BatchProgressEvent } from '../core/websocket-visualization.service';
import { DataTransformService, EvaluationProgressEvent } from '../data/data-transform.service';
import { BatchMonitorService, BatchMonitorState } from '../../services/batch-monitor.service';

/**
 * Particle Data for instanced mesh
 */
interface ParticleData {
  id: string;
  position: THREE.Vector3;
  velocity: THREE.Vector3;
  color: THREE.Color;
  size: number;
  progress: number;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
}

/**
 * Live Batch Monitor Component
 *
 * Real-time 3D visualization of CQL evaluation batch progress.
 * Uses GPU-instanced particles for high performance rendering of
 * thousands of concurrent evaluations.
 *
 * Features:
 * - Real-time WebSocket updates
 * - GPU-accelerated particle system
 * - Interactive 3D camera controls
 * - Performance monitoring (FPS)
 * - Connection status indicator
 * - Batch progress statistics
 */
@Component({
  selector: 'app-live-batch-monitor',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
  ],
  templateUrl: './live-batch-monitor.component.html',
  styleUrl: './live-batch-monitor.component.scss',
})
export class LiveBatchMonitorComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('sceneContainer', { static: true }) sceneContainer!: ElementRef<HTMLDivElement>;

  // Component state
  private destroy$ = new Subject<void>();
  private particles: ParticleData[] = [];
  private instancedMesh?: THREE.InstancedMesh;
  private animationCallback?: (delta: number, elapsed: number) => void;

  // WebSocket status
  wsStatus: WebSocketStatus = WebSocketStatus.DISCONNECTED;
  WebSocketStatus = WebSocketStatus; // For template

  // Batch progress data
  batchProgress?: BatchProgressEvent;
  evaluationCount = 0;

  // Batch monitor state
  batchState?: BatchMonitorState;

  // UI state
  useSimulation = false;
  isLoading = true;
  isMonitoring = false;

  // Configuration options
  selectedLibraryId = '';
  patientCount = 100;
  availableLibraries = [
    { id: 'test-library-1', name: 'CDC A1C Control Measure', version: '1.0.0' },
    { id: 'test-library-2', name: 'CMS Diabetes Care', version: '2.0.0' },
    { id: 'test-library-3', name: 'HEDIS Comprehensive Diabetes', version: '1.5.0' },
  ];

  constructor(
    private sceneService: ThreeSceneService,
    private wsService: WebSocketVisualizationService,
    private transformService: DataTransformService,
    private batchMonitorService: BatchMonitorService
  ) {}

  ngOnInit(): void {
    // Initialize with first library
    if (this.availableLibraries.length > 0) {
      this.selectedLibraryId = this.availableLibraries[0].id;
    }

    // Subscribe to WebSocket status
    this.batchMonitorService.websocketStatus$
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        this.wsStatus = status;
        console.log('WebSocket status:', status);
      });

    // Subscribe to batch monitor state
    this.batchMonitorService.state$
      .pipe(takeUntil(this.destroy$))
      .subscribe(state => {
        this.batchState = state;
        console.log('Batch monitor state:', state);

        // Update monitoring flag
        this.isMonitoring = state.status === 'MONITORING' || state.status === 'LOADING_PATIENTS' || state.status === 'SUBMITTING_BATCH';
      });

    // Subscribe to batch progress events
    this.wsService.batchProgress$
      .pipe(takeUntil(this.destroy$))
      .subscribe(event => {
        this.batchProgress = event;
        this.updateBatchVisualization(event);
      });

    // Subscribe to evaluation progress events
    this.wsService.evaluationProgress$
      .pipe(takeUntil(this.destroy$))
      .subscribe(event => {
        this.evaluationCount++;
        this.addOrUpdateParticle(event);
      });
  }

  ngAfterViewInit(): void {
    // Initialize Three.js scene
    this.initScene();

    // Don't auto-connect - wait for user to start batch evaluation
    console.log('Visualization ready. Waiting for user to start batch evaluation.');
  }

  ngOnDestroy(): void {
    // Clean up
    this.destroy$.next();
    this.destroy$.complete();

    // Unregister animation callback
    if (this.animationCallback) {
      this.sceneService.unregisterAnimationCallback(this.animationCallback);
    }

    // Disconnect WebSocket
    this.wsService.disconnect();

    // Dispose scene (don't dispose the service itself as it's singleton)
    // this.sceneService.dispose();
  }

  /**
   * Initialize Three.js scene
   */
  private initScene(): void {
    this.sceneService.initScene(this.sceneContainer.nativeElement, {
      enableStats: true,
      enableOrbitControls: true,
      enableGrid: true,
      backgroundColor: 0x0a0e1a,
      cameraPosition: new THREE.Vector3(0, 80, 150),
    });

    // Create particle system
    this.createParticleSystem();

    // Register animation callback
    this.animationCallback = (delta: number, elapsed: number) => {
      this.updateParticles(delta, elapsed);
    };
    this.sceneService.registerAnimationCallback(this.animationCallback);

    // Start animation
    this.sceneService.startAnimation();

    this.isLoading = false;
  }

  /**
   * Create GPU-instanced particle system
   */
  private createParticleSystem(): void {
    // Create base geometry (sphere for each particle)
    const geometry = new THREE.SphereGeometry(1, 16, 16);

    // Create material
    const material = new THREE.MeshPhongMaterial({
      color: 0xffffff,
      shininess: 30,
      transparent: true,
      opacity: 0.9,
    });

    // Create instanced mesh (max 1000 particles)
    this.instancedMesh = new THREE.InstancedMesh(geometry, material, 1000);
    this.instancedMesh.instanceMatrix.setUsage(THREE.DynamicDrawUsage);

    // Add to scene
    this.sceneService.addToScene(this.instancedMesh);

    // Initialize with empty particles
    this.particles = [];
  }

  /**
   * Update batch visualization
   */
  private updateBatchVisualization(event: BatchProgressEvent): void {
    console.log('Batch progress update:', event);

    // Generate particles for new patients if needed
    const targetParticleCount = event.totalPatients;
    while (this.particles.length < targetParticleCount) {
      const index = this.particles.length;
      const angle = (index / targetParticleCount) * Math.PI * 2;
      const radius = 40 + Math.random() * 20;

      const particle: ParticleData = {
        id: `particle-${index}`,
        position: new THREE.Vector3(
          Math.cos(angle) * radius,
          Math.random() * 10,
          Math.sin(angle) * radius
        ),
        velocity: new THREE.Vector3(
          (Math.random() - 0.5) * 0.5,
          (Math.random() - 0.5) * 0.5,
          (Math.random() - 0.5) * 0.5
        ),
        color: new THREE.Color(0x2196f3), // Blue (pending)
        size: 1.0,
        progress: 0,
        status: 'PENDING',
      };

      this.particles.push(particle);
    }

    // Update particle states based on progress
    const completedCount = event.completedCount ?? event.completedPatients ?? 0;
    const successCount = event.successCount ?? event.successfulEvaluations ?? 0;
    const successRate = successCount / Math.max(1, completedCount);

    for (let i = 0; i < completedCount && i < this.particles.length; i++) {
      const particle = this.particles[i];
      if (particle.status === 'PENDING') {
        particle.status = Math.random() < successRate ? 'SUCCESS' : 'FAILED';
        particle.color = particle.status === 'SUCCESS'
          ? new THREE.Color(0x4caf50) // Green
          : new THREE.Color(0xf44336); // Red
        particle.progress = 100;
        particle.size = particle.status === 'SUCCESS' ? 1.2 : 0.8;
      }
    }
  }

  /**
   * Add or update particle for evaluation
   */
  private addOrUpdateParticle(event: EvaluationProgressEvent): void {
    const visualProps = this.transformService.transformEvaluationProgress(
      event,
      this.particles.length,
      100
    );

    // Find existing particle or create new one
    let particle = this.particles.find(p => p.id === event.patientId);

    if (!particle) {
      particle = {
        id: event.patientId,
        position: this.transformService.toVector3(visualProps.position),
        velocity: new THREE.Vector3(
          (Math.random() - 0.5) * 0.2,
          (Math.random() - 0.5) * 0.2,
          (Math.random() - 0.5) * 0.2
        ),
        color: visualProps.color,
        size: visualProps.size,
        progress: event.progress,
        status: event.status,
      };
      this.particles.push(particle);
    } else {
      particle.color = visualProps.color;
      particle.size = visualProps.size;
      particle.progress = event.progress;
      particle.status = event.status;
    }
  }

  /**
   * Update particles in animation loop
   */
  private updateParticles(delta: number, elapsed: number): void {
    if (!this.instancedMesh || this.particles.length === 0) return;

    const matrix = new THREE.Matrix4();
    const color = new THREE.Color();

    // Update each particle instance
    for (let i = 0; i < this.particles.length; i++) {
      const particle = this.particles[i];

      // Update position with velocity
      particle.position.add(particle.velocity.clone().multiplyScalar(delta * 10));

      // Bounce off boundaries
      const boundary = 60;
      if (Math.abs(particle.position.x) > boundary) {
        particle.velocity.x *= -1;
        particle.position.x = Math.sign(particle.position.x) * boundary;
      }
      if (particle.position.y < 0 || particle.position.y > 50) {
        particle.velocity.y *= -1;
        particle.position.y = Math.max(0, Math.min(50, particle.position.y));
      }
      if (Math.abs(particle.position.z) > boundary) {
        particle.velocity.z *= -1;
        particle.position.z = Math.sign(particle.position.z) * boundary;
      }

      // Pulse animation for pending particles
      if (particle.status === 'PENDING') {
        particle.size = 1.0 + Math.sin(elapsed * 2 + i * 0.1) * 0.2;
      }

      // Set instance matrix (position, rotation, scale)
      matrix.makeScale(particle.size, particle.size, particle.size);
      matrix.setPosition(particle.position);
      this.instancedMesh.setMatrixAt(i, matrix);

      // Set instance color
      this.instancedMesh.setColorAt(i, particle.color);
    }

    // Update instance count
    this.instancedMesh.count = this.particles.length;

    // Mark for update
    this.instancedMesh.instanceMatrix.needsUpdate = true;
    if (this.instancedMesh.instanceColor) {
      this.instancedMesh.instanceColor.needsUpdate = true;
    }
  }

  /**
   * Start real batch evaluation
   */
  startBatchEvaluation(): void {
    if (!this.selectedLibraryId) {
      console.error('No library selected');
      return;
    }

    if (this.isMonitoring) {
      console.warn('Batch evaluation already in progress');
      return;
    }

    console.log('Starting batch evaluation...', {
      libraryId: this.selectedLibraryId,
      patientCount: this.patientCount
    });

    // Reset particles
    this.particles = [];
    this.batchProgress = undefined;
    this.evaluationCount = 0;

    // Start batch evaluation
    this.batchMonitorService.startBatchEvaluation({
      libraryId: this.selectedLibraryId,
      patientCount: this.patientCount,
      autoConnect: true
    })
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (progress) => {
        console.log('Batch progress received:', progress);
        this.batchProgress = progress;
        this.updateBatchVisualization(progress);
      },
      error: (error) => {
        console.error('Batch evaluation error:', error);
        this.isMonitoring = false;
      },
      complete: () => {
        console.log('Batch evaluation completed');
        this.isMonitoring = false;
      }
    });
  }

  /**
   * Stop batch monitoring
   */
  stopBatchMonitoring(): void {
    console.log('Stopping batch monitoring...');
    this.batchMonitorService.stopBatchMonitoring();
    this.isMonitoring = false;
  }

  /**
   * Start simulation (fallback when WebSocket unavailable)
   */
  startSimulation(): void {
    this.useSimulation = true;
    console.log('Starting simulation...');

    // Reset particles
    this.particles = [];
    this.batchProgress = undefined;

    this.wsService.simulateBatchProgress(30)
      .pipe(takeUntil(this.destroy$))
      .subscribe(event => {
        this.batchProgress = event;
        this.updateBatchVisualization(event);
      });
  }

  /**
   * Reconnect to WebSocket
   */
  reconnect(): void {
    this.batchMonitorService.disconnectWebSocket();
    setTimeout(() => {
      this.batchMonitorService.connectWebSocket();
    }, 500);
  }

  /**
   * Get status color for chip
   */
  getStatusColor(): string {
    switch (this.wsStatus) {
      case WebSocketStatus.CONNECTED:
        return 'primary';
      case WebSocketStatus.CONNECTING:
      case WebSocketStatus.RECONNECTING:
        return 'accent';
      case WebSocketStatus.DISCONNECTED:
      case WebSocketStatus.ERROR:
        return 'warn';
      default:
        return '';
    }
  }

  /**
   * Get status icon
   */
  getStatusIcon(): string {
    switch (this.wsStatus) {
      case WebSocketStatus.CONNECTED:
        return 'check_circle';
      case WebSocketStatus.CONNECTING:
      case WebSocketStatus.RECONNECTING:
        return 'sync';
      case WebSocketStatus.DISCONNECTED:
        return 'cloud_off';
      case WebSocketStatus.ERROR:
        return 'error';
      default:
        return 'help';
    }
  }
}
