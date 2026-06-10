import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useUiStore } from '../../stores/ui.store';
import { inboundService } from '../../services/inbound.service';
import { masterDataService } from '../../services/masterData.service';
import { ArrowLeft, Loader2, CheckCircle2, QrCode } from 'lucide-react';

const ReceiptReceive = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();

  const [receipt, setReceipt] = useState(null);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // Serial tracking state: key is item.id, value is array of string serials
  const [serialInputs, setSerialInputs] = useState({});
  const [expandedSerials, setExpandedSerials] = useState({});

  useEffect(() => {
    fetchReceiptDetail();
  }, [id]);

  const fetchReceiptDetail = async () => {
    setLoading(true);
    try {
      const data = await inboundService.getReceiptById(id);
      setReceipt(data);
      setItems(data.items);
      
      // Initialize serial states
      const serials = {};
      data.items.forEach(item => {
        if (item.has_serial) {
          serials[item.id] = item.serial_number ? item.serial_number.split(',') : [];
        }
      });
      setSerialInputs(serials);
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
      if (item.id === itemId) {
        return { ...item, actual_qty: isNaN(qty) ? '' : qty };
      }
      return item;
    });
    setItems(updated);

    // Re-adjust serial arrays length if has_serial
    const targetItem = items.find(item => item.id === itemId);
    if (targetItem && targetItem.has_serial) {
      const newQty = isNaN(qty) ? 0 : Math.floor(qty);
      const currentSerials = serialInputs[itemId] || [];
      let updatedSerials = [...currentSerials];
      
      if (updatedSerials.length > newQty) {
        updatedSerials = updatedSerials.slice(0, newQty);
      } else {
        while (updatedSerials.length < newQty) {
          updatedSerials.push('');
        }
      }
      
      setSerialInputs({
        ...serialInputs,
        [itemId]: updatedSerials
      });
    }
  };

  const handleSerialChange = (itemId, index, value) => {
    const updated = [...(serialInputs[itemId] || [])];
    updated[index] = value.trim();
    setSerialInputs({
      ...serialInputs,
      [itemId]: updated
    });
  };

  const toggleSerialExpander = (itemId) => {
    setExpandedSerials({
      ...expandedSerials,
      [itemId]: !expandedSerials[itemId]
    });
  };

  const generateMockSerials = (itemId, sku, qty) => {
    const mockSerials = Array.from({ length: qty }, (_, i) => `SR-${sku}-${1000 + i}`);
    setSerialInputs({
      ...serialInputs,
      [itemId]: mockSerials
    });
    addToast(`Đã tự động tạo ${qty} mã Serial mẫu`, 'info');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validations
    for (const item of items) {
      if (item.actual_qty === '' || item.actual_qty < 0) {
        addToast(`Vui lòng điền số thực nhận hợp lệ cho sản phẩm ID ${item.product_id}`, 'warning');
        return;
      }

      if (item.has_serial) {
        const serials = serialInputs[item.id] || [];
        const filledSerials = serials.filter(s => s.trim() !== '');
        
        if (filledSerials.length !== Math.floor(item.actual_qty)) {
          addToast(`Sản phẩm ${item.product_id} yêu cầu nhập đủ ${Math.floor(item.actual_qty)} số Serial (Hiện tại: ${filledSerials.length})`, 'warning');
          return;
        }

        // Check duplicates
        const unique = new Set(filledSerials);
        if (unique.size !== filledSerials.length) {
          addToast(`Phát hiện mã Serial trùng lặp cho sản phẩm ID ${item.product_id}`, 'warning');
          return;
        }
      }
    }

    const payload = {
      items: items.map(item => ({
        receipt_item_id: item.id,
        actual_qty: item.actual_qty,
        serials: item.has_serial ? serialInputs[item.id] : null
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

  const getProductName = (productId) => {
    return productId === 1 ? 'Màn hình ASUS ProArt 27K' : 'Chuột Logitech MX Master 3S';
  };

  const getProductSku = (productId) => {
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
        <div className="bg-white border border-hairline-light rounded-lg p-6 shadow-sm card-premium">
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
              <span>{receipt.source_order_code || 'N/A'}</span>
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
        <div className="bg-white border border-hairline-light rounded-lg shadow-sm card-premium overflow-hidden">
          <div className="p-4 border-b border-hairline-light bg-zinc-50">
            <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40">
              Danh sách sản phẩm kiểm đếm
            </h3>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-zinc-50 border-b border-hairline-light">
                  <th className="px-6 py-3 font-bold text-shade-60">Sản phẩm</th>
                  <th className="px-6 py-3 font-bold text-shade-60 text-right w-24">Dự kiến</th>
                  <th className="px-6 py-3 font-bold text-shade-60 text-right w-36">Thực nhận</th>
                  <th className="px-6 py-3 font-bold text-shade-60 text-center w-28">Theo dõi Serial</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {items.map((item) => {
                  const sku = getProductSku(item.product_id);
                  const serials = serialInputs[item.id] || [];
                  const isExpanded = !!expandedSerials[item.id];
                  
                  return (
                    <React.Fragment key={item.id}>
                      <tr className="hover:bg-zinc-50/50">
                        <td className="px-6 py-4">
                          <span className="font-bold block">{sku}</span>
                          <span className="text-shade-50 block">{getProductName(item.product_id)}</span>
                        </td>
                        <td className="px-6 py-4 text-right font-bold text-shade-60">{item.expected_qty}</td>
                        <td className="px-6 py-4 text-right">
                          <input
                            type="number"
                            min="0"
                            step="any"
                            value={item.actual_qty === null ? '' : item.actual_qty}
                            onChange={(e) => handleQtyChange(item.id, e.target.value)}
                            placeholder="Nhập số đếm..."
                            className="text-input text-right font-bold w-32 py-1.5 focus:ring-1 focus:ring-zinc-400"
                            required
                          />
                        </td>
                        <td className="px-6 py-4 text-center whitespace-nowrap">
                          {item.has_serial ? (
                            <button
                              type="button"
                              onClick={() => toggleSerialExpander(item.id)}
                              className={`inline-flex items-center justify-center gap-1 rounded-full px-3 py-1 text-xs font-bold whitespace-nowrap transition-colors duration-150 mx-auto ${
                                isExpanded 
                                  ? 'bg-zinc-200 text-ink border border-zinc-300' 
                                  : 'bg-canvas-light text-ink border border-ink hover:bg-zinc-100'
                              }`}
                            >
                              <QrCode className="w-3.5 h-3.5" />
                              <span>Serial ({serials.filter(s => s !== '').length})</span>
                            </button>
                          ) : (
                            <span className="text-shade-40 text-[10px] italic">Không hỗ trợ</span>
                          )}
                        </td>
                      </tr>

                      {/* Expander panel for Serials */}
                      {item.has_serial && isExpanded && (
                        <tr>
                          <td colSpan="4" className="bg-zinc-50 px-6 py-4 border-t border-b border-hairline-light">
                            <div className="flex flex-col gap-3">
                              <div className="flex items-center justify-between border-b border-hairline-light pb-2">
                                <span className="text-[11px] font-bold uppercase tracking-wider text-shade-60">
                                  Khai báo danh sách {Math.floor(item.actual_qty || 0)} mã Serial
                                </span>
                                {item.actual_qty > 0 && (
                                  <button
                                    type="button"
                                    onClick={() => generateMockSerials(item.id, sku, Math.floor(item.actual_qty))}
                                    className="text-[10px] font-semibold text-indigo-600 hover:text-indigo-800 transition-colors"
                                  >
                                    + Tạo nhanh mã Serial mẫu
                                  </button>
                                )}
                              </div>

                              {item.actual_qty <= 0 ? (
                                <div className="text-xs text-shade-50 italic py-2">
                                  * Nhập số lượng thực tế nhận lớn hơn 0 trước khi khai báo Serial.
                                </div>
                              ) : (
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5">
                                  {Array.from({ length: Math.floor(item.actual_qty) }).map((_, sIdx) => (
                                    <div key={sIdx} className="flex flex-col gap-1">
                                      <span className="text-[10px] text-shade-40 font-mono">Serial #{sIdx + 1}</span>
                                      <input
                                        type="text"
                                        placeholder={`Quét/Nhập serial...`}
                                        value={serials[sIdx] || ''}
                                        onChange={(e) => handleSerialChange(item.id, sIdx, e.target.value)}
                                        className="text-input text-[11px] py-1"
                                        required
                                      />
                                    </div>
                                  ))}
                                </div>
                              )}
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>

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
