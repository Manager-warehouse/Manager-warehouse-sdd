import { describe, it, expect, beforeEach, vi } from 'vitest';
import pricingService from '../../src/services/pricing.service.js';
import apiClient from '../../src/services/api.client.js';

vi.mock('../../src/services/api.client.js', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
  useMock: false,
}));

describe('Pricing Service API Calls', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches price history list with filters', async () => {
    const mockList = [
      { id: 1, product_sku: 'NOI-001', cost_price: 100000, selling_price: 150000, status: 'APPROVED' },
    ];
    apiClient.get.mockResolvedValueOnce({ data: mockList });

    const result = await pricingService.getAll({ product_id: 10, status: 'APPROVED' });

    expect(apiClient.get).toHaveBeenCalledWith('/price-history?status=APPROVED&productId=10');
    expect(result).toEqual(mockList);
  });

  it('creates new price history entry', async () => {
    const newEntry = { product_id: 10, cost_price: 100000, selling_price: 150000 };
    apiClient.post.mockResolvedValueOnce({ data: { id: 2, ...newEntry, status: 'PENDING' } });

    const result = await pricingService.create(newEntry);

    expect(apiClient.post).toHaveBeenCalledWith('/price-history', newEntry);
    expect(result.status).toBe('PENDING');
  });

  it('approves price history entry', async () => {
    apiClient.put.mockResolvedValueOnce({ data: { id: 2, status: 'APPROVED' } });

    const result = await pricingService.approve(2);

    expect(apiClient.put).toHaveBeenCalledWith('/price-history/2/approve');
    expect(result.status).toBe('APPROVED');
  });
});
