import axios from 'axios';
import { useAuthStore } from '../stores/auth.store';

const FIELD_LABELS = {
  dealerId: 'Đại lý nhận hàng',
  warehouseId: 'Kho xuất',
  type: 'Loại phiếu',
  expectedDeliveryDate: 'Ngày giao dự kiến',
  documentDate: 'Ngày chứng từ',
  items: 'Danh sách sản phẩm',
  productId: 'Sản phẩm',
  requestedQty: 'Số lượng',
  unitPrice: 'Đơn giá',
};

const ERROR_MESSAGE_BY_CODE = {
  VALIDATION_ERROR: 'Dữ liệu nhập chưa hợp lệ.',
  INVALID_REQUEST_BODY: 'Dữ liệu gửi lên không đúng định dạng.',
  INVALID_CREDENTIALS: 'Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.',
  REFRESH_TOKEN_MISSING: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  UNAUTHORIZED: 'Bạn cần đăng nhập để tiếp tục.',
  ACCESS_DENIED: 'Bạn không có quyền thực hiện thao tác này.',
  WAREHOUSE_SCOPE_FORBIDDEN: 'Bạn không được phân quyền thao tác trên kho đã chọn.',
  RESOURCE_NOT_FOUND: 'Không tìm thấy dữ liệu liên quan. Vui lòng tải lại trang và thử lại.',
  WAREHOUSE_INACTIVE: 'Kho đã chọn đang ngừng hoạt động.',
  WAREHOUSE_HAS_STOCK: 'Không thể tắt kho vì kho vẫn đang còn hàng tồn kho.',
  LOCATION_HAS_STOCK: 'Không thể tắt vị trí lưu trữ (bin) vì vẫn đang còn hàng tồn kho.',
  WAREHOUSE_TYPE_INVALID: 'Không thể tạo phiếu xuất từ kho trung chuyển.',
  DELIVERY_ORDER_TYPE_INVALID: 'Loại phiếu không hợp lệ. Màn này chỉ tạo phiếu xuất bán.',
  INVALID_DELIVERY_DATE: 'Ngày giao hàng dự kiến không hợp lệ.',
  BUSINESS_RULE_VIOLATION: 'Dữ liệu không thỏa mãn quy tắc nghiệp vụ.',
  MISSING_PRICE: 'Chưa có bảng giá đã duyệt cho sản phẩm trong phiếu.',
  CREDIT_HOLD: 'Đại lý không đạt điều kiện công nợ để tạo đơn xuất.',
  PERIOD_CLOSED: 'Kỳ kế toán của ngày chứng từ đã đóng.',
  ACCOUNTING_PERIOD_CLOSED: 'Kỳ kế toán của ngày chứng từ đã đóng.',
  PERIOD_NOT_YET_ENDED: 'Kỳ kế toán chưa kết thúc, chỉ có thể khóa sau khi kỳ đã kết thúc.',
  PERIOD_NOT_CLOSED: 'Kỳ kế toán này chưa bị khóa, không cần mở lại.',
  UNPROCESSABLE_ENTITY: 'Dữ liệu không đủ điều kiện để xử lý.',
  INSUFFICIENT_STOCK: 'Tồn kho khả dụng không đủ trong kho đã chọn.',
  DUPLICATE_PRODUCT_ITEM: 'Một sản phẩm không được xuất hiện nhiều dòng trong cùng phiếu.',
  DELIVERY_ORDER_NUMBER_CONFLICT: 'Số đơn xuất kho bị trùng. Vui lòng thử tạo lại.',
  INVENTORY_VERSION_CONFLICT: 'Tồn kho vừa được cập nhật bởi thao tác khác. Vui lòng tải lại và thử lại.',
  WAREHOUSE_PRODUCT_RESERVATION_CONFLICT: 'Tồn giữ chỗ của sản phẩm vừa thay đổi. Vui lòng tải lại và thử lại.',
  CONCURRENT_MODIFICATION: 'Dữ liệu vừa được thay đổi bởi thao tác khác. Vui lòng tải lại và thử lại.',
  DATA_INTEGRITY_VIOLATION: 'Dữ liệu không thỏa mãn ràng buộc của hệ thống.',
  TRIP_SCHEDULE_INVALID: 'Lịch trình chuyến đi không hợp lệ: thời gian kết thúc phải sau thời gian bắt đầu.',
  TRIP_START_IN_PAST: 'Thời gian bắt đầu chuyến không được ở quá khứ.',
  TRIP_END_IN_PAST: 'Thời gian kết thúc dự kiến không được ở quá khứ.',
  TRIP_END_MUST_NOT_BE_AFTER_REQUIRED_DATE: 'Kết thúc dự kiến của chuyến không được sau ngày cần hàng của phiếu điều chuyển.',
  TRANSFER_REQUIRED_DATE_EXPIRED: 'Phiếu điều chuyển đã quá ngày cần hàng, không thể lập chuyến hoặc tiếp tục xử lý.',
  VEHICLE_SCHEDULE_OVERLAP: 'Xe đã có chuyến khác trùng thời gian. Vui lòng chọn xe hoặc khung giờ khác.',
  DRIVER_SCHEDULE_OVERLAP: 'Tài xế đã có chuyến khác trùng thời gian. Vui lòng chọn tài xế hoặc khung giờ khác.',
  VEHICLE_NOT_AVAILABLE: 'Xe hiện không khả dụng. Vui lòng chọn xe khác.',
  DRIVER_NOT_AVAILABLE: 'Tài xế hiện không khả dụng. Vui lòng chọn tài xế khác.',
  DRIVER_LICENSE_EXPIRED: 'Tài xế chưa có hạn GPLX hoặc GPLX đã hết hạn.',
  DELIVERY_ORDER_UPDATE_FORBIDDEN: 'Chỉ có thể cập nhật đơn xuất khi trạng thái còn mới và chưa có kế hoạch lấy hàng.',
  DELIVERY_ORDER_CANCEL_FORBIDDEN: 'Không thể hủy đơn xuất ở trạng thái hiện tại.',
  PICKED_GOODS_RETURN_REQUIRED: 'Đơn xuất đã có hàng được lấy, cần hoàn hàng về bin trước khi hủy.',
  RESERVATION_NOT_FOUND: 'Không tìm thấy lượng tồn đã giữ chỗ cho đơn xuất này. Vui lòng tải lại và thử lại.',
  RECEIPT_NOT_PUTAWAY_COMPLETED: 'Phiếu nhập kho chưa hoàn tất cất kho (Putaway), không thể lập hóa đơn mua hàng.',
  RECEIPT_NO_SUPPLIER: 'Phiếu nhập kho này không có nhà cung cấp liên kết, không thể lập hóa đơn mua hàng.',
  NO_OPEN_PERIOD: 'Không tìm thấy kỳ kế toán đang mở cho ngày hạch toán đã chọn.',
  RECEIPT_NO_ITEMS: 'Phiếu nhập kho không có dòng hàng nào để lập hóa đơn.',
  ITEM_UNIT_COST_MISSING: 'Thiếu đơn giá vốn trên dòng hàng của phiếu nhập, không thể lập hóa đơn.',
  SUPPLIER_INVOICE_MISMATCH: 'Hóa đơn mua hàng này không thuộc về nhà cung cấp đã chọn.',
  SUPPLIER_INVOICE_ALREADY_PAID: 'Hóa đơn mua hàng này đã được thanh toán đủ.',
  EMPTY_FILE: 'File tải lên bị rỗng. Vui lòng chọn lại file.',
  INVOICE_DEALER_MISMATCH: 'Hóa đơn này không thuộc về đại lý đã chọn.',
  INVOICE_ALREADY_PAID: 'Hóa đơn này đã được thanh toán đủ.',
  DELIVERY_ORDER_STATUS_INVALID: 'Đơn xuất phải đang giao hàng (IN_TRANSIT) trước khi có thể lập hóa đơn.',
  DELIVERY_ORDER_NOT_DELIVERED: 'Đơn xuất chưa hoàn tất xác nhận giao hàng (OTP + POD), không thể lập hóa đơn.',
  NO_FAILED_QTY: 'Không còn số lượng hàng lỗi trong khu cách ly để tiêu hủy.',
  ALREADY_DISPOSED: 'Mặt hàng này đã có yêu cầu tiêu hủy hoặc đã được xử lý tiêu hủy.',
  INVALID_TYPE: 'Phiếu điều chỉnh này không phải yêu cầu tiêu hủy.',
  ALREADY_APPROVED: 'Yêu cầu tiêu hủy này đã được phê duyệt.',
  MISSING_STOCK_KEYS: 'Thiếu thông tin lô hoặc vị trí cách ly để trừ tồn.',
};

