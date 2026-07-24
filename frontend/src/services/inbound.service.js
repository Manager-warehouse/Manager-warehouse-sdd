import apiClient, { useMock } from './api.client';
import { masterDataService } from './masterData.service';

const KEYS = {
  RECEIPTS: 'wms_db_receipts',
  RECEIPT_ITEMS: 'wms_db_receipt_items',
  BATCHES: 'wms_db_batches',
  DEBIT_NOTES: 'wms_db_debit_notes',
  DAMAGE_REPORTS: 'wms_db_damage_reports',
  ADJUSTMENTS: 'wms_db_adjustments',
};

// Initial Mock Data
const INITIAL_RECEIPTS = [
  {
    id: 1,
    receipt_number: "RC-20260601-0001",
    source_order_code: "PO-ASUS-001",
    type: "PURCHASE",
    warehouse_id: 1, // HP
    supplier_id: 1,
    dealer_id: null,
    contact_person: "Nguyễn Văn Giao",
    source_channel: "Zalo",
    status: "APPROVED",
    approved_by: 3,
    approved_at: "2026-06-01T15:30:00Z",
    rejection_reason: null,
    document_date: "2026-06-01",
    accounting_period_id: 1,
    created_by: 7,
    notes: "Lệnh nhập linh kiện ASUS",
    created_at: "2026-06-01T08:00:00Z",
    updated_at: "2026-06-01T15:30:00Z"
  },
  {
    id: 2,
    receipt_number: "RC-20260602-0001",
    source_order_code: "PO-ASUS-002",
    type: "PURCHASE",
    warehouse_id: 1, // HP
    supplier_id: 1,
    dealer_id: null,
    contact_person: "Trần Hữu Giao",
    source_channel: "Email",
    status: "QC_COMPLETED",
    approved_by: null,
    approved_at: null,
    rejection_reason: null,
    document_date: "2026-06-02",
    accounting_period_id: 1,
    created_by: 7,
    notes: "Lệnh nhập đợt 2",
    created_at: "2026-06-02T09:00:00Z",
    updated_at: "2026-06-02T16:20:00Z"
  },
  {
    id: 3,
    receipt_number: "RC-20260603-0001",
    source_order_code: "DO-DL-HP01",
    type: "RETURN",
    warehouse_id: 1, // HP
    supplier_id: null,
    dealer_id: 1,
    contact_person: "Đại lý Hoàng Phát",
    source_channel: "Zalo",
    status: "PENDING_RECEIPT",
    approved_by: null,
    approved_at: null,
    rejection_reason: null,
    document_date: "2026-06-03",
    accounting_period_id: 1,
    created_by: 7,
    notes: "Đại lý trả lại hàng lỗi vỏ hộp",
    created_at: "2026-06-03T08:30:00Z",
    updated_at: "2026-06-03T08:30:00Z"
  },
  {
    id: 4,
    receipt_number: "RC-20260603-0002",
    source_order_code: "PO-ASUS-003",
    type: "PURCHASE",
    warehouse_id: 2, // HN
    supplier_id: 1,
    dealer_id: null,
    contact_person: "Nguyễn Văn Giao",
    source_channel: "Email",
    status: "DRAFT",
    approved_by: null,
    approved_at: null,
    rejection_reason: null,
    document_date: "2026-06-03",
    accounting_period_id: 1,
    created_by: 7,
    notes: "Lệnh nhập Asus Hà Nội",
    created_at: "2026-06-03T09:15:00Z",
    updated_at: "2026-06-03T11:00:00Z"
  },
  {
    id: 5,
    receipt_number: "RC-20260603-0003",
    source_order_code: "PO-ASUS-004",
    type: "PURCHASE",
    warehouse_id: 3, // HCM
    supplier_id: 1,
    dealer_id: null,
    contact_person: "Lê Giao Nhận HCM",
    source_channel: "Zalo",
    status: "PENDING_RECEIPT",
    approved_by: null,
    approved_at: null,
    rejection_reason: null,
    document_date: "2026-06-03",
    accounting_period_id: 1,
    created_by: 7,
    notes: "Lệnh nhập Asus cho chi nhánh HCM",
    created_at: "2026-06-03T08:30:00Z",
    updated_at: "2026-06-03T08:30:00Z"
  },
  {
    id: 6,
    receipt_number: "RC-20260603-0004",
    source_order_code: "PO-LOGI-001",
    type: "PURCHASE",
    warehouse_id: 3, // HCM
    supplier_id: 1,
    dealer_id: null,
    contact_person: "Lê Giao Nhận HCM",
    source_channel: "Email",
    status: "APPROVED",
    approved_by: 3,
    approved_at: "2026-06-03T11:20:00Z",
    rejection_reason: null,
    document_date: "2026-06-03",
    accounting_period_id: 1,
    created_by: 7,
    notes: "Lệnh nhập chuột Logitech HCM đã duyệt cất kho",
    created_at: "2026-06-03T09:15:00Z",
    updated_at: "2026-06-03T11:20:00Z"
  },
  {
    id: 7,
    receipt_number: "RC-20260603-0005",
    source_order_code: "PO-ASUS-005",
    type: "PURCHASE",
    warehouse_id: 3, // HCM
    supplier_id: 1,
    dealer_id: null,
    contact_person: "Trần Văn Giao",
    source_channel: "Phone",
    status: "DRAFT",
    approved_by: null,
    approved_at: null,
    rejection_reason: null,
    document_date: "2026-06-03",
    accounting_period_id: 1,
    created_by: 7,
    notes: "Lệnh nhập chờ kiểm QC HCM",
    created_at: "2026-06-03T10:00:00Z",
    updated_at: "2026-06-03T10:00:00Z"
  }
];


