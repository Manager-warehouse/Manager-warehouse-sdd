import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Upload, Download, FileSpreadsheet, Search, X, Edit2, Ban, DollarSign, Loader2, Warehouse, RefreshCw } from 'lucide-react';
import Pagination from '../../components/common/Pagination';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { useDebounce } from '../../hooks/useDebounce';
import pricingService from '../../services/pricing.service';
import { masterDataService } from '../../services/masterData.service';
import { ROLES } from '../../utils/constants';

const STATUS_LABEL = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', CANCELLED: 'Đã hủy' };
const STATUS_STYLE = {
  PENDING:   'bg-warning-50 text-warning-800 border-warning-300',
  APPROVED:  'bg-aloe-10 text-success-900 border-success-300',
  CANCELLED: 'bg-canvas-cream text-shade-50 border-hairline-light',
};

export default function PriceListManagement() {
  const { user, hasRole, activeWarehouse } = useAuthStore();
  const { addToast } = useUiStore();

  const [entries, setEntries] = useState([]);
  const [allEntries, setAllEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [replaceTarget, setReplaceTarget] = useState(null);
  const [showImport, setShowImport] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const canWrite = hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ADMIN);
  const warehouseId = activeWarehouse?.id;

  const fetchEntries = useCallback(async () => {
    if (!warehouseId) return;
    setLoading(true);
    try {
      const baseParams = { warehouse_id: warehouseId };
      const [filtered, all] = await Promise.all([
        pricingService.getAll(statusFilter !== 'ALL' ? { ...baseParams, status: statusFilter } : baseParams),
        pricingService.getAll(baseParams),
      ]);
      setEntries(filtered);
      setAllEntries(all);
    } catch (err) {
      addToast(err.message || 'Không tải được danh sách bảng giá', 'error');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, warehouseId]);

  useEffect(() => { fetchEntries(); }, [fetchEntries]);

  const filtered = entries.filter(e =>
    e.product_sku?.toLowerCase().includes(search.toLowerCase()) ||
    e.product_name?.toLowerCase().includes(search.toLowerCase())
  );

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const safePage = Math.min(currentPage, totalPages);
  const paginated = filtered.slice((safePage - 1) * pageSize, safePage * pageSize);

  useEffect(() => { setCurrentPage(1); }, [search, statusFilter]);

  const totalEntries = allEntries.length;
  const pendingCount = allEntries.filter(e => e.status === 'PENDING').length;
  const approvedCount = allEntries.filter(e => e.status === 'APPROVED').length;

  const handleExportXlsx = async () => {
    if (entries.length === 0) { addToast('Không có dữ liệu để xuất', 'warning'); return; }
    try {
      const params = { warehouse_id: warehouseId };
      if (statusFilter !== 'ALL') params.status = statusFilter;
      await pricingService.exportXlsx(params);
    } catch (err) {
      addToast(err.message || 'Không thể xuất file', 'error');
    }
  };

  const handleCancel = async (id) => {
    if (!window.confirm('Xác nhận hủy bản giá này?')) return;
    try {
      await pricingService.cancel(id);
      addToast('Đã hủy bản giá', 'success');
      fetchEntries();
    } catch (err) {
      addToast(err.message || 'Không thể hủy bản giá', 'error');
    }
  };

  const handleReplace = (entry) => {
    setEditTarget(null);
    setReplaceTarget(entry);
    setShowForm(true);
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4 flex-wrap">
        <div className="flex-1 min-w-0">
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Tài chính / Bảng giá
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Quản lý bảng giá
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Quản lý giá vốn & giá bán theo kỳ hiệu lực tại <span className="font-semibold text-ink">{activeWarehouse?.name ?? '—'}</span>. Bản giá mới cần được Kế toán trưởng phê duyệt trước khi có hiệu lực.
          </p>
        </div>
        <div className="flex gap-2 flex-wrap lg:flex-nowrap items-center w-full lg:w-auto flex-shrink-0">
          <Button onClick={handleExportXlsx} variant="primary" icon={FileSpreadsheet} className="flex-none">
            Xuất Excel
          </Button>
          {canWrite && (
            <>
              <Button onClick={() => setShowImport(true)} variant="outline-light" icon={Download} className="flex-none">
                Nhập Excel
              </Button>
              <Button onClick={() => pricingService.downloadTemplate().catch(() => addToast('Không tải được file mẫu', 'error'))} variant="outline-light" icon={Upload} className="flex-none">
                Tải mẫu
              </Button>
              <Button onClick={() => { setEditTarget(null); setShowForm(true); }} variant="primary" icon={Plus} className="flex-none">
                Thêm bản giá
              </Button>
            </>
          )}
        </div>
      </div>

      {/* KPI Summary */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        {[
          { label: 'Tổng bản giá', value: totalEntries, icon: <DollarSign className="w-5 h-5" />, accent: 'text-shade-60 bg-canvas-cream' },
          { label: 'Chờ duyệt', value: pendingCount, icon: <DollarSign className="w-5 h-5" />, accent: 'text-warning-600 bg-warning-50' },
          { label: 'Đã duyệt', value: approvedCount, icon: <DollarSign className="w-5 h-5" />, accent: 'text-success-600 bg-success-50' },
        ].map(({ label, value, icon, accent }) => (
          <div key={label} className="bg-canvas-light rounded-lg border border-hairline-light p-4 shadow-level-3 flex items-center gap-3">
            <div className={`p-2.5 rounded-full ${accent}`}>{icon}</div>
            <div>
              <p className="text-xs text-shade-50 font-medium">{label}</p>
              <p className="text-2xl font-bold text-ink">{value}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="bg-canvas-light rounded-lg border border-hairline-light p-4 shadow-level-3 flex flex-col md:flex-row gap-4 items-center justify-between">
        <div className="w-full md:w-80">
          <Input
            type="text"
            leftIcon={Search}
            placeholder="Tìm SKU hoặc tên sản phẩm..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <Input
          type="select"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          options={[
            { value: 'ALL', label: 'Tất cả' },
            { value: 'PENDING', label: 'Chờ duyệt' },
            { value: 'APPROVED', label: 'Đã duyệt' },
            { value: 'CANCELLED', label: 'Đã hủy' },
          ]}
        />
      </div>

      {/* Table */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="bg-canvas-light rounded-lg border border-hairline-light p-12 text-center shadow-level-3">
          <DollarSign className="w-12 h-12 text-shade-30 mx-auto mb-4" />
          <h3 className="text-lg font-bold mb-1">Không tìm thấy bản giá nào</h3>
          <p className="text-sm text-shade-50">Thay đổi bộ lọc hoặc thêm bản giá mới để bắt đầu.</p>
        </div>
      ) : (
        <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
          {/* Desktop/tablet: table view */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-canvas-cream border-b border-hairline-light">
                  <th className="px-6 py-4 text-xs font-semibold text-shade-60 uppercase tracking-wider">SKU</th>
                  <th className="px-6 py-4 text-xs font-semibold text-shade-60 uppercase tracking-wider">Sản phẩm</th>
                  <th className="px-6 py-4 text-xs font-semibold text-shade-60 uppercase tracking-wider">Hiệu lực từ</th>
                  <th className="px-6 py-4 text-xs font-semibold text-shade-60 uppercase tracking-wider text-right">Giá vốn</th>
                  <th className="px-6 py-4 text-xs font-semibold text-shade-60 uppercase tracking-wider text-right">Giá bán</th>
                  <th className="px-6 py-4 text-xs font-semibold text-shade-60 uppercase tracking-wider">Trạng thái</th>
                  <th className="px-6 py-4 text-xs font-semibold text-shade-60 uppercase tracking-wider">Ghi chú</th>
                  <th className="px-6 py-4 text-xs font-semibold text-shade-60 uppercase tracking-wider text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {paginated.map((entry) => (
                  <tr key={entry.id} className="hover:bg-canvas-cream/50 transition-colors">
                    <td className="px-6 py-4 font-mono text-xs text-shade-60">{entry.product_sku}</td>
                    <td className="px-6 py-4 text-xs font-semibold">{entry.product_name}</td>
                    <td className="px-6 py-4 text-xs text-shade-50 whitespace-nowrap">
                      {entry.effective_date}
                    </td>
                    <td className="px-6 py-4 text-xs text-shade-60 text-right tabular-nums">
                      {formatVND(entry.cost_price)}
                    </td>
                    <td className="px-6 py-4 text-xs font-semibold text-ink text-right tabular-nums">
                      {formatVND(entry.selling_price)}
                    </td>
                    <td className="px-6 py-4">
                      <Badge colorClassName={STATUS_STYLE[entry.status]}>
                        {STATUS_LABEL[entry.status]}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-xs text-shade-50 max-w-[140px] truncate">
                      {entry.notes || '—'}
                    </td>
                    <td className="px-6 py-4 text-right whitespace-nowrap">
                      <div className="flex gap-2 justify-end items-center">
                        {canWrite && entry.status === 'PENDING' && entry.created_by?.id === user?.id && (
                          <>
                            <button
                              onClick={() => { setEditTarget(entry); setShowForm(true); }}
                              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill border border-ink bg-canvas-light text-ink hover:bg-canvas-cream text-xs font-semibold transition-colors"
                            >
                              <Edit2 className="w-3.5 h-3.5" />
                              Sửa
                            </button>
                            <button
                              onClick={() => handleCancel(entry.id)}
                              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill border border-danger-200 text-danger-600 hover:bg-danger-50 text-xs font-semibold transition-colors"
                            >
                              <Ban className="w-3.5 h-3.5" />
                              Hủy
                            </button>
                          </>
                        )}
                        {canWrite && entry.status === 'APPROVED' && (
                          <button
                            onClick={() => handleReplace(entry)}
                            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill border border-ink bg-canvas-light text-ink hover:bg-canvas-cream text-xs font-semibold transition-colors"
                          >
                            <RefreshCw className="w-3.5 h-3.5" />
                            Cập nhật giá
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile: stacked card view */}
          <div className="flex flex-col gap-3 p-4 md:hidden">
            {paginated.map((entry) => (
              <div key={entry.id} className="rounded-lg border border-hairline-light bg-canvas-cream/30 overflow-hidden">
                <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                  <span className="font-mono text-xs text-shade-60">{entry.product_sku}</span>
                  <Badge colorClassName={STATUS_STYLE[entry.status]}>
                    {STATUS_LABEL[entry.status]}
                  </Badge>
                </div>
                <div className="p-4 flex flex-col gap-2 text-xs">
                  <div className="font-semibold">{entry.product_name}</div>
                  <p className="text-shade-50">Hiệu lực từ: <span className="font-medium text-ink">{entry.effective_date}</span></p>
                  <p className="text-shade-50">Giá vốn: <span className="text-ink tabular-nums">{formatVND(entry.cost_price)}</span></p>
                  <p className="text-shade-50">Giá bán: <span className="font-semibold text-ink tabular-nums">{formatVND(entry.selling_price)}</span></p>
                  <p className="text-shade-50">Ghi chú: <span className="text-ink">{entry.notes || '—'}</span></p>
                </div>
                {canWrite && entry.status === 'PENDING' && entry.created_by?.id === user?.id && (
                  <div className="p-4 border-t border-hairline-light flex gap-2 justify-end items-center">
                    <button
                      onClick={() => { setEditTarget(entry); setShowForm(true); }}
                      className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill border border-ink bg-canvas-light text-ink hover:bg-canvas-cream text-xs font-semibold transition-colors"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                      Sửa
                    </button>
                    <button
                      onClick={() => handleCancel(entry.id)}
                      className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill border border-danger-200 text-danger-600 hover:bg-danger-50 text-xs font-semibold transition-colors"
                    >
                      <Ban className="w-3.5 h-3.5" />
                      Hủy
                    </button>
                  </div>
                )}
                {canWrite && entry.status === 'APPROVED' && (
                  <div className="p-4 border-t border-hairline-light flex gap-2 justify-end items-center">
                    <button
                      onClick={() => handleReplace(entry)}
                      className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill border border-ink bg-canvas-light text-ink hover:bg-canvas-cream text-xs font-semibold transition-colors"
                    >
                      <RefreshCw className="w-3.5 h-3.5" />
                      Cập nhật giá
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
          <Pagination
            currentPage={safePage}
            totalPages={totalPages}
            totalItems={filtered.length}
            pageSize={pageSize}
            onPageChange={setCurrentPage}
            onPageSizeChange={(s) => { setPageSize(s); setCurrentPage(1); }}
          />
        </div>
      )}

      {showForm && (
        <PriceEntryModal
          entry={editTarget}
          replaceSource={replaceTarget}
          warehouseId={warehouseId}
          warehouseName={activeWarehouse?.name}
          onClose={() => { setShowForm(false); setEditTarget(null); setReplaceTarget(null); }}
          onSaved={() => { setShowForm(false); setEditTarget(null); setReplaceTarget(null); fetchEntries(); }}
        />
      )}

      {showImport && (
        <ImportModal
          onClose={() => setShowImport(false)}
          onDone={() => { setShowImport(false); fetchEntries(); }}
        />
      )}
    </div>
  );
}

// ── PriceEntryModal ────────────────────────────────────────────────────────

function PriceEntryModal({ entry, replaceSource, warehouseId, warehouseName, onClose, onSaved }) {
  const { addToast } = useUiStore();
  const isEdit = !!entry;
  const isReplace = !isEdit && !!replaceSource;
  const seed = entry ?? replaceSource ?? null;
  const [form, setForm] = useState({
    product_id: seed?.product_id ?? '',
    warehouse_id: seed?.warehouse_id ?? warehouseId ?? '',
    effective_date: isEdit ? (entry?.effective_date ?? '') : isReplace ? todayISO() : '',
    cost_price: seed?.cost_price ?? '',
    selling_price: seed?.selling_price ?? '',
    notes: entry?.notes ?? '',
  });
  const [submitting, setSubmitting] = useState(false);

  const [products, setProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [showSearchResults, setShowSearchResults] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);

  const set = (key, val) => setForm(f => ({ ...f, [key]: val }));

  useEffect(() => {
    const fetchProducts = async () => {
      setLoadingProducts(true);
      try {
        const data = await masterDataService.getProducts();
        const activeProducts = data.filter(p => p.is_active);
        setProducts(activeProducts);

        if (seed?.product_id) {
          const prod = activeProducts.find(p => p.id === seed.product_id);
          if (prod) {
            setSelectedProduct(prod);
          } else {
            setSelectedProduct({
              id: seed.product_id,
              sku: seed.product_sku || '',
              name: seed.product_name || 'Sản phẩm không xác định'
            });
          }
        }
      } catch (err) {
        addToast('Không tải được danh sách sản phẩm', 'error');
      } finally {
        setLoadingProducts(false);
      }
    };
    fetchProducts();
  }, [seed, addToast]);

  const debouncedSearchQuery = useDebounce(searchQuery, 250);

  useEffect(() => {
    if (!debouncedSearchQuery.trim()) {
      setSearchResults([]);
      return;
    }
    const filtered = products.filter(p =>
      p.sku.toLowerCase().includes(debouncedSearchQuery.toLowerCase()) ||
      p.name.toLowerCase().includes(debouncedSearchQuery.toLowerCase())
    );
    setSearchResults(filtered);
  }, [debouncedSearchQuery, products]);

  // Đóng dropdown khi click ngoài
  useEffect(() => {
    const handleClickOutside = () => {
      setShowSearchResults(false);
    };
    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, []);

  const handleSelectProduct = (product) => {
    setSelectedProduct(product);
    set('product_id', product.id);
    setSearchQuery('');
    setShowSearchResults(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.product_id || !form.warehouse_id || !form.effective_date || !form.cost_price || !form.selling_price) {
      addToast('Vui lòng điền đầy đủ các trường bắt buộc', 'error');
      return;
    }
    setSubmitting(true);
    try {
      if (isEdit) {
        await pricingService.update(entry.id, form);
        addToast('Đã cập nhật bản giá', 'success');
      } else {
        await pricingService.create({
          ...form,
          product_id: Number(form.product_id),
          warehouse_id: Number(form.warehouse_id),
        });
        addToast('Đã tạo bản giá, chờ Kế toán trưởng duyệt', 'success');
      }
      onSaved();
    } catch (err) {
      addToast(err.message || 'Không thể lưu bản giá', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-canvas-night/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-canvas-cream rounded-lg max-w-lg w-full border border-hairline-light shadow-level-4 overflow-hidden flex flex-col max-h-[85vh]">
        <div className="p-6 border-b border-hairline-light flex items-center justify-between bg-canvas-light">
          <div>
            <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
              Tài chính / Bảng giá
            </span>
            <h3 className="text-xl font-bold">{isEdit ? 'Sửa bản giá' : isReplace ? 'Cập nhật giá' : 'Thêm bản giá mới'}</h3>
          </div>
          <button onClick={onClose} className="p-1 hover:bg-canvas-cream rounded-pill transition-colors text-shade-50 hover:text-ink">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto flex-1 flex flex-col gap-4">
          {isReplace && (
            <div className="text-xs text-shade-50 bg-canvas-light border border-hairline-light rounded-lg p-3">
              Bản giá này sẽ thay thế bản giá <span className="font-semibold text-ink">đang APPROVED</span> hiện
              tại (hiệu lực từ <span className="font-semibold text-ink">{replaceSource.effective_date}</span>,
              giá bán <span className="font-semibold text-ink">{formatVND(replaceSource.selling_price)}</span>)
              kể từ ngày hiệu lực bạn chọn bên dưới. Bản giá cũ vẫn được giữ nguyên trong lịch sử.
            </div>
          )}
          {(isEdit || isReplace) ? (
            <div>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Sản phẩm
              </label>
              <input
                type="text"
                value={selectedProduct ? `${selectedProduct.sku} - ${selectedProduct.name}` : (seed?.product_name || `ID: ${form.product_id}`)}
                disabled
                className="text-input w-full bg-canvas-cream text-shade-50 cursor-not-allowed font-semibold"
              />
            </div>
          ) : selectedProduct ? (
            <div>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Sản phẩm <span className="text-danger-500">*</span>
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={`${selectedProduct.sku} - ${selectedProduct.name}`}
                  disabled
                  className="text-input flex-1 bg-canvas-cream text-shade-60 font-semibold"
                />
                <button
                  type="button"
                  onClick={() => {
                    setSelectedProduct(null);
                    set('product_id', '');
                  }}
                  className="btn-pill btn-pill-outline-light text-xs py-1.5 px-3 hover:text-danger-600 hover:border-danger-300"
                >
                  Thay đổi
                </button>
              </div>
            </div>
          ) : (
            <div className="relative" onClick={(e) => e.stopPropagation()}>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Tìm kiếm sản phẩm <span className="text-danger-500">*</span>
              </label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-60" />
                <input
                  type="text"
                  placeholder="Nhập tên sản phẩm hoặc SKU..."
                  value={searchQuery}
                  onChange={(e) => {
                    setSearchQuery(e.target.value);
                    setShowSearchResults(true);
                  }}
                  onFocus={() => setShowSearchResults(true)}
                  className="w-full text-input pl-9"
                />
                {loadingProducts && (
                  <div className="absolute right-3 top-1/2 -translate-y-1/2">
                    <Loader2 className="w-4 h-4 animate-spin text-shade-60" />
                  </div>
                )}
              </div>

              {showSearchResults && searchQuery.trim() !== '' && (
                <div className="absolute left-0 right-0 mt-1 bg-canvas-light border border-hairline-light rounded-lg shadow-level-4 max-h-60 overflow-y-auto z-50">
                  {searchResults.length === 0 ? (
                    <div className="p-3 text-xs text-shade-50 text-center">Không tìm thấy sản phẩm</div>
                  ) : (
                    searchResults.map(prod => (
                      <div
                        key={prod.id}
                        onClick={() => handleSelectProduct(prod)}
                        className="p-2.5 hover:bg-canvas-cream cursor-pointer transition-colors border-b border-hairline-light last:border-0 flex items-center justify-between text-xs"
                      >
                        <div>
                          <span className="font-bold block text-ink">{prod.sku}</span>
                          <span className="text-shade-50 block">{prod.name}</span>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              )}
            </div>
          )}

          <div>
            <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
              Kho áp dụng
            </label>
            <input
              type="text"
              value={entry?.warehouse_name ?? replaceSource?.warehouse_name ?? warehouseName ?? '—'}
              disabled
              className="text-input w-full bg-canvas-cream text-shade-50 cursor-not-allowed font-semibold"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
              Hiệu lực từ ngày <span className="text-danger-500">*</span>
            </label>
            <input type="date" value={form.effective_date} onChange={e => set('effective_date', e.target.value)}
              className="text-input w-full" />
            <p className="text-[11px] text-shade-50 mt-1">
              Bản giá có hiệu lực kể từ ngày này cho đến khi có bản giá APPROVED khác mới hơn thay thế.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Giá vốn (VNĐ) <span className="text-danger-500">*</span>
              </label>
              <input type="number" min="1" value={form.cost_price} onChange={e => set('cost_price', e.target.value)}
                className="text-input w-full" placeholder="0" />
            </div>
            <div>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Giá bán (VNĐ) <span className="text-danger-500">*</span>
              </label>
              <input type="number" min="1" value={form.selling_price} onChange={e => set('selling_price', e.target.value)}
                className="text-input w-full" placeholder="0" />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">Ghi chú</label>
            <textarea value={form.notes} onChange={e => set('notes', e.target.value)}
              rows={2} className="text-input w-full resize-none" placeholder="Tùy chọn..." />
          </div>
        </form>

        <div className="p-4 border-t border-hairline-light bg-canvas-cream flex justify-between gap-3">
          <button type="button" onClick={onClose} className="btn-pill btn-pill-outline-light text-xs">Đóng</button>
          <button onClick={handleSubmit} disabled={submitting}
            className="btn-pill btn-pill-primary text-xs py-1.5 px-5 disabled:opacity-50 flex items-center gap-1.5">
            {submitting ? <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Đang lưu...</> : isEdit ? 'Cập nhật' : isReplace ? 'Cập nhật giá' : 'Tạo bản giá'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── ImportModal ────────────────────────────────────────────────────────────

function ImportModal({ onClose, onDone }) {
  const { addToast } = useUiStore();
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleUpload = async () => {
    if (!file) { addToast('Vui lòng chọn file .xlsx', 'error'); return; }
    setSubmitting(true);
    try {
      const res = await pricingService.importExcel(file);
      setResult(res);
      if (res.failed_count === 0) {
        addToast(`Import thành công ${res.created_count} bản giá`, 'success');
      } else {
        addToast(`Import xong: ${res.created_count} thành công, ${res.failed_count} lỗi`, 'warning');
      }
    } catch (err) {
      addToast(err.message || 'Import thất bại', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-canvas-night/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-canvas-cream rounded-lg max-w-lg w-full border border-hairline-light shadow-level-4 overflow-hidden flex flex-col max-h-[85vh]">
        <div className="p-6 border-b border-hairline-light flex items-center justify-between bg-canvas-light">
          <div>
            <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
              Tài chính / Bảng giá
            </span>
            <h3 className="text-xl font-bold">Import bảng giá từ Excel</h3>
          </div>
          <button onClick={onClose} className="p-1 hover:bg-canvas-cream rounded-pill transition-colors text-shade-50 hover:text-ink">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 overflow-y-auto flex-1 flex flex-col gap-4">
          {!result ? (
            <>
              <p className="text-sm text-shade-50">
                Chọn file <span className="font-mono text-xs bg-canvas-cream px-1.5 py-0.5 rounded-pill">.xlsx</span> đúng
                cột: <span className="font-mono text-xs">product_sku, warehouse_code, effective_date, cost_price, selling_price, notes</span>.
                Tối đa 1.000 dòng.
              </p>
              <div className="border-2 border-dashed border-hairline-light rounded-lg p-8 text-center">
                <input type="file" accept=".xlsx" onChange={e => setFile(e.target.files[0])}
                  className="hidden" id="xlsx-upload" />
                <label htmlFor="xlsx-upload"
                  className="cursor-pointer flex flex-col items-center gap-2 text-shade-50 hover:text-ink transition-colors">
                  <Upload className="w-8 h-8" />
                  <span className="text-sm">{file ? file.name : 'Nhấn để chọn file .xlsx'}</span>
                </label>
              </div>
            </>
          ) : (
            <>
              <div className="grid grid-cols-3 gap-3 text-center">
                <StatBox label="Tổng dòng" value={result.total_rows} />
                <StatBox label="Thành công" value={result.created_count} color="text-success-700" />
                <StatBox label="Lỗi" value={result.failed_count} color="text-danger-600" />
              </div>
              {result.failed?.length > 0 && (
                <div className="max-h-48 overflow-y-auto rounded-lg border border-hairline-light">
                  <table className="w-full text-xs">
                    <thead className="bg-canvas-cream sticky top-0">
                      <tr>
                        <th className="px-3 py-2 text-left font-bold text-shade-60">Dòng</th>
                        <th className="px-3 py-2 text-left font-bold text-shade-60">SKU</th>
                        <th className="px-3 py-2 text-left font-bold text-shade-60">Lỗi</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {result.failed.map((f, i) => (
                        <tr key={i}>
                          <td className="px-3 py-2 tabular-nums">{f.row}</td>
                          <td className="px-3 py-2 font-mono">{f.product_sku}</td>
                          <td className="px-3 py-2 text-danger-600">{f.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </div>

        <div className="p-4 border-t border-hairline-light bg-canvas-cream flex justify-between gap-3">
          <button onClick={onClose} className="btn-pill btn-pill-outline-light text-xs">Đóng</button>
          {!result ? (
            <button onClick={handleUpload} disabled={submitting || !file}
              className="btn-pill btn-pill-primary text-xs py-1.5 px-5 disabled:opacity-50 flex items-center gap-1.5">
              {submitting ? <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Đang xử lý...</> : 'Tải lên'}
            </button>
          ) : result.created_count > 0 && (
            <button onClick={onDone} className="btn-pill btn-pill-primary text-xs py-1.5 px-5">
              Xem danh sách
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function StatBox({ label, value, color = 'text-ink' }) {
  return (
    <div className="bg-canvas-cream rounded-lg p-3 border border-hairline-light">
      <div className={`text-2xl font-bold ${color}`}>{value}</div>
      <div className="text-xs text-shade-50 mt-0.5">{label}</div>
    </div>
  );
}

function formatVND(n) {
  return Number(n).toLocaleString('vi-VN') + ' đ';
}

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}