const looksLikeErrorCode = (value = '') => /^[A-Z][A-Z0-9_:-]+$/.test(String(value).trim());

const hasVietnameseText = (value = '') => /[À-ỹĐđ]/.test(String(value));

const fieldLabel = (field = '') => {
  const normalized = String(field).replace(/\[\d+\]/g, '').split('.').pop();
  return FIELD_LABELS[normalized] || field;
};

const translateFallbackMessage = (message) => {
  if (!message) return null;
  if (hasVietnameseText(message)) return message;
  if (message === 'Network Error') return 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng và thử lại.';
  if (message.includes('timeout')) return 'Yêu cầu quá thời gian chờ. Vui lòng thử lại.';
  if (looksLikeErrorCode(message)) return ERROR_MESSAGE_BY_CODE[message] || null;
  return null;
};

const translateValidationText = (value) => {
  const text = String(value);
  if (hasVietnameseText(text)) return text;
  if (text.includes('must not be null')) return 'không được để trống';
  if (text.includes('must not be empty')) return 'không được để trống';
  if (text.includes('must be greater than 0')) return 'phải lớn hơn 0';
  if (text.includes('must be greater than or equal to')) return 'không được nhỏ hơn giá trị tối thiểu';
  if (text.includes('must match')) return 'không đúng định dạng';
  if (text.includes('Failed to convert')) return 'không đúng định dạng';
  return 'không hợp lệ';
};

