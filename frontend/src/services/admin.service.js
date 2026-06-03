import apiClient, { useMock } from './api.client';
import { MOCK_USERS } from '../utils/constants';

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
    return parsed;
  } catch {
    localStorage.setItem('wms_db_users', JSON.stringify(MOCK_USERS));
    return MOCK_USERS;
  }
};

const saveMockDbUsers = (users) => {
  localStorage.setItem('wms_db_users', JSON.stringify(users));
};

const getMockAuditLogs = () => {
  const logs = localStorage.getItem('wms_audit_logs');
  if (logs) return JSON.parse(logs);

  const initialLogs = [
    {
      id: 1,
      timestamp: new Date(Date.now() - 3600000 * 5).toISOString(),
      actorName: 'System Admin',
      actorRole: 'ADMIN',
      action: 'UPDATE',
      entityType: 'SystemConfig',
      entityId: 1,
      description: 'UPDATE SystemConfig 1',
      warehouseId: null,
      warehouseCode: null,
      oldValue: { closingDay: 3 },
      newValue: { closingDay: 5 },
      ipAddress: '127.0.0.1'
    },
    {
      id: 2,
      timestamp: new Date(Date.now() - 3600000 * 24).toISOString(),
      actorName: 'System Admin',
      actorRole: 'ADMIN',
      action: 'CREATE',
      entityType: 'User',
      entityId: 3,
      description: 'CREATE User 3',
      warehouseId: 1,
      warehouseCode: 'HP-01',
      oldValue: null,
      newValue: { email: 'manager_hp@phucanh.vn', role: 'WAREHOUSE_MANAGER' },
      ipAddress: '127.0.0.1'
    }
  ];
  localStorage.setItem('wms_audit_logs', JSON.stringify(initialLogs));
  return initialLogs;
};

const addMockAuditLog = (action, entityType, entityId, description, newValue = {}) => {
  const logs = getMockAuditLogs();
  const currentUser = JSON.parse(localStorage.getItem('wms_user')) || {};
  const newLog = {
    id: logs.length + 1,
    timestamp: new Date().toISOString(),
    actorName: currentUser.fullName || 'System Admin',
    actorRole: currentUser.role || 'ADMIN',
    action,
    entityType,
    entityId,
    description,
    warehouseId: null,
    warehouseCode: null,
    oldValue: null,
    newValue,
    ipAddress: '127.0.0.1'
  };
  localStorage.setItem('wms_audit_logs', JSON.stringify([newLog, ...logs]));
};

const getFilteredMockAuditLogs = ({
  page = 1,
  pageSize = 30,
  from,
  to,
  warehouseId
} = {}) => {
  const fromTime = from ? new Date(from).getTime() : null;
  const toTime = to ? new Date(to).getTime() : null;
  const filtered = getMockAuditLogs()
    .filter((log) => {
      const logTime = new Date(log.timestamp).getTime();
      const matchesFrom = !fromTime || logTime >= fromTime;
      const matchesTo = !toTime || logTime <= toTime;
      const matchesWarehouse = !warehouseId || String(log.warehouseId) === String(warehouseId);
      return matchesFrom && matchesTo && matchesWarehouse;
    })
    .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

  const start = (page - 1) * pageSize;
  return {
    data: filtered.slice(start, start + pageSize),
    page,
    pageSize,
    hasNext: start + pageSize < filtered.length,
    hasPrevious: page > 1,
    requiresFilterForOlder: !from && !to && !warehouseId && page >= 50
  };
};

export const adminService = {
  getUsers: async () => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 500));
      return getMockDbUsers();
    }
    const response = await apiClient.get('/admin/users');
    return response.data;
  },

  getUserById: async (id) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 300));
      const user = getMockDbUsers().find((item) => item.id === Number(id));
      if (!user) throw new Error('USER_NOT_FOUND');
      return user;
    }
    const response = await apiClient.get(`/admin/users/${id}`);
    return response.data;
  },

  createUser: async (userData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 800));
      const users = getMockDbUsers();
      const exists = users.some((u) => u.email.toLowerCase() === userData.email.toLowerCase());
      if (exists) throw new Error('EMAIL_TAKEN');
      if (userData.password && userData.password.length < 8) throw new Error('WEAK_PASSWORD');

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
      addMockAuditLog('CREATE', 'User', newUser.id, `CREATE User ${newUser.id}`, {
        email: newUser.email,
        role: newUser.role
      });
      return newUser;
    }
    const response = await apiClient.post('/admin/users', userData);
    return response.data;
  },

  updateUser: async (id, userData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 800));
      const users = getMockDbUsers();
      const idx = users.findIndex((u) => u.id === Number(id));
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

      const currentUser = JSON.parse(localStorage.getItem('wms_user'));
      if (currentUser && currentUser.id === Number(id)) {
        localStorage.setItem('wms_user', JSON.stringify(updatedUser));
      }

      users[idx] = updatedUser;
      saveMockDbUsers(users);
      addMockAuditLog('UPDATE', 'User', Number(id), `UPDATE User ${id}`, {
        fullName: updatedUser.fullName,
        role: updatedUser.role
      });
      return updatedUser;
    }
    const response = await apiClient.put(`/admin/users/${id}`, userData);
    return response.data;
  },

  toggleUserStatus: async (id, isActive) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const users = getMockDbUsers();
      const idx = users.findIndex((u) => u.id === Number(id));
      if (idx === -1) throw new Error('USER_NOT_FOUND');

      users[idx].isActive = isActive;
      saveMockDbUsers(users);
      const action = isActive ? 'STATUS_CHANGE' : 'SOFT_DELETE';
      addMockAuditLog(action, 'User', Number(id), `${action} User ${id}`, {
        isActive
      });
      return users[idx];
    }
    const response = await apiClient.put(`/admin/users/${id}/status`, { isActive });
    return response.data;
  },

  getAuditLogs: async (params = {}) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      return getFilteredMockAuditLogs(params);
    }
    const response = await apiClient.get('/audit-logs', { params });
    return response.data;
  },

  getAuditLogById: async (id) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 250));
      const log = getMockAuditLogs().find((item) => item.id === Number(id));
      if (!log) throw new Error('AUDIT_LOG_NOT_FOUND');
      return log;
    }
    const response = await apiClient.get(`/audit-logs/${id}`);
    return response.data;
  }
};
