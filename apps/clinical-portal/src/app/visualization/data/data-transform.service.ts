import { Injectable } from '@angular/core';
import * as THREE from 'three';
import { scaleLinear, scaleSequential } from 'd3-scale';
import { interpolateViridis, interpolatePlasma, interpolateCool } from 'd3-scale-chromatic';
import { QualityMeasureResult } from '../../models/quality-result.model';
import { CqlEvaluation, EvaluationStatus } from '../../models/evaluation.model';

/**
 * 3D Position Data
 */
export interface Position3D {
  x: number;
  y: number;
  z: number;
}

/**
 * Visual Properties for 3D objects
 */
export interface VisualProperties {
  position: Position3D;
  color: THREE.Color;
  size: number;
  opacity: number;
}

/**
 * Evaluation Progress Event (from WebSocket)
 */
export interface EvaluationProgressEvent {
  batchId?: string;
  patientId: string;
  status: EvaluationStatus;
  progress: number;
  message?: string;
  timestamp: string;
}

/**
 * Data Transform Service
 *
 * Transforms healthcare data (evaluations, quality measures, patients)
 * into 3D coordinates and visual properties for Three.js visualization.
 *
 * Features:
 * - Position mapping (scatter plots, grids, radial layouts)
 * - Color mapping based on status, compliance, scores
 * - Size scaling based on metrics
 * - Normalization and distribution functions
 * - D3 scales for continuous color interpolation
 */
@Injectable({
  providedIn: 'root'
})
export class DataTransformService {
  // Color schemes for different visualization types
  private readonly STATUS_COLORS = {
    SUCCESS: new THREE.Color(0x4caf50), // Green
    FAILED: new THREE.Color(0xf44336), // Red
    PENDING: new THREE.Color(0xff9800), // Orange
  };

  private readonly COMPLIANCE_COLORS = {
    COMPLIANT: new THREE.Color(0x4caf50), // Green
    NON_COMPLIANT: new THREE.Color(0xff9800), // Orange
    NOT_ELIGIBLE: new THREE.Color(0x9e9e9e), // Gray
  };

  private readonly CATEGORY_COLORS = {
    HEDIS: new THREE.Color(0x2196f3), // Blue
    CMS: new THREE.Color(0x9c27b0), // Purple
    CUSTOM: new THREE.Color(0x009688), // Teal
  };

  constructor() {}

  /**
   * Transform evaluation progress event to visual properties
   * Used for real-time particle visualization
   */
  transformEvaluationProgress(event: EvaluationProgressEvent, index: number, total: number): VisualProperties {
    // Position in a spiral/circular layout
    const angle = (index / total) * Math.PI * 2;
    const radius = 30 + (event.progress / 100) * 20; // Expand as progress increases

    const position: Position3D = {
      x: Math.cos(angle) * radius,
      y: event.progress / 5, // Height based on progress
      z: Math.sin(angle) * radius,
    };

    // Color based on status
    let color: THREE.Color;
    if (event.status === 'SUCCESS') {
      color = this.STATUS_COLORS.SUCCESS;
    } else if (event.status === 'FAILED') {
      color = this.STATUS_COLORS.FAILED;
    } else {
      // Interpolate from blue to yellow as progress increases
      const t = event.progress / 100;
      color = new THREE.Color().lerpColors(
        new THREE.Color(0x2196f3), // Blue
        new THREE.Color(0xffeb3b), // Yellow
        t
      );
    }

    // Size based on status
    const size = event.status === 'SUCCESS' ? 1.2 : event.status === 'FAILED' ? 0.8 : 1.0;

    // Opacity
    const opacity = event.status === 'PENDING' ? 0.7 : 1.0;

    return { position, color, size, opacity };
  }

