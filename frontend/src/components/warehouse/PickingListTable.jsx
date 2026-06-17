import React from 'react';
import { CheckCircle2, Circle, AlertCircle } from 'lucide-react';

const PickingListTable = ({ items, isPicking, onPickItem, pickedItems }) => {
  return (
    <div className="bg-white rounded-lg border border-hairline-light shadow-sm overflow-hidden card-premium">
      <div className="px-6 py-3.5 bg-zinc-50 border-b border-hairline-light">
        <h3 className="text-xs font-bold uppercase tracking-wider text-shade-60">
          Danh sách cần lấy (Picking List)
        </h3>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-zinc-50 border-b border-hairline-light">
              <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Sản phẩm</th>
              <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">SKU</th>
              <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Vị trí / Lô</th>
              <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider text-right">SL yêu cầu</th>
              {isPicking && <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider text-right">Thực lấy</th>}
              {isPicking && <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider text-center">Trạng thái</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-hairline-light">
            {items.map((item) => {
              const picked = pickedItems?.find(p => p.id === item.id);
              const isFullyPicked = picked?.issued_qty === item.requested_qty;
              const hasError = picked && picked.issued_qty > item.requested_qty;

              return (
                <tr key={item.id} className={`transition-colors ${isFullyPicked ? 'bg-emerald-50/40' : 'hover:bg-zinc-50'}`}>
                  <td className="px-6 py-4 text-xs font-semibold text-ink">{item.product_name}</td>
                  <td className="px-6 py-4 text-xs text-shade-50 font-mono">{item.sku}</td>
                  <td className="px-6 py-4">
                    <p className="text-xs font-semibold text-ink">{item.bin_code || <span className="text-shade-40 font-normal italic">Chưa xếp vị trí</span>}</p>
                    <p className="text-[11px] text-shade-40">{item.batch_number || '—'}</p>
                  </td>
                  <td className="px-6 py-4 text-xs font-bold text-ink text-right">{item.requested_qty}</td>

                  {isPicking && (
                    <td className="px-6 py-4 text-right">
                      <input
                        type="number"
                        min="0"
                        max={item.requested_qty}
                        className={`w-20 text-input text-xs py-1 text-right ${hasError ? 'border-red-400 focus:border-red-500' : ''}`}
                        value={picked?.issued_qty ?? 0}
                        onChange={(e) => onPickItem(item.id, Number(e.target.value), picked?.serial_number)}
                      />
                    </td>
                  )}

                  {isPicking && (
                    <td className="px-6 py-4 text-center">
                      {isFullyPicked
                        ? <CheckCircle2 className="w-5 h-5 text-emerald-500 mx-auto" />
                        : hasError
                          ? <AlertCircle className="w-5 h-5 text-red-500 mx-auto" title="Số lượng vượt mức" />
                          : <Circle className="w-5 h-5 text-shade-30 mx-auto" />
                      }
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default PickingListTable;