const INITIAL_RECEIPT_ITEMS = [
  {
    id: 1,
    receipt_id: 1,
    product_id: 1, // ASUS Monitor ProArt
    batch_id: 1,
    location_id: null, // Chưa putaway
    expected_qty: 100.00,
    actual_qty: 100.00,
    qc_passed_qty: 80.00,
    qc_failed_qty: 20.00,
    qc_result: "PARTIAL",
    qc_failure_reason: "20 cái móp méo vỏ bọc",
    unit_cost: 8500000.00,
    quarantine_status: "PENDING" // PENDING, RTV_COMPLETED, DISPOSED
  },
  {
    id: 2,
    receipt_id: 2,
    product_id: 2, // Logitech MX Master 3S
    batch_id: null,
    location_id: null,
    expected_qty: 200.00,
    actual_qty: 198.00,
    qc_passed_qty: 198.00,
    qc_failed_qty: 0.00,
    qc_result: "PASSED",
    qc_failure_reason: null,
    unit_cost: 2100000.00,
    quarantine_status: "PENDING"
  },
  {
    id: 3,
    receipt_id: 3,
    product_id: 1,
    batch_id: null,
    location_id: null,
    expected_qty: 10.00,
    actual_qty: null,
    qc_passed_qty: null,
    qc_failed_qty: null,
    qc_result: "PENDING",
    qc_failure_reason: null,
    unit_cost: 8500000.00,
    quarantine_status: "PENDING"
  },
  {
    id: 4,
    receipt_id: 4,
    product_id: 2,
    batch_id: null,
    location_id: null,
    expected_qty: 50.00,
    actual_qty: 50.00,
    qc_passed_qty: null,
    qc_failed_qty: null,
    qc_result: "PENDING",
    qc_failure_reason: null,
    unit_cost: 2100000.00,
    quarantine_status: "PENDING"
  },
  {
    id: 5,
    receipt_id: 5,
    product_id: 1, // ASUS Monitor
    batch_id: null,
    location_id: null,
    expected_qty: 50.00,
    actual_qty: null,
    qc_passed_qty: null,
    qc_failed_qty: null,
    qc_result: "PENDING",
    qc_failure_reason: null,
    unit_cost: 8500000.00,
    quarantine_status: "PENDING"
  },
  {
    id: 6,
    receipt_id: 6,
    product_id: 2, // Logitech Mouse
    batch_id: 3,
    location_id: null,
    expected_qty: 120.00,
    actual_qty: 120.00,
    qc_passed_qty: 120.00,
    qc_failed_qty: 0.00,
    qc_result: "PASSED",
    qc_failure_reason: null,
    unit_cost: 2100000.00,
    quarantine_status: "PENDING"
  },
  {
    id: 7,
    receipt_id: 7,
    product_id: 1, // ASUS Monitor
    batch_id: null,
    location_id: null,
    expected_qty: 30.00,
    actual_qty: 30.00,
    qc_passed_qty: null,
    qc_failed_qty: null,
    qc_result: "PENDING",
    qc_failure_reason: null,
    unit_cost: 8500000.00,
    quarantine_status: "PENDING"
  }
];


const INITIAL_BATCHES = [
  {
    id: 1,
    batch_number: "BT-RC-20260601-0001-SKU-PA-001-A",
    product_id: 1,
    warehouse_id: 1,
    received_date: "2026-06-01",
    quantity: 80.00,
    created_at: "2026-06-01T15:30:00Z"
  },
  {
    id: 2,
    batch_number: "BT-RC-20260601-0001-SKU-PA-001-C",
    product_id: 1,
    warehouse_id: 1,
    received_date: "2026-06-01",
    quantity: 20.00,
    created_at: "2026-06-01T15:30:00Z"
  },
  {
    id: 3,
    batch_number: "BT-RC-20260603-0004-SKU-LOGI-MX3-A",
    product_id: 2,
    warehouse_id: 3,
    received_date: "2026-06-03",
    quantity: 120.00,
    created_at: "2026-06-03T11:20:00Z"
  }
];

// Helper functions for LocalStorage mock database
const getDb = (key, initial) => {
  const data = localStorage.getItem(key);
  if (!data) {
    localStorage.setItem(key, JSON.stringify(initial));
    return initial;
  }
  try {
    return JSON.parse(data);
  } catch (e) {
    localStorage.setItem(key, JSON.stringify(initial));
    return initial;
  }
};

const saveDb = (key, data) => {
  localStorage.setItem(key, JSON.stringify(data));
};

const addMockAuditLog = (action, entityType, entityId, details) => {
  const logs = JSON.parse(localStorage.getItem('wms_audit_logs')) || [];
  const currentUser = JSON.parse(localStorage.getItem('wms_user')) || { fullName: 'System' };
  const newLog = {
    id: logs.length + 1,
    actorName: currentUser.fullName,
    action,
    entityType,
    entityId,
    details,
    createdAt: new Date().toISOString()
  };
  localStorage.setItem('wms_audit_logs', JSON.stringify([newLog, ...logs]));
};

