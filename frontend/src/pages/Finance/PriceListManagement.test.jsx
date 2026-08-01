import { describe, expect, it } from 'vitest';
import { buildProductPriceRows } from './PriceListManagement';

const products = [
  { id: 1, sku: 'SKU-001', name: 'Nồi', is_active: true },
  { id: 2, sku: 'SKU-002', name: 'Chảo', is_active: true },
  { id: 3, sku: 'SKU-003', name: 'Ngừng bán', is_active: false },
];

describe('buildProductPriceRows', () => {
  it('returns one row for every active SKU and marks missing prices', () => {
    const rows = buildProductPriceRows(products, [], 10);

    expect(rows).toHaveLength(2);
    expect(rows[0]).toMatchObject({ product_sku: 'SKU-001', status: 'UNPRICED', warehouse_id: 10 });
    expect(rows[1]).toMatchObject({ product_sku: 'SKU-002', status: 'UNPRICED', warehouse_id: 10 });
  });

  it('shows a pending price instead of the previously approved price', () => {
    const prices = [
      { id: 11, product_id: 1, status: 'APPROVED', effective_date: '2026-07-01', cost_price: 10000 },
      { id: 12, product_id: 1, status: 'PENDING', effective_date: '2026-08-01', cost_price: 12000 },
    ];

    const [row] = buildProductPriceRows(products, prices, 10);

    expect(row).toMatchObject({ id: 12, product_sku: 'SKU-001', product_name: 'Nồi', status: 'PENDING' });
  });

  it('ignores cancelled prices and keeps only the latest approved price per SKU', () => {
    const prices = [
      { id: 21, product_id: 1, status: 'APPROVED', effective_date: '2026-06-01' },
      { id: 22, product_id: 1, status: 'APPROVED', effective_date: '2026-07-01' },
      { id: 23, product_id: 2, status: 'CANCELLED', effective_date: '2026-08-01' },
    ];

    const rows = buildProductPriceRows(products, prices, 10);

    expect(rows[0].id).toBe(22);
    expect(rows[1].status).toBe('UNPRICED');
  });
});