  /**
   * Transform quality measure result to visual properties
   * Used for patient quality constellation
   */
  transformQualityResult(result: QualityMeasureResult, index: number): VisualProperties {
    // Position based on compliance rate and score
    const position: Position3D = {
      x: (Math.random() - 0.5) * 100, // Random scatter with clustering
      y: result.complianceRate / 2, // Height based on compliance
      z: (Math.random() - 0.5) * 100,
    };

    // Color based on compliance
    let color: THREE.Color;
    if (!result.denominatorEligible) {
      color = this.COMPLIANCE_COLORS.NOT_ELIGIBLE;
    } else if (result.numeratorCompliant) {
      color = this.COMPLIANCE_COLORS.COMPLIANT;
    } else {
      color = this.COMPLIANCE_COLORS.NON_COMPLIANT;
    }

    // Size based on score
    const size = 0.5 + (result.score / 100);

    return { position, color, size, opacity: 1.0 };
  }

  /**
   * Transform CQL evaluation to visual properties
   */
  transformEvaluation(evaluation: CqlEvaluation, index: number): VisualProperties {
    // Position in grid layout
    const gridSize = 10;
    const row = Math.floor(index / gridSize);
    const col = index % gridSize;

    const position: Position3D = {
      x: (col - gridSize / 2) * 5,
      y: evaluation.durationMs ? Math.log10(evaluation.durationMs) : 0,
      z: (row - gridSize / 2) * 5,
    };

    // Color based on status
    const color = this.STATUS_COLORS[evaluation.status];

    // Size based on duration
    const size = evaluation.durationMs ? Math.min(2.0, evaluation.durationMs / 1000) : 1.0;

    return { position, color, size, opacity: 1.0 };
  }

  /**
   * Create scatter plot positions for patient population
   * Maps patients to 3D space based on multiple quality metrics
   */
  createScatterPlotPositions(
    results: QualityMeasureResult[],
    xMetric: keyof QualityMeasureResult,
    yMetric: keyof QualityMeasureResult,
    zMetric: keyof QualityMeasureResult
  ): Position3D[] {
    if (results.length === 0) return [];

    // Extract values for each metric
    const xValues = results.map(r => Number(r[xMetric]) || 0);
    const yValues = results.map(r => Number(r[yMetric]) || 0);
    const zValues = results.map(r => Number(r[zMetric]) || 0);

    // Create scales
    const xScale = scaleLinear()
      .domain([Math.min(...xValues), Math.max(...xValues)])
      .range([-50, 50]);

    const yScale = scaleLinear()
      .domain([Math.min(...yValues), Math.max(...yValues)])
      .range([0, 50]);

    const zScale = scaleLinear()
      .domain([Math.min(...zValues), Math.max(...zValues)])
      .range([-50, 50]);

    // Transform to 3D positions
    return results.map((result, index) => ({
      x: xScale(xValues[index]),
      y: yScale(yValues[index]),
      z: zScale(zValues[index]),
    }));
  }

  /**
   * Create radial layout positions
   * Arranges items in a circular pattern
   */
  createRadialLayout(count: number, radius = 50, layers = 1): Position3D[] {
    const positions: Position3D[] = [];
    const itemsPerLayer = Math.ceil(count / layers);
    const angleStep = (Math.PI * 2) / itemsPerLayer;
    const startAngle = -Math.PI + angleStep / 2;

    for (let i = 0; i < count; i++) {
      const layer = Math.floor(i / itemsPerLayer);
      const indexInLayer = i % itemsPerLayer;
      const angle = startAngle + indexInLayer * angleStep;
      const layerRadius = radius * (layer + 1) / layers;

      positions.push({
        x: Math.cos(angle) * layerRadius,
        y: layer * 10,
        z: Math.sin(angle) * layerRadius,
      });
    }

    return positions;
  }

