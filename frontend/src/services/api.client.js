import axios from 'axios';

const buildBackendErrorMessage = (status, data, fallbackMessage) => {
  if (!data) {
    return fallbackMessage;
  }

  const code = data.code || data.error;
  const message = data.message || data.error || fallbackMessage;

  if (code && message && code !== message) {
    return `${code}: ${message}`;
  }
  if (message) {
    return message;
  }
  if (code) {
    return code;
  }
  return status ? `HTTP ${status}` : fallbackMessage;
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

// Interceptor to add JWT authorization header
apiClient.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('wms_token');
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
    const isAuthRequest = originalRequest.url && (
      originalRequest.url.includes('/auth/login') ||
      originalRequest.url.includes('/auth/refresh') ||
      originalRequest.url.includes('/auth/forgot-password') ||
      originalRequest.url.includes('/auth/verify-otp')
    );

    // Handle Token Expired (401) for non-auth requests
    if (error.response && error.response.status === 401 && !originalRequest._retry && !isAuthRequest) {
      originalRequest._retry = true;
      try {
        // Reuse an in-flight refresh instead of firing a new one per
        // concurrent 401 — the backend rotates the refresh token on every
        // call, so a second parallel call would invalidate the first.
        if (!refreshPromise) {
          refreshPromise = refreshClient.post('/auth/refresh', {
            refreshToken: sessionStorage.getItem('wms_refresh_token')
          }).finally(() => {
            refreshPromise = null;
          });
        }
        const response = await refreshPromise;
        const { accessToken, refreshToken } = response.data;
        sessionStorage.setItem('wms_token', accessToken);
        // Backend rotates the refresh token on every use; persist the new
        // one or the next refresh call will be replaying an invalidated token.
        if (refreshToken) {
          sessionStorage.setItem('wms_refresh_token', refreshToken);
        }
        apiClient.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Clear session and redirect to login
        sessionStorage.removeItem('wms_user');
        sessionStorage.removeItem('wms_token');
        sessionStorage.removeItem('wms_refresh_token');
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
