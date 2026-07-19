import React, { useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../stores/auth.store';
import { WAREHOUSES } from '../utils/constants';
import Header from '../components/layout/Header';
import Sidebar from '../components/layout/Sidebar';
import Footer from '../components/layout/Footer';


const ProtectedRoute = ({ allowedRoles = [] }) => {
  const location = useLocation();
  const { token, user, activeWarehouse, setActiveWarehouse } = useAuthStore();

  useEffect(() => {
    document.body.classList.add('layout-locked');
    return () => {
      document.body.classList.remove('layout-locked');
    };
  }, []);

  useEffect(() => {
    if (user && user.role === 'WAREHOUSE_MANAGER' && user.warehouses && user.warehouses.length > 0) {
      const isAllowedPage = location.pathname === '/dashboard' || location.pathname === '/inventory-availability';
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
  if (allowedRoles.length > 0 && user.role !== 'ADMIN' && !allowedRoles.includes(user.role)) {
    return <Navigate to="/forbidden" replace />;
  }

  return (
    <div className="flex flex-col h-dvh w-full max-w-full bg-canvas-cream text-ink overflow-hidden">
      <Header />

      <div className="flex flex-1 min-w-0 overflow-hidden">
        <Sidebar />

        <main className="flex-1 min-w-0 overflow-x-hidden overflow-y-auto">
          <div className="min-h-full min-w-0 flex flex-col">
            <div className="flex-1 min-w-0 flex flex-col p-4 sm:p-6">
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
