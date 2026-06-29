import apiClient from './api.client';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

// ── mock data ──────────────────────────────────────────────────────────────
const MOCK_CEO_DASHBOARD = {
  as_of_time: new Date().toISOString(),
  kpis: {
    total_inventory_value: 4850900000.00,
    p_and_l: {
      period: '2026-06',
      revenue: 1250000000.00,
      cogs: 850000000.00,
      operating_costs: 75000000.00,
      net_profit: 325000000.00
    },
    qc_failure_rate: 0.024,
    on_time_delivery_rate: 0.945
  },
  top_debtors: [
    { dealer_id: 12, dealer_name: 'Đại lý Phúc Hưng', overdue_amount: 150000000.00, max_overdue_days: 18 },
    { dealer_id: 7, dealer_name: 'Đại lý Minh Anh', overdue_amount: 90000000.00, max_overdue_days: 12 }
  ]
};

const MOCK_VALUATION = {
  generated_at: new Date().toISOString(),
  filters: { warehouse_id: 1 },
  summary: { total_items: 2, total_qty: 800.00, total_valuation: 70100000.00 },
  records: [
    { warehouse_id: 1, warehouse_name: 'Kho Hải Phòng', product_id: 42, product_sku: 'POT-001', product_name: 'Nồi inox 3 đáy Supor', batch_number: 'BAT-20260601-HP', total_qty: 500.00, unit_cost: 85000.00, valuation_amount: 42500000.00 },
    { warehouse_id: 1, warehouse_name: 'Kho Hải Phòng', product_id: 15, product_sku: 'PAN-002', product_name: 'Chảo chống dính Sunhouse 26cm', batch_number: 'BAT-20260515-HP', total_qty: 300.00, unit_cost: 92000.00, valuation_amount: 27600000.00 }
  ]
};

const MOCK_PRODUCTIVITY = {
  warehouse_id: 1,
  warehouse_name: 'Kho Hải Phòng',
  start_date: '2026-06-01',
  end_date: '2026-06-15',
  staff_productivity: [
    { employee_code: 'NV-008', full_name: 'Trần Văn Bằng', role: 'WAREHOUSE_STAFF', picking_runs_count: 45, total_picked_qty: 2350.00 },
    { employee_code: 'NV-012', full_name: 'Lê Văn Cường', role: 'WAREHOUSE_STAFF', picking_runs_count: 38, total_picked_qty: 1980.00 }
  ],
  storekeeper_productivity: [
    { employee_code: 'TK-002', full_name: 'Nguyễn Thị Mai', role: 'STOREKEEPER', picking_plans_created: 28, total_qc_checked_qty: 4330.00 }
  ],
  driver_productivity: [
    { employee_code: 'TX-005', full_name: 'Phạm Văn Đông', role: 'DRIVER', trips_completed: 15, successful_deliveries: 42 }
  ]
};

const MOCK_ALERTS = {
  content: [
    { id: 105, warehouse_id: 1, warehouse_name: 'Kho Hải Phòng', product_id: 42, product_sku: 'POT-001', product_name: 'Nồi inox 3 đáy Supor', current_qty: 45.00, reorder_point: 100.00, alert_type: 'LOW_STOCK', is_resolved: false, resolved_at: null, created_at: new Date().toISOString() },
    { id: 98, warehouse_id: 2, warehouse_name: 'Kho Hà Nội', product_id: 15, product_sku: 'PAN-002', product_name: 'Chảo chống dính Sunhouse 26cm', current_qty: 150.00, reorder_point: 120.00, alert_type: 'LOW_STOCK', is_resolved: true, resolved_at: new Date().toISOString(), created_at: new Date().toISOString() }
  ],
  totalElements: 2,
  totalPages: 1
};

// Helper to support normalization between camelCase from backend and snake_case in UI
const value = (object, camel, snake, fallback = null) => {
  if (object?.[camel] !== undefined && object?.[camel] !== null) return object[camel];
  if (object?.[snake] !== undefined && object?.[snake] !== null) return object[snake];
  return fallback;
};

const asArray = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  return [];
};

