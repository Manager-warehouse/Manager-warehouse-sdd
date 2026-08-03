import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { financeService } from '../../services/finance.service';
import { useUiStore } from '../../stores/ui.store';
import { getLocalDateString } from '../../utils/format';
import {
  ArrowLeft, FileSpreadsheet, FileText, Landmark, ShoppingCart,
  Receipt, Wrench, Tag
} from 'lucide-react';

const TABS = [
  { key: 'invoices', label: 'Hóa đơn Bán', icon: FileText },
  { key: 'payments', label: 'Phiếu thu', icon: Landmark },
  { key: 'supplier_invoices', label: 'Hóa đơn Mua', icon: ShoppingCart },
  { key: 'supplier_payments', label: 'Phiếu chi', icon: Receipt },
  { key: 'corrections', label: 'Điều chỉnh', icon: Wrench },
  { key: 'prices', label: 'Thay đổi Giá', icon: Tag }
];

const money = (v) => `${Number(v || 0).toLocaleString()}đ`;

// AP has no credit-hold/blocking mechanism (unlike AR) - this is purely informational,
// flagging invoices past their due date so staff can prioritize payment.
const isOverdue = (inv) => inv.status !== 'PAID' && inv.due_date && inv.due_date < getLocalDateString();

const PeriodDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') || 'invoices';

  const [summary, setSummary] = useState(null);
  const [correctionVouchers, setCorrectionVouchers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [summaryData, voucherList] = await Promise.all([
        financeService.getPeriodSummary(id),
        financeService.getCorrectionVouchers()
      ]);
      setSummary(summaryData);
      // Same original_period_id semantics as PeriodClosing.jsx's vouchersForPeriod -
      // this tab shows corrections that fixed a document originally posted in this period,
      // not corrections merely dated/posted here.
      setCorrectionVouchers(
        (voucherList || []).filter(
          (v) => (v.original_period_id ?? v.originalPeriodId) === Number(id)
        )
      );
    } catch (err) {
      console.error('Failed to load period summary:', err);
      addToast('Không thể tải tổng hợp tài chính kỳ', 'error');
    } finally {
      setLoading(false);
    }
  }, [id, addToast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleExport = async () => {
    setExporting(true);
    try {
      await financeService.exportPeriodSummaryXlsx(id);
    } catch (err) {
      console.error('Export period summary failed:', err);
      addToast(err.message || 'Không thể xuất file Excel', 'error');
    } finally {
      setExporting(false);
    }
  };

  const setTab = (tab) => setSearchParams({ tab });

  if (loading || !summary) {
    return (
      <div className="flex items-center justify-center py-20 text-shade-50">
        <svg className="animate-spin h-6 w-6 text-ink mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
        </svg>
        <span>Đang tải tổng hợp tài chính kỳ...</span>
      </div>
    );
  }

  const kpis = [
    { label: 'Doanh thu (Hóa đơn Bán)', value: summary.invoice_total },
    { label: 'Đã thu', value: summary.payment_total },
    { label: 'Mua hàng (Hóa đơn Mua)', value: summary.supplier_invoice_total },
    { label: 'Đã chi', value: summary.supplier_payment_total },
    { label: 'Giá vốn (COGS)', value: summary.cogs },
    { label: 'Lợi nhuận gộp', value: summary.gross_margin }
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-3">
        <button onClick={() => navigate('/finance/periods')} className="p-2 rounded-pill hover:bg-canvas-cream text-shade-60">
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div className="flex-1">
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Tài chính / Kỳ Kế toán
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Tổng hợp Tài chính Kỳ {summary.period_name}
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            {summary.start_date} — {summary.end_date} · Trạng thái: {summary.status === 'CLOSED' ? 'Đã Khóa' : 'Đang Mở'}
          </p>
        </div>
        <button
          onClick={handleExport}
          disabled={exporting}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-pill bg-ink text-onPrimary text-xs font-semibold hover:opacity-90 disabled:opacity-50"
        >
          <FileSpreadsheet className="w-3.5 h-3.5" />
          {exporting ? 'Đang xuất...' : 'Xuất Excel'}
        </button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        {kpis.map((k) => (
          <div key={k.label} className="bg-canvas-light border border-hairline-light rounded-lg p-3 shadow-level-3">
            <span className="text-[9px] font-bold text-shade-50 uppercase tracking-wider block mb-1">{k.label}</span>
            <span className="text-sm font-bold text-ink">{money(k.value)}</span>
          </div>
        ))}
      </div>

      <div className="flex border-b border-hairline-light overflow-x-auto">
        {TABS.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            className={`flex items-center gap-2 px-4 py-3 text-xs font-semibold uppercase tracking-wider border-b-2 whitespace-nowrap transition-colors ${
              activeTab === key ? 'border-ink text-ink font-bold' : 'border-transparent text-shade-40 hover:text-ink'
            }`}
            onClick={() => setTab(key)}
          >
            <Icon className="w-3.5 h-3.5" />
            {label}
          </button>
        ))}
      </div>

      <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden overflow-x-auto">
        {activeTab === 'invoices' && (
          <table className="w-full border-collapse text-left text-xs">
            <thead>
              <tr className="bg-canvas-cream border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                <th className="p-3">Số Hóa đơn</th><th className="p-3">Đại lý</th><th className="p-3">Ngày phát hành</th>
                <th className="p-3 text-right">Tổng tiền</th><th className="p-3 text-right">Đã thu</th><th className="p-3 text-center">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline-light">
              {summary.invoices.length === 0 ? <EmptyRow cols={6} /> : summary.invoices.map((e) => (
                <tr key={e.id}>
                  <td className="p-3 font-bold text-ink">{e.invoice_number}</td>
                  <td className="p-3">{e.dealer_name}</td>
                  <td className="p-3 text-shade-60">{e.issue_date}</td>
                  <td className="p-3 text-right font-bold">{money(e.total_amount)}</td>
                  <td className="p-3 text-right">{money(e.paid_amount)}</td>
                  <td className="p-3 text-center">{e.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {activeTab === 'payments' && (
          <table className="w-full border-collapse text-left text-xs">
            <thead>
              <tr className="bg-canvas-cream border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                <th className="p-3">Số Phiếu thu</th><th className="p-3">Đại lý</th><th className="p-3">Hóa đơn</th>
                <th className="p-3">Ngày thu</th><th className="p-3 text-right">Số tiền</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline-light">
              {summary.payments.length === 0 ? <EmptyRow cols={5} /> : summary.payments.map((e) => (
                <tr key={e.id}>
                  <td className="p-3 font-bold text-ink">{e.payment_number}</td>
                  <td className="p-3">{e.dealer_name}</td>
                  <td className="p-3 text-shade-60">{e.invoice_number}</td>
                  <td className="p-3 text-shade-60">{e.payment_date}</td>
                  <td className="p-3 text-right font-bold text-emerald-600">+{money(e.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {activeTab === 'supplier_invoices' && (
          <table className="w-full border-collapse text-left text-xs">
            <thead>
              <tr className="bg-canvas-cream border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                <th className="p-3">Số Hóa đơn</th><th className="p-3">Nhà cung cấp</th><th className="p-3">Ngày phát hành</th>
                <th className="p-3">Hạn thanh toán</th>
                <th className="p-3 text-right">Tổng tiền</th><th className="p-3 text-right">Đã trả</th><th className="p-3 text-center">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline-light">
              {summary.supplier_invoices.length === 0 ? <EmptyRow cols={7} /> : summary.supplier_invoices.map((e) => (
                <tr key={e.id}>
                  <td className="p-3 font-bold text-ink">{e.invoice_number}</td>
                  <td className="p-3">{e.supplier_name}</td>
                  <td className="p-3 text-shade-60">{e.issue_date}</td>
                  <td className="p-3">
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <span className={`whitespace-nowrap ${isOverdue(e) ? 'text-red-600 font-semibold' : 'text-shade-60'}`}>{e.due_date}</span>
                      {isOverdue(e) && (
                        <span className="px-1.5 py-0.5 rounded-pill bg-red-100 text-red-700 border border-red-200 text-[9px] font-bold uppercase whitespace-nowrap">
                          Quá hạn
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="p-3 text-right font-bold">{money(e.total_amount)}</td>
                  <td className="p-3 text-right">{money(e.paid_amount)}</td>
                  <td className="p-3 text-center">{e.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {activeTab === 'supplier_payments' && (
          <table className="w-full border-collapse text-left text-xs">
            <thead>
              <tr className="bg-canvas-cream border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                <th className="p-3">Số Phiếu chi</th><th className="p-3">Nhà cung cấp</th><th className="p-3">Hóa đơn</th>
                <th className="p-3">Ngày chi</th><th className="p-3 text-right">Số tiền</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline-light">
              {summary.supplier_payments.length === 0 ? <EmptyRow cols={5} /> : summary.supplier_payments.map((e) => (
                <tr key={e.id}>
                  <td className="p-3 font-bold text-ink">{e.payment_number}</td>
                  <td className="p-3">{e.supplier_name}</td>
                  <td className="p-3 text-shade-60">{e.invoice_number}</td>
                  <td className="p-3 text-shade-60">{e.payment_date}</td>
                  <td className="p-3 text-right font-bold text-red-600">-{money(e.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {activeTab === 'corrections' && (
          <table className="w-full border-collapse text-left text-xs">
            <thead>
              <tr className="bg-canvas-cream border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                <th className="p-3">Mã Bút toán</th><th className="p-3">Tham chiếu</th><th className="p-3">Đối tượng</th>
                <th className="p-3 text-right">Số tiền</th><th className="p-3">Lý do</th><th className="p-3">Người lập</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline-light">
              {correctionVouchers.length === 0 ? <EmptyRow cols={6} /> : correctionVouchers.map((v) => (
                <tr key={v.id}>
                  <td className="p-3 font-bold text-ink">{v.adjustment_number || v.adjustmentNumber}</td>
                  <td className="p-3 text-shade-60">{v.reference_number || v.referenceNumber || `#${v.reference_id || v.referenceId}`}</td>
                  <td className="p-3">{v.dealer_name || v.dealerName || v.supplier_name || v.supplierName || '—'}</td>
                  <td className={`p-3 text-right font-bold ${Number(v.amount_delta ?? v.amountDelta) < 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                    {Number(v.amount_delta ?? v.amountDelta) > 0 ? '+' : ''}{money(v.amount_delta ?? v.amountDelta)}
                  </td>
                  <td className="p-3 text-shade-60 max-w-xs truncate" title={v.reason}>{v.reason}</td>
                  <td className="p-3 text-shade-60">{v.approved_by_name || v.approvedByName || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {activeTab === 'prices' && (
          <table className="w-full border-collapse text-left text-xs">
            <thead>
              <tr className="bg-canvas-cream border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                <th className="p-3">SKU</th><th className="p-3">Sản phẩm</th><th className="p-3">Kho</th>
                <th className="p-3">Ngày hiệu lực</th><th className="p-3 text-right">Giá vốn</th>
                <th className="p-3 text-right">Giá bán</th><th className="p-3">Người duyệt</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline-light">
              {summary.price_changes.length === 0 ? <EmptyRow cols={7} /> : summary.price_changes.map((e) => (
                <tr key={e.id}>
                  <td className="p-3 font-bold text-ink">{e.product_sku}</td>
                  <td className="p-3">{e.product_name}</td>
                  <td className="p-3 text-shade-60">{e.warehouse_code}</td>
                  <td className="p-3 text-shade-60">{e.effective_date}</td>
                  <td className="p-3 text-right">{money(e.cost_price)}</td>
                  <td className="p-3 text-right font-bold">{money(e.selling_price)}</td>
                  <td className="p-3 text-shade-60">{e.approved_by?.full_name || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

const EmptyRow = ({ cols }) => (
  <tr><td colSpan={cols} className="p-8 text-center text-shade-40 italic">Không có dữ liệu cho mục này trong kỳ.</td></tr>
);

export default PeriodDetail;
