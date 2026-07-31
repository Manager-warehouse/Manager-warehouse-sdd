import React from 'react';
import { AlertTriangle, Plus, Save, Trash2 } from 'lucide-react';

const formatCandidateLabel = (candidate, productName) => {
  const parts = [];
  if (productName) {
    parts.push(productName);
  }
  parts.push(candidate.batch_code || `Lô ${candidate.batch_id || '-'}`);
  parts.push(candidate.location_code || `Vị trí ${candidate.location_id || '-'}`);
  parts.push(candidate.zone_code || `Khu ${candidate.zone_id || '-'}`);
  parts.push(`Khả dụng ${Number(candidate.available_qty || 0)}`);

  return parts.join(' · ');
};

const formatFailedSourceLabel = (source) => {
  const parts = [];
  if (source.product_name) {
    parts.push(source.product_name);
  }
  parts.push(source.batch_code || `Lô ${source.batch_id || '-'}`);
  parts.push(source.location_code || `Vị trí ${source.location_id || '-'}`);
  parts.push(source.zone_code || `Khu ${source.zone_id || '-'}`);
  parts.push(`QC fail ${Number(source.qc_fail_qty || 0)}`);

  return parts.join(' · ');
};

const sumPlannedQty = (item) => (item.allocations || []).reduce(
  (total, allocation) => total + Number(allocation.planned_qty || 0),
  0,
);

