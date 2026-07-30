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
  WAREHOUSE_TYPE_INVALID: 'Không thể tạo phiếu xuất từ kho trung chuyển.',
  DELIVERY_ORDER_TYPE_INVALID: 'Loại phiếu không hợp lệ. Màn này chỉ tạo phiếu xuất bán.',
  INVALID_DELIVERY_DATE: 'Ngày giao hàng dự kiến không hợp lệ.',
  BUSINESS_RULE_VIOLATION: 'Dữ liệu không thỏa mãn quy tắc nghiệp vụ.',
  MISSING_PRICE: 'Chưa có bảng giá đã duyệt cho sản phẩm trong phiếu.',
  CREDIT_HOLD: 'Đại lý không đạt điều kiện công nợ để tạo đơn xuất.',
  PERIOD_CLOSED: 'Kỳ kế toán của ngày chứng từ đã đóng.',
  ACCOUNTING_PERIOD_CLOSED: 'Kỳ kế toán của ngày chứng từ đã đóng.',
  UNPROCESSABLE_ENTITY: 'Dữ liệu không đủ điều kiện để xử lý.',
  INSUFFICIENT_STOCK: 'Tồn kho khả dụng không đủ trong kho đã chọn.',
  DUPLICATE_PRODUCT_ITEM: 'Một sản phẩm không được xuất hiện nhiều dòng trong cùng phiếu.',
  DELIVERY_ORDER_NUMBER_CONFLICT: 'Số đơn xuất kho bị trùng. Vui lòng thử tạo lại.',
  INVENTORY_VERSION_CONFLICT: 'Tồn kho vừa được cập nhật bởi thao tác khác. Vui lòng tải lại và thử lại.',
  WAREHOUSE_PRODUCT_RESERVATION_CONFLICT: 'Tồn giữ chỗ của sản phẩm vừa thay đổi. Vui lòng tải lại và thử lại.',
  CONCURRENT_MODIFICATION: 'Dữ liệu vừa được thay đổi bởi thao tác khác. Vui lòng tải lại và thử lại.',
  DATA_INTEGRITY_VIOLATION: 'Dữ liệu không thỏa mãn ràng buộc của hệ thống.',
  DELIVERY_ORDER_CANCEL_FORBIDDEN: 'Không thể hủy đơn xuất ở trạng thái hiện tại.',
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

const deliveryOrderMessageByBackendText = (code, message = '') => {
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
  if (code === 'UNPROCESSABLE_ENTITY' && message.includes('No accounting period configured')) {
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
  const detailsMessage = code === 'VALIDATION_ERROR' ? validationDetailsMessage(data.details) : null;

  if (detailsMessage) {
    return detailsMessage;
  }
  if (translatedByText) {
    return translatedByText;
  }
  if (code && ERROR_MESSAGE_BY_CODE[code]) {
    return ERROR_MESSAGE_BY_CODE[code];
  }
  if (message && hasVietnameseText(message)) {
    return message;
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

const authStorage = getBrowserStorage('localStorage');
const legacyAuthStorage = getBrowserStorage('sessionStorage');

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
