import { getStarTier, getStarColor, StarTier } from './star-ratings.model';

describe('Star Ratings Model', () => {
  describe('getStarTier', () => {
    const cases: [number, StarTier][] = [
      [0.0, 'critical'],
      [1.0, 'critical'],
      [1.99, 'critical'],
      [2.0, 'below-average'],
      [2.5, 'below-average'],
      [2.99, 'below-average'],
      [3.0, 'average'],
      [3.25, 'average'],
      [3.49, 'average'],
      [3.5, 'above-average'],
      [3.75, 'above-average'],
      [3.99, 'above-average'],
      [4.0, 'bonus'],
      [4.25, 'bonus'],
      [4.49, 'bonus'],
      [4.5, 'exceptional'],
      [4.75, 'exceptional'],
      [5.0, 'exceptional'],
    ];

    it.each(cases)('should return %s for rating %d', (rating, expected) => {
      expect(getStarTier(rating)).toBe(expected);
    });
  });

  describe('getStarColor', () => {
    it('should return red for critical ratings', () => {
      expect(getStarColor(1.5)).toBe('#D32F2F');
    });

    it('should return orange for below-average ratings', () => {
      expect(getStarColor(2.5)).toBe('#F57C00');
    });

    it('should return yellow for average ratings', () => {
      expect(getStarColor(3.25)).toBe('#FBC02D');
    });

    it('should return light green for above-average ratings', () => {
      expect(getStarColor(3.75)).toBe('#66BB6A');
    });

    it('should return dark green for bonus-eligible ratings', () => {
      expect(getStarColor(4.25)).toBe('#388E3C');
    });

    it('should return gold for exceptional ratings', () => {
      expect(getStarColor(4.75)).toBe('#FFD700');
    });

    it('should handle boundary value 0.0', () => {
      expect(getStarColor(0.0)).toBe('#D32F2F');
    });

    it('should handle boundary value 2.0 exactly', () => {
      expect(getStarColor(2.0)).toBe('#F57C00');
    });

    it('should handle boundary value 4.0 exactly', () => {
      expect(getStarColor(4.0)).toBe('#388E3C');
    });

    it('should handle max value 5.0', () => {
      expect(getStarColor(5.0)).toBe('#FFD700');
    });
  });
});
