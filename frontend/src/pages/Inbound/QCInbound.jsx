import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useUiStore } from '../../stores/ui.store';
import { inboundService } from '../../services/inbound.service';
import { ArrowLeft, Loader2, ShieldCheck, ShieldAlert } from 'lucide-react';
import Badge from '../../components/common/Badge';

const QCInbound = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();

  const [receipt, setReceipt] = useState(null);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchReceiptDetail();
  }, [id]);

  const fetchReceiptDetail = async () => {
    setLoading(true);
    try {
      const data = await inboundService.getReceiptById(id);
      setReceipt(data);
      
      // Initialize items with default values
      const initialItems = data.items.map(item => ({
        ...item,
        id: item.receipt_item_id || item.id, // Fallback to item.id if receipt_item_id is not available
        qc_passed_qty: item.actual_qty, // default to all passed
        qc_failed_qty: 0,
        qc_failure_reason: '',
      }));
      setItems(initialItems);
    } catch (e) {
      addToast('Lỗi khi tải chi tiết phiếu nhập', 'error');
      navigate('/inbound/receipts');
    } finally {
      setLoading(false);
    }
  };

  const handlePassedQtyChange = (itemId, value) => {
    const qty = parseInt(value);
    const updated = items.map(item => {
      if (item.id === itemId) {
        const newPassed = isNaN(qty) ? '' : qty;
        // Auto-calculate failed quantity to match actual
        const actual = item.actual_qty || 0;
        const newFailed = isNaN(qty) ? 0 : Math.max(0, actual - qty);
        return { 
          ...item, 
          qc_passed_qty: newPassed,
          qc_failed_qty: newFailed
        };
      }
      return item;
    });
    setItems(updated);
  };

  const handleFailedQtyChange = (itemId, value) => {
    const qty = parseInt(value);
    const updated = items.map(item => {
      if (item.id === itemId) {
        const newFailed = isNaN(qty) ? '' : qty;
        // Auto-calculate passed quantity to match actual
        const actual = item.actual_qty || 0;
        const newPassed = isNaN(qty) ? 0 : Math.max(0, actual - qty);
        return { 
          ...item, 
          qc_failed_qty: newFailed,
          qc_passed_qty: newPassed
        };
      }
      return item;
    });
    setItems(updated);
  };

  const handleReasonChange = (itemId, value) => {
    const updated = items.map(item => {
      if (item.id === itemId) {
        return { ...item, qc_failure_reason: value };
      }
      return item;
    });
    setItems(updated);
  };

  const checkValidationErrors = () => {
    let hasError = false;
    items.forEach(item => {
      const passed = parseInt(item.qc_passed_qty) || 0;
      const failed = parseInt(item.qc_failed_qty) || 0;
      const actual = parseInt(item.actual_qty) || 0;
      
      if (passed + failed !== actual) {
        hasError = true;
      }
      if (failed > 0 && !item.qc_failure_reason.trim()) {
        hasError = true;
      }
    });
    return hasError;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Check errors again
    for (const item of items) {
      const passed = parseInt(item.qc_passed_qty);
      const failed = parseInt(item.qc_failed_qty);
      const actual = parseInt(item.actual_qty);

      if (isNaN(passed) || isNaN(failed)) {
        addToast('Vui lòng điền đầy đủ số lượng đạt và lỗi', 'warning');
        return;
      }

      if (passed + failed !== actual) {
        addToast(`Tổng số lượng đạt và lỗi của sản phẩm ${getProductSku(item)} phải bằng số thực nhận (${actual})`, 'warning');
        return;
      }

      if (failed > 0 && !item.qc_failure_reason.trim()) {
        addToast(`Vui lòng nhập lý do lỗi cho sản phẩm ${getProductSku(item)}`, 'warning');
        return;
      }

      if (passed < 0 || failed < 0) {
        addToast('Số lượng không được phép âm', 'warning');
        return;
      }
    }

    const payload = {
      action: 'SUBMIT',
      items: items.map(item => ({
        receipt_item_id: item.id,
        qc_passed_qty: item.qc_passed_qty,
        qc_failed_qty: item.qc_failed_qty,
        qc_failure_reason: item.qc_failed_qty > 0 ? item.qc_failure_reason : null
      }))
    };

    setSubmitting(true);
    try {
      await inboundService.qcReceipt(id, payload);
      addToast('Đã ghi nhận kết quả QC. Thủ kho xác nhận ở danh sách phiếu.', 'success');
      navigate('/inbound/receipts');
    } catch (error) {
      const serverMessage = error.response?.data?.message || error.response?.data?.error || error.message;
      const detail = serverMessage === 'QC_PASSED_FAILED_MISMATCH' ? 'Lệch số lượng đếm QC' : serverMessage;
      addToast(detail, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const getProductName = (item) => {
    return item?.product_name || 'N/A';
  };

  const getProductSku = (item) => {
    return item?.product_sku || 'N/A';
  };

  const getRowResultBadge = (item) => {
    const passed = parseInt(item.qc_passed_qty) || 0;
    const failed = parseInt(item.qc_failed_qty) || 0;
    const actual = parseInt(item.actual_qty) || 0;

    if (passed + failed !== actual) {
      return <Badge size="sm" type="danger">Lỗi lệch</Badge>;
    }
    if (passed === actual) {
      return <Badge size="sm" type="success">Đạt 100%</Badge>;
    }
    if (failed === actual) {
      return <Badge size="sm" type="danger">Lỗi 100%</Badge>;
    }
    return <Badge size="sm" type="warning">Một phần</Badge>;
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
      {/* Header section */}
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
          Kiểm định chất lượng hàng nhập
        </h1>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-6">
        {/* Receipt header card */}
        <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 shadow-level-3 card-premium">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 border-b border-hairline-light pb-2 mb-4">
            Thông tin chứng từ nhận hàng
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-xs font-semibold">
            <div>
              <span className="text-shade-50 block mb-0.5 font-normal">Mã phiếu nhập:</span>
              <span className="text-sm font-bold text-ink">{receipt.receipt_number}</span>
            </div>
            <div>
              <span className="text-shade-50 block mb-0.5 font-normal">Chứng từ gốc (PO/DO):</span>
              <span>{receipt.source_reference || receipt.source_order_code || 'N/A'}</span>
            </div>
            <div>
              <span className="text-shade-50 block mb-0.5 font-normal">Loại nhập:</span>
              <span>{receipt.type === 'PURCHASE' ? 'Nhập mua' : 'Nhập trả'}</span>
            </div>
            <div>
              <span className="text-shade-50 block mb-0.5 font-normal">Trạng thái kiểm đếm:</span>
              <span className="text-info-600 bg-info-50 px-1.5 py-0.5 rounded-pill border border-info-200">Đã đếm xong</span>
            </div>
          </div>
        </div>

        <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
          <div className="flex flex-col gap-2 border-b border-hairline-light bg-canvas-cream p-4 md:flex-row md:items-center md:justify-between">
            <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40">
              Biên bản phân loại chất lượng
            </h3>
            <span className="text-[10px] text-shade-50 font-semibold italic">
              * Quy tắc: Passed Qty + Failed Qty = Actual Qty
            </span>
          </div>

          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-canvas-cream border-b border-hairline-light">
                  <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Sản phẩm</th>
                  <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right w-24">Thực nhận</th>
                  <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right w-24">Đạt kiểm định</th>
                  <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right w-24">Hàng không đạt</th>
                  <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 w-44">Chi tiết lỗi (Nếu hỏng)</th>
                  <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-center w-24">Kết quả</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {items.map((item) => {
                  const passed = parseInt(item.qc_passed_qty) || 0;
                  const failed = parseInt(item.qc_failed_qty) || 0;
                  const actual = parseInt(item.actual_qty) || 0;
                  const isMismatch = passed + failed !== actual;

                  return (
                    <tr key={item.id} className={`hover:bg-canvas-cream/50 transition-colors ${isMismatch ? 'bg-danger-50/30' : ''}`}>
                      <td className="px-6 py-4">
                        <span className="font-bold block">{getProductSku(item)}</span>
                        <span className="text-shade-50 block">{getProductName(item)}</span>
                      </td>
                      <td className="px-4 py-4 text-right font-bold text-shade-60">{item.actual_qty}</td>
                      <td className="px-4 py-4">
                        <input
                          type="number"
                          min="0"
                          max={item.actual_qty}
                          step="1"
                          value={item.qc_passed_qty}
                          onChange={(e) => handlePassedQtyChange(item.id, e.target.value)}
                          className="text-input text-right font-bold w-20 py-1"
                          required
                        />
                      </td>
                      <td className="px-4 py-4">
                        <input
                          type="number"
                          min="0"
                          max={item.actual_qty}
                          step="1"
                          value={item.qc_failed_qty}
                          onChange={(e) => handleFailedQtyChange(item.id, e.target.value)}
                          className="text-input text-right font-bold w-20 py-1 text-danger-600 border-danger-200"
                          required
                        />
                      </td>
                      <td className="px-4 py-4">
                        <input
                          type="text"
                          placeholder="Móp méo, rỉ sét..."
                          value={item.qc_failure_reason || ''}
                          onChange={(e) => handleReasonChange(item.id, e.target.value)}
                          className={`text-input py-1 text-xs ${failed > 0 && !item.qc_failure_reason.trim() ? 'border-danger-300 bg-danger-50/20' : ''}`}
                          required={failed > 0}
                        />
                      </td>
                      <td className="px-4 py-4 text-center">
                        {getRowResultBadge(item)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="flex flex-col gap-3 p-4 md:hidden">
            {items.map((item) => {
              const passed = parseInt(item.qc_passed_qty) || 0;
              const failed = parseInt(item.qc_failed_qty) || 0;
              const actual = parseInt(item.actual_qty) || 0;
              const isMismatch = passed + failed !== actual;

              return (
                <div
                  key={item.id}
                  className={`rounded-lg border p-4 shadow-level-3 ${
                    isMismatch ? 'border-danger-200 bg-danger-50/30' : 'border-hairline-light bg-canvas-light'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <span className="block font-mono text-[11px] font-bold text-ink">{getProductSku(item)}</span>
                      <span className="mt-1 block text-xs text-shade-50">{getProductName(item)}</span>
                    </div>
                    <div className="shrink-0">{getRowResultBadge(item)}</div>
                  </div>

                  <div className="mt-4 rounded-md bg-canvas-cream px-3 py-2 text-xs font-semibold text-shade-60">
                    Thực nhận: <span className="text-ink">{item.actual_qty}</span>
                  </div>

                  <div className="mt-4 grid grid-cols-2 gap-3">
                    <label className="flex flex-col gap-1.5">
                      <span className="text-[10px] font-bold uppercase tracking-wider text-shade-60">Đạt kiểm định</span>
                      <input
                        type="number"
                        min="0"
                        max={item.actual_qty}
                        step="1"
                        value={item.qc_passed_qty}
                        onChange={(e) => handlePassedQtyChange(item.id, e.target.value)}
                        className="text-input min-h-[44px] text-right font-bold"
                        required
                      />
                    </label>

                    <label className="flex flex-col gap-1.5">
                      <span className="text-[10px] font-bold uppercase tracking-wider text-shade-60">Hàng không đạt</span>
                      <input
                        type="number"
                        min="0"
                        max={item.actual_qty}
                        step="1"
                        value={item.qc_failed_qty}
                        onChange={(e) => handleFailedQtyChange(item.id, e.target.value)}
                        className="text-input min-h-[44px] text-right font-bold text-danger-600 border-danger-200"
                        required
                      />
                    </label>
                  </div>

                  <label className="mt-3 flex flex-col gap-1.5">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-shade-60">
                      Chi tiết lỗi nếu có
                    </span>
                    <input
                      type="text"
                      placeholder="Móp méo, rỉ sét..."
                      value={item.qc_failure_reason || ''}
                      onChange={(e) => handleReasonChange(item.id, e.target.value)}
                      className={`text-input min-h-[44px] text-xs ${
                        failed > 0 && !item.qc_failure_reason.trim() ? 'border-danger-300 bg-danger-50/20' : ''
                      }`}
                      required={failed > 0}
                    />
                  </label>
                </div>
              );
            })}
          </div>
        </div>

        {/* Warning card for mismatch */}
        {checkValidationErrors() && (
          <div className="bg-danger-50 border border-danger-200 text-danger-900 rounded-lg p-4 text-xs font-semibold flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 text-danger-600 flex-shrink-0" />
            <span>
              Phát hiện lỗi nhập liệu: Tổng số lượng Đạt và Lỗi phải bằng số đếm thực tế của Thủ kho. Các sản phẩm có số lượng lỗi phải điền rõ lý do hư hỏng.
            </span>
          </div>
        )}

        {/* Actions */}
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={() => navigate('/inbound/receipts')}
            className="btn-pill btn-pill-outline-light"
          >
            Hủy
          </button>
          <button
            type="submit"
            disabled={submitting || checkValidationErrors()}
            className="btn-pill btn-pill-primary flex items-center gap-2 disabled:opacity-50"
          >
            {submitting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Đang gửi...
              </>
            ) : (
              <>
                <ShieldCheck className="w-4 h-4" />
                <span>Gửi kết quả kiểm định</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default QCInbound;
