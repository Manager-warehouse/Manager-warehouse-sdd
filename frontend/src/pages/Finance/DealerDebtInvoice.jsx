import React, { useState, useEffect, useCallback } from 'react';
import { financeService } from '../../services/finance.service';
import { masterDataService } from '../../services/masterData.service';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import { ROLES } from '../../utils/constants';
import Button from '../../components/common/Button';
import { FileText, CalendarDays, Eye, Lock, CheckCircle2, AlertTriangle, Image as ImageIcon, PenTool } from 'lucide-react';

const DealerDebtInvoice = () => {
  const { addToast } = useUiStore();
  const { user, hasRole } = useAuthStore();
  const isManager = hasRole(ROLES.ACCOUNTANT_MANAGER) || hasRole(ROLES.ADMIN);

  const [activeTab, setActiveTab] = useState('invoices');
  const [loading, setLoading] = useState(false);

  // Data States
  const [invoices, setInvoices] = useState([]);
  const [periods, setPeriods] = useState([]);
  const [dealers, setDealers] = useState([]);

  // Filters
  const [selectedDealer, setSelectedDealer] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');

  // Modal States - Xem đối chứng POD của một hóa đơn (đối chiếu hóa đơn tự động sinh)
  const [showPodModal, setShowPodModal] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState(null);

  const [showClosePeriodModal, setShowClosePeriodModal] = useState(false);
  const [selectedPeriod, setSelectedPeriod] = useState(null);
  const [closePeriodNotes, setClosePeriodNotes] = useState('');

  const loadInitialData = useCallback(async () => {
    setLoading(true);
    try {
      const dealersList = await masterDataService.getDealers();
      setDealers(dealersList || []);

      if (activeTab === 'invoices') {
        const filters = {};
        if (selectedDealer !== 'ALL') filters.dealerId = selectedDealer;
        if (selectedStatus !== 'ALL') filters.status = selectedStatus;
        const invs = await financeService.getInvoices(filters);
        setInvoices(invs || []);
      } else if (activeTab === 'periods') {
        const perds = await financeService.getAccountingPeriods();
        setPeriods(perds || []);
      }
    } catch (err) {
      console.error('Error loading finance data:', err);
      addToast('Không thể tải dữ liệu tài chính', 'error');
    } finally {
      setLoading(false);
    }
  }, [activeTab, selectedDealer, selectedStatus, addToast]);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  const openPodModal = (invoice) => {
    setSelectedInvoice(invoice);
    setShowPodModal(true);
  };

  // Handle Period Closing
  const handleOpenClosePeriodModal = (period) => {
    setSelectedPeriod(period);
    setClosePeriodNotes('');
    setShowClosePeriodModal(true);
  };

  const handleClosePeriod = async (e) => {
    e.preventDefault();
    try {
      await financeService.closeAccountingPeriod(selectedPeriod.id, closePeriodNotes);
      addToast(`Đã khóa thành công kỳ kế toán ${selectedPeriod.period_name}`, 'success');
      setShowClosePeriodModal(false);
      loadInitialData();
    } catch (err) {
      console.error('Period close failed:', err);
      addToast(err.message || 'Không thể đóng kỳ kế toán dở dang', 'error');
    }
  };

  return (
    <div className="flex flex-col gap-5 md:gap-6 min-w-0">
      <div>
        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Tài chính & Kế toán
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Hóa đơn & Công nợ Đại lý
        </h1>
        <p className="text-xs text-shade-50 font-light mt-1">
          Xem danh sách hóa đơn tự động sinh kèm bằng chứng giao hàng (POD) để đối chiếu, và quản lý các kỳ kế toán.
        </p>
      </div>

      {/* Tabs Menu */}
      <div className="grid grid-cols-2 gap-2 border-b border-hairline-light md:flex md:gap-0">
        <button
          className={`flex flex-col items-center justify-center gap-1 px-2 py-2.5 text-[10px] font-semibold uppercase leading-tight tracking-wider border-b-2 transition-colors md:flex-row md:gap-2 md:px-5 md:py-3 md:text-xs ${
            activeTab === 'invoices'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => setActiveTab('invoices')}
        >
          <FileText className="w-4 h-4" />
          <span className="md:hidden">Hóa đơn</span>
          <span className="hidden md:inline">Danh sách hóa đơn</span>
        </button>
        <button
          className={`flex flex-col items-center justify-center gap-1 px-2 py-2.5 text-[10px] font-semibold uppercase leading-tight tracking-wider border-b-2 transition-colors md:flex-row md:gap-2 md:px-5 md:py-3 md:text-xs ${
            activeTab === 'periods'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => setActiveTab('periods')}
        >
          <CalendarDays className="w-4 h-4" />
          <span className="md:hidden">Kỳ kế toán</span>
          <span className="hidden md:inline">Kỳ kế toán & Khóa sổ</span>
        </button>
      </div>

      {/* Main Content Area */}
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
          {activeTab === 'invoices' && (
            <div className="flex flex-col gap-4">
              {/* Filters Panel */}
              <div className="bg-canvas-cream border border-hairline-light rounded-lg p-3 md:p-4 flex flex-col gap-3 md:flex-row md:flex-wrap md:items-center md:gap-4">
                <div className="flex flex-col gap-1 md:min-w-[200px]">
                  <label className="text-[10px] font-bold text-shade-60 uppercase tracking-widest">Lọc Đại lý</label>
                  <select
                    value={selectedDealer}
                    onChange={e => setSelectedDealer(e.target.value)}
                    className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none focus:border-shade-60 w-full"
                  >
                    <option value="ALL">Tất cả Đại lý</option>
                    {dealers.map(d => (
                      <option key={d.id} value={d.id}>{d.name}</option>
                    ))}
                  </select>
                </div>
                <div className="flex flex-col gap-1 md:min-w-[150px]">
                  <label className="text-[10px] font-bold text-shade-60 uppercase tracking-widest">Trạng thái HĐ</label>
                  <select
                    value={selectedStatus}
                    onChange={e => setSelectedStatus(e.target.value)}
                    className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none focus:border-shade-60 w-full"
                  >
                    <option value="ALL">Tất cả Trạng thái</option>
                    <option value="UNPAID">Chưa thanh toán</option>
                    <option value="PARTIALLY_PAID">Thanh toán một phần</option>
                    <option value="PAID">Đã thanh toán</option>
                  </select>
                </div>
                <div className="md:ml-auto md:pt-4">
                  <Button variant="outline-light" onClick={loadInitialData}>Tìm kiếm</Button>
                </div>
              </div>

              {/* Invoices List Table */}
              <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
                <div className="flex flex-col gap-3 p-3 md:hidden">
                  {invoices.length === 0 ? (
                    <div className="p-6 text-center text-xs text-shade-40 italic">
                      Không tìm thấy hóa đơn nào phù hợp với bộ lọc.
                    </div>
                  ) : (
                    invoices.map(inv => (
                      <div key={inv.id} className="rounded-lg border border-hairline-light bg-canvas-light p-3">
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <div className="font-semibold text-sm text-ink truncate">{inv.invoice_number}</div>
                            <div className="text-[10px] text-shade-50 truncate">DO: {inv.do_number}</div>
                          </div>
                          <span
                            className={`shrink-0 px-2 py-0.5 rounded-pill text-[9px] font-bold uppercase border ${
                              inv.status === 'PAID'
                                ? 'bg-aloe-10 text-ink border-aloe-10'
                                : inv.status === 'PARTIALLY_PAID'
                                ? 'bg-yellow-100 text-yellow-800 border-yellow-200'
                                : 'bg-red-50 text-red-700 border-red-200'
                            }`}
                          >
                            {inv.status === 'PAID' ? 'Đã thu' : inv.status === 'PARTIALLY_PAID' ? 'Trả một phần' : 'Chưa trả'}
                          </span>
                        </div>
                        <div className="mt-3 grid grid-cols-2 gap-2 text-[11px] text-shade-60">
                          <div className="col-span-2 font-medium text-ink truncate">{inv.dealer_name}</div>
                          <div>Phát hành: {inv.issue_date}</div>
                          <div>Hạn trả: {inv.due_date}</div>
                          <div className="col-span-2 text-right text-sm font-semibold text-ink">
                            {(inv.total_amount || 0).toLocaleString()}đ
                          </div>
                        </div>
                        <div className="mt-3 flex justify-end">
                          <button
                            onClick={() => openPodModal(inv)}
                            className="inline-flex min-h-10 items-center justify-center gap-1 rounded-md bg-aloe-10 px-3 text-[11px] font-semibold text-ink"
                          >
                            <Eye className="w-3.5 h-3.5" />
                            Xem POD
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
                <div className="hidden md:block overflow-x-auto">
                  <table className="w-full border-collapse text-left text-xs">
                    <thead>
                      <tr className="bg-canvas-light border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                        <th className="p-4">Số hóa đơn</th>
                        <th className="p-4">Số đơn giao hàng</th>
                        <th className="p-4">Đại lý</th>
                        <th className="p-4">Ngày phát hành</th>
                        <th className="p-4">Hạn thanh toán</th>
                        <th className="p-4 text-right">Tổng số tiền</th>
                        <th className="p-4 text-center">Trạng thái</th>
                        <th className="p-4 text-right">Bằng chứng POD</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {invoices.length === 0 ? (
                        <tr>
                          <td colSpan="8" className="p-8 text-center text-shade-40 italic">
                            Không tìm thấy hóa đơn nào phù hợp với bộ lọc.
                          </td>
                        </tr>
                      ) : (
                        invoices.map(inv => (
                          <tr key={inv.id} className="hover:bg-canvas-cream/50">
                            <td className="p-4 font-semibold text-ink">{inv.invoice_number}</td>
                            <td className="p-4 text-shade-60">{inv.do_number}</td>
                            <td className="p-4 font-medium text-ink">{inv.dealer_name}</td>
                            <td className="p-4 text-shade-60">{inv.issue_date}</td>
                            <td className="p-4 text-shade-60">{inv.due_date}</td>
                            <td className="p-4 text-right font-semibold text-ink">
                              {(inv.total_amount || 0).toLocaleString()}đ
                            </td>
                            <td className="p-4 text-center">
                              <span
                                className={`px-2 py-0.5 rounded-pill text-[9px] font-bold uppercase border ${
                                  inv.status === 'PAID'
                                    ? 'bg-aloe-10 text-ink border-aloe-10'
                                    : inv.status === 'PARTIALLY_PAID'
                                    ? 'bg-yellow-100 text-yellow-800 border-yellow-200'
                                    : 'bg-red-50 text-red-700 border-red-200'
                                }`}
                              >
                                {inv.status === 'PAID' ? 'Đã thu tiền' : inv.status === 'PARTIALLY_PAID' ? 'Trả một phần' : 'Chưa trả'}
                              </span>
                            </td>
                            <td className="p-4 text-right">
                              <button
                                onClick={() => openPodModal(inv)}
                                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-pill bg-aloe-10 text-ink hover:opacity-80 transition-opacity font-semibold text-[10px]"
                              >
                                <Eye className="w-3 h-3" />
                                Xem POD
                              </button>
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

          {activeTab === 'periods' && (
            <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
              <div className="p-4 bg-canvas-cream border-b border-hairline-light flex flex-col gap-1 md:flex-row md:justify-between md:items-center">
                <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider">
                  Kỳ kế toán hệ thống WMS
                </span>
                <span className="text-[10px] text-shade-40 italic">
                  * Hệ thống chặn mọi chỉnh sửa khi kỳ kế toán ở trạng thái CLOSED
                </span>
              </div>
              <div className="flex flex-col gap-3 p-3 md:hidden">
                {periods.map(period => (
                  <div key={period.id} className="rounded-lg border border-hairline-light bg-canvas-light p-3">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <div className="font-semibold text-sm text-ink">{period.period_name}</div>
                        <div className="text-[11px] text-shade-50 mt-1">
                          {period.start_date} - {period.end_date}
                        </div>
                      </div>
                      <span
                        className={`shrink-0 px-2 py-0.5 rounded-pill text-[9px] font-bold uppercase border ${
                          period.status === 'CLOSED'
                            ? 'bg-red-50 text-red-700 border-red-200'
                            : 'bg-aloe-10 text-ink border-aloe-10'
                        }`}
                      >
                        {period.status === 'CLOSED' ? 'Đã đóng' : 'OPEN'}
                      </span>
                    </div>
                    <div className="mt-3 text-[11px] text-shade-60">
                      Chốt: {period.closed_at ? new Date(period.closed_at).toLocaleString('vi-VN') : 'Chưa chốt'}
                    </div>
                    <div className="mt-1 text-[11px] text-shade-60">
                      Người chốt: {period.closed_by_name || '—'}
                    </div>
                    <div className="mt-3 flex justify-end">
                      {period.status === 'OPEN' ? (
                        <Button
                          variant="outline-light"
                          size="sm"
                          icon={Lock}
                          onClick={() => handleOpenClosePeriodModal(period)}
                          disabled={!isManager}
                        >
                          Khóa sổ
                        </Button>
                      ) : (
                        <span className="text-shade-40 inline-flex items-center gap-1 text-[11px] font-medium">
                          <CheckCircle2 className="w-3.5 h-3.5 text-shade-40" />
                          Đã khóa cứng
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
              <div className="hidden md:block overflow-x-auto">
                <table className="w-full border-collapse text-left text-xs">
                  <thead>
                    <tr className="bg-canvas-light border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                      <th className="p-4">Tên kỳ</th>
                      <th className="p-4">Ngày bắt đầu</th>
                      <th className="p-4">Ngày kết thúc</th>
                      <th className="p-4 text-center">Trạng thái</th>
                      <th className="p-4">Thời điểm chốt</th>
                      <th className="p-4">Người chốt</th>
                      <th className="p-4 text-right">Khóa kỳ</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {periods.map(period => (
                      <tr key={period.id} className="hover:bg-canvas-cream/50">
                        <td className="p-4 font-semibold text-ink">{period.period_name}</td>
                        <td className="p-4 text-shade-60">{period.start_date}</td>
                        <td className="p-4 text-shade-60">{period.end_date}</td>
                        <td className="p-4 text-center">
                          <span
                            className={`px-2 py-0.5 rounded-pill text-[9px] font-bold uppercase border ${
                              period.status === 'CLOSED'
                                ? 'bg-red-50 text-red-700 border-red-200'
                                : 'bg-aloe-10 text-ink border-aloe-10'
                            }`}
                          >
                            {period.status === 'CLOSED' ? 'Đã đóng' : 'Đang mở (OPEN)'}
                          </span>
                        </td>
                        <td className="p-4 text-shade-50">
                          {period.closed_at ? new Date(period.closed_at).toLocaleString('vi-VN') : '—'}
                        </td>
                        <td className="p-4 text-shade-60 font-medium">{period.closed_by_name || '—'}</td>
                        <td className="p-4 text-right">
                          {period.status === 'OPEN' ? (
                            <Button
                              variant="outline-light"
                              size="sm"
                              icon={Lock}
                              onClick={() => handleOpenClosePeriodModal(period)}
                              disabled={!isManager}
                            >
                              Khóa sổ
                            </Button>
                          ) : (
                            <span className="text-shade-40 inline-flex items-center gap-1 text-[11px] font-medium pr-3">
                              <CheckCircle2 className="w-3.5 h-3.5 text-shade-40" />
                              Đã khóa cứng
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}

      {/* --- MODAL: XEM ĐỐI CHỨNG POD (đối chiếu hóa đơn tự động sinh) --- */}
      {showPodModal && selectedInvoice && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-4 max-w-lg w-full overflow-hidden flex flex-col">
            <div className="p-5 border-b border-hairline-light bg-canvas-cream flex justify-between items-center">
              <h3 className="font-semibold text-sm uppercase tracking-wider text-ink">Bằng chứng bàn giao (Proof of Delivery - DO: {selectedInvoice.do_number})</h3>
              <button onClick={() => setShowPodModal(false)} className="text-shade-40 hover:text-ink font-bold text-sm">✕</button>
            </div>
            <div className="p-6 flex flex-col gap-6 overflow-y-auto max-h-[70vh]">
              {/* Lịch sử OTP */}
              <div className="flex flex-col gap-2 p-3 bg-aloe-10/10 border border-aloe-10/30 rounded-md">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-ink" />
                  <span className="text-xs font-semibold text-ink uppercase tracking-wider">Xác nhận bằng mã OTP</span>
                </div>
                <div className="text-xs text-shade-70 font-light mt-1">
                  Đại lý đã nhập mã xác thực thành công lúc: <strong className="text-ink font-semibold">{new Date(selectedInvoice.otp_verified_at).toLocaleString('vi-VN')}</strong>
                </div>
              </div>

              {/* Ảnh chụp POD */}
              <div className="flex flex-col gap-2">
                <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest flex items-center gap-1.5">
                  <ImageIcon className="w-3.5 h-3.5" />
                  Ảnh chụp thực tế lúc giao hàng
                </span>
                <div className="border border-hairline-light rounded overflow-hidden aspect-video bg-canvas-cream relative flex items-center justify-center">
                  {selectedInvoice.pod_image_url ? (
                    <img src={selectedInvoice.pod_image_url} alt="Ảnh biên bản giao nhận" className="object-cover w-full h-full" />
                  ) : (
                    <span className="text-shade-40 text-xs italic">Không tìm thấy ảnh bàn giao</span>
                  )}
                </div>
              </div>

              {/* Chữ ký đại lý */}
              <div className="flex flex-col gap-2">
                <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest flex items-center gap-1.5">
                  <PenTool className="w-3.5 h-3.5" />
                  Chữ ký điện tử của Đại lý
                </span>
                <div className="border border-hairline-light rounded bg-canvas-cream p-4 h-24 flex items-center justify-center">
                  {selectedInvoice.pod_signature_url ? (
                    <img src={selectedInvoice.pod_signature_url} alt="Signature" className="h-full object-contain filter grayscale" />
                  ) : (
                    <span className="text-shade-40 text-xs italic">Ký nhận tại quầy</span>
                  )}
                </div>
              </div>

              <div className="text-[11px] text-shade-40 italic text-center">
                Thời gian tài xế cập nhật biên bản giao nhận: {new Date(selectedInvoice.pod_timestamp).toLocaleString('vi-VN')}
              </div>
            </div>
            <div className="p-4 border-t border-hairline-light bg-canvas-cream flex justify-end">
              <Button variant="outline-light" onClick={() => setShowPodModal(false)}>Đóng đối soát</Button>
            </div>
          </div>
        </div>
      )}

      {/* --- MODAL: KHÓA SỔ KỲ KẾ TOÁN --- */}
      {showClosePeriodModal && selectedPeriod && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn">
          <form onSubmit={handleClosePeriod} className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-4 max-w-md w-full overflow-hidden flex flex-col">
            <div className="p-5 border-b border-hairline-light bg-canvas-cream flex justify-between items-center">
              <h3 className="font-semibold text-sm uppercase tracking-wider text-ink flex items-center gap-2">
                <Lock className="w-4 h-4 text-red-600" />
                Khóa sổ Kỳ kế toán: {selectedPeriod.period_name}
              </h3>
              <button type="button" onClick={() => setShowClosePeriodModal(false)} className="text-shade-40 hover:text-ink font-bold text-sm">✕</button>
            </div>
            <div className="p-6 flex flex-col gap-4">
              <div className="text-xs text-shade-70 leading-relaxed font-light">
                Bạn đang chuẩn bị thực hiện <strong>KHÓA SỔ</strong> kỳ kế toán từ ngày <strong className="text-ink">{selectedPeriod.start_date}</strong> đến ngày <strong className="text-ink">{selectedPeriod.end_date}</strong>.
              </div>

              <div className="flex flex-col gap-1.5">
                <label htmlFor="periodCloseNotes" className="text-xs font-semibold text-ink">Ghi chú khóa sổ (Bắt buộc)</label>
                <textarea
                  id="periodCloseNotes"
                  value={closePeriodNotes}
                  onChange={e => setClosePeriodNotes(e.target.value)}
                  className="bg-canvas-light text-ink text-xs border border-hairline-light rounded p-3 outline-none focus:border-shade-60 min-h-[80px]"
                  placeholder="Nhập lý do chốt sổ hoặc ghi chú..."
                  required
                />
              </div>

              <div className="flex items-start gap-2 p-3 bg-red-50 border border-red-200 rounded text-[11px] text-red-800 font-light mt-2">
                <AlertTriangle className="w-4 h-4 flex-shrink-0" />
                <span>
                  <strong>HÀNH ĐỘNG CÓ RỦI RO CAO:</strong> Khóa sổ kỳ kế toán sẽ <strong>KHÓA CỨNG</strong> vĩnh viễn toàn bộ chứng từ (Nhập, Xuất, Điều chuyển, Kiểm kê, Thanh toán) thuộc kỳ này. Không thể đảo ngược thao tác này.
                </span>
              </div>
            </div>
            <div className="p-4 border-t border-hairline-light bg-canvas-cream flex justify-end gap-3">
              <Button type="button" variant="outline-light" onClick={() => setShowClosePeriodModal(false)}>Hủy bỏ</Button>
              <Button type="submit" variant="primary" className="bg-red-600 hover:bg-red-700 text-white border-red-600">Xác nhận Khóa sổ</Button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

export default DealerDebtInvoice;
