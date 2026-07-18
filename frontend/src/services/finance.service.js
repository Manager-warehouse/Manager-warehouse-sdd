import apiClient, { useMock } from './api.client';

// LocalStorage Persistence Keys
const KEYS = {
  INVOICES: 'wms_db_invoices',
  PAYMENTS: 'wms_db_payment_receipts',
  PERIODS: 'wms_db_accounting_periods',
  NOTIFICATIONS: 'wms_db_billing_notifications',
  DEALERS: 'wms_db_dealers',
  DELIVERY_ORDERS: 'wms_db_delivery_orders'
};

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
    otp_verified_at: '2026-06-11T09:58:30Z',
    pod_image_url: 'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=500',
    pod_signature_url: 'https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=500',
    pod_timestamp: '2026-06-11T10:00:00Z'
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

  scanPaymentReceiptOcr: async (file) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 1500));
      const dealers = getDb(KEYS.DEALERS, []);

      // Phân tích tên file ảnh giả lập số tiền
      let filenameClean = file.name.toLowerCase();
      if (filenameClean.includes("screenshot")) {
        // Nếu là file screenshot hệ thống tự đặt tên, xóa bỏ phần số ngày giờ đi kèm (ví dụ: screenshot 2026-06-17 221536)
        filenameClean = filenameClean.replace(/screenshot[\s-_]*\d+.*/g, 'screenshot');
      } else {
        // Loại bỏ phần ngày tháng năm của screenshot (dạng 202x-xx-xx hoặc năm 202x)
        filenameClean = filenameClean.replace(/202\d[-_]\d{2}[-_]\d{2}/g, '').replace(/202\d/g, '');
      }
      let amount = 25000000.00;
      const numberPattern = /\d{4,}/;
      const match = filenameClean.match(numberPattern);
      if (match) {
        amount = parseFloat(match[0]);
      }
      
      // Phân tích tên file ảnh để map đại lý: ưu tiên số tài khoản ngân hàng (nếu có trong tên file
      // giả lập ảnh chụp), sau đó mới rơi về khớp tên/mã đại lý — mô phỏng lại thứ tự khớp của backend.
      let matchedDealer = null;
      let matchedByBankAccount = false;
      const filename = file.name.toLowerCase();
      const filenameDigitsOnly = filename.replace(/[^0-9]/g, '');
      for (const d of dealers) {
        if (d.bank_account_number) {
          const cleanAccount = String(d.bank_account_number).replace(/[^0-9]/g, '');
          if (cleanAccount.length >= 6 && filenameDigitsOnly.includes(cleanAccount)) {
            matchedDealer = d;
            matchedByBankAccount = true;
            break;
          }
        }
      }
      if (!matchedDealer) {
        for (const d of dealers) {
          const cleanName = d.name.toLowerCase().replace(/[^a-z0-9]/g, '');
          const cleanFilename = filename.replace(/[^a-z0-9]/g, '');
          if (cleanFilename.includes(cleanName) || filename.includes(d.code.toLowerCase().replace(/-/g, '_'))) {
            matchedDealer = d;
            break;
          }
        }
      }

      if (!matchedDealer) {
        return {
          amount: amount,
          payment_date: new Date().toISOString().slice(0, 10),
          dealer_id: null,
          notes: `CK TIEN HANG - KHONG RO DAI LY (OCR_MOCK_${Math.floor(Math.random() * 100000)})`,
          confidence_score: 0.55
        };
      }

      return {
        amount: amount,
        payment_date: new Date().toISOString().slice(0, 10),
        dealer_id: matchedDealer.id,
        notes: `CK TIEN HANG - ${matchedDealer.name.toUpperCase()} - GIAO DICH OCR_MOCK_${Math.floor(Math.random() * 100000)}`,
        confidence_score: matchedByBankAccount ? 0.97 : 0.95
      };
    }
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post('/payment-receipts/ocr', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
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
  }
};
