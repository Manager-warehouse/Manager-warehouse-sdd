import React, { useState, useEffect, useCallback } from 'react';
import { CheckCircle, TrendingUp, TrendingDown, Minus, Info, ChevronDown, ChevronUp } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import pricingService from '../../services/pricing.service';

const BADGE = 'text-[10px] font-semibold px-2 py-0.5 rounded-pill border uppercase';

export default function PriceApproval() {
  const { addToast } = useUiStore();
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [approving, setApproving] = useState(null);
  const [expanded, setExpanded] = useState(null);

  const fetchPending = useCallback(async () => {
    setLoading(true);
    try {
      const data = await pricingService.getAll({ status: 'PENDING' });
      setEntries(data);
    } catch (err) {
      addToast(err.message || 'Không tải được danh sách chờ duyệt', 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchPending(); }, [fetchPending]);

  const handleApprove = async (id) => {
    if (!window.confirm('Xác nhận duyệt bản giá này?')) return;
    setApproving(id);
    try {
      await pricingService.approve(id);
      addToast('Đã duyệt bản giá', 'success');
      fetchPending();
    } catch (err) {
      addToast(err.message || 'Không thể duyệt bản giá', 'error');
    } finally {
      setApproving(null);
    }
  };

  const toggle = (id) => setExpanded(prev => prev === id ? null : id);

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold tracking-tight text-ink">Duyệt bảng giá</h1>
        <p className="text-sm text-shade-50 mt-0.5">Các bản giá đang chờ phê duyệt</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="w-6 h-6 border-2 border-ink border-t-transparent rounded-full animate-spin" />
        </div>
      ) : entries.length === 0 ? (
        <div className="card-premium flex flex-col items-center justify-center py-20 text-center">
          <CheckCircle className="w-10 h-10 text-emerald-500 mb-3" />
          <p className="text-sm font-medium text-ink">Không có bản giá nào chờ duyệt</p>
          <p className="text-xs text-shade-50 mt-1">Tất cả bản giá đã được xử lý</p>
        </div>
      ) : (
        <div className="space-y-3">
          {entries.map(entry => (
            <PriceCard
              key={entry.id}
              entry={entry}
              expanded={expanded === entry.id}
              onToggle={() => toggle(entry.id)}
              onApprove={() => handleApprove(entry.id)}
              approving={approving === entry.id}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function PriceCard({ entry, expanded, onToggle, onApprove, approving }) {
  const prev = entry.previous_approved;

  return (
    <div className="card-premium overflow-hidden">
      {/* Header row */}
      <div className="flex items-center gap-4 px-6 py-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <span className="font-mono text-xs text-shade-50 bg-zinc-100 px-1.5 py-0.5 rounded">
              {entry.product_sku}
            </span>
            <span className="font-medium text-ink truncate">{entry.product_name}</span>
          </div>
          <div className="text-xs text-shade-50 mt-0.5">
            {entry.effective_date} → {entry.end_date} &nbsp;·&nbsp;
            Tạo bởi <span className="font-medium">{entry.created_by?.full_name}</span>
          </div>
        </div>

        <div className="text-right shrink-0">
          <div className="text-lg font-display font-semibold tabular-nums">
            {formatVND(entry.selling_price)}
          </div>
          <div className="text-xs text-shade-50">Giá vốn: {formatVND(entry.cost_price)}</div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button onClick={onToggle}
            className="p-2 rounded-md hover:bg-zinc-100 text-shade-50 transition-colors">
            {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
          <button onClick={onApprove} disabled={approving}
            className="btn-pill-primary text-sm px-4 py-2 disabled:opacity-50 flex items-center gap-1.5">
            <CheckCircle className="w-4 h-4" />
            {approving ? 'Đang duyệt...' : 'Phê duyệt'}
          </button>
        </div>
      </div>

      {/* Delta section */}
      {expanded && (
        <div className="border-t border-hairline-light bg-zinc-50 px-6 py-4">
          {prev ? (
            <div className="space-y-3">
              <p className="text-xs font-semibold text-shade-60 uppercase tracking-wider">
                So sánh với bản giá đã duyệt trước
              </p>
              <div className="text-xs text-shade-50 mb-2">
                Kỳ trước: {prev.effective_date} → {prev.end_date}
              </div>
              <div className="grid grid-cols-2 gap-4">
                <DeltaRow
                  label="Giá vốn"
                  prev={prev.cost_price}
                  curr={entry.cost_price}
                  delta={prev.cost_price_delta}
                  deltaPct={prev.cost_price_delta_pct}
                />
                <DeltaRow
                  label="Giá bán"
                  prev={prev.selling_price}
                  curr={entry.selling_price}
                  delta={prev.selling_price_delta}
                  deltaPct={prev.selling_price_delta_pct}
                />
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-2 text-xs text-shade-50">
              <Info className="w-4 h-4 shrink-0" />
              Chưa có bản giá đã duyệt nào cho sản phẩm này — đây là bản giá đầu tiên.
            </div>
          )}

          {entry.notes && (
            <div className="mt-3 pt-3 border-t border-hairline-light">
              <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider">Ghi chú: </span>
              <span className="text-xs text-ink">{entry.notes}</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function DeltaRow({ label, prev, curr, delta, deltaPct }) {
  const isIncrease = delta > 0;
  const isDecrease = delta < 0;

  return (
    <div className="bg-canvas-cream rounded-lg border border-hairline-light p-3 space-y-1">
      <div className="text-xs font-semibold text-shade-60 uppercase tracking-wider">{label}</div>
      <div className="flex items-end gap-2">
        <span className="text-lg font-display font-semibold tabular-nums">{formatVND(curr)}</span>
        {delta !== undefined && delta !== null && (
          <span className={`text-xs flex items-center gap-0.5 mb-0.5 font-semibold
            ${isIncrease ? 'text-red-600' : isDecrease ? 'text-emerald-700' : 'text-shade-40'}`}>
            {isIncrease ? <TrendingUp className="w-3.5 h-3.5" />
              : isDecrease ? <TrendingDown className="w-3.5 h-3.5" />
              : <Minus className="w-3.5 h-3.5" />}
            {deltaPct != null ? `${Math.abs(deltaPct).toFixed(1)}%` : ''}
          </span>
        )}
      </div>
      <div className="text-xs text-shade-40">
        Kỳ trước: {formatVND(prev)}
        {delta !== undefined && delta !== null && delta !== 0 && (
          <span className={`ml-1 ${delta > 0 ? 'text-red-500' : 'text-emerald-600'}`}>
            ({delta > 0 ? '+' : ''}{formatVND(delta)})
          </span>
        )}
      </div>
    </div>
  );
}

function formatVND(n) {
  return Number(n).toLocaleString('vi-VN') + ' đ';
}
