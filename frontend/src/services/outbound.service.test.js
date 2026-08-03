import { beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
  put: vi.fn(),
}));

vi.mock('./api.client', () => ({
  default: {
    put: mocks.put,
  },
  useMock: false,
}));

describe('outboundService pick/QC payload', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends pass and fail quantities for a partially failed allocation', async () => {
    const { outboundService } = await import('./outbound.service');
    mocks.put.mockResolvedValue({ data: { id: 100, items: [] } });

    await outboundService.confirmQCOutbound(100, {
      items: [{
        do_item_id: 200,
        allocation_id: 900,
        batch_id: 71,
        location_id: 800,
        zone_id: 30,
        picked_qty: 10,
        qc_fail_qty: 2,
        reason: 'Móp méo',
        staging_location_id: 880,
        quarantine_location_id: 990,
      }],
    });

    expect(mocks.put).toHaveBeenCalledWith(
      '/delivery-orders/100/pick-qc-result',
      expect.objectContaining({
        idempotencyKey: expect.stringMatching(/^pick-qc-/),
        results: [{
          doItemId: 200,
          allocationId: 900,
          batchId: 71,
          locationId: 800,
          zoneId: 30,
          pickedQty: 10,
          qcPassQty: 8,
          qcFailQty: 2,
          qcFailReason: 'Móp méo',
          stagingLocationId: 880,
          quarantineLocationId: 990,
          notes: '',
        }],
      }),
    );
  });

  it('rounds decimal pass quantity to the database quantity scale', async () => {
    const { outboundService } = await import('./outbound.service');
    mocks.put.mockResolvedValue({ data: { id: 100, items: [] } });

    await outboundService.confirmQCOutbound(100, {
      items: [{
        do_item_id: 200,
        allocation_id: 900,
        batch_id: 71,
        location_id: 800,
        zone_id: 30,
        picked_qty: 0.3,
        qc_fail_qty: 0.1,
        reason: 'Trầy xước',
        staging_location_id: 880,
        quarantine_location_id: 990,
      }],
    });

    expect(mocks.put.mock.calls[0][1].results[0]).toEqual(expect.objectContaining({
      pickedQty: 0.3,
      qcPassQty: 0.2,
      qcFailQty: 0.1,
    }));
  });
});
