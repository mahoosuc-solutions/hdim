/**
 * Tests for BatchProgressBar component
 * Following TDD approach - tests written first
 */

import { describe, it, expect } from 'vitest';
import { renderWithTheme, createMockBatchProgress, screen } from '../../test/test-utils';
import { BatchProgressBar } from '../BatchProgressBar';

describe('BatchProgressBar', () => {
  it('renders with 0% progress', () => {
    const progress = createMockBatchProgress({
      completedCount: 0,
      percentComplete: 0,
      pendingCount: 100,
    });

    renderWithTheme(<BatchProgressBar progress={progress} />);

    expect(screen.getByText('0%')).toBeInTheDocument();
    expect(screen.getByText('0 / 100')).toBeInTheDocument();
  });

  it('displays correct percentage (45%)', () => {
    const progress = createMockBatchProgress({
      completedCount: 45,
      percentComplete: 45.0,
      totalPatients: 100,
    });

    renderWithTheme(<BatchProgressBar progress={progress} />);

    expect(screen.getByText('45%')).toBeInTheDocument();
  });

  it('shows completed/total counts (45/100)', () => {
    const progress = createMockBatchProgress({
      completedCount: 45,
      totalPatients: 100,
    });

    renderWithTheme(<BatchProgressBar progress={progress} />);

    expect(screen.getByText('45 / 100')).toBeInTheDocument();
  });

  it('displays throughput (3.6 eval/s)', () => {
    const progress = createMockBatchProgress({
      currentThroughput: 3.6,
    });

    renderWithTheme(<BatchProgressBar progress={progress} />);

    expect(screen.getByText(/3\.6 eval\/s/)).toBeInTheDocument();
  });

  it('displays ETA (15s)', () => {
    const progress = createMockBatchProgress({
      estimatedTimeRemainingMs: 15278, // ~15 seconds
    });

    renderWithTheme(<BatchProgressBar progress={progress} />);

    // Should display ETA in seconds or minutes
    expect(screen.getByText(/ETA:/)).toBeInTheDocument();
    expect(screen.getByText(/15s/)).toBeInTheDocument();
  });

  it('displays ETA in minutes for longer durations', () => {
    const progress = createMockBatchProgress({
      estimatedTimeRemainingMs: 125000, // ~2 minutes
    });

    renderWithTheme(<BatchProgressBar progress={progress} />);

    expect(screen.getByText(/ETA:/)).toBeInTheDocument();
    expect(screen.getByText(/2m 5s/)).toBeInTheDocument();
  });

  it('applies correct color based on progress - in progress (blue)', () => {
    const progress = createMockBatchProgress({
      completedCount: 45,
      percentComplete: 45.0,
      totalPatients: 100,
    });

    const { container } = renderWithTheme(<BatchProgressBar progress={progress} />);

    // MUI LinearProgress with primary color (blue) for in-progress
    const progressBar = container.querySelector('.MuiLinearProgress-colorPrimary');
    expect(progressBar).toBeInTheDocument();
  });

  it('applies correct color based on progress - complete (green)', () => {
    const progress = createMockBatchProgress({
      completedCount: 100,
      percentComplete: 100.0,
      totalPatients: 100,
      pendingCount: 0,
    });

    const { container } = renderWithTheme(<BatchProgressBar progress={progress} />);

    // MUI LinearProgress with success color (green) for complete
    const progressBar = container.querySelector('.MuiLinearProgress-colorSuccess');
    expect(progressBar).toBeInTheDocument();
  });

  it('applies correct color based on progress - errors (warning)', () => {
    const progress = createMockBatchProgress({
      completedCount: 50,
      percentComplete: 50.0,
      totalPatients: 100,
      failedCount: 25, // 50% failure rate
      successCount: 25,
    });

    const { container } = renderWithTheme(<BatchProgressBar progress={progress} />);

    // MUI LinearProgress with warning color for high error rate
    const progressBar = container.querySelector('.MuiLinearProgress-colorWarning');
    expect(progressBar).toBeInTheDocument();
  });

  it('updates when props change', () => {
    const initialProgress = createMockBatchProgress({
      completedCount: 45,
      percentComplete: 45.0,
      totalPatients: 100,
    });

    const { rerender } = renderWithTheme(<BatchProgressBar progress={initialProgress} />);

    expect(screen.getByText('45%')).toBeInTheDocument();
    expect(screen.getByText('45 / 100')).toBeInTheDocument();

    // Update progress
    const updatedProgress = createMockBatchProgress({
      completedCount: 75,
      percentComplete: 75.0,
      totalPatients: 100,
    });

    rerender(
      <BatchProgressBar progress={updatedProgress} />
    );

    expect(screen.getByText('75%')).toBeInTheDocument();
    expect(screen.getByText('75 / 100')).toBeInTheDocument();
  });

  it('handles zero throughput gracefully', () => {
    const progress = createMockBatchProgress({
      currentThroughput: 0,
    });

    renderWithTheme(<BatchProgressBar progress={progress} />);

    expect(screen.getByText(/0\.0 eval\/s/)).toBeInTheDocument();
  });

  it('displays measure name', () => {
    const progress = createMockBatchProgress({
      measureName: 'Comprehensive Diabetes Care',
    });

    renderWithTheme(<BatchProgressBar progress={progress} />);

    expect(screen.getByText('Comprehensive Diabetes Care')).toBeInTheDocument();
  });

  it('shows success and failed counts', () => {
    const progress = createMockBatchProgress({
      successCount: 43,
      failedCount: 2,
      completedCount: 45,
    });

    renderWithTheme(<BatchProgressBar progress={progress} />);

    expect(screen.getByText(/Success: 43/)).toBeInTheDocument();
    expect(screen.getByText(/Failed: 2/)).toBeInTheDocument();
  });
});
