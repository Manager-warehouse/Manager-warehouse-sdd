import axios from 'axios';
import { useAuthStore } from '../stores/auth.store';

const buildBackendErrorMessage = (status, data, fallbackMessage) => {
  if (!data) {
    return fallbackMessage;
  }

  // Prefer the translated message from backend — don't prepend the code
  // because the code is technical (e.g. "DUPLICATE_EXTERNAL_INSTRUCTION")
  // and would confuse end-users. The code is still in data.code for
  // programmatic checks if needed.
  const message = data.message;
  if (message && message.trim()) {
    return message.trim();
  }

  // Fallback: try error field, then HTTP status
  const errorField = data.error;
  if (errorField && errorField.trim()) {
    return errorField.trim();
  }

  return status ? `Lỗi ${status} — vui lòng thử lại.` : (fallbackMessage || 'Có lỗi xảy ra.');
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
