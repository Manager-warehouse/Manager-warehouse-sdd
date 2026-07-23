import React, { useState, useEffect, useCallback } from 'react';
import { financeService } from '../../services/finance.service';
import { masterDataService } from '../../services/masterData.service';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import { ROLES } from '../../utils/constants';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import PhotoCaptureInput from '../../components/common/PhotoCaptureInput';
import { FileText, Landmark, BellRing, ShieldAlert, Plus, CheckCircle2, TrendingDown, Building2, UploadCloud } from 'lucide-react';

const OCR_LOW_CONFIDENCE_THRESHOLD = 0.75;

const SupplierInvoices = () => {
  const { addToast } = useUiStore();
  const { hasRole } = useAuthStore();
  const isAccountant = hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ADMIN);

  const [activeTab, setActiveTab] = useState('notifications'); // 'notifications' | 'invoices' | 'payments'
  const [loading, setLoading] = useState(false);

  // Data States
  const [suppliers, setSuppliers] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [supplierInvoices, setSupplierInvoices] = useState([]);
  const [supplierPayments, setSupplierPayments] = useState([]);

  // Modal States
  const [showCreateInvoiceModal, setShowCreateInvoiceModal] = useState(false);
  const [selectedNotification, setSelectedNotification] = useState(null);
  const [invoiceFormData, setInvoiceFormData] = useState({
    receiptId: '',
    supplierInvoiceNumber: '',
    documentDate: new Date().toISOString().slice(0, 10),
    dueDate: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10),
    notes: ''
  });

  const [showCreatePaymentModal, setShowCreatePaymentModal] = useState(false);
  const [selectedInvoiceForPayment, setSelectedInvoiceForPayment] = useState(null);
  const [paymentFormData, setPaymentFormData] = useState({
    supplierId: '',
    supplierInvoiceId: '',
    amount: '',
    paymentDate: new Date().toISOString().slice(0, 10),
    paymentMethod: 'BANK_TRANSFER',
    documentDate: new Date().toISOString().slice(0, 10),
    notes: ''
  });

  // OCR states for UNC Payment Scan
  const [ocrLoading, setOcrLoading] = useState(false);
  const [ocrFileName, setOcrFileName] = useState('');
  const [ocrConfidence, setOcrConfidence] = useState(null);
  const [ocrResetKey, setOcrResetKey] = useState(0);
  const ocrLowConfidence = ocrConfidence !== null && ocrConfidence < OCR_LOW_CONFIDENCE_THRESHOLD;

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [suppList, invs] = await Promise.all([
        masterDataService.getSuppliers(),
        financeService.getSupplierInvoices()
      ]);
      setSuppliers(suppList || []);
      setSupplierInvoices(invs || []);

      if (activeTab === 'notifications') {
        const notifs = await financeService.getSupplierBillingNotifications();
        setNotifications(notifs || []);
      } else if (activeTab === 'invoices') {
        // supplierInvoices already loaded
      } else if (activeTab === 'payments') {
        const pmts = await financeService.getSupplierPayments();
        setSupplierPayments(pmts || []);
      }
    } catch (err) {
      console.error('Failed to load supplier finance data:', err);
      addToast('Không thể tải dữ liệu hóa đơn mua hàng', 'error');
    } finally {
      setLoading(false);
    }
  }, [activeTab, addToast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Open modal to create invoice from receipt notification
  const handleOpenCreateInvoice = (notif) => {
    setSelectedNotification(notif);
    setInvoiceFormData({
      receiptId: notif.receipt_id || notif.receiptId,
      supplierInvoiceNumber: `VAT-${Math.floor(100000 + Math.random() * 900000)}`,
      documentDate: new Date().toISOString().slice(0, 10),
      dueDate: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10),
      notes: `Lập hóa đơn mua hàng từ phiếu nhập ${notif.receipt_number || notif.receiptNumber}`
    });
    setShowCreateInvoiceModal(true);
  };

  // Submit Supplier Invoice Creation
  const handleSubmitInvoice = async (e) => {
    e.preventDefault();
    try {
      await financeService.createSupplierInvoice(invoiceFormData);
      addToast('Lập Hóa đơn Mua hàng & Ghi nhận nợ NCC thành công!', 'success');
      setShowCreateInvoiceModal(false);
      loadData();
    } catch (err) {
      console.error('Create supplier invoice failed:', err);
      addToast(err.message || 'Không thể tạo hóa đơn mua hàng', 'error');
    }
  };

  // Open modal to create payment for supplier invoice (from specific invoice)
  const handleOpenCreatePayment = (inv) => {
    setSelectedInvoiceForPayment(inv);
    const remaining = Number(inv.total_amount || inv.totalAmount || 0) - Number(inv.paid_amount || inv.paidAmount || 0);
    setPaymentFormData({
      supplierId: String(inv.supplier_id || inv.supplierId || ''),
      supplierInvoiceId: String(inv.id),
      amount: String(remaining > 0 ? remaining : (inv.total_amount || inv.totalAmount || 0)),
      paymentDate: new Date().toISOString().slice(0, 10),
      paymentMethod: 'BANK_TRANSFER',
      documentDate: new Date().toISOString().slice(0, 10),
      notes: `Thanh toán cho hóa đơn ${inv.invoice_number || inv.invoiceNumber}`
    });
    setOcrFileName('');
    setOcrConfidence(null);
    setOcrResetKey(prev => prev + 1);
    setShowCreatePaymentModal(true);
  };

  // Open modal to create payment standalone (e.g. from Header / OCR button)
  const handleOpenCreatePaymentStandalone = () => {
    setSelectedInvoiceForPayment(null);
    const initialSupplierId = suppliers.length > 0 ? String(suppliers[0].id) : '';
    let initialInvoiceId = '';
    let initialAmount = '';

    if (initialSupplierId) {
      const unpaidInvs = supplierInvoices.filter(inv =>
        String(inv.supplier_id || inv.supplierId) === String(initialSupplierId) && inv.status !== 'PAID'
      );
      if (unpaidInvs.length > 0) {
        initialInvoiceId = String(unpaidInvs[0].id);
        const remaining = Number(unpaidInvs[0].total_amount || unpaidInvs[0].totalAmount || 0) - Number(unpaidInvs[0].paid_amount || unpaidInvs[0].paidAmount || 0);
        initialAmount = String(remaining > 0 ? remaining : (unpaidInvs[0].total_amount || unpaidInvs[0].totalAmount || 0));
      }
    }

    setPaymentFormData({
      supplierId: initialSupplierId,
      supplierInvoiceId: initialInvoiceId,
      amount: initialAmount,
      paymentDate: new Date().toISOString().slice(0, 10),
      paymentMethod: 'BANK_TRANSFER',
      documentDate: new Date().toISOString().slice(0, 10),
      notes: ''
    });
    setOcrFileName('');
    setOcrConfidence(null);
    setOcrResetKey(prev => prev + 1);
    setShowCreatePaymentModal(true);
  };

  // Handle supplier change in payment form
  const handleSupplierChangeInPayment = (suppId) => {
    let autoInvoiceId = '';
    let autoAmount = '';
    if (suppId) {
      const unpaidInvs = supplierInvoices.filter(inv =>
        String(inv.supplier_id || inv.supplierId) === String(suppId) && inv.status !== 'PAID'
      );
      if (unpaidInvs.length > 0) {
        autoInvoiceId = String(unpaidInvs[0].id);
        const remaining = Number(unpaidInvs[0].total_amount || unpaidInvs[0].totalAmount || 0) - Number(unpaidInvs[0].paid_amount || unpaidInvs[0].paidAmount || 0);
        autoAmount = String(remaining > 0 ? remaining : (unpaidInvs[0].total_amount || unpaidInvs[0].totalAmount || 0));
      }
    }
    setPaymentFormData(prev => ({
      ...prev,
      supplierId: suppId,
      supplierInvoiceId: autoInvoiceId,
      amount: autoAmount || prev.amount
    }));
  };

  // Handle invoice change in payment form
  const handleInvoiceChangeInPayment = (invId) => {
    const selectedInv = supplierInvoices.find(i => String(i.id) === String(invId));
    let newAmount = paymentFormData.amount;
    if (selectedInv) {
      const remaining = Number(selectedInv.total_amount || selectedInv.totalAmount || 0) - Number(selectedInv.paid_amount || selectedInv.paidAmount || 0);
      newAmount = String(remaining > 0 ? remaining : (selectedInv.total_amount || selectedInv.totalAmount || 0));
    }
    setPaymentFormData(prev => ({
      ...prev,
      supplierInvoiceId: invId,
      amount: newAmount
    }));
  };

  // OCR Upload UNC File Selected
  const handleOcrFileSelected = async (file) => {
    if (!file) return;

    if (!['image/png', 'image/jpeg', 'image/jpg'].includes(file.type)) {
      addToast('Chỉ hỗ trợ file ảnh PNG, JPEG, JPG', 'error');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      addToast('Dung lượng ảnh tối đa 5MB', 'error');
      return;
    }

    setOcrLoading(true);
    setOcrFileName(file.name);
    setOcrConfidence(null);
    try {
      const result = await financeService.scanSupplierPaymentOcr(file);
      const matchedSuppId = result.supplierId ? String(result.supplierId) : paymentFormData.supplierId;

      let matchedInvoiceId = result.supplierInvoiceId ? String(result.supplierInvoiceId) : '';
      if (!matchedInvoiceId && matchedSuppId) {
        const unpaidInvs = supplierInvoices.filter(inv =>
          String(inv.supplier_id || inv.supplierId) === String(matchedSuppId) && inv.status !== 'PAID'
        );
        if (unpaidInvs.length > 0) {
          matchedInvoiceId = String(unpaidInvs[0].id);
        }
      }

      setPaymentFormData(prev => ({
        ...prev,
        supplierId: matchedSuppId || prev.supplierId,
        supplierInvoiceId: matchedInvoiceId || prev.supplierInvoiceId,
        amount: result.amount ? String(result.amount) : prev.amount,
        paymentDate: result.paymentDate || prev.paymentDate,
        notes: result.notes || prev.notes
      }));
      setOcrConfidence(result.confidenceScore ?? null);

      const confidencePercent = Math.round((result.confidenceScore || 0) * 100);
      if ((result.confidenceScore ?? 1) < OCR_LOW_CONFIDENCE_THRESHOLD) {
        addToast(`Độ tin cậy nhận diện thấp (${confidencePercent}%). Vui lòng kiểm tra kỹ trước khi lưu.`, 'warning');
      } else {
        addToast(`Quét OCR ủy nhiệm chi thành công! Độ chính xác: ${confidencePercent}%`, 'success');
      }
    } catch (err) {
      console.error('OCR UNC scan failed:', err);
      addToast(err.message || 'Không thể quét OCR ủy nhiệm chi', 'error');
      setOcrFileName('');
    } finally {
      setOcrLoading(false);
    }
  };

  // Submit Supplier Payment
  const handleSubmitPayment = async (e) => {
    e.preventDefault();
    if (!paymentFormData.supplierId) {
      addToast('Vui lòng chọn Nhà cung cấp', 'error');
      return;
    }
    if (!paymentFormData.supplierInvoiceId) {
      addToast('Vui lòng chọn Hóa đơn mua hàng cần thanh toán', 'error');
      return;
    }
    if (Number(paymentFormData.amount) <= 0) {
      addToast('Số tiền chi phải lớn hơn 0', 'error');
      return;
    }
    try {
      await financeService.createSupplierPayment(paymentFormData);
      addToast('Ghi nhận Phiếu chi thanh toán NCC thành công!', 'success');
      setShowCreatePaymentModal(false);
      loadData();
    } catch (err) {
      console.error('Create supplier payment failed:', err);
      addToast(err.message || 'Không thể lập phiếu chi thanh toán NCC', 'error');
    }
  };

  // Available unpaid invoices for currently selected supplier in modal
  const availableInvoicesForSupplier = supplierInvoices.filter(inv =>
    String(inv.supplier_id || inv.supplierId) === String(paymentFormData.supplierId) &&
    inv.status !== 'PAID'
  );

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Tài chính / Quản lý Phải trả (AP)
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Hóa đơn Mua hàng & Công nợ Nhà cung cấp
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Tiếp nhận thông báo phiếu nhập kho `COMPLETED`, lập Hóa đơn Mua hàng ghi nhận nợ NCC, quét OCR ủy nhiệm chi (UNC) và lập Phiếu chi thanh toán.
          </p>
        </div>
        {isAccountant && (
          <div className="flex items-center gap-2">
            <Button
              variant="primary"
              onClick={handleOpenCreatePaymentStandalone}
              className="flex items-center gap-2 shadow-sm"
            >
              <UploadCloud className="w-4 h-4" />
              Lập Phiếu Chi (Quét OCR UNC)
            </Button>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="flex border-b border-hairline-light">
        <button
          className={`flex items-center gap-2 px-5 py-3 text-xs font-semibold uppercase tracking-wider border-b-2 transition-colors ${
            activeTab === 'notifications'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => setActiveTab('notifications')}
        >
          <BellRing className="w-4 h-4" />
          Thông báo Lập Hóa đơn Mua
        </button>
        <button
          className={`flex items-center gap-2 px-5 py-3 text-xs font-semibold uppercase tracking-wider border-b-2 transition-colors ${
            activeTab === 'invoices'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => setActiveTab('invoices')}
        >
          <FileText className="w-4 h-4" />
          Hóa đơn Mua hàng (SINV)
        </button>
        <button
          className={`flex items-center gap-2 px-5 py-3 text-xs font-semibold uppercase tracking-wider border-b-2 transition-colors ${
            activeTab === 'payments'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => setActiveTab('payments')}
        >
          <Landmark className="w-4 h-4" />
          Phiếu chi Thanh toán NCC
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20 text-shade-50">
          <svg className="animate-spin h-6 w-6 text-ink mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          <span>Đang tải dữ liệu...</span>
        </div>
      ) : (
        <>
          {/* TAB 1: WORKLIST THÔNG BÁO LẬP HÓA ĐƠN MUA */}
          {activeTab === 'notifications' && (
            <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
              <div className="p-4 bg-canvas-cream border-b border-hairline-light flex items-center justify-between">
                <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider">
                  Danh sách Phiếu nhập kho cần lập Hóa đơn Mua hàng
                </span>
                <span className="text-[10px] bg-shade-70 text-onPrimary px-2.5 py-0.5 rounded-pill font-bold">
                  {notifications.filter(n => n.invoiceStatus === 'NOT_INVOICED').length} Phiếu chờ
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full border-collapse text-left text-xs">
                  <thead>
                    <tr className="bg-canvas-light border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                      <th className="p-4">Số Phiếu Nhập</th>
                      <th className="p-4">Nhà cung cấp</th>
                      <th className="p-4">Kho nhập</th>
                      <th className="p-4">Thời gian hoàn thành</th>
                      <th className="p-4 text-right">Ước tính giá trị</th>
                      <th className="p-4 text-center">Trạng thái HĐ</th>
                      <th className="p-4 text-center">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {notifications.length === 0 ? (
                      <tr>
                        <td colSpan="7" className="p-8 text-center text-shade-40 italic">
                          Không có thông báo lập hóa đơn mua hàng nào cần xử lý.
                        </td>
                      </tr>
                    ) : (
                      notifications.map(notif => (
                        <tr key={notif.id} className="hover:bg-canvas-cream/50">
                          <td className="p-4 font-bold text-ink">{notif.receiptOrderNumber || notif.receipt_number}</td>
                          <td className="p-4 font-medium text-ink">{notif.supplierName || notif.supplier_name}</td>
                          <td className="p-4 text-shade-60">Kho #{notif.warehouseId || notif.warehouse_id}</td>
                          <td className="p-4 text-shade-60">{notif.completedAt || 'Mới hoàn tất'}</td>
                          <td className="p-4 text-right font-bold text-ink">
                            {(notif.totalAmountEstimate || 0).toLocaleString()}đ
                          </td>
                          <td className="p-4 text-center">
                            <span className={`px-2.5 py-0.5 rounded-pill text-[9px] font-bold uppercase ${
                              notif.invoiceStatus === 'INVOICED'
                                ? 'bg-aloe-10 text-ink'
                                : 'bg-amber-100 text-amber-800 border border-amber-200'
                            }`}>
                              {notif.invoiceStatus === 'INVOICED' ? 'Đã lập HĐ' : 'Chưa lập HĐ'}
                            </span>
                          </td>
                          <td className="p-4 text-center">
                            {notif.invoiceStatus !== 'INVOICED' && isAccountant && (
                              <Button
                                size="sm"
                                variant="primary"
                                onClick={() => handleOpenCreateInvoice(notif)}
                              >
                                <Plus className="w-3.5 h-3.5 mr-1" />
                                Lập Hóa đơn Mua
                              </Button>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 2: HÓA ĐƠN MUA HÀNG */}
          {activeTab === 'invoices' && (
            <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
              <div className="p-4 bg-canvas-cream border-b border-hairline-light flex items-center justify-between">
                <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider">
                  Sổ Hóa đơn Mua hàng & Dư nợ Phải trả NCC
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full border-collapse text-left text-xs">
                  <thead>
                    <tr className="bg-canvas-light border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                      <th className="p-4">Mã HĐ Nội bộ</th>
                      <th className="p-4">Số HĐ VAT (NCC)</th>
                      <th className="p-4">Nhà cung cấp</th>
                      <th className="p-4">Ngày hạch toán</th>
                      <th className="p-4">Hạn thanh toán</th>
                      <th className="p-4 text-right">Tổng số tiền</th>
                      <th className="p-4 text-center">Trạng thái</th>
                      <th className="p-4 text-center">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {supplierInvoices.length === 0 ? (
                      <tr>
                        <td colSpan="8" className="p-8 text-center text-shade-40 italic">
                          Chưa có hóa đơn mua hàng nào trong hệ thống.
                        </td>
                      </tr>
                    ) : (
                      supplierInvoices.map(inv => (
                        <tr key={inv.id} className="hover:bg-canvas-cream/50">
                          <td className="p-4 font-bold text-ink">{inv.invoice_number}</td>
                          <td className="p-4 font-semibold text-shade-70">{inv.supplier_invoice_number}</td>
                          <td className="p-4 font-medium text-ink">{inv.supplier_name || 'NCC #' + inv.supplier_id}</td>
                          <td className="p-4 text-shade-60">{inv.document_date}</td>
                          <td className="p-4 text-shade-60">{inv.due_date}</td>
                          <td className="p-4 text-right font-bold text-ink">
                            {(inv.total_amount || 0).toLocaleString()}đ
                          </td>
                          <td className="p-4 text-center">
                            <span className={`px-2.5 py-0.5 rounded-pill text-[9px] font-bold uppercase ${
                              inv.status === 'PAID'
                                ? 'bg-aloe-10 text-ink'
                                : inv.status === 'PARTIALLY_PAID'
                                ? 'bg-yellow-100 text-yellow-800'
                                : 'bg-red-100 text-red-700 border border-red-200'
                            }`}>
                              {inv.status === 'PAID' ? 'Đã TT' : inv.status === 'PARTIALLY_PAID' ? 'TT 1 Phần' : 'Chưa TT'}
                            </span>
                          </td>
                          <td className="p-4 text-center">
                            {inv.status !== 'PAID' && isAccountant && (
                              <Button
                                size="sm"
                                variant="secondary"
                                onClick={() => handleOpenCreatePayment(inv)}
                              >
                                <Landmark className="w-3.5 h-3.5 mr-1" />
                                Chi thanh toán
                              </Button>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 3: PHIẾU CHI THANH TOÁN NCC */}
          {activeTab === 'payments' && (
            <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
              <div className="p-4 bg-canvas-cream border-b border-hairline-light flex items-center justify-between">
                <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider">
                  Nhật ký Phiếu chi Thanh toán Nhà cung cấp
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full border-collapse text-left text-xs">
                  <thead>
                    <tr className="bg-canvas-light border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                      <th className="p-4">Số Phiếu Chi</th>
                      <th className="p-4">Nhà cung cấp</th>
                      <th className="p-4">Hóa đơn mua</th>
                      <th className="p-4">Ngày chi</th>
                      <th className="p-4">Hình thức</th>
                      <th className="p-4 text-right">Số tiền chi</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {supplierPayments.length === 0 ? (
                      <tr>
                        <td colSpan="6" className="p-8 text-center text-shade-40 italic">
                          Chưa có phiếu chi thanh toán NCC nào.
                        </td>
                      </tr>
                    ) : (
                      supplierPayments.map(sp => (
                        <tr key={sp.id} className="hover:bg-canvas-cream/50">
                          <td className="p-4 font-bold text-ink">{sp.payment_number}</td>
                          <td className="p-4 font-medium text-ink">{sp.supplier_name || 'NCC #' + sp.supplier_id}</td>
                          <td className="p-4 text-shade-60">{sp.invoice_number || '#' + sp.supplier_invoice_id}</td>
                          <td className="p-4 text-shade-60">{sp.payment_date}</td>
                          <td className="p-4 text-shade-50">
                            {sp.payment_method === 'BANK_TRANSFER' ? 'Chuyển khoản (UNC)' : 'Tiền mặt'}
                          </td>
                          <td className="p-4 text-right font-bold text-red-600">
                            -{(sp.amount || 0).toLocaleString()}đ
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}

      {/* MODAL 1: LẬP HÓA ĐƠN MUA HÀNG */}
      {showCreateInvoiceModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-4 w-full max-w-lg p-6 flex flex-col gap-4">
            <h2 className="text-base font-bold uppercase tracking-wider text-ink pb-2 border-b border-hairline-light">
              Lập Hóa đơn Mua hàng từ Phiếu nhập kho
            </h2>

            <form onSubmit={handleSubmitInvoice} className="flex flex-col gap-4 text-xs">
              <div>
                <label className="font-semibold text-shade-60">Phiếu nhập kho</label>
                <input
                  type="text"
                  value={selectedNotification?.receiptOrderNumber || selectedNotification?.receipt_number || ''}
                  disabled
                  className="w-full bg-canvas-cream border border-hairline-light rounded p-2 text-ink font-bold mt-1"
                />
              </div>

              <div>
                <label className="font-semibold text-shade-60">Nhà cung cấp</label>
                <input
                  type="text"
                  value={selectedNotification?.supplierName || selectedNotification?.supplier_name || ''}
                  disabled
                  className="w-full bg-canvas-cream border border-hairline-light rounded p-2 text-ink mt-1"
                />
              </div>

              <Input
                id="supplierInvoiceNumber"
                label="Số Hóa đơn VAT do NCC phát hành"
                value={invoiceFormData.supplierInvoiceNumber}
                onChange={e => setInvoiceFormData(prev => ({ ...prev, supplierInvoiceNumber: e.target.value }))}
                required
                placeholder="ví dụ: VAT-88392"
              />

              <div className="grid grid-cols-2 gap-4">
                <Input
                  id="documentDate"
                  label="Ngày hạch toán"
                  type="date"
                  value={invoiceFormData.documentDate}
                  onChange={e => setInvoiceFormData(prev => ({ ...prev, documentDate: e.target.value }))}
                  required
                />
                <Input
                  id="dueDate"
                  label="Hạn thanh toán"
                  type="date"
                  value={invoiceFormData.dueDate}
                  onChange={e => setInvoiceFormData(prev => ({ ...prev, dueDate: e.target.value }))}
                  required
                />
              </div>

              <div className="flex flex-col gap-1">
                <label className="font-semibold text-ink">Ghi chú</label>
                <textarea
                  value={invoiceFormData.notes}
                  onChange={e => setInvoiceFormData(prev => ({ ...prev, notes: e.target.value }))}
                  className="bg-canvas-light border border-hairline-light rounded p-2 text-ink min-h-[60px]"
                />
              </div>

              <div className="flex justify-end gap-3 mt-4 pt-3 border-t border-hairline-light">
                <Button type="button" variant="secondary" onClick={() => setShowCreateInvoiceModal(false)}>
                  Hủy bỏ
                </Button>
                <Button type="submit" variant="primary">
                  Lưu Hóa đơn & Ghi nợ
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 2: LẬP PHIẾU CHI THANH TOÁN NCC + QUÉT OCR ỦY NHIỆM CHI */}
      {showCreatePaymentModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-4 w-full max-w-xl p-6 flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
            <h2 className="text-base font-bold uppercase tracking-wider text-ink pb-2 border-b border-hairline-light flex items-center justify-between">
              <span>Lập Phiếu Chi Thanh Toán NCC</span>
              {selectedInvoiceForPayment && (
                <span className="text-xs text-shade-50 font-normal">HĐ: {selectedInvoiceForPayment?.invoice_number}</span>
              )}
            </h2>

            {/* AREA QUÉT OCR ỦY NHIỆM CHI (UNC) */}
            <div className="flex flex-col gap-2 border border-dashed border-hairline rounded p-4 bg-canvas-cream/40">
              <PhotoCaptureInput
                key={ocrResetKey}
                label="Quét ảnh ủy nhiệm chi / Giấy báo Nợ (OCR UNC)"
                output="file"
                disabled={ocrLoading}
                onChange={handleOcrFileSelected}
              />
              {ocrLoading && (
                <div className="flex items-center justify-center py-1 text-shade-50 gap-2">
                  <svg className="animate-spin h-4 w-4 text-ink" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                  </svg>
                  <span className="text-[11px] font-medium">Đang trích xuất dữ liệu UNC...</span>
                </div>
              )}
              {ocrLowConfidence && !ocrLoading && (
                <div className="flex items-start gap-2 bg-amber-50 border border-amber-200 rounded px-2.5 py-2">
                  <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                  <span className="text-[10px] text-amber-800 font-medium leading-snug">
                    Độ tin cậy OCR ({Math.round(ocrConfidence * 100)}%). Vui lòng kiểm tra lại thông tin chi trước khi lưu.
                  </span>
                </div>
              )}
            </div>

            <form onSubmit={handleSubmitPayment} className="flex flex-col gap-4 text-xs">
              {/* SELECTOR NHÀ CUNG CẤP */}
              <div className="flex flex-col gap-1">
                <label className="font-semibold text-ink">
                  Nhà cung cấp <span className="text-red-500">*</span>
                </label>
                <select
                  value={paymentFormData.supplierId}
                  onChange={e => handleSupplierChangeInPayment(e.target.value)}
                  required
                  className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none focus:border-ink"
                >
                  <option value="">-- Chọn Nhà cung cấp --</option>
                  {suppliers.map(s => (
                    <option key={s.id} value={s.id}>
                      {s.companyName || s.company_name || s.name || `NCC #${s.id}`}
                    </option>
                  ))}
                </select>
              </div>

              {/* SELECTOR HÓA ĐƠN MUA HÀNG */}
              <div className="flex flex-col gap-1">
                <label className="font-semibold text-ink">
                  Hóa đơn Mua hàng (SINV) <span className="text-red-500">*</span>
                </label>
                <select
                  value={paymentFormData.supplierInvoiceId}
                  onChange={e => handleInvoiceChangeInPayment(e.target.value)}
                  required
                  className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none focus:border-ink"
                >
                  <option value="">-- Chọn Hóa đơn Mua hàng --</option>
                  {availableInvoicesForSupplier.map(inv => {
                    const remaining = Number(inv.total_amount || inv.totalAmount || 0) - Number(inv.paid_amount || inv.paidAmount || 0);
                    return (
                      <option key={inv.id} value={inv.id}>
                        {inv.invoice_number || inv.invoiceNumber} (VAT: {inv.supplier_invoice_number || inv.supplierInvoiceNumber || 'N/A'}) - Dư nợ: {remaining.toLocaleString()}đ [{inv.status}]
                      </option>
                    );
                  })}
                </select>
                {paymentFormData.supplierId && availableInvoicesForSupplier.length === 0 && (
                  <span className="text-[11px] text-amber-700 italic">
                    Nhà cung cấp này chưa có Hóa đơn mua hàng nào cần thanh toán.
                  </span>
                )}
              </div>

              <Input
                id="amount"
                label="Số tiền thanh toán (VND)"
                type="number"
                value={paymentFormData.amount}
                onChange={e => setPaymentFormData(prev => ({ ...prev, amount: e.target.value }))}
                required
              />

              <div className="grid grid-cols-2 gap-4">
                <Input
                  id="paymentDate"
                  label="Ngày chi tiền"
                  type="date"
                  value={paymentFormData.paymentDate}
                  onChange={e => setPaymentFormData(prev => ({ ...prev, paymentDate: e.target.value }))}
                  required
                />
                <div className="flex flex-col gap-1">
                  <label className="font-semibold text-ink">Hình thức chi</label>
                  <select
                    value={paymentFormData.paymentMethod}
                    onChange={e => setPaymentFormData(prev => ({ ...prev, paymentMethod: e.target.value }))}
                    className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none"
                  >
                    <option value="BANK_TRANSFER">Chuyển khoản (ủy nhiệm chi)</option>
                    <option value="CASH">Tiền mặt</option>
                  </select>
                </div>
              </div>

              <div className="flex flex-col gap-1">
                <label className="font-semibold text-ink">Nội dung / Ghi chú UNC</label>
                <textarea
                  value={paymentFormData.notes}
                  onChange={e => setPaymentFormData(prev => ({ ...prev, notes: e.target.value }))}
                  className="bg-canvas-light border border-hairline-light rounded p-2 text-ink min-h-[60px]"
                />
              </div>

              <div className="flex justify-end gap-3 mt-4 pt-3 border-t border-hairline-light">
                <Button type="button" variant="secondary" onClick={() => setShowCreatePaymentModal(false)}>
                  Hủy bỏ
                </Button>
                <Button type="submit" variant="primary">
                  Ghi nhận Phiếu chi
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default SupplierInvoices;
