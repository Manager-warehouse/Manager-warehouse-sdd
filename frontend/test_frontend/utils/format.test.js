import { formatDate, getAvatarFallback, formatNumber } from '../../src/utils/format';

describe('formatDate', () => {
  test.each([
    [null, ''],
    [undefined, ''],
    ['', ''],
    ['invalid-date', ''],
    ['2026-07-04T12:30:00Z', '04/07/2026', true]
  ])('formatDate(%s) should return %s', (input, expected, useContain) => {
    const result = formatDate(input);
    if (useContain) {
      expect(result).toContain(expected);
    } else {
      expect(result).toBe(expected);
    }
  });
});

describe('getAvatarFallback', () => {
  test.each([
    [null, '?'],
    ['', '?'],
    ['John', 'JO'],
    ['a', 'A'],
    ['John Doe', 'JD'],
    ['Nguyen Van A', 'NA']
  ])('getAvatarFallback(%s) should return %s', (input, expected) => {
    expect(getAvatarFallback(input)).toBe(expected);
  });
});

describe('formatNumber', () => {
  test.each([
    [null, '0'],
    [undefined, '0'],
    [1000, '1.000'],
    [1234567, '1.234.567']
  ])('formatNumber(%s) should return %s', (input, expected) => {
    expect(formatNumber(input)).toBe(expected);
  });
});

import { interWarehouseTransferStatusLabel, INTER_WAREHOUSE_TRANSFER_STATUS } from '../../src/utils/interWarehouseTransferStatus';

describe('interWarehouseTransferStatusLabel', () => {
  test.each([
    [INTER_WAREHOUSE_TRANSFER_STATUS.NEW, 'Mới'],
    [INTER_WAREHOUSE_TRANSFER_STATUS.APPROVED, 'Đã duyệt'],
    ['UNKNOWN_STATUS', 'UNKNOWN_STATUS'],
    [null, null]
  ])('interWarehouseTransferStatusLabel(%s) should return %s', (input, expected) => {
    expect(interWarehouseTransferStatusLabel(input)).toBe(expected);
  });
});
