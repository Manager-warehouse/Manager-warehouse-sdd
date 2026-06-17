import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ClipboardList, ArrowLeft } from 'lucide-react';
import { useUiStore } from '../../stores/ui.store';
import { stocktakeService } from '../../services/stocktake.service';

const StocktakeForm = () => {
  const navigate = useNavigate();
  const { activeWarehouse, showToast } = useUiStore();

  const today = new Date().toISOString().slice(0, 10);

  const [form, setForm] = useState({
    stock_take_date: today,
    document_date: today,
    accounting_period_id: 1, // default period; real impl would fetch open periods
    notes: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState({});

  const set = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setErrors((prev) => ({ ...prev, [field]: undefined }));
  };

  const validate = () => {
    const errs = {};
    if (!form.stock_take_date) errs.stock_take_date = 'Bắt buộc';
    if (!form.document_date) errs.document_date = 'Bắt buộc';
    if (!form.accounting_period_id) errs.accounting_period_id = 'Bắt buộc';
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }
    if (!activeWarehouse?.id) {
      showToast?.('error', 'Vui lòng chọn kho trước khi tạo phiếu kiểm kê');
      return;
    }

    setSubmitting(true);
    try {
      const created = await stocktakeService.createStockTake({
        warehouse_id: activeWarehouse.id,
        stock_take_date: form.stock_take_date,
        document_date: form.document_date,
        accounting_period_id: Number(form.accounting_period_id),
        notes: form.notes || undefined,
      });
      showToast?.('success', `Đã tạo phiếu kiểm kê ${created.stock_take_number}`);
      navigate(`/stocktake/${created.id}`);
    } catch (err) {
      showToast?.('error', err.message || 'Tạo phiếu kiểm kê thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="p-6 max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate('/stocktake')}
          className="p-2 rounded-xl text-shade-50 hover:bg-canvas-cream transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
        </button>
        <ClipboardList className="w-6 h-6 text-aloe-40" />
        <div>
          <h1 className="text-xl font-bold text-canvas-night">Tạo phiếu kiểm kê</h1>
          {activeWarehouse && (
            <p className="text-xs text-shade-50">{activeWarehouse.name}</p>
          )}
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="bg-white rounded-2xl border border-hairline shadow-xs p-6 space-y-5">
        {/* Warehouse (read-only) */}
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-shade-40 uppercase tracking-wider">Kho kiểm kê</label>
          <div className="px-3 py-2.5 rounded-xl bg-canvas-cream border border-hairline text-sm text-shade-30">
            {activeWarehouse?.name || '(Chưa chọn kho)'}
          </div>
        </div>

        {/* Stock Take Date */}
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-shade-40 uppercase tracking-wider">
            Ngày kiểm kê <span className="text-red-500">*</span>
          </label>
          <input
            type="date"
            value={form.stock_take_date}
            onChange={(e) => set('stock_take_date', e.target.value)}
            className={`w-full px-3 py-2.5 rounded-xl border text-sm outline-none transition-colors ${
              errors.stock_take_date ? 'border-red-400 bg-red-50' : 'border-hairline focus:border-aloe-40'
            }`}
          />
          {errors.stock_take_date && (
            <p className="text-xs text-red-500">{errors.stock_take_date}</p>
          )}
        </div>

        {/* Document Date */}
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-shade-40 uppercase tracking-wider">
            Ngày chứng từ <span className="text-red-500">*</span>
          </label>
          <input
            type="date"
            value={form.document_date}
            onChange={(e) => set('document_date', e.target.value)}
            className={`w-full px-3 py-2.5 rounded-xl border text-sm outline-none transition-colors ${
              errors.document_date ? 'border-red-400 bg-red-50' : 'border-hairline focus:border-aloe-40'
            }`}
          />
          {errors.document_date && (
            <p className="text-xs text-red-500">{errors.document_date}</p>
          )}
        </div>

        {/* Accounting Period */}
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-shade-40 uppercase tracking-wider">
            Kỳ kế toán <span className="text-red-500">*</span>
          </label>
          <select
            value={form.accounting_period_id}
            onChange={(e) => set('accounting_period_id', e.target.value)}
            className={`w-full px-3 py-2.5 rounded-xl border text-sm outline-none transition-colors ${
              errors.accounting_period_id ? 'border-red-400 bg-red-50' : 'border-hairline focus:border-aloe-40'
            }`}
          >
            <option value={1}>T06/2026 (đang mở)</option>
          </select>
          {errors.accounting_period_id && (
            <p className="text-xs text-red-500">{errors.accounting_period_id}</p>
          )}
        </div>

        {/* Notes */}
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-shade-40 uppercase tracking-wider">Ghi chú</label>
          <textarea
            rows={3}
            value={form.notes}
            onChange={(e) => set('notes', e.target.value)}
            placeholder="Ghi chú thêm (tuỳ chọn)"
            className="w-full px-3 py-2.5 rounded-xl border border-hairline focus:border-aloe-40 text-sm outline-none transition-colors resize-none"
          />
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={() => navigate('/stocktake')}
            className="px-5 py-2.5 rounded-pill text-xs font-semibold border border-hairline text-shade-50 hover:bg-canvas-cream transition-colors"
          >
            Hủy
          </button>
          <button
            type="submit"
            disabled={submitting || !activeWarehouse}
            className="px-5 py-2.5 rounded-pill text-xs font-semibold bg-aloe-40 text-white hover:bg-aloe-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {submitting ? 'Đang tạo...' : 'Tạo phiếu kiểm kê'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default StocktakeForm;
