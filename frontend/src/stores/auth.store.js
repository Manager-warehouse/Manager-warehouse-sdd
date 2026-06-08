import { create } from 'zustand';
import { WAREHOUSES } from '../utils/constants';

export const useAuthStore = create((set, get) => {
  // Try to load initial session from sessionStorage
  const storedUser = sessionStorage.getItem('wms_user');
  const storedToken = sessionStorage.getItem('wms_token');
  const storedWarehouse = sessionStorage.getItem('wms_active_warehouse');

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

    login: (user, token, refreshToken) => {
      sessionStorage.setItem('wms_user', JSON.stringify(user));
      sessionStorage.setItem('wms_token', token);
      if (refreshToken) {
        sessionStorage.setItem('wms_refresh_token', refreshToken);
      }

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
        sessionStorage.setItem('wms_active_warehouse', JSON.stringify(activeWarehouse));
      } else {
        sessionStorage.removeItem('wms_active_warehouse');
      }

      set({ user, token, activeWarehouse });
    },

    logout: () => {
      sessionStorage.removeItem('wms_user');
      sessionStorage.removeItem('wms_token');
      sessionStorage.removeItem('wms_refresh_token');
      sessionStorage.removeItem('wms_active_warehouse');
      set({ user: null, token: null, activeWarehouse: null });
    },

    setActiveWarehouse: (warehouse) => {
      sessionStorage.setItem('wms_active_warehouse', JSON.stringify(warehouse));
      set({ activeWarehouse: warehouse });
    },

    hasRole: (role) => {
      const user = get().user;
      if (!user) return false;
      return user.role === role;
    },

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
