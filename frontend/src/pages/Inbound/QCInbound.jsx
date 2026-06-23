import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useUiStore } from '../../stores/ui.store';
import { inboundService } from '../../services/inbound.service';
import { ArrowLeft, Loader2, ShieldCheck, ShieldAlert } from 'lucide-react';

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
    const qty = parseFloat(value);
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
    const qty = parseFloat(value);
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
      const passed = parseFloat(item.qc_passed_qty) || 0;
      const failed = parseFloat(item.qc_failed_qty) || 0;
      const actual = parseFloat(item.actual_qty) || 0;
      
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
      const passed = parseFloat(item.qc_passed_qty);
      const failed = parseFloat(item.qc_failed_qty);
      const actual = parseFloat(item.actual_qty);

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
    if (item && item.product_name) return item.product_name;
    const productId = typeof item === 'object' ? item.product_id : item;
    return productId === 1 ? 'Màn hình ASUS ProArt 27K' : 'Chuột Logitech MX Master 3S';
  };

  const getProductSku = (item) => {
    if (item && item.product_sku) return item.product_sku;
    const productId = typeof item === 'object' ? item.product_id : item;
    return productId === 1 ? 'SKU-PA-001' : 'SKU-LOGI-MX3';
  };

  const getRowResultBadge = (item) => {
    const passed = parseFloat(item.qc_passed_qty) || 0;
    const failed = parseFloat(item.qc_failed_qty) || 0;
    const actual = parseFloat(item.actual_qty) || 0;

    const baseStyle = "text-[9px] font-bold px-1.5 py-0.2 rounded border uppercase";

    if (passed + failed !== actual) {
      return <span className={`${baseStyle} bg-red-100 text-red-800 border-red-200`}>Lỗi lệch</span>;
    }
    if (passed === actual) {
      return <span className={`${baseStyle} bg-emerald-50 text-emerald-700 border-emerald-200`}>Đạt 100%</span>;
    }
    if (failed === actual) {
      return <span className={`${baseStyle} bg-red-50 text-red-700 border-red-200`}>Lỗi 100%</span>;
    }
    return <span className={`${baseStyle} bg-amber-50 text-amber-700 border-amber-200`}>Một phần</span>;
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
          Vận hành / Inbound
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Kiểm định chất lượng QC Inbound
        </h1>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-6">
        {/* Receipt header card */}
        <div className="bg-white border border-hairline-light rounded-lg p-6 shadow-sm card-premium">
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
              <span>{receipt.type === 'PURCHASE' ? 'Nhập mua (PO)' : 'Trả hàng (DO hoàn)'}</span>
            </div>
            <div>
              <span className="text-shade-50 block mb-0.5 font-normal">Trạng thái kiểm đếm:</span>
              <span className="text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded border border-blue-100">Đã đếm xong</span>
            </div>
          </div>
        </div>

        <div className="bg-white border border-hairline-light rounded-lg shadow-sm card-premium overflow-hidden">
          <div className="p-4 border-b border-hairline-light bg-zinc-50 flex items-center justify-between">
            <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40">
              Biên bản phân loại chất lượng
            </h3>
            <span className="text-[10px] text-shade-50 font-semibold italic">
              * Quy tắc: Passed Qty + Failed Qty = Actual Qty
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-zinc-50 border-b border-hairline-light">
                  <th className="px-6 py-3 font-bold text-shade-60">Sản phẩm</th>
                  <th className="px-4 py-3 font-bold text-shade-60 text-right w-24">Thực nhận</th>
                  <th className="px-4 py-3 font-bold text-shade-60 text-right w-24">Đạt QC</th>
                  <th className="px-4 py-3 font-bold text-shade-60 text-right w-24">Lỗi QC</th>
                  <th className="px-4 py-3 font-bold text-shade-60 w-44">Chi tiết lỗi (Nếu hỏng)</th>
                  <th className="px-4 py-3 font-bold text-shade-60 text-center w-24">Kết quả</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {items.map((item) => {
                  const passed = parseFloat(item.qc_passed_qty) || 0;
                  const failed = parseFloat(item.qc_failed_qty) || 0;
                  const actual = parseFloat(item.actual_qty) || 0;
                  const isMismatch = passed + failed !== actual;

                  return (
                    <tr key={item.id} className={`hover:bg-zinc-50/50 ${isMismatch ? 'bg-red-50/30' : ''}`}>
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
                          step="any"
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
                          step="any"
                          value={item.qc_failed_qty}
                          onChange={(e) => handleFailedQtyChange(item.id, e.target.value)}
                          className="text-input text-right font-bold w-20 py-1 text-red-600 border-red-200"
                          required
                        />
                      </td>
                      <td className="px-4 py-4">
                        <input
                          type="text"
                          placeholder="Móp méo, rỉ sét..."
                          value={item.qc_failure_reason || ''}
                          onChange={(e) => handleReasonChange(item.id, e.target.value)}
                          className={`text-input py-1 text-xs ${failed > 0 && !item.qc_failure_reason.trim() ? 'border-red-300 bg-red-50/20' : ''}`}
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
        </div>

        {/* Warning card for mismatch */}
        {checkValidationErrors() && (
          <div className="bg-red-50 border border-red-200 text-red-900 rounded-lg p-4 text-xs font-semibold flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 text-red-600 flex-shrink-0" />
            <span>
              Phát hiện lỗi nhập liệu: Tổng số lượng Đạt và Lỗi phải bằng số đếm thực tế của Thủ kho. Các sản phẩm có số lượng lỗi phải điền rõ lý do hư hỏng.
            </span>
          </div>
        )}

        {/* Actions */}
        <div className="flex justify-end gap-3">
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
                <span>Gửi kết quả QC</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default QCInbound;
