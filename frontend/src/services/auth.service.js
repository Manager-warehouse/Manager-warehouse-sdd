import apiClient, { useMock } from './api.client';
import { MOCK_USERS } from '../utils/constants';

export const authService = {
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

  logout: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      return { success: true };
    } else {
      const response = await apiClient.post('/auth/logout');
      return response.data;
    }
  },

  getMe: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 200));
      const storedUser = sessionStorage.getItem('wms_user');
      return storedUser ? JSON.parse(storedUser) : null;
    } else {
      const response = await apiClient.get('/auth/me');
      return response.data;
    }
  },

  updateProfile: async (fullName, email, phone) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 600));
      const storedUser = JSON.parse(sessionStorage.getItem('wms_user'));
      if (!storedUser) throw new Error('UNAUTHORIZED');
      
      const updatedUser = { ...storedUser, fullName, email, phone };
      sessionStorage.setItem('wms_user', JSON.stringify(updatedUser));
      
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

  forgotPassword: async (email) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 800));
      return { success: true };
    } else {
      const response = await apiClient.post('/auth/forgot-password', { email });
      return response.data;
    }
  },

  verifyOtp: async (email, otp, newPassword) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 800));
      if (otp !== '123456') throw new Error('INVALID_OTP');
      return { success: true };
    } else {
      const response = await apiClient.post('/auth/verify-otp', { email, otp, newPassword });
      return response.data;
    }
  }
};
