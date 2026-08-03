/**
 * Định tuyến ứng dụng (Spec 001 + các Spec khác).
 * Public: /login, /forgot-password, /forbidden
 * Protected: route cần đăng nhập, phân quyền theo role qua ProtectedRoute.
 * Mỗi nhóm route gắn allowedRoles tương ứng với vai trò trong hệ thống.
 */
import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import Login from '../pages/Auth/Login';
import ForgotPassword from '../pages/Auth/ForgotPassword';
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
import PutawayPlan from '../pages/Inbound/PutawayPlan';
import QuarantineWorkspace from '../pages/Inbound/QuarantineWorkspace';
import ReturnsWorkspace from '../pages/Inbound/ReturnsWorkspace';
import InterWarehouseTransferWorkspace from '../pages/InterWarehouseTransfer/InterWarehouseTransferWorkspace';
import TransferRequestWorkspace from '../pages/InterWarehouseTransfer/TransferRequestWorkspace';
import TransferDiscrepancyWorkspace from '../pages/InterWarehouseTransfer/TransferDiscrepancyWorkspace';
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
import DealerDebtInvoice from '../pages/Finance/DealerDebtInvoice';
import Payments from '../pages/Finance/Payments';
import SupplierInvoices from '../pages/Finance/SupplierInvoices';
import PeriodClosing from '../pages/Finance/PeriodClosing';
import PeriodDetail from '../pages/Finance/PeriodDetail';
import CreditAgingReport from '../pages/Reports/CreditAgingReport';
import { ROLES, getDefaultRouteByRole } from '../utils/constants';
import { useAuthStore } from '../stores/auth.store';

// Chuyển hướng mặc định: chưa đăng nhập → login, đã đăng nhập → trang theo role
const DefaultRedirect = () => {
  const { user } = useAuthStore();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={getDefaultRouteByRole(user.role)} replace />;
};

