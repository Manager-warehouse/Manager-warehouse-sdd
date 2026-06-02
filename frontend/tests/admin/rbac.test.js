import { useAuthStore } from '../../src/stores/auth.store';
import { ROLES } from '../../src/utils/constants';

// Mock Zustand to work without a full DOM environment if needed
describe('RBAC Warehouse and Role Access Tests', () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
  });

  test('ADMIN should have access to all warehouses', () => {
    const adminUser = {
      id: 1,
      role: ROLES.ADMIN,
      warehouses: []
    };
    useAuthStore.getState().login(adminUser, 'mock-jwt-token');
    
    expect(useAuthStore.getState().hasWarehouseAccess(1)).toBe(true);
    expect(useAuthStore.getState().hasWarehouseAccess(2)).toBe(true);
    expect(useAuthStore.getState().hasWarehouseAccess(999)).toBe(true);
  });

  test('CEO should have access to all warehouses', () => {
    const ceoUser = {
      id: 2,
      role: ROLES.CEO,
      warehouses: [1, 2, 3]
    };
    useAuthStore.getState().login(ceoUser, 'mock-jwt-token');

    expect(useAuthStore.getState().hasWarehouseAccess(1)).toBe(true);
    expect(useAuthStore.getState().hasWarehouseAccess(2)).toBe(true);
    expect(useAuthStore.getState().hasWarehouseAccess(3)).toBe(true);
  });

  test('WAREHOUSE_MANAGER should only have access to assigned warehouses', () => {
    const managerUser = {
      id: 3,
      role: ROLES.WAREHOUSE_MANAGER,
      warehouses: [1]
    };
    useAuthStore.getState().login(managerUser, 'mock-jwt-token');

    expect(useAuthStore.getState().hasWarehouseAccess(1)).toBe(true);
    expect(useAuthStore.getState().hasWarehouseAccess(2)).toBe(false);
    expect(useAuthStore.getState().hasWarehouseAccess(3)).toBe(false);
  });

  test('User with empty warehouses should be blocked by default', () => {
    const restrictedUser = {
      id: 4,
      role: ROLES.STOREKEEPER,
      warehouses: []
    };
    useAuthStore.getState().login(restrictedUser, 'mock-jwt-token');

    expect(useAuthStore.getState().hasWarehouseAccess(1)).toBe(false);
    expect(useAuthStore.getState().hasWarehouseAccess(2)).toBe(false);
  });
});
