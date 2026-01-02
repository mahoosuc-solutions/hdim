/**
 * Tests for ComplianceGauge component
 * Following TDD approach - tests written first
 */

import { describe, it, expect } from 'vitest';
import { renderWithTheme, screen } from '../../test/test-utils';
import { ComplianceGauge } from '../ComplianceGauge';

describe('ComplianceGauge', () => {
  it('renders with 0% compliance', () => {
    renderWithTheme(<ComplianceGauge complianceRate={0} />);

    expect(screen.getByText(/0\.0%/)).toBeInTheDocument();
  });

  it('displays correct percentage (73.7%)', () => {
    renderWithTheme(<ComplianceGauge complianceRate={73.7} />);

    expect(screen.getByText('73.7%')).toBeInTheDocument();
  });

  it('applies red color for low compliance (<50%)', () => {
    const { container } = renderWithTheme(<ComplianceGauge complianceRate={35} />);

    // MUI CircularProgress with error color (red) for low compliance
    const progressBar = container.querySelector('.MuiCircularProgress-colorError');
    expect(progressBar).toBeInTheDocument();
  });

  it('applies orange color for medium compliance (50-75%)', () => {
    const { container } = renderWithTheme(<ComplianceGauge complianceRate={65} />);

    // MUI CircularProgress with warning color (orange) for medium compliance
    const progressBar = container.querySelector('.MuiCircularProgress-colorWarning');
    expect(progressBar).toBeInTheDocument();
  });

  it('applies blue color for good compliance (75-90%)', () => {
    const { container } = renderWithTheme(<ComplianceGauge complianceRate={82} />);

    // MUI CircularProgress with info color (blue) for good compliance
    const progressBar = container.querySelector('.MuiCircularProgress-colorInfo');
    expect(progressBar).toBeInTheDocument();
  });

  it('applies green color for excellent compliance (>90%)', () => {
    const { container } = renderWithTheme(<ComplianceGauge complianceRate={95} />);

    // MUI CircularProgress with success color (green) for excellent compliance
    const progressBar = container.querySelector('.MuiCircularProgress-colorSuccess');
    expect(progressBar).toBeInTheDocument();
  });

  it('shows denominator/numerator counts below gauge', () => {
    renderWithTheme(
      <ComplianceGauge
        complianceRate={73.7}
        numerator={28}
        denominator={38}
      />
    );

    expect(screen.getByText('28 / 38')).toBeInTheDocument();
  });

  it('displays optional label', () => {
    renderWithTheme(
      <ComplianceGauge
        complianceRate={73.7}
        label="Compliance Rate"
      />
    );

    expect(screen.getByText('Compliance Rate')).toBeInTheDocument();
  });

  it('updates when props change', () => {
    const { rerender } = renderWithTheme(<ComplianceGauge complianceRate={45} />);

    expect(screen.getByText(/45\.0%/)).toBeInTheDocument();

    // Update compliance rate
    rerender(<ComplianceGauge complianceRate={85} />);

    expect(screen.getByText(/85\.0%/)).toBeInTheDocument();
  });

  it('handles edge case at 50% (boundary between red and orange)', () => {
    const { container } = renderWithTheme(<ComplianceGauge complianceRate={50} />);

    // At exactly 50%, should be warning (orange)
    const progressBar = container.querySelector('.MuiCircularProgress-colorWarning');
    expect(progressBar).toBeInTheDocument();
    expect(screen.getByText(/50\.0%/)).toBeInTheDocument();
  });

  it('handles edge case at 75% (boundary between orange and blue)', () => {
    const { container } = renderWithTheme(<ComplianceGauge complianceRate={75} />);

    // At exactly 75%, should be info (blue)
    const progressBar = container.querySelector('.MuiCircularProgress-colorInfo');
    expect(progressBar).toBeInTheDocument();
    expect(screen.getByText(/75\.0%/)).toBeInTheDocument();
  });

  it('handles edge case at 90% (boundary between blue and green)', () => {
    const { container } = renderWithTheme(<ComplianceGauge complianceRate={90} />);

    // At exactly 90%, should be success (green)
    const progressBar = container.querySelector('.MuiCircularProgress-colorSuccess');
    expect(progressBar).toBeInTheDocument();
    expect(screen.getByText(/90\.0%/)).toBeInTheDocument();
  });

  it('handles 100% compliance', () => {
    const { container } = renderWithTheme(<ComplianceGauge complianceRate={100} />);

    const progressBar = container.querySelector('.MuiCircularProgress-colorSuccess');
    expect(progressBar).toBeInTheDocument();
    expect(screen.getByText(/100\.0%/)).toBeInTheDocument();
  });

  it('renders without counts when not provided', () => {
    renderWithTheme(<ComplianceGauge complianceRate={73.7} />);

    expect(screen.getByText('73.7%')).toBeInTheDocument();
    expect(screen.queryByText('/')).not.toBeInTheDocument();
  });

  it('displays percentage with one decimal place', () => {
    renderWithTheme(<ComplianceGauge complianceRate={73.68} />);

    // Should round to one decimal place
    expect(screen.getByText('73.7%')).toBeInTheDocument();
  });

  it('displays label and counts together', () => {
    renderWithTheme(
      <ComplianceGauge
        complianceRate={73.7}
        label="Overall Compliance"
        numerator={28}
        denominator={38}
      />
    );

    expect(screen.getByText('Overall Compliance')).toBeInTheDocument();
    expect(screen.getByText('28 / 38')).toBeInTheDocument();
  });
});
