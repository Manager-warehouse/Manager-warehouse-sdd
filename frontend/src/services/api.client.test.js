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
});
