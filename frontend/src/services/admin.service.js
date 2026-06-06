import apiClient, { useMock } from './api.client';
import { MOCK_USERS } from '../utils/constants';

// Seed and retrieve user accounts from localStorage to act as a persistent mock database during development
const getMockDbUsers = () => {
  const users = localStorage.getItem('wms_db_users');
  if (!users) {
    localStorage.setItem('wms_db_users', JSON.stringify(MOCK_USERS));
    return MOCK_USERS;
  }
  try {
    const parsed = JSON.parse(users);
    if (parsed && parsed.length > 0 && 'username' in parsed[0]) {
      localStorage.setItem('wms_db_users', JSON.stringify(MOCK_USERS));
      localStorage.removeItem('wms_user');
      localStorage.removeItem('wms_token');
      localStorage.removeItem('wms_active_warehouse');
      return MOCK_USERS;
    }
    if (parsed && parsed.length < MOCK_USERS.length) {
      localStorage.setItem('wms_db_users', JSON.stringify(MOCK_USERS));
      return MOCK_USERS;
    }
    return parsed;
  } catch (e) {
    console.error('Failed to parse mock DB users, resetting to default seed data:', e);
    localStorage.setItem('wms_db_users', JSON.stringify(MOCK_USERS));
    return MOCK_USERS;
  }
};

const saveMockDbUsers = (users) => {
  localStorage.setItem('wms_db_users', JSON.stringify(users));
};

// Store mock audit records locally to simulate tracking of system alterations
const getMockAuditLogs = () => {
  const logs = localStorage.getItem('wms_audit_logs');
  if (!logs) {
    const initialLogs = [
      {
        id: 1,
        actorName: 'Nguyễn Văn Admin',
        action: 'USER_CREATED',
        entityType: 'User',
        entityId: 3,
        details: 'Tạo tài khoản quản lý kho Hải Phòng manager_hp',
        createdAt: new Date(Date.now() - 3600000 * 24).toISOString()
      },
      {
        id: 2,
        actorName: 'Nguyễn Văn Admin',
        action: 'SYSTEM_CONFIG_UPDATED',
        entityType: 'SystemConfig',
        entityId: 1,
        details: 'Cập nhật thời hạn khóa sổ kế toán thành ngày 5 hàng tháng',
        createdAt: new Date(Date.now() - 3600000 * 5).toISOString()
      }
    ];
    localStorage.setItem('wms_audit_logs', JSON.stringify(initialLogs));
    return initialLogs;
  }
  return JSON.parse(logs);
};

const addMockAuditLog = (action, entityType, entityId, details) => {
  const logs = getMockAuditLogs();
  const currentUser = JSON.parse(localStorage.getItem('wms_user')) || { fullName: 'System' };
  const newLog = {
    id: logs.length + 1,
    actorName: currentUser.fullName,
    action,
    entityType,
    entityId,
    details,
    createdAt: new Date().toISOString()
  };
  localStorage.setItem('wms_audit_logs', JSON.stringify([newLog, ...logs]));
};

// Store system parameter thresholds locally to persist across browser updates
const getMockSystemConfig = () => {
  const config = localStorage.getItem('wms_system_config');
  if (!config) {
    const initialConfig = {
      defaultCreditLimit: 500000000,
      defaultPaymentTermDays: 30,
      creditHoldOverdueDays: 30,
      creditUnlockBufferPct: 0.8,
      monthlyClosingDay: 5,
      minInventoryWarningThreshold: 10
    };
    localStorage.setItem('wms_system_config', JSON.stringify(initialConfig));
    return initialConfig;
  }
  const parsed = JSON.parse(config);
  // Migrate legacy data schemas to prevent page crashes for users with old state cached
  if ('managerApprovalLimit' in parsed || 'shiftDurationHours' in parsed) {
    const migrated = {
      defaultCreditLimit: parsed.defaultCreditLimit ?? 500000000,
      defaultPaymentTermDays: parsed.defaultPaymentTermDays ?? 30,
      creditHoldOverdueDays: parsed.creditHoldOverdueDays ?? 30,
      creditUnlockBufferPct: parsed.creditUnlockBufferPct ?? 0.8,
      monthlyClosingDay: parsed.monthlyClosingDay ?? 5,
      minInventoryWarningThreshold: parsed.minWarningStock ?? parsed.minInventoryWarningThreshold ?? 10
    };
    localStorage.setItem('wms_system_config', JSON.stringify(migrated));
    return migrated;
  }
  return parsed;
};