const normalizeCeoDashboard = (data = {}) => {
  const rawKpis = value(data, 'kpis', 'kpis', {});
  const rawPL = value(rawKpis, 'pAndL', 'p_and_l', {});
  const rawDebtors = asArray(value(data, 'topDebtors', 'top_debtors', []));
  
  return {
    as_of_time: value(data, 'asOfTime', 'as_of_time'),
    kpis: {
      total_inventory_value: Number(value(rawKpis, 'totalInventoryValue', 'total_inventory_value', 0)),
      qc_failure_rate: Number(value(rawKpis, 'qcFailureRate', 'qc_failure_rate', 0)),
      on_time_delivery_rate: Number(value(rawKpis, 'onTimeDeliveryRate', 'on_time_delivery_rate', 0)),
      p_and_l: {
        period: value(rawPL, 'period', 'period', ''),
        revenue: Number(value(rawPL, 'revenue', 'revenue', 0)),
        cogs: Number(value(rawPL, 'cogs', 'cogs', 0)),
        operating_costs: Number(value(rawPL, 'operatingCosts', 'operating_costs', 0)),
        net_profit: Number(value(rawPL, 'netProfit', 'net_profit', 0))
      }
    },
    top_debtors: rawDebtors.map(d => ({
      dealer_id: value(d, 'dealerId', 'dealer_id'),
      dealer_name: value(d, 'dealerName', 'dealer_name', ''),
      overdue_amount: Number(value(d, 'overdueAmount', 'overdue_amount', 0)),
      max_overdue_days: Number(value(d, 'maxOverdueDays', 'max_overdue_days', 0))
    }))
  };
};

const normalizeInventoryValuation = (data = {}) => {
  const rawFilters = value(data, 'filters', 'filters', {});
  const rawSummary = value(data, 'summary', 'summary', {});
  const rawRecords = asArray(value(data, 'records', 'records', []));
  
  return {
    generated_at: value(data, 'generatedAt', 'generated_at'),
    filters: {
      warehouse_id: value(rawFilters, 'warehouseId', 'warehouse_id')
    },
    summary: {
      total_items: Number(value(rawSummary, 'totalItems', 'total_items', 0)),
      total_qty: Number(value(rawSummary, 'totalQty', 'total_qty', 0)),
      total_valuation: Number(value(rawSummary, 'totalValuation', 'total_valuation', 0))
    },
    records: rawRecords.map(r => ({
      warehouse_id: value(r, 'warehouseId', 'warehouse_id'),
      warehouse_name: value(r, 'warehouseName', 'warehouse_name', ''),
      product_id: value(r, 'productId', 'product_id'),
      product_sku: value(r, 'productSku', 'product_sku', ''),
      product_name: value(r, 'productName', 'product_name', ''),
      batch_number: value(r, 'batchNumber', 'batch_number', ''),
      total_qty: Number(value(r, 'totalQty', 'total_qty', 0)),
      unit_cost: Number(value(r, 'unitCost', 'unit_cost', 0)),
      valuation_amount: Number(value(r, 'valuationAmount', 'valuation_amount', 0))
    }))
  };
};

const normalizeProductivityReport = (data = {}) => {
  const rawStaff = asArray(value(data, 'staffProductivity', 'staff_productivity', []));
  const rawSk = asArray(value(data, 'storekeeperProductivity', 'storekeeper_productivity', []));
  const rawDriver = asArray(value(data, 'driverProductivity', 'driver_productivity', []));
  
  return {
    warehouse_id: value(data, 'warehouseId', 'warehouse_id'),
    warehouse_name: value(data, 'warehouseName', 'warehouse_name', ''),
    start_date: value(data, 'startDate', 'start_date', ''),
    end_date: value(data, 'endDate', 'end_date', ''),
    staff_productivity: rawStaff.map(p => ({
      employee_code: value(p, 'employeeCode', 'employee_code', ''),
      full_name: value(p, 'fullName', 'full_name', ''),
      role: value(p, 'role', 'role', ''),
      picking_runs_count: Number(value(p, 'pickingRunsCount', 'picking_runs_count', 0)),
      total_picked_qty: Number(value(p, 'totalPickedQty', 'total_picked_qty', 0))
    })),
    storekeeper_productivity: rawSk.map(p => ({
      employee_code: value(p, 'employeeCode', 'employee_code', ''),
      full_name: value(p, 'fullName', 'full_name', ''),
      role: value(p, 'role', 'role', ''),
      picking_plans_created: Number(value(p, 'pickingPlansCreated', 'picking_plans_created', 0)),
      total_qc_checked_qty: Number(value(p, 'totalQcCheckedQty', 'total_qc_checked_qty', 0))
    })),
    driver_productivity: rawDriver.map(p => ({
      employee_code: value(p, 'employeeCode', 'employee_code', ''),
      full_name: value(p, 'fullName', 'full_name', ''),
      role: value(p, 'role', 'role', ''),
      trips_completed: Number(value(p, 'tripsCompleted', 'trips_completed', 0)),
      successful_deliveries: Number(value(p, 'successfulDeliveries', 'successful_deliveries', 0))
    }))
  };
};

