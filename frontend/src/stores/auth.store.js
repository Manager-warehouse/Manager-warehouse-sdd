import { create } from 'zustand';
import { WAREHOUSES } from '../utils/constants';

export const useAuthStore = create((set, get) => {
  // Try to load initial session from localStorage
  const storedUser = localStorage.getItem('wms_user');
  const storedToken = localStorage.getItem('wms_token');
  const storedWarehouse = localStorage.getItem('wms_active_warehouse');

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

    login: (user, token) => {
      localStorage.setItem('wms_user', JSON.stringify(user));
      localStorage.setItem('wms_token', token);

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
        localStorage.setItem('wms_active_warehouse', JSON.stringify(activeWarehouse));
      } else {
        localStorage.removeItem('wms_active_warehouse');
      }

      set({ user, token, activeWarehouse });
    },

    logout: () => {
      localStorage.removeItem('wms_user');
      localStorage.removeItem('wms_token');
      localStorage.removeItem('wms_active_warehouse');
      set({ user: null, token: null, activeWarehouse: null });
    },

    setActiveWarehouse: (warehouse) => {
      localStorage.setItem('wms_active_warehouse', JSON.stringify(warehouse));
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