const validationDetailsMessage = (details) => {
  if (!details || typeof details !== 'object' || Array.isArray(details)) {
    return null;
  }
  const lines = Object.entries(details)
    .filter(([, value]) => value)
    .slice(0, 4)
    .map(([field, value]) => `${fieldLabel(field)}: ${translateValidationText(value)}`);
  return lines.length ? lines.join('; ') : null;
};

// English doc-label fragments used by AccountingPeriodServiceImpl#checkNoPending, mapped
// to Vietnamese - keep in sync with the docLabel arguments passed there.
const PENDING_DOCUMENT_LABELS = {
  'pending/unapproved inbound receipts': 'phiếu nhập kho chưa được duyệt',
  'pending delivery orders': 'đơn xuất kho chưa hoàn tất',
  'pending internal warehouse transfers': 'phiếu điều chuyển nội bộ chưa hoàn tất',
  'pending stocktakes': 'phiếu kiểm kê chưa được duyệt',
  'unapproved adjustments': 'phiếu điều chỉnh tồn kho chưa được duyệt',
  'completed delivery orders have no invoice yet': 'đơn xuất kho đã hoàn tất nhưng chưa lập hóa đơn',
};

// "PENDING_DOCUMENTS_EXIST: 2 pending delivery orders exist in this period (e.g. DO-1, DO-2)."
const PENDING_DOCUMENTS_PATTERN = /^\d+\s+(.+?)\s+exist in this period \(e\.g\.\s+(.+?)\)\.$/;

const pendingDocumentsMessage = (message) => {
  const match = PENDING_DOCUMENTS_PATTERN.exec(message.replace(/^PENDING_DOCUMENTS_EXIST:\s*/, ''));
  if (!match) return null;
  const [, englishLabel, samples] = match;
  const countMatch = /^(\d+)/.exec(message.replace(/^PENDING_DOCUMENTS_EXIST:\s*/, ''));
  const count = countMatch ? countMatch[1] : '';
  const viLabel = PENDING_DOCUMENT_LABELS[englishLabel] || englishLabel;
  return `Không thể khóa kỳ kế toán: còn ${count} ${viLabel} (VD: ${samples}).`;
};

