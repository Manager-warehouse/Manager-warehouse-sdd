import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import Login from '../pages/Auth/Login';
import ForgotPassword from '../pages/Auth/ForgotPassword';
import Dashboard from '../pages/Dashboard';
import Profile from '../pages/Profile/Profile';
import Forbidden from '../pages/Forbidden/Forbidden';
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
import SystemConfig from '../pages/Admin/SystemConfig';
import AuditLogs from '../pages/Admin/AuditLogs';
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
      {/* Publicly accessible views that do not require any user authentication session */}
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
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