// Auto-seed HCM mock data if LocalStorage is already initialized
if (typeof window !== 'undefined' && window.localStorage) {
  try {
    const storedReceipts = localStorage.getItem(KEYS.RECEIPTS);
    if (storedReceipts) {
      const receipts = JSON.parse(storedReceipts);
      const hasHcm = receipts.some(r => r.warehouse_id === 3);
      if (!hasHcm) {
        const hcmReceipts = [
          {
            id: 5,
            receipt_number: "RC-20260603-0003",
            source_order_code: "PO-ASUS-004",
            type: "PURCHASE",
            warehouse_id: 3,
            supplier_id: 1,
            dealer_id: null,
            contact_person: "Lê Giao Nhận HCM",
            source_channel: "Zalo",
            status: "PENDING_RECEIPT",
            approved_by: null,
            approved_at: null,
            rejection_reason: null,
            document_date: "2026-06-03",
            accounting_period_id: 1,
            created_by: 7,
            notes: "Lệnh nhập Asus cho chi nhánh HCM",
            created_at: "2026-06-03T08:30:00Z",
            updated_at: "2026-06-03T08:30:00Z"
          },
          {
            id: 6,
            receipt_number: "RC-20260603-0004",
            source_order_code: "PO-LOGI-001",
            type: "PURCHASE",
            warehouse_id: 3,
            supplier_id: 1,
            dealer_id: null,
            contact_person: "Lê Giao Nhận HCM",
            source_channel: "Email",
            status: "APPROVED",
            approved_by: 3,
            approved_at: "2026-06-03T11:20:00Z",
            rejection_reason: null,
            document_date: "2026-06-03",
            accounting_period_id: 1,
            created_by: 7,
            notes: "Lệnh nhập chuột Logitech HCM đã duyệt cất kho",
            created_at: "2026-06-03T09:15:00Z",
            updated_at: "2026-06-03T11:20:00Z"
          },
          {
            id: 7,
            receipt_number: "RC-20260603-0005",
            source_order_code: "PO-ASUS-005",
            type: "PURCHASE",
            warehouse_id: 3,
            supplier_id: 1,
            dealer_id: null,
            contact_person: "Trần Văn Giao",
            source_channel: "Phone",
            status: "DRAFT",
            approved_by: null,
            approved_at: null,
            rejection_reason: null,
            document_date: "2026-06-03",
            accounting_period_id: 1,
            created_by: 7,
            notes: "Lệnh nhập chờ kiểm QC HCM",
            created_at: "2026-06-03T10:00:00Z",
            updated_at: "2026-06-03T10:00:00Z"
          }
        ];
        localStorage.setItem(KEYS.RECEIPTS, JSON.stringify([...receipts, ...hcmReceipts]));
        
        const storedItems = localStorage.getItem(KEYS.RECEIPT_ITEMS);
        if (storedItems) {
          const items = JSON.parse(storedItems);
          const hcmItems = [
            {
              id: 5,
              receipt_id: 5,
              product_id: 1,
              batch_id: null,
              location_id: null,
              expected_qty: 50.00,
              actual_qty: null,
              qc_passed_qty: null,
              qc_failed_qty: null,
              qc_result: "PENDING",
              qc_failure_reason: null,
              unit_cost: 8500000.00,
              quarantine_status: "PENDING"
            },
            {
              id: 6,
              receipt_id: 6,
              product_id: 2,
              batch_id: 3,
              location_id: null,
              expected_qty: 120.00,
              actual_qty: 120.00,
              qc_passed_qty: 120.00,
              qc_failed_qty: 0.00,
              qc_result: "PASSED",
              qc_failure_reason: null,
              unit_cost: 2100000.00,
              quarantine_status: "PENDING"
            },
            {
              id: 7,
              receipt_id: 7,
              product_id: 1,
              batch_id: null,
              location_id: null,
              expected_qty: 30.00,
              actual_qty: 30.00,
              qc_passed_qty: null,
              qc_failed_qty: null,
              qc_result: "PENDING",
              qc_failure_reason: null,
              unit_cost: 8500000.00,
              quarantine_status: "PENDING"
            }
          ];
          localStorage.setItem(KEYS.RECEIPT_ITEMS, JSON.stringify([...items, ...hcmItems]));
        }

        const storedBatches = localStorage.getItem(KEYS.BATCHES);
        if (storedBatches) {
          const batches = JSON.parse(storedBatches);
          const hcmBatch = {
            id: 3,
            batch_number: "BT-RC-20260603-0004-SKU-LOGI-MX3-A",
            product_id: 2,
            warehouse_id: 3,
            received_date: "2026-06-03",
            quantity: 120.00,
            created_at: "2026-06-03T11:20:00Z"
          };
          localStorage.setItem(KEYS.BATCHES, JSON.stringify([...batches, hcmBatch]));
        }
      }
    }
  } catch (e) {
    console.error('Error seeding missing HCM mock data', e);
  }
}

