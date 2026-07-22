import { describe, it, expect, beforeEach, vi } from 'vitest';
import returnsService from '../../src/services/returns.service.js';
import apiClient from '../../src/services/api.client.js';

vi.mock('../../src/services/api.client.js', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
  useMock: false,
}));

describe('Returns & Scrap Service API Calls', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('creates customer return receipt', async () => {
    const payload = { warehouseId: 1, deliveryOrderId: 100, dealerId: 5, notes: 'Defective' };
    apiClient.post.mockResolvedValueOnce({ data: { id: 400, receiptNumber: 'REC-RET-001' } });

    const result = await returnsService.createReturn(payload);

    expect(apiClient.post).toHaveBeenCalledWith('/returns', payload);
    expect(result.receiptNumber).toBe('REC-RET-001');
  });

  it('processes return QC items', async () => {
    const qcPayload = { expectedVersion: 0, items: [{ receiptItemId: 1, passedQty: 8, failedQty: 2 }] };
    apiClient.put.mockResolvedValueOnce({ data: { id: 400, status: 'APPROVED' } });

    const result = await returnsService.processQc(400, qcPayload);

    expect(apiClient.put).toHaveBeenCalledWith('/returns/400/qc', qcPayload);
    expect(result.status).toBe('APPROVED');
  });

  it('creates credit note for approved return', async () => {
    apiClient.post.mockResolvedValueOnce({ data: { creditNoteId: 90, creditNoteNumber: 'CN-001' } });

    const result = await returnsService.createCreditNote(400, { reason: 'Credit' });

    expect(apiClient.post).toHaveBeenCalledWith('/returns/400/credit-note', { reason: 'Credit' });
    expect(result.creditNoteNumber).toBe('CN-001');
  });
});
