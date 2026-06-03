import React from 'react';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';

const Pagination = ({ 
  currentPage, 
  totalPages, 
  totalItems, 
  pageSize, 
  onPageChange, 
  onPageSizeChange,
  pageSizeOptions = [10, 25, 50, 100]
}) => {
  // Prevent invalid pages
  const handlePageChange = (page) => {
    if (page >= 1 && page <= totalPages && page !== currentPage) {
      onPageChange(page);
    }
  };

  // Generate page numbers to display
  const getPageNumbers = () => {
    const pages = [];
    
    if (totalPages <= 5) {
      // Show all pages if 5 or fewer
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Show dynamic range
      if (currentPage <= 3) {
        pages.push(1, 2, 3, 4, '...', totalPages);
      } else if (currentPage >= totalPages - 2) {
        pages.push(1, '...', totalPages - 3, totalPages - 2, totalPages - 1, totalPages);
      } else {
        pages.push(1, '...', currentPage - 1, currentPage, currentPage + 1, '...', totalPages);
      }
    }
    
    return pages;
  };

  if (totalItems === 0) return null;

  const startItem = (currentPage - 1) * pageSize + 1;
  const endItem = Math.min(currentPage * pageSize, totalItems);

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between px-6 py-4 border-t border-hairline-light bg-canvas-light gap-4">
      {/* Information text */}
      <div className="flex items-center gap-4 text-xs text-shade-50 font-medium">
        <div>
          Hiển thị <span className="font-bold text-ink">{startItem}</span> - <span className="font-bold text-ink">{endItem}</span> trong <span className="font-bold text-ink">{totalItems}</span> kết quả
        </div>
        
        {/* Page size selector */}
        {onPageSizeChange && (
          <div className="flex items-center gap-2">
            <span className="hidden sm:inline text-hairline-light">|</span>
            <label htmlFor="pageSize" className="sr-only">Số dòng</label>
            <select
              id="pageSize"
              value={pageSize}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              className="bg-canvas-cream border border-hairline-light text-ink text-xs rounded-md px-2 py-1 focus:outline-none focus:ring-1 focus:ring-ink"
            >
              {pageSizeOptions.map(size => (
                <option key={size} value={size}>{size} dòng</option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Pagination Controls */}
      <div className="flex items-center gap-1">
        <button
          onClick={() => handlePageChange(1)}
          disabled={currentPage === 1}
          className="w-8 h-8 flex items-center justify-center rounded-pill text-shade-50 hover:text-ink hover:bg-canvas-cream disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-shade-50 transition-colors"
          title="Trang đầu"
        >
          <ChevronsLeft className="w-4 h-4" />
        </button>
        <button
          onClick={() => handlePageChange(currentPage - 1)}
          disabled={currentPage === 1}
          className="w-8 h-8 flex items-center justify-center rounded-pill text-shade-50 hover:text-ink hover:bg-canvas-cream disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-shade-50 transition-colors"
          title="Trang trước"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>

        <div className="flex items-center mx-2 gap-1">
          {getPageNumbers().map((page, index) => {
            if (page === '...') {
              return (
                <span key={`ellipsis-${index}`} className="px-2 text-shade-40 text-xs">
                  ...
                </span>
              );
            }
            
            const isActive = page === currentPage;
            return (
              <button
                key={page}
                onClick={() => handlePageChange(page)}
                className={`min-w-[32px] h-8 px-1.5 text-xs font-semibold rounded-pill flex items-center justify-center transition-all
                  ${isActive 
                    ? 'bg-ink text-onPrimary shadow-sm' 
                    : 'text-shade-60 hover:bg-canvas-cream hover:text-ink border border-transparent hover:border-hairline-light'
                  }`}
              >
                {page}
              </button>
            );
          })}
        </div>

        <button
          onClick={() => handlePageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          className="w-8 h-8 flex items-center justify-center rounded-pill text-shade-50 hover:text-ink hover:bg-canvas-cream disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-shade-50 transition-colors"
          title="Trang sau"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
        <button
          onClick={() => handlePageChange(totalPages)}
          disabled={currentPage === totalPages}
          className="w-8 h-8 flex items-center justify-center rounded-pill text-shade-50 hover:text-ink hover:bg-canvas-cream disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-shade-50 transition-colors"
          title="Trang cuối"
        >
          <ChevronsRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

export default Pagination;
