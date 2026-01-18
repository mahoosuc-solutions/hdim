/**
 * Algorithm Renderer Strategy
 *
 * Adaptive rendering strategy that automatically selects between SVG and Canvas
 * based on algorithm complexity (block count and connection count).
 *
 * Strategy:
 * - < 100 blocks:  Use SVG (better accessibility, text rendering)
 * - 100-150 blocks: Evaluate performance, may switch to Canvas
 * - 150+ blocks:   Use Canvas (2-3x performance improvement)
 * - 500+ blocks:   Canvas required (SVG would timeout)
 */

export interface RenderingPerformance {
  renderTime: number;
  blockCount: number;
  connectionCount: number;
  recommendedStrategy: 'svg' | 'canvas';
  reason: string;
}

export interface RenderingConfig {
  svgThreshold: number;         // Switch to Canvas at this block count
  canvasThreshold: number;      // Minimum blocks for Canvas recommendation
  performanceBudget: number;    // Maximum acceptable render time (ms)
}

const DEFAULT_CONFIG: RenderingConfig = {
  svgThreshold: 150,      // Use Canvas for 150+ blocks
  canvasThreshold: 100,   // Evaluate Canvas starting at 100 blocks
  performanceBudget: 50   // Target <50ms render time
};

export class AlgorithmRendererStrategy {
  private config: RenderingConfig;
  private performanceHistory: RenderingPerformance[] = [];

