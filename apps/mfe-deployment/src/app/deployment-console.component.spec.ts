import { DeploymentConsoleComponent } from './deployment-console.component';

describe('DeploymentConsoleComponent polling throttling', () => {
  const resolvePollMs = (value: unknown): number =>
    (DeploymentConsoleComponent.prototype as any).resolveStatusPollMs(value);

  it('uses default poll interval when value is missing', () => {
    expect(resolvePollMs(undefined)).toBe(10000);
  });

  it('clamps poll interval to lower bound', () => {
    expect(resolvePollMs(100)).toBe(2000);
  });

  it('clamps poll interval to upper bound', () => {
    expect(resolvePollMs(120000)).toBe(60000);
  });

  it('sanitizes non-numeric values to default', () => {
    expect(resolvePollMs('abc')).toBe(10000);
  });
});