  /**
   * Create grid layout positions
   */
  createGridLayout(count: number, spacing = 5): Position3D[] {
    const positions: Position3D[] = [];
    const gridSize = Math.ceil(Math.sqrt(count));
    const offset = (gridSize - 1) / 2;

    for (let i = 0; i < count; i++) {
      const row = Math.floor(i / gridSize);
      const col = i % gridSize;

      positions.push({
        x: (col - offset) * spacing,
        y: 0,
        z: (row - offset) * spacing,
      });
    }

    return positions;
  }

  /**
   * Create spiral layout positions
   */
  createSpiralLayout(count: number, radiusStep = 2, heightStep = 0.5): Position3D[] {
    const positions: Position3D[] = [];
    const angleStep = Math.PI / 4; // 45 degrees per step

    for (let i = 0; i < count; i++) {
      const angle = i * angleStep;
      const radius = i * radiusStep;
      const height = i * heightStep;

      positions.push({
        x: Math.cos(angle) * radius,
        y: height,
        z: Math.sin(angle) * radius,
      });
    }

    return positions;
  }

  /**
   * Map compliance rate to color using gradient
   */
  complianceRateToColor(complianceRate: number): THREE.Color {
    // 0-100 scale
    const normalized = Math.max(0, Math.min(100, complianceRate)) / 100;

    // Red (0%) -> Yellow (50%) -> Green (100%)
    if (normalized < 0.5) {
      // Red to Yellow
      const t = normalized * 2;
      return new THREE.Color().lerpColors(
        new THREE.Color(0xf44336), // Red
        new THREE.Color(0xffeb3b), // Yellow
        t
      );
    } else {
      // Yellow to Green
      const t = (normalized - 0.5) * 2;
      return new THREE.Color().lerpColors(
        new THREE.Color(0xffeb3b), // Yellow
        new THREE.Color(0x4caf50), // Green
        t
      );
    }
  }

  /**
   * Map score to color using D3 viridis scale
   */
  scoreToColor(score: number, min = 0, max = 100): THREE.Color {
    const normalized = (score - min) / (max - min);
    const colorScale = scaleSequential(interpolateViridis).domain([0, 1]);
    const d3Color = colorScale(normalized);
    return new THREE.Color(d3Color);
  }

  /**
   * Map category to color
   */
  categoryToColor(category: string): THREE.Color {
    return this.CATEGORY_COLORS[category as keyof typeof this.CATEGORY_COLORS] || new THREE.Color(0x607d8b);
  }

  /**
   * Map status to color
   */
  statusToColor(status: EvaluationStatus): THREE.Color {
    return this.STATUS_COLORS[status];
  }

  /**
   * Normalize position to fit within bounds
   */
  normalizePosition(position: Position3D, bounds: { min: number; max: number }): Position3D {
    const scale = (bounds.max - bounds.min) / 2;
    const center = (bounds.min + bounds.max) / 2;

    return {
      x: (position.x - center) / scale,
      y: (position.y - center) / scale,
      z: (position.z - center) / scale,
    };
  }

  /**
   * Calculate bounding box for positions
   */
  calculateBoundingBox(positions: Position3D[]): THREE.Box3 {
    const box = new THREE.Box3();

    positions.forEach(pos => {
      box.expandByPoint(new THREE.Vector3(pos.x, pos.y, pos.z));
    });

    return box;
  }

  /**
   * Animate position transition
   */
  lerpPosition(from: Position3D, to: Position3D, t: number): Position3D {
    return {
      x: from.x + (to.x - from.x) * t,
      y: from.y + (to.y - from.y) * t,
      z: from.z + (to.z - from.z) * t,
    };
  }

  /**
   * Convert Position3D to THREE.Vector3
   */
  toVector3(position: Position3D): THREE.Vector3 {
    return new THREE.Vector3(position.x, position.y, position.z);
  }

  /**
   * Convert THREE.Vector3 to Position3D
   */
  fromVector3(vector: THREE.Vector3): Position3D {
    return {
      x: vector.x,
      y: vector.y,
      z: vector.z,
    };
  }
}
