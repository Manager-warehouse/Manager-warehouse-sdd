import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../stores/auth.store';
import { useUiStore } from '../stores/ui.store';
import Header from '../components/layout/Header';
import Sidebar from '../components/layout/Sidebar';
import Footer from '../components/layout/Footer';

const ProtectedRoute = ({ allowedRoles = [] }) => {
  const { token, user } = useAuthStore();
  const { addToast } = useUiStore();

  if (!token || !user) {
    // Redirect to login if not authenticated
    return <Navigate to="/login" replace />;
  }

  // Check if role restriction applies
  if (allowedRoles.length > 0) {
    const hasRole = allowedRoles.includes(user.role);
    if (!hasRole) {
      // Show error toast and redirect to dashboard
      setTimeout(() => {
        addToast('Bạn không có quyền truy cập chức năng này', 'error');
      }, 0);
      return <Navigate to="/dashboard" replace />;
    }
  }

  return (
    <div className="flex flex-col min-h-screen bg-canvas-cream text-ink">
      {/* Top Navbar */}
      <Header />
      
      {/* Main Container */}
      <div className="flex flex-1">
        {/* Sidebar Navigation */}
        <Sidebar />
        
        {/* Page Content Panel */}
        <main className="flex-1 p-6 md:p-8 flex flex-col overflow-y-auto">
          <Outlet />
          
          {/* Footer inside content column */}
          <Footer />
        </main>
      </div>
    </div>
  );
};

export default ProtectedRoute;
