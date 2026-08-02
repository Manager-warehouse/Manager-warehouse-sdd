import { describe, expect, it } from 'vitest';
import { buildBackendErrorMessage, isPublicAuthRequest } from './api.client';

describe('isPublicAuthRequest', () => {
  it('treats OTP checking as a public authentication request', () => {
    expect(isPublicAuthRequest('/auth/otp/check')).toBe(true);
  });

  it('does not treat protected APIs as public authentication requests', () => {
    expect(isPublicAuthRequest('/stock-takes')).toBe(false);
  });
});

describe('buildBackendErrorMessage', () => {
  it('translates delivery-order stock errors to Vietnamese', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'INSUFFICIENT_STOCK',
      message: 'Insufficient stock in selected warehouse',
    }, 'Request failed')).toBe('Tồn kho khả dụng không đủ trong kho đã chọn.');
  });

  it('translates backend credit-hold reasons to Vietnamese', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'CREDIT_HOLD',
      message: 'Dealer credit limit exceeded',
    }, 'Request failed')).toBe('Đại lý vượt hạn mức công nợ.');
  });

  it('renders validation details with Vietnamese field labels and messages', () => {
    expect(buildBackendErrorMessage(400, {
      code: 'VALIDATION_ERROR',
      details: {
        expectedDeliveryDate: 'must not be null',
        requestedQty: 'must be greater than 0',
      },
    }, 'Request failed')).toBe('Ngày giao dự kiến: không được để trống; Số lượng: phải lớn hơn 0');
  });

  it('does not show raw axios English network messages', () => {
    expect(buildBackendErrorMessage(undefined, null, 'Network Error'))
      .toBe('Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng và thử lại.');
  });

  it('uses Vietnamese generic text for unknown server errors', () => {
    expect(buildBackendErrorMessage(500, { message: 'Internal Server Error' }, 'Request failed'))
      .toBe('Máy chủ đang gặp lỗi. Vui lòng thử lại sau.');
  });

  it('translates PENDING_DOCUMENTS_EXIST with the real count and sample document numbers', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'PENDING_DOCUMENTS_EXIST',
      message: 'PENDING_DOCUMENTS_EXIST: 2 pending/unapproved inbound receipts exist in this period (e.g. RN-1, RN-2).',
    }, 'Request failed')).toBe('Không thể khóa kỳ kế toán: còn 2 phiếu nhập kho chưa được duyệt (VD: RN-1, RN-2).');
  });

  it('falls back to the generic 422 message only when PENDING_DOCUMENTS_EXIST text does not match the expected shape', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'PENDING_DOCUMENTS_EXIST',
      message: 'PENDING_DOCUMENTS_EXIST: something unexpected',
    }, 'Request failed')).toBe('Dữ liệu không đủ điều kiện để xử lý.');
  });

  it('translates RECEIPT_NO_SUPPLIER instead of the generic 422 fallback', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'RECEIPT_NO_SUPPLIER',
      message: 'RECEIPT_NO_SUPPLIER: Receipt does not have an associated supplier',
    }, 'Request failed')).toBe('Phiếu nhập kho này không có nhà cung cấp liên kết, không thể lập hóa đơn mua hàng.');
  });

  it('translates NO_OPEN_PERIOD instead of the generic 422 fallback', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'NO_OPEN_PERIOD',
      message: 'NO_OPEN_PERIOD: No open accounting period found for date 2026-08-02',
    }, 'Request failed')).toBe('Không tìm thấy kỳ kế toán đang mở cho ngày hạch toán đã chọn.');
  });

  it('translates RECEIPT_NO_ITEMS instead of the generic 422 fallback', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'RECEIPT_NO_ITEMS',
      message: 'RECEIPT_NO_ITEMS: Receipt has no items to invoice',
    }, 'Request failed')).toBe('Phiếu nhập kho không có dòng hàng nào để lập hóa đơn.');
  });

  it('translates RECEIPT_NOT_PUTAWAY_COMPLETED instead of the generic 422 fallback', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'RECEIPT_NOT_PUTAWAY_COMPLETED',
      message: 'RECEIPT_NOT_PUTAWAY_COMPLETED: Receipt must be put away before creating a supplier invoice',
    }, 'Request failed')).toBe('Phiếu nhập kho chưa hoàn tất cất kho (Putaway), không thể lập hóa đơn mua hàng.');
  });

  it('translates ITEM_UNIT_COST_MISSING instead of the generic 422 fallback', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'ITEM_UNIT_COST_MISSING',
      message: 'ITEM_UNIT_COST_MISSING: Receipt item unit cost is required for invoicing',
    }, 'Request failed')).toBe('Thiếu đơn giá vốn trên dòng hàng của phiếu nhập, không thể lập hóa đơn.');
  });

  it('translates PAYMENT_EXCEEDS_BALANCE with the real remaining balance', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'PAYMENT_EXCEEDS_BALANCE',
      message: 'PAYMENT_EXCEEDS_BALANCE: Payment amount exceeds remaining invoice balance of 0.00',
    }, 'Request failed')).toBe('Số tiền thanh toán vượt quá dư nợ còn lại của hóa đơn (0.00đ).');
  });

  it('translates SUPPLIER_INVOICE_MISMATCH instead of the generic 422 fallback', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'SUPPLIER_INVOICE_MISMATCH',
      message: 'SUPPLIER_INVOICE_MISMATCH: Supplier invoice does not belong to the specified supplier',
    }, 'Request failed')).toBe('Hóa đơn mua hàng này không thuộc về nhà cung cấp đã chọn.');
  });

  it('translates SUPPLIER_INVOICE_ALREADY_PAID instead of the generic 422 fallback', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'SUPPLIER_INVOICE_ALREADY_PAID',
      message: 'SUPPLIER_INVOICE_ALREADY_PAID: Supplier invoice is already fully paid',
    }, 'Request failed')).toBe('Hóa đơn mua hàng này đã được thanh toán đủ.');
  });

  it('translates NO_OPEN_PERIOD for supplier payment dates too', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'NO_OPEN_PERIOD',
      message: 'NO_OPEN_PERIOD: No open accounting period found for payment date 2026-08-02',
    }, 'Request failed')).toBe('Không tìm thấy kỳ kế toán đang mở cho ngày hạch toán đã chọn.');
  });

  it('translates EMPTY_FILE instead of the generic 422 fallback', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'EMPTY_FILE',
      message: 'EMPTY_FILE: Uploaded file is empty',
    }, 'Request failed')).toBe('File tải lên bị rỗng. Vui lòng chọn lại file.');
  });

  it('translates INVOICE_DEALER_MISMATCH instead of the generic 422 fallback', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'INVOICE_DEALER_MISMATCH',
      message: 'INVOICE_DEALER_MISMATCH: Invoice does not belong to the specified dealer',
    }, 'Request failed')).toBe('Hóa đơn này không thuộc về đại lý đã chọn.');
  });

  it('translates dealer INVOICE_ALREADY_PAID instead of the generic 409 fallback', () => {
    expect(buildBackendErrorMessage(409, {
      code: 'INVOICE_ALREADY_PAID',
      message: 'INVOICE_ALREADY_PAID: Invoice is already fully paid',
    }, 'Request failed')).toBe('Hóa đơn này đã được thanh toán đủ.');
  });

  it('translates OVERPAYMENT_EXCEEDS_INVOICE with the real remaining balance', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'OVERPAYMENT_EXCEEDS_INVOICE',
      message: 'OVERPAYMENT_EXCEEDS_INVOICE: Payment amount exceeds invoice remaining balance of 0.00',
    }, 'Request failed')).toBe('Số tiền thanh toán vượt quá dư nợ còn lại của hóa đơn (0.00đ).');
  });

  it('translates NO_OPEN_PERIOD for dealer payment dates too', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'NO_OPEN_PERIOD',
      message: 'NO_OPEN_PERIOD: No open accounting period found for payment date',
    }, 'Request failed')).toBe('Không tìm thấy kỳ kế toán đang mở cho ngày hạch toán đã chọn.');
  });

  it('translates DELIVERY_ORDER_STATUS_INVALID instead of the generic 422 fallback', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'DELIVERY_ORDER_STATUS_INVALID',
      message: 'Delivery Order must be IN_TRANSIT before auto-invoice',
    }, 'Request failed')).toBe('Đơn xuất phải đang giao hàng (IN_TRANSIT) trước khi có thể lập hóa đơn.');
  });

  it('translates DELIVERY_ORDER_NOT_DELIVERED instead of the generic 422 fallback', () => {
    expect(buildBackendErrorMessage(422, {
      code: 'DELIVERY_ORDER_NOT_DELIVERED',
      message: 'DELIVERY_ORDER_NOT_DELIVERED: Delivery Order has not completed OTP + POD confirmation',
    }, 'Request failed')).toBe('Đơn xuất chưa hoàn tất xác nhận giao hàng (OTP + POD), không thể lập hóa đơn.');
  });
});