const DeliveryOrderPickingPlanEditor = ({
  items = [],
  candidatesByItemId = {},
  stockAvailabilities = {},
  mode = 'picking',
  title = 'Lập kế hoạch lấy hàng',
  description = 'Chọn hàng tồn kho cụ thể theo từng lô hàng và vị trí trong kho trước khi lưu kế hoạch lấy hàng.',
  saveLabel = 'Lưu kế hoạch lấy hàng',
  submitting = false,
  disableSave = false,
  onAddAllocation,
  onAllocationChange,
  onCandidateSelect,
  onFailedSourceSelect,
  onRemoveAllocation,
  onSave,
}) => (
  <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
    <div className="px-6 py-4 border-b border-hairline-light bg-canvas-cream flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
      <div>
        <h3 className="text-xs font-bold uppercase tracking-wider text-shade-60">
          {title}
        </h3>
        <p className="text-xs text-shade-50 mt-1">
          {description}
        </p>
      </div>
      <button
        type="button"
        disabled={submitting || disableSave}
        onClick={onSave}
        className="btn-pill btn-pill-primary inline-flex items-center gap-2 disabled:opacity-50"
      >
        <Save className="w-4 h-4" />
        {saveLabel}
      </button>
    </div>

    <div className="p-6 space-y-6">
      {items.map((item) => {
        const candidates = candidatesByItemId[item.id] || [];
        const failedSources = item.failed_sources || [];
        const plannedQty = sumPlannedQty(item);
        const requiredQty = Number(item.replacement_required_qty ?? item.requested_qty ?? 0);
        const qtyMatched = plannedQty === requiredQty;
        const availableInWarehouse = Number(stockAvailabilities[item.product_id] ?? 999999);
        const isInsufficient = mode === 'replacement' && availableInWarehouse < requiredQty;
        const missingQty = requiredQty - availableInWarehouse;

        return (
          <section key={item.id} className="border border-hairline-light rounded-lg overflow-hidden">
            <div className="px-4 py-3 bg-canvas-cream border-b border-hairline-light flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="text-sm font-semibold text-ink">{item.product_name}</p>
                <p className="text-xs text-shade-50 font-mono mb-1">{item.sku || '-'}</p>
                {isInsufficient && (
                  <div className="mt-1 flex items-center gap-1.5 text-xs text-danger-700 font-semibold bg-danger-50 border border-danger-200 rounded px-2.5 py-1">
                    <AlertTriangle className="w-3.5 h-3.5" />
                    <span>Thiếu {missingQty} sản phẩm. Chờ nhập thêm hàng trong thời gian gần nhất</span>
                  </div>
                )}
              </div>
              <div className="text-xs font-semibold">
                <span className="text-shade-50">{mode === 'replacement' ? 'Cần bù:' : 'Yêu cầu:'}</span> {requiredQty}
                <span className={`ml-3 ${qtyMatched ? 'text-success-700' : 'text-warning-700'}`}>
                  Đã phân bổ: {plannedQty}
                </span>
              </div>
            </div>

            <div className="p-4 space-y-3">
              {!candidates.length && (
                <div className="flex items-start gap-2 rounded-lg border border-warning-200 bg-warning-50 px-3 py-2.5 text-xs text-warning-800">
                  <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                  <span>Chưa có danh sách tồn kho FIFO để chọn cho dòng này.</span>
                </div>
              )}

              {(item.allocations || []).map((allocation, index) => (
                <div
                  key={`${item.id}-${allocation.allocation_id || index}`}
                  className={`grid grid-cols-1 gap-3 rounded-lg border border-hairline-light p-3 ${
                    mode === 'replacement'
                      ? 'md:grid-cols-[minmax(0,1.35fr)_minmax(0,1.65fr)_110px_minmax(0,1fr)_auto]'
                      : 'md:grid-cols-[minmax(0,2fr)_110px_auto]'
                  }`}
                >
                  {mode === 'replacement' && (
                    <div className="space-y-2">
                      <label className="block text-[11px] font-semibold uppercase tracking-wider text-shade-50">
                        Hàng lỗi QC
                      </label>
                      <select
                        value={allocation.failed_inventory_id || ''}
                        onChange={(event) => onFailedSourceSelect(item.id, index, event.target.value)}
                        className="w-full rounded-md border border-hairline-light bg-canvas-light px-3 py-2 text-sm text-ink focus:border-ink focus:outline-none focus:ring-1 focus:ring-ink"
                      >
                        <option value="">Chọn dòng đã QC fail</option>
                        {failedSources.map((source) => (
                          <option key={source.inventory_id} value={source.inventory_id}>
                            {formatFailedSourceLabel(source)}
                          </option>
                        ))}
                      </select>
                    </div>
                  )}

                  <div className="space-y-2">
                    <label className="block text-[11px] font-semibold uppercase tracking-wider text-shade-50">
                      {mode === 'replacement' ? 'Nguồn hàng bù' : 'Nguồn lấy hàng'}
                    </label>
                    <select
                      value={allocation.inventory_id || ''}
                      onChange={(event) => onCandidateSelect(item.id, index, event.target.value)}
                      className="w-full rounded-md border border-hairline-light bg-canvas-light px-3 py-2 text-sm text-ink focus:border-ink focus:outline-none focus:ring-1 focus:ring-ink"
                    >
                      <option value="">Chọn batch / vị trí / khu</option>
                      {candidates.map((candidate) => (
                        <option key={candidate.inventory_id} value={candidate.inventory_id}>
                          {formatCandidateLabel(candidate, item.product_name)}
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
                      step="0.01"
                      value={allocation.planned_qty ?? 0}
                      onChange={(event) => onAllocationChange(item.id, index, 'planned_qty', event.target.value)}
                      className="w-full rounded-md border border-hairline-light bg-canvas-light px-3 py-2 text-sm text-ink focus:border-ink focus:outline-none focus:ring-1 focus:ring-ink"
                    />
                  </div>

                  {mode === 'replacement' && (
                    <div className="space-y-2">
                      <label className="block text-[11px] font-semibold uppercase tracking-wider text-shade-50">
                        Lý do
                      </label>
                      <input
                        value={allocation.reason || ''}
                        onChange={(event) => onAllocationChange(item.id, index, 'reason', event.target.value)}
                        className="w-full rounded-md border border-hairline-light bg-canvas-light px-3 py-2 text-sm text-ink focus:border-ink focus:outline-none focus:ring-1 focus:ring-ink"
                        placeholder="VD: Hàng lỗi QC"
                      />
                    </div>
                  )}

                  <div className="flex items-end justify-between gap-2 md:justify-end">
                    <div className="text-[11px] text-shade-50">
                      Lô {allocation.batch_code || allocation.batch_id || '-'} · Khu {allocation.zone_code || allocation.zone_id || '-'}
                    </div>
                    <button
                      type="button"
                      onClick={() => onRemoveAllocation(item.id, index)}
                      disabled={(item.allocations || []).length <= 1}
                      className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-hairline-light text-shade-50 transition hover:bg-canvas-cream hover:text-ink disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}

              <button
                type="button"
                onClick={() => onAddAllocation(item.id)}
                className="inline-flex items-center gap-2 rounded-pill border border-hairline-light px-4 py-2 text-xs font-semibold text-shade-60 transition hover:bg-canvas-cream hover:text-ink"
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
