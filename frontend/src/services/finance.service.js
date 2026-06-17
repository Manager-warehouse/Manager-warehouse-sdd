import apiClient, { useMock } from './api.client';

// LocalStorage Persistence Keys
const KEYS = {
  INVOICES: 'wms_db_invoices',
  PAYMENTS: 'wms_db_payment_receipts',
  PERIODS: 'wms_db_accounting_periods',
  NOTIFICATIONS: 'wms_db_billing_notifications',
  DEALERS: 'wms_db_dealers',
  DELIVERY_ORDERS: 'wms_db_delivery_orders',
  RETURNS: 'wms_db_returns',
  DISPOSALS: 'wms_db_disposals',
  QUARANTINE: 'wms_db_quarantine_items'
};

const INITIAL_QUARANTINE = [
  {
    id: 1,
    productSku: 'SP-001',
    productName: 'Nồi chiên không dầu Lock&Lock 5.2L',
    qcFailedQty: 10,
    qcFailureReason: 'Bể vỡ vỏ nhựa bên ngoài',
    receiptNumber: 'REC-20260612-001',
    supplierId: 1,
    totalValue: 8000000.00,
    unit: 'cái',
    receiptId: 101,
    receiptVersion: 1,
    batchId: 5,
    locationId: 99
  },
  {
    id: 2,
    productSku: 'SP-002',
    productName: 'Chảo chống dính Tefal 24cm',
    qcFailedQty: 150,
    qcFailureReason: 'Trầy xước lòng chảo diện rộng',
    receiptNumber: 'REC-20260613-002',
    supplierId: 2,
    totalValue: 120000000.00, // > 100M VND to test CEO approval
    unit: 'cái',
    receiptId: 102,
    receiptVersion: 1,
    batchId: 6,
    locationId: 99
  }
];

// Initial Mock Data
const INITIAL_PERIODS = [
  {
    id: 1,
    period_name: '2026-05',
    start_date: '2026-05-01',
    end_date: '2026-05-31',
    status: 'CLOSED',
    closed_by_name: 'Phạm Kế Toán Trưởng',
    closed_at: '2026-06-01T17:00:00Z',
    notes: 'Khóa sổ định kỳ tháng 5/2026'
  },
  {
    id: 2,
    period_name: '2026-06',
    start_date: '2026-06-01',
    end_date: '2026-06-30',
    status: 'OPEN',
    closed_by_name: null,
    closed_at: null,
    notes: null
  }
];

const INITIAL_NOTIFICATIONS = [
  {
    id: 1,
    do_id: 7,
    do_number: 'DO-20260610-001',
    dealer_id: 1,
    dealer_name: 'Công ty TNHH Tin học Hoàng Phát',
    warehouse_id: 3,
    delivered_at: '2026-06-11T10:00:00Z',
    total_amount_estimate: 8500000.00,
    invoice_status: 'NOT_INVOICED',
    status: 'ACTIVE',
    otpVerifiedAt: '2026-06-11T09:58:30Z',
    podImageUrl: 'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=500',
    podSignatureUrl: 'https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=500',
    podTimestamp: '2026-06-11T10:00:00Z'
  }
];

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
  const currentUser = JSON.parse(localStorage.getItem('wms_user')) || { fullName: 'System Accountant' };
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

