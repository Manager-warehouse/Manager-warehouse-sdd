import React from 'react';

const Table = ({
  headers = [],
  data = [],
  renderRow,
  loading = false,
  emptyMessage = 'Không có dữ liệu hiển thị'
}) => {
  return (
    <div className="w-full overflow-hidden bg-canvas-light">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-canvas-cream border-b border-hairline-light">
              {headers.map((header, idx) => (
                <th
                  key={idx}
                  className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60"
                >
                  {header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-hairline-light">
            {loading ? (
              <tr>
                <td colSpan={headers.length} className="px-6 py-12 text-center text-shade-50">
                  <div className="flex justify-center items-center gap-2">
                    <svg className="animate-spin h-5 w-5 text-ink" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <span>Đang tải dữ liệu...</span>
                  </div>
                </td>
              </tr>
            ) : data.length === 0 ? (
              <tr>
                <td colSpan={headers.length} className="px-6 py-12 text-center text-shade-50">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              data.map((item, idx) => renderRow(item, idx))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Table;
