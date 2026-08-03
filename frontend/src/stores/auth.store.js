/**
 * Store xác thực Zustand (Spec 001).
 * Quản lý: user, token, activeWarehouse (kho đang làm việc).
 * Dùng sessionStorage để mỗi tab trình duyệt giữ phiên riêng.
 * Cung cấp: login, logout, updateTokens, setActiveWarehouse, hasRole, hasWarehouseAccess.
 */
import { create } from 'zustand';
import { WAREHOUSES } from '../utils/constants';

// Fallback storage cho môi trường không có window (SSR/test)
const createMemoryStorage = () => {
  const values = new Map();
  return {
    getItem: (key) => values.get(key) || null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  };
};

// Lấy storage an toàn — fallback sang memory nếu không khả dụng
const getBrowserStorage = (name) => {
  try {
    return typeof window !== 'undefined' && window[name] ? window[name] : createMemoryStorage();
  } catch {
    return createMemoryStorage();
  }
};

export const useAuthStore = create((set, get) => {
  // Dùng sessionStorage để mỗi tab trình duyệt giữ phiên riêng
  const storage = getBrowserStorage('sessionStorage');
  const legacyStorage = getBrowserStorage('localStorage');
  // Di chuyển dữ liệu cũ từ localStorage sang sessionStorage (tương thích ngược)
  const migrateLegacyValue = (key) => {
    const current = storage.getItem(key);
    if (current) return current;
    const legacy = legacyStorage.getItem(key);
    if (legacy) {
      storage.setItem(key, legacy);
      legacyStorage.removeItem(key);
    }
    return legacy;
  };

  const storedUser = migrateLegacyValue('wms_user');
  const storedToken = migrateLegacyValue('wms_token');
  const storedWarehouse = migrateLegacyValue('wms_active_warehouse');
  migrateLegacyValue('wms_refresh_token');

  let parsedUser = null;
  let parsedWarehouse = null;

  try {
    if (storedUser) parsedUser = JSON.parse(storedUser);
    if (storedWarehouse) parsedWarehouse = JSON.parse(storedWarehouse);
  } catch (e) {
    console.error('Failed to parse stored auth session', e);
  }

  return {
    user: parsedUser,
    token: storedToken,
    activeWarehouse: parsedWarehouse,

    // Đăng nhập — lưu user/token vào session, tính kho mặc định theo warehouse assignment
    login: (user, token, refreshToken) => {
      storage.setItem('wms_user', JSON.stringify(user));
      storage.setItem('wms_token', token);
      legacyStorage.removeItem('wms_user');
      legacyStorage.removeItem('wms_token');
      if (refreshToken) {
        storage.setItem('wms_refresh_token', refreshToken);
      } else {
        storage.removeItem('wms_refresh_token');
      }
      legacyStorage.removeItem('wms_refresh_token');

      // Default active warehouse to first assigned warehouse or first warehouse
      let activeWarehouse = null;
      if (user.warehouses && user.warehouses.length > 0) {
        // Find assigned warehouse details
        const assigned = WAREHOUSES.find(w => user.warehouses.includes(w.id));
        if (assigned) activeWarehouse = assigned;
      } else {
        // SYSTEM_ADMIN / CEO have access to all warehouses. Default to HP-01
        activeWarehouse = WAREHOUSES[0];
      }

      if (activeWarehouse) {
        storage.setItem('wms_active_warehouse', JSON.stringify(activeWarehouse));
        legacyStorage.removeItem('wms_active_warehouse');
      } else {
        storage.removeItem('wms_active_warehouse');
        legacyStorage.removeItem('wms_active_warehouse');
      }

      set({ user, token, activeWarehouse });
    },

    // Cập nhật access/refresh token (sau khi refresh token rotation)
    updateTokens: (token, refreshToken) => {
      storage.setItem('wms_token', token);
      legacyStorage.removeItem('wms_token');
      if (refreshToken) {
        storage.setItem('wms_refresh_token', refreshToken);
      } else {
        storage.removeItem('wms_refresh_token');
      }
      legacyStorage.removeItem('wms_refresh_token');
      set({ token });
    },

    // Đăng xuất — xóa sạch session và reset state
    logout: () => {
      storage.removeItem('wms_user');
      storage.removeItem('wms_token');
      storage.removeItem('wms_refresh_token');
      storage.removeItem('wms_active_warehouse');
      legacyStorage.removeItem('wms_user');
      legacyStorage.removeItem('wms_token');
      legacyStorage.removeItem('wms_refresh_token');
      legacyStorage.removeItem('wms_active_warehouse');
      set({ user: null, token: null, activeWarehouse: null });
    },

    // Chuyển kho làm việc hiện tại
    setActiveWarehouse: (warehouse) => {
      storage.setItem('wms_active_warehouse', JSON.stringify(warehouse));
      legacyStorage.removeItem('wms_active_warehouse');
      set({ activeWarehouse: warehouse });
    },

    // Kiểm tra user hiện tại có role cụ thể không
    hasRole: (role) => {
      const user = get().user;
      if (!user) return false;
      return user.role === role;
    },

    // Kiểm tra user có quyền truy cập kho cụ thể (ADMIN/CEO = tất cả kho)
    hasWarehouseAccess: (warehouseId) => {
      const user = get().user;
      if (!user) return false;
      // ADMIN or CEO has access to all warehouses
      if (user.role === 'ADMIN' || user.role === 'CEO') return true;
      // Empty array implies no warehouse gán - block access by default
      if (!user.warehouses || user.warehouses.length === 0) return false;
      return user.warehouses.includes(Number(warehouseId));
    }
  };
});
