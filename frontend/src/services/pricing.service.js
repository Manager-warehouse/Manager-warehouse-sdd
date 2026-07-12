import apiClient from './api.client';

const BASE = '/price-history';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

// ── mock data ──────────────────────────────────────────────────────────────
const MOCK_ENTRIES = [
  {
    id: 1, product_id: 1, product_sku: 'POT-001', product_name: 'Nồi inox 20cm',
    effective_date: '2026-06-01',
    cost_price: 80000, selling_price: 115000, status: 'APPROVED', notes: null,
    created_by: { id: 5, full_name: 'Nguyễn Kế Toán' },
    approved_by: { id: 6, full_name: 'Phạm Kế Toán Trưởng' },
    approved_at: '2026-05-28T14:00:00Z', cancelled_by: null, cancelled_at: null,
    created_at: '2026-05-25T09:00:00Z',
  },
  {
    id: 2, product_id: 1, product_sku: 'POT-001', product_name: 'Nồi inox 20cm',
    effective_date: '2026-07-01',
    cost_price: 85000, selling_price: 120000, status: 'PENDING',
    notes: 'NCC tăng nguyên liệu', created_by: { id: 5, full_name: 'Nguyễn Kế Toán' },
    approved_by: null, approved_at: null, cancelled_by: null, cancelled_at: null,
    created_at: '2026-06-20T10:00:00Z',
  },
  {
    id: 3, product_id: 2, product_sku: 'PAN-002', product_name: 'Chảo chống dính 26cm',
    effective_date: '2026-07-01',
    cost_price: 120000, selling_price: 175000, status: 'PENDING', notes: null,
    created_by: { id: 5, full_name: 'Nguyễn Kế Toán' },
    approved_by: null, approved_at: null, cancelled_by: null, cancelled_at: null,
    created_at: '2026-06-21T08:30:00Z',
  },
];

let mockEntries = [...MOCK_ENTRIES];
let nextId = 10;

// ── API calls ──────────────────────────────────────────────────────────────

