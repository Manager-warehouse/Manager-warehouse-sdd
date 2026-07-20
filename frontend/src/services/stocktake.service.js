import apiClient from './api.client';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

// ─── localStorage helpers ─────────────────────────────────────────────────────

const KEYS = {
  STOCKTAKES: 'wms_db_stocktakes',
  STOCKTAKE_ITEMS: 'wms_db_stocktake_items',
};

const getDb = (key, initial) => {
  const data = localStorage.getItem(key);
  if (!data) {
    localStorage.setItem(key, JSON.stringify(initial));
    return initial;
  }
  try {
    return JSON.parse(data);
  } catch {
    return initial;
  }
};

const saveDb = (key, data) => localStorage.setItem(key, JSON.stringify(data));

const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms));

// ─── Seed data ────────────────────────────────────────────────────────────────

const SEED_STOCKTAKES = [
  {
    id: 1,
    stock_take_number: 'ST-20260617-000001',
    warehouse_id: 1,
    warehouse_name: 'Kho Hải Phòng',
    conducted_by_id: 3,
    conducted_by_name: 'Nguyễn Thủ Kho',
    approved_by_id: null,
    approved_by_name: null,
    approved_at: null,
    status: 'DRAFT',
    approval_level: null,
    is_employee_fault: false,
    rejection_reason: null,
    total_variance_value: 0,
    stock_take_date: '2026-06-17',
    document_date: '2026-06-17',
    accounting_period_id: 1,
    accounting_period_name: 'T06/2026',
    created_at: '2026-06-17T08:00:00+07:00',
    updated_at: '2026-06-17T08:00:00+07:00',
    items: [],
  },
  {
    id: 2,
    stock_take_number: 'ST-20260610-000001',
    warehouse_id: 1,
    warehouse_name: 'Kho Hải Phòng',
    conducted_by_id: 3,
    conducted_by_name: 'Nguyễn Thủ Kho',
    approved_by_id: 2,
    approved_by_name: 'Trần Trưởng Kho',
    approved_at: '2026-06-10T16:00:00+07:00',
    status: 'APPROVED',
    approval_level: 'MANAGER',
    is_employee_fault: false,
    rejection_reason: null,
    total_variance_value: -12500000,
    stock_take_date: '2026-06-10',
    document_date: '2026-06-10',
    accounting_period_id: 1,
    accounting_period_name: 'T06/2026',
    created_at: '2026-06-10T08:00:00+07:00',
    updated_at: '2026-06-10T16:00:00+07:00',
    items: [],
  },
  {
    id: 3,
    stock_take_number: 'ST-20260615-000001',
    warehouse_id: 1,
    warehouse_name: 'Kho Hải Phòng',
    conducted_by_id: 3,
    conducted_by_name: 'Nguyễn Thủ Kho',
    approved_by_id: null,
    approved_by_name: null,
    approved_at: null,
    status: 'PENDING_APPROVAL',
    approval_level: 'MANAGER',
    is_employee_fault: false,
    rejection_reason: null,
    total_variance_value: -45000000,
    stock_take_date: '2026-06-15',
    document_date: '2026-06-15',
    accounting_period_id: 1,
    accounting_period_name: 'T06/2026',
    created_at: '2026-06-15T08:00:00+07:00',
    updated_at: '2026-06-15T14:00:00+07:00',
    items: [
      {
        id: 1,
        product_id: 1,
        product_sku: 'SKU-001',
        product_name: 'Nồi inox 24cm',
        batch_id: 1,
        batch_number: 'BT-20260501-000001',
        location_id: 10,
        location_code: 'WH-HP.A.01.1.01',
        system_qty: 100,
        actual_qty: 88,
        variance_qty: -12,
        variance_value: -18000000,
        is_employee_fault: false,
        notes: null,
      },
      {
        id: 2,
        product_id: 2,
        product_sku: 'SKU-002',
        product_name: 'Chảo chống dính 28cm',
        batch_id: 2,
        batch_number: 'BT-20260501-000002',
        location_id: 11,
        location_code: 'WH-HP.A.01.1.02',
        system_qty: 50,
        actual_qty: 45,
        variance_qty: -5,
        variance_value: -27000000,
        is_employee_fault: false,
        notes: null,
      },
    ],
  },
];

// ─── Service ──────────────────────────────────────────────────────────────────

