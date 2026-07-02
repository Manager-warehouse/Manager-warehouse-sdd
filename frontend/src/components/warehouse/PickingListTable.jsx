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

      <div className="overflow-x-auto">
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
    </div>
  );
};

export default PickingListTable;
