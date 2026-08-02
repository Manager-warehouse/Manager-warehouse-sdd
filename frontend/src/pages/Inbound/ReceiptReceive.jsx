import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, Loader2 } from 'lucide-react';
import { useUiStore } from '../../stores/ui.store';
import { inboundService } from '../../services/inbound.service';
import { masterDataService } from '../../services/masterData.service';

const emptyToZero = (value) => (value === '' || value === null || value === undefined ? 0 : Number(value));
const hasQty = (value) => value !== '' && value !== null && value !== undefined;
const qualityTotalQty = (item) => {
  if (!hasQty(item.quality_passed_qty) && !hasQty(item.quality_failed_qty)) return '';
  return emptyToZero(item.quality_passed_qty) + emptyToZero(item.quality_failed_qty);
};
const expectedQtyOf = (item) => Number(item.expected_qty ?? item.expectedQty ?? 0);
const isQualityTotalMatched = (item) => hasQty(item.actual_qty)
  && hasQty(item.quality_passed_qty)
  && hasQty(item.quality_failed_qty)
  && qualityTotalQty(item) === emptyToZero(item.actual_qty);
const isCountMatchedExpected = (item) => hasQty(item.actual_qty) && emptyToZero(item.actual_qty) === expectedQtyOf(item);
const requiresReason = (item) => emptyToZero(item.quality_failed_qty) > 0
  || (hasQty(item.actual_qty) && emptyToZero(item.actual_qty) < expectedQtyOf(item));

const STATUS_LABELS = {
  PENDING_MANAGER_APPROVAL: 'Chờ quản lý duyệt',
  REVISION_REQUIRED: 'Cần chỉnh sửa',
  PENDING_RECEIPT: 'Chờ nhận hàng',
  DRAFT: 'Đang nhận & QC',
  QC_COMPLETED: 'Đã QC',
  QC_FAILED: 'QC có hàng lỗi',
  APPROVED: 'Đã duyệt',
  PARTIALLY_APPROVED: 'Duyệt một phần',
  PUTAWAY_COMPLETED: 'Đã cất kệ',
  RETURN_TO_SUPPLIER_PENDING: 'Chờ trả NCC',
  RETURNED_TO_SUPPLIER: 'Đã trả NCC',
  CANCELLED: 'Đã hủy'
};

const normalizeItem = (item) => {
  const actual = item.actual_qty ?? item.actualQty ?? '';
  const hasActual = actual !== '' && actual !== null && actual !== undefined;
  const savedFailed = item.quality_failed_qty ?? item.qualityFailedQty ?? item.qc_failed_qty ?? item.qcFailedQty;
  const failed = hasActual ? (savedFailed ?? 0) : '';
  const savedPassed = item.quality_passed_qty
    ?? item.qualityPassedQty
    ?? item.qc_passed_qty
    ?? item.qcPassedQty;
  const passed = hasActual ? (savedPassed ?? actual) : '';

  return {
    ...item,
    actual_qty: hasActual ? actual : '',
    quality_passed_qty: passed,
    quality_failed_qty: failed,
    qc_failure_reason: item.qc_failure_reason ?? item.qcFailureReason ?? ''
  };
};

