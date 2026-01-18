import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import {
  MeasureAlgorithm,
  PopulationBlock,
  BlockConnection,
  HistoryEntry,
  UndoRedoStack,
  MeasureBuilderState,
} from '../models/measure-builder.model';

/**
 * Service to manage measure algorithm state and operations
 * Handles algorithm building, validation, and history management
 */
@Injectable({
  providedIn: 'root',
})
export class AlgorithmBuilderService {
  private algorithmSubject = new BehaviorSubject<MeasureAlgorithm | null>(null);
  private stateSubject = new BehaviorSubject<MeasureBuilderState | null>(null);
  private undoRedoStack: UndoRedoStack = {
    history: [],
    currentIndex: -1,
    maxSize: 50,
  };

  algorithm$ = this.algorithmSubject.asObservable();
  state$ = this.stateSubject.asObservable();

  constructor() {}

  /**
   * Initialize algorithm with default population structure
   */
  initializeAlgorithm(): MeasureAlgorithm {
    const algorithm: MeasureAlgorithm = {
      initialPopulation: this.createPopulationBlock(
        'initial',
        'Initial Population',
        'All patients meeting inclusion criteria',
        '#1976d2'
      ),
      denominator: this.createPopulationBlock(
        'denominator',
        'Denominator',
        'Patients in the measure population',
        '#388e3c'
      ),
      numerator: this.createPopulationBlock(
        'numerator',
        'Numerator',
        'Patients meeting quality criteria',
        '#f57c00'
      ),
      exclusions: [],
      exceptions: [],
      connections: [],
      compositeWeights: [],
    };

    this.algorithmSubject.next(algorithm);
    return algorithm;
  }

  /**
   * Create a population block with unique ID and default positioning
   */
  private createPopulationBlock(
    type: string,
    label: string,
    description: string,
    color: string,
    position?: { x: number; y: number }
  ): PopulationBlock {
    return {
      id: `block_${type}_${Date.now()}`,
      label,
      description,
      type: type as any,
      condition: '', // Will be filled by user/slider
      color,
      position: position || this.getDefaultPosition(type),
    };
  }

  /**
   * Get default canvas position based on block type
   */
  private getDefaultPosition(type: string): { x: number; y: number } {
    const positions: Record<string, { x: number; y: number }> = {
      initial: { x: 400, y: 50 },
      denominator: { x: 400, y: 200 },
      numerator: { x: 400, y: 350 },
    };
    return positions[type] || { x: 400, y: 150 };
  }

  /**
   * Add exclusion block to algorithm
   */
  addExclusionBlock(label: string = 'Denominator Exclusion'): PopulationBlock {
    const algorithm = this.algorithmSubject.value;
    if (!algorithm) return null as any;

    const exclusionBlock = this.createPopulationBlock(
      'exclusion',
      label,
      'Patients excluded from denominator',
      '#d32f2f',
      { x: 150, y: 200 + (algorithm.exclusions?.length || 0) * 100 }
    );

    if (!algorithm.exclusions) {
      algorithm.exclusions = [];
    }
    algorithm.exclusions.push(exclusionBlock);
    this.algorithmSubject.next(algorithm);
    this.recordHistory('Added exclusion block', algorithm);

    return exclusionBlock;
  }

  /**
   * Add exception block to algorithm
   */
  addExceptionBlock(label: string = 'Denominator Exception'): PopulationBlock {
    const algorithm = this.algorithmSubject.value;
    if (!algorithm) return null as any;

    const exceptionBlock = this.createPopulationBlock(
      'exception',
      label,
      'Patients with valid medical exceptions',
      '#7b1fa2',
      { x: 650, y: 200 + (algorithm.exceptions?.length || 0) * 100 }
    );

    if (!algorithm.exceptions) {
      algorithm.exceptions = [];
    }
    algorithm.exceptions.push(exceptionBlock);
    this.algorithmSubject.next(algorithm);
    this.recordHistory('Added exception block', algorithm);

    return exceptionBlock;
  }

  /**
   * Remove exclusion or exception block
   */
  removeBlock(blockId: string): void {
    const algorithm = this.algorithmSubject.value;
    if (!algorithm) return;

    // Remove from exclusions
    if (algorithm.exclusions) {
      algorithm.exclusions = algorithm.exclusions.filter((b) => b.id !== blockId);
    }

    // Remove from exceptions
    if (algorithm.exceptions) {
      algorithm.exceptions = algorithm.exceptions.filter((b) => b.id !== blockId);
    }

    // Remove related connections
    if (algorithm.connections) {
      algorithm.connections = algorithm.connections.filter(
        (c) => c.sourceBlockId !== blockId && c.targetBlockId !== blockId
      );
    }

    this.algorithmSubject.next(algorithm);
    this.recordHistory('Removed block', algorithm);
  }

