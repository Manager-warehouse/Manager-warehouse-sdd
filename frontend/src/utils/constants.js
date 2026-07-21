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
};

export const ROLE_LABELS = {
  [ROLES.ADMIN]: 'Quản trị hệ thống',
  [ROLES.CEO]: 'Giám đốc điều hành',
  [ROLES.WAREHOUSE_MANAGER]: 'Quản lý kho',
  [ROLES.STOREKEEPER]: 'Thủ kho',
  [ROLES.WAREHOUSE_STAFF]: 'Nhân viên kho',
  [ROLES.ACCOUNTANT]: 'Kế toán viên',
  [ROLES.ACCOUNTANT_MANAGER]: 'Kế toán trưởng',
  [ROLES.PLANNER]: 'Nhân viên kế hoạch',
  [ROLES.DISPATCHER]: 'Điều phối viên',
  [ROLES.DRIVER]: 'Tài xế',
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
    fullName: 'Anh Phương Quản Trị',
    email: 'admin@quanlykho.vn',
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
    fullName: 'Trần Quản Lý',
    email: 'ceo@quanlykho.vn',
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
    email: 'manager.hp@quanlykho.vn',
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
    email: 'keeper.hn@quanlykho.vn',
    phone: '0934567890',
    role: ROLES.STOREKEEPER,
    jobTitle: 'Thủ kho Hà Nội',
    shift: 'Ca chiều',
    region: 'Hà Nội',
    warehouses: [2], // HN only
    isActive: true,
  },
  {
    id: 5,
    code: 'NV-005',
    fullName: 'Nguyễn Kế Toán',
    email: 'accountant@quanlykho.vn',
    phone: '0911222333',
    role: ROLES.ACCOUNTANT,
    jobTitle: 'Kế toán viên',
    shift: 'Hành chính',
    region: 'Toàn quốc',
    warehouses: [1, 2, 3],
    isActive: true,
  },
  {
    id: 6,
    code: 'NV-006',
    fullName: 'Phạm Kế Toán Trưởng',
    email: 'acc_manager@quanlykho.vn',
    phone: '0922333444',
    role: ROLES.ACCOUNTANT_MANAGER,
    jobTitle: 'Kế toán trưởng',
    shift: 'Hành chính',
    region: 'Toàn quốc',
    warehouses: [1, 2, 3],
    isActive: true,
  },
  {
    id: 7,
    code: 'NV-007',
    fullName: 'Trần Kế Hoạch',
    email: 'planner@quanlykho.vn',
    phone: '0933444555',
    role: ROLES.PLANNER,
    jobTitle: 'Nhân viên Kế hoạch',
    shift: 'Hành chính',
    region: 'Toàn quốc',
    warehouses: [1, 2, 3],
    isActive: true,
  },
  {
    id: 8,
    code: 'NV-008',
    fullName: 'Lê Điều Phối',
    email: 'dispatcher@quanlykho.vn',
    phone: '0944555666',
    role: ROLES.DISPATCHER,
    jobTitle: 'Điều phối viên',
    shift: 'Hành chính',
    region: 'Toàn quốc',
    warehouses: [1, 2, 3],
    isActive: true,
  },
  {
    id: 13,
    code: 'NV-013',
    fullName: 'Nguyễn Văn Tài Xế 1',
    email: 'driver1@quanlykho.vn',
    phone: '0904445556',
    role: ROLES.DRIVER,
    jobTitle: 'Tài xế nội bộ',
    shift: 'Hành chính',
    region: 'Hải Phòng',
    warehouses: [1],
    isActive: true,
  },
  {
    id: 14,
    code: 'NV-014',
    fullName: 'Trần Văn Giao Hàng',
    email: 'driver2@quanlykho.vn',
    phone: '0905556667',
    role: ROLES.DRIVER,
    jobTitle: 'Tài xế nội bộ',
    shift: 'Hành chính',
    region: 'Hà Nội',
    warehouses: [2],
    isActive: true,
  },
  {
    id: 9,
    code: 'NV-009',
    fullName: 'Nguyễn Văn QC',
    email: 'qc.hp@quanlykho.vn',
    phone: '0955666777',
    role: ROLES.WAREHOUSE_STAFF,
    jobTitle: 'Nhân viên QC Hải Phòng',
    shift: 'Ca sáng',
    region: 'Hải Phòng',
    warehouses: [1],
    isActive: true,
  },
  {
    id: 10,
    code: 'NV-010',
    fullName: 'Lê Thủ Kho HCM',
    email: 'keeper.hcm@quanlykho.vn',
    phone: '0966777888',
    role: ROLES.STOREKEEPER,
    jobTitle: 'Thủ kho Hồ Chí Minh',
    shift: 'Ca sáng',
    region: 'Hồ Chí Minh',
    warehouses: [3],
    isActive: true,
  },
];

export const getDefaultRouteByRole = (role) => {
  switch (role) {
    case ROLES.ADMIN:
      return '/admin/users';
    case ROLES.CEO:
      return '/reports/ceo-dashboard';
    case ROLES.ACCOUNTANT_MANAGER:
      return '/finance/price-approval';
    case ROLES.ACCOUNTANT:
      return '/finance/invoices';
    case ROLES.WAREHOUSE_MANAGER:
    case ROLES.STOREKEEPER:
    case ROLES.WAREHOUSE_STAFF:
    case ROLES.PLANNER:
      return '/inbound/receipts';
    case ROLES.DISPATCHER:
      return '/outbound/trips';
    case ROLES.DRIVER:
      return '/outbound/driver/trips';
    default:
      return '/profile';
  }
};

