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

  it('sends the Storekeeper rejection decision and recount reason', async () => {
    const { outboundService } = await import('./outbound.service');
    mocks.put.mockResolvedValue({ data: { id: 100, status: 'WAITING_PICKING', items: [] } });

    await outboundService.rejectQualityOutbound(100, 'So luong thuc te khong khop', 'Kiem tra lai');

    expect(mocks.put).toHaveBeenCalledWith('/delivery-orders/100/quality-approval', {
      decision: 'REJECT',
      rejectionReason: 'So luong thuc te khong khop',
      notes: 'Kiem tra lai',
    });
  });

  it('sends shortage reason and normalizes server-derived shortage quantity', async () => {
    const { outboundService } = await import('./outbound.service');
    mocks.put.mockResolvedValue({
      data: {
        doId: 100,
        flowStatus: 'COUNT_QC_SUBMITTED',
        items: [{
          doItemId: 200,
          productId: 30,
          batchId: 71,
          expectedQty: 10,
          actualQty: 8,
          qualityPassQty: 8,
          qualityFailQty: 0,
          shortageQty: 2,
          shortageReason: 'Thiếu một thùng khi xe về kho',
        }],
      },
    });

    const result = await outboundService.submitReturnedGoodsCountQc(100, {
      notes: '',
      items: [{
        do_item_id: 200,
        product_id: 30,
        batch_id: 71,
        actual_qty: 8,
        quality_pass_qty: 8,
        quality_fail_qty: 0,
        shortage_reason: 'Thiếu một thùng khi xe về kho',
      }],
    });

    expect(mocks.put).toHaveBeenCalledWith('/delivery-orders/100/returned-goods/count-qc', {
      notes: '',
      items: [{
        doItemId: 200,
        productId: 30,
        batchId: 71,
        actualQty: 8,
        qualityPassQty: 8,
        qualityFailQty: 0,
        qualityFailureReason: null,
        shortageReason: 'Thiếu một thùng khi xe về kho',
      }],
    });
    expect(result.items[0]).toEqual(expect.objectContaining({
      actual_qty: 8,
      shortage_qty: 2,
      shortage_reason: 'Thiếu một thùng khi xe về kho',
    }));
  });

  it('sends zero and null for an absent failed putaway branch', async () => {
    const { outboundService } = await import('./outbound.service');
    mocks.put.mockResolvedValue({ data: { doId: 100, items: [] } });

    await outboundService.planReturnedGoodsPutaway(100, {
      notes: '',
      items: [{
        do_item_id: 200,
        batch_id: 71,
        destination_location_id: 801,
        planned_qty: 8,
        failed_destination_location_id: '',
        failed_planned_qty: 0,
      }],
    });

    expect(mocks.put.mock.calls[0][1].items[0]).toEqual({
      doItemId: 200,
      batchId: 71,
      destinationLocationId: 801,
      plannedQty: 8,
      failedDestinationLocationId: null,
      failedPlannedQty: 0,
    });
  });
});