// "PAYMENT_EXCEEDS_BALANCE: Payment amount exceeds remaining invoice balance of 0.00"
const PAYMENT_EXCEEDS_BALANCE_PATTERN = /remaining invoice balance of\s+([\d.,]+)/;

const paymentExceedsBalanceMessage = (message) => {
  const match = PAYMENT_EXCEEDS_BALANCE_PATTERN.exec(message);
  if (!match) return null;
  const [, remaining] = match;
  return `Số tiền thanh toán vượt quá dư nợ còn lại của hóa đơn (${remaining}đ).`;
};

// "OVERPAYMENT_EXCEEDS_INVOICE: Payment amount exceeds invoice remaining balance of 0.00"
const OVERPAYMENT_EXCEEDS_INVOICE_PATTERN = /invoice remaining balance of\s+([\d.,]+)/;

const overpaymentExceedsInvoiceMessage = (message) => {
  const match = OVERPAYMENT_EXCEEDS_INVOICE_PATTERN.exec(message);
  if (!match) return null;
  const [, remaining] = match;
  return `Số tiền thanh toán vượt quá dư nợ còn lại của hóa đơn (${remaining}đ).`;
};

const deliveryOrderMessageByBackendText = (code, message = '') => {
  if (code === 'PENDING_DOCUMENTS_EXIST') {
    return pendingDocumentsMessage(message);
  }
  if (code === 'PAYMENT_EXCEEDS_BALANCE') {
    return paymentExceedsBalanceMessage(message);
  }
  if (code === 'OVERPAYMENT_EXCEEDS_INVOICE') {
    return overpaymentExceedsInvoiceMessage(message);
  }
  if (code === 'CREDIT_HOLD') {
    if (message.includes('credit limit exceeded')) {
      return 'Đại lý vượt hạn mức công nợ.';
    }
    if (message.includes('overdue')) {
      return 'Đại lý có hóa đơn quá hạn thanh toán.';
    }
    if (message.includes('credit hold')) {
      return 'Đại lý đang bị chặn công nợ.';
    }
  }
  if (code === 'UNPROCESSABLE_ENTITY' && (
    message.includes('No accounting period configured') || message.includes('No open accounting period')
  )) {
    return 'Chưa cấu hình kỳ kế toán cho ngày chứng từ.';
  }
  if (code === 'RESOURCE_NOT_FOUND') {
    if (message.includes('Warehouse')) return 'Không tìm thấy kho đã chọn.';
    if (message.includes('Dealer')) return 'Không tìm thấy đại lý đã chọn.';
    if (message.includes('product') || message.includes('Product')) return 'Không tìm thấy sản phẩm đang hoạt động.';
  }
  if (code === 'BUSINESS_RULE_VIOLATION' && message.includes('Dealer is inactive')) {
    return 'Đại lý đã ngừng hoạt động.';
  }
  return null;
};

