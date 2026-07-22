import { describe, it, expect, beforeEach, vi } from 'vitest';
import reportService from '../../src/services/report.service.js';
import apiClient from '../../src/services/api.client.js';

vi.mock('../../src/services/api.client.js', () => ({
  default: {
    get: vi.fn(),
  },
  useMock: false,
}));

describe('Report & Dashboard Service API Calls', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches CEO dashboard metrics', async () => {
    const mockData = {
      kpis: { totalInventoryValue: 12500000000 },
      topDebtors: [{ dealerId: 1, dealerName: 'Dealer A' }],
    };
    apiClient.get.mockResolvedValueOnce({ data: mockData });

    const result = await reportService.getCeoDashboard();

    expect(apiClient.get).toHaveBeenCalledWith('/dashboard/ceo');
    expect(result.kpis.total_inventory_value).toBe(12500000000);
  });

  it('fetches inventory valuation report', async () => {
    const mockValuation = {
      summary: { totalValuation: 5000000000 },
      records: [],
    };
    apiClient.get.mockResolvedValueOnce({ data: mockValuation });

    const result = await reportService.getInventoryValuation(1);

    expect(apiClient.get).toHaveBeenCalledWith('/reports/inventory-valuation?warehouseId=1');
    expect(result.summary.total_valuation).toBe(5000000000);
  });

  it('fetches low stock alerts with pagination', async () => {
    const mockAlerts = { content: [{ id: 1, currentQty: 5 }], totalElements: 1 };
    apiClient.get.mockResolvedValueOnce({ data: mockAlerts });

    const result = await reportService.getLowStockAlerts({ warehouseId: 1, isResolved: false, page: 0, size: 20 });

    expect(apiClient.get).toHaveBeenCalledWith('/alerts/low-stock', {
      params: { page: 0, size: 20, warehouseId: 1, isResolved: false },
    });
    expect(result.content).toHaveLength(1);
  });
});