  /**
   * Update block position (for canvas drag)
   */
  updateBlockPosition(blockId: string, x: number, y: number): void {
    const algorithm = this.algorithmSubject.value;
    if (!algorithm) return;

    const findAndUpdate = (blocks: PopulationBlock[] | undefined) => {
      if (blocks) {
        const block = blocks.find((b) => b.id === blockId);
        if (block) {
          block.position = { x, y };
          return true;
        }
      }
      return false;
    };

    if (algorithm.initialPopulation.id === blockId) {
      algorithm.initialPopulation.position = { x, y };
    } else if (algorithm.denominator.id === blockId) {
      algorithm.denominator.position = { x, y };
    } else if (algorithm.numerator.id === blockId) {
      algorithm.numerator.position = { x, y };
    } else if (
      !findAndUpdate(algorithm.exclusions) &&
      !findAndUpdate(algorithm.exceptions)
    ) {
      return;
    }

    this.algorithmSubject.next(algorithm);
  }

  /**
   * Update block condition (CQL)
   */
  updateBlockCondition(blockId: string, condition: string): void {
    const algorithm = this.algorithmSubject.value;
    if (!algorithm) return;

    const findAndUpdate = (block: PopulationBlock | undefined) => {
      if (block && block.id === blockId) {
        block.condition = condition;
        return true;
      }
      return false;
    };

    const checkArray = (blocks: PopulationBlock[] | undefined) => {
      if (blocks) {
        const block = blocks.find((b) => b.id === blockId);
        if (block) {
          block.condition = condition;
          return true;
        }
      }
      return false;
    };

    if (
      !findAndUpdate(algorithm.initialPopulation) &&
      !findAndUpdate(algorithm.denominator) &&
      !findAndUpdate(algorithm.numerator) &&
      !checkArray(algorithm.exclusions) &&
      !checkArray(algorithm.exceptions)
    ) {
      return;
    }

    this.algorithmSubject.next(algorithm);
    this.recordHistory('Updated block condition', algorithm);
  }

  /**
   * Add connection between blocks
   */
  addConnection(
    sourceBlockId: string,
    targetBlockId: string,
    connectionType: 'inclusion' | 'exclusion' | 'exception' = 'inclusion'
  ): BlockConnection {
    const algorithm = this.algorithmSubject.value;
    if (!algorithm) return null as any;

    const connection: BlockConnection = {
      id: `conn_${Date.now()}`,
      sourceBlockId,
      targetBlockId,
      connectionType,
    };

    if (!algorithm.connections) {
      algorithm.connections = [];
    }
    algorithm.connections.push(connection);

    this.algorithmSubject.next(algorithm);
    this.recordHistory('Added connection', algorithm);

    return connection;
  }

  /**
   * Remove connection between blocks
   */
  removeConnection(connectionId: string): void {
    const algorithm = this.algorithmSubject.value;
    if (!algorithm || !algorithm.connections) return;

    algorithm.connections = algorithm.connections.filter((c) => c.id !== connectionId);
    this.algorithmSubject.next(algorithm);
    this.recordHistory('Removed connection', algorithm);
  }

  /**
   * Validate algorithm structure
   */
  validateAlgorithm(algorithm: MeasureAlgorithm): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    // Check required population blocks
    if (!algorithm.initialPopulation || !algorithm.initialPopulation.condition) {
      errors.push('Initial Population condition is required');
    }

    if (!algorithm.denominator || !algorithm.denominator.condition) {
      errors.push('Denominator condition is required');
    }

    if (!algorithm.numerator || !algorithm.numerator.condition) {
      errors.push('Numerator condition is required');
    }

    // Check that numerator is subset of denominator
    // (This would require more sophisticated logic analysis)

    // Check for orphaned exclusions/exceptions
    if (algorithm.exclusions?.some((ex) => !ex.condition)) {
      errors.push('All exclusion blocks must have conditions');
    }

    if (algorithm.exceptions?.some((ex) => !ex.condition)) {
      errors.push('All exception blocks must have conditions');
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }

