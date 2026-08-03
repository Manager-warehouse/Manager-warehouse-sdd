import { describe, expect, it } from 'vitest';

import { updateReturnedGoodsRow } from './returnedGoods.utils';

describe('updateReturnedGoodsRow', () => {
  const row = {
    expected_qty: 10,
    actual_qty: 10,
    quality_pass_qty: 10,
    quality_fail_qty: 0,
    shortage_reason: 'Seal was broken during return',
  };

  it('derives shortage from expected and actual received quantity', () => {
    const updated = updateReturnedGoodsRow(row, 'actual_qty', '8');

    expect(updated).toEqual(expect.objectContaining({
      actual_qty: '8',
      quality_fail_qty: 0,
      shortage_qty: 2,
      shortage_reason: 'Seal was broken during return',
    }));
  });

  it('derives failed quality quantity from actual received minus passed', () => {
    const updated = updateReturnedGoodsRow({ ...row, actual_qty: 8 }, 'quality_pass_qty', '6');

    expect(updated).toEqual(expect.objectContaining({
      quality_pass_qty: '6',
      quality_fail_qty: 2,
      failed_planned_qty: 2,
      planned_qty: 6,
    }));
  });

  it('clears shortage reason when the full expected quantity is received', () => {
    const updated = updateReturnedGoodsRow({ ...row, actual_qty: 8 }, 'actual_qty', '10');

    expect(updated.shortage_qty).toBe(0);
    expect(updated.shortage_reason).toBe('');
  });
});
