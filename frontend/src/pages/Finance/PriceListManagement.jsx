import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Upload, Download, Search, X, Edit2, Ban, DollarSign, Loader2, Warehouse } from 'lucide-react';
import Pagination from '../../components/common/Pagination';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import pricingService from '../../services/pricing.service';
import { masterDataService } from '../../services/masterData.service';
import { ROLES } from '../../utils/constants';

const STATUS_LABEL = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', CANCELLED: 'Đã hủy' };
const STATUS_STYLE = {
  PENDING:   'bg-amber-50 text-amber-800 border-amber-300',
  APPROVED:  'bg-aloe-10 text-emerald-900 border-emerald-300',
  CANCELLED: 'bg-zinc-100 text-zinc-500 border-zinc-300',
};
const BADGE = 'text-[10px] font-semibold px-2 py-0.5 rounded-pill border uppercase tracking-wider whitespace-nowrap';

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

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
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
        <div className="flex gap-2 flex-wrap justify-end">
          <button onClick={handleExportXlsx}
            className="btn-pill btn-pill-outline-light flex items-center gap-2 text-sm">
            <Upload className="w-4 h-4" /> Xuất Excel
          </button>
          {canWrite && (
            <>
              <button onClick={() => setShowImport(true)}
                className="btn-pill btn-pill-outline-light flex items-center gap-2 text-sm">
                <Download className="w-4 h-4" /> Nhập Excel
              </button>
              <button onClick={() => pricingService.downloadTemplate().catch(() => addToast('Không tải được file mẫu', 'error'))}
                className="btn-pill btn-pill-outline-light flex items-center gap-2 text-sm">
                <Upload className="w-4 h-4" /> Tải mẫu
              </button>
              <button onClick={() => { setEditTarget(null); setShowForm(true); }}
                className="btn-pill btn-pill-primary flex items-center gap-2">
                <Plus className="w-4 h-4" />
                <span>Thêm bản giá</span>
              </button>
            </>
          )}
        </div>
      </div>

      {/* KPI Summary */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        {[
          { label: 'Tổng bản giá', value: totalEntries, icon: <DollarSign className="w-5 h-5" />, accent: 'text-zinc-600 bg-zinc-100' },
          { label: 'Chờ duyệt', value: pendingCount, icon: <DollarSign className="w-5 h-5" />, accent: 'text-amber-600 bg-amber-50' },
          { label: 'Đã duyệt', value: approvedCount, icon: <DollarSign className="w-5 h-5" />, accent: 'text-emerald-600 bg-emerald-50' },
        ].map(({ label, value, icon, accent }) => (
          <div key={label} className="bg-white rounded-lg border border-hairline-light p-4 shadow-sm flex items-center gap-3">
            <div className={`p-2.5 rounded-full ${accent}`}>{icon}</div>
            <div>
              <p className="text-xs text-shade-50 font-medium">{label}</p>
              <p className="text-2xl font-bold text-ink">{value}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg border border-hairline-light p-4 shadow-sm flex flex-col md:flex-row gap-4 items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-40" />
          <input
            type="text"
            placeholder="Tìm SKU hoặc tên sản phẩm..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full text-input pl-10"
          />
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-shade-50">Trạng thái:</span>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="text-input text-xs py-1.5">
            <option value="ALL">Tất cả</option>
            <option value="PENDING">Chờ duyệt</option>
            <option value="APPROVED">Đã duyệt</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>
      </div>

      {/* Table */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm">
          <DollarSign className="w-12 h-12 text-shade-30 mx-auto mb-4" />
          <h3 className="text-lg font-bold mb-1">Không tìm thấy bản giá nào</h3>
          <p className="text-sm text-shade-50">Thay đổi bộ lọc hoặc thêm bản giá mới để bắt đầu.</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg border border-hairline-light shadow-sm overflow-hidden card-premium">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-zinc-50 border-b border-hairline-light">
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">SKU</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Sản phẩm</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Kỳ hiệu lực</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider text-right">Giá vốn</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider text-right">Giá bán</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Trạng thái</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Ghi chú</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {paginated.map((entry) => (
                  <tr key={entry.id} className="hover:bg-zinc-50 transition-colors">
                    <td className="px-6 py-4 font-mono text-xs text-shade-60">{entry.product_sku}</td>
                    <td className="px-6 py-4 text-xs font-semibold">{entry.product_name}</td>
                    <td className="px-6 py-4 text-xs text-shade-50 whitespace-nowrap">
                      {entry.effective_date} → {entry.end_date}
                    </td>
                    <td className="px-6 py-4 text-xs text-shade-60 text-right tabular-nums">
                      {formatVND(entry.cost_price)}
                    </td>
                    <td className="px-6 py-4 text-xs font-semibold text-ink text-right tabular-nums">
                      {formatVND(entry.selling_price)}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`${BADGE} ${STATUS_STYLE[entry.status]}`}>
                        {STATUS_LABEL[entry.status]}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-xs text-shade-50 max-w-[140px] truncate">
                      {entry.notes || '—'}
                    </td>
                    <td className="px-6 py-4 text-right whitespace-nowrap">
                      {canWrite && entry.status === 'PENDING' && entry.created_by?.id === user?.id && (
                        <div className="flex gap-2 justify-end items-center">
                          <button
                            onClick={() => { setEditTarget(entry); setShowForm(true); }}
                            className="inline-flex items-center justify-center rounded-full border border-ink bg-canvas-light text-ink hover:bg-zinc-100 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
                          >
                            <Edit2 className="w-3 h-3 mr-1" /> Sửa
                          </button>
                          <button
                            onClick={() => handleCancel(entry.id)}
                            className="inline-flex items-center justify-center rounded-full border border-red-300 text-red-600 hover:bg-red-50 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
                          >
                            <Ban className="w-3 h-3 mr-1" /> Hủy
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
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
          warehouseId={warehouseId}
          warehouseName={activeWarehouse?.name}
          onClose={() => { setShowForm(false); setEditTarget(null); }}
          onSaved={() => { setShowForm(false); setEditTarget(null); fetchEntries(); }}
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

function PriceEntryModal({ entry, warehouseId, warehouseName, onClose, onSaved }) {
  const { addToast } = useUiStore();
  const [form, setForm] = useState({
    product_id: entry?.product_id ?? '',
    warehouse_id: entry?.warehouse_id ?? warehouseId ?? '',
    effective_date: entry?.effective_date ?? '',
    end_date: entry?.end_date ?? '',
    cost_price: entry?.cost_price ?? '',
    selling_price: entry?.selling_price ?? '',
    notes: entry?.notes ?? '',
  });
  const [submitting, setSubmitting] = useState(false);
  const isEdit = !!entry;

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

        if (isEdit && entry?.product_id) {
          const prod = activeProducts.find(p => p.id === entry.product_id);
          if (prod) {
            setSelectedProduct(prod);
          } else {
            setSelectedProduct({
              id: entry.product_id,
              sku: entry.product_sku || '',
              name: entry.product_name || 'Sản phẩm không xác định'
            });
          }
        }
      } catch (err) {
        console.error('Lỗi tải sản phẩm:', err);
        addToast('Không tải được danh sách sản phẩm', 'error');
      } finally {
        setLoadingProducts(false);
      }
    };
    fetchProducts();
  }, [isEdit, entry, addToast]);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      return;
    }
    const delayDebounce = setTimeout(() => {
      const filtered = products.filter(p =>
        p.sku.toLowerCase().includes(searchQuery.toLowerCase()) ||
        p.name.toLowerCase().includes(searchQuery.toLowerCase())
      );
      setSearchResults(filtered);
    }, 250);

    return () => clearTimeout(delayDebounce);
  }, [searchQuery, products]);

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
    if (!form.product_id || !form.warehouse_id || !form.effective_date || !form.end_date || !form.cost_price || !form.selling_price) {
      addToast('Vui lòng điền đầy đủ các trường bắt buộc', 'error');
      return;
    }
    if (form.effective_date > form.end_date) {
      addToast('Ngày bắt đầu phải trước ngày kết thúc', 'error');
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
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-canvas-cream rounded-lg max-w-lg w-full border border-hairline-light shadow-2xl overflow-hidden flex flex-col max-h-[85vh]">
        <div className="p-6 border-b border-hairline-light flex items-center justify-between bg-white">
          <div>
            <span className="text-[10px] font-bold text-shade-40 uppercase tracking-widest block mb-1">
              Tài chính / Bảng giá
            </span>
            <h3 className="text-xl font-bold">{isEdit ? 'Sửa bản giá' : 'Thêm bản giá mới'}</h3>
          </div>
          <button onClick={onClose} className="p-1 hover:bg-zinc-100 rounded-full transition-colors text-shade-50 hover:text-ink">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto flex-1 flex flex-col gap-4">
          {isEdit ? (
            <div>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Sản phẩm
              </label>
              <input
                type="text"
                value={selectedProduct ? `${selectedProduct.sku} - ${selectedProduct.name}` : (entry?.product_name || `ID: ${form.product_id}`)}
                disabled
                className="text-input w-full bg-zinc-50 text-shade-50 cursor-not-allowed font-semibold"
              />
            </div>
          ) : selectedProduct ? (
            <div>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Sản phẩm <span className="text-red-500">*</span>
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={`${selectedProduct.sku} - ${selectedProduct.name}`}
                  disabled
                  className="text-input flex-1 bg-zinc-50 text-shade-60 font-semibold"
                />
                <button
                  type="button"
                  onClick={() => {
                    setSelectedProduct(null);
                    set('product_id', '');
                  }}
                  className="btn-pill btn-pill-outline-light text-xs py-1.5 px-3 hover:text-red-600 hover:border-red-300"
                >
                  Thay đổi
                </button>
              </div>
            </div>
          ) : (
            <div className="relative" onClick={(e) => e.stopPropagation()}>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Tìm kiếm sản phẩm <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-40" />
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
                    <Loader2 className="w-4 h-4 animate-spin text-shade-40" />
                  </div>
                )}
              </div>

              {showSearchResults && searchQuery.trim() !== '' && (
                <div className="absolute left-0 right-0 mt-1 bg-white border border-hairline-light rounded-lg shadow-xl max-h-60 overflow-y-auto z-50">
                  {searchResults.length === 0 ? (
                    <div className="p-3 text-xs text-shade-50 text-center">Không tìm thấy sản phẩm</div>
                  ) : (
                    searchResults.map(prod => (
                      <div
                        key={prod.id}
                        onClick={() => handleSelectProduct(prod)}
                        className="p-2.5 hover:bg-zinc-50 cursor-pointer transition-colors border-b border-hairline-light last:border-0 flex items-center justify-between text-xs"
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
              value={entry?.warehouse_name ?? warehouseName ?? '—'}
              disabled
              className="text-input w-full bg-zinc-50 text-shade-50 cursor-not-allowed font-semibold"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Từ ngày <span className="text-red-500">*</span>
              </label>
              <input type="date" value={form.effective_date} onChange={e => set('effective_date', e.target.value)}
                className="text-input w-full" />
            </div>
            <div>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Đến ngày <span className="text-red-500">*</span>
              </label>
              <input type="date" value={form.end_date} onChange={e => set('end_date', e.target.value)}
                className="text-input w-full" />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Giá vốn (VNĐ) <span className="text-red-500">*</span>
              </label>
              <input type="number" min="1" value={form.cost_price} onChange={e => set('cost_price', e.target.value)}
                className="text-input w-full" placeholder="0" />
            </div>
            <div>
              <label className="block text-xs font-bold text-shade-60 uppercase tracking-wider mb-1.5">
                Giá bán (VNĐ) <span className="text-red-500">*</span>
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

        <div className="p-4 border-t border-hairline-light bg-zinc-50 flex justify-between gap-3">
          <button type="button" onClick={onClose} className="btn-pill btn-pill-outline-light text-xs">Đóng</button>
          <button onClick={handleSubmit} disabled={submitting}
            className="btn-pill btn-pill-primary text-xs py-1.5 px-5 disabled:opacity-50 flex items-center gap-1.5">
            {submitting ? <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Đang lưu...</> : isEdit ? 'Cập nhật' : 'Tạo bản giá'}
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
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-canvas-cream rounded-lg max-w-lg w-full border border-hairline-light shadow-2xl overflow-hidden flex flex-col max-h-[85vh]">
        <div className="p-6 border-b border-hairline-light flex items-center justify-between bg-white">
          <div>
            <span className="text-[10px] font-bold text-shade-40 uppercase tracking-widest block mb-1">
              Tài chính / Bảng giá
            </span>
            <h3 className="text-xl font-bold">Import bảng giá từ Excel</h3>
          </div>
          <button onClick={onClose} className="p-1 hover:bg-zinc-100 rounded-full transition-colors text-shade-50 hover:text-ink">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 overflow-y-auto flex-1 flex flex-col gap-4">
          {!result ? (
            <>
              <p className="text-sm text-shade-50">
                Chọn file <span className="font-mono text-xs bg-zinc-100 px-1.5 py-0.5 rounded">.xlsx</span> đúng
                cột: <span className="font-mono text-xs">product_sku, effective_date, end_date, cost_price, selling_price, notes</span>.
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
                <StatBox label="Thành công" value={result.created_count} color="text-emerald-700" />
                <StatBox label="Lỗi" value={result.failed_count} color="text-red-600" />
              </div>
              {result.failed?.length > 0 && (
                <div className="max-h-48 overflow-y-auto rounded-lg border border-hairline-light">
                  <table className="w-full text-xs">
                    <thead className="bg-zinc-50 sticky top-0">
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
                          <td className="px-3 py-2 text-red-600">{f.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </div>

        <div className="p-4 border-t border-hairline-light bg-zinc-50 flex justify-between gap-3">
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
    <div className="bg-zinc-50 rounded-lg p-3 border border-hairline-light">
      <div className={`text-2xl font-bold ${color}`}>{value}</div>
      <div className="text-xs text-shade-50 mt-0.5">{label}</div>
    </div>
  );
}

function formatVND(n) {
  return Number(n).toLocaleString('vi-VN') + ' đ';
}
