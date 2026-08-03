import { describe, expect, it } from 'vitest';
import {
  calculateDeliveryOrderWeight,
  calculateWarehouseFleetCapacity,
  getDeliveryOrderSubmitErrorMessage,
} from './DeliveryOrders';

describe('getDeliveryOrderSubmitErrorMessage', () => {
  it('preserves the warehouse fleet capacity message returned by the API', () => {
    const message = 'Tải trọng quá lớn để giao trong 1 lần, vui lòng chia nhỏ đơn thành nhiều phiếu xuất kho để có thể giao hàng.';

    expect(getDeliveryOrderSubmitErrorMessage(new Error(message), false)).toBe(message);
  });

  it('uses the create and update fallbacks when the API has no message', () => {
    expect(getDeliveryOrderSubmitErrorMessage({}, false)).toBe('Lỗi khi tạo đơn xuất hàng');
    expect(getDeliveryOrderSubmitErrorMessage({}, true)).toBe('Lỗi khi cập nhật đơn xuất hàng');
  });
});

describe('delivery order weight summaries', () => {
  it('calculates order weight from selected product weights and requested quantities', () => {
    const products = [
      { id: 10, weight_kg: 2.5 },
      { id: 20, weight_kg: 4 },
    ];
    const items = [
      { product_id: 10, requested_qty: 3 },
      { product_id: 20, requested_qty: 2 },
    ];

    expect(calculateDeliveryOrderWeight(items, products)).toBe(15.5);
  });

  it('sums every active vehicle in the selected warehouse regardless of status', () => {
    const vehicles = [
      { warehouse_id: 1, is_active: true, status: 'AVAILABLE', max_weight_kg: 1000 },
      { warehouse_id: 1, is_active: true, status: 'ON_TRIP', max_weight_kg: 1500 },
      { warehouse_id: 1, is_active: true, status: 'MAINTENANCE', max_weight_kg: 500 },
      { warehouse_id: 1, is_active: false, status: 'AVAILABLE', max_weight_kg: 9000 },
      { warehouse_id: 2, is_active: true, status: 'AVAILABLE', max_weight_kg: 8000 },
    ];

    expect(calculateWarehouseFleetCapacity(vehicles, 1)).toBe(3000);
  });
});
