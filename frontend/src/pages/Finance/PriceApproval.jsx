import React, { useState, useEffect, useCallback } from 'react';
import { CheckCircle, TrendingUp, TrendingDown, Minus, Info, ChevronDown, ChevronUp, Check, Loader2, ClipboardCheck } from 'lucide-react';
import Pagination from '../../components/common/Pagination';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import pricingService from '../../services/pricing.service';

export default function PriceApproval() {
  const { addToast } = useUiStore();
  const activeWarehouse = useAuthStore(s => s.activeWarehouse);
  const warehouseId = activeWarehouse?.id;
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [approving, setApproving] = useState(null);
  const [expanded, setExpanded] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const fetchPending = useCallback(async () => {
    if (!warehouseId) return;
    setLoading(true);
    try {
      const data = await pricingService.getAll({ status: 'PENDING', warehouse_id: warehouseId });
      setEntries(data);
    } catch (err) {
      addToast(err.message || 'Không tải được danh sách chờ duyệt', 'error');
    } finally {
      setLoading(false);
    }
  }, [warehouseId]);

  useEffect(() => { fetchPending(); }, [fetchPending]);

  const handleApprove = async (id) => {
    if (!window.confirm('Xác nhận duyệt bản giá này?')) return;
    setApproving(id);
    try {
      await pricingService.approve(id);
      addToast('Đã duyệt bản giá thành công', 'success');
      fetchPending();
    } catch (err) {
      addToast(err.message || 'Không thể duyệt bản giá', 'error');
    } finally {
      setApproving(null);
    }
  };

  const toggle = (id) => setExpanded(prev => prev === id ? null : id);

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div>
        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Tài chính / Duyệt bảng giá
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Phê duyệt bảng giá
        </h1>
        <p className="text-xs text-shade-50 font-light mt-1">
          Xem xét và phê duyệt các bản giá mới do Kế toán viên đề xuất
          {activeWarehouse && <> — Kho <span className="font-semibold text-ink">{activeWarehouse.name}</span></>}.
          {!loading && entries.length > 0 && (
            <span className="ml-1 font-semibold text-amber-700">{entries.length} bản giá đang chờ duyệt.</span>
          )}
        </p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : entries.length === 0 ? (
        <div className="bg-canvas-light rounded-lg border border-hairline-light p-12 text-center shadow-level-3">
          <CheckCircle className="w-12 h-12 text-emerald-400 mx-auto mb-4" />
          <h3 className="text-lg font-bold mb-1">Không có bản giá nào chờ duyệt</h3>
          <p className="text-sm text-shade-50">Tất cả bản giá đã được xử lý.</p>
        </div>
      ) : (
        <>
          <div className="flex flex-col gap-3">
            {entries
              .slice((currentPage - 1) * pageSize, currentPage * pageSize)
              .map(entry => (
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
          <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
            <Pagination
              currentPage={currentPage}
              totalPages={Math.max(1, Math.ceil(entries.length / pageSize))}
              totalItems={entries.length}
              pageSize={pageSize}
              onPageChange={setCurrentPage}
              onPageSizeChange={(s) => { setPageSize(s); setCurrentPage(1); }}
            />
          </div>
        </>
      )}
    </div>
  );
}

function PriceCard({ entry, expanded, onToggle, onApprove, approving }) {
  const prev = entry.previous_approved;

  return (
    <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
      {/* Header row */}
      <div className="flex items-center gap-4 px-6 py-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <span className="font-mono text-[10px] text-shade-50 bg-canvas-cream px-1.5 py-0.5 rounded-pill border border-hairline-light">
              {entry.product_sku}
            </span>
            <span className="font-semibold text-sm text-ink truncate">{entry.product_name}</span>
          </div>
          <div className="text-xs text-shade-50 mt-1">
            {entry.warehouse_name && (
              <span className="inline-block bg-blue-50 text-blue-700 border border-blue-200 rounded-pill px-1.5 py-0.5 text-[10px] font-semibold mr-2">
                {entry.warehouse_name}
              </span>
            )}
            Kỳ hiệu lực: <span className="font-medium text-ink">{entry.effective_date} → {entry.end_date}</span>
            &nbsp;·&nbsp; Tạo bởi <span className="font-medium text-ink">{entry.created_by?.full_name}</span>
          </div>
        </div>

        <div className="text-right shrink-0">
          <div className="text-lg font-bold tabular-nums text-ink">
            {formatVND(entry.selling_price)}
          </div>
          <div className="text-xs text-shade-50">Giá vốn: {formatVND(entry.cost_price)}</div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={onToggle}
            className="p-2 rounded-pill hover:bg-canvas-cream text-shade-50 transition-colors"
            title={expanded ? 'Thu gọn' : 'Xem chi tiết so sánh'}
          >
            {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
          <button
            onClick={onApprove}
            disabled={approving}
            className="btn-pill btn-pill-aloe text-xs py-1.5 px-4 font-semibold disabled:opacity-50 flex items-center gap-1.5"
          >
            {approving ? (
              <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Đang duyệt...</>
            ) : (
              <><Check className="w-3.5 h-3.5" /> Phê duyệt</>
            )}
          </button>
        </div>
      </div>

      {/* Delta comparison section */}
      {expanded && (
        <div className="border-t border-hairline-light bg-canvas-cream px-6 py-4">
          {prev ? (
            <div className="flex flex-col gap-3">
              <p className="text-xs font-bold text-shade-60 uppercase tracking-widest">
                So sánh với bản giá đã duyệt trước
              </p>
              <p className="text-xs text-shade-50">Kỳ trước: {prev.effective_date} → {prev.end_date}</p>
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
              <span className="text-xs font-bold text-shade-60 uppercase tracking-wider">Ghi chú: </span>
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
    <div className="bg-canvas-light rounded-lg border border-hairline-light p-3 space-y-1 shadow-level-3">
      <div className="text-xs font-bold text-shade-60 uppercase tracking-wider">{label}</div>
      <div className="flex items-end gap-2">
        <span className="text-lg font-bold tabular-nums">{formatVND(curr)}</span>
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
