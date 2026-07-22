import { describe, it, expect, beforeEach, vi } from 'vitest';
import { masterDataService } from '../../src/services/masterData.service.js';
import apiClient from '../../src/services/api.client.js';

vi.mock('../../src/services/api.client.js', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
  useMock: false,
}));

describe('Master Data Service API Calls', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches supplier list', async () => {
    const suppliers = [{ id: 1, code: 'SUP-001', company_name: 'Sunhouse' }];
    apiClient.get.mockResolvedValueOnce({ data: suppliers });

    const result = await masterDataService.getSuppliers();

    expect(apiClient.get).toHaveBeenCalledWith('/suppliers');
    expect(result).toEqual(suppliers);
  });

  it('creates new supplier', async () => {
    const payload = { code: 'SUP-002', company_name: 'LocknLock' };
    apiClient.post.mockResolvedValueOnce({ data: { id: 2, ...payload } });

    const result = await masterDataService.createSupplier(payload);

    expect(apiClient.post).toHaveBeenCalledWith('/suppliers', { code: 'SUP-002', companyName: 'LocknLock' });
    expect(result.code).toBe('SUP-002');
  });

  it('toggles dealer status', async () => {
    apiClient.delete.mockResolvedValueOnce({ status: 204 });

    await masterDataService.toggleDealerStatus(10, false);

    expect(apiClient.delete).toHaveBeenCalledWith('/dealers/10');
  });
});