export const inboundService = {

  // --- RECEIPTS ---
  getReceipts: async (warehouseId) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      const receipts = getDb(KEYS.RECEIPTS, INITIAL_RECEIPTS);
      if (warehouseId) {
        return receipts.filter(r => r.warehouse_id === Number(warehouseId));
      }
      return receipts;
    }
    const url = warehouseId ? `/receipts?warehouseId=${warehouseId}` : '/receipts';
    const response = await apiClient.get(url);
    return response.data;
  },

  getReceiptById: async (id) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 200));
      const receipts = getDb(KEYS.RECEIPTS, INITIAL_RECEIPTS);
      const receipt = receipts.find(r => r.id === Number(id));
      if (!receipt) throw new Error('RECEIPT_NOT_FOUND');

      const items = getDb(KEYS.RECEIPT_ITEMS, INITIAL_RECEIPT_ITEMS);
      const receiptItems = items.filter(item => item.receipt_id === Number(id)).map(item => ({
        ...item,
        receipt_item_id: item.id
      }));

      return {
        ...receipt,
        items: receiptItems
      };
    }
    const response = await apiClient.get(`/receipts/${id}`);
    return response.data;
  },

  createReceipt: async (receiptData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const receipts = getDb(KEYS.RECEIPTS, INITIAL_RECEIPTS);
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, INITIAL_RECEIPT_ITEMS);

      // Generate receipt number RC-YYYYMMDD-XXXX
      const today = new Date();
      const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '');
      const countToday = receipts.filter(r => r.receipt_number.startsWith(`RC-${dateStr}`)).length + 1;
      const receiptNumber = `RC-${dateStr}-${String(countToday).padStart(4, '0')}`;

      const currentUser = JSON.parse(localStorage.getItem('wms_user')) || { id: 7 };

      const newReceipt = {
        id: receipts.length > 0 ? Math.max(...receipts.map(r => r.id)) + 1 : 1,
        receipt_number: receiptNumber,
        source_order_code: receiptData.source_order_code || '',
        type: receiptData.type,
        warehouse_id: Number(receiptData.warehouse_id),
        supplier_id: receiptData.supplier_id ? Number(receiptData.supplier_id) : null,
        dealer_id: receiptData.dealer_id ? Number(receiptData.dealer_id) : null,
        contact_person: receiptData.contact_person || '',
        source_channel: receiptData.source_channel || 'Zalo',
        status: 'PENDING_RECEIPT',
        approved_by: null,
        approved_at: null,
        rejection_reason: null,
        document_date: receiptData.document_date || today.toISOString().slice(0, 10),
        accounting_period_id: 1,
        created_by: currentUser.id,
        notes: receiptData.notes || '',
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      };

      // Add items
      const newItems = receiptData.items.map((item, index) => ({
        id: receiptItems.length > 0 ? Math.max(...receiptItems.map(ri => ri.id)) + index + 1 : index + 1,
        receipt_id: newReceipt.id,
        product_id: Number(item.product_id),
        batch_id: null,
        location_id: null,
        expected_qty: parseFloat(item.expected_qty),
        actual_qty: null,
        qc_passed_qty: null,
        qc_failed_qty: null,
        qc_result: 'PENDING',
        qc_failure_reason: null,
        unit_cost: parseFloat(item.unit_cost) || 0.0,
        quarantine_status: 'PENDING'
      }));

      receipts.push(newReceipt);
      receiptItems.push(...newItems);

      saveDb(KEYS.RECEIPTS, receipts);
      saveDb(KEYS.RECEIPT_ITEMS, receiptItems);

      addMockAuditLog('RECEIPT_DRAFTED', 'Receipt', newReceipt.id, `Lập lệnh nhập kho thô: ${newReceipt.receipt_number}`);
      return newReceipt;
    }
    const response = await apiClient.post('/receipts', receiptData);
    return response.data;
  },

  receiveReceipt: async (id, receiveData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const receipts = getDb(KEYS.RECEIPTS, INITIAL_RECEIPTS);
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, INITIAL_RECEIPT_ITEMS);

      const rIdx = receipts.findIndex(r => r.id === Number(id));
      if (rIdx === -1) throw new Error('RECEIPT_NOT_FOUND');
      if (receipts[rIdx].status !== 'PENDING_RECEIPT') throw new Error('RECEIPT_ALREADY_PROCESSED');

      // Update actual counts
      receiveData.items.forEach(updateItem => {
        const riIdx = receiptItems.findIndex(ri => ri.id === Number(updateItem.receipt_item_id));
        if (riIdx !== -1) {
          const counted = updateItem.counted_qty !== undefined ? updateItem.counted_qty : updateItem.actual_qty;
          receiptItems[riIdx].actual_qty = parseFloat(counted);        }
      });

      receipts[rIdx].status = 'DRAFT';
      receipts[rIdx].updated_at = new Date().toISOString();

      saveDb(KEYS.RECEIPTS, receipts);
      saveDb(KEYS.RECEIPT_ITEMS, receiptItems);

      addMockAuditLog('RECEIPT_RECEIVED', 'Receipt', id, `Xác nhận kiểm đếm thực tế cho phiếu: ${receipts[rIdx].receipt_number}`);
      return receipts[rIdx];
    }
    const response = await apiClient.put(`/receipts/${id}/receive`, receiveData);
    return response.data;
  },

  qcReceipt: async (id, qcData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const receipts = getDb(KEYS.RECEIPTS, INITIAL_RECEIPTS);
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, INITIAL_RECEIPT_ITEMS);

      const rIdx = receipts.findIndex(r => r.id === Number(id));
      if (rIdx === -1) throw new Error('RECEIPT_NOT_FOUND');
      if (receipts[rIdx].status !== 'DRAFT') throw new Error('RECEIPT_NOT_READY_FOR_QC');

      if (qcData.action === 'CONFIRM') {
        const items = receiptItems.filter(item => item.receipt_id === Number(id));
        const hasPending = items.some(item => !item.qc_result || item.qc_result === 'PENDING');
        if (hasPending) throw new Error('QC_NOT_YET_SUBMITTED');

        const hasFailed = items.some(item => item.qc_result === 'FAILED');
        receipts[rIdx].status = hasFailed ? 'QC_FAILED' : 'QC_COMPLETED';
        receipts[rIdx].updated_at = new Date().toISOString();
        saveDb(KEYS.RECEIPTS, receipts);
        addMockAuditLog('RECEIPT_QC_CONFIRM', 'Receipt', id, `Xác nhận QC cho phiếu: ${receipts[rIdx].receipt_number}`);
        return receipts[rIdx];
      }

      if (qcData.items) {
        qcData.items.forEach(updateItem => {
          const riIdx = receiptItems.findIndex(ri => ri.id === Number(updateItem.receipt_item_id));
          if (riIdx !== -1) {
            const item = receiptItems[riIdx];
            const passed = parseFloat(updateItem.qc_passed_qty);
            const failed = parseFloat(updateItem.qc_failed_qty);

            if (passed + failed !== item.actual_qty) {
              throw new Error('QC_PASSED_FAILED_MISMATCH');
            }

            item.qc_passed_qty = passed;
            item.qc_failed_qty = failed;
            item.qc_failure_reason = updateItem.qc_failure_reason || null;
            item.qc_result = failed === 0 ? 'PASSED' : 'FAILED';
          }
        });
        saveDb(KEYS.RECEIPT_ITEMS, receiptItems);
      }

      receipts[rIdx].updated_at = new Date().toISOString();
      saveDb(KEYS.RECEIPTS, receipts);

      addMockAuditLog('RECEIPT_QC_SUBMIT', 'Receipt', id, `Ghi nhận kết quả QC cho phiếu: ${receipts[rIdx].receipt_number}`);
      return receipts[rIdx];
    }
    const response = await apiClient.put(`/receipts/${id}/qc`, qcData);
    return response.data;
  },

  approveReceipt: async (id, notes, version) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 600));
      const receipts = getDb(KEYS.RECEIPTS, INITIAL_RECEIPTS);
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, INITIAL_RECEIPT_ITEMS);
      const batches = getDb(KEYS.BATCHES, INITIAL_BATCHES);

      const rIdx = receipts.findIndex(r => r.id === Number(id));
      if (rIdx === -1) throw new Error('RECEIPT_NOT_FOUND');
      if (receipts[rIdx].status !== 'QC_COMPLETED') throw new Error('RECEIPT_NOT_READY_FOR_APPROVAL');

      const receipt = receipts[rIdx];
      const currentUser = JSON.parse(localStorage.getItem('wms_user')) || { id: 3 };

      const items = receiptItems.filter(ri => ri.receipt_id === receipt.id);
      const products = await masterDataService.getProducts();

      // Create Batches
      items.forEach(item => {
        const prod = products.find(p => p.id === item.product_id) || { sku: 'PROD' };

        // 1. Batch for Passed
        if (item.qc_passed_qty > 0) {
          const batchNumber = `BT-${receipt.receipt_number}-${prod.sku}`;
          const bIdx = batches.findIndex(b => b.batch_number === batchNumber);
          if (bIdx === -1) {
            const newBatch = {
              id: batches.length > 0 ? Math.max(...batches.map(b => b.id)) + 1 : 1,
              batch_number: batchNumber,
              product_id: item.product_id,
              warehouse_id: receipt.warehouse_id,
              received_date: new Date().toISOString().slice(0, 10),
              quantity: item.qc_passed_qty,
              created_at: new Date().toISOString()
            };
            batches.push(newBatch);
            item.batch_id = newBatch.id;
          } else {
            batches[bIdx].quantity += item.qc_passed_qty;
            item.batch_id = batches[bIdx].id;
          }
        }

        // 2. Batch for Failed (Force FAILED, Quarantine)
        if (item.qc_failed_qty > 0) {
          const failBatchNumber = `BT-${receipt.receipt_number}-${prod.sku}-FAILED`;
          const bIdx = batches.findIndex(b => b.batch_number === failBatchNumber);
          
          let failBatchId;
          if (bIdx === -1) {
            const newBatch = {
              id: batches.length > 0 ? Math.max(...batches.map(b => b.id)) + 1 : 1,
              batch_number: failBatchNumber,
              product_id: item.product_id,
              warehouse_id: receipt.warehouse_id,
              received_date: new Date().toISOString().slice(0, 10),
              quantity: item.qc_failed_qty,
              created_at: new Date().toISOString()
            };
            batches.push(newBatch);
            failBatchId = newBatch.id;
          } else {
            batches[bIdx].quantity += item.qc_failed_qty;
            failBatchId = batches[bIdx].id;
          }

          // Automatically route quarantine items to quarantine zone location
          // Find quarantine location in this warehouse
          const locations = getDb('wms_db_warehouse_locations', []);
          const qLoc = locations.find(l => l.warehouse_id === receipt.warehouse_id && l.is_quarantine === true);
          
          if (qLoc) {
            // Add failed stock to inventories immediately at quarantine location
            const inventories = getDb('wms_db_inventories', []);
            const invIdx = inventories.findIndex(inv => 
              inv.warehouse_id === receipt.warehouse_id &&
              inv.product_id === item.product_id &&
              inv.batch_id === failBatchId &&
              inv.location_id === qLoc.id
            );

            if (invIdx === -1) {
              inventories.push({
                id: inventories.length > 0 ? Math.max(...inventories.map(inv => inv.id)) + 1 : 1,
                warehouse_id: receipt.warehouse_id,
                product_id: item.product_id,
                batch_id: failBatchId,
                location_id: qLoc.id,
                total_qty: item.qc_failed_qty,
                reserved_qty: 0,
                cost_price: item.unit_cost,
                version: 0,
                updated_at: new Date().toISOString()
              });
            } else {
              inventories[invIdx].total_qty += item.qc_failed_qty;
              inventories[invIdx].updated_at = new Date().toISOString();
              inventories[invIdx].version += 1;
            }
            saveDb('wms_db_inventories', inventories);

            // Re-calc bin capacity
            const vol = qLoc.current_volume_m3 + (item.qc_failed_qty * (prod.volume_m3 || 0));
            const wt = qLoc.current_weight_kg + (item.qc_failed_qty * (prod.weight_kg || 0));
            qLoc.current_volume_m3 = parseFloat(vol.toFixed(3));
            qLoc.current_weight_kg = parseFloat(wt.toFixed(2));
            
            const allLocations = getDb('wms_db_warehouse_locations', []);
            const locIdx = allLocations.findIndex(l => l.id === qLoc.id);
            if (locIdx !== -1) {
              allLocations[locIdx] = qLoc;
              saveDb('wms_db_warehouse_locations', allLocations);
            }
          }
        }
      });

      receipts[rIdx].status = 'APPROVED';
      receipts[rIdx].approved_by = currentUser.id;
      receipts[rIdx].approved_at = new Date().toISOString();
      receipts[rIdx].notes = notes || receipts[rIdx].notes;
      receipts[rIdx].updated_at = new Date().toISOString();

      saveDb(KEYS.RECEIPTS, receipts);
      saveDb(KEYS.RECEIPT_ITEMS, receiptItems);
      saveDb(KEYS.BATCHES, batches);

      addMockAuditLog('RECEIPT_APPROVED', 'Receipt', id, `Ký duyệt nhập kho chính thức: ${receipts[rIdx].receipt_number}`);
      return receipts[rIdx];
    }
    const response = await apiClient.put(`/receipts/${id}/approve`, { expectedVersion: version, reason: notes });
    return response.data;
  },

  rejectReceipt: async (id, reason, version) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const receipts = getDb(KEYS.RECEIPTS, INITIAL_RECEIPTS);

      const rIdx = receipts.findIndex(r => r.id === Number(id));
      if (rIdx === -1) throw new Error('RECEIPT_NOT_FOUND');
      if (receipts[rIdx].status !== 'QC_COMPLETED') throw new Error('RECEIPT_NOT_READY_FOR_REJECTION');

      receipts[rIdx].status = 'REJECTED';
      receipts[rIdx].rejection_reason = reason;
      receipts[rIdx].updated_at = new Date().toISOString();

      saveDb(KEYS.RECEIPTS, receipts);

      addMockAuditLog('RECEIPT_REJECTED', 'Receipt', id, `Từ chối phê duyệt nhập kho phiếu: ${receipts[rIdx].receipt_number}. Lý do: ${reason}`);
      return receipts[rIdx];
    }
    const response = await apiClient.put(`/receipts/${id}/reject`, { expectedVersion: version, reason: reason });
    return response.data;
  },

  putawayReceipt: async (id, putawayData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 600));
      const receipts = getDb(KEYS.RECEIPTS, INITIAL_RECEIPTS);
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, INITIAL_RECEIPT_ITEMS);
      const inventories = getDb('wms_db_inventories', []);
      const locations = getDb('wms_db_warehouse_locations', []);
      const products = await masterDataService.getProducts();

      const rIdx = receipts.findIndex(r => r.id === Number(id));
      if (rIdx === -1) throw new Error('RECEIPT_NOT_FOUND');
      if (receipts[rIdx].status !== 'APPROVED') throw new Error('RECEIPT_NOT_APPROVED_FOR_PUTAWAY');

      // Update location and inventories
      for (const updateItem of putawayData.items) {
        const riIdx = receiptItems.findIndex(ri => ri.id === Number(updateItem.receipt_item_id));
        if (riIdx !== -1) {
          const item = receiptItems[riIdx];
          const locId = Number(updateItem.location_id);
          
          // Verify capacity
          const loc = locations.find(l => l.id === locId);
          if (!loc) throw new Error('BIN_NOT_FOUND');

          const prod = products.find(p => p.id === item.product_id) || { volume_m3: 0.1, weight_kg: 10 };
          const incomingVolume = item.qc_passed_qty * (prod.volume_m3 || 0);
          const incomingWeight = item.qc_passed_qty * (prod.weight_kg || 0);

          if (loc.current_volume_m3 + incomingVolume > loc.capacity_m3 ||
              loc.current_weight_kg + incomingWeight > loc.capacity_kg) {
            throw new Error('BIN_CAPACITY_EXCEEDED');
          }

          item.location_id = locId;

          // Increase inventories
          const invIdx = inventories.findIndex(inv => 
            inv.warehouse_id === receipts[rIdx].warehouse_id &&
            inv.product_id === item.product_id &&
            inv.batch_id === item.batch_id &&
            inv.location_id === locId
          );

          if (invIdx === -1) {
            inventories.push({
              id: inventories.length > 0 ? Math.max(...inventories.map(inv => inv.id)) + 1 : 1,
              warehouse_id: receipts[rIdx].warehouse_id,
              product_id: item.product_id,
              batch_id: item.batch_id,
              location_id: locId,
              total_qty: item.qc_passed_qty,
              reserved_qty: 0,
              cost_price: item.unit_cost,
              version: 0,
              updated_at: new Date().toISOString()
            });
          } else {
            inventories[invIdx].total_qty += item.qc_passed_qty;
            inventories[invIdx].updated_at = new Date().toISOString();
            inventories[invIdx].version += 1;
          }

          // Update bin capacity locally
          loc.current_volume_m3 = parseFloat((loc.current_volume_m3 + incomingVolume).toFixed(3));
          loc.current_weight_kg = parseFloat((loc.current_weight_kg + incomingWeight).toFixed(2));
          
          const locIdx = locations.findIndex(l => l.id === loc.id);
          if (locIdx !== -1) locations[locIdx] = loc;
        }
      }

      // If all items of the receipt are putaway, we can mark putaway completed or simply update notes
      saveDb(KEYS.RECEIPT_ITEMS, receiptItems);
      saveDb('wms_db_inventories', inventories);
      saveDb('wms_db_warehouse_locations', locations);

      addMockAuditLog('RECEIPT_PUTAWAY_COMPLETED', 'Receipt', id, `Hoàn thành cất hàng (Putaway) cho phiếu: ${receipts[rIdx].receipt_number}`);
      return receipts[rIdx];
    }
    // For real backend: convert snake_case payload to camelCase
    const apiPayload = {
      expectedVersion: putawayData.expectedVersion !== undefined ? putawayData.expectedVersion : putawayData.expected_version,
      items: (putawayData.items || []).map(item => ({
        receiptItemId: item.receiptItemId !== undefined ? item.receiptItemId : item.receipt_item_id,
        locationId: item.locationId !== undefined ? item.locationId : item.location_id
      }))
    };
    const response = await apiClient.put(`/receipts/${id}/complete`, apiPayload);
    return response.data;
  },

  // --- QUARANTINE HANDLING ---
  getQuarantineItems: async (warehouseId) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      const receipts = getDb(KEYS.RECEIPTS, INITIAL_RECEIPTS);
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, INITIAL_RECEIPT_ITEMS);
      const products = await masterDataService.getProducts();

      // Find approved receipts belonging to the warehouse
      const whReceipts = receipts.filter(r => r.warehouse_id === Number(warehouseId) && r.status === 'APPROVED');
      const whReceiptIds = whReceipts.map(r => r.id);

      // Find failed items in those receipts
      const failedItems = receiptItems.filter(item => 
        whReceiptIds.includes(item.receipt_id) && 
        item.qc_failed_qty > 0 && 
        item.quarantine_status === 'PENDING'
      );

      // Map details
      return failedItems.map(item => {
        const rc = whReceipts.find(r => r.id === item.receipt_id);
        const prod = products.find(p => p.id === item.product_id);
        return {
          ...item,
          receipt_item_id: item.id,
          receipt_number: rc.receipt_number,
          supplier_id: rc.supplier_id,
          product_name: prod ? prod.name : 'Unknown Product',
          product_sku: prod ? prod.sku : 'Unknown SKU',
          total_value: item.qc_failed_qty * item.unit_cost
        };
      });
    }
    const response = await apiClient.get(`/quarantine/items?warehouseId=${warehouseId}`);
    return response.data;
  },

  handleRtv: async (receiptId, receiptVersion, notes) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, INITIAL_RECEIPT_ITEMS);
      const receipts = getDb(KEYS.RECEIPTS, INITIAL_RECEIPTS);
      const debitNotes = getDb(KEYS.DEBIT_NOTES, []);
      const adjustments = getDb(KEYS.ADJUSTMENTS, []);
      const inventories = getDb('wms_db_inventories', []);
      const locations = getDb('wms_db_warehouse_locations', []);
      const products = await masterDataService.getProducts();

      const receipt = receipts.find(r => r.id === Number(receiptId));
      if (!receipt) throw new Error('RECEIPT_NOT_FOUND');

      const items = receiptItems.filter(ri => ri.receipt_id === receipt.id);
      const pendingItems = items.filter(ri => ri.quarantine_status === 'PENDING');

      pendingItems.forEach(item => {
        item.quarantine_status = 'RTV_COMPLETED';
      });

      const today = new Date();
      const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '');
      const countDn = debitNotes.length + 1;
      const dnNumber = `DN-${dateStr}-${String(countDn).padStart(4, '0')}`;
      
      const currentUser = JSON.parse(localStorage.getItem('wms_user')) || { id: 3 };

      const totalFailedQty = items.reduce((sum, ri) => sum + (ri.qc_failed_qty || 0), 0);
      const totalAmount = items.reduce((sum, ri) => sum + ((ri.qc_failed_qty || 0) * (ri.unit_cost || 0)), 0);

      const newDn = {
        id: debitNotes.length + 1,
        debit_note_number: dnNumber,
        supplier_id: receipt.supplier_id || 1,
        receipt_id: receipt.id,
        failed_qty: totalFailedQty,
        amount: totalAmount,
        reason: notes,
        created_by: currentUser.id,
        document_date: today.toISOString().slice(0, 10),
        accounting_period_id: 1,
        created_at: new Date().toISOString()
      };
      debitNotes.push(newDn);

      // Create Adjustment
      const countAdj = adjustments.length + 1;
      const adjNumber = `ADJ-RTV-${dateStr}-${String(countAdj).padStart(4, '0')}`;

      const newAdj = {
        id: adjustments.length + 1,
        adjustment_number: adjNumber,
        warehouse_id: receipt.warehouse_id,
        product_id: items[0]?.product_id || 1,
        batch_id: items[0]?.batch_id || 2,
        location_id: 102,
        quantity_adjustment: -totalFailedQty,
        type: 'RETURN_TO_VENDOR',
        reference_id: newDn.id,
        reference_type: 'debit_notes',
        reason: notes,
        approved_by: currentUser.id,
        approved_at: new Date().toISOString(),
        document_date: today.toISOString().slice(0, 10),
        accounting_period_id: 1,
        created_by: currentUser.id,
        created_at: new Date().toISOString()
      };
      adjustments.push(newAdj);

      // Decrease quarantine inventory
      const qLoc = locations.find(l => l.warehouse_id === receipt.warehouse_id && l.is_quarantine === true);
      if (qLoc) {
        items.forEach(item => {
          const qty = item.qc_failed_qty || 0;
          if (qty <= 0) return;
          const prod = products.find(p => p.id === item.product_id);
          const batches = getDb(KEYS.BATCHES, INITIAL_BATCHES);
          const failBatch = batches.find(b => b.product_id === item.product_id && b.warehouse_id === receipt.warehouse_id && b.batch_number.endsWith('-FAILED'));
          const failBatchId = failBatch ? failBatch.id : 2;

          const invIdx = inventories.findIndex(inv =>
            inv.warehouse_id === receipt.warehouse_id &&
            inv.product_id === item.product_id &&
            inv.batch_id === failBatchId &&
            inv.location_id === qLoc.id
          );

          if (invIdx !== -1) {
            inventories[invIdx].total_qty = Math.max(0, inventories[invIdx].total_qty - qty);
            inventories[invIdx].updated_at = new Date().toISOString();
            inventories[invIdx].version += 1;
          }

          const vol = Math.max(0, qLoc.current_volume_m3 - (qty * (prod?.volume_m3 || 0)));
          const wt = Math.max(0, qLoc.current_weight_kg - (qty * (prod?.weight_kg || 0)));
          qLoc.current_volume_m3 = parseFloat(vol.toFixed(3));
          qLoc.current_weight_kg = parseFloat(wt.toFixed(2));
        });
        
        const locIdx = locations.findIndex(l => l.id === qLoc.id);
        if (locIdx !== -1) locations[locIdx] = qLoc;
      }

      saveDb(KEYS.RECEIPT_ITEMS, receiptItems);
      saveDb(KEYS.DEBIT_NOTES, debitNotes);
      saveDb(KEYS.ADJUSTMENTS, adjustments);
      saveDb('wms_db_inventories', inventories);
      saveDb('wms_db_warehouse_locations', locations);

      addMockAuditLog('QUARANTINE_RTV', 'Receipt', receiptId, `Đã xuất trả hàng lỗi NCC (RTV) cho phiếu nhập ${receipt.receipt_number}. Đã sinh Debit Note: ${dnNumber}`);
      return newDn;
    }
    const response = await apiClient.post(`/receipts/${receiptId}/rtv`, {
      expectedVersion: receiptVersion,
      reason: notes
    });
    return response.data;
  },

  handleDisposal: async (receiptItemId, cause, imageUrl) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, INITIAL_RECEIPT_ITEMS);
      const receipts = getDb(KEYS.RECEIPTS, INITIAL_RECEIPTS);
      const damageReports = getDb('wms_db_damage_reports', []);
      const adjustments = getDb(KEYS.ADJUSTMENTS, []);
      const products = await masterDataService.getProducts();

      const riIdx = receiptItems.findIndex(ri => ri.id === Number(receiptItemId));
      if (riIdx === -1) throw new Error('ITEM_NOT_FOUND');
      
      const item = receiptItems[riIdx];
      const receipt = receipts.find(r => r.id === item.receipt_id);
      if (!receipt) throw new Error('RECEIPT_NOT_FOUND');

      const prod = products.find(p => p.id === item.product_id);
      const totalValue = item.qc_failed_qty * item.unit_cost;

      // 1. Create Damage Report
      const today = new Date();
      const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '');
      const countDr = damageReports.length + 1;
      const drNumber = `DR-${dateStr}-${String(countDr).padStart(4, '0')}`;
      
      const currentUser = JSON.parse(localStorage.getItem('wms_user')) || { id: 3 };

      const newDr = {
        id: damageReports.length + 1,
        report_number: drNumber,
        warehouse_id: receipt.warehouse_id,
        product_id: item.product_id,
        batch_id: item.batch_id || 2,
        quantity: item.qc_failed_qty,
        cause: cause || 'Hư hỏng không thể phục hồi',
        image_url: imageUrl || '',
        reported_by: currentUser.id,
        report_date: today.toISOString().slice(0, 10),
        created_at: new Date().toISOString()
      };
      damageReports.push(newDr);

      // 2. Create Adjustment
      const countAdj = adjustments.length + 1;
      const adjNumber = `ADJ-DIS-${dateStr}-${String(countAdj).padStart(4, '0')}`;

      // Calculate auto-approval
      const autoApproved = totalValue < 5000000.00; // < 5M VND

      const newAdj = {
        id: adjustments.length + 1,
        adjustment_number: adjNumber,
        warehouse_id: receipt.warehouse_id,
        product_id: item.product_id,
        batch_id: item.batch_id || 2,
        location_id: 102,
        quantity_adjustment: -item.qc_failed_qty,
        type: 'DISPOSAL',
        reference_id: newDr.id,
        reference_type: 'damage_reports',
        reason: cause || 'Tiêu hủy hàng lỗi cách ly',
        approved_by: autoApproved ? currentUser.id : null,
        approved_at: autoApproved ? new Date().toISOString() : null,
        document_date: today.toISOString().slice(0, 10),
        accounting_period_id: 1,
        created_by: currentUser.id,
        created_at: new Date().toISOString()
      };
      adjustments.push(newAdj);

      if (autoApproved) {
        // Auto-approve, decrease quarantine inventory
        item.quarantine_status = 'DISPOSED';
        
        const inventories = getDb('wms_db_inventories', []);
        const locations = getDb('wms_db_warehouse_locations', []);
        const qLoc = locations.find(l => l.warehouse_id === receipt.warehouse_id && l.is_quarantine === true);
        
        if (qLoc) {
          const invIdx = inventories.findIndex(inv => 
            inv.warehouse_id === receipt.warehouse_id &&
            inv.product_id === item.product_id &&
            inv.location_id === qLoc.id
          );

          if (invIdx !== -1) {
            inventories[invIdx].total_qty = Math.max(0, inventories[invIdx].total_qty - item.qc_failed_qty);
            inventories[invIdx].updated_at = new Date().toISOString();
            inventories[invIdx].version += 1;
          }

          // Re-calc capacity
          const vol = Math.max(0, qLoc.current_volume_m3 - (item.qc_failed_qty * (prod?.volume_m3 || 0)));
          const wt = Math.max(0, qLoc.current_weight_kg - (item.qc_failed_qty * (prod?.weight_kg || 0)));
          qLoc.current_volume_m3 = parseFloat(vol.toFixed(3));
          qLoc.current_weight_kg = parseFloat(wt.toFixed(2));
          
          const locIdx = locations.findIndex(l => l.id === qLoc.id);
          if (locIdx !== -1) locations[locIdx] = qLoc;
        }

        saveDb('wms_db_inventories', inventories);
        saveDb('wms_db_warehouse_locations', locations);
        saveDb(KEYS.RECEIPT_ITEMS, receiptItems);
      }

      saveDb('wms_db_damage_reports', damageReports);
      saveDb(KEYS.ADJUSTMENTS, adjustments);

      const logMsg = autoApproved 
        ? `Đã tạo phiếu tiêu hủy tự động duyệt (Giá trị: ${totalValue.toLocaleString('vi-VN')} VND)` 
        : `Đã tạo yêu cầu tiêu hủy chờ phê duyệt (Giá trị: ${totalValue.toLocaleString('vi-VN')} VND)`;
      addMockAuditLog('QUARANTINE_DISPOSAL_REQUEST', 'ReceiptItem', receiptItemId, logMsg);
      
      return {
        adjustment: newAdj,
        autoApproved
      };
    }
    const response = await apiClient.post(`/receipts/${receiptItemId}/dispose`, { cause, image_url: imageUrl });
    return response.data;
  },

  handleDisposalFromQuarantine: async (quarantineRecordId, cause, imageUrl) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      return { autoApproved: true };
    }
    const response = await apiClient.post(`/quarantine/${quarantineRecordId}/dispose`, { cause, image_url: imageUrl });
    return response.data;
  },

  getPendingDisposals: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      const adjustments = getDb(KEYS.ADJUSTMENTS, []);
      const damageReports = getDb('wms_db_damage_reports', []);
      const products = await masterDataService.getProducts();
      
      const pendingDisposals = adjustments.filter(adj => adj.type === 'DISPOSAL' && adj.approved_at === null);
      
      return pendingDisposals.map(adj => {
        const dr = damageReports.find(d => d.id === adj.reference_id) || {};
        const prod = products.find(p => p.id === adj.product_id) || {};
        const failedQty = Math.abs(adj.quantity_adjustment);
        const costPrice = prod ? 8500000.00 : 0.0; // dummy fallback
        return {
          ...adj,
          cause: dr.cause || 'Không rõ',
          reported_by_name: 'Trưởng Kho Hải Phòng',
          product_sku: prod.sku || 'Unknown',
          product_name: prod.name || 'Unknown Product',
          failed_qty: failedQty,
          total_value: failedQty * costPrice
        };
      });
    }
    const response = await apiClient.get('/disposals/pending');
    return response.data;
  },

  approveDisposal: async (adjustmentId) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const adjustments = getDb(KEYS.ADJUSTMENTS, []);
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, INITIAL_RECEIPT_ITEMS);
      const inventories = getDb('wms_db_inventories', []);
      const locations = getDb('wms_db_warehouse_locations', []);
      const products = await masterDataService.getProducts();

      const adjIdx = adjustments.findIndex(adj => adj.id === Number(adjustmentId));
      if (adjIdx === -1) throw new Error('ADJUSTMENT_NOT_FOUND');

      const adj = adjustments[adjIdx];
      const failedQty = Math.abs(adj.quantity_adjustment);
      const prod = products.find(p => p.id === adj.product_id);

      const currentUser = JSON.parse(localStorage.getItem('wms_user')) || { id: 2 }; // CEO or Manager

      // 1. Update adjustment
      adj.approved_by = currentUser.id;
      adj.approved_at = new Date().toISOString();

      // 2. Find and update receipt item quarantine status
      // In mock DB, we assume there's a receipt item matched
      const riIdx = receiptItems.findIndex(ri => ri.product_id === adj.product_id && ri.qc_failed_qty === failedQty && ri.quarantine_status === 'PENDING');
      if (riIdx !== -1) {
        receiptItems[riIdx].quarantine_status = 'DISPOSED';
      }

      // 3. Decrease quarantine inventory
      const qLoc = locations.find(l => l.warehouse_id === adj.warehouse_id && l.is_quarantine === true);
      if (qLoc) {
        const invIdx = inventories.findIndex(inv => 
          inv.warehouse_id === adj.warehouse_id &&
          inv.product_id === adj.product_id &&
          inv.location_id === qLoc.id
        );

        if (invIdx !== -1) {
          inventories[invIdx].total_qty = Math.max(0, inventories[invIdx].total_qty - failedQty);
          inventories[invIdx].updated_at = new Date().toISOString();
          inventories[invIdx].version += 1;
        }

        // Re-calc capacity
        const vol = Math.max(0, qLoc.current_volume_m3 - (failedQty * (prod?.volume_m3 || 0)));
        const wt = Math.max(0, qLoc.current_weight_kg - (failedQty * (prod?.weight_kg || 0)));
        qLoc.current_volume_m3 = parseFloat(vol.toFixed(3));
        qLoc.current_weight_kg = parseFloat(wt.toFixed(2));
        
        const locIdx = locations.findIndex(l => l.id === qLoc.id);
        if (locIdx !== -1) locations[locIdx] = qLoc;
      }

      saveDb(KEYS.ADJUSTMENTS, adjustments);
      saveDb(KEYS.RECEIPT_ITEMS, receiptItems);
      saveDb('wms_db_inventories', inventories);
      saveDb('wms_db_warehouse_locations', locations);

      addMockAuditLog('QUARANTINE_DISPOSAL_APPROVED', 'Adjustment', adjustmentId, `Phê duyệt yêu cầu tiêu hủy: ${adj.adjustment_number} bởi ${currentUser.fullName}`);
      return adj;
    }
    const response = await apiClient.put(`/disposal/${adjustmentId}/approve`);
    return response.data;
  }
};
