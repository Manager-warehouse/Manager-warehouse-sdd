import React, { useEffect, useState } from 'react';
import { X, Loader2, History } from 'lucide-react';
import Badge from '../../components/common/Badge';
import pricingService from '../../services/pricing.service';
import { useUiStore } from '../../stores/ui.store';
import { STATUS_LABEL, STATUS_STYLE, formatVND } from './PriceListManagement';

export default function PriceHistoryModal({ product, onClose }) {
  const { addToast } = useUiStore();
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    pricingService.getByProduct(product.id)
      .then(res => {
        if (!active) return;
        // effective_date DESC, created_at as tiebreaker — newest price first
        const sorted = [...(res.entries ?? [])].sort((a, b) => {
          const keyA = `${a.effective_date ?? ''}|${a.created_at ?? ''}`;
          const keyB = `${b.effective_date ?? ''}|${b.created_at ?? ''}`;
          return keyA < keyB ? 1 : -1;
        });
        setEntries(sorted);
      })
      .catch(err => addToast(err.message || 'Không tải được lịch sử giá', 'error'))
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [product.id, addToast]);

  return (
    <div className="fixed inset-0 bg-canvas-night/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-canvas-cream rounded-lg max-w-2xl w-full border border-hairline-light shadow-level-4 overflow-hidden flex flex-col max-h-[85vh]">
        <div className="p-6 border-b border-hairline-light flex items-center justify-between bg-canvas-light">
          <div>
            <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
              Tài chính / Bảng giá
            </span>
            <h3 className="text-xl font-bold flex items-center gap-2">
              <History className="w-5 h-5 text-shade-50" />
              Lịch sử giá — {product.sku}
            </h3>
            <p className="text-xs text-shade-50 mt-1">{product.name}</p>
          </div>
          <button onClick={onClose} className="p-1 hover:bg-canvas-cream rounded-pill transition-colors text-shade-50 hover:text-ink">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="overflow-y-auto flex-1">
          {loading ? (
            <div className="flex items-center justify-center p-12">
              <Loader2 className="w-6 h-6 animate-spin text-shade-50" />
            </div>
          ) : entries.length === 0 ? (
            <div className="p-8 text-center text-sm text-shade-50">Chưa có bản giá nào cho sản phẩm này.</div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-canvas-light border-b border-hairline-light sticky top-0">
                  <th className="px-4 py-3 text-xs font-semibold text-shade-60 uppercase tracking-wider">Hiệu lực từ</th>
                  <th className="px-4 py-3 text-xs font-semibold text-shade-60 uppercase tracking-wider text-right">Giá vốn</th>
                  <th className="px-4 py-3 text-xs font-semibold text-shade-60 uppercase tracking-wider text-right">Giá bán</th>
                  <th className="px-4 py-3 text-xs font-semibold text-shade-60 uppercase tracking-wider">Trạng thái</th>
                  <th className="px-4 py-3 text-xs font-semibold text-shade-60 uppercase tracking-wider">Người tạo</th>
                  <th className="px-4 py-3 text-xs font-semibold text-shade-60 uppercase tracking-wider">Ghi chú</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {entries.map(e => (
                  <tr key={e.id} className="hover:bg-canvas-cream/50 transition-colors">
                    <td className="px-4 py-3 text-xs text-shade-60 whitespace-nowrap">{e.effective_date || '—'}</td>
                    <td className="px-4 py-3 text-xs text-shade-60 text-right tabular-nums">
                      {e.cost_price == null ? '—' : formatVND(e.cost_price)}
                    </td>
                    <td className="px-4 py-3 text-xs font-semibold text-ink text-right tabular-nums">
                      {e.selling_price == null ? '—' : formatVND(e.selling_price)}
                    </td>
                    <td className="px-4 py-3">
                      <Badge colorClassName={STATUS_STYLE[e.status]}>{STATUS_LABEL[e.status] ?? e.status}</Badge>
                    </td>
                    <td className="px-4 py-3 text-xs text-shade-50">{e.created_by?.full_name || '—'}</td>
                    <td className="px-4 py-3 text-xs text-shade-50 max-w-[160px] truncate">{e.notes || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="p-4 border-t border-hairline-light bg-canvas-cream flex justify-end">
          <button onClick={onClose} className="btn-pill btn-pill-outline-light text-xs">Đóng</button>
        </div>
      </div>
    </div>
  );
}