const pricingService = {
  async getAll(params = {}) {
    if (USE_MOCK) {
      await delay(300);
      let list = [...mockEntries];
      if (params.status) list = list.filter(e => e.status === params.status);
      if (params.product_id) list = list.filter(e => e.product_id === Number(params.product_id));
      if (params.warehouse_id) list = list.filter(e => e.warehouse_id === Number(params.warehouse_id));
      return list;
    }
    const query = new URLSearchParams();
    if (params.status) query.set('status', params.status);
    if (params.product_id) query.set('productId', params.product_id);
    if (params.warehouse_id) query.set('warehouseId', params.warehouse_id);
    const response = await apiClient.get(`${BASE}?${query}`);
    return response.data;
  },

  async getById(id) {
    if (USE_MOCK) {
      await delay(200);
      const entry = mockEntries.find(e => e.id === id);
      if (!entry) throw new Error('Bản giá không tồn tại');
      return entry;
    }
    const response = await apiClient.get(`${BASE}/${id}`);
    return response.data;
  },

  async getByProduct(productId) {
    if (USE_MOCK) {
      await delay(200);
      const entries = mockEntries.filter(e => e.product_id === productId);
      return { product_id: productId, product_sku: entries[0]?.product_sku ?? '', entries };
    }
    const response = await apiClient.get(`/products/${productId}/price-history`);
    return response.data;
  },

  async lookupApproved({ product_id, warehouse_id, date }) {
    if (USE_MOCK) {
      await delay(200);
      const targetDate = date || new Date().toISOString().slice(0, 10);
      // Effective-date-only model: the APPROVED entry with the latest
      // effective_date not after targetDate is the one in effect.
      const entry = mockEntries
        .filter(e => {
          if (e.product_id !== Number(product_id) || e.status !== 'APPROVED') return false;
          if (e.warehouse_id && Number(e.warehouse_id) !== Number(warehouse_id)) return false;
          return e.effective_date <= targetDate;
        })
        .sort((a, b) => (a.effective_date < b.effective_date ? 1 : -1))[0];
      if (!entry) {
        const error = new Error('PRICE_NOT_FOUND');
        error.response = { status: 404 };
        throw error;
      }
      return entry;
    }
    const response = await apiClient.get(`${BASE}/lookup`, {
      params: {
        productId: Number(product_id),
        warehouseId: Number(warehouse_id),
        date,
      },
    });
    return response.data;
  },

  async create(data) {
    if (USE_MOCK) {
      await delay(400);
      const entry = {
        id: nextId++,
        product_id: data.product_id,
        product_sku: 'SKU-' + data.product_id,
        product_name: 'Sản phẩm ' + data.product_id,
        effective_date: data.effective_date,
        cost_price: Number(data.cost_price),
        selling_price: Number(data.selling_price),
        notes: data.notes || null,
        status: 'PENDING',
        created_by: { id: 5, full_name: 'Nguyễn Kế Toán' },
        approved_by: null, approved_at: null,
        cancelled_by: null, cancelled_at: null,
        created_at: new Date().toISOString(),
      };
      mockEntries.push(entry);
      return entry;
    }
    const response = await apiClient.post(BASE, data);
    return response.data;
  },

  async update(id, data) {
    if (USE_MOCK) {
      await delay(300);
      const idx = mockEntries.findIndex(e => e.id === id);
      if (idx === -1) throw new Error('Bản giá không tồn tại');
      mockEntries[idx] = { ...mockEntries[idx], ...data };
      return mockEntries[idx];
    }
    const { effective_date, cost_price, selling_price, notes } = data;
    const response = await apiClient.put(`${BASE}/${id}`, { effective_date, cost_price, selling_price, notes });
    return response.data;
  },

  async cancel(id) {
    if (USE_MOCK) {
      await delay(300);
      const idx = mockEntries.findIndex(e => e.id === id);
      if (idx === -1) throw new Error('Bản giá không tồn tại');
      mockEntries[idx] = { ...mockEntries[idx], status: 'CANCELLED', cancelled_at: new Date().toISOString() };
      return mockEntries[idx];
    }
    const response = await apiClient.put(`${BASE}/${id}/cancel`);
    return response.data;
  },

  async approve(id) {
    if (USE_MOCK) {
      await delay(300);
      const idx = mockEntries.findIndex(e => e.id === id);
      if (idx === -1) throw new Error('Bản giá không tồn tại');
      mockEntries[idx] = {
        ...mockEntries[idx], status: 'APPROVED',
        approved_by: { id: 6, full_name: 'Phạm Kế Toán Trưởng' },
        approved_at: new Date().toISOString(),
      };
      return mockEntries[idx];
    }
    const response = await apiClient.put(`${BASE}/${id}/approve`);
    return response.data;
  },

  async lookup(productId, warehouseId, date) {
    if (USE_MOCK) {
      await delay(200);
      // Effective-date-only model: latest effective_date <= date wins.
      const entry = mockEntries
        .filter(e => e.product_id === productId && e.warehouse_id === warehouseId
          && e.status === 'APPROVED' && e.effective_date <= date)
        .sort((a, b) => (a.effective_date < b.effective_date ? 1 : -1))[0];
      if (!entry) throw Object.assign(new Error('Không có giá hợp lệ'), { status: 404 });
      return entry;
    }
    const response = await apiClient.get(`${BASE}/lookup?productId=${productId}&warehouseId=${warehouseId}&date=${date}`);
    return response.data;
  },

  async importExcel(file) {
    if (USE_MOCK) {
      await delay(800);
      return { total_rows: 3, created_count: 2, failed_count: 1, created: [], failed: [] };
    }
    const form = new FormData();
    form.append('file', file);
    const response = await apiClient.post(`${BASE}/import`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  async downloadTemplate() {
    const response = await apiClient.get(`${BASE}/import/template`, { responseType: 'blob' });
    const url = URL.createObjectURL(response.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'price_import_template.xlsx';
    a.click();
    URL.revokeObjectURL(url);
  },

  async exportXlsx(params = {}) {
    const apiParams = {};
    if (params.status) apiParams.status = params.status;
    if (params.warehouse_id) apiParams.warehouseId = params.warehouse_id;
    const response = await apiClient.get(`${BASE}/export`, { params: apiParams, responseType: 'blob' });
    const url = URL.createObjectURL(response.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = `bang-gia-${new Date().toISOString().slice(0, 10)}.xlsx`;
    a.click();
    URL.revokeObjectURL(url);
  },
};

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export default pricingService;