  constructor(config: Partial<RenderingConfig> = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config };
  }

  /**
   * Determine optimal rendering strategy based on algorithm complexity
   */
  determineStrategy(blockCount: number, connectionCount: number): 'svg' | 'canvas' {
    // Always use Canvas for very large algorithms
    if (blockCount >= this.config.svgThreshold) {
      return 'canvas';
    }

    // Evaluate based on connection complexity
    const totalElements = blockCount + connectionCount;

    // Canvas recommended for complex algorithms (many connections)
    if (totalElements > this.config.canvasThreshold * 2) {
      return 'canvas';
    }

    // Default to SVG for accessibility and text rendering
    return 'svg';
  }

  /**
   * Record rendering performance and adjust strategy
   */
  recordPerformance(
    renderTime: number,
    blockCount: number,
    connectionCount: number,
    strategy: 'svg' | 'canvas'
  ): void {
    const perf: RenderingPerformance = {
      renderTime,
      blockCount,
      connectionCount,
      recommendedStrategy: this.determineStrategy(blockCount, connectionCount),
      reason: this.generateReason(renderTime, blockCount, strategy)
    };

    this.performanceHistory.push(perf);

    // Keep only last 100 measurements
    if (this.performanceHistory.length > 100) {
      this.performanceHistory.shift();
    }
  }

  /**
   * Get rendering recommendation based on current algorithm
   */
  getRecommendation(blockCount: number, connectionCount: number): {
    strategy: 'svg' | 'canvas';
    reason: string;
    expectedPerformance: number;
  } {
    const strategy = this.determineStrategy(blockCount, connectionCount);
    const expectedPerformance = this.estimateRenderTime(blockCount, strategy);

    return {
      strategy,
      reason: this.generateReason(expectedPerformance, blockCount, strategy),
      expectedPerformance
    };
  }

  /**
   * Estimate render time based on historical data
   */
  private estimateRenderTime(blockCount: number, strategy: 'svg' | 'canvas'): number {
    // Filter history for same strategy
    const relevantHistory = this.performanceHistory.filter(
      p => p.recommendedStrategy === strategy
    );

    if (relevantHistory.length === 0) {
      // Use baseline estimates
      return strategy === 'svg'
        ? Math.min(blockCount, 50) // SVG: ~0.5ms per block, capped at 50ms
        : Math.min(blockCount * 0.3, 80); // Canvas: ~0.3ms per block, capped at 80ms
    }

    // Calculate average render time per block
    const avgTimePerBlock = relevantHistory.reduce((sum, p) => {
      return sum + (p.renderTime / p.blockCount);
    }, 0) / relevantHistory.length;

    return Math.min(blockCount * avgTimePerBlock, 500); // Cap at 500ms
  }

  /**
   * Generate human-readable reason for strategy choice
   */
  private generateReason(renderTime: number, blockCount: number, strategy: 'svg' | 'canvas'): string {
    if (strategy === 'canvas') {
      return `Canvas selected for ${blockCount} blocks (expected ${renderTime.toFixed(0)}ms render time)`;
    }

    if (renderTime > this.config.performanceBudget) {
      return `SVG performance degrading (${renderTime.toFixed(0)}ms > ${this.config.performanceBudget}ms budget)`;
    }

    return `SVG optimal for ${blockCount} blocks (${renderTime.toFixed(0)}ms render time)`;
  }

  /**
   * Get performance metrics
   */
  getMetrics(): {
    averageRenderTime: number;
    maxRenderTime: number;
    minRenderTime: number;
    recordCount: number;
    svgAveragePerBlock: number;
    canvasAveragePerBlock: number;
  } {
    if (this.performanceHistory.length === 0) {
      return {
        averageRenderTime: 0,
        maxRenderTime: 0,
        minRenderTime: 0,
        recordCount: 0,
        svgAveragePerBlock: 0,
        canvasAveragePerBlock: 0
      };
    }

    const allRenderTimes = this.performanceHistory.map(p => p.renderTime);
    const averageRenderTime = allRenderTimes.reduce((a, b) => a + b, 0) / allRenderTimes.length;

    const svgPerformance = this.performanceHistory.filter(p => p.recommendedStrategy === 'svg');
    const canvasPerformance = this.performanceHistory.filter(p => p.recommendedStrategy === 'canvas');

    const svgAveragePerBlock = svgPerformance.length > 0
      ? svgPerformance.reduce((sum, p) => sum + (p.renderTime / p.blockCount), 0) / svgPerformance.length
      : 0;

    const canvasAveragePerBlock = canvasPerformance.length > 0
      ? canvasPerformance.reduce((sum, p) => sum + (p.renderTime / p.blockCount), 0) / canvasPerformance.length
      : 0;

    return {
      averageRenderTime,
      maxRenderTime: Math.max(...allRenderTimes),
      minRenderTime: Math.min(...allRenderTimes),
      recordCount: this.performanceHistory.length,
      svgAveragePerBlock,
      canvasAveragePerBlock
    };
  }

  /**
   * Reset performance history
   */
  reset(): void {
    this.performanceHistory = [];
  }

  /**
   * Get performance report
   */
  getReport(): string {
    const metrics = this.getMetrics();
    const lastPerf = this.performanceHistory[this.performanceHistory.length - 1];

    let report = `\n${'='.repeat(60)}\nRENDERING STRATEGY REPORT\n${'='.repeat(60)}\n\n`;
    report += `Current Strategy: ${lastPerf?.recommendedStrategy.toUpperCase() || 'N/A'}\n`;
    report += `Reason: ${lastPerf?.reason || 'No data'}\n\n`;

    report += `Performance Metrics:\n`;
    report += `  Average Render Time: ${metrics.averageRenderTime.toFixed(2)}ms\n`;
    report += `  Min Render Time: ${metrics.minRenderTime.toFixed(2)}ms\n`;
    report += `  Max Render Time: ${metrics.maxRenderTime.toFixed(2)}ms\n`;
    report += `  Measurements: ${metrics.recordCount}\n\n`;

    report += `Per-Block Performance:\n`;
    report += `  SVG: ${metrics.svgAveragePerBlock.toFixed(3)}ms/block\n`;
    report += `  Canvas: ${metrics.canvasAveragePerBlock.toFixed(3)}ms/block\n`;

    const improvement = ((metrics.svgAveragePerBlock - metrics.canvasAveragePerBlock) / metrics.svgAveragePerBlock * 100);
    if (improvement > 0) {
      report += `  Canvas Improvement: ${improvement.toFixed(1)}% faster\n`;
    }

    report += `\n${'='.repeat(60)}\n`;

    return report;
  }
}