const normalizeLowStockAlerts = (data = {}) => {
  const rawContent = asArray(value(data, 'content', 'content', []));
  return {
    content: rawContent.map(a => ({
      id: value(a, 'id', 'id'),
      warehouse_id: value(a, 'warehouseId', 'warehouse_id'),
      warehouse_name: value(a, 'warehouseName', 'warehouse_name', ''),
      product_id: value(a, 'productId', 'product_id'),
      product_sku: value(a, 'productSku', 'product_sku', ''),
      product_name: value(a, 'productName', 'product_name', ''),
      current_qty: Number(value(a, 'currentQty', 'current_qty', 0)),
      reorder_point: Number(value(a, 'reorderPoint', 'reorder_point', 0)),
      alert_type: value(a, 'alertType', 'alert_type', 'LOW_STOCK'),
      is_resolved: value(a, 'isResolved', 'is_resolved', false),
      resolved_at: value(a, 'resolvedAt', 'resolved_at'),
      created_at: value(a, 'createdAt', 'created_at')
    })),
    totalElements: value(data, 'totalElements', 'totalElements', 0),
    totalPages: value(data, 'totalPages', 'totalPages', 0)
  };
};

// ── API calls ──────────────────────────────────────────────────────────────

const reportService = {
  async getCeoDashboard() {
    if (USE_MOCK) {
      await delay(400);
      return normalizeCeoDashboard(MOCK_CEO_DASHBOARD);
    }
    const response = await apiClient.get('/dashboard/ceo');
    return normalizeCeoDashboard(response.data);
  },

  async getInventoryValuation(warehouseId = null) {
    if (USE_MOCK) {
      await delay(400);
      return normalizeInventoryValuation(MOCK_VALUATION);
    }
    const query = warehouseId ? `?warehouseId=${warehouseId}` : '';
    const response = await apiClient.get(`/reports/inventory-valuation${query}`);
    return normalizeInventoryValuation(response.data);
  },

  async getProductivityReport(warehouseId, startDate, endDate) {
    if (USE_MOCK) {
      await delay(400);
      return normalizeProductivityReport(MOCK_PRODUCTIVITY);
    }
    const response = await apiClient.get(`/reports/productivity`, {
      params: { warehouseId, startDate, endDate }
    });
    return normalizeProductivityReport(response.data);
  },

  async exportProductivityExcel(warehouseId, startDate, endDate) {
    if (USE_MOCK) {
      await delay(500);
      alert('Mock Export Excel: Tải file thành công!');
      return;
    }
    const response = await apiClient.get(`/reports/productivity/export`, {
      params: { warehouseId, startDate, endDate },
      responseType: 'blob'
    });
    const url = URL.createObjectURL(response.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Productivity_Report_WH${warehouseId}_${startDate}_${endDate}.xlsx`;
    a.click();
    URL.revokeObjectURL(url);
  },

  async getLowStockAlerts(params = {}) {
    if (USE_MOCK) {
      await delay(300);
      return normalizeLowStockAlerts(MOCK_ALERTS);
    }
    const apiParams = {
      page: params.page || 0,
      size: params.size || 30
    };
    if (params.warehouseId) apiParams.warehouseId = params.warehouseId;
    if (params.productId) apiParams.productId = params.productId;
    if (params.isResolved !== undefined) apiParams.isResolved = params.isResolved;

    const response = await apiClient.get(`/alerts/low-stock`, { params: apiParams });
    return normalizeLowStockAlerts(response.data);
  }
};

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export default reportService;
