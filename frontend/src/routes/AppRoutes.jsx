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
import ReturnsWorkspace from '../pages/Inbound/ReturnsWorkspace';
import InterWarehouseTransferWorkspace from '../pages/InterWarehouseTransfer/InterWarehouseTransferWorkspace';
import TransferRequestWorkspace from '../pages/InterWarehouseTransfer/TransferRequestWorkspace';
import SystemConfig from '../pages/Admin/SystemConfig';
import AuditLogs from '../pages/Admin/AuditLogs';
import DeliveryOrders from '../pages/Outbound/DeliveryOrders';
import DeliveryOrderDetail from '../pages/Outbound/DeliveryOrderDetail';
import QCOutbound from '../pages/Outbound/QCOutbound';
import TripPlanning from '../pages/Outbound/TripPlanning';
import DriverTrip from '../pages/Outbound/DriverTrip';
import StocktakeList from '../pages/Stocktake/StocktakeList';
import StocktakeForm from '../pages/Stocktake/StocktakeForm';
import StocktakeDetail from '../pages/Stocktake/StocktakeDetail';
import PriceListManagement from '../pages/Finance/PriceListManagement';
import PriceApproval from '../pages/Finance/PriceApproval';
import CeoDashboard from '../pages/Reports/CeoDashboard';
import InventoryValuation from '../pages/Reports/InventoryValuation';
import LowStockAlerts from '../pages/Reports/LowStockAlerts';
import ProductivityReport from '../pages/Reports/ProductivityReport';
import DealerDebtInvoice from '../pages/Finance/DealerDebtInvoice';
import Payments from '../pages/Finance/Payments';
import { ROLES, getDefaultRouteByRole } from '../utils/constants';
import { useAuthStore } from '../stores/auth.store';

const DefaultRedirect = () => {
  const { user } = useAuthStore();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={getDefaultRouteByRole(user.role)} replace />;
};

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
        <Route path="/admin/config" element={<SystemConfig />} />
        <Route path="/admin/audit-logs" element={<AuditLogs />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.ADMIN]} />}>
        <Route path="/admin/users" element={<UserManagement />} />
      </Route>

      {/* Master Data Management protected routes */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.PLANNER, ROLES.ADMIN, ROLES.CEO]} />}>
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

      <Route element={<ProtectedRoute allowedRoles={[ROLES.WAREHOUSE_STAFF, ROLES.ADMIN]} />}>
        <Route path="/inbound/receive/:id" element={<ReceiptReceive />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.STOREKEEPER, ROLES.ADMIN]} />}>
        <Route path="/inbound/putaway/:id" element={<PutawayPlan />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.WAREHOUSE_STAFF, ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN]} />}>
        <Route path="/inbound/qc/:id" element={<QCInbound />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.CEO, ROLES.ADMIN]} />}>
        <Route path="/inbound/quarantine" element={<QuarantineWorkspace />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.WAREHOUSE_STAFF, ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.CEO, ROLES.ADMIN]} />}>
        <Route path="/inbound/returns" element={<ReturnsWorkspace />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.PLANNER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.WAREHOUSE_MANAGER, ROLES.DISPATCHER, ROLES.ADMIN, ROLES.CEO]} />}>
        <Route path="/transfers" element={<InterWarehouseTransferWorkspace />} />
        <Route path="/transfers/:id" element={<InterWarehouseTransferWorkspace />} />
        <Route path="/transfers/requests" element={<TransferRequestWorkspace />} />
      </Route>

      {/* Outbound & Delivery protected routes */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.PLANNER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.WAREHOUSE_MANAGER, ROLES.DISPATCHER, ROLES.ACCOUNTANT, ROLES.ADMIN, ROLES.CEO]} />}>
        <Route path="/outbound/delivery-orders" element={<DeliveryOrders />} />
        <Route path="/outbound/delivery-orders/:id" element={<DeliveryOrderDetail />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.ADMIN]} />}>
        <Route path="/outbound/qc/:id" element={<QCOutbound />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.DISPATCHER, ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]} />}>
        <Route path="/outbound/trips" element={<TripPlanning />} />
        <Route path="/outbound/trips/:id" element={<TripPlanning />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.DRIVER, ROLES.ADMIN]} />}>
        <Route path="/outbound/driver/trips" element={<DriverTrip />} />
        <Route path="/outbound/driver/trips/:id" element={<DriverTrip />} />
      </Route>

      {/* Stocktake protected routes */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.WAREHOUSE_MANAGER, ROLES.STOREKEEPER, ROLES.CEO, ROLES.ADMIN]} />}>
        <Route path="/stocktake" element={<StocktakeList />} />
        <Route path="/stocktake/new" element={<StocktakeForm />} />
        <Route path="/stocktake/:id" element={<StocktakeDetail />} />
      </Route>

      {/* Finance — Pricing & COGS */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN, ROLES.CEO]} />}>
        <Route path="/finance/price-list" element={<PriceListManagement />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN]} />}>
        <Route path="/finance/price-approval" element={<PriceApproval />} />
      </Route>

      {/* Reports & Alerts (Module 010) */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN]} />}>
        <Route path="/reports/ceo-dashboard" element={<CeoDashboard />} />
      </Route>
      <Route element={<ProtectedRoute allowedRoles={[ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN]} />}>
        <Route path="/reports/inventory-valuation" element={<InventoryValuation />} />
      </Route>
      <Route element={<ProtectedRoute allowedRoles={[ROLES.WAREHOUSE_MANAGER, ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN]} />}>
        <Route path="/reports/productivity" element={<ProductivityReport />} />
      </Route>
      <Route element={<ProtectedRoute allowedRoles={[ROLES.WAREHOUSE_MANAGER, ROLES.PLANNER, ROLES.ADMIN]} />}>
        <Route path="/reports/low-stock" element={<LowStockAlerts />} />
      </Route>

      {/* Finance & Credit protected routes */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER, ROLES.ADMIN, ROLES.CEO]} />}>
        <Route path="/finance/invoices" element={<DealerDebtInvoice />} />
        <Route path="/finance/payments" element={<Payments />} />
      </Route>

      {/* Default Redirects */}
      <Route path="/" element={<DefaultRedirect />} />
      <Route path="*" element={<DefaultRedirect />} />
    </Routes>
  );
};

export default AppRoutes;
