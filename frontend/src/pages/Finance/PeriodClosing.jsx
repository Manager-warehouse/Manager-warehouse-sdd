import React, { useState, useEffect, useCallback } from 'react';
import { financeService } from '../../services/finance.service';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import { ROLES } from '../../utils/constants';
import Button from '../../components/common/Button';
import { Calendar, Lock, Unlock, CheckCircle2, AlertCircle, Clock, ChevronDown, ChevronRight, Wrench } from 'lucide-react';

const REFERENCE_TYPE_LABELS = {
  INVOICE: 'Hóa đơn Bán',
  PAYMENT_RECEIPT: 'Phiếu thu AR',
  SUPPLIER_INVOICE: 'Hóa đơn Mua',
  SUPPLIER_PAYMENT: 'Phiếu chi AP'
};

const PeriodClosing = () => {
  const { addToast } = useUiStore();
  const { hasRole } = useAuthStore();
  const isAccountantManager = hasRole(ROLES.ACCOUNTANT_MANAGER) || hasRole(ROLES.ADMIN);

  const [periods, setPeriods] = useState([]);
  const [correctionVouchers, setCorrectionVouchers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [closingPeriodId, setClosingPeriodId] = useState(null);
  const [confirmModalPeriod, setConfirmModalPeriod] = useState(null);
  const [expandedPeriodId, setExpandedPeriodId] = useState(null);

  const loadPeriods = useCallback(async () => {
    setLoading(true);
    try {
      const [periodList, voucherList] = await Promise.all([
        financeService.getAccountingPeriods(),
        financeService.getCorrectionVouchers()
      ]);
      setPeriods(periodList || []);
      setCorrectionVouchers(voucherList || []);
    } catch (err) {
      console.error('Failed to load accounting periods:', err);
      addToast('Không thể tải danh sách Kỳ kế toán', 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    loadPeriods();
  }, [loadPeriods]);

  // Correction vouchers reference the CLOSED period they fixed via original_period_id -
  // distinct from accounting_period_id, which is the currently-OPEN period the voucher
  // itself was posted in. Grouping here avoids a separate backend filter for what is,
  // by design, a rare/small dataset.
  const vouchersForPeriod = (periodId) => correctionVouchers.filter(
    v => (v.original_period_id ?? v.originalPeriodId) === periodId
  );

  const handleClosePeriod = async (periodId) => {
    setClosingPeriodId(periodId);
    try {
      await financeService.closeAccountingPeriod(periodId);
      addToast('Khóa kỳ kế toán thành công!', 'success');
      setConfirmModalPeriod(null);
      loadPeriods();
    } catch (err) {
      console.error('Failed to close accounting period:', err);
      addToast(err.message || 'Không thể khóa kỳ kế toán', 'error');
    } finally {
      setClosingPeriodId(null);
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Tài chính / Kế toán Tổng hợp
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Quản lý Kỳ Kế toán & Chốt sổ
        </h1>
        <p className="text-xs text-shade-50 font-light mt-1">
          Theo dõi trạng thái các kỳ kế toán phát sinh và thực hiện chốt sổ định kỳ (Month-end Closing) để khóa dữ liệu tài chính.
        </p>
      </div>

      <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
        <div className="p-4 bg-canvas-cream border-b border-hairline-light flex items-center justify-between">
          <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider flex items-center gap-2">
            <Calendar className="w-4 h-4 text-ink" />
            Danh sách Kỳ kế toán Hệ thống
          </span>
          <span className="text-[10px] bg-shade-70 text-onPrimary px-2.5 py-0.5 rounded-pill font-bold">
            {periods.filter(p => p.status === 'OPEN').length} Kỳ đang mở (OPEN)
          </span>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-20 text-shade-50">
            <svg className="animate-spin h-6 w-6 text-ink mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            <span>Đang tải thông tin kỳ kế toán...</span>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-xs">
              <thead>
                <tr className="bg-canvas-light border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                  <th className="p-4">Tên Kỳ Kế Toán</th>
                  <th className="p-4">Ngày bắt đầu</th>
                  <th className="p-4">Ngày kết thúc</th>
                  <th className="p-4 text-center">Trạng thái</th>
                  <th className="p-4">Người khóa kỳ</th>
                  <th className="p-4">Thời gian khóa</th>
                  <th className="p-4 text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {periods.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="p-8 text-center text-shade-40 italic">
                      Chưa có kỳ kế toán nào được cấu hình trong hệ thống.
                    </td>
                  </tr>
                ) : (
                  periods.map((p) => {
                    const periodVouchers = p.status === 'CLOSED' ? vouchersForPeriod(p.id) : [];
                    const isExpanded = expandedPeriodId === p.id;
                    return (
                    <React.Fragment key={p.id}>
                    <tr className="hover:bg-canvas-cream/50">
                      <td className="p-4 font-bold text-ink flex items-center gap-2">
                        {p.status === 'CLOSED' && (
                          <button
                            onClick={() => setExpandedPeriodId(isExpanded ? null : p.id)}
                            className="text-shade-40 hover:text-ink"
                            title="Xem lịch sử bút toán điều chỉnh"
                          >
                            {isExpanded ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
                          </button>
                        )}
                        {p.period_name || p.periodName}
                        {periodVouchers.length > 0 && (
                          <span className="text-[9px] bg-shade-70 text-onPrimary px-2 py-0.5 rounded-pill font-bold">
                            {periodVouchers.length} điều chỉnh
                          </span>
                        )}
                      </td>
                      <td className="p-4 text-shade-60">{p.start_date || p.startDate}</td>
                      <td className="p-4 text-shade-60">{p.end_date || p.endDate}</td>
                      <td className="p-4 text-center">
                        <span
                          className={`px-2.5 py-0.5 rounded-pill text-[9px] font-bold uppercase inline-flex items-center gap-1 ${
                            p.status === 'CLOSED'
                              ? 'bg-aloe-10 text-ink'
                              : 'bg-amber-100 text-amber-800 border border-amber-200'
                          }`}
                        >
                          {p.status === 'CLOSED' ? (
                            <>
                              <Lock className="w-3 h-3 text-ink" />
                              Đã Khóa
                            </>
                          ) : (
                            <>
                              <Unlock className="w-3 h-3 text-amber-700" />
                              Đang Mở
                            </>
                          )}
                        </span>
                      </td>
                      <td className="p-4 text-shade-70 font-medium">
                        {p.closed_by_name || p.closedByName || (p.status === 'CLOSED' ? 'Kế toán trưởng' : '—')}
                      </td>
                      <td className="p-4 text-shade-60">
                        {p.closed_at || p.closedAt ? new Date(p.closed_at || p.closedAt).toLocaleString() : '—'}
                      </td>
                      <td className="p-4 text-center">
                        {p.status === 'OPEN' && isAccountantManager && (
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={() => setConfirmModalPeriod(p)}
                            disabled={closingPeriodId === p.id}
                          >
                            <Lock className="w-3.5 h-3.5 mr-1" />
                            Khóa kỳ kế toán
                          </Button>
                        )}
                      </td>
                    </tr>
                    {isExpanded && (
                      <tr>
                        <td colSpan="7" className="p-0 bg-canvas-cream/40">
                          <div className="p-4 flex flex-col gap-2">
                            <span className="text-[10px] font-bold text-shade-60 uppercase tracking-wider flex items-center gap-1.5">
                              <Wrench className="w-3.5 h-3.5" />
                              Bút toán Điều chỉnh cho kỳ {p.period_name || p.periodName}
                            </span>
                            {periodVouchers.length === 0 ? (
                              <span className="text-[11px] text-shade-40 italic">
                                Chưa có bút toán điều chỉnh nào phát sinh cho kỳ này.
                              </span>
                            ) : (
                              <table className="w-full border-collapse text-left text-[11px]">
                                <thead>
                                  <tr className="text-shade-50 uppercase tracking-wider">
                                    <th className="py-1.5 pr-4">Mã Bút toán</th>
                                    <th className="py-1.5 pr-4">Chứng từ gốc</th>
                                    <th className="py-1.5 pr-4">Đối tượng</th>
                                    <th className="py-1.5 pr-4 text-right">Số tiền</th>
                                    <th className="py-1.5 pr-4">Lý do</th>
                                    <th className="py-1.5 pr-4">Người lập</th>
                                  </tr>
                                </thead>
                                <tbody className="divide-y divide-hairline-light/60">
                                  {periodVouchers.map(v => (
                                    <tr key={v.id}>
                                      <td className="py-1.5 pr-4 font-bold text-ink">{v.adjustment_number || v.adjustmentNumber}</td>
                                      <td className="py-1.5 pr-4 text-shade-60">
                                        {REFERENCE_TYPE_LABELS[v.reference_type || v.referenceType] || v.reference_type} #{v.reference_id || v.referenceId}
                                      </td>
                                      <td className="py-1.5 pr-4 text-shade-70">
                                        {v.dealer_name || v.dealerName || v.supplier_name || v.supplierName || '—'}
                                      </td>
                                      <td className={`py-1.5 pr-4 text-right font-bold ${
                                        Number(v.amount_delta ?? v.amountDelta) < 0 ? 'text-emerald-600' : 'text-red-600'
                                      }`}>
                                        {Number(v.amount_delta ?? v.amountDelta) > 0 ? '+' : ''}
                                        {Number(v.amount_delta ?? v.amountDelta).toLocaleString()}đ
                                      </td>
                                      <td className="py-1.5 pr-4 text-shade-60 max-w-xs truncate" title={v.reason}>{v.reason}</td>
                                      <td className="py-1.5 pr-4 text-shade-60">{v.approved_by_name || v.approvedByName || '—'}</td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            )}
                          </div>
                        </td>
                      </tr>
                    )}
                    </React.Fragment>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* CONFIRMATION MODAL */}
      {confirmModalPeriod && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-4 w-full max-w-md p-6 flex flex-col gap-4">
            <h2 className="text-base font-bold text-ink pb-2 border-b border-hairline-light flex items-center gap-2">
              <AlertCircle className="w-5 h-5 text-amber-600" />
              Xác nhận Khóa Kỳ Kế Toán {confirmModalPeriod.period_name || confirmModalPeriod.periodName}?
            </h2>
            <p className="text-xs text-shade-60 leading-relaxed">
              Sau khi khóa kỳ kế toán này, hệ thống sẽ **nghiêm cấm** mọi thao tác tạo mới hoặc chỉnh sửa Hóa đơn bán hàng, Phiếu thu, Hóa đơn mua hàng và Phiếu chi có ngày hạch toán thuộc kỳ này.
            </p>
            <div className="flex justify-end gap-3 mt-4 pt-3 border-t border-hairline-light">
              <Button variant="secondary" onClick={() => setConfirmModalPeriod(null)}>
                Hủy bỏ
              </Button>
              <Button
                variant="primary"
                onClick={() => handleClosePeriod(confirmModalPeriod.id)}
                disabled={closingPeriodId === confirmModalPeriod.id}
              >
                {closingPeriodId === confirmModalPeriod.id ? 'Đang xử lý...' : 'Xác nhận Khóa sổ'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PeriodClosing;
