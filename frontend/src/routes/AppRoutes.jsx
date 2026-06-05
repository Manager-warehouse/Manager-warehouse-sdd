import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import Login from '../pages/Auth/Login';
import Dashboard from '../pages/Dashboard';
import Profile from '../pages/Profile/Profile';
import UserManagement from '../pages/Admin/UserManagement';
import ProductManagement from '../pages/Admin/ProductManagement';
import WarehouseManagement from '../pages/Admin/WarehouseManagement';
import PartnerManagement from '../pages/Admin/PartnerManagement';
import FleetManagement from '../pages/Admin/FleetManagement';
import ReceiptList from '../pages/Inbound/ReceiptList';
import ReceiptForm from '../pages/Inbound/ReceiptForm';
import ReceiptReceive from '../pages/Inbound/ReceiptReceive';
import QCInbound from '../pages/Inbound/QCInbound';
import PutawayPlan from '../pages/Inbound/PutawayPlan';
import QuarantineWorkspace from '../pages/Inbound/QuarantineWorkspace';
import { ROLES } from '../utils/constants';

const AppRoutes = () => {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<Login />} />

      {/* Protected routes */}
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/profile" element={<Profile />} />
      </Route>

      {/* Admin specific protected routes */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.ADMIN]} />}>
        <Route path="/admin/users" element={<UserManagement />} />
      </Route>

      {/* Master Data Management protected routes */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.PLANNER, ROLES.ADMIN, ROLES.CEO]} />}>
        <Route path="/admin/products" element={<ProductManagement />} />
      </Route>
      
      <Route element={<ProtectedRoute allowedRoles={[ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]} />}>
        <Route path="/admin/warehouses" element={<WarehouseManagement />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN, ROLES.CEO]} />}>
        <Route path="/admin/partners" element={<PartnerManagement />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.DISPATCHER, ROLES.ADMIN, ROLES.CEO]} />}>
        <Route path="/admin/fleet" element={<FleetManagement />} />
      </Route>

      {/* Inbound & QC protected routes */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.PLANNER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]} />}>
        <Route path="/inbound/receipts" element={<ReceiptList />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.PLANNER, ROLES.ADMIN]} />}>
        <Route path="/inbound/create" element={<ReceiptForm />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.STOREKEEPER, ROLES.ADMIN]} />}>
        <Route path="/inbound/receive/:id" element={<ReceiptReceive />} />
        <Route path="/inbound/putaway/:id" element={<PutawayPlan />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.WAREHOUSE_STAFF, ROLES.ADMIN]} />}>
        <Route path="/inbound/qc/:id" element={<QCInbound />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.WAREHOUSE_MANAGER, ROLES.CEO, ROLES.ADMIN]} />}>
        <Route path="/inbound/quarantine" element={<QuarantineWorkspace />} />
      </Route>

      {/* Default Redirects */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};

export default AppRoutes;
