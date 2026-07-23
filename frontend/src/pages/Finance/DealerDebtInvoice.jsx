import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { financeService } from '../../services/finance.service';
import { masterDataService } from '../../services/masterData.service';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import { ROLES } from '../../utils/constants';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import PhotoCaptureInput from '../../components/common/PhotoCaptureInput';
import { FileText, Landmark, BellRing, ShieldAlert, Plus, Eye, Image as ImageIcon, PenTool, UploadCloud, CheckCircle2 } from 'lucide-react';

const OCR_LOW_CONFIDENCE_THRESHOLD = 0.75;

const DealerDebtInvoice = () => {
  const { addToast } = useUiStore();
  const { hasRole } = useAuthStore();
  const isAccountant = hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ADMIN);

  const [searchParams, setSearchParams] = useSearchParams();
  const initialTab = searchParams.get('tab') || 'notifications';
  const [activeTab, setActiveTab] = useState(initialTab); // 'notifications' | 'invoices' | 'payments'
  const [loading, setLoading] = useState(false);

  // Data States
  const [dealers, setDealers] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [paymentReceipts, setPaymentReceipts] = useState([]);

  // Modal States - Create Invoice from Delivery Notification
  const [showCreateInvoiceModal, setShowCreateInvoiceModal] = useState(false);
  const [selectedNotif, setSelectedNotif] = useState(null);
  const [invoiceFormData, setInvoiceFormData] = useState({
    doId: '',
    documentDate: new Date().toISOString().slice(0, 10),
    notes: ''
  });

  // Modal States - POD View
  const [showPodModal, setShowPodModal] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState(null);

  // Modal States - Create Payment Receipt & OCR
  const [showCreatePaymentModal, setShowCreatePaymentModal] = useState(false);
  const [selectedInvoiceForPayment, setSelectedInvoiceForPayment] = useState(null);
  const [paymentFormData, setPaymentFormData] = useState({
    dealerId: '',
    invoiceId: '',
    amount: '',
    paymentDate: new Date().toISOString().slice(0, 10),
    paymentMethod: 'BANK_TRANSFER',
    notes: ''
  });

  // OCR States for AR Receipt Scan
  const [ocrLoading, setOcrLoading] = useState(false);
  const [ocrFileName, setOcrFileName] = useState('');
  const [ocrConfidence, setOcrConfidence] = useState(null);
  const [ocrResetKey, setOcrResetKey] = useState(0);
  const ocrLowConfidence = ocrConfidence !== null && ocrConfidence < OCR_LOW_CONFIDENCE_THRESHOLD;

  // Sync tab with search param if changed externally
  useEffect(() => {
    const tabParam = searchParams.get('tab');
    if (tabParam && ['notifications', 'invoices', 'payments'].includes(tabParam)) {
      setActiveTab(tabParam);
    }
  }, [searchParams]);

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setSearchParams({ tab });
  };

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [dealersList, invs] = await Promise.all([
        masterDataService.getDealers(),
        financeService.getInvoices()
      ]);
      setDealers(dealersList || []);
      setInvoices(invs || []);

      if (activeTab === 'notifications') {
        const notifs = await financeService.getBillingNotifications();
        setNotifications(notifs || []);
      } else if (activeTab === 'payments') {
        const pmts = await financeService.getPaymentReceipts();
        setPaymentReceipts(pmts || []);
      }
    } catch (err) {
      console.error('Failed to load AR finance data:', err);
      addToast('Không thể tải dữ liệu hóa đơn bán hàng', 'error');
    } finally {
      setLoading(false);
    }
  }, [activeTab, addToast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Open modal to create invoice from delivery notification
  const handleOpenCreateInvoice = (notif) => {
    setSelectedNotif(notif);
    setInvoiceFormData({
      doId: notif.do_id || notif.doId,
      documentDate: new Date().toISOString().slice(0, 10),
      notes: `Lập hóa đơn bán hàng cho đơn xuất ${notif.do_number || notif.doNumber}`
    });
    setShowCreateInvoiceModal(true);
  };

  const handleSubmitInvoice = async (e) => {
    e.preventDefault();
    try {
      await financeService.createInvoice(invoiceFormData.doId, invoiceFormData.documentDate, invoiceFormData.notes);
      addToast('Lập Hóa đơn Bán hàng & Ghi nhận nợ Đại lý thành công!', 'success');
      setShowCreateInvoiceModal(false);
      loadData();
    } catch (err) {
      console.error('Create invoice failed:', err);
      addToast(err.message || 'Không thể tạo hóa đơn bán hàng', 'error');
    }
  };

  // Open modal to create payment receipt for specific invoice
  const handleOpenCreatePayment = (inv) => {
    setSelectedInvoiceForPayment(inv);
    const remaining = Number(inv.total_amount || inv.totalAmount || 0) - Number(inv.paid_amount || inv.paidAmount || 0);
    setPaymentFormData({
      dealerId: String(inv.dealer_id || inv.dealerId || ''),
      invoiceId: String(inv.id),
      amount: String(remaining > 0 ? remaining : (inv.total_amount || inv.totalAmount || 0)),
      paymentDate: new Date().toISOString().slice(0, 10),
      paymentMethod: 'BANK_TRANSFER',
      notes: `Thu tiền hóa đơn ${inv.invoice_number || inv.invoiceNumber}`
    });
    setOcrFileName('');
    setOcrConfidence(null);
    setOcrResetKey(prev => prev + 1);
    setShowCreatePaymentModal(true);
  };

  // Open modal standalone
  const handleOpenCreatePaymentStandalone = () => {
    setSelectedInvoiceForPayment(null);
    const initialDealerId = dealers.length > 0 ? String(dealers[0].id) : '';
    let initialInvoiceId = '';
    let initialAmount = '';

    if (initialDealerId) {
      const unpaidInvs = invoices.filter(inv =>
        String(inv.dealer_id || inv.dealerId) === String(initialDealerId) && inv.status !== 'PAID'
      );
      if (unpaidInvs.length > 0) {
        initialInvoiceId = String(unpaidInvs[0].id);
        const remaining = Number(unpaidInvs[0].total_amount || unpaidInvs[0].totalAmount || 0) - Number(unpaidInvs[0].paid_amount || unpaidInvs[0].paidAmount || 0);
        initialAmount = String(remaining > 0 ? remaining : (unpaidInvs[0].total_amount || unpaidInvs[0].totalAmount || 0));
      }
    }

    setPaymentFormData({
      dealerId: initialDealerId,
      invoiceId: initialInvoiceId,
      amount: initialAmount,
      paymentDate: new Date().toISOString().slice(0, 10),
      paymentMethod: 'BANK_TRANSFER',
      notes: ''
    });
    setOcrFileName('');
    setOcrConfidence(null);
    setOcrResetKey(prev => prev + 1);
    setShowCreatePaymentModal(true);
  };

  const handleDealerChangeInPayment = (dId) => {
    let autoInvoiceId = '';
    let autoAmount = '';
    if (dId) {
      const unpaidInvs = invoices.filter(inv =>
        String(inv.dealer_id || inv.dealerId) === String(dId) && inv.status !== 'PAID'
      );
      if (unpaidInvs.length > 0) {
        autoInvoiceId = String(unpaidInvs[0].id);
        const remaining = Number(unpaidInvs[0].total_amount || unpaidInvs[0].totalAmount || 0) - Number(unpaidInvs[0].paid_amount || unpaidInvs[0].paidAmount || 0);
        autoAmount = String(remaining > 0 ? remaining : (unpaidInvs[0].total_amount || unpaidInvs[0].totalAmount || 0));
      }
    }
    setPaymentFormData(prev => ({
      ...prev,
      dealerId: dId,
      invoiceId: autoInvoiceId,
      amount: autoAmount || prev.amount
    }));
  };

  const handleInvoiceChangeInPayment = (invId) => {
    const selectedInv = invoices.find(i => String(i.id) === String(invId));
    let newAmount = paymentFormData.amount;
    if (selectedInv) {
      const remaining = Number(selectedInv.total_amount || selectedInv.totalAmount || 0) - Number(selectedInv.paid_amount || selectedInv.paidAmount || 0);
      newAmount = String(remaining > 0 ? remaining : (selectedInv.total_amount || selectedInv.totalAmount || 0));
    }
    setPaymentFormData(prev => ({
      ...prev,
      invoiceId: invId,
      amount: newAmount
    }));
  };

  // OCR Upload AR Payment Receipt
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
      const result = await financeService.scanPaymentReceiptOcr(file);
      const matchedDealerId = result.dealer_id || result.dealerId ? String(result.dealer_id || result.dealerId) : paymentFormData.dealerId;

      let matchedInvoiceId = '';
      if (matchedDealerId) {
        const unpaidInvs = invoices.filter(inv =>
          String(inv.dealer_id || inv.dealerId) === String(matchedDealerId) && inv.status !== 'PAID'
        );
        if (unpaidInvs.length > 0) {
          matchedInvoiceId = String(unpaidInvs[0].id);
        }
      }

      setPaymentFormData(prev => ({
        ...prev,
        dealerId: matchedDealerId || prev.dealerId,
        invoiceId: matchedInvoiceId || prev.invoiceId,
        amount: result.amount ? String(result.amount) : prev.amount,
        paymentDate: result.payment_date || result.paymentDate || prev.paymentDate,
        notes: result.notes || prev.notes
      }));
      setOcrConfidence(result.confidence_score ?? result.confidenceScore ?? null);

      const confidencePercent = Math.round((result.confidence_score || result.confidenceScore || 0) * 100);
      if ((result.confidence_score ?? result.confidenceScore ?? 1) < OCR_LOW_CONFIDENCE_THRESHOLD) {
        addToast(`Độ tin cậy nhận diện thấp (${confidencePercent}%). Vui lòng kiểm tra kỹ số tiền thu.`, 'warning');
      } else {
        addToast(`Quét OCR hóa đơn thành công! Độ chính xác: ${confidencePercent}%`, 'success');
      }
    } catch (err) {
      console.error('OCR AR scan failed:', err);
      addToast(err.message || 'Không thể quét OCR hóa đơn thu nợ', 'error');
      setOcrFileName('');
    } finally {
      setOcrLoading(false);
    }
  };

  const handleSubmitPayment = async (e) => {
    e.preventDefault();
    if (!paymentFormData.dealerId) {
      addToast('Vui lòng chọn Đại lý nộp tiền', 'error');
      return;
    }
    if (!paymentFormData.invoiceId) {
      addToast('Vui lòng chọn Hóa đơn cần cấn trừ', 'error');
      return;
    }
    if (Number(paymentFormData.amount) <= 0) {
      addToast('Số tiền thu phải lớn hơn 0', 'error');
      return;
    }
    try {
      await financeService.createPaymentReceipt(paymentFormData);
      addToast('Ghi nhận Phiếu thu và cấn trừ công nợ thành công!', 'success');
      setShowCreatePaymentModal(false);
      loadData();
    } catch (err) {
      console.error('Create AR payment failed:', err);
      addToast(err.message || 'Không thể ghi nhận phiếu thu', 'error');
    }
  };

  const availableInvoicesForDealer = invoices.filter(inv =>
    String(inv.dealer_id || inv.dealerId) === String(paymentFormData.dealerId) &&
    inv.status !== 'PAID'
  );

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Tài chính / Quản lý Phải thu (AR)
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Hóa đơn Bán hàng & Công nợ Đại lý
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Tiếp nhận thông báo đơn xuất `DELIVERED`, lập Hóa đơn Bán hàng ghi nhận nợ Đại lý, quét OCR hóa đơn thu tiền và lập Phiếu thu cấn trừ công nợ.
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
              Ghi nhận Phiếu thu (Quét OCR)
            </Button>
          </div>
        )}
      </div>

      {/* Navigation Tabs */}
      <div className="flex border-b border-hairline-light">
        <button
          className={`flex items-center gap-2 px-5 py-3 text-xs font-semibold uppercase tracking-wider border-b-2 transition-colors ${
            activeTab === 'notifications'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => handleTabChange('notifications')}
        >
          <BellRing className="w-4 h-4" />
          Thông báo Lập Hóa đơn Bán
        </button>
        <button
          className={`flex items-center gap-2 px-5 py-3 text-xs font-semibold uppercase tracking-wider border-b-2 transition-colors ${
            activeTab === 'invoices'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => handleTabChange('invoices')}
        >
          <FileText className="w-4 h-4" />
          Hóa đơn Bán hàng (SINV)
        </button>
        <button
          className={`flex items-center gap-2 px-5 py-3 text-xs font-semibold uppercase tracking-wider border-b-2 transition-colors ${
            activeTab === 'payments'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => handleTabChange('payments')}
        >
          <Landmark className="w-4 h-4" />
          Phiếu thu AR & Quét OCR
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20 text-shade-50">
          <svg className="animate-spin h-6 w-6 text-ink mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          <span>Đang tải dữ liệu Phải thu AR...</span>
        </div>
      ) : (
        <>
          {/* TAB 1: WORKLIST THÔNG BÁO LẬP HÓA ĐƠN BÁN */}
          {activeTab === 'notifications' && (
            <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
              <div className="p-4 bg-canvas-cream border-b border-hairline-light flex items-center justify-between">
                <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider">
                  Danh sách Đơn xuất kho đã giao (DELIVERED) chờ lập Hóa đơn
                </span>
                <span className="text-[10px] bg-shade-70 text-onPrimary px-2.5 py-0.5 rounded-pill font-bold">
                  {notifications.filter(n => n.invoice_status === 'NOT_INVOICED').length} Đơn chờ
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full border-collapse text-left text-xs">
                  <thead>
                    <tr className="bg-canvas-light border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                      <th className="p-4">Số Đơn Xuất Kho (DO)</th>
                      <th className="p-4">Đại lý</th>
                      <th className="p-4">Kho xuất</th>
                      <th className="p-4">Thời gian giao</th>
                      <th className="p-4 text-right">Ước tính giá trị</th>
                      <th className="p-4 text-center">Trạng thái HĐ</th>
                      <th className="p-4 text-center">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {notifications.length === 0 ? (
                      <tr>
                        <td colSpan="7" className="p-8 text-center text-shade-40 italic">
                          Không có thông báo lập hóa đơn bán hàng nào cần xử lý.
                        </td>
                      </tr>
                    ) : (
                      notifications.map(notif => (
                        <tr key={notif.id} className="hover:bg-canvas-cream/50">
                          <td className="p-4 font-bold text-ink">{notif.do_number || notif.doNumber}</td>
                          <td className="p-4 font-medium text-ink">{notif.dealer_name || notif.dealerName}</td>
                          <td className="p-4 text-shade-60">Kho #{notif.warehouse_id || notif.warehouseId}</td>
                          <td className="p-4 text-shade-60">{notif.delivered_at || 'Mới hoàn tất'}</td>
                          <td className="p-4 text-right font-bold text-ink">
                            {(notif.total_amount_estimate || 0).toLocaleString()}đ
                          </td>
                          <td className="p-4 text-center">
                            <span className={`px-2.5 py-0.5 rounded-pill text-[9px] font-bold uppercase ${
                              notif.invoice_status === 'INVOICED'
                                ? 'bg-aloe-10 text-ink'
                                : 'bg-amber-100 text-amber-800 border border-amber-200'
                            }`}>
                              {notif.invoice_status === 'INVOICED' ? 'Đã lập HĐ' : 'Chưa lập HĐ'}
                            </span>
                          </td>
                          <td className="p-4 text-center">
                            {notif.invoice_status !== 'INVOICED' && isAccountant && (
                              <Button
                                size="sm"
                                variant="primary"
                                onClick={() => handleOpenCreateInvoice(notif)}
                              >
                                <Plus className="w-3.5 h-3.5 mr-1" />
                                Lập Hóa đơn Bán
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

          {/* TAB 2: SỔ HÓA ĐƠN BÁN HÀNG */}
          {activeTab === 'invoices' && (
            <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
              <div className="p-4 bg-canvas-cream border-b border-hairline-light flex items-center justify-between">
                <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider">
                  Sổ Hóa đơn Bán hàng & Dư nợ Phải thu Đại lý
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full border-collapse text-left text-xs">
                  <thead>
                    <tr className="bg-canvas-light border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                      <th className="p-4">Số Hóa đơn</th>
                      <th className="p-4">Số Đơn giao hàng (DO)</th>
                      <th className="p-4">Đại lý</th>
                      <th className="p-4">Ngày phát hành</th>
                      <th className="p-4">Hạn thanh toán</th>
                      <th className="p-4 text-right">Tổng số tiền</th>
                      <th className="p-4 text-center">Trạng thái</th>
                      <th className="p-4 text-center">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {invoices.length === 0 ? (
                      <tr>
                        <td colSpan="8" className="p-8 text-center text-shade-40 italic">
                          Chưa có hóa đơn bán hàng nào trong hệ thống.
                        </td>
                      </tr>
                    ) : (
                      invoices.map(inv => (
                        <tr key={inv.id} className="hover:bg-canvas-cream/50">
                          <td className="p-4 font-bold text-ink">{inv.invoice_number}</td>
                          <td className="p-4 font-semibold text-shade-70">{inv.do_number}</td>
                          <td className="p-4 font-medium text-ink">{inv.dealer_name}</td>
                          <td className="p-4 text-shade-60">{inv.issue_date}</td>
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
                              {inv.status === 'PAID' ? 'Đã thu' : inv.status === 'PARTIALLY_PAID' ? 'Thu 1 Phần' : 'Chưa thu'}
                            </span>
                          </td>
                          <td className="p-4 text-center flex items-center justify-center gap-2">
                            <button
                              onClick={() => { setSelectedInvoice(inv); setShowPodModal(true); }}
                              className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-canvas-cream text-ink text-[11px] font-semibold hover:bg-hairline-light"
                            >
                              <Eye className="w-3 h-3" />
                              POD
                            </button>
                            {inv.status !== 'PAID' && isAccountant && (
                              <Button
                                size="sm"
                                variant="secondary"
                                onClick={() => handleOpenCreatePayment(inv)}
                              >
                                <Landmark className="w-3.5 h-3.5 mr-1" />
                                Thu tiền
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

          {/* TAB 3: PHIẾU THU AR & QUÉT OCR */}
          {activeTab === 'payments' && (
            <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
              <div className="p-4 bg-canvas-cream border-b border-hairline-light flex items-center justify-between">
                <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider">
                  Nhật ký Phiếu thu Công nợ Đại lý
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full border-collapse text-left text-xs">
                  <thead>
                    <tr className="bg-canvas-light border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                      <th className="p-4">Số Phiếu Thu</th>
                      <th className="p-4">Đại lý</th>
                      <th className="p-4">Hóa đơn cấn trừ</th>
                      <th className="p-4">Ngày thu</th>
                      <th className="p-4">Hình thức</th>
                      <th className="p-4 text-right">Số tiền thu</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {paymentReceipts.length === 0 ? (
                      <tr>
                        <td colSpan="6" className="p-8 text-center text-shade-40 italic">
                          Chưa có phiếu thu công nợ đại lý nào.
                        </td>
                      </tr>
                    ) : (
                      paymentReceipts.map(pr => (
                        <tr key={pr.id} className="hover:bg-canvas-cream/50">
                          <td className="p-4 font-bold text-ink">{pr.payment_number}</td>
                          <td className="p-4 font-medium text-ink">{pr.dealer_name || 'Đại lý #' + pr.dealer_id}</td>
                          <td className="p-4 text-shade-60">{pr.invoice_number || '#' + pr.invoice_id}</td>
                          <td className="p-4 text-shade-60">{pr.payment_date}</td>
                          <td className="p-4 text-shade-50">
                            {pr.payment_method === 'BANK_TRANSFER' ? 'Chuyển khoản' : 'Tiền mặt'}
                          </td>
                          <td className="p-4 text-right font-bold text-emerald-600">
                            +{(pr.amount || 0).toLocaleString()}đ
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

      {/* MODAL 1: LẬP HÓA ĐƠN BÁN HÀNG */}
      {showCreateInvoiceModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-4 w-full max-w-lg p-6 flex flex-col gap-4">
            <h2 className="text-base font-bold uppercase tracking-wider text-ink pb-2 border-b border-hairline-light">
              Lập Hóa đơn Bán hàng từ Đơn giao hàng (DO)
            </h2>

            <form onSubmit={handleSubmitInvoice} className="flex flex-col gap-4 text-xs">
              <div>
                <label className="font-semibold text-shade-60">Số đơn giao hàng (DO)</label>
                <input
                  type="text"
                  value={selectedNotif?.do_number || selectedNotif?.doNumber || ''}
                  disabled
                  className="w-full bg-canvas-cream border border-hairline-light rounded p-2 text-ink font-bold mt-1"
                />
              </div>

              <div>
                <label className="font-semibold text-shade-60">Đại lý mua hàng</label>
                <input
                  type="text"
                  value={selectedNotif?.dealer_name || selectedNotif?.dealerName || ''}
                  disabled
                  className="w-full bg-canvas-cream border border-hairline-light rounded p-2 text-ink mt-1"
                />
              </div>

              <Input
                id="documentDate"
                label="Ngày hạch toán"
                type="date"
                value={invoiceFormData.documentDate}
                onChange={e => setInvoiceFormData(prev => ({ ...prev, documentDate: e.target.value }))}
                required
              />

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

      {/* MODAL 2: GHI NHẬN PHIẾU THU & QUÉT OCR HÓA ĐƠN */}
      {showCreatePaymentModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-4 w-full max-w-xl p-6 flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
            <h2 className="text-base font-bold uppercase tracking-wider text-ink pb-2 border-b border-hairline-light flex items-center justify-between">
              <span>Ghi nhận Phiếu Thu AR</span>
              {selectedInvoiceForPayment && (
                <span className="text-xs text-shade-50 font-normal">HĐ: {selectedInvoiceForPayment?.invoice_number}</span>
              )}
            </h2>

            {/* AREA QUÉT OCR HÓA ĐƠN CHUYỂN KHOẢN */}
            <div className="flex flex-col gap-2 border border-dashed border-hairline rounded p-4 bg-canvas-cream/40">
              <PhotoCaptureInput
                key={ocrResetKey}
                label="Quét hóa đơn chuyển khoản (OCR AR)"
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
                  <span className="text-[11px] font-medium">Đang trích xuất dữ liệu hóa đơn...</span>
                </div>
              )}
              {ocrLowConfidence && !ocrLoading && (
                <div className="flex items-start gap-2 bg-amber-50 border border-amber-200 rounded px-2.5 py-2">
                  <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                  <span className="text-[10px] text-amber-800 font-medium leading-snug">
                    Độ tin cậy OCR ({Math.round(ocrConfidence * 100)}%). Vui lòng kiểm tra lại thông tin thu trước khi lưu.
                  </span>
                </div>
              )}
            </div>

            <form onSubmit={handleSubmitPayment} className="flex flex-col gap-4 text-xs">
              {/* SELECTOR ĐẠI LÝ */}
              <div className="flex flex-col gap-1">
                <label className="font-semibold text-ink">
                  Đại lý nộp tiền <span className="text-red-500">*</span>
                </label>
                <select
                  value={paymentFormData.dealerId}
                  onChange={e => handleDealerChangeInPayment(e.target.value)}
                  required
                  className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none focus:border-ink"
                >
                  <option value="">-- Chọn Đại lý --</option>
                  {dealers.map(d => (
                    <option key={d.id} value={d.id}>
                      {d.name} ({d.code})
                    </option>
                  ))}
                </select>
              </div>

              {/* SELECTOR HÓA ĐƠN BÁN HÀNG */}
              <div className="flex flex-col gap-1">
                <label className="font-semibold text-ink">
                  Hóa đơn cấn trừ <span className="text-red-500">*</span>
                </label>
                <select
                  value={paymentFormData.invoiceId}
                  onChange={e => handleInvoiceChangeInPayment(e.target.value)}
                  required
                  className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none focus:border-ink"
                >
                  <option value="">-- Chọn Hóa đơn --</option>
                  {availableInvoicesForDealer.map(inv => {
                    const remaining = Number(inv.total_amount || inv.totalAmount || 0) - Number(inv.paid_amount || inv.paidAmount || 0);
                    return (
                      <option key={inv.id} value={inv.id}>
                        {inv.invoice_number || inv.invoiceNumber} - Dư nợ: {remaining.toLocaleString()}đ [{inv.status}]
                      </option>
                    );
                  })}
                </select>
                {paymentFormData.dealerId && availableInvoicesForDealer.length === 0 && (
                  <span className="text-[11px] text-amber-700 italic">
                    Đại lý này hiện không có Hóa đơn nợ nào cần thanh toán.
                  </span>
                )}
              </div>

              <Input
                id="amount"
                label="Số tiền thu nợ (VND)"
                type="number"
                value={paymentFormData.amount}
                onChange={e => setPaymentFormData(prev => ({ ...prev, amount: e.target.value }))}
                required
              />

              <div className="grid grid-cols-2 gap-4">
                <Input
                  id="paymentDate"
                  label="Ngày thu tiền"
                  type="date"
                  value={paymentFormData.paymentDate}
                  onChange={e => setPaymentFormData(prev => ({ ...prev, paymentDate: e.target.value }))}
                  required
                />
                <div className="flex flex-col gap-1">
                  <label className="font-semibold text-ink">Hình thức thu</label>
                  <select
                    value={paymentFormData.paymentMethod}
                    onChange={e => setPaymentFormData(prev => ({ ...prev, paymentMethod: e.target.value }))}
                    className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none"
                  >
                    <option value="BANK_TRANSFER">Chuyển khoản ngân hàng</option>
                    <option value="CASH">Tiền mặt</option>
                  </select>
                </div>
              </div>

              <div className="flex flex-col gap-1">
                <label className="font-semibold text-ink">Nội dung / Ghi chú</label>
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
                  Ghi nhận Phiếu thu
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 3: XEM BẰNG CHỨNG GIAO HÀNG POD */}
      {showPodModal && selectedInvoice && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-4 w-full max-w-lg p-6 flex flex-col gap-4 max-h-[85vh] overflow-y-auto">
            <h2 className="text-base font-bold uppercase tracking-wider text-ink pb-2 border-b border-hairline-light flex items-center justify-between">
              <span>Bằng chứng Bàn giao POD</span>
              <span className="text-xs text-shade-50">DO: {selectedInvoice.do_number}</span>
            </h2>

            <div className="flex flex-col gap-4 text-xs">
              <div className="flex items-center gap-2 p-3 bg-aloe-10/20 border border-aloe-10 rounded">
                <CheckCircle2 className="w-4 h-4 text-ink shrink-0" />
                <span>Mã OTP đã được đại lý xác nhận giao hàng thành công.</span>
              </div>

              {selectedInvoice.pod_image_url ? (
                <div className="flex flex-col gap-1">
                  <label className="font-semibold text-shade-60 flex items-center gap-1">
                    <ImageIcon className="w-3.5 h-3.5" /> Ảnh giao nhận thực tế
                  </label>
                  <img src={selectedInvoice.pod_image_url} alt="POD" className="rounded border border-hairline-light max-h-48 object-cover w-full" />
                </div>
              ) : (
                <span className="text-shade-40 italic">Không có ảnh chụp POD.</span>
              )}

              {selectedInvoice.pod_signature_url && (
                <div className="flex flex-col gap-1">
                  <label className="font-semibold text-shade-60 flex items-center gap-1">
                    <PenTool className="w-3.5 h-3.5" /> Chữ ký đại lý
                  </label>
                  <img src={selectedInvoice.pod_signature_url} alt="Signature" className="rounded border border-hairline-light max-h-24 object-contain bg-canvas-cream p-2" />
                </div>
              )}
            </div>

            <div className="flex justify-end mt-4 pt-3 border-t border-hairline-light">
              <Button variant="secondary" onClick={() => setShowPodModal(false)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DealerDebtInvoice;
