import React from 'react';
import { AlertTriangle, Plus, Save, Trash2 } from 'lucide-react';

const formatCandidateLabel = (candidate) => {
  const parts = [
    candidate.location_code || `Vị trí ${candidate.location_id || '-'}`,
    candidate.zone_code || `Khu ${candidate.zone_id || '-'}`,
    candidate.batch_code || `Lô ${candidate.batch_id || '-'}`,
    `Khả dụng ${Number(candidate.available_qty || 0)}`,
  ];

  return parts.join(' · ');
};

const sumPlannedQty = (item) => (item.allocations || []).reduce(
  (total, allocation) => total + Number(allocation.planned_qty || 0),
  0,
);

const DeliveryOrderPickingPlanEditor = ({
  items = [],
  candidatesByItemId = {},
  submitting = false,
  disableSave = false,
  onAddAllocation,
  onAllocationChange,
  onCandidateSelect,
  onRemoveAllocation,
  onSave,
}) => (
  <div className="bg-white rounded-lg border border-hairline-light shadow-sm card-premium overflow-hidden">
    <div className="px-6 py-4 border-b border-hairline-light bg-zinc-50 flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
      <div>
        <h3 className="text-xs font-bold uppercase tracking-wider text-shade-60">
          Lập kế hoạch lấy hàng
        </h3>
        <p className="text-xs text-shade-50 mt-1">
          Chọn inventory cụ thể theo từng batch, bin, zone trước khi lưu picking plan.
        </p>
      </div>
      <button
        type="button"
        disabled={submitting || disableSave}
        onClick={onSave}
        className="btn-pill btn-pill-primary inline-flex items-center gap-2 disabled:opacity-50"
      >
        <Save className="w-4 h-4" />
        Lưu kế hoạch lấy hàng
      </button>
    </div>

    <div className="p-6 space-y-6">
      {items.map((item) => {
        const candidates = candidatesByItemId[item.id] || [];
        const plannedQty = sumPlannedQty(item);
        const qtyMatched = plannedQty === Number(item.requested_qty || 0);

        return (
          <section key={item.id} className="border border-hairline-light rounded-lg overflow-hidden">
            <div className="px-4 py-3 bg-zinc-50 border-b border-hairline-light flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="text-sm font-semibold text-ink">{item.product_name}</p>
                <p className="text-xs text-shade-50 font-mono">{item.sku || '-'}</p>
              </div>
              <div className="text-xs font-semibold">
                <span className="text-shade-50">Yêu cầu:</span> {Number(item.requested_qty || 0)}
                <span className={`ml-3 ${qtyMatched ? 'text-emerald-700' : 'text-amber-700'}`}>
                  Đã phân bổ: {plannedQty}
                </span>
              </div>
            </div>

            <div className="p-4 space-y-3">
              {!candidates.length && (
                <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-xs text-amber-800">
                  <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                  <span>API hiện chưa trả danh sách tồn kho FIFO để chọn cho dòng này. Có thể chỉ xem phân bổ đã lưu.</span>
                </div>
              )}

              {(item.allocations || []).map((allocation, index) => (
                <div key={`${item.id}-${allocation.allocation_id || index}`} className="grid grid-cols-1 gap-3 rounded-lg border border-hairline-light p-3 md:grid-cols-[minmax(0,2fr)_110px_auto]">
                  <div className="space-y-2">
                    <label className="block text-[11px] font-semibold uppercase tracking-wider text-shade-50">
                      Nguồn lấy hàng
                    </label>
                    <select
                      value={allocation.inventory_id || ''}
                      onChange={(event) => onCandidateSelect(item.id, index, event.target.value)}
                      className="w-full rounded-md border border-hairline-light bg-canvas-light px-3 py-2 text-sm text-ink focus:border-ink focus:outline-none focus:ring-1 focus:ring-ink"
                    >
                      <option value="">Chọn batch / vị trí / khu</option>
                      {candidates.map((candidate) => (
                        <option key={candidate.inventory_id} value={candidate.inventory_id}>
                          {formatCandidateLabel(candidate)}
                        </option>
                      ))}
                    </select>
                    <p className="text-[11px] text-shade-50">
                      Inventory #{allocation.inventory_id || '-'} · {allocation.location_code || `Vị trí ${allocation.location_id || '-'}`}
                    </p>
                  </div>

                  <div className="space-y-2">
                    <label className="block text-[11px] font-semibold uppercase tracking-wider text-shade-50">
                      SL kế hoạch
                    </label>
                    <input
                      type="number"
                      min="0"
                      step="1"
                      value={allocation.planned_qty ?? 0}
                      onChange={(event) => onAllocationChange(item.id, index, 'planned_qty', event.target.value)}
                      className="w-full rounded-md border border-hairline-light bg-canvas-light px-3 py-2 text-sm text-ink focus:border-ink focus:outline-none focus:ring-1 focus:ring-ink"
                    />
                  </div>

                  <div className="flex items-end justify-between gap-2 md:justify-end">
                    <div className="text-[11px] text-shade-50">
                      Lô {allocation.batch_code || allocation.batch_id || '-'} · Khu {allocation.zone_code || allocation.zone_id || '-'}
                    </div>
                    <button
                      type="button"
                      onClick={() => onRemoveAllocation(item.id, index)}
                      disabled={(item.allocations || []).length <= 1}
                      className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-hairline-light text-shade-50 transition hover:bg-zinc-100 hover:text-ink disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}

              <button
                type="button"
                onClick={() => onAddAllocation(item.id)}
                className="inline-flex items-center gap-2 rounded-pill border border-hairline-light px-4 py-2 text-xs font-semibold text-shade-60 transition hover:bg-zinc-100 hover:text-ink"
              >
                <Plus className="w-3.5 h-3.5" />
                Thêm dòng phân bổ
              </button>
            </div>
          </section>
        );
      })}
    </div>
  </div>
);

export default DeliveryOrderPickingPlanEditor;
