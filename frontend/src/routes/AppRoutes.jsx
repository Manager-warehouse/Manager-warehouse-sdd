import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import Login from '../pages/Auth/Login';
import Dashboard from '../pages/Dashboard';
import Profile from '../pages/Profile/Profile';
import Forbidden from '../pages/Forbidden/Forbidden';
import UserManagement from '../pages/Admin/UserManagement';
import SystemConfig from '../pages/Admin/SystemConfig';
import AuditLogs from '../pages/Admin/AuditLogs';
import { ROLES } from '../utils/constants';

const AppRoutes = () => {
  return (
    <Routes>
      {/* Publicly accessible views that do not require any user authentication session */}
      <Route path="/login" element={<Login />} />
      <Route path="/forbidden" element={<Forbidden />} />

      {/* Authenticated views accessible to any user with a valid JWT token */}
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/profile" element={<Profile />} />
      </Route>

      {/* Restricted administrative console views that require full administrator privileges */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.ADMIN]} />}>
        <Route path="/admin/users" element={<UserManagement />} />
        <Route path="/admin/config" element={<SystemConfig />} />
        <Route path="/admin/audit-logs" element={<AuditLogs />} />
      </Route>

      {/* Default fallbacks redirecting invalid or root paths to the main landing dashboard */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};

export default AppRoutes;