const AppRoutes = () => {
  return (
    <Routes>
      {/* Route công khai — không cần đăng nhập */}
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/forbidden" element={<Forbidden />} />

      {/* Route cần đăng nhập — mọi user có JWT hợp lệ */}
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<Navigate to="/admin/users" replace />} />
        <Route path="/profile" element={<Profile />} />
      </Route>

      {/* Route quản trị — chỉ ADMIN (Spec 001: cấu hình hệ thống, audit log) */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.ADMIN]} />}>
        <Route path="/admin/config" element={<SystemConfig />} />
        <Route path="/admin/audit-logs" element={<AuditLogs />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.ADMIN]} />}>
        <Route path="/admin/users" element={<UserManagement />} />
      </Route>

      {/* Master Data Management protected routes */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER, ROLES.PLANNER, ROLES.CEO]} />}>
        <Route path="/admin/products" element={<ProductManagement />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.ADMIN, ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.STOREKEEPER]} />}>
        <Route path="/admin/warehouses" element={<WarehouseManagement />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.ACCOUNTANT_MANAGER, ROLES.ACCOUNTANT]} />}>
        <Route path="/admin/partners" element={<PartnerManagement />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.DISPATCHER]} />}>
        <Route path="/admin/fleet" element={<FleetManagement />} />
      </Route>

      {/* Inbound & QC protected routes */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.ACCOUNTANT_MANAGER, ROLES.PLANNER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.ACCOUNTANT]} />}>
        <Route path="/inbound/receipts" element={<ReceiptList />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.PLANNER]} />}>
        <Route path="/inbound/create" element={<ReceiptForm />} />
        <Route path="/inbound/receipts/:id/revision" element={<ReceiptForm />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF]} />}>
        <Route path="/inbound/receive/:id" element={<ReceiptReceive />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.STOREKEEPER]} />}>
        <Route path="/inbound/putaway/:id" element={<PutawayPlan />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF]} />}>
        <Route path="/inbound/qc/:id" element={<ReceiptReceive />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.STOREKEEPER]} />}>
        <Route path="/inbound/quarantine" element={<QuarantineWorkspace />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.ACCOUNTANT_MANAGER, ROLES.STOREKEEPER, ROLES.ACCOUNTANT]} />}>
        <Route path="/inbound/returns" element={<ReturnsWorkspace />} />
      </Route>

      {/* Điều chuyển nội bộ - TRF: route vận hành phiếu thật từ lập phiếu, lập chuyến, xuất kho tới nhận kho. */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.PLANNER, ROLES.DISPATCHER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF]} />}>
        <Route path="/transfers" element={<InterWarehouseTransferWorkspace />} />
        <Route path="/transfers/:id" element={<InterWarehouseTransferWorkspace />} />
      </Route>

      {/* Điều chuyển nội bộ - TRQ: route yêu cầu đề xuất, trưởng kho nguồn duyệt xong Planner mới convert sang TRF. */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.PLANNER]} />}>
        <Route path="/transfers/requests" element={<TransferRequestWorkspace />} />
      </Route>

      {/* Điều chuyển nội bộ - discrepancy: route theo dõi thiếu/thừa sau nhận hàng để chốt trách nhiệm. */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO]} />}>
        <Route path="/transfers/discrepancies" element={<TransferDiscrepancyWorkspace />} />
      </Route>

      {/* Outbound & Delivery protected routes */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.PLANNER, ROLES.DISPATCHER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.ACCOUNTANT, ROLES.ACCOUNTANT_MANAGER]} />}>
        <Route path="/outbound/delivery-orders" element={<DeliveryOrders />} />
        <Route path="/outbound/delivery-orders/:id" element={<DeliveryOrderDetail />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.WAREHOUSE_MANAGER, ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF]} />}>
        <Route path="/outbound/qc/:id" element={<QCOutbound />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.DISPATCHER]} />}>
        <Route path="/outbound/trips" element={<TripPlanning />} />
        <Route path="/outbound/trips/:id" element={<TripPlanning />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.DRIVER]} />}>
        <Route path="/outbound/driver/trips" element={<DriverTrip />} />
        <Route path="/outbound/driver/trips/:id" element={<DriverTrip />} />
      </Route>

      {/* Stocktake protected routes */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.STOREKEEPER]} />}>
        <Route path="/stocktake" element={<StocktakeList />} />
        <Route path="/stocktake/new" element={<StocktakeForm />} />
        <Route path="/stocktake/:id" element={<StocktakeDetail />} />
      </Route>

      {/* Finance — Pricing & COGS */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.ACCOUNTANT_MANAGER, ROLES.ACCOUNTANT]} />}>
        <Route path="/finance/price-list" element={<PriceListManagement />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[ROLES.ACCOUNTANT_MANAGER]} />}>
        <Route path="/finance/price-approval" element={<PriceApproval />} />
      </Route>

      {/* Reports & Alerts (Module 010) */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO]} />}>
        <Route path="/reports/ceo-dashboard" element={<CeoDashboard />} />
      </Route>
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.ACCOUNTANT_MANAGER]} />}>
        <Route path="/reports/inventory-valuation" element={<InventoryValuation />} />
      </Route>
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.WAREHOUSE_MANAGER, ROLES.PLANNER]} />}>
        <Route path="/reports/low-stock" element={<LowStockAlerts />} />
      </Route>

      {/* Finance & Credit protected routes (Module 008) */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.CEO, ROLES.ACCOUNTANT_MANAGER, ROLES.ACCOUNTANT]} />}>
        <Route path="/finance/invoices" element={<DealerDebtInvoice />} />
        <Route path="/finance/payments" element={<DealerDebtInvoice />} />
        <Route path="/finance/supplier-invoices" element={<SupplierInvoices />} />
        <Route path="/finance/periods" element={<PeriodClosing />} />
        <Route path="/finance/periods/:id" element={<PeriodDetail />} />
        <Route path="/reports/credit-aging" element={<CreditAgingReport />} />
      </Route>

      {/* Default Redirects */}
      <Route path="/" element={<DefaultRedirect />} />
      <Route path="*" element={<DefaultRedirect />} />
    </Routes>
  );
};

export default AppRoutes;
