import React, { useCallback, useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../stores/auth.store';
import { WAREHOUSES } from '../utils/constants';
import Header from '../components/layout/Header';
import Sidebar from '../components/layout/Sidebar';
import Footer from '../components/layout/Footer';


const ProtectedRoute = ({ allowedRoles = [] }) => {
  const location = useLocation();
  const { token, user, activeWarehouse, setActiveWarehouse } = useAuthStore();

  const clampHorizontalScroll = useCallback((event) => {
    const target = event.currentTarget;
    if (target.scrollLeft !== 0) {
      target.scrollLeft = 0;
    }
    document.documentElement.scrollLeft = 0;
    document.body.scrollLeft = 0;
  }, []);

  useEffect(() => {
    document.body.classList.add('layout-locked');
    document.documentElement.scrollLeft = 0;
    document.body.scrollLeft = 0;
    return () => {
      document.body.classList.remove('layout-locked');
    };
  }, []);

  useEffect(() => {
    if (user && user.role === 'WAREHOUSE_MANAGER' && user.warehouses && user.warehouses.length > 0) {
      const isAllowedPage = location.pathname === '/dashboard';
      if (!isAllowedPage) {
        const assignedId = user.warehouses[0];
        if (activeWarehouse && activeWarehouse.id !== assignedId) {
          const assignedWarehouse = WAREHOUSES.find(w => w.id === assignedId);
          if (assignedWarehouse) {
            setActiveWarehouse(assignedWarehouse);
          }
        }
      }
    }
  }, [location.pathname, user, activeWarehouse, setActiveWarehouse]);


// Send unauthenticated users back to login page to acquire a session token
  if (!token || !user) {
    return <Navigate to="/login" replace />;
  }

  // Prevent users without authorized roles from accessing this route to enforce RBAC
  if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
    return <Navigate to="/forbidden" replace />;
  }

  return (
    <div className="flex h-dvh w-full max-w-[100vw] flex-col overflow-hidden bg-canvas-cream text-ink">
      <Header />

      <div className="flex min-w-0 max-w-full flex-1 overflow-hidden">
        <Sidebar />

        <main
          className="app-main min-w-0 max-w-full flex-1 overflow-y-auto"
          onScroll={clampHorizontalScroll}
          onTouchMove={clampHorizontalScroll}
        >
          <div className="app-main-inner flex min-h-full min-w-0 max-w-full flex-col">
            <div className="app-content flex min-w-0 max-w-full flex-1 flex-col px-3 py-4 sm:p-6">
              <Outlet />
            </div>

            <Footer />
          </div>
        </main>
      </div>
    </div>
  );
};

export default ProtectedRoute;
