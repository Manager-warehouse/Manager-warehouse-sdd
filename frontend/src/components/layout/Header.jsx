import React, { useState, useMemo } from 'react';
import { Menu, LogOut, User, Warehouse, ChevronDown } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { WAREHOUSES } from '../../utils/constants';
import { getAvatarFallback } from '../../utils/format';
import { useNavigate, useLocation } from 'react-router-dom';

import BillingNotificationMenu from './BillingNotificationMenu';

const Header = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, activeWarehouse, setActiveWarehouse, logout } = useAuthStore();
  const { toggleSidebar, addToast } = useUiStore();
  
  const [warehouseDropdownOpen, setWarehouseDropdownOpen] = useState(false);
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);

  // Determine allowed warehouses for this user
  const allowedWarehouses = useMemo(() => {
    if (!user) return [];
    if (user.role === 'ADMIN' || user.role === 'CEO') {
      return WAREHOUSES;
    }
    if (user.role === 'WAREHOUSE_MANAGER') {
      const isAllowedPage = location.pathname === '/dashboard';
      if (isAllowedPage) {
        return WAREHOUSES;
      }
    }
    return WAREHOUSES.filter(
      (w) => user.warehouses && user.warehouses.includes(w.id)
    );
  }, [user, location.pathname]);

  const isSelectorInteractive = allowedWarehouses.length > 1;


  const handleWarehouseChange = (wh) => {
    setActiveWarehouse(wh);
    setWarehouseDropdownOpen(false);
    addToast(`Đã chuyển làm việc sang ${wh.name}`, 'info');
  };

  const handleLogout = () => {
    logout();
    addToast('Đăng xuất thành công', 'success');
    navigate('/login');
  };

  return (
    <header className="app-safe-header sticky top-0 z-50 flex w-full max-w-[100vw] items-end justify-between overflow-visible border-b border-hairline-light bg-canvas-light px-3 pb-3 sm:px-6">
      {/* Left side: Hamburger menu and Brand */}
      <div className="flex min-w-0 flex-1 items-center gap-2 sm:gap-4">
        <button
          onClick={toggleSidebar}
          className="p-1.5 rounded-full hover:bg-canvas-cream text-ink focus:outline-none"
        >
          <Menu className="w-5 h-5" />
        </button>
        <span className="font-display font-semibold text-lg tracking-tight text-ink hidden md:inline-block">
          Phúc Anh <span className="font-light text-shade-50">WMS</span>
        </span>
      </div>

      {/* Right side: Actions */}
      <div className="flex min-w-0 max-w-[58vw] shrink-0 items-center justify-end gap-1 sm:max-w-none sm:gap-4">
        
        {/* Billing Notifications for Accountant */}
        {(user?.role === 'ACCOUNTANT' || user?.role === 'ADMIN') && (
          <BillingNotificationMenu />
        )}

        {/* Warehouse Selector Dropdown */}
        {activeWarehouse && (
          <div className="relative min-w-0">
            <button
              onClick={() => {
                if (isSelectorInteractive) {
                  setWarehouseDropdownOpen(!warehouseDropdownOpen);
                  setProfileDropdownOpen(false);
                }
              }}
              disabled={!isSelectorInteractive}
              className={`flex max-w-[7.5rem] items-center gap-1.5 sm:gap-2 px-2.5 sm:px-3 py-1.5 rounded-pill border border-hairline-light bg-canvas-cream text-xs font-semibold text-ink uppercase tracking-wider transition-colors focus:outline-none ${
                isSelectorInteractive ? 'hover:bg-shade-30 cursor-pointer' : 'cursor-default opacity-85'
              }`}
            >
              <Warehouse className="hidden h-3.5 w-3.5 shrink-0 sm:block" />
              <span className="truncate">{activeWarehouse.code}</span>
              {isSelectorInteractive && <ChevronDown className="w-3 h-3 text-shade-60" />}
            </button>


            {warehouseDropdownOpen && (
              <div className="absolute right-0 mt-2 w-56 bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 py-1.5 z-50">
                <div className="px-3 py-1.5 text-[10px] font-bold text-shade-50 uppercase tracking-widest border-b border-hairline-light mb-1">
                  Chọn Kho làm việc
                </div>
                {allowedWarehouses.map((wh) => (
                  <button
                    key={wh.id}
                    onClick={() => handleWarehouseChange(wh)}
                    className={`w-full text-left px-3 py-2 text-xs flex items-center justify-between hover:bg-canvas-cream transition-colors ${
                      activeWarehouse.id === wh.id ? 'font-semibold text-ink bg-canvas-cream' : 'text-shade-60'
                    }`}
                  >
                    <span>{wh.name}</span>
                    <span className="text-[10px] text-shade-40 font-mono">{wh.code}</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {/* User Profile Dropdown */}
        {user && (
          <div className="relative">
            <button
              onClick={() => {
                setProfileDropdownOpen(!profileDropdownOpen);
                setWarehouseDropdownOpen(false);
              }}
              className="flex h-10 shrink-0 items-center gap-0 rounded-pill px-0.5 py-1 hover:bg-canvas-cream focus:outline-none focus:ring-1 focus:ring-ink sm:gap-2 sm:px-1"
            >
              <div className="w-8 h-8 shrink-0 rounded-full bg-ink text-onPrimary font-semibold text-xs flex items-center justify-center">
                {getAvatarFallback(user.fullName)}
              </div>
              <ChevronDown className="hidden h-3.5 w-3.5 text-shade-60 sm:block" />
            </button>

            {profileDropdownOpen && (
              <div className="fixed right-3 top-[calc(4.5rem+var(--app-safe-top))] z-[80] w-52 rounded-lg border border-hairline-light bg-canvas-light py-1 shadow-level-3 sm:absolute sm:right-0 sm:top-auto sm:mt-2 sm:w-48">
                <div className="px-4 py-2 border-b border-hairline-light">
                  <div className="text-xs font-semibold text-ink truncate">{user.fullName}</div>
                  <div className="text-[10px] text-shade-50 truncate mt-0.5">{user.email || ''}</div>
                </div>
                
                <button
                  onClick={() => {
                    setProfileDropdownOpen(false);
                    navigate('/profile');
                  }}
                  className="w-full text-left px-4 py-2 text-xs text-shade-70 hover:bg-canvas-cream flex items-center gap-2"
                >
                  <User className="w-3.5 h-3.5" />
                  <span>Trang cá nhân</span>
                </button>

                <button
                  onClick={handleLogout}
                  className="w-full text-left px-4 py-2 text-xs text-danger-600 hover:bg-danger-50 flex items-center gap-2 border-t border-hairline-light"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  <span>Đăng xuất</span>
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </header>
  );
};

export default Header;
