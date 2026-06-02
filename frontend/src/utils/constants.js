// System constant values for WMS - Spec 001/002 aligned with database.md

export const ROLES = {
  ADMIN: 'ADMIN',
  CEO: 'CEO',
  WAREHOUSE_MANAGER: 'WAREHOUSE_MANAGER',
  STOREKEEPER: 'STOREKEEPER',
  WAREHOUSE_STAFF: 'WAREHOUSE_STAFF',
  ACCOUNTANT: 'ACCOUNTANT',
  ACCOUNTANT_MANAGER: 'ACCOUNTANT_MANAGER',
  PLANNER: 'PLANNER',
  DISPATCHER: 'DISPATCHER',
  DRIVER: 'DRIVER',
  REPORT_VIEWER: 'REPORT_VIEWER',
};

export const ROLE_LABELS = {
  [ROLES.ADMIN]: 'System Administrator',
  [ROLES.CEO]: 'CEO / Director',
  [ROLES.WAREHOUSE_MANAGER]: 'Warehouse Manager',
  [ROLES.STOREKEEPER]: 'Storekeeper',
  [ROLES.WAREHOUSE_STAFF]: 'Warehouse Staff',
  [ROLES.ACCOUNTANT]: 'Accountant',
  [ROLES.ACCOUNTANT_MANAGER]: 'Accountant Manager',
  [ROLES.PLANNER]: 'Inventory Planner',
  [ROLES.DISPATCHER]: 'Dispatcher',
  [ROLES.DRIVER]: 'Driver',
  [ROLES.REPORT_VIEWER]: 'Report Viewer',
};

export const WAREHOUSES = [
  { id: 1, code: 'HP-01', name: 'Kho Hải Phòng', address: 'Số 1 Lê Thánh Tông, Ngô Quyền, Hải Phòng' },
  { id: 2, code: 'HN-01', name: 'Kho Hà Nội', address: 'Số 15 Cầu Giấy, Quan Hoa, Hà Nội' },
  { id: 3, code: 'HCM-01', name: 'Kho Hồ Chí Minh', address: 'Số 100 Cộng Hòa, Tân Bình, TP. HCM' },
];

export const MOCK_USERS = [
  {
    id: 1,
    code: 'NV-001',
    fullName: 'Nguyễn Văn Admin',
    email: 'admin@phucanh.vn',
    phone: '0912345678',
    role: ROLES.ADMIN,
    jobTitle: 'Quản trị hệ thống',
    shift: 'Hành chính',
    region: 'Toàn quốc',
    warehouses: [], // NULL/empty assignment means all warehouses in assigns table
    isActive: true,
  },
  {
    id: 2,
    code: 'NV-002',
    fullName: 'Trần Phúc Anh',
    email: 'ceo@phucanh.vn',
    phone: '0987654321',
    role: ROLES.CEO,
    jobTitle: 'Giám đốc điều hành',
    shift: 'Hành chính',
    region: 'Toàn quốc',
    warehouses: [1, 2, 3],
    isActive: true,
  },
  {
    id: 3,
    code: 'NV-003',
    fullName: 'Lê Văn Trưởng Kho',
    email: 'manager.hp@phucanh.vn',
    phone: '0901234567',
    role: ROLES.WAREHOUSE_MANAGER,
    jobTitle: 'Trưởng kho Hải Phòng',
    shift: 'Ca sáng',
    region: 'Hải Phòng',
    warehouses: [1], // HP only
    isActive: true,
  },
  {
    id: 4,
    code: 'NV-004',
    fullName: 'Phạm Thủ Kho',
    email: 'keeper.hn@phucanh.vn',
    phone: '0934567890',
    role: ROLES.STOREKEEPER,
    jobTitle: 'Thủ kho Hà Nội',
    shift: 'Ca chiều',
    region: 'Hà Nội',
    warehouses: [2], // HN only
    isActive: true,
  },
];
