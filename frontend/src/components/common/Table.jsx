import React from 'react';
import { Loader2 } from 'lucide-react';

const Table = ({
  headers = [],
  data = [],
  renderRow,
  renderCard,
  loading = false,
  emptyMessage = 'Không có dữ liệu hiển thị'
}) => {
  if (loading) {
    return (
      <div className="flex items-center justify-center p-20">
        <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className="w-full bg-canvas-light px-6 py-12 text-center text-shade-50">
        {emptyMessage}
      </div>
    );
  }

  return (
    <div className="w-full overflow-hidden bg-canvas-light">
      {/* Desktop/tablet: table view (full-width if no card fallback is provided) */}
      <div className={renderCard ? 'hidden md:block overflow-x-auto' : 'overflow-x-auto'}>
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-canvas-cream border-b border-hairline-light">
              {headers.map((header, idx) => (
                <th
                  key={idx}
                  className={`px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 ${idx === headers.length - 1 ? 'text-right' : ''}`}
                >
                  {header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-hairline-light">
            {data.map((item, idx) => renderRow(item, idx))}
          </tbody>
        </table>
      </div>

      {/* Mobile: stacked card view */}
      {renderCard && (
        <div className="flex flex-col gap-3 p-4 md:hidden">
          {data.map((item, idx) => renderCard(item, idx))}
        </div>
      )}
    </div>
  );
};

export default Table;
