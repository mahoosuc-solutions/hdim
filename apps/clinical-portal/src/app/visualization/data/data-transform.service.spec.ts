import { TestBed } from '@angular/core/testing';
import { DataTransformService, EvaluationProgressEvent, Position3D, VisualProperties } from './data-transform.service';
import { QualityMeasureResult } from '../../models/quality-result.model';
import { CqlEvaluation, EvaluationStatus } from '../../models/evaluation.model';
import * as THREE from 'three';

describe('DataTransformService', () => {
  let service: DataTransformService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DataTransformService],
    });

    service = TestBed.inject(DataTransformService);
  });

  describe('initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('transformEvaluationProgress', () => {
    it('should transform evaluation progress to visual properties', () => {
      const event: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'PENDING',
        progress: 50,
        timestamp: new Date().toISOString(),
      };

      const result = service.transformEvaluationProgress(event, 0, 10);

      expect(result).toBeDefined();
      expect(result.position).toBeDefined();
      expect(result.color).toBeInstanceOf(THREE.Color);
      expect(result.size).toBeGreaterThan(0);
      expect(result.opacity).toBeGreaterThan(0);
    });

    it('should position items in circular layout', () => {
      const event: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'PENDING',
        progress: 0,
        timestamp: new Date().toISOString(),
      };

      const result1 = service.transformEvaluationProgress(event, 0, 4);
      const result2 = service.transformEvaluationProgress(event, 1, 4);

      // Different positions for different indices
      expect(result1.position.x).not.toBe(result2.position.x);
    });

    it('should use green color for SUCCESS status', () => {
      const event: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'SUCCESS',
        progress: 100,
        timestamp: new Date().toISOString(),
      };

      const result = service.transformEvaluationProgress(event, 0, 1);

      expect(result.color.getHex()).toBe(0x4caf50); // Green
    });

    it('should use red color for FAILED status', () => {
      const event: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'FAILED',
        progress: 100,
        timestamp: new Date().toISOString(),
      };

      const result = service.transformEvaluationProgress(event, 0, 1);

      expect(result.color.getHex()).toBe(0xf44336); // Red
    });

    it('should interpolate color for PENDING status based on progress', () => {
      const event: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'PENDING',
        progress: 50,
        timestamp: new Date().toISOString(),
      };

      const result = service.transformEvaluationProgress(event, 0, 1);

      // Color should be interpolated between blue and yellow
      expect(result.color.getHex()).not.toBe(0x2196f3); // Not pure blue
      expect(result.color.getHex()).not.toBe(0xffeb3b); // Not pure yellow
    });

    it('should set larger size for SUCCESS status', () => {
      const successEvent: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'SUCCESS',
        progress: 100,
        timestamp: new Date().toISOString(),
      };

      const pendingEvent: EvaluationProgressEvent = {
        patientId: 'patient-2',
        status: 'PENDING',
        progress: 50,
        timestamp: new Date().toISOString(),
      };

      const successResult = service.transformEvaluationProgress(successEvent, 0, 1);
      const pendingResult = service.transformEvaluationProgress(pendingEvent, 0, 1);

      expect(successResult.size).toBeGreaterThan(pendingResult.size);
    });

    it('should set smaller size for FAILED status', () => {
      const failedEvent: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'FAILED',
        progress: 100,
        timestamp: new Date().toISOString(),
      };

      const pendingEvent: EvaluationProgressEvent = {
        patientId: 'patient-2',
        status: 'PENDING',
        progress: 50,
        timestamp: new Date().toISOString(),
      };

      const failedResult = service.transformEvaluationProgress(failedEvent, 0, 1);
      const pendingResult = service.transformEvaluationProgress(pendingEvent, 0, 1);

      expect(failedResult.size).toBeLessThan(pendingResult.size);
    });

    it('should set lower opacity for PENDING status', () => {
      const pendingEvent: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'PENDING',
        progress: 50,
        timestamp: new Date().toISOString(),
      };

      const successEvent: EvaluationProgressEvent = {
        patientId: 'patient-2',
        status: 'SUCCESS',
        progress: 100,
        timestamp: new Date().toISOString(),
      };

      const pendingResult = service.transformEvaluationProgress(pendingEvent, 0, 1);
      const successResult = service.transformEvaluationProgress(successEvent, 0, 1);

      expect(pendingResult.opacity).toBeLessThan(successResult.opacity);
    });

    it('should expand radius as progress increases', () => {
      const event1: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'PENDING',
        progress: 0,
        timestamp: new Date().toISOString(),
      };

      const event2: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'PENDING',
        progress: 100,
        timestamp: new Date().toISOString(),
      };

      const result1 = service.transformEvaluationProgress(event1, 0, 1);
      const result2 = service.transformEvaluationProgress(event2, 0, 1);

      const radius1 = Math.sqrt(result1.position.x ** 2 + result1.position.z ** 2);
      const radius2 = Math.sqrt(result2.position.x ** 2 + result2.position.z ** 2);

      expect(radius2).toBeGreaterThan(radius1);
    });

    it('should increase height based on progress', () => {
      const event1: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'PENDING',
        progress: 0,
        timestamp: new Date().toISOString(),
      };

      const event2: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'PENDING',
        progress: 100,
        timestamp: new Date().toISOString(),
      };

      const result1 = service.transformEvaluationProgress(event1, 0, 1);
      const result2 = service.transformEvaluationProgress(event2, 0, 1);

      expect(result2.position.y).toBeGreaterThan(result1.position.y);
    });
  });

  describe('transformQualityResult', () => {
    it('should transform quality measure result to visual properties', () => {
      const result: QualityMeasureResult = {
        id: 'result-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        measureId: 'CDC-A1C9',
        measureName: 'Diabetes HbA1c Control',
        measureCategory: 'HEDIS',
        measureYear: 2024,
        numeratorCompliant: true,
        denominatorEligible: true,
        complianceRate: 85.5,
        score: 90,
        calculationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        createdBy: 'system',
        version: 1,
      };

      const visual = service.transformQualityResult(result, 0);

      expect(visual).toBeDefined();
      expect(visual.position).toBeDefined();
      expect(visual.color).toBeInstanceOf(THREE.Color);
      expect(visual.size).toBeGreaterThan(0);
      expect(visual.opacity).toBe(1.0);
    });

    it('should use green color for compliant results', () => {
      const result: QualityMeasureResult = {
        id: 'result-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        measureId: 'CDC-A1C9',
        measureName: 'Diabetes HbA1c Control',
        measureCategory: 'HEDIS',
        measureYear: 2024,
        numeratorCompliant: true,
        denominatorEligible: true,
        complianceRate: 85.5,
        score: 90,
        calculationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        createdBy: 'system',
        version: 1,
      };

      const visual = service.transformQualityResult(result, 0);

      expect(visual.color.getHex()).toBe(0x4caf50); // Green
    });

    it('should use orange color for non-compliant results', () => {
      const result: QualityMeasureResult = {
        id: 'result-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        measureId: 'CDC-A1C9',
        measureName: 'Diabetes HbA1c Control',
        measureCategory: 'HEDIS',
        measureYear: 2024,
        numeratorCompliant: false,
        denominatorEligible: true,
        complianceRate: 45.5,
        score: 50,
        calculationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        createdBy: 'system',
        version: 1,
      };

      const visual = service.transformQualityResult(result, 0);

      expect(visual.color.getHex()).toBe(0xff9800); // Orange
    });

    it('should use gray color for not eligible results', () => {
      const result: QualityMeasureResult = {
        id: 'result-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        measureId: 'CDC-A1C9',
        measureName: 'Diabetes HbA1c Control',
        measureCategory: 'HEDIS',
        measureYear: 2024,
        numeratorCompliant: false,
        denominatorEligible: false,
        complianceRate: 0,
        score: 0,
        calculationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        createdBy: 'system',
        version: 1,
      };

      const visual = service.transformQualityResult(result, 0);

      expect(visual.color.getHex()).toBe(0x9e9e9e); // Gray
    });

    it('should scale size based on score', () => {
      const highScore: QualityMeasureResult = {
        id: 'result-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        measureId: 'CDC-A1C9',
        measureName: 'Diabetes HbA1c Control',
        measureCategory: 'HEDIS',
        measureYear: 2024,
        numeratorCompliant: true,
        denominatorEligible: true,
        complianceRate: 95,
        score: 100,
        calculationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        createdBy: 'system',
        version: 1,
      };

      const lowScore: QualityMeasureResult = {
        ...highScore,
        id: 'result-2',
        score: 20,
      };

      const highVisual = service.transformQualityResult(highScore, 0);
      const lowVisual = service.transformQualityResult(lowScore, 0);

      expect(highVisual.size).toBeGreaterThan(lowVisual.size);
    });

    it('should position height based on compliance rate', () => {
      const highCompliance: QualityMeasureResult = {
        id: 'result-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        measureId: 'CDC-A1C9',
        measureName: 'Diabetes HbA1c Control',
        measureCategory: 'HEDIS',
        measureYear: 2024,
        numeratorCompliant: true,
        denominatorEligible: true,
        complianceRate: 90,
        score: 90,
        calculationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        createdBy: 'system',
        version: 1,
      };

      const lowCompliance: QualityMeasureResult = {
        ...highCompliance,
        id: 'result-2',
        complianceRate: 30,
      };

      const highVisual = service.transformQualityResult(highCompliance, 0);
      const lowVisual = service.transformQualityResult(lowCompliance, 0);

      expect(highVisual.position.y).toBeGreaterThan(lowVisual.position.y);
    });
  });

  describe('transformEvaluation', () => {
    it('should transform CQL evaluation to visual properties', () => {
      const evaluation: CqlEvaluation = {
        id: 'eval-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        status: 'SUCCESS',
        durationMs: 1500,
        evaluationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
      };

      const visual = service.transformEvaluation(evaluation, 0);

      expect(visual).toBeDefined();
      expect(visual.position).toBeDefined();
      expect(visual.color).toBeInstanceOf(THREE.Color);
      expect(visual.size).toBeGreaterThan(0);
    });

    it('should position evaluations in grid layout', () => {
      const evaluation: CqlEvaluation = {
        id: 'eval-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        status: 'SUCCESS',
        durationMs: 1000,
        evaluationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
      };

      const visual1 = service.transformEvaluation(evaluation, 0);
      const visual2 = service.transformEvaluation(evaluation, 1);
      const visual11 = service.transformEvaluation(evaluation, 11);

      // Different x positions within same row
      expect(visual1.position.x).not.toBe(visual2.position.x);
      // Different z positions for different rows
      expect(visual1.position.z).not.toBe(visual11.position.z);
    });

    it('should set color based on evaluation status', () => {
      const successEval: CqlEvaluation = {
        id: 'eval-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        status: 'SUCCESS',
        durationMs: 1000,
        evaluationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
      };

      const failedEval: CqlEvaluation = {
        ...successEval,
        id: 'eval-2',
        status: 'FAILED',
      };

      const successVisual = service.transformEvaluation(successEval, 0);
      const failedVisual = service.transformEvaluation(failedEval, 0);

      expect(successVisual.color.getHex()).toBe(0x4caf50); // Green
      expect(failedVisual.color.getHex()).toBe(0xf44336); // Red
    });

    it('should scale size based on duration', () => {
      const fastEval: CqlEvaluation = {
        id: 'eval-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        status: 'SUCCESS',
        durationMs: 500,
        evaluationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
      };

      const slowEval: CqlEvaluation = {
        ...fastEval,
        id: 'eval-2',
        durationMs: 5000,
      };

      const fastVisual = service.transformEvaluation(fastEval, 0);
      const slowVisual = service.transformEvaluation(slowEval, 0);

      expect(slowVisual.size).toBeGreaterThan(fastVisual.size);
    });

    it('should cap size at maximum value', () => {
      const verySlowEval: CqlEvaluation = {
        id: 'eval-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        status: 'SUCCESS',
        durationMs: 10000,
        evaluationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
      };

      const visual = service.transformEvaluation(verySlowEval, 0);

      expect(visual.size).toBeLessThanOrEqual(2.0);
    });

    it('should set height based on log of duration', () => {
      const eval1: CqlEvaluation = {
        id: 'eval-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        status: 'SUCCESS',
        durationMs: 100,
        evaluationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
      };

      const eval2: CqlEvaluation = {
        ...eval1,
        id: 'eval-2',
        durationMs: 10000,
      };

      const visual1 = service.transformEvaluation(eval1, 0);
      const visual2 = service.transformEvaluation(eval2, 0);

      expect(visual2.position.y).toBeGreaterThan(visual1.position.y);
    });

    it('should handle evaluations without duration', () => {
      const evaluation: CqlEvaluation = {
        id: 'eval-1',
        tenantId: 'tenant-1',
        patientId: 'patient-1',
        status: 'PENDING',
        evaluationDate: new Date().toISOString(),
        createdAt: new Date().toISOString(),
      };

      const visual = service.transformEvaluation(evaluation, 0);

      expect(visual.position.y).toBe(0);
      expect(visual.size).toBe(1.0);
    });
  });

  describe('createScatterPlotPositions', () => {
    it('should create positions for scatter plot', () => {
      const results: QualityMeasureResult[] = [
        {
          id: 'result-1',
          tenantId: 'tenant-1',
          patientId: 'patient-1',
          measureId: 'CDC-A1C9',
          measureName: 'Test',
          measureCategory: 'HEDIS',
          measureYear: 2024,
          numeratorCompliant: true,
          denominatorEligible: true,
          complianceRate: 80,
          score: 85,
          calculationDate: new Date().toISOString(),
          createdAt: new Date().toISOString(),
          createdBy: 'system',
          version: 1,
        },
        {
          id: 'result-2',
          tenantId: 'tenant-1',
          patientId: 'patient-2',
          measureId: 'CDC-A1C9',
          measureName: 'Test',
          measureCategory: 'HEDIS',
          measureYear: 2024,
          numeratorCompliant: false,
          denominatorEligible: true,
          complianceRate: 40,
          score: 50,
          calculationDate: new Date().toISOString(),
          createdAt: new Date().toISOString(),
          createdBy: 'system',
          version: 1,
        },
      ];

      const positions = service.createScatterPlotPositions(
        results,
        'complianceRate',
        'score',
        'measureYear'
      );

      expect(positions.length).toBe(2);
      expect(positions[0]).toBeDefined();
      expect(positions[1]).toBeDefined();
    });

    it('should return empty array for empty input', () => {
      const positions = service.createScatterPlotPositions(
        [],
        'complianceRate',
        'score',
        'measureYear'
      );

      expect(positions).toEqual([]);
    });

    it('should scale positions to defined range', () => {
      const results: QualityMeasureResult[] = [
        {
          id: 'result-1',
          tenantId: 'tenant-1',
          patientId: 'patient-1',
          measureId: 'CDC-A1C9',
          measureName: 'Test',
          measureCategory: 'HEDIS',
          measureYear: 2024,
          numeratorCompliant: true,
          denominatorEligible: true,
          complianceRate: 100,
          score: 100,
          calculationDate: new Date().toISOString(),
          createdAt: new Date().toISOString(),
          createdBy: 'system',
          version: 1,
        },
      ];

      const positions = service.createScatterPlotPositions(
        results,
        'complianceRate',
        'score',
        'measureYear'
      );

      // Positions should be within expected ranges
      expect(positions[0].x).toBeGreaterThanOrEqual(-50);
      expect(positions[0].x).toBeLessThanOrEqual(50);
      expect(positions[0].y).toBeGreaterThanOrEqual(0);
      expect(positions[0].y).toBeLessThanOrEqual(50);
    });

    it('should fallback to zero for non-numeric metrics', () => {
      const results: QualityMeasureResult[] = [
        {
          id: 'result-1',
          tenantId: 'tenant-1',
          patientId: 'patient-1',
          measureId: 'CDC-A1C9',
          measureName: 'Test',
          measureCategory: 'HEDIS',
          measureYear: 2024,
          numeratorCompliant: true,
          denominatorEligible: true,
          complianceRate: undefined as any,
          score: undefined as any,
          calculationDate: new Date().toISOString(),
          createdAt: new Date().toISOString(),
          createdBy: 'system',
          version: 1,
        },
      ];

      const positions = service.createScatterPlotPositions(
        results,
        'complianceRate',
        'score',
        'measureYear'
      );

      expect(positions[0].x).toBeDefined();
      expect(positions[0].y).toBeDefined();
      expect(positions[0].z).toBeDefined();
    });
  });

  describe('createRadialLayout', () => {
    it('should create radial layout positions', () => {
      const positions = service.createRadialLayout(8, 50, 1);

      expect(positions.length).toBe(8);
      positions.forEach((pos) => {
        expect(pos.x).toBeDefined();
        expect(pos.y).toBeDefined();
        expect(pos.z).toBeDefined();
      });
    });

    it('should arrange items in circular pattern', () => {
      const positions = service.createRadialLayout(4, 50, 1);

      // Items should be evenly distributed around circle
      const angles = positions.map((pos) => Math.atan2(pos.z, pos.x));
      const angleStep = (Math.PI * 2) / 4;

      for (let i = 1; i < angles.length; i++) {
        const diff = Math.abs(angles[i] - angles[i - 1]);
        expect(diff).toBeCloseTo(angleStep, 1);
      }
    });

    it('should create multiple layers', () => {
      const positions = service.createRadialLayout(20, 50, 2);

      const heights = [...new Set(positions.map((pos) => pos.y))];
      expect(heights.length).toBeGreaterThan(1);
    });

    it('should scale radius for different layers', () => {
      const positions = service.createRadialLayout(20, 50, 2);

      // Items in different layers should have different radii
      const layer1Items = positions.slice(0, 10);
      const layer2Items = positions.slice(10);

      const layer1Radius = Math.sqrt(layer1Items[0].x ** 2 + layer1Items[0].z ** 2);
      const layer2Radius = Math.sqrt(layer2Items[0].x ** 2 + layer2Items[0].z ** 2);

      expect(layer1Radius).not.toBe(layer2Radius);
    });
  });

  describe('createGridLayout', () => {
    it('should create grid layout positions', () => {
      const positions = service.createGridLayout(9, 5);

      expect(positions.length).toBe(9);
      positions.forEach((pos) => {
        expect(pos.x).toBeDefined();
        expect(pos.y).toBe(0);
        expect(pos.z).toBeDefined();
      });
    });

    it('should arrange items in square grid', () => {
      const positions = service.createGridLayout(9, 5);

      // Should form 3x3 grid
      const xPositions = [...new Set(positions.map((pos) => pos.x))];
      const zPositions = [...new Set(positions.map((pos) => pos.z))];

      expect(xPositions.length).toBe(3);
      expect(zPositions.length).toBe(3);
    });

    it('should respect spacing parameter', () => {
      const spacing = 10;
      const positions = service.createGridLayout(4, spacing);

      // Calculate distance between adjacent items
      const distance = Math.abs(positions[1].x - positions[0].x);
      expect(distance).toBe(spacing);
    });

    it('should center grid around origin', () => {
      const positions = service.createGridLayout(9, 5);

      const avgX = positions.reduce((sum, pos) => sum + pos.x, 0) / positions.length;
      const avgZ = positions.reduce((sum, pos) => sum + pos.z, 0) / positions.length;

      expect(avgX).toBeCloseTo(0, 0);
      expect(avgZ).toBeCloseTo(0, 0);
    });

    it('should return empty grid layout for zero count', () => {
      const positions = service.createGridLayout(0, 5);
      expect(positions).toEqual([]);
    });
  });

  describe('createSpiralLayout', () => {
    it('should create spiral layout positions', () => {
      const positions = service.createSpiralLayout(10, 2, 0.5);

      expect(positions.length).toBe(10);
      positions.forEach((pos) => {
        expect(pos.x).toBeDefined();
        expect(pos.y).toBeDefined();
        expect(pos.z).toBeDefined();
      });
    });

    it('should increase radius along spiral', () => {
      const positions = service.createSpiralLayout(10, 2, 0.5);

      const radius1 = Math.sqrt(positions[0].x ** 2 + positions[0].z ** 2);
      const radius5 = Math.sqrt(positions[4].x ** 2 + positions[4].z ** 2);
      const radius10 = Math.sqrt(positions[9].x ** 2 + positions[9].z ** 2);

      expect(radius5).toBeGreaterThan(radius1);
      expect(radius10).toBeGreaterThan(radius5);
    });

    it('should increase height along spiral', () => {
      const positions = service.createSpiralLayout(10, 2, 0.5);

      expect(positions[5].y).toBeGreaterThan(positions[0].y);
      expect(positions[9].y).toBeGreaterThan(positions[5].y);
    });

    it('should return empty spiral layout for zero count', () => {
      const positions = service.createSpiralLayout(0, 2, 0.5);
      expect(positions).toEqual([]);
    });
  });

  describe('color mapping functions', () => {
    describe('complianceRateToColor', () => {
      it('should map low compliance to red', () => {
        const color = service.complianceRateToColor(0);
        expect(color.getHex()).toBe(0xf44336); // Red
      });

      it('should map medium compliance to yellow', () => {
        const color = service.complianceRateToColor(50);
        expect(color.getHex()).toBe(0xffeb3b); // Yellow
      });

      it('should map high compliance to green', () => {
        const color = service.complianceRateToColor(100);
        expect(color.getHex()).toBe(0x4caf50); // Green
      });

      it('should interpolate colors smoothly', () => {
        const color25 = service.complianceRateToColor(25);
        const color75 = service.complianceRateToColor(75);

        expect(color25.getHex()).not.toBe(color75.getHex());
      });

      it('should clamp values below 0', () => {
        const color = service.complianceRateToColor(-10);
        expect(color.getHex()).toBe(0xf44336); // Red (0%)
      });

      it('should clamp values above 100', () => {
        const color = service.complianceRateToColor(150);
        expect(color.getHex()).toBe(0x4caf50); // Green (100%)
      });
    });

    describe('scoreToColor', () => {
      it('should map score to viridis color scale', () => {
        const color = service.scoreToColor(50, 0, 100);
        expect(color).toBeInstanceOf(THREE.Color);
      });

      it('should handle custom min/max ranges', () => {
        const color1 = service.scoreToColor(50, 0, 100);
        const color2 = service.scoreToColor(500, 0, 1000);

        // Both should map to middle of scale
        expect(color1.getHex()).toBe(color2.getHex());
      });
    });

    describe('categoryToColor', () => {
      it('should map HEDIS to blue', () => {
        const color = service.categoryToColor('HEDIS');
        expect(color.getHex()).toBe(0x2196f3); // Blue
      });

      it('should map CMS to purple', () => {
        const color = service.categoryToColor('CMS');
        expect(color.getHex()).toBe(0x9c27b0); // Purple
      });

      it('should map CUSTOM to teal', () => {
        const color = service.categoryToColor('CUSTOM');
        expect(color.getHex()).toBe(0x009688); // Teal
      });

      it('should return default color for unknown category', () => {
        const color = service.categoryToColor('UNKNOWN');
        expect(color.getHex()).toBe(0x607d8b); // Gray
      });
    });

    describe('statusToColor', () => {
      it('should map SUCCESS to green', () => {
        const color = service.statusToColor('SUCCESS');
        expect(color.getHex()).toBe(0x4caf50); // Green
      });

      it('should map FAILED to red', () => {
        const color = service.statusToColor('FAILED');
        expect(color.getHex()).toBe(0xf44336); // Red
      });

      it('should map PENDING to orange', () => {
        const color = service.statusToColor('PENDING');
        expect(color.getHex()).toBe(0xff9800); // Orange
      });
    });
  });

  describe('position utilities', () => {
    describe('normalizePosition', () => {
      it('should normalize position to bounds', () => {
        const position: Position3D = { x: 10, y: 20, z: 30 };
        const bounds = { min: 0, max: 100 };

        const normalized = service.normalizePosition(position, bounds);

        expect(normalized.x).toBeLessThan(position.x);
        expect(normalized.y).toBeLessThan(position.y);
        expect(normalized.z).toBeLessThan(position.z);
      });

      it('should center around origin', () => {
        const position: Position3D = { x: 50, y: 50, z: 50 };
        const bounds = { min: 0, max: 100 };

        const normalized = service.normalizePosition(position, bounds);

        expect(normalized.x).toBeCloseTo(0, 5);
        expect(normalized.y).toBeCloseTo(0, 5);
        expect(normalized.z).toBeCloseTo(0, 5);
      });
    });

    describe('calculateBoundingBox', () => {
      it('should calculate bounding box for positions', () => {
        const positions: Position3D[] = [
          { x: 0, y: 0, z: 0 },
          { x: 10, y: 10, z: 10 },
          { x: -5, y: -5, z: -5 },
        ];

        const box = service.calculateBoundingBox(positions);

        expect(box.min.x).toBe(-5);
        expect(box.max.x).toBe(10);
        expect(box.min.y).toBe(-5);
        expect(box.max.y).toBe(10);
      });
    });

    describe('lerpPosition', () => {
      it('should interpolate between positions', () => {
        const from: Position3D = { x: 0, y: 0, z: 0 };
        const to: Position3D = { x: 10, y: 10, z: 10 };

        const mid = service.lerpPosition(from, to, 0.5);

        expect(mid.x).toBe(5);
        expect(mid.y).toBe(5);
        expect(mid.z).toBe(5);
      });

      it('should return from position at t=0', () => {
        const from: Position3D = { x: 0, y: 0, z: 0 };
        const to: Position3D = { x: 10, y: 10, z: 10 };

        const result = service.lerpPosition(from, to, 0);

        expect(result).toEqual(from);
      });

      it('should return to position at t=1', () => {
        const from: Position3D = { x: 0, y: 0, z: 0 };
        const to: Position3D = { x: 10, y: 10, z: 10 };

        const result = service.lerpPosition(from, to, 1);

        expect(result).toEqual(to);
      });
    });
  });

  describe('vector conversion', () => {
    describe('toVector3', () => {
      it('should convert Position3D to THREE.Vector3', () => {
        const position: Position3D = { x: 10, y: 20, z: 30 };

        const vector = service.toVector3(position);

        expect(vector).toBeInstanceOf(THREE.Vector3);
        expect(vector.x).toBe(10);
        expect(vector.y).toBe(20);
        expect(vector.z).toBe(30);
      });
    });

    describe('fromVector3', () => {
      it('should convert THREE.Vector3 to Position3D', () => {
        const vector = new THREE.Vector3(10, 20, 30);

        const position = service.fromVector3(vector);

        expect(position.x).toBe(10);
        expect(position.y).toBe(20);
        expect(position.z).toBe(30);
      });
    });

    it('should be reversible', () => {
      const original: Position3D = { x: 10, y: 20, z: 30 };

      const vector = service.toVector3(original);
      const converted = service.fromVector3(vector);

      expect(converted).toEqual(original);
    });
  });

  describe('edge cases', () => {
    it('should handle zero values', () => {
      const event: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'PENDING',
        progress: 0,
        timestamp: new Date().toISOString(),
      };

      const result = service.transformEvaluationProgress(event, 0, 1);

      expect(result).toBeDefined();
      expect(result.size).toBeGreaterThan(0);
    });

    it('should handle very large values', () => {
      const event: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'PENDING',
        progress: 1000000,
        timestamp: new Date().toISOString(),
      };

      const result = service.transformEvaluationProgress(event, 0, 1);

      expect(result).toBeDefined();
    });

    it('should handle negative indices gracefully', () => {
      const event: EvaluationProgressEvent = {
        patientId: 'patient-1',
        status: 'PENDING',
        progress: 50,
        timestamp: new Date().toISOString(),
      };

      const result = service.transformEvaluationProgress(event, -1, 10);

      expect(result).toBeDefined();
    });

    it('should handle single item layouts', () => {
      const positions = service.createRadialLayout(1);

      expect(positions.length).toBe(1);
      expect(positions[0]).toBeDefined();
    });

    it('should handle empty metric values in scatter plot', () => {
      const results: QualityMeasureResult[] = [
        {
          id: 'result-1',
          tenantId: 'tenant-1',
          patientId: 'patient-1',
          measureId: 'CDC-A1C9',
          measureName: 'Test',
          measureCategory: 'HEDIS',
          measureYear: 2024,
          numeratorCompliant: true,
          denominatorEligible: true,
          complianceRate: 0,
          score: 0,
          calculationDate: new Date().toISOString(),
          createdAt: new Date().toISOString(),
          createdBy: 'system',
          version: 1,
        },
      ];

      const positions = service.createScatterPlotPositions(
        results,
        'complianceRate',
        'score',
        'measureYear'
      );

      expect(positions.length).toBe(1);
      expect(positions[0]).toBeDefined();
    });
  });
});
