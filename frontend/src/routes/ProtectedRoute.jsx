import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../stores/auth.store';
import Header from '../components/layout/Header';
import Sidebar from '../components/layout/Sidebar';
import Footer from '../components/layout/Footer';

const ProtectedRoute = ({ allowedRoles = [] }) => {
  const { token, user } = useAuthStore();

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
        
        <main className="flex-1 p-6 md:p-8 flex flex-col overflow-y-auto">
          <Outlet />
          
          <Footer />
        </main>
      </div>
    </div>
  );
};

export default ProtectedRoute;
