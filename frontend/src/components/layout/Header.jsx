import React, { useState } from 'react';
import { Menu, LogOut, User, Warehouse, ChevronDown } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { WAREHOUSES } from '../../utils/constants';
import { getAvatarFallback } from '../../utils/format';
import { useNavigate } from 'react-router-dom';
import BillingNotificationMenu from './BillingNotificationMenu';

const Header = () => {
  const navigate = useNavigate();
  const { user, activeWarehouse, setActiveWarehouse, logout } = useAuthStore();
  const { toggleSidebar, addToast } = useUiStore();
  
  const [warehouseDropdownOpen, setWarehouseDropdownOpen] = useState(false);
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);

  // Determine allowed warehouses for this user
  const allowedWarehouses = WAREHOUSES.filter(
    (w) => !user?.warehouses || user.warehouses.length === 0 || user.warehouses.includes(w.id)
  );

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
    <header className="app-safe-header sticky top-0 z-40 bg-canvas-light border-b border-hairline-light px-6 flex items-center justify-between">
      {/* Left side: Hamburger menu and Brand */}
      <div className="flex items-center gap-4">
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
      <div className="flex items-center gap-4">
        
        {/* Billing Notifications for Accountant */}
        {(user?.role === 'ACCOUNTANT' || user?.role === 'ADMIN') && (
          <BillingNotificationMenu />
        )}

        {/* Warehouse Selector Dropdown */}
        {activeWarehouse && (
          <div className="relative">
            <button
              onClick={() => {
                setWarehouseDropdownOpen(!warehouseDropdownOpen);
                setProfileDropdownOpen(false);
              }}
              className="flex items-center gap-2 px-3 py-1.5 rounded-pill border border-hairline-light bg-canvas-cream hover:bg-shade-30 text-xs font-semibold text-ink uppercase tracking-wider transition-colors focus:outline-none"
            >
              <Warehouse className="w-3.5 h-3.5" />
              <span>{activeWarehouse.code}</span>
              <ChevronDown className="w-3 h-3 text-shade-60" />
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
              className="flex items-center gap-2 p-1 rounded-pill hover:bg-canvas-cream focus:outline-none focus:ring-1 focus:ring-ink"
            >
              <div className="w-8 h-8 rounded-full bg-ink text-onPrimary font-semibold text-xs flex items-center justify-center">
                {getAvatarFallback(user.fullName)}
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-shade-60 mr-1" />
            </button>

            {profileDropdownOpen && (
              <div className="absolute right-0 mt-2 w-48 bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 py-1 z-50">
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
