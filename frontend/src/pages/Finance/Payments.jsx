import React, { useState, useEffect, useCallback } from 'react';
import { financeService } from '../../services/finance.service';
import { masterDataService } from '../../services/masterData.service';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import { ROLES } from '../../utils/constants';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import { Landmark, Receipt, ClipboardList, ShieldAlert, CheckCircle2, TrendingUp, TrendingDown, Users, UploadCloud } from 'lucide-react';

const Payments = () => {
  const { addToast } = useUiStore();
  const { hasRole } = useAuthStore();
  const isAccountant = hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ADMIN);

  const [activeTab, setActiveTab] = useState('create');
  const [loading, setLoading] = useState(false);

  // Data States
  const [dealers, setDealers] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [paymentReceipts, setPaymentReceipts] = useState([]);
  const [agingReport, setAgingReport] = useState([]);

  // Form States
  const [formData, setFormData] = useState({
    dealerId: '',
    invoiceId: '',
    amount: '',
    paymentDate: new Date().toISOString().slice(0, 10),
    paymentMethod: 'BANK_TRANSFER',
    notes: ''
  });

  // OCR states
  const [ocrLoading, setOcrLoading] = useState(false);
  const [ocrFileName, setOcrFileName] = useState('');

  const handleOcrUpload = async (e) => {
    const file = e.target.files[0];
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
    try {
      const result = await financeService.scanPaymentReceiptOcr(file);
      setFormData(prev => ({
        ...prev,
        dealerId: result.dealer_id ? String(result.dealer_id) : '',
        amount: result.amount ? String(result.amount) : '',
        paymentDate: result.payment_date || prev.paymentDate,
        notes: result.notes || ''
      }));

      addToast(`Quét hóa đơn thành công! Độ chính xác nhận diện: ${Math.round(result.confidence_score * 100)}%`, 'success');
    } catch (err) {
      console.error('OCR failed:', err);
      addToast(err.message || 'Không thể quét hóa đơn OCR', 'error');
      setOcrFileName('');
    } finally {
      setOcrLoading(false);
    }
  };

  // Selected Invoice Info for validation & helper text
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [invoiceRemaining, setInvoiceRemaining] = useState(0);

  const loadInitialData = useCallback(async () => {
    setLoading(true);
    try {
      const dealersList = await masterDataService.getDealers();
      setDealers(dealersList || []);

      if (activeTab === 'create') {
        const paymentsList = await financeService.getPaymentReceipts();
        setPaymentReceipts(paymentsList || []);
      } else if (activeTab === 'aging') {
        const report = await financeService.getAgingReport();
        setAgingReport(report || []);
      }
    } catch (err) {
      console.error('Error loading data:', err);
      addToast('Không thể tải dữ liệu thanh toán công nợ', 'error');
    } finally {
      setLoading(false);
    }
  }, [activeTab, addToast]);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  // Load unpaid invoices when selected dealer changes
  useEffect(() => {
    if (!formData.dealerId) {
      setInvoices([]);
      return;
    }

    const loadUnpaidInvoices = async () => {
      try {
        const allInvs = await financeService.getInvoices({ dealerId: formData.dealerId });
        // Lọc hóa đơn chưa thanh toán hoặc thanh toán một phần
        setInvoices(allInvs.filter(i => i.status !== 'PAID'));
        setFormData(prev => ({ ...prev, invoiceId: '' }));
        setSelectedInvoice(null);
        setInvoiceRemaining(0);
      } catch (err) {
        console.error('Failed to load invoices:', err);
      }
    };

    loadUnpaidInvoices();
  }, [formData.dealerId]);

  // Calculate remaining amount for selected invoice
  useEffect(() => {
    if (!formData.invoiceId) {
      setSelectedInvoice(null);
      setInvoiceRemaining(0);
      return;
    }

    const inv = invoices.find(i => i.id === Number(formData.invoiceId));
    if (inv) {
      setSelectedInvoice(inv);
      // Dư nợ còn lại của hóa đơn
      // Đối với mock data, ta xem số tiền cần thu.
      // Dưới đây giả sử chưa thanh toán gì hoặc thanh toán một phần
      setInvoiceRemaining(inv.total_amount); 
    }
  }, [formData.invoiceId, invoices]);

  // Handle Form Submission
  const handleSubmitPayment = async (e) => {
    e.preventDefault();
    
    if (Number(formData.amount) <= 0) {
      addToast('Số tiền thu phải lớn hơn 0', 'error');
      return;
    }

    if (Number(formData.amount) > invoiceRemaining) {
      addToast(`Số tiền thu vượt quá dư nợ còn lại của hóa đơn (${invoiceRemaining.toLocaleString()}đ)`, 'error');
      return;
    }

    try {
      await financeService.createPaymentReceipt({
        dealerId: formData.dealerId,
        invoiceId: formData.invoiceId,
        amount: formData.amount,
        paymentDate: formData.paymentDate,
        paymentMethod: formData.paymentMethod,
        notes: formData.notes
      });

      addToast('Ghi nhận phiếu thu và cấn trừ công nợ thành công', 'success');
      
      // Reset form
      setFormData({
        dealerId: '',
        invoiceId: '',
        amount: '',
        paymentDate: new Date().toISOString().slice(0, 10),
        paymentMethod: 'BANK_TRANSFER',
        notes: ''
      });
      setSelectedInvoice(null);
      setInvoiceRemaining(0);

      loadInitialData();
    } catch (err) {
      console.error('Submit payment failed:', err);
      addToast(err.message || 'Không thể tạo phiếu thu', 'error');
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Tài chính / Công nợ
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Thu nợ & Báo cáo Phân kỳ Công nợ
        </h1>
        <p className="text-xs text-shade-50 font-light mt-1">
          Ghi nhận phiếu thu cấn trừ công nợ đại lý, tự động kích hoạt lại trạng thái tín dụng và theo dõi báo cáo công nợ phân kỳ (Aging Report).
        </p>
      </div>

      {/* Tabs Menu */}
      <div className="flex border-b border-hairline-light">
        <button
          className={`flex items-center gap-2 px-5 py-3 text-xs font-semibold uppercase tracking-wider border-b-2 transition-colors ${
            activeTab === 'create'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => setActiveTab('create')}
        >
          <Receipt className="w-4 h-4" />
          Phiếu thu công nợ
        </button>
        <button
          className={`flex items-center gap-2 px-5 py-3 text-xs font-semibold uppercase tracking-wider border-b-2 transition-colors ${
            activeTab === 'aging'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => setActiveTab('aging')}
        >
          <ClipboardList className="w-4 h-4" />
          Báo cáo phân kỳ công nợ (Aging)
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
          {activeTab === 'create' && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Lập phiếu thu form */}
              <div className="lg:col-span-1 bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 p-6 flex flex-col gap-5 h-fit">
                <div className="flex items-center gap-3 pb-3 border-b border-hairline-light">
                  <Landmark className="w-4 h-4 text-shade-60" />
                  <h2 className="text-sm font-semibold uppercase tracking-wider text-ink">Ghi nhận phiếu thu mới</h2>
                </div>

                {/* Upload OCR hóa đơn chuyển khoản */}
                <div className="flex flex-col gap-1.5 border border-dashed border-hairline rounded p-4 bg-canvas-cream/30 text-center">
                  <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block">
                    Quét hóa đơn thông minh (OCR)
                  </span>
                  {ocrLoading ? (
                    <div className="flex flex-col items-center justify-center py-2 text-shade-50 gap-2">
                      <svg className="animate-spin h-5 w-5 text-ink" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                      </svg>
                      <span className="text-[11px] font-medium">Đang nhận diện hóa đơn...</span>
                    </div>
                  ) : ocrFileName ? (
                    <div className="flex items-center justify-between bg-canvas-light border border-hairline-light rounded px-2.5 py-1.5 text-xs">
                      <span className="truncate max-w-[150px] font-medium text-ink">{ocrFileName}</span>
                      <button
                        type="button"
                        onClick={() => {
                          setOcrFileName('');
                          setFormData({
                            dealerId: '',
                            invoiceId: '',
                            amount: '',
                            paymentDate: new Date().toISOString().slice(0, 10),
                            paymentMethod: 'BANK_TRANSFER',
                            notes: ''
                          });
                        }}
                        className="text-red-500 hover:text-red-700 font-bold ml-2 text-[10px] uppercase tracking-wider"
                      >
                        Xóa
                      </button>
                    </div>
                  ) : (
                    <label className="flex flex-col items-center gap-1 cursor-pointer py-1.5 hover:bg-canvas-cream/50 rounded transition-colors">
                      <UploadCloud className="w-5 h-5 text-shade-40" />
                      <span className="text-[11px] font-semibold text-ink">
                        Chọn ảnh giao dịch chuyển khoản
                      </span>
                      <span className="text-[9px] text-shade-40">
                        Định dạng PNG, JPG (tối đa 5MB)
                      </span>
                      <input
                        type="file"
                        accept="image/png, image/jpeg, image/jpg"
                        onChange={handleOcrUpload}
                        className="hidden"
                        disabled={!isAccountant}
                      />
                    </label>
                  )}
                </div>

                <form onSubmit={handleSubmitPayment} className="flex flex-col gap-4">
                  {/* Chọn đại lý */}
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-semibold text-ink">Đại lý nộp tiền</label>
                    <select
                      value={formData.dealerId}
                      onChange={e => setFormData(prev => ({ ...prev, dealerId: e.target.value }))}
                      className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none focus:border-shade-60"
                      required
                    >
                      <option value="">-- Chọn Đại lý --</option>
                      {dealers.map(d => (
                        <option key={d.id} value={d.id}>{d.name} ({d.code})</option>
                      ))}
                    </select>
                  </div>

                  {/* Chọn hóa đơn cấn trừ */}
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-semibold text-ink">Hóa đơn cấn trừ</label>
                    <select
                      value={formData.invoiceId}
                      onChange={e => setFormData(prev => ({ ...prev, invoiceId: e.target.value }))}
                      disabled={!formData.dealerId || invoices.length === 0}
                      className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none focus:border-shade-60 disabled:opacity-50"
                      required
                    >
                      <option value="">
                        {!formData.dealerId
                          ? '-- Vui lòng chọn Đại lý trước --'
                          : invoices.length === 0
                          ? '-- Đại lý không có hóa đơn nợ --'
                          : '-- Chọn Hóa đơn nợ --'}
                      </option>
                      {invoices.map(i => (
                        <option key={i.id} value={i.id}>
                          {i.invoice_number} (Còn nợ: {i.total_amount.toLocaleString()}đ)
                        </option>
                      ))}
                    </select>
                    {selectedInvoice && (
                      <span className="text-[10px] text-shade-40 mt-1">
                        Hạn thanh toán: {selectedInvoice.due_date}
                      </span>
                    )}
                  </div>

                  {/* Số tiền thu */}
                  <div className="flex flex-col gap-1">
                    <Input
                      id="amount"
                      label="Số tiền thu nợ (VND)"
                      type="number"
                      value={formData.amount}
                      onChange={e => setFormData(prev => ({ ...prev, amount: e.target.value }))}
                      placeholder="Nhập số tiền..."
                      required
                      disabled={!formData.invoiceId}
                    />
                    {invoiceRemaining > 0 && (
                      <span className="text-[10px] text-shade-50 font-semibold mt-1">
                        Dư nợ còn lại tối đa: {invoiceRemaining.toLocaleString()} VND
                      </span>
                    )}
                  </div>

                  {/* Phương thức thanh toán */}
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-semibold text-ink">Phương thức thanh toán</label>
                    <select
                      value={formData.paymentMethod}
                      onChange={e => setFormData(prev => ({ ...prev, paymentMethod: e.target.value }))}
                      className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none focus:border-shade-60"
                      required
                    >
                      <option value="BANK_TRANSFER">Chuyển khoản ngân hàng</option>
                      <option value="CASH">Tiền mặt</option>
                    </select>
                  </div>

                  {/* Ngày thu */}
                  <Input
                    id="paymentDate"
                    label="Ngày ghi nhận thanh toán"
                    type="date"
                    value={formData.paymentDate}
                    onChange={e => setFormData(prev => ({ ...prev, paymentDate: e.target.value }))}
                    required
                  />

                  {/* Ghi chú */}
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="notes" className="text-xs font-semibold text-ink">Ghi chú phiếu thu</label>
                    <textarea
                      id="notes"
                      value={formData.notes}
                      onChange={e => setFormData(prev => ({ ...prev, notes: e.target.value }))}
                      className="bg-canvas-light text-ink text-xs border border-hairline-light rounded p-2 outline-none focus:border-shade-60 min-h-[60px]"
                      placeholder="Số tài khoản, ngân hàng, nội dung..."
                    />
                  </div>

                  <Button
                    type="submit"
                    variant="primary"
                    disabled={!isAccountant || !formData.invoiceId}
                    className="w-full mt-2"
                  >
                    Ghi nhận Phiếu thu
                  </Button>
                </form>
              </div>

              {/* Lịch sử phiếu thu */}
              <div className="lg:col-span-2 bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 flex flex-col overflow-hidden">
                <div className="p-4 bg-canvas-cream border-b border-hairline-light">
                  <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider">
                    Nhật ký phiếu thu công nợ
                  </span>
                </div>
                <div className="overflow-x-auto flex-1">
                  <table className="w-full border-collapse text-left text-xs">
                    <thead>
                      <tr className="bg-canvas-light border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                        <th className="p-4">Số phiếu thu</th>
                        <th className="p-4">Đại lý</th>
                        <th className="p-4">Khấu trừ HĐ</th>
                        <th className="p-4">Ngày thu</th>
                        <th className="p-4">Hình thức</th>
                        <th className="p-4 text-right">Số tiền</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {paymentReceipts.length === 0 ? (
                        <tr>
                          <td colSpan="6" className="p-8 text-center text-shade-40 italic">
                            Chưa ghi nhận phiếu thu công nợ nào.
                          </td>
                        </tr>
                      ) : (
                        paymentReceipts.map(payment => (
                          <tr key={payment.id} className="hover:bg-canvas-cream/50">
                            <td className="p-4 font-semibold text-ink">{payment.payment_number}</td>
                            <td className="p-4 font-medium text-ink">{payment.dealer_name}</td>
                            <td className="p-4 text-shade-60">{payment.invoice_number}</td>
                            <td className="p-4 text-shade-60">{payment.payment_date}</td>
                            <td className="p-4 text-shade-50">
                              {payment.payment_method === 'BANK_TRANSFER' ? 'Chuyển khoản' : 'Tiền mặt'}
                            </td>
                            <td className="p-4 text-right font-bold text-ink">
                              {payment.amount.toLocaleString()}đ
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'aging' && (
            <div className="flex flex-col gap-6">
              {/* Summary Cards */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-canvas-light border border-hairline-light rounded-lg p-5 flex items-center justify-between shadow-level-3">
                  <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold text-shade-50 uppercase tracking-wider">Tổng Dư nợ Hệ thống</span>
                    <span className="text-xl font-semibold text-ink">
                      {agingReport.reduce((acc, curr) => acc + (curr.current_balance || 0), 0).toLocaleString()}đ
                    </span>
                  </div>
                  <div className="p-3 rounded-full bg-red-50 text-red-600">
                    <TrendingUp className="w-5 h-5" />
                  </div>
                </div>

                <div className="bg-canvas-light border border-hairline-light rounded-lg p-5 flex items-center justify-between shadow-level-3">
                    <div className="flex flex-col gap-1">
                      <span className="text-[10px] font-bold text-shade-50 uppercase tracking-wider">Công nợ Quá hạn (&gt;30 ngày)</span>
                      <span className="text-xl font-semibold text-red-600">
                      {agingReport.reduce((acc, curr) => acc + (curr.overdue_31_to_60 || 0) + (curr.overdue_61_to_90 || 0) + (curr.overdue_over_90 || 0), 0).toLocaleString()}đ
                    </span>
                  </div>
                  <div className="p-3 rounded-full bg-yellow-50 text-yellow-600">
                    <TrendingDown className="w-5 h-5" />
                  </div>
                </div>

                <div className="bg-canvas-light border border-hairline-light rounded-lg p-5 flex items-center justify-between shadow-level-3">
                  <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold text-shade-50 uppercase tracking-wider">Đại lý bị Khóa (Credit Hold)</span>
                    <span className="text-xl font-semibold text-ink">
                      {agingReport.filter(r => r.credit_status === 'CREDIT_HOLD').length} / {agingReport.length}
                    </span>
                  </div>
                  <div className="p-3 rounded-full bg-red-100 text-red-700">
                    <Users className="w-5 h-5" />
                  </div>
                </div>
              </div>

              {/* Bảng báo cáo chi tiết */}
              <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
                <div className="p-4 bg-canvas-cream border-b border-hairline-light">
                  <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider">
                    Bảng phân tích tuổi nợ đại lý (Aging Analysis)
                  </span>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse text-left text-[11px]">
                    <thead>
                      <tr className="bg-canvas-cream/50 border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider text-[10px]">
                        <th className="p-3">Đại lý</th>
                        <th className="p-3 text-right">Hạn mức nợ</th>
                        <th className="p-3 text-right bg-canvas-cream/30">Tổng dư nợ</th>
                        <th className="p-3 text-right">Nợ trong hạn</th>
                        <th className="p-3 text-right">Quá hạn 1-30d</th>
                        <th className="p-3 text-right">Quá hạn 31-60d</th>
                        <th className="p-3 text-right">Quá hạn 61-90d</th>
                        <th className="p-3 text-right">Quá hạn &gt;90d</th>
                        <th className="p-3 text-center">Rủi ro</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {agingReport.length === 0 ? (
                        <tr>
                          <td colSpan="9" className="p-8 text-center text-shade-40 italic text-xs">
                            Không có dữ liệu công nợ đại lý.
                          </td>
                        </tr>
                      ) : (
                        agingReport.map(row => (
                          <tr key={row.dealer_id} className="hover:bg-canvas-cream/50">
                            <td className="p-3">
                              <div className="font-semibold text-ink text-xs">{row.dealer_name}</div>
                              <div className="text-[9px] text-shade-40">Mã: #{row.dealer_code} | Trạng thái: {row.credit_status}</div>
                            </td>
                            <td className="p-3 text-right font-medium text-shade-70">
                              {(row.credit_limit || 0).toLocaleString()}đ
                            </td>
                            <td className={`p-3 text-right font-bold bg-canvas-cream/20 ${row.credit_status === 'CREDIT_HOLD' ? 'text-red-600' : 'text-ink'}`}>
                              {(row.current_balance || 0).toLocaleString()}đ
                            </td>
                            <td className="p-3 text-right text-shade-60">
                              {(row.in_term_amount || 0).toLocaleString()}đ
                            </td>
                            <td className="p-3 text-right text-shade-70">
                              {(row.overdue_1_to_30 || 0).toLocaleString()}đ
                            </td>
                            <td className="p-3 text-right text-yellow-700">
                              {(row.overdue_31_to_60 || 0).toLocaleString()}đ
                            </td>
                            <td className="p-3 text-right text-red-600 font-medium">
                              {(row.overdue_61_to_90 || 0).toLocaleString()}đ
                            </td>
                            <td className="p-3 text-right text-red-700 font-bold">
                              {(row.overdue_over_90 || 0).toLocaleString()}đ
                            </td>
                            <td className="p-3 text-center">
                              <span
                                className={`px-2 py-0.5 rounded-pill text-[9px] font-bold uppercase ${
                                  row.risk_level === 'HIGH_RISK'
                                    ? 'bg-red-100 text-red-700 border border-red-200'
                                    : 'bg-aloe-10 text-ink border border-aloe-10'
                                }`}
                              >
                                {row.risk_level === 'HIGH_RISK' ? 'Rủi ro cao' : 'An toàn'}
                              </span>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default Payments;
