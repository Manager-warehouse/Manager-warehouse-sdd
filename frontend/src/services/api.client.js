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

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

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
        // Attempt to refresh token (mock or actual)
        // If we are using mock, this won't happen since mock won't throw 401.
        // For real API:
        const response = await axios.post('/api/v1/auth/refresh', {
          refreshToken: sessionStorage.getItem('wms_refresh_token')
        });
        const { accessToken } = response.data;
        sessionStorage.setItem('wms_token', accessToken);
        apiClient.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
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
