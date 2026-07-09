import React from 'react';

const formatAllocation = (allocation) => {
  const parts = [
    allocation.location_code || (allocation.location_id ? `Vị trí ${allocation.location_id}` : null),
    allocation.zone_code || (allocation.zone_id ? `Khu ${allocation.zone_id}` : null),
    allocation.batch_code || (allocation.batch_id ? `Lô ${allocation.batch_id}` : null),
  ].filter(Boolean);

  return parts.length ? parts.join(' · ') : 'Chưa xếp vị trí';
};

const PickingListTable = ({ items = [] }) => {
  return (
    <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-sm overflow-hidden card-premium">
      <div className="px-6 py-3.5 bg-canvas-cream border-b border-hairline-light">
        <h3 className="text-xs font-bold uppercase tracking-wider text-shade-60">
          Kế hoạch lấy hàng theo phân bổ
        </h3>
      </div>

      <div className="hidden md:block overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-canvas-cream border-b border-hairline-light">
              <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Sản phẩm</th>
              <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">SKU</th>
              <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Phân bổ lấy hàng</th>
              <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider text-right">SL kế hoạch</th>
              <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider text-right">SL đã lấy</th>
            </tr>
          </thead>

          <tbody className="divide-y divide-hairline-light">
            {items.flatMap((item) => {
              const allocations = item.allocations?.length
                ? item.allocations
                : [{
                    allocation_id: null,
                    planned_qty: item.planned_qty || item.requested_qty || 0,
                    picked_qty: item.picked_qty || 0,
                  }];

              return allocations.map((allocation, index) => (
                <tr key={`${item.id}-${allocation.allocation_id || index}`} className="hover:bg-canvas-cream transition-colors">
                  <td className="px-6 py-4 text-xs font-semibold text-ink">{item.product_name}</td>
                  <td className="px-6 py-4 text-xs text-shade-50 font-mono">{item.sku || '-'}</td>
                  <td className="px-6 py-4 text-xs text-shade-60">{formatAllocation(allocation)}</td>
                  <td className="px-6 py-4 text-xs font-bold text-ink text-right">{Number(allocation.planned_qty || 0)}</td>
                  <td className="px-6 py-4 text-xs font-semibold text-shade-60 text-right">{Number(allocation.picked_qty || 0)}</td>
                </tr>
              ));
            })}

            {!items.length && (
              <tr>
                <td colSpan="5" className="px-6 py-10 text-center text-sm text-shade-40 italic">
                  Đơn này chưa có dòng hàng nào.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex flex-col gap-3 p-4 md:hidden">
        {items.flatMap((item) => {
          const allocations = item.allocations?.length
            ? item.allocations
            : [{
                allocation_id: null,
                planned_qty: item.planned_qty || item.requested_qty || 0,
                picked_qty: item.picked_qty || 0,
              }];

          return allocations.map((allocation, index) => (
            <div
              key={`${item.id}-${allocation.allocation_id || index}`}
              className="rounded-lg border border-hairline-light bg-canvas-light p-4 shadow-level-3"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="text-sm font-semibold text-ink">{item.product_name}</div>
                  <div className="mt-1 font-mono text-[11px] text-shade-50">{item.sku || '-'}</div>
                </div>
                <div className="shrink-0 rounded-pill bg-aloe-10 px-3 py-1 text-[11px] font-bold text-ink">
                  {Number(allocation.planned_qty || 0)}
                </div>
              </div>

              <div className="mt-3 rounded-md bg-canvas-cream px-3 py-2 text-xs text-shade-60">
                {formatAllocation(allocation)}
              </div>

              <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
                <div className="rounded-md border border-hairline-light p-2">
                  <span className="block text-[10px] uppercase tracking-wider text-shade-50">Kế hoạch</span>
                  <span className="font-bold text-ink">{Number(allocation.planned_qty || 0)}</span>
                </div>
                <div className="rounded-md border border-hairline-light p-2">
                  <span className="block text-[10px] uppercase tracking-wider text-shade-50">Đã lấy</span>
                  <span className="font-bold text-shade-60">{Number(allocation.picked_qty || 0)}</span>
                </div>
              </div>
            </div>
          ));
        })}

        {!items.length && (
          <div className="px-4 py-10 text-center text-sm italic text-shade-40">
            Đơn này chưa có dòng hàng nào.
          </div>
        )}
      </div>
    </div>
  );
};

export default PickingListTable;
