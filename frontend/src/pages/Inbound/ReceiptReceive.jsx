import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useUiStore } from '../../stores/ui.store';
import { inboundService } from '../../services/inbound.service';
import { masterDataService } from '../../services/masterData.service';
import { ArrowLeft, Loader2, CheckCircle2 } from 'lucide-react';

const ReceiptReceive = () => {
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
      setItems(data.items);

    } catch (e) {
      addToast('Lỗi khi tải chi tiết phiếu nhập', 'error');
      navigate('/inbound/receipts');
    } finally {
      setLoading(false);
    }
  };

  const handleQtyChange = (itemId, value) => {
    const qty = parseFloat(value);
    const updated = items.map(item => {
      if (item.receipt_item_id === itemId) {
        return { ...item, actual_qty: isNaN(qty) ? '' : qty };
      }
      return item;
    });
    setItems(updated);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validations
    for (const item of items) {
      if (item.actual_qty === null || item.actual_qty === undefined || item.actual_qty === '' || item.actual_qty < 0) {
        addToast(`Vui lòng điền số thực nhận hợp lệ cho sản phẩm ID ${item.product_id}`, 'warning');
        return;
      }
    }

    const payload = {
      items: items.map(item => ({
        receipt_item_id: item.receipt_item_id,
        counted_qty: Number(item.actual_qty)
      }))
    };

    setSubmitting(true);
    try {
      await inboundService.receiveReceipt(id, payload);
      addToast('Cập nhật số đếm thực tế thành công', 'success');
      navigate('/inbound/receipts');
    } catch (err) {
      addToast('Lỗi khi cập nhật số lượng đếm', 'error');
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
          Kiểm đếm thực tế nhận hàng
        </h1>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-6">
        {/* Receipt details header card */}
        <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 shadow-level-3 card-premium">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 border-b border-hairline-light pb-2 mb-4">
            Chi tiết chứng từ gốc
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
              <span className="text-shade-50 block mb-0.5 font-normal">Kênh tiếp nhận:</span>
              <span>{receipt.source_channel}</span>
            </div>
          </div>
        </div>

        {/* Items Table */}
        <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
          <div className="p-4 border-b border-hairline-light bg-canvas-cream">
            <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40">
              Danh sách sản phẩm kiểm đếm
            </h3>
          </div>

          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-canvas-cream border-b border-hairline-light">
                  <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Sản phẩm</th>
                  <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right w-24">Dự kiến</th>
                  <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right w-36">Thực nhận</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {items.map((item) => {
                  const sku = getProductSku(item);
                  
                  return (
                    <tr key={item.receipt_item_id} className="hover:bg-canvas-cream/50 transition-colors">
                      <td className="px-6 py-4">
                        <span className="font-bold block">{sku}</span>
                        <span className="text-shade-50 block">{getProductName(item)}</span>
                      </td>
                      <td className="px-6 py-4 text-right font-bold text-shade-60">{item.expected_qty}</td>
                      <td className="px-6 py-4 text-right">
                        <input
                          type="number"
                          min="0"
                          step="any"
                          value={item.actual_qty === null ? '' : item.actual_qty}
                          onChange={(e) => handleQtyChange(item.receipt_item_id, e.target.value)}
                          placeholder="Nhập số đếm..."
                          className="text-input text-right font-bold w-32 py-1.5 focus:ring-1 focus:ring-ink"
                          required
                        />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="flex flex-col gap-3 p-4 md:hidden">
            {items.map((item) => {
              const sku = getProductSku(item);

              return (
                <div
                  key={item.receipt_item_id}
                  className="rounded-lg border border-hairline-light bg-canvas-light p-4 shadow-level-3"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <span className="block font-mono text-[11px] font-bold text-ink">{sku}</span>
                      <span className="mt-1 block text-xs text-shade-50">{getProductName(item)}</span>
                    </div>
                    <div className="shrink-0 rounded-pill bg-canvas-cream px-3 py-1 text-right text-[11px] font-bold text-shade-60">
                      Dự kiến: {item.expected_qty}
                    </div>
                  </div>

                  <label className="mt-4 flex flex-col gap-1.5">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-shade-60">
                      Số lượng thực nhận
                    </span>
                    <input
                      type="number"
                      min="0"
                      step="any"
                      value={item.actual_qty === null ? '' : item.actual_qty}
                      onChange={(e) => handleQtyChange(item.receipt_item_id, e.target.value)}
                      placeholder="Nhập số đếm..."
                      className="text-input min-h-[44px] text-right text-base font-bold"
                      required
                    />
                  </label>
                </div>
              );
            })}
          </div>
        </div>

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
            disabled={submitting}
            className="btn-pill btn-pill-primary flex items-center gap-2 disabled:opacity-50"
          >
            {submitting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Đang lưu...
              </>
            ) : (
              <>
                <CheckCircle2 className="w-4 h-4" />
                <span>Lưu số đếm thực tế</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default ReceiptReceive;
