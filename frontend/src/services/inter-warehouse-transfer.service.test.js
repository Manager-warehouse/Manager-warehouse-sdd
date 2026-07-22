import { describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
  post: vi.fn(),
}));

vi.mock('./api.client', () => ({
  default: {
    post: mocks.post,
  },
  useMock: false,
}));

describe('interWarehouseTransferService', () => {
  it('posts source load report payload to the transfer endpoint', async () => {
    const { interWarehouseTransferService } = await import('./inter-warehouse-transfer.service');
    const items = [{ transferItemId: 101, loadedQty: 10 }];
    mocks.post.mockResolvedValue({ data: { id: 7 } });

    await interWarehouseTransferService.recordSourceLoadReport(7, items, 'Xep lai sau QC fail');

    expect(mocks.post).toHaveBeenCalledWith('/inter-warehouse-transfers/7/source-load-report', {
      items,
      reworkReason: 'Xep lai sau QC fail',
    });
  });

  it('posts direct return-to-source payload with reason', async () => {
    const { interWarehouseTransferService } = await import('./inter-warehouse-transfer.service');
    mocks.post.mockResolvedValue({ data: { id: 7, isReturned: true } });

    await interWarehouseTransferService.returnToSource(7, { reason: 'Xe gap su co', wrongSkuItems: [] });

    expect(mocks.post).toHaveBeenCalledWith('/inter-warehouse-transfers/7/return-to-source', {
      reason: 'Xe gap su co',
      wrongSkuItems: [],
    });
  });
});
