import { useAuthStore } from '../../src/stores/auth.store';
import { ROLES, getDefaultRouteByRole } from '../../src/utils/constants';

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

  test('hasWarehouseAccess should return false when no user is logged in', () => {
    useAuthStore.getState().logout();
    expect(useAuthStore.getState().hasWarehouseAccess(1)).toBe(false);
  });

  test('hasWarehouseAccess should return false when user warehouses property is missing', () => {
    const userWithoutWarehouses = {
      id: 5,
      role: ROLES.STOREKEEPER
    };
    useAuthStore.getState().login(userWithoutWarehouses, 'mock-jwt-token');
    expect(useAuthStore.getState().hasWarehouseAccess(1)).toBe(false);
  });

  test('login should handle optional refreshToken', () => {
    const user = { id: 6, role: ROLES.STOREKEEPER, warehouses: [1] };
    useAuthStore.getState().login(user, 'mock-jwt-token', 'mock-refresh-token');
    expect(sessionStorage.getItem('wms_refresh_token')).toBe('mock-refresh-token');
  });

  test('login should handle user with invalid warehouse ID (not in constant list)', () => {
    const user = { id: 7, role: ROLES.STOREKEEPER, warehouses: [999] }; // 999 not in constant list
    useAuthStore.getState().login(user, 'mock-jwt-token');
    expect(useAuthStore.getState().activeWarehouse).toBeNull();
    expect(sessionStorage.getItem('wms_active_warehouse')).toBeNull();
  });

  test('setActiveWarehouse should update store state and sessionStorage', () => {
    const warehouse = { id: 2, code: 'HN-01', name: 'Kho Hà Nội' };
    useAuthStore.getState().setActiveWarehouse(warehouse);
    expect(useAuthStore.getState().activeWarehouse).toEqual(warehouse);
    expect(JSON.parse(sessionStorage.getItem('wms_active_warehouse'))).toEqual(warehouse);
  });

  test('hasRole should evaluate role correctly and handle null user', () => {
    // Null user
    useAuthStore.getState().logout();
    expect(useAuthStore.getState().hasRole(ROLES.ADMIN)).toBe(false);

    // Active user
    const user = { id: 8, role: ROLES.ADMIN, warehouses: [] };
    useAuthStore.getState().login(user, 'mock-jwt-token');
    expect(useAuthStore.getState().hasRole(ROLES.ADMIN)).toBe(true);
    expect(useAuthStore.getState().hasRole(ROLES.CEO)).toBe(false);
    expect(useAuthStore.getState().hasRole(ROLES.STOREKEEPER)).toBe(false);
    expect(useAuthStore.getState().hasRole(ROLES.DRIVER)).toBe(false);
  });

  test('getDefaultRouteByRole should return the correct default route for each role', () => {
    expect(getDefaultRouteByRole(ROLES.ADMIN)).toBe('/admin/users');
    expect(getDefaultRouteByRole(ROLES.CEO)).toBe('/reports/ceo-dashboard');
    expect(getDefaultRouteByRole(ROLES.ACCOUNTANT_MANAGER)).toBe('/finance/price-approval');
    expect(getDefaultRouteByRole(ROLES.ACCOUNTANT)).toBe('/finance/invoices');
    expect(getDefaultRouteByRole(ROLES.WAREHOUSE_MANAGER)).toBe('/inbound/receipts');
    expect(getDefaultRouteByRole(ROLES.STOREKEEPER)).toBe('/inbound/receipts');
    expect(getDefaultRouteByRole(ROLES.WAREHOUSE_STAFF)).toBe('/inbound/receipts');
    expect(getDefaultRouteByRole(ROLES.PLANNER)).toBe('/inbound/receipts');
    expect(getDefaultRouteByRole(ROLES.DISPATCHER)).toBe('/outbound/trips');
    expect(getDefaultRouteByRole(ROLES.DRIVER)).toBe('/outbound/driver/trips');
    expect(getDefaultRouteByRole('UNKNOWN')).toBe('/profile');
  });
});