export const buildBackendErrorMessage = (status, data, fallbackMessage) => {
  if (!data) {
    return translateFallbackMessage(fallbackMessage)
      || 'Không thể kết nối tới máy chủ. Vui lòng thử lại.';
  }

  const code = data.code || data.error;
  const message = data.message || data.error || fallbackMessage;
  const translatedByText = deliveryOrderMessageByBackendText(code, message);
  const translatedByMessageCode = looksLikeErrorCode(message) ? ERROR_MESSAGE_BY_CODE[message] : null;
  const detailsMessage = code === 'VALIDATION_ERROR' ? validationDetailsMessage(data.details) : null;

  if (detailsMessage) {
    return detailsMessage;
  }
  if (translatedByText) {
    return translatedByText;
  }
  if (translatedByMessageCode) {
    return translatedByMessageCode;
  }
  if (message && hasVietnameseText(message)) {
    return message;
  }
  if (code && ERROR_MESSAGE_BY_CODE[code]) {
    return ERROR_MESSAGE_BY_CODE[code];
  }
  if (message && !looksLikeErrorCode(message) && !/^[\x00-\x7F]+$/.test(message)) {
    return message;
  }
  if (status === 400) return 'Dữ liệu gửi lên chưa hợp lệ.';
  if (status === 401) return 'Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.';
  if (status === 403) return 'Bạn không có quyền thực hiện thao tác này.';
  if (status === 404) return 'Không tìm thấy dữ liệu yêu cầu.';
  if (status === 409) return 'Dữ liệu vừa thay đổi. Vui lòng tải lại và thử lại.';
  if (status === 422) return 'Dữ liệu không đủ điều kiện để xử lý.';
  if (status >= 500) return 'Máy chủ đang gặp lỗi. Vui lòng thử lại sau.';

  return translateFallbackMessage(fallbackMessage) || 'Đã xảy ra lỗi. Vui lòng thử lại.';
};

const API_BASE_URL = import.meta['env'].VITE_API_BASE_URL || '/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Plain axios instance (no interceptors) for the refresh call itself, so it
// always targets the configured API base URL and never recurses into the
// 401 handler below.
const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Shared in-flight refresh promise so concurrent 401s trigger a single
// refresh call instead of racing each other with independent requests.
let refreshPromise = null;

const getBrowserStorage = (name) => {
  try {
    return typeof window !== 'undefined' && window[name] ? window[name] : null;
  } catch {
    return null;
  }
};

const authStorage = getBrowserStorage('sessionStorage');
const legacyAuthStorage = getBrowserStorage('localStorage');

const getAuthValue = (key) => {
  if (!authStorage) return null;
  const value = authStorage.getItem(key);
  if (value) return value;
  const legacyValue = legacyAuthStorage?.getItem(key);
  if (legacyValue) {
    authStorage.setItem(key, legacyValue);
    legacyAuthStorage.removeItem(key);
  }
  return legacyValue;
};

const clearAuthSession = () => {
  useAuthStore.getState().logout();
};

export const isPublicAuthRequest = (url = '') => (
  url.includes('/auth/login') ||
  url.includes('/auth/refresh') ||
  url.includes('/auth/forgot-password') ||
  url.includes('/auth/otp/check') ||
  url.includes('/auth/verify-otp')
);

// Interceptor to add JWT authorization header
apiClient.interceptors.request.use(
  (config) => {
    const token = getAuthValue('wms_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor to handle common responses (such as 401 Unauthorized)
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // Check if the request is an authentication endpoint
    const isAuthRequest = isPublicAuthRequest(originalRequest.url);

    // Handle Token Expired (401) for non-auth requests
    if (error.response && error.response.status === 401 && !originalRequest._retry && !isAuthRequest) {
      originalRequest._retry = true;
      try {
        // Reuse an in-flight refresh instead of firing a new one per
        // concurrent 401 — the backend rotates the refresh token on every
        // call, so a second parallel call would invalidate the first.
        if (!refreshPromise) {
          const refreshTokenValue = getAuthValue('wms_refresh_token');
          if (!refreshTokenValue) {
            throw new Error('REFRESH_TOKEN_MISSING');
          }
          refreshPromise = refreshClient.post('/auth/refresh', {
            refreshToken: refreshTokenValue
          }).finally(() => {
            refreshPromise = null;
          });
        }
        const response = await refreshPromise;
        const { accessToken, refreshToken } = response.data;
        useAuthStore.getState().updateTokens(accessToken, refreshToken);
        apiClient.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Clear session and redirect to login
        clearAuthSession();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    // Normalize error message from backend
    if (error.response) {
      error.message = buildBackendErrorMessage(
        error.response.status,
        error.response.data,
        error.message,
      );
    }
    
    return Promise.reject(error);
  }
);

export default apiClient;
export const useMock = import.meta.env.VITE_USE_MOCK === 'true';