const ReceiptReceive = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();

  const [receipt, setReceipt] = useState(null);
  const [items, setItems] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchReceiptDetail();
  }, [id]);

  const fetchReceiptDetail = async () => {
    setLoading(true);
    try {
      const [data, supplierData] = await Promise.all([
        inboundService.getReceiptById(id),
        masterDataService.getSuppliers()
      ]);
      setReceipt(data);
      setSuppliers(supplierData || []);
      setItems((data.items || []).map(normalizeItem));
    } catch (e) {
      addToast('Lỗi khi tải chi tiết phiếu nhập', 'error');
      navigate('/inbound/receipts');
    } finally {
      setLoading(false);
    }
  };

  const updateItem = (itemId, updater) => {
    setItems((prev) => prev.map((item) => (
      item.receipt_item_id === itemId ? updater(item) : item
    )));
  };

  const handleQtyChange = (itemId, field, value) => {
    if (value !== '' && !/^\d+$/.test(value)) return;
    updateItem(itemId, (item) => {
      if (field === 'quality_failed_qty') {
        const failed = value === '' ? '' : Number(value);
        const updated = { ...item, quality_failed_qty: failed };
        return {
          ...updated,
          qc_failure_reason: requiresReason(updated) ? item.qc_failure_reason : ''
        };
      }
      if (field === 'quality_passed_qty') {
        const passed = value === '' ? '' : Number(value);
        const updated = { ...item, quality_passed_qty: passed };
        return {
          ...updated,
          qc_failure_reason: requiresReason(updated) ? item.qc_failure_reason : ''
        };
      }
      const updated = { ...item, [field]: value === '' ? '' : Number(value) };
      return {
        ...updated,
        qc_failure_reason: requiresReason(updated) ? item.qc_failure_reason : ''
      };
    });
  };

  const handleReasonChange = (itemId, value) => {
    updateItem(itemId, (item) => ({ ...item, qc_failure_reason: value }));
  };

  const rowState = (item) => {
    const failed = emptyToZero(item.quality_failed_qty);
    if (!hasQty(item.actual_qty) && !hasQty(item.quality_passed_qty) && !hasQty(item.quality_failed_qty)) return { type: 'pending', label: 'Chưa nhập' };
    if (!isQualityTotalMatched(item)) return { type: 'warning', label: 'QC khác số lượng đếm thực tế' };
    if (!isCountMatchedExpected(item)) return { type: 'warning', label: 'Lệch kế hoạch' };
    if (failed > 0) return { type: 'failed', label: 'Có lỗi' };
    return { type: 'passed', label: 'Đạt' };
  };

  const canSaveResult = items.length > 0
    && items.every((item) => isQualityTotalMatched(item)
      && (!requiresReason(item) || item.qc_failure_reason.trim()));

  const validateBeforeSubmit = () => {
    for (const item of items) {
      const passed = emptyToZero(item.quality_passed_qty);
      const failed = emptyToZero(item.quality_failed_qty);
      const actual = emptyToZero(item.actual_qty);
      if (!hasQty(item.actual_qty) || !hasQty(item.quality_passed_qty) || !hasQty(item.quality_failed_qty) || actual < 0 || passed < 0 || failed < 0) {
        addToast(`Vui lòng nhập số lượng hợp lệ cho ${getProductSku(item)}`, 'warning');
        return false;
      }
      if (qualityTotalQty(item) !== actual) {
        addToast(`Số lượng đạt và lỗi phải bằng số lượng đếm cho ${getProductSku(item)}`, 'warning');
        return false;
      }
      if (requiresReason(item) && !item.qc_failure_reason.trim()) {
        addToast(`Vui lòng nhập lý do lỗi hoặc thiếu cho ${getProductSku(item)}`, 'warning');
        return false;
      }
    }
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateBeforeSubmit()) return;

    const payload = {
      expectedVersion: receipt.version || 0,
      items: items.map((item) => ({
        receiptItemId: item.receipt_item_id,
        actualQty: Number(item.actual_qty),
        qualityPassedQty: Number(item.quality_passed_qty),
        qualityFailedQty: Number(item.quality_failed_qty),
        qcFailureReason: item.qc_failure_reason?.trim() || null
      }))
    };

    setSubmitting(true);
    try {
      const saved = await inboundService.receiveQcReceipt(id, payload);
      addToast(saved.status === 'QC_FAILED' ? 'Đã lưu QC có hàng lỗi' : 'Đã lưu và hoàn tất QC', 'success');
      navigate('/inbound/receipts');
    } catch (err) {
      addToast(err.response?.data?.message || err.message || 'Lỗi khi lưu nhận hàng & QC', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const getProductName = (item) => {
    if (item?.product_name) return item.product_name;
    const productId = typeof item === 'object' ? item.product_id : item;
    return productId === 1 ? 'Màn hình ASUS ProArt 27K' : 'Chuột Logitech MX Master 3S';
  };

  const getProductSku = (item) => {
    if (item?.product_sku) return item.product_sku;
    const productId = typeof item === 'object' ? item.product_id : item;
    return productId === 1 ? 'SKU-PA-001' : 'SKU-LOGI-MX3';
  };

  const getReceiptStatusLabel = (status) => STATUS_LABELS[status] || status || '-';

  const getSupplierName = () => (
    receipt.supplier_name
      || receipt.supplierName
      || suppliers.find((supplier) => supplier.id === receipt.supplier_id)?.company_name
      || suppliers.find((supplier) => supplier.id === receipt.supplierId)?.company_name
      || (receipt.supplier_id ? `NCC ID: ${receipt.supplier_id}` : '-')
  );

  const resultClass = (type) => {
    if (type === 'passed') return 'bg-success-50 text-success-800 border-success-300';
    if (type === 'failed') return 'bg-danger-50 text-danger-700 border-danger-300';
    if (type === 'warning') return 'bg-warning-50 text-warning-700 border-warning-300';
    return 'bg-canvas-cream text-shade-60 border-hairline-light';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-20">
        <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <button
          onClick={() => navigate('/inbound/receipts')}
          className="flex items-center gap-2 text-xs font-semibold text-shade-50 hover:text-ink transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại danh sách</span>
        </button>
        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Vận hành / Nhập kho
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Nhận hàng & QC đầu vào
        </h1>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-6">
        <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 shadow-level-3 card-premium">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 border-b border-hairline-light pb-2 mb-4">
            Thông tin phiếu nhập
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs font-semibold">
            <div>
              <span className="text-shade-50 block mb-0.5 font-normal">Mã phiếu nhập:</span>
              <span className="text-sm font-bold text-ink">{receipt.receipt_number}</span>
            </div>
            <div>
              <span className="text-shade-50 block mb-0.5 font-normal">Nhà cung cấp:</span>
              <span>{getSupplierName()}</span>
            </div>
            <div>
              <span className="text-shade-50 block mb-0.5 font-normal">Trạng thái:</span>
              <span>{getReceiptStatusLabel(receipt.status)}</span>
            </div>
          </div>
        </div>

        <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
          <div className="hidden lg:block overflow-x-auto">
            <table className="data-table-grid w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-canvas-cream border-b border-hairline-light">
                  {['Mã hàng', 'Tên hàng', 'SL dự kiến', 'SL thực tế', 'Hàng đạt yêu cầu', 'Không đạt yêu cầu', 'Lý do lỗi/thiếu', 'Kết quả'].map((heading) => (
                    <th key={heading} className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">
                      {heading}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {items.map((item) => {
                  const result = rowState(item);
                  return (
                    <tr key={item.receipt_item_id} className="hover:bg-canvas-cream/50 transition-colors align-top">
                      <td className="px-4 py-4 font-mono font-bold">{getProductSku(item)}</td>
                      <td className="px-4 py-4 min-w-[180px] text-shade-70">{getProductName(item)}</td>
                      <td className="px-4 py-4 text-right font-bold text-shade-60">{expectedQtyOf(item)}</td>
                      <td className="px-4 py-3"><QtyInput value={item.actual_qty} onChange={(value) => handleQtyChange(item.receipt_item_id, 'actual_qty', value)} /></td>
                      <td className="px-4 py-3"><QtyInput value={item.quality_passed_qty} onChange={(value) => handleQtyChange(item.receipt_item_id, 'quality_passed_qty', value)} /></td>
                      <td className="px-4 py-3"><QtyInput value={item.quality_failed_qty} onChange={(value) => handleQtyChange(item.receipt_item_id, 'quality_failed_qty', value)} /></td>
                      <td className="px-4 py-3 min-w-[180px]">
                        <input
                          type="text"
                          value={item.qc_failure_reason}
                          onChange={(e) => handleReasonChange(item.receipt_item_id, e.target.value)}
                          disabled={!requiresReason(item)}
                          required={requiresReason(item)}
                          className="text-input w-full py-1.5 disabled:opacity-50"
                        />
                      </td>
                      <td className="px-4 py-4">
                        <span className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] font-bold ${resultClass(result.type)}`}>
                          {result.label}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="grid grid-cols-1 gap-3 p-4 lg:hidden">
            {items.map((item) => {
              const result = rowState(item);
              return (
                <div key={item.receipt_item_id} className="rounded-lg border border-hairline-light bg-canvas-light p-4 shadow-level-3">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <span className="block font-mono text-[11px] font-bold text-ink">{getProductSku(item)}</span>
                      <span className="mt-1 block text-xs text-shade-50">{getProductName(item)}</span>
                    </div>
                    <span className={`shrink-0 rounded-full border px-2.5 py-1 text-[11px] font-bold ${resultClass(result.type)}`}>
                      {result.label}
                    </span>
                  </div>
                  <div className="mt-4 grid grid-cols-2 gap-3 text-xs">
                    <ReadonlyQty label="SL dự kiến" value={expectedQtyOf(item)} />
                    <FieldQty label="Đếm số lượng" value={item.actual_qty} onChange={(value) => handleQtyChange(item.receipt_item_id, 'actual_qty', value)} />
                    <FieldQty label="Hàng đạt yêu cầu" value={item.quality_passed_qty} onChange={(value) => handleQtyChange(item.receipt_item_id, 'quality_passed_qty', value)} />
                    <FieldQty label="Không đạt yêu cầu" value={item.quality_failed_qty} onChange={(value) => handleQtyChange(item.receipt_item_id, 'quality_failed_qty', value)} />
                  </div>
                  <label className="mt-3 flex flex-col gap-1.5">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-shade-60">Lý do lỗi/thiếu</span>
                    <input
                      type="text"
                      value={item.qc_failure_reason}
                      onChange={(e) => handleReasonChange(item.receipt_item_id, e.target.value)}
                      disabled={!requiresReason(item)}
                      required={requiresReason(item)}
                      className="text-input min-h-[40px] disabled:opacity-50"
                    />
                  </label>
                </div>
              );
            })}
          </div>
        </div>

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button type="button" onClick={() => navigate('/inbound/receipts')} className="btn-pill btn-pill-outline-light">
            Hủy
          </button>
          <button type="submit" disabled={submitting || !canSaveResult} className="btn-pill btn-pill-primary flex items-center gap-2 disabled:opacity-50">
            {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
            <span>{submitting ? 'Đang lưu...' : 'Lưu nhận hàng & QC'}</span>
          </button>
        </div>
      </form>
    </div>
  );
};

const QtyInput = ({ value, onChange }) => (
  <input
    type="number"
    min="0"
    step="1"
    value={value}
    onChange={(e) => onChange(e.target.value)}
    className="text-input text-right font-bold w-28 py-1.5 focus:ring-1 focus:ring-ink"
    required
  />
);

const ReadonlyQty = ({ label, value, valueClassName = 'text-ink' }) => (
  <div className="rounded-lg border border-hairline-light bg-canvas-cream px-3 py-2">
    <span className="block text-[10px] font-bold uppercase tracking-wider text-shade-60">{label}</span>
    <span className={`mt-1 block text-right text-sm font-bold ${valueClassName}`}>{value}</span>
  </div>
);

const FieldQty = ({ label, value, onChange }) => (
  <label className="flex flex-col gap-1.5">
    <span className="text-[10px] font-bold uppercase tracking-wider text-shade-60">{label}</span>
    <QtyInput value={value} onChange={onChange} />
  </label>
);

export default ReceiptReceive;
