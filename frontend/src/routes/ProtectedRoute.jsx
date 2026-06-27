import React, { useEffect } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../stores/auth.store';
import Header from '../components/layout/Header';
import Sidebar from '../components/layout/Sidebar';
import Footer from '../components/layout/Footer';

const ProtectedRoute = ({ allowedRoles = [] }) => {
  const { token, user } = useAuthStore();

  useEffect(() => {
    document.body.classList.add('layout-locked');
    return () => {
      document.body.classList.remove('layout-locked');
    };
  }, []);

// Send unauthenticated users back to login page to acquire a session token
  if (!token || !user) {
    return <Navigate to="/login" replace />;
  }

  // Prevent users without authorized roles from accessing this route to enforce RBAC
  if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
    return <Navigate to="/forbidden" replace />;
  }

  return (
    <div className="flex flex-col h-screen bg-canvas-cream text-ink overflow-hidden">
      <Header />
      
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        
        <main className="flex-1 overflow-y-auto">
          <div className="min-h-full flex flex-col">
            <div className="flex-1 flex flex-col p-6">
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
