import { describe, expect, it } from 'vitest';
import { buildSplitPlanPayload, getSplitAllocationItems, getSplitFleetAssignments } from './TripPlanning';

describe('TripPlanning split allocation', () => {
  it('keeps QC-passed quantities separated by delivery item and batch', () => {
    const order = {
      items: [{
        id: 20,
        product_id: 2,
        product_name: 'Product 2',
        qc_pass_qty: 22,
        allocations: [
          { batch_id: 37, batch_code: 'B-37', qc_pass_qty: 20 },
          { batch_id: 25, batch_code: 'B-25', qc_pass_qty: 2 },
        ],
      }, {
        id: 21,
        product_id: 6,
        product_name: 'Product 6',
        qc_pass_qty: 988,
        allocations: [
          { batch_id: 36, batch_code: 'B-36', qc_pass_qty: 980 },
          { batch_id: 21, batch_code: 'B-21', qc_pass_qty: 8 },
        ],
      }],
    };

    expect(getSplitAllocationItems(order)).toEqual([
      expect.objectContaining({ key: '20:37', do_item_id: 20, batch_id: 37, quantity: 20 }),
      expect.objectContaining({ key: '20:25', do_item_id: 20, batch_id: 25, quantity: 2 }),
      expect.objectContaining({ key: '21:36', do_item_id: 21, batch_id: 36, quantity: 980 }),
      expect.objectContaining({ key: '21:21', do_item_id: 21, batch_id: 21, quantity: 8 }),
    ]);
  });

  it('uses the driver assigned to vehicle one as the lead driver', () => {
    const order = {
      id: 12,
      items: [{
        id: 20,
        product_id: 2,
        qc_pass_qty: 22,
        allocations: [{ batch_id: 37, qc_pass_qty: 22 }],
      }],
    };
    const rows = [
      { vehicle_id: 3, driver_id: 3, item_quantities: { '20:37': 12 } },
      { vehicle_id: 1, driver_id: 2, item_quantities: { '20:37': 10 } },
    ];

    const payload = buildSplitPlanPayload({
      order,
      rows,
      plannedStartAt: '2026-08-04T17:51',
      plannedEndAt: '2026-08-05T17:51',
    });

    expect(payload.lead_driver_id).toBe(3);
    expect(payload.legs.map((leg) => leg.driver_id)).toEqual([3, 2]);
  });

  it('groups every vehicle and driver belonging to the same split plan', () => {
    const trips = [{
      id: 101,
      driver_id: 2,
      delivery_orders: [{ split_plan_id: 77, is_split_lead: false }],
    }, {
      id: 102,
      driver_id: 3,
      delivery_orders: [{ split_plan_id: 77, is_split_lead: true }],
    }, {
      id: 103,
      driver_id: 4,
      delivery_orders: [{ split_plan_id: 88, is_split_lead: true }],
    }];

    const assignments = getSplitFleetAssignments(trips[0], trips);

    expect(assignments.map((trip) => trip.id)).toEqual([102, 101]);
  });
});
