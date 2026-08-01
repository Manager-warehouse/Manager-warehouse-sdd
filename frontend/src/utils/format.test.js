import { formatNumber, getAvatarFallback, getLocalDateString } from './format';

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

  test('reads the local calendar date, not the UTC one', () => {
    // 2026-08-02T02:00:00 in a UTC+7 timezone is still 2026-08-01 in UTC - a plain
    // `toISOString().slice(0, 10)` would misread this as the previous day.
    const localMidnightPlus2h = new Date(2026, 7, 2, 2, 0, 0);
    expect(getLocalDateString(localMidnightPlus2h)).toBe('2026-08-02');
  });

  test('pads single-digit month and day', () => {
    expect(getLocalDateString(new Date(2026, 0, 5))).toBe('2026-01-05');
  });
});
