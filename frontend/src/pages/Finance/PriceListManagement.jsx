import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Upload, Download, Search, X, Edit2, Ban, ChevronDown } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import pricingService from '../../services/pricing.service';
import { ROLES } from '../../utils/constants';

const STATUS_LABEL = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', CANCELLED: 'Đã hủy' };
const STATUS_STYLE = {
  PENDING:   'bg-amber-50 text-amber-800 border-amber-300',
  APPROVED:  'bg-aloe-10 text-emerald-900 border-emerald-300',
  CANCELLED: 'bg-zinc-100 text-zinc-500 border-zinc-300',
};
const BADGE = 'text-[10px] font-semibold px-2 py-0.5 rounded-pill border uppercase';

export default function PriceListManagement() {
  const { user, hasRole } = useAuthStore();
  const { addToast } = useUiStore();

  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [showImport, setShowImport] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const canWrite = hasRole(ROLES.ACCOUNTANT);

  const fetchEntries = useCallback(async () => {
    setLoading(true);
    try {
      const params = statusFilter !== 'ALL' ? { status: statusFilter } : {};
      const data = await pricingService.getAll(params);
      setEntries(data);
    } catch (err) {
      addToast(err.message || 'Không tải được danh sách bảng giá', 'error');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => { fetchEntries(); }, [fetchEntries]);

  const filtered = entries.filter(e =>
    e.product_sku?.toLowerCase().includes(search.toLowerCase()) ||
    e.product_name?.toLowerCase().includes(search.toLowerCase())
  );

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
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold tracking-tight text-ink">Bảng giá</h1>
          <p className="text-sm text-shade-50 mt-0.5">Quản lý giá bán & giá vốn theo kỳ</p>
        </div>
        {canWrite && (
          <div className="flex gap-2">
            <button onClick={() => setShowImport(true)}
              className="btn-pill-outline-light flex items-center gap-1.5 text-sm px-4 py-2">
              <Upload className="w-4 h-4" /> Import Excel
            </button>
            <a href={pricingService.getTemplateUrl()} download
              className="btn-pill-outline-light flex items-center gap-1.5 text-sm px-4 py-2">
              <Download className="w-4 h-4" /> Tải mẫu
            </a>
            <button onClick={() => { setEditTarget(null); setShowForm(true); }}
              className="btn-pill-primary flex items-center gap-1.5 text-sm px-4 py-2">
              <Plus className="w-4 h-4" /> Thêm bản giá
            </button>
          </div>
        )}
      </div>

      {/* Filters */}
      <div className="flex gap-3 items-center">
        <div className="relative flex-1 max-w-xs">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-40" />
          <input value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Tìm SKU hoặc tên sản phẩm..."
            className="text-input pl-9 w-full text-sm" />
        </div>
        {['ALL', 'PENDING', 'APPROVED', 'CANCELLED'].map(s => (
          <button key={s} onClick={() => setStatusFilter(s)}
            className={`btn-pill text-xs px-3 py-1.5 border transition-colors
              ${statusFilter === s
                ? 'bg-ink text-onPrimary border-ink'
                : 'bg-canvas-cream text-shade-60 border-hairline-light hover:border-shade-40'}`}>
            {s === 'ALL' ? 'Tất cả' : STATUS_LABEL[s]}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="card-premium overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <div className="w-6 h-6 border-2 border-ink border-t-transparent rounded-full animate-spin" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-20 text-shade-50 text-sm">Không có bản giá nào</div>
        ) : (
          <table className="w-full">
            <thead className="bg-zinc-50 border-b border-hairline-light">
              <tr>
                {['SKU', 'Sản phẩm', 'Hiệu lực', 'Giá vốn', 'Giá bán', 'Trạng thái', 'Ghi chú', ''].map(h => (
                  <th key={h} className="px-5 py-3.5 text-left text-xs font-bold text-shade-60 uppercase tracking-wider">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline-light">
              {filtered.map(entry => (
                <tr key={entry.id} className="hover:bg-zinc-50 transition-colors">
                  <td className="px-5 py-3.5 font-mono text-xs text-shade-60">{entry.product_sku}</td>
                  <td className="px-5 py-3.5 text-sm text-ink font-medium">{entry.product_name}</td>
                  <td className="px-5 py-3.5 text-xs text-shade-60 whitespace-nowrap">
                    {entry.effective_date} → {entry.end_date}
                  </td>
                  <td className="px-5 py-3.5 text-sm text-ink text-right tabular-nums">
                    {formatVND(entry.cost_price)}
                  </td>
                  <td className="px-5 py-3.5 text-sm font-semibold text-ink text-right tabular-nums">
                    {formatVND(entry.selling_price)}
                  </td>
                  <td className="px-5 py-3.5">
                    <span className={`${BADGE} ${STATUS_STYLE[entry.status]}`}>
                      {STATUS_LABEL[entry.status]}
                    </span>
                  </td>
                  <td className="px-5 py-3.5 text-xs text-shade-50 max-w-[160px] truncate">
                    {entry.notes || '—'}
                  </td>
                  <td className="px-5 py-3.5">
                    {canWrite && entry.status === 'PENDING' &&
                      entry.created_by?.id === user?.id && (
                      <div className="flex gap-1 justify-end">
                        <button onClick={() => { setEditTarget(entry); setShowForm(true); }}
                          className="p-1.5 rounded-md hover:bg-zinc-100 text-shade-50 hover:text-ink transition-colors">
                          <Edit2 className="w-3.5 h-3.5" />
                        </button>
                        <button onClick={() => handleCancel(entry.id)}
                          className="p-1.5 rounded-md hover:bg-red-50 text-shade-50 hover:text-red-600 transition-colors">
                          <Ban className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Create/Edit modal */}
      {showForm && (
        <PriceEntryModal
          entry={editTarget}
          onClose={() => { setShowForm(false); setEditTarget(null); }}
          onSaved={() => { setShowForm(false); setEditTarget(null); fetchEntries(); }}
        />
      )}

      {/* Import modal */}
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

function PriceEntryModal({ entry, onClose, onSaved }) {
  const { addToast } = useUiStore();
  const [form, setForm] = useState({
    product_id: entry?.product_id ?? '',
    effective_date: entry?.effective_date ?? '',
    end_date: entry?.end_date ?? '',
    cost_price: entry?.cost_price ?? '',
    selling_price: entry?.selling_price ?? '',
    notes: entry?.notes ?? '',
  });
  const [submitting, setSubmitting] = useState(false);
  const isEdit = !!entry;

  const set = (key, val) => setForm(f => ({ ...f, [key]: val }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.product_id || !form.effective_date || !form.end_date
        || !form.cost_price || !form.selling_price) {
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
        await pricingService.create({ ...form, product_id: Number(form.product_id) });
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
      <div className="bg-canvas-cream rounded-xl border border-hairline-light shadow-2xl w-full max-w-lg">
        <div className="flex items-center justify-between px-6 py-4 border-b border-hairline-light">
          <h2 className="font-display text-lg font-semibold tracking-tight">
            {isEdit ? 'Sửa bản giá' : 'Thêm bản giá mới'}
          </h2>
          <button onClick={onClose} className="p-1.5 rounded-md hover:bg-zinc-100 text-shade-50">
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-shade-60 uppercase tracking-wider mb-1.5">
              Mã sản phẩm (ID) <span className="text-red-500">*</span>
            </label>
            <input type="number" value={form.product_id} onChange={e => set('product_id', e.target.value)}
              disabled={isEdit} className="text-input w-full text-sm" placeholder="Nhập product ID" />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-shade-60 uppercase tracking-wider mb-1.5">
                Từ ngày <span className="text-red-500">*</span>
              </label>
              <input type="date" value={form.effective_date} onChange={e => set('effective_date', e.target.value)}
                className="text-input w-full text-sm" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-shade-60 uppercase tracking-wider mb-1.5">
                Đến ngày <span className="text-red-500">*</span>
              </label>
              <input type="date" value={form.end_date} onChange={e => set('end_date', e.target.value)}
                className="text-input w-full text-sm" />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-shade-60 uppercase tracking-wider mb-1.5">
                Giá vốn (VNĐ) <span className="text-red-500">*</span>
              </label>
              <input type="number" min="1" value={form.cost_price} onChange={e => set('cost_price', e.target.value)}
                className="text-input w-full text-sm" placeholder="0" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-shade-60 uppercase tracking-wider mb-1.5">
                Giá bán (VNĐ) <span className="text-red-500">*</span>
              </label>
              <input type="number" min="1" value={form.selling_price} onChange={e => set('selling_price', e.target.value)}
                className="text-input w-full text-sm" placeholder="0" />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-shade-60 uppercase tracking-wider mb-1.5">Ghi chú</label>
            <textarea value={form.notes} onChange={e => set('notes', e.target.value)}
              rows={2} className="text-input w-full text-sm resize-none" placeholder="Tùy chọn..." />
          </div>

          <div className="flex gap-2 pt-2">
            <button type="button" onClick={onClose}
              className="btn-pill-outline-light flex-1 text-sm py-2">Hủy</button>
            <button type="submit" disabled={submitting}
              className="btn-pill-primary flex-1 text-sm py-2 disabled:opacity-50">
              {submitting ? 'Đang lưu...' : isEdit ? 'Cập nhật' : 'Tạo bản giá'}
            </button>
          </div>
        </form>
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
      <div className="bg-canvas-cream rounded-xl border border-hairline-light shadow-2xl w-full max-w-lg">
        <div className="flex items-center justify-between px-6 py-4 border-b border-hairline-light">
          <h2 className="font-display text-lg font-semibold tracking-tight">Import bảng giá từ Excel</h2>
          <button onClick={onClose} className="p-1.5 rounded-md hover:bg-zinc-100 text-shade-50">
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          {!result ? (
            <>
              <p className="text-sm text-shade-50">
                Chọn file <span className="font-mono text-xs bg-zinc-100 px-1.5 py-0.5 rounded">.xlsx</span> theo
                đúng cột: <span className="font-mono text-xs">product_sku, effective_date, end_date, cost_price, selling_price, notes</span>.
                Tối đa 1.000 dòng.
              </p>
              <div className="border-2 border-dashed border-hairline-light rounded-lg p-6 text-center">
                <input type="file" accept=".xlsx" onChange={e => setFile(e.target.files[0])}
                  className="hidden" id="xlsx-upload" />
                <label htmlFor="xlsx-upload"
                  className="cursor-pointer flex flex-col items-center gap-2 text-shade-50 hover:text-ink transition-colors">
                  <Upload className="w-8 h-8" />
                  <span className="text-sm">{file ? file.name : 'Nhấn để chọn file .xlsx'}</span>
                </label>
              </div>
              <div className="flex gap-2">
                <button onClick={onClose} className="btn-pill-outline-light flex-1 text-sm py-2">Hủy</button>
                <button onClick={handleUpload} disabled={submitting || !file}
                  className="btn-pill-primary flex-1 text-sm py-2 disabled:opacity-50">
                  {submitting ? 'Đang xử lý...' : 'Tải lên'}
                </button>
              </div>
            </>
          ) : (
            <>
              <div className="grid grid-cols-3 gap-3 text-center">
                <Stat label="Tổng dòng" value={result.total_rows} />
                <Stat label="Thành công" value={result.created_count} color="text-emerald-700" />
                <Stat label="Lỗi" value={result.failed_count} color="text-red-600" />
              </div>
              {result.failed?.length > 0 && (
                <div className="max-h-48 overflow-y-auto rounded-lg border border-hairline-light">
                  <table className="w-full text-xs">
                    <thead className="bg-zinc-50 sticky top-0">
                      <tr>
                        <th className="px-3 py-2 text-left text-shade-60">Dòng</th>
                        <th className="px-3 py-2 text-left text-shade-60">SKU</th>
                        <th className="px-3 py-2 text-left text-shade-60">Lỗi</th>
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
              <div className="flex gap-2">
                <button onClick={onClose} className="btn-pill-outline-light flex-1 text-sm py-2">Đóng</button>
                {result.created_count > 0 && (
                  <button onClick={onDone} className="btn-pill-primary flex-1 text-sm py-2">Xem danh sách</button>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function Stat({ label, value, color = 'text-ink' }) {
  return (
    <div className="bg-zinc-50 rounded-lg p-3 border border-hairline-light">
      <div className={`text-2xl font-display font-semibold ${color}`}>{value}</div>
      <div className="text-xs text-shade-50 mt-0.5">{label}</div>
    </div>
  );
}

function formatVND(n) {
  return Number(n).toLocaleString('vi-VN') + ' đ';
}
