/**
 * Service xác thực người dùng (Spec 001).
 * Cung cấp: đăng nhập, đăng xuất, lấy thông tin user, cập nhật hồ sơ,
 * đổi mật khẩu, quên mật khẩu (gửi OTP), kiểm tra OTP, xác thực OTP + đặt mật khẩu mới.
 * Hỗ trợ mock mode (VITE_USE_MOCK=true) để dev frontend độc lập.
 */
import apiClient, { useMock } from './api.client';
import { MOCK_USERS } from '../utils/constants';

// Lấy sessionStorage an toàn — trả null nếu không khả dụng (SSR/test)
const getAuthStorage = () => {
  try {
    return typeof window !== 'undefined' && window.sessionStorage ? window.sessionStorage : null;
  } catch {
    return null;
  }
};

export const authService = {
  /** Đăng nhập — trả về accessToken, refreshToken, user. Throw INVALID_CREDENTIALS / USER_INACTIVE. */
  login: async (email, password) => {
    if (useMock) {
      // Simulate network delay
      await new Promise(resolve => setTimeout(resolve, 800));
      
      const user = MOCK_USERS.find(
        u => u.email.toLowerCase() === email.toLowerCase()
      );
      
      if (!user) {
        throw new Error('INVALID_CREDENTIALS');
      }

      if (!user.isActive) {
        throw new Error('USER_INACTIVE');
      }
      
      // Return a simulated JWT token and user profile
      return {
        accessToken: `mock-jwt-token-for-${user.email}`,
        refreshToken: `mock-refresh-token-for-${user.email}`,
        user
      };
    } else {
      const response = await apiClient.post('/auth/login', { email, password });
      return response.data;
    }
  },

  /** Đăng xuất — gửi refreshToken để backend vô hiệu hóa phiên. */
  logout: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      return { success: true };
    } else {
      const refreshToken = getAuthStorage()?.getItem('wms_refresh_token');
      const response = await apiClient.post('/auth/logout', refreshToken ? { refreshToken } : undefined);
      return response.data;
    }
  },

  /** Lấy thông tin user hiện tại từ JWT (GET /auth/me). */
  getMe: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 200));
      const storedUser = getAuthStorage()?.getItem('wms_user');
      return storedUser ? JSON.parse(storedUser) : null;
    } else {
      const response = await apiClient.get('/auth/me');
      return response.data;
    }
  },

  /** Cập nhật hồ sơ cá nhân (tên, email, SĐT). */
  updateProfile: async (fullName, email, phone) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 600));
      const storedUser = JSON.parse(getAuthStorage()?.getItem('wms_user') || 'null');
      if (!storedUser) throw new Error('UNAUTHORIZED');
      
      const updatedUser = { ...storedUser, fullName, email, phone };
      getAuthStorage()?.setItem('wms_user', JSON.stringify(updatedUser));
      
      // Update in our mock DB too
      const dbUsersStr = localStorage.getItem('wms_db_users');
      if (dbUsersStr) {
        const dbUsers = JSON.parse(dbUsersStr);
        const uidx = dbUsers.findIndex(u => u.id === storedUser.id);
        if (uidx !== -1) {
          dbUsers[uidx] = updatedUser;
          localStorage.setItem('wms_db_users', JSON.stringify(dbUsers));
        }
      }

      const idx = MOCK_USERS.findIndex(u => u.id === storedUser.id);
      if (idx !== -1) MOCK_USERS[idx] = updatedUser;
      
      return updatedUser;
    } else {
      const response = await apiClient.put('/auth/profile', { fullName, email, phone });
      return response.data;
    }
  },

  /** Đổi mật khẩu — yêu cầu mật khẩu hiện tại + mới (tối thiểu 8 ký tự, 1 hoa, 1 thường, 1 số). */
  changePassword: async (currentPassword, newPassword) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 800));
      // Weak password checks (from Spec 001)
      if (newPassword.length < 8) {
        throw new Error('WEAK_PASSWORD');
      }
      return { success: true };
    } else {
      const response = await apiClient.put('/auth/change-password', { currentPassword, newPassword });
      return response.data;
    }
  },

  /** Gửi yêu cầu quên mật khẩu — backend gửi OTP 6 số qua email. */
  forgotPassword: async (email) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 800));
      return { success: true };
    } else {
      const response = await apiClient.post('/auth/forgot-password', { email });
      return response.data;
    }
  },

  /** Xác thực OTP + đặt mật khẩu mới — bước cuối trong luồng quên mật khẩu. */
  verifyOtp: async (email, otp, newPassword) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 800));
      if (otp !== '123456') throw new Error('INVALID_OTP');
      return { success: true };
    } else {
      const response = await apiClient.post('/auth/verify-otp', { email, otp, newPassword });
      return response.data;
    }
  },

  /** Kiểm tra OTP hợp lệ (chưa đặt mật khẩu mới) — dùng ở bước 2 quên mật khẩu. */
  checkOtp: async (email, otp) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      if (otp !== '123456') throw new Error('INVALID_OTP');
      return { success: true };
    } else {
      const response = await apiClient.post('/auth/otp/check', { email, otp });
      return response.data;
    }
  }
};
