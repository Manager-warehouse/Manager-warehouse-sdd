import axios from 'axios';

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
    const token = localStorage.getItem('wms_token');
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
    
    // Handle Token Expired (401)
    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        // Attempt to refresh token (mock or actual)
        // If we are using mock, this won't happen since mock won't throw 401.
        // For real API:
        const response = await axios.post('/api/v1/auth/refresh', {
          refreshToken: localStorage.getItem('wms_refresh_token')
        });
        const { accessToken } = response.data;
        localStorage.setItem('wms_token', accessToken);
        apiClient.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Clear session and redirect to login
        localStorage.removeItem('wms_user');
        localStorage.removeItem('wms_token');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export default apiClient;
export const useMock = import.meta.env.VITE_USE_MOCK === 'true';