export const financeService = {
  // --- BILLING NOTIFICATIONS ---
  getBillingNotifications: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      const list = getDb(KEYS.NOTIFICATIONS, INITIAL_NOTIFICATIONS);
      return list.filter(n => n.invoice_status === 'NOT_INVOICED' && n.status === 'ACTIVE');
    }
    const response = await apiClient.get('/billing-notifications');
    return response.data;
  },

  readBillingNotification: async (id) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 200));
      const list = getDb(KEYS.NOTIFICATIONS, INITIAL_NOTIFICATIONS);
      const idx = list.findIndex(n => n.id === Number(id));
      if (idx !== -1) {
        list[idx].status = 'READ';
        list[idx].read_at = new Date().toISOString();
        saveDb(KEYS.NOTIFICATIONS, list);
      }
      return { success: true };
    }
    const response = await apiClient.put(`/billing-notifications/${id}/read`);
    return response.data;
  },

  // --- INVOICES ---
  getInvoices: async (filters = {}) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      let list = getDb(KEYS.INVOICES, []);
      if (filters.dealerId) {
        list = list.filter(i => i.dealer_id === Number(filters.dealerId));
      }
      if (filters.status && filters.status !== 'ALL') {
        list = list.filter(i => i.status === filters.status);
      }
      return list.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
    }
    let url = '/invoices';
    const params = [];
    if (filters.dealerId) params.push(`dealerId=${filters.dealerId}`);
    if (filters.status) params.push(`status=${filters.status}`);
    if (params.length > 0) url += `?${params.join('&')}`;
    const response = await apiClient.get(url);
    return response.data;
  },

  getInvoiceById: async (id) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 200));
      const list = getDb(KEYS.INVOICES, []);
      const invoice = list.find(i => i.id === Number(id));
      if (!invoice) throw new Error('INVOICE_NOT_FOUND');
      return invoice;
    }
    const response = await apiClient.get(`/invoices/${id}`);
    return response.data;
  },

  createInvoice: async (doId, documentDate, notes) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const notifications = getDb(KEYS.NOTIFICATIONS, INITIAL_NOTIFICATIONS);
      const invoices = getDb(KEYS.INVOICES, []);
      const dealers = getDb(KEYS.DEALERS, []);
      const dos = getDb(KEYS.DELIVERY_ORDERS, []);

      const notification = notifications.find(n => n.do_id === Number(doId));
      if (!notification) throw new Error('DELIVERY_ORDER_NOT_DELIVERED');

      const dealer = dealers.find(d => d.id === notification.dealer_id);
      if (!dealer) throw new Error('DEALER_NOT_FOUND');

      // 1. Tạo Invoice mới
      const today = new Date();
      const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '').slice(0, 6);
      const count = invoices.length + 1;
      const invoiceNumber = `INV-${dateStr}-${String(count).padStart(6, '0')}`;

      const totalAmount = notification.total_amount_estimate;

      const newInvoice = {
        id: invoices.length > 0 ? Math.max(...invoices.map(i => i.id)) + 1 : 1,
        invoice_number: invoiceNumber,
        do_id: Number(doId),
        do_number: notification.do_number,
        dealer_id: dealer.id,
        dealer_name: dealer.name,
        total_amount: totalAmount,
        issue_date: documentDate,
        due_date: new Date(new Date(documentDate).getTime() + (dealer.payment_term_days || 30) * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
        status: 'UNPAID',
        document_date: documentDate,
        accounting_period_id: 2, // mock OPEN period id
        accounting_period_name: '2026-06',
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      };

      invoices.push(newInvoice);
      saveDb(KEYS.INVOICES, invoices);

      // 2. Trừ/Cập nhật công nợ đại lý
      dealer.current_balance = (dealer.current_balance || 0) + totalAmount;
      if (dealer.current_balance > dealer.credit_limit) {
        dealer.credit_status = 'CREDIT_HOLD';
      }
      saveDb(KEYS.DEALERS, dealers);

      // 3. Cập nhật DO status sang COMPLETED
      const doIdx = dos.findIndex(d => d.id === Number(doId));
      if (doIdx !== -1) {
        dos[doIdx].status = 'COMPLETED';
        dos[doIdx].updated_at = new Date().toISOString();
        saveDb(KEYS.DELIVERY_ORDERS, dos);
      }

      // 4. Lưu notification
      notification.invoice_status = 'INVOICED';
      notification.status = 'ARCHIVED';
      saveDb(KEYS.NOTIFICATIONS, notifications);

      addMockAuditLog('INVOICE_CREATED', 'Invoice', newInvoice.id, `Lập hóa đơn ${invoiceNumber} cho đơn hàng ${notification.do_number} - Số tiền: ${totalAmount.toLocaleString()} VND`);
      return newInvoice;
    }
    const response = await apiClient.post('/invoices', { do_id: doId, document_date: documentDate, notes });
    return response.data;
  },

  // --- PAYMENT RECEIPTS ---
  getPaymentReceipts: async (filters = {}) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      let list = getDb(KEYS.PAYMENTS, []);
      if (filters.dealerId) {
        list = list.filter(p => p.dealer_id === Number(filters.dealerId));
      }
      return list.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
    }
    let url = '/payment-receipts';
    const params = [];
    if (filters.dealerId) params.push(`dealerId=${filters.dealerId}`);
    if (filters.accountingPeriodId) params.push(`accountingPeriodId=${filters.accountingPeriodId}`);
    if (params.length > 0) url += `?${params.join('&')}`;
    const response = await apiClient.get(url);
    return response.data;
  },

  createPaymentReceipt: async (paymentData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const payments = getDb(KEYS.PAYMENTS, []);
      const invoices = getDb(KEYS.INVOICES, []);
      const dealers = getDb(KEYS.DEALERS, []);

      const dealer = dealers.find(d => d.id === Number(paymentData.dealerId));
      const invoice = invoices.find(i => i.id === Number(paymentData.invoiceId));

      if (!dealer || !invoice) throw new Error('INVALID_DEALER_OR_INVOICE');

      // 1. Tạo phiếu thu
      const today = new Date();
      const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '').slice(0, 6);
      const count = payments.length + 1;
      const paymentNumber = `PAY-${dateStr}-${String(count).padStart(6, '0')}`;

      const amount = Number(paymentData.amount);

      const newPayment = {
        id: payments.length > 0 ? Math.max(...payments.map(p => p.id)) + 1 : 1,
        payment_number: paymentNumber,
        dealer_id: dealer.id,
        dealer_name: dealer.name,
        invoice_id: invoice.id,
        invoice_number: invoice.invoice_number,
        amount: amount,
        payment_date: paymentData.paymentDate,
        payment_method: paymentData.paymentMethod,
        accounting_period_id: 2,
        accounting_period_name: '2026-06',
        notes: paymentData.notes || '',
        created_at: new Date().toISOString()
      };

      payments.push(newPayment);
      saveDb(KEYS.PAYMENTS, payments);

      // 2. Cập nhật hóa đơn
      invoice.status = 'PAID'; // Đơn giản hóa mock: set PAID luôn
      invoice.updated_at = new Date().toISOString();
      saveDb(KEYS.INVOICES, invoices);

      // 3. Cập nhật dư nợ đại lý
      dealer.current_balance = Math.max(0, (dealer.current_balance || 0) - amount);
      if (dealer.current_balance < (dealer.credit_limit * 0.8)) {
        dealer.credit_status = 'ACTIVE';
      }
      saveDb(KEYS.DEALERS, dealers);

      addMockAuditLog('PAYMENT_RECEIPT_CREATED', 'PaymentReceipt', newPayment.id, `Ghi nhận phiếu thu ${paymentNumber} cấn trừ hóa đơn ${invoice.invoice_number} - Số tiền: ${amount.toLocaleString()} VND`);
      return newPayment;
    }
    const response = await apiClient.post('/payment-receipts', {
      dealer_id: paymentData.dealerId,
      invoice_id: paymentData.invoiceId,
      amount: paymentData.amount,
      payment_date: paymentData.paymentDate,
      payment_method: paymentData.paymentMethod,
      notes: paymentData.notes
    });
    return response.data;
  },

  // --- AGING REPORT ---
  getAgingReport: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const dealers = getDb(KEYS.DEALERS, []);
      const invoices = getDb(KEYS.INVOICES, []);

      return dealers.map(dealer => {
        const unpaid = invoices.filter(i => i.dealer_id === dealer.id && i.status !== 'PAID');
        
        let inTerm = 0;
        let overdue1to30 = 0;
        let overdue31to60 = 0;
        let overdue61to90 = 0;
        let overdueOver90 = 0;

        unpaid.forEach(invoice => {
          const due = new Date(invoice.due_date);
          const diffDays = Math.ceil((new Date() - due) / (1000 * 60 * 60 * 24));
          
          if (diffDays <= 0) {
            inTerm += invoice.total_amount;
          } else if (diffDays <= 30) {
            overdue1to30 += invoice.total_amount;
          } else if (diffDays <= 60) {
            overdue31to60 += invoice.total_amount;
          } else if (diffDays <= 90) {
            overdue61to90 += invoice.total_amount;
          } else {
            overdueOver90 += invoice.total_amount;
          }
        });

        const riskLevel = (overdue61to90 > 0 || overdueOver90 > 0) ? 'HIGH_RISK' : 'NORMAL';

        return {
          dealer_id: dealer.id,
          dealer_code: dealer.code,
          dealer_name: dealer.name,
          credit_limit: dealer.credit_limit,
          current_balance: dealer.current_balance,
          credit_status: dealer.credit_status,
          in_term_amount: inTerm,
          overdue_1_to_30: overdue1to30,
          overdue_31_to_60: overdue31to60,
          overdue_61_to_90: overdue61to90,
          overdue_over_90: overdueOver90,
          risk_level: riskLevel
        };
      });
    }
    const response = await apiClient.get('/credit/aging-report');
    return response.data;
  },

  // --- ACCOUNTING PERIODS ---
  getAccountingPeriods: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 200));
      return getDb(KEYS.PERIODS, INITIAL_PERIODS);
    }
    const response = await apiClient.get('/accounting-periods');
    return response.data;
  },

  closeAccountingPeriod: async (id, notes) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const list = getDb(KEYS.PERIODS, INITIAL_PERIODS);
      const idx = list.findIndex(p => p.id === Number(id));
      if (idx === -1) throw new Error('PERIOD_NOT_FOUND');

      list[idx].status = 'CLOSED';
      list[idx].closed_by_name = 'Phạm Kế Toán Trưởng';
      list[idx].closed_at = new Date().toISOString();
      list[idx].notes = notes;

      saveDb(KEYS.PERIODS, list);
      addMockAuditLog('PERIOD_CLOSED', 'AccountingPeriod', id, `Chốt sổ kỳ kế toán: ${list[idx].period_name}`);
      return list[idx];
    }
    const response = await apiClient.put(`/accounting-periods/${id}/close`, { notes });
    return response.data;
  },

  // --- QUARANTINE & DISPOSAL ---
  getQuarantineItems: async (warehouseId) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      return getDb(KEYS.QUARANTINE, INITIAL_QUARANTINE);
    }
    const response = await apiClient.get(`/quarantine/items?warehouseId=${warehouseId}`);
    return response.data;
  },

  createDisposal: async (disposalData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const disposals = getDb(KEYS.DISPOSALS, []);
      const id = disposals.length > 0 ? Math.max(...disposals.map(d => d.id)) + 1 : 1;
      const count = disposals.length + 1;
      const docDateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '').slice(0, 6);
      const adjNumber = `ADJ-${docDateStr}-${String(count).padStart(6, '0')}`;
      const drNumber = `DR-${docDateStr}-${String(count).padStart(6, '0')}`;

      const quarantineItems = getDb(KEYS.QUARANTINE, INITIAL_QUARANTINE);
      const qItem = quarantineItems.find(q => q.batchId === Number(disposalData.batchId) && q.locationId === Number(disposalData.locationId));
      const cost = qItem ? qItem.totalValue / qItem.qcFailedQty : 800000;
      const totalValue = Number(disposalData.quantity) * cost;

      const newDisposal = {
        id,
        adjustmentNumber: adjNumber,
        damageReportNumber: drNumber,
        warehouseId: Number(disposalData.warehouseId),
        productId: Number(disposalData.productId),
        batchId: Number(disposalData.batchId),
        locationId: Number(disposalData.locationId),
        quantity: Number(disposalData.quantity),
        cause: disposalData.cause,
        imageUrl: disposalData.imageUrl || '',
        totalValueEstimate: totalValue,
        confirmed: false,
        version: 0,
        created_at: new Date().toISOString()
      };

      disposals.push(newDisposal);
      saveDb(KEYS.DISPOSALS, disposals);
      addMockAuditLog('QUARANTINE_DISPOSAL_CREATE', 'Adjustment', id, `Lập đề xuất tiêu hủy ${adjNumber} cho sản phẩm ID ${disposalData.productId}`);
      return newDisposal;
    }
    const response = await apiClient.post('/disposal', disposalData);
    return response.data;
  },

  approveDisposal: async (id, expectedVersion) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const disposals = getDb(KEYS.DISPOSALS, []);
      const idx = disposals.findIndex(d => d.id === Number(id));
      if (idx === -1) throw new Error('DISPOSAL_NOT_FOUND');

      const disposal = disposals[idx];
      disposal.confirmed = true;
      disposal.approvedBy = 2; // mock manager/CEO
      disposal.approvedAt = new Date().toISOString();
      disposal.version += 1;
      saveDb(KEYS.DISPOSALS, disposals);

      // Trừ tồn kho trong Quarantine
      const quarantineItems = getDb(KEYS.QUARANTINE, INITIAL_QUARANTINE);
      const qIdx = quarantineItems.findIndex(q => q.batchId === disposal.batchId && q.locationId === disposal.locationId);
      if (qIdx !== -1) {
        quarantineItems[qIdx].qcFailedQty = Math.max(0, quarantineItems[qIdx].qcFailedQty - disposal.quantity);
        quarantineItems[qIdx].totalValue = Math.max(0, quarantineItems[qIdx].totalValue - disposal.totalValueEstimate);
        if (quarantineItems[qIdx].qcFailedQty === 0) {
          quarantineItems.splice(qIdx, 1);
        }
        saveDb(KEYS.QUARANTINE, quarantineItems);
      }

      addMockAuditLog('QUARANTINE_DISPOSAL_APPROVE', 'Adjustment', id, `Phê duyệt tiêu hủy ${disposal.adjustmentNumber}`);
      return disposal;
    }
    const response = await apiClient.put(`/disposal/${id}/approve`, { expectedVersion });
    return response.data;
  },

  getDisposals: async () => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 200));
      return getDb(KEYS.DISPOSALS, []);
    }
    const response = await apiClient.get('/disposal');
    return response.data;
  },

  // --- CUSTOMER RETURNS ---
  getReturns: async (warehouseId) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      const list = getDb(KEYS.RETURNS, []);
      return list.filter(r => r.warehouse_id === Number(warehouseId));
    }
    const response = await apiClient.get(`/returns?warehouseId=${warehouseId}`);
    return response.data;
  },

  createReturn: async (returnData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const returns = getDb(KEYS.RETURNS, []);
      const id = returns.length > 0 ? Math.max(...returns.map(r => r.id)) + 1 : 101;
      const count = returns.length + 1;
      const docDateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '').slice(0, 6);
      const receiptNumber = `RET-${docDateStr}-${String(count).padStart(6, '0')}`;

      const newReturn = {
        id,
        receiptNumber,
        warehouse_id: Number(returnData.warehouseId),
        dealer_id: Number(returnData.dealerId),
        delivery_order_id: Number(returnData.deliveryOrderId),
        contactPerson: returnData.contactPerson,
        status: 'DRAFT',
        documentDate: returnData.documentDate,
        notes: returnData.notes || '',
        items: returnData.items.map((it, idx) => ({
          id: 200 + idx,
          productId: Number(it.productId),
          expectedQty: Number(it.expectedQty),
          actualQty: null,
          qcPassedQty: null,
          qcFailedQty: null,
          qcResult: null,
          qcFailureReason: null
        })),
        version: 0
      };

      returns.push(newReturn);
      saveDb(KEYS.RETURNS, returns);
      addMockAuditLog('RETURN_CREATE', 'Receipt', id, `Lập nháp phiếu hoàn trả ${receiptNumber}`);
      return newReturn;
    }
    const response = await apiClient.post('/returns', returnData);
    return response.data;
  },

  submitReturnQc: async (id, qcData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const returns = getDb(KEYS.RETURNS, []);
      const idx = returns.findIndex(r => r.id === Number(id));
      if (idx === -1) throw new Error('RETURN_NOT_FOUND');

      const ret = returns[idx];
      ret.status = 'QC_COMPLETED';
      ret.version = Number(qcData.expectedVersion) + 1;

      qcData.items.forEach(qi => {
        const item = ret.items.find(i => i.id === qi.receiptItemId);
        if (item) {
          item.actualQty = qi.actualQty;
          item.qcPassedQty = qi.qcPassedQty;
          item.qcFailedQty = qi.qcFailedQty;
          item.qcFailureReason = qi.qcFailureReason;
          item.qcResult = qi.qcFailedQty === 0 ? 'PASSED' : (qi.qcPassedQty === 0 ? 'FAILED' : 'PARTIAL');
        }
      });

      saveDb(KEYS.RETURNS, returns);
      addMockAuditLog('RECEIPT_QC_CONFIRM', 'Receipt', id, `Nhập kết quả QC hàng hoàn cho phiếu ${ret.receiptNumber}`);
      return ret;
    }
    const response = await apiClient.put(`/returns/${id}/qc`, qcData);
    return response.data;
  },

  approveReturn: async (id, expectedVersion) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const returns = getDb(KEYS.RETURNS, []);
      const idx = returns.findIndex(r => r.id === Number(id));
      if (idx === -1) throw new Error('RETURN_NOT_FOUND');

      const ret = returns[idx];
      ret.status = 'APPROVED';
      ret.version = expectedVersion + 1;
      saveDb(KEYS.RETURNS, returns);
      addMockAuditLog('RECEIPT_APPROVE', 'Receipt', id, `Duyệt nhập kho hàng hoàn ${ret.receiptNumber}`);
      return ret;
    }
    const response = await apiClient.put(`/returns/${id}/approve`, { expectedVersion });
    return response.data;
  },

  completeReturnPutaway: async (id, putawayData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const returns = getDb(KEYS.RETURNS, []);
      const idx = returns.findIndex(r => r.id === Number(id));
      if (idx === -1) throw new Error('RETURN_NOT_FOUND');

      const ret = returns[idx];
      ret.version = putawayData.expectedVersion + 1;
      saveDb(KEYS.RETURNS, returns);

      // Tăng tồn kho
      const quarantineItems = getDb(KEYS.QUARANTINE, INITIAL_QUARANTINE);
      ret.items.forEach(item => {
        if (item.qcFailedQty && item.qcFailedQty > 0) {
          quarantineItems.push({
            id: quarantineItems.length + 1,
            productSku: `SP-00${item.productId}`,
            productName: `Sản phẩm ID ${item.productId}`,
            qcFailedQty: item.qcFailedQty,
            qcFailureReason: item.qcFailureReason || 'Lỗi trả hàng',
            receiptNumber: ret.receiptNumber,
            supplierId: 1,
            totalValue: item.qcFailedQty * 500000,
            unit: 'cái',
            receiptId: ret.id,
            receiptVersion: ret.version,
            batchId: 10 + item.id,
            locationId: 99
          });
        }
      });
      saveDb(KEYS.QUARANTINE, quarantineItems);

      addMockAuditLog('RECEIPT_PUTAWAY_COMPLETE', 'Receipt', id, `Hoàn tất xếp dỡ hàng hoàn ${ret.receiptNumber}`);
      return ret;
    }
    const response = await apiClient.put(`/returns/${id}/complete`, putawayData);
    return response.data;
  },

  createReturnCreditNote: async (id, creditNoteData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const returns = getDb(KEYS.RETURNS, []);
      const ret = returns.find(r => r.id === Number(id));
      if (!ret) throw new Error('RETURN_NOT_FOUND');

      const creditNotes = getDb(KEYS.CREDIT_NOTES, []);
      const dealers = getDb(KEYS.DEALERS, []);
      const dealer = dealers.find(d => d.id === ret.dealer_id);

      if (!dealer) throw new Error('DEALER_NOT_FOUND');

      const today = new Date();
      const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '').slice(0, 6);
      const count = creditNotes.length + 1;
      const cnNumber = `CN-${dateStr}-${String(count).padStart(6, '0')}`;

      // Tính tổng tiền hoàn (mock: 1.5M/cái)
      const totalAmount = ret.items.reduce((sum, item) => sum + ((item.actualQty || 0) * 1500000), 0);

      const newCreditNote = {
        creditNoteId: creditNotes.length + 1,
        creditNoteNumber: cnNumber,
        amount: totalAmount,
        dealerId: dealer.id,
        receiptId: ret.id,
        message: 'Credit Note generated successfully.'
      };

      creditNotes.push(newCreditNote);
      saveDb(KEYS.CREDIT_NOTES, creditNotes);

      // Cập nhật công nợ đại lý
      dealer.current_balance = Math.max(0, (dealer.current_balance || 0) - totalAmount);
      saveDb(KEYS.DEALERS, dealers);

      addMockAuditLog('CREDIT_NOTE_CREATE', 'CreditNote', newCreditNote.creditNoteId, `Tạo Credit Note ${cnNumber} hoàn tiền ${totalAmount.toLocaleString()} VND`);
      return newCreditNote;
    }
    const response = await apiClient.post(`/returns/${id}/credit-note`, creditNoteData);
    return response.data;
  }
}