  /**
   * Duplicate a block (useful for creating similar criteria)
   */
  duplicateBlock(blockId: string, offsetX: number = 50, offsetY: number = 50): PopulationBlock | null {
    const algorithm = this.algorithmSubject.value;
    if (!algorithm) return null;

    const findBlock = (blocks: PopulationBlock[] | undefined): PopulationBlock | null => {
      if (blocks) {
        const block = blocks.find((b) => b.id === blockId);
        if (block) return block;
      }
      return null;
    };

    let sourceBlock: PopulationBlock | null = null;

    if (algorithm.initialPopulation.id === blockId) {
      sourceBlock = algorithm.initialPopulation;
    } else if (algorithm.denominator.id === blockId) {
      sourceBlock = algorithm.denominator;
    } else if (algorithm.numerator.id === blockId) {
      sourceBlock = algorithm.numerator;
    } else {
      sourceBlock =
        findBlock(algorithm.exclusions) || findBlock(algorithm.exceptions);
    }

    if (!sourceBlock) return null;

    const newBlock = this.createPopulationBlock(
      sourceBlock.type,
      sourceBlock.label + ' (Copy)',
      sourceBlock.description,
      sourceBlock.color,
      {
        x: sourceBlock.position.x + offsetX,
        y: sourceBlock.position.y + offsetY,
      }
    );

    newBlock.condition = sourceBlock.condition;

    if (sourceBlock.type === 'exclusion') {
      if (!algorithm.exclusions) algorithm.exclusions = [];
      algorithm.exclusions.push(newBlock);
    } else if (sourceBlock.type === 'exception') {
      if (!algorithm.exceptions) algorithm.exceptions = [];
      algorithm.exceptions.push(newBlock);
    }

    this.algorithmSubject.next(algorithm);
    this.recordHistory('Duplicated block', algorithm);

    return newBlock;
  }

  /**
   * Record state change in history for undo/redo
   */
  private recordHistory(description: string, algorithm: MeasureAlgorithm): void {
    // Remove any future history if user made a change after undo
    if (this.undoRedoStack.currentIndex < this.undoRedoStack.history.length - 1) {
      this.undoRedoStack.history = this.undoRedoStack.history.slice(
        0,
        this.undoRedoStack.currentIndex + 1
      );
    }

    const entry: HistoryEntry = {
      id: `hist_${Date.now()}`,
      timestamp: new Date(),
      state: {
        name: '',
        algorithm: JSON.parse(JSON.stringify(algorithm)),
        sliderConfigurations: [],
        currentCql: '',
        isDirty: true,
      },
      description,
    };

    this.undoRedoStack.history.push(entry);
    this.undoRedoStack.currentIndex++;

    // Keep history size under control
    if (this.undoRedoStack.history.length > this.undoRedoStack.maxSize) {
      this.undoRedoStack.history.shift();
      this.undoRedoStack.currentIndex--;
    }
  }

  /**
   * Undo last change
   */
  undo(): boolean {
    if (this.undoRedoStack.currentIndex <= 0) return false;

    this.undoRedoStack.currentIndex--;
    const entry = this.undoRedoStack.history[this.undoRedoStack.currentIndex];
    this.algorithmSubject.next(entry.state.algorithm);

    return true;
  }

  /**
   * Redo last undone change
   */
  redo(): boolean {
    if (this.undoRedoStack.currentIndex >= this.undoRedoStack.history.length - 1) {
      return false;
    }

    this.undoRedoStack.currentIndex++;
    const entry = this.undoRedoStack.history[this.undoRedoStack.currentIndex];
    this.algorithmSubject.next(entry.state.algorithm);

    return true;
  }

  /**
   * Check if undo is available
   */
  canUndo(): boolean {
    return this.undoRedoStack.currentIndex > 0;
  }

  /**
   * Check if redo is available
   */
  canRedo(): boolean {
    return this.undoRedoStack.currentIndex < this.undoRedoStack.history.length - 1;
  }

  /**
   * Get current algorithm
   */
  getCurrentAlgorithm(): MeasureAlgorithm | null {
    return this.algorithmSubject.value;
  }

  /**
   * Get entire measure builder state
   */
  getCurrentState(): MeasureBuilderState | null {
    return this.stateSubject.value;
  }

  /**
   * Update entire state
   */
  updateState(state: Partial<MeasureBuilderState>): void {
    const current = this.stateSubject.value;
    if (current) {
      const updated = { ...current, ...state };
      this.stateSubject.next(updated);
    }
  }

  /**
   * Reset to initial state
   */
  reset(): void {
    this.undoRedoStack = {
      history: [],
      currentIndex: -1,
      maxSize: 50,
    };
    this.algorithmSubject.next(null);
    this.stateSubject.next(null);
  }
}
