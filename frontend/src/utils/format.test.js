import { formatNumber, getAvatarFallback } from './format';

describe('format utilities', () => {
  test('formats missing quantities as zero', () => {
    expect(formatNumber(null)).toBe('0');
    expect(formatNumber(undefined)).toBe('0');
  });

  test('builds a stable avatar fallback', () => {
    expect(getAvatarFallback('Nguyen Van An')).toBe('NA');
    expect(getAvatarFallback('Kho')).toBe('KH');
    expect(getAvatarFallback('')).toBe('?');
  });
});