const saveMockSystemConfig = (config) => {
  localStorage.setItem('wms_system_config', JSON.stringify(config));
};

export const adminService = {
  getUsers: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      return getMockDbUsers();
    } else {
      const response = await apiClient.get('/admin/users');
      return response.data;
    }
  },

  getUserById: async (id) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      const users = getMockDbUsers();
      const user = users.find(u => u.id === Number(id));
      if (!user) throw new Error('USER_NOT_FOUND');
      return user;
    } else {
      const response = await apiClient.get(`/admin/users/${id}`);
      return response.data;
    }
  },

  createUser: async (userData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 800));
      
      const users = getMockDbUsers();
      // Email uniqueness check
      const exists = users.some(u => u.email.toLowerCase() === userData.email.toLowerCase());
      if (exists) {
        throw new Error('EMAIL_TAKEN');
      }

      // Password strength validation (min 8 chars)
      if (userData.password && userData.password.length < 8) {
        throw new Error('WEAK_PASSWORD');
      }

      const newUser = {
        id: users.length + 1,
        code: userData.code,
        fullName: userData.fullName,
        email: userData.email,
        phone: userData.phone,
        role: userData.role,
        jobTitle: userData.jobTitle,
        shift: userData.shift,
        region: userData.region,
        warehouses: userData.warehouses.map(Number),
        isActive: true
      };

      users.push(newUser);
      saveMockDbUsers(users);
      addMockAuditLog('USER_CREATED', 'User', newUser.id, `Tạo tài khoản ${newUser.email} (${newUser.fullName})`);

      return newUser;
    } else {
      const response = await apiClient.post('/admin/users', userData);
      return response.data;
    }
  },

  updateUser: async (id, userData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 800));
      const users = getMockDbUsers();
      const idx = users.findIndex(u => u.id === Number(id));
      if (idx === -1) throw new Error('USER_NOT_FOUND');

      const oldUser = users[idx];
      const updatedUser = {
        ...oldUser,
        fullName: userData.fullName,
        email: userData.email,
        phone: userData.phone,
        role: userData.role,
        jobTitle: userData.jobTitle,
        shift: userData.shift,
        region: userData.region,
        warehouses: userData.warehouses.map(Number),
        isActive: userData.isActive !== undefined ? userData.isActive : oldUser.isActive
      };

      // If updating active session user
      const currentUser = JSON.parse(localStorage.getItem('wms_user'));
      if (currentUser && currentUser.id === Number(id)) {
        localStorage.setItem('wms_user', JSON.stringify(updatedUser));
      }

      users[idx] = updatedUser;
      saveMockDbUsers(users);
      addMockAuditLog('USER_UPDATED', 'User', id, `Cập nhật tài khoản ${updatedUser.email} (${updatedUser.fullName})`);

      return updatedUser;
    } else {
      const response = await apiClient.put(`/admin/users/${id}`, userData);
      return response.data;
    }
  },

  toggleUserStatus: async (id, isActive) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const users = getMockDbUsers();
      const idx = users.findIndex(u => u.id === Number(id));
      if (idx === -1) throw new Error('USER_NOT_FOUND');

      users[idx].isActive = isActive;
      saveMockDbUsers(users);
      
      const action = isActive ? 'USER_ACTIVATED' : 'USER_DEACTIVATED';
      addMockAuditLog(action, 'User', id, `${isActive ? 'Kích hoạt' : 'Khóa'} tài khoản ${users[idx].email}`);
      
      return users[idx];
    } else {
      const response = await apiClient.put(`/admin/users/${id}/status`, { isActive });
      return response.data;
    }
  },

  getSystemConfig: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      return getMockSystemConfig();
    } else {
      const response = await apiClient.get('/admin/config');
      return response.data;
    }
  },

  updateSystemConfig: async (configData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 600));
      const current = getMockSystemConfig();
      const updated = { ...current, ...configData };
      saveMockSystemConfig(updated);

      addMockAuditLog(
        'SYSTEM_CONFIG_UPDATED',
        'SystemConfig',
        1,
        `Cập nhật cấu hình hệ thống: Ngày khóa sổ ${updated.monthlyClosingDay}, Hạn mức nợ ${updated.defaultCreditLimit?.toLocaleString()} VND, Thời hạn thanh toán ${updated.defaultPaymentTermDays} ngày`
      );

      return updated;
    } else {
      const response = await apiClient.put('/admin/config', configData);
      return response.data;
    }
  },

  getAuditLogs: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      return getMockAuditLogs();
    } else {
      const response = await apiClient.get('/audit-logs');
      return response.data;
    }
  }
};
