import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Users, UserSquare, ShieldAlert, BarChart3, Package2, Settings, History } from 'lucide-react';
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

  // Dummy menus to show full WMS modules structure (as disabled or mocked)
  const mockupModules = [
    { title: 'Nhập kho (Inbound)', icon: Package2 },
    { title: 'Xuất kho (Outbound)', icon: Package2 },
    { title: 'Điều chuyển (Transfer)', icon: Package2 },
    { title: 'Kiểm kê (Stocktake)', icon: Package2 },
    { title: 'Báo cáo & Cảnh báo', icon: BarChart3 }
  ];

  if (!sidebarOpen) return null;

  return (
    <aside className="w-64 bg-canvas-night text-onPrimary border-r border-hairline-dark flex flex-col h-[calc(100vh-4rem)]">
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

        <div>
          <div className="px-3 py-1.5 text-[10px] font-bold text-shade-40 uppercase tracking-widest mb-2">
            Nghiệp vụ WMS
          </div>
          <div className="flex flex-col gap-1">
            {mockupModules.map((item, idx) => (
              <div
                key={idx}
                className="flex items-center justify-between px-3 py-2.5 rounded-pill text-xs font-semibold uppercase tracking-wider text-shade-50 opacity-60 cursor-not-allowed hover:bg-canvas-nightElevated"
                title="Sẽ khả dụng trong giai đoạn sau của dự án"
              >
                <div className="flex items-center gap-3">
                  <item.icon className="w-4 h-4" />
                  <span>{item.title}</span>
                </div>
                <span className="text-[9px] bg-shade-60 text-canvas-night px-1.5 py-0.5 rounded font-bold">
                  Next
                </span>
              </div>
            ))}
          </div>
        </div>
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