export const stocktakeService = {

  getStockTakes: async (warehouseId, status, page = 0, size = 10) => {
    if (useMock) {
      await delay();
      let list = getDb(KEYS.STOCKTAKES, SEED_STOCKTAKES);
      list = list.filter((s) => s.warehouse_id === Number(warehouseId));
      if (status) list = list.filter((s) => s.status === status);
      const totalElements = list.length;
      const totalPages = Math.max(1, Math.ceil(totalElements / size));
      const start = page * size;
      const content = list.slice(start, start + size);
      return { content, totalElements, totalPages, number: page, size };
    }
    const params = { warehouse_id: warehouseId, page, size };
    if (status) params.status = status;
    const res = await apiClient.get('/stocktakes', { params });
    return res.data;
  },

  getStockTakeById: async (id) => {
    if (useMock) {
      await delay();
      const list = getDb(KEYS.STOCKTAKES, SEED_STOCKTAKES);
      const st = list.find((s) => s.id === Number(id));
      if (!st) throw new Error('Không tìm thấy phiếu kiểm kê');
      return st;
    }
    const res = await apiClient.get(`/stocktakes/${id}`);
    return res.data;
  },

  createStockTake: async (data) => {
    if (useMock) {
      await delay();
      const list = getDb(KEYS.STOCKTAKES, SEED_STOCKTAKES);
      const newId = Math.max(0, ...list.map((s) => s.id)) + 1;
      const now = new Date().toISOString();
      const dateStr = now.slice(0, 10).replace(/-/g, '');
      const num = String(newId).padStart(6, '0');
      const newSt = {
        id: newId,
        stock_take_number: `ST-${dateStr}-${num}`,
        warehouse_id: data.warehouse_id,
        warehouse_name: 'Kho hiện tại',
        conducted_by_id: 3,
        conducted_by_name: 'Người dùng hiện tại',
        approved_by_id: null,
        approved_by_name: null,
        approved_at: null,
        status: 'DRAFT',
        approval_level: null,
        is_employee_fault: false,
        rejection_reason: null,
        total_variance_value: 0,
        stock_take_date: data.stock_take_date,
        document_date: data.document_date,
        accounting_period_id: data.accounting_period_id,
        accounting_period_name: 'T06/2026',
        created_at: now,
        updated_at: now,
        items: [],
      };
      list.push(newSt);
      saveDb(KEYS.STOCKTAKES, list);
      return newSt;
    }
    const res = await apiClient.post('/stocktakes', data);
    return res.data;
  },

  startStockTake: async (id) => {
    if (useMock) {
      await delay();
      const list = getDb(KEYS.STOCKTAKES, SEED_STOCKTAKES);
      const st = list.find((s) => s.id === Number(id));
      if (!st) throw new Error('Không tìm thấy phiếu kiểm kê');
      if (st.status !== 'DRAFT') throw new Error('Phiếu phải ở trạng thái DRAFT để bắt đầu kiểm kê');
      st.status = 'IN_PROGRESS';
      st.updated_at = new Date().toISOString();
      saveDb(KEYS.STOCKTAKES, list);
      return st;
    }
    const res = await apiClient.put(`/stocktakes/${id}/start`);
    return res.data;
  },

  recordCount: async (id, items) => {
    if (useMock) {
      await delay();
      const list = getDb(KEYS.STOCKTAKES, SEED_STOCKTAKES);
      const st = list.find((s) => s.id === Number(id));
      if (!st) throw new Error('Không tìm thấy phiếu kiểm kê');
      if (st.status !== 'IN_PROGRESS') throw new Error('Phiếu phải ở trạng thái IN_PROGRESS');
      for (const countItem of items) {
        if (countItem.actual_qty < 0) throw new Error('Số lượng thực tế không được âm');
        const item = st.items.find((i) => i.id === countItem.item_id);
        if (item) {
          item.actual_qty = countItem.actual_qty;
          item.variance_qty = countItem.actual_qty - item.system_qty;
          item.variance_value = item.variance_qty * 50000; // mock cost price
          item.is_employee_fault = countItem.is_employee_fault || false;
          item.notes = countItem.notes || null;
        }
      }
      st.updated_at = new Date().toISOString();
      saveDb(KEYS.STOCKTAKES, list);
      return st;
    }
    const res = await apiClient.put(`/stocktakes/${id}/count`, { items });
    return res.data;
  },

  completeStockTake: async (id) => {
    if (useMock) {
      await delay();
      const list = getDb(KEYS.STOCKTAKES, SEED_STOCKTAKES);
      const st = list.find((s) => s.id === Number(id));
      if (!st) throw new Error('Không tìm thấy phiếu kiểm kê');
      const allCounted = st.items.every((i) => i.actual_qty !== null && i.actual_qty !== undefined);
      if (!allCounted) throw new Error('Tất cả dòng hàng phải có số lượng thực tế trước khi hoàn tất');
      const total = st.items.reduce((sum, i) => sum + (i.variance_value || 0), 0);
      st.total_variance_value = total;
      const abs = Math.abs(total);
      st.approval_level = abs < 5000000 ? 'AUTO' : abs <= 100000000 && !st.is_employee_fault ? 'MANAGER' : 'CEO';
      st.status = st.approval_level === 'AUTO' ? 'APPROVED' : 'PENDING_APPROVAL';
      st.updated_at = new Date().toISOString();
      saveDb(KEYS.STOCKTAKES, list);
      return st;
    }
    const res = await apiClient.put(`/stocktakes/${id}/complete`);
    return res.data;
  },

  cancelStockTake: async (id) => {
    if (useMock) {
      await delay();
      const list = getDb(KEYS.STOCKTAKES, SEED_STOCKTAKES);
      const st = list.find((s) => s.id === Number(id));
      if (!st) throw new Error('Không tìm thấy phiếu kiểm kê');
      if (st.status !== 'DRAFT' && st.status !== 'IN_PROGRESS') {
        throw new Error('Chỉ có thể hủy phiếu ở trạng thái DRAFT hoặc IN_PROGRESS');
      }
      st.status = 'CANCELLED';
      st.updated_at = new Date().toISOString();
      saveDb(KEYS.STOCKTAKES, list);
      return st;
    }
    const res = await apiClient.put(`/stocktakes/${id}/cancel`);
    return res.data;
  },

  approveStockTake: async (id) => {
    if (useMock) {
      await delay();
      const list = getDb(KEYS.STOCKTAKES, SEED_STOCKTAKES);
      const st = list.find((s) => s.id === Number(id));
      if (!st) throw new Error('Không tìm thấy phiếu kiểm kê');
      st.status = 'APPROVED';
      st.approved_by_id = 2;
      st.approved_by_name = 'Trần Trưởng Kho';
      st.approved_at = new Date().toISOString();
      st.updated_at = new Date().toISOString();
      saveDb(KEYS.STOCKTAKES, list);
      return st;
    }
    const res = await apiClient.put(`/stocktakes/${id}/approve`);
    return res.data;
  },

  rejectStockTake: async (id, rejectionReason) => {
    if (useMock) {
      await delay();
      const list = getDb(KEYS.STOCKTAKES, SEED_STOCKTAKES);
      const st = list.find((s) => s.id === Number(id));
      if (!st) throw new Error('Không tìm thấy phiếu kiểm kê');
      st.status = 'REJECTED';
      st.rejection_reason = rejectionReason;
      st.updated_at = new Date().toISOString();
      saveDb(KEYS.STOCKTAKES, list);
      return st;
    }
    const res = await apiClient.put(`/stocktakes/${id}/reject`, { rejection_reason: rejectionReason });
    return res.data;
  },

  approveCeoStockTake: async (id) => {
    if (useMock) {
      await delay();
      const list = getDb(KEYS.STOCKTAKES, SEED_STOCKTAKES);
      const st = list.find((s) => s.id === Number(id));
      if (!st) throw new Error('Không tìm thấy phiếu kiểm kê');
      st.status = 'APPROVED';
      st.approved_by_id = 1;
      st.approved_by_name = 'CEO';
      st.approved_at = new Date().toISOString();
      st.updated_at = new Date().toISOString();
      saveDb(KEYS.STOCKTAKES, list);
      return st;
    }
    const res = await apiClient.put(`/stocktakes/${id}/approve-ceo`);
    return res.data;
  },

  rejectCeoStockTake: async (id, rejectionReason) => {
    if (useMock) {
      await delay();
      const list = getDb(KEYS.STOCKTAKES, SEED_STOCKTAKES);
      const st = list.find((s) => s.id === Number(id));
      if (!st) throw new Error('Không tìm thấy phiếu kiểm kê');
      st.status = 'REJECTED';
      st.rejection_reason = rejectionReason;
      st.updated_at = new Date().toISOString();
      saveDb(KEYS.STOCKTAKES, list);
      return st;
    }
    const res = await apiClient.put(`/stocktakes/${id}/reject-ceo`, { rejection_reason: rejectionReason });
    return res.data;
  },
};
