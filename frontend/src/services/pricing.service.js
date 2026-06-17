import apiClient from './api.client';

const BASE = '/price-history';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

// ── mock data ──────────────────────────────────────────────────────────────
const MOCK_ENTRIES = [
  {
    id: 1, product_id: 1, product_sku: 'POT-001', product_name: 'Nồi inox 20cm',
    effective_date: '2026-06-01', end_date: '2026-06-30',
    cost_price: 80000, selling_price: 115000, status: 'APPROVED', notes: null,
    created_by: { id: 5, full_name: 'Nguyễn Kế Toán' },
    approved_by: { id: 6, full_name: 'Phạm Kế Toán Trưởng' },
    approved_at: '2026-05-28T14:00:00Z', cancelled_by: null, cancelled_at: null,
    created_at: '2026-05-25T09:00:00Z',
  },
  {
    id: 2, product_id: 1, product_sku: 'POT-001', product_name: 'Nồi inox 20cm',
    effective_date: '2026-07-01', end_date: '2026-07-31',
    cost_price: 85000, selling_price: 120000, status: 'PENDING',
    notes: 'NCC tăng nguyên liệu', created_by: { id: 5, full_name: 'Nguyễn Kế Toán' },
    approved_by: null, approved_at: null, cancelled_by: null, cancelled_at: null,
    created_at: '2026-06-20T10:00:00Z',
  },
  {
    id: 3, product_id: 2, product_sku: 'PAN-002', product_name: 'Chảo chống dính 26cm',
    effective_date: '2026-07-01', end_date: '2026-07-31',
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
      return list;
    }
    const query = new URLSearchParams();
    if (params.status) query.set('status', params.status);
    if (params.product_id) query.set('productId', params.product_id);
    return apiClient.get(`${BASE}?${query}`);
  },

  async getById(id) {
    if (USE_MOCK) {
      await delay(200);
      const entry = mockEntries.find(e => e.id === id);
      if (!entry) throw new Error('Bản giá không tồn tại');
      return entry;
    }
    return apiClient.get(`${BASE}/${id}`);
  },

  async getByProduct(productId) {
    if (USE_MOCK) {
      await delay(200);
      const entries = mockEntries.filter(e => e.product_id === productId);
      return { product_id: productId, product_sku: entries[0]?.product_sku ?? '', entries };
    }
    return apiClient.get(`/products/${productId}/price-history`);
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
        end_date: data.end_date,
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
    return apiClient.post(BASE, data);
  },

  async update(id, data) {
    if (USE_MOCK) {
      await delay(300);
      const idx = mockEntries.findIndex(e => e.id === id);
      if (idx === -1) throw new Error('Bản giá không tồn tại');
      mockEntries[idx] = { ...mockEntries[idx], ...data };
      return mockEntries[idx];
    }
    return apiClient.put(`${BASE}/${id}`, data);
  },

  async cancel(id) {
    if (USE_MOCK) {
      await delay(300);
      const idx = mockEntries.findIndex(e => e.id === id);
      if (idx === -1) throw new Error('Bản giá không tồn tại');
      mockEntries[idx] = { ...mockEntries[idx], status: 'CANCELLED', cancelled_at: new Date().toISOString() };
      return mockEntries[idx];
    }
    return apiClient.delete(`${BASE}/${id}`);
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
    return apiClient.put(`${BASE}/${id}/approve`);
  },

  async lookup(productId, date) {
    if (USE_MOCK) {
      await delay(200);
      const entry = mockEntries.find(
        e => e.product_id === productId && e.status === 'APPROVED'
          && e.effective_date <= date && e.end_date >= date
      );
      if (!entry) throw Object.assign(new Error('Không có giá hợp lệ'), { status: 404 });
      return entry;
    }
    return apiClient.get(`${BASE}/lookup?product_id=${productId}&date=${date}`);
  },

  async importExcel(file) {
    if (USE_MOCK) {
      await delay(800);
      return { total_rows: 3, created_count: 2, failed_count: 1, created: [], failed: [] };
    }
    const form = new FormData();
    form.append('file', file);
    return apiClient.post(`${BASE}/import`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getTemplateUrl() {
    return `${import.meta.env.VITE_API_BASE_URL || '/api/v1'}${BASE}/import/template`;
  },
};

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export default pricingService;
