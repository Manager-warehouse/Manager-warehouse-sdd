import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Users, UserSquare, ShieldAlert, BarChart3, Package2, Settings, History, Box, Warehouse, Handshake, Truck, MapPin, PackageCheck, ClipboardList, DollarSign, CheckSquare, ArrowRightLeft } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { ROLES } from '../../utils/constants';

const Sidebar = () => {
  const { user, hasRole } = useAuthStore();
  const sidebarOpen = useUiStore((state) => state.sidebarOpen);

  const menuItems = [
    {
      title: 'Tổng quan',
      path: '/dashboard',
      icon: LayoutDashboard,
      roles: [] // All roles
    },
    {
      title: 'Quản lý tài khoản',
      path: '/admin/users',
      icon: Users,
      roles: [ROLES.ADMIN]
    },
    {
      title: 'Cấu hình hệ thống',
      path: '/admin/config',
      icon: Settings,
      roles: [ROLES.ADMIN]
    },
    {
      title: 'Nhật ký hoạt động',
      path: '/admin/audit-logs',
      icon: History,
      roles: [ROLES.ADMIN]
    },
    {
      title: 'Trang cá nhân',
      path: '/profile',
      icon: UserSquare,
      roles: []
    }
  ];

  const masterDataItems = [
    {
      title: 'SKU & Sản phẩm',
      path: '/admin/products',
      icon: Box,
      roles: [ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.PLANNER, ROLES.ADMIN, ROLES.CEO]
    },
    {
      title: 'Kho & Vị trí',
      path: '/admin/warehouses',
      icon: Warehouse,
      roles: [ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]
    },
    {
      title: 'Đại lý & Nhà CC',
      path: '/admin/partners',
      icon: Handshake,
      roles: [ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN, ROLES.CEO]
    },
    {
      title: 'Đội xe & Tài xế',
      path: '/admin/fleet',
      icon: Truck,
      roles: [ROLES.DISPATCHER, ROLES.ADMIN, ROLES.CEO]
    }
  ];

  const inboundItems = [
    {
      title: 'Phiếu Nhập & QC',
      path: '/inbound/receipts',
      icon: Package2,
      roles: [ROLES.PLANNER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]
    },
    {
      title: 'Xử lý hàng lỗi',
      path: '/inbound/quarantine',
      icon: ShieldAlert,
      roles: [ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.CEO, ROLES.ADMIN]
    },
    {
      title: 'Trả hàng đại lý',
      path: '/inbound/returns',
      icon: ArrowRightLeft,
      roles: [ROLES.WAREHOUSE_STAFF, ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.CEO, ROLES.ADMIN]
    }
  ];

  const transferItems = [
    {
      title: 'Điều chuyển nội bộ',
      path: '/transfers',
      icon: Package2,
      roles: [ROLES.PLANNER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.WAREHOUSE_MANAGER, ROLES.DISPATCHER, ROLES.ADMIN, ROLES.CEO]
    }
  ];

  const outboundItems = [
    {
      title: 'Đơn xuất hàng',
      path: '/outbound/delivery-orders',
      icon: PackageCheck,
      roles: [ROLES.PLANNER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.WAREHOUSE_MANAGER, ROLES.DISPATCHER, ROLES.ACCOUNTANT, ROLES.ADMIN, ROLES.CEO]
    },
    {
      title: 'Quản lý chuyến xe',
      path: '/outbound/trips',
      icon: Truck,
      roles: [ROLES.DISPATCHER, ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]
    },
    {
      title: 'Giao hàng của tôi',
      path: '/outbound/driver/trips',
      icon: MapPin,
      roles: [ROLES.DRIVER, ROLES.ADMIN]
    }
  ];

  const stocktakeItems = [
    {
      title: 'Danh sách kiểm kê',
      path: '/stocktake',
      icon: ClipboardList,
      roles: [ROLES.WAREHOUSE_MANAGER, ROLES.STOREKEEPER, ROLES.CEO, ROLES.ADMIN],
    },
  ];

  const financeItems = [
    {
      title: 'Bảng giá',
      path: '/finance/price-list',
      icon: DollarSign,
      roles: [ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN, ROLES.CEO]
    },
    {
      title: 'Duyệt bảng giá',
      path: '/finance/price-approval',
      icon: CheckSquare,
      roles: [ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN]
    }
  ];

  const reportItems = [
    {
      title: 'Báo cáo quản trị (CEO)',
      path: '/reports/ceo-dashboard',
      icon: BarChart3,
      roles: [ROLES.CEO, ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN]
    },
    {
      title: 'Báo cáo giá trị tồn',
      path: '/reports/inventory-valuation',
      icon: DollarSign,
      roles: [ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN]
    },
    {
      title: 'Cảnh báo tồn kho',
      path: '/reports/low-stock',
      icon: ShieldAlert,
      roles: [ROLES.WAREHOUSE_MANAGER, ROLES.PLANNER, ROLES.ADMIN]
    },
    {
      title: 'Báo cáo năng suất',
      path: '/reports/productivity',
      icon: ClipboardList,
      roles: [ROLES.WAREHOUSE_MANAGER, ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN]
    }
  ];


  if (!sidebarOpen) return null;

  return (
    <aside className="w-64 bg-canvas-night text-onPrimary border-r border-hairline-dark flex flex-col h-full">
      {/* Scrollable menu area */}
      <div className="flex-1 overflow-y-auto py-6 px-4 flex flex-col gap-6">
        <div>
          <div className="px-3 py-1.5 text-[10px] font-bold text-shade-40 uppercase tracking-widest mb-2">
            Hệ thống chính
          </div>
          <nav className="flex flex-col gap-1">
            {menuItems
              .filter(item => item.roles.length === 0 || item.roles.some(role => hasRole(role)))
              .map((item) => (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-pill text-xs font-semibold uppercase tracking-wider transition-colors ${
                      isActive
                        ? 'bg-onPrimary text-canvas-night'
                        : 'text-shade-40 hover:text-onPrimary hover:bg-canvas-nightElevated'
                    }`
                  }
                >
                  <item.icon className="w-4 h-4 flex-shrink-0" />
                  <span>{item.title}</span>
                </NavLink>
              ))}
          </nav>
        </div>

        {inboundItems.filter(item => item.roles.some(role => hasRole(role))).length > 0 && (
          <div>
            <div className="px-3 py-1.5 text-[10px] font-bold text-shade-40 uppercase tracking-widest mb-2">
              Nhập hàng & QC
            </div>
            <nav className="flex flex-col gap-1">
              {inboundItems
                .filter(item => item.roles.some(role => hasRole(role)))
                .map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                      `flex items-center gap-3 px-3 py-2.5 rounded-pill text-xs font-semibold uppercase tracking-wider transition-colors ${
                        isActive
                          ? 'bg-onPrimary text-canvas-night'
                          : 'text-shade-40 hover:text-onPrimary hover:bg-canvas-nightElevated'
                      }`
                    }
                  >
                    <item.icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.title}</span>
                  </NavLink>
                ))}
            </nav>
          </div>
        )}

        {transferItems.filter(item => item.roles.some(role => hasRole(role))).length > 0 && (
          <div>
            <div className="px-3 py-1.5 text-[10px] font-bold text-shade-40 uppercase tracking-widest mb-2">
              Điều chuyển
            </div>
            <nav className="flex flex-col gap-1">
              {transferItems
                .filter(item => item.roles.some(role => hasRole(role)))
                .map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                      `flex items-center gap-3 px-3 py-2.5 rounded-pill text-xs font-semibold uppercase tracking-wider transition-colors ${
                        isActive
                          ? 'bg-onPrimary text-canvas-night'
                          : 'text-shade-40 hover:text-onPrimary hover:bg-canvas-nightElevated'
                      }`
                    }
                  >
                    <item.icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.title}</span>
                  </NavLink>
                ))}
            </nav>
          </div>
        )}

        {masterDataItems.filter(item => item.roles.some(role => hasRole(role))).length > 0 && (
          <div>
            <div className="px-3 py-1.5 text-[10px] font-bold text-shade-40 uppercase tracking-widest mb-2">
              Danh mục nền tảng
            </div>
            <nav className="flex flex-col gap-1">
              {masterDataItems
                .filter(item => item.roles.some(role => hasRole(role)))
                .map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                      `flex items-center gap-3 px-3 py-2.5 rounded-pill text-xs font-semibold uppercase tracking-wider transition-colors ${
                        isActive
                          ? 'bg-onPrimary text-canvas-night'
                          : 'text-shade-40 hover:text-onPrimary hover:bg-canvas-nightElevated'
                      }`
                    }
                  >
                    <item.icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.title}</span>
                  </NavLink>
                ))}
            </nav>
          </div>
        )}

        {outboundItems.filter(item => item.roles.some(role => hasRole(role))).length > 0 && (
          <div>
            <div className="px-3 py-1.5 text-[10px] font-bold text-shade-40 uppercase tracking-widest mb-2">
              Xuất hàng & Giao vận
            </div>
            <nav className="flex flex-col gap-1">
              {outboundItems
                .filter(item => item.roles.some(role => hasRole(role)))
                .map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                      `flex items-center gap-3 px-3 py-2.5 rounded-pill text-xs font-semibold uppercase tracking-wider transition-colors ${
                        isActive
                          ? 'bg-onPrimary text-canvas-night'
                          : 'text-shade-40 hover:text-onPrimary hover:bg-canvas-nightElevated'
                      }`
                    }
                  >
                    <item.icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.title}</span>
                  </NavLink>
                ))}
            </nav>
          </div>
        )}

        {stocktakeItems.filter(item => item.roles.some(role => hasRole(role))).length > 0 && (
          <div>
            <div className="px-3 py-1.5 text-[10px] font-bold text-shade-40 uppercase tracking-widest mb-2">
              Kiểm kê kho
            </div>
            <nav className="flex flex-col gap-1">
              {stocktakeItems
                .filter(item => item.roles.some(role => hasRole(role)))
                .map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                      `flex items-center gap-3 px-3 py-2.5 rounded-pill text-xs font-semibold uppercase tracking-wider transition-colors ${
                        isActive
                          ? 'bg-onPrimary text-canvas-night'
                          : 'text-shade-40 hover:text-onPrimary hover:bg-canvas-nightElevated'
                      }`
                    }
                  >
                    <item.icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.title}</span>
                  </NavLink>
                ))}
            </nav>
          </div>
        )}

        {financeItems.filter(item => item.roles.some(role => hasRole(role))).length > 0 && (
          <div>
            <div className="px-3 py-1.5 text-[10px] font-bold text-shade-40 uppercase tracking-widest mb-2">
              Tài chính & Bảng giá
            </div>
            <nav className="flex flex-col gap-1">
              {financeItems
                .filter(item => item.roles.some(role => hasRole(role)))
                .map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                      `flex items-center gap-3 px-3 py-2.5 rounded-pill text-xs font-semibold uppercase tracking-wider transition-colors ${
                        isActive
                          ? 'bg-onPrimary text-canvas-night'
                          : 'text-shade-40 hover:text-onPrimary hover:bg-canvas-nightElevated'
                      }`
                    }
                  >
                    <item.icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.title}</span>
                  </NavLink>
                ))}
            </nav>
          </div>
        )}

        {reportItems.filter(item => item.roles.some(role => hasRole(role))).length > 0 && (
          <div>
            <div className="px-3 py-1.5 text-[10px] font-bold text-shade-40 uppercase tracking-widest mb-2">
              Báo cáo & Cảnh báo
            </div>
            <nav className="flex flex-col gap-1">
              {reportItems
                .filter(item => item.roles.some(role => hasRole(role)))
                .map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                      `flex items-center gap-3 px-3 py-2.5 rounded-pill text-xs font-semibold uppercase tracking-wider transition-colors ${
                        isActive
                          ? 'bg-onPrimary text-canvas-night'
                          : 'text-shade-40 hover:text-onPrimary hover:bg-canvas-nightElevated'
                      }`
                    }
                  >
                    <item.icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.title}</span>
                  </NavLink>
                ))}
            </nav>
          </div>
        )}

      </div>

      {/* Sidebar Footer */}
      <div className="p-4 border-t border-hairline-dark flex flex-col gap-1.5 bg-canvas-nightElevated">
        <div className="flex items-center gap-2">
          <ShieldAlert className="w-4 h-4 text-aloe-10" />
          <span className="text-[10px] font-bold text-shade-30 uppercase tracking-wider">
            Vai trò hiện tại
          </span>
        </div>
        <div className="flex flex-wrap gap-1">
          {user?.role && (
            <span
              className="text-[9px] font-semibold bg-shade-70 text-onPrimary px-2 py-0.5 rounded-pill border border-shade-60"
            >
              {user.role}
            </span>
          )}
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
