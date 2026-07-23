import React, { useEffect } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { Users, SquareUser, ShieldAlert, BarChart3, Package2, Settings, History, Box, Warehouse, Handshake, Truck, MapPin, PackageCheck, ClipboardList, DollarSign, CheckSquare, ArrowRightLeft, FileText, Landmark, Calendar, PieChart } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { useMediaQuery } from '../../hooks/useMediaQuery';
import { ROLES, ROLE_LABELS } from '../../utils/constants';

const Sidebar = () => {
  const { user, hasRole } = useAuthStore();
  const sidebarOpen = useUiStore((state) => state.sidebarOpen);
  const setSidebarOpen = useUiStore((state) => state.setSidebarOpen);
  const isMobile = useMediaQuery('(max-width: 767px)');
  const location = useLocation();

  // Close the drawer after navigating on mobile so the destination page is visible.
  useEffect(() => {
    if (isMobile) setSidebarOpen(false);
  }, [location.pathname]);

  const menuItems = [
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
      icon: SquareUser,
      roles: []
    }
  ];

  const masterDataItems = [
    {
      title: 'Mã & Sản phẩm',
      path: '/admin/products',
      icon: Box,
      roles: [ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.PLANNER, ROLES.CEO]
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
      roles: [ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.WAREHOUSE_MANAGER, ROLES.CEO]
    },
    {
      title: 'Đội xe & Tài xế',
      path: '/admin/fleet',
      icon: Truck,
      roles: [ROLES.DISPATCHER, ROLES.WAREHOUSE_MANAGER, ROLES.CEO]
    }
  ];

  const inboundItems = [
    {
      title: 'Phiếu Nhập & Kiểm định',
      path: '/inbound/receipts',
      icon: Package2,
      roles: [ROLES.PLANNER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.WAREHOUSE_MANAGER, ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.CEO]
    },
    {
      title: 'Xử lý hàng lỗi',
      path: '/inbound/quarantine',
      icon: ShieldAlert,
      roles: [ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.CEO]
    },
    {
      title: 'Đại lý trả hàng',
      path: '/inbound/returns',
      icon: ArrowRightLeft,
      roles: [ROLES.WAREHOUSE_STAFF, ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.CEO]
    }
  ];

  const transferItems = [
    {
      title: 'Yêu cầu điều chuyển',
      path: '/transfers/requests',
      icon: ClipboardList,
      roles: [ROLES.PLANNER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.WAREHOUSE_MANAGER, ROLES.DISPATCHER, ROLES.CEO]
    },
    {
      title: 'Phiếu điều chuyển',
      path: '/transfers',
      icon: Package2,
      end: true,
      roles: [ROLES.PLANNER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.WAREHOUSE_MANAGER, ROLES.DISPATCHER, ROLES.CEO]
    }
  ];

  const outboundItems = [
    {
      title: 'Đơn xuất hàng',
      path: '/outbound/delivery-orders',
      icon: PackageCheck,
      roles: [ROLES.PLANNER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.WAREHOUSE_MANAGER, ROLES.DISPATCHER, ROLES.ACCOUNTANT, ROLES.CEO]
    },
    {
      title: 'Quản lý chuyến xe',
      path: '/outbound/trips',
      icon: Truck,
      roles: [ROLES.DISPATCHER, ROLES.WAREHOUSE_MANAGER, ROLES.CEO]
    },
    {
      title: 'Giao hàng của tôi',
      path: '/outbound/driver/trips',
      icon: MapPin,
      roles: [ROLES.DRIVER]
    }
  ];

  const stocktakeItems = [
    {
      title: 'Danh sách kiểm kê',
      path: '/stocktake',
      icon: ClipboardList,
      roles: [ROLES.WAREHOUSE_MANAGER, ROLES.STOREKEEPER, ROLES.CEO],
    },
  ];

  const financeItems = [
    {
      title: 'Bảng giá',
      path: '/finance/price-list',
      icon: DollarSign,
      roles: [ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.CEO]
    },
    {
      title: 'Duyệt bảng giá',
      path: '/finance/price-approval',
      icon: CheckSquare,
      roles: [ROLES.ACCOUNTANT_MANAGER]
    },
    {
      title: 'Thu Tiền Đại lý (AR)',
      path: '/finance/invoices',
      icon: FileText,
      roles: [ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.CEO]
    },
    {
      title: 'Trả Tiền NCC (AP)',
      path: '/finance/supplier-invoices',
      icon: Landmark,
      roles: [ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.CEO]
    },
    {
      title: 'Kỳ kế toán & Khóa sổ',
      path: '/finance/periods',
      icon: Calendar,
      roles: [ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.CEO]
    }
  ];

  const reportItems = [
    {
      title: 'Báo cáo quản trị (Giám đốc)',
      path: '/reports/ceo-dashboard',
      icon: BarChart3,
      roles: [ROLES.CEO]
    },
    {
      title: 'Báo cáo giá trị tồn',
      path: '/reports/inventory-valuation',
      icon: DollarSign,
      roles: [ROLES.ACCOUNTANT_MANAGER, ROLES.WAREHOUSE_MANAGER, ROLES.CEO]
    },
    {
      title: 'Cảnh báo tồn kho',
      path: '/reports/low-stock',
      icon: ShieldAlert,
      roles: [ROLES.WAREHOUSE_MANAGER, ROLES.PLANNER, ROLES.CEO]
    }
  ];


  if (!sidebarOpen) return null;

  return (
    <>
      {/* Mobile-only backdrop: closes the drawer on tap without covering the header */}
      <div
        className="app-safe-drawer-backdrop fixed inset-x-0 bottom-0 z-30 bg-black/50 md:hidden"
        onClick={() => setSidebarOpen(false)}
      />
      <aside
        className="app-safe-drawer fixed bottom-0 left-0 z-30 w-72 max-w-[85vw]
          md:static md:top-auto md:bottom-auto md:z-auto md:w-64 md:max-w-none
          bg-canvas-night text-onPrimary border-r border-hairline-dark flex flex-col md:h-full"
      >
      {/* Scrollable menu area */}
      <div className="flex-1 overflow-y-auto py-6 px-4 flex flex-col gap-6">
        <div>
          <div className="px-3 py-1.5 text-[10px] font-bold text-shade-40 uppercase tracking-widest mb-2">
            Hệ thống chính
          </div>
          <nav className="flex flex-col gap-1">
            {menuItems
              .filter(item => !(item.hiddenForRoles || []).some(role => hasRole(role)))
              .filter(item => item.roles.length === 0 || item.roles.some(role => hasRole(role)))
              .map((item) => (
                <NavLink
                  key={item.path}
                  to={item.path}
                  end
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
              Nhập hàng & Kiểm định
            </div>
            <nav className="flex flex-col gap-1">
              {inboundItems
                .filter(item => item.roles.some(role => hasRole(role)))
                .map((item) => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    end
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
                    end={!!item.end}
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
                    end
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
                    end
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
                    end
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
                    end
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
                    end
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
              {ROLE_LABELS[user.role] || user.role}
            </span>
          )}
        </div>
      </div>
      </aside>
    </>
  );
};

export default Sidebar;
