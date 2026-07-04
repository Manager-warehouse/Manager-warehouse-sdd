import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { stocktakeService } from '../../services/stocktake.service';
import Button from '../../components/common/Button';

const StocktakeForm = () => {
  const navigate = useNavigate();
  const { activeWarehouse } = useAuthStore();
  const { showToast } = useUiStore();

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
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex items-start gap-3">
        <button
          onClick={() => navigate('/stocktake')}
          className="p-2 rounded-pill text-shade-50 hover:bg-canvas-cream transition-colors mt-1 shrink-0"
        >
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Kiểm kê</span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">Tạo phiếu kiểm kê</h1>
          {activeWarehouse && (
            <p className="text-xs text-shade-50 font-light mt-1">{activeWarehouse.name}</p>
          )}
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 p-6 flex flex-col gap-5">
        {/* Grid 2 cột */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {/* Warehouse (read-only) */}
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold text-shade-40 uppercase tracking-wider">Kho kiểm kê</label>
            <div className="px-3 py-2.5 rounded-md bg-canvas-cream border border-hairline-light text-sm text-shade-30">
              {activeWarehouse?.name || '(Chưa chọn kho)'}
            </div>
          </div>

          {/* Accounting Period */}
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold text-shade-40 uppercase tracking-wider">
              Kỳ kế toán <span className="text-red-500">*</span>
            </label>
            <select
              value={form.accounting_period_id}
              onChange={(e) => set('accounting_period_id', e.target.value)}
              className={`w-full px-3 py-2.5 rounded-md border text-sm outline-none transition-colors ${
                errors.accounting_period_id ? 'border-red-400 bg-red-50' : 'border-hairline-light focus:border-ink'
              }`}
            >
              <option value={1}>T06/2026 (đang mở)</option>
            </select>
            {errors.accounting_period_id && (
              <p className="text-xs text-red-500">{errors.accounting_period_id}</p>
            )}
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
              className={`w-full px-3 py-2.5 rounded-md border text-sm outline-none transition-colors ${
                errors.stock_take_date ? 'border-red-400 bg-red-50' : 'border-hairline-light focus:border-ink'
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
              className={`w-full px-3 py-2.5 rounded-md border text-sm outline-none transition-colors ${
                errors.document_date ? 'border-red-400 bg-red-50' : 'border-hairline-light focus:border-ink'
              }`}
            />
            {errors.document_date && (
              <p className="text-xs text-red-500">{errors.document_date}</p>
            )}
          </div>
        </div>

        {/* Notes - full width */}
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-shade-40 uppercase tracking-wider">Ghi chú</label>
          <textarea
            rows={3}
            value={form.notes}
            onChange={(e) => set('notes', e.target.value)}
            placeholder="Ghi chú thêm (tuỳ chọn)"
            className="w-full px-3 py-2.5 rounded-md border border-hairline-light focus:border-ink text-sm outline-none transition-colors resize-none"
          />
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={() => navigate('/stocktake')}
            className="px-5 py-2.5 rounded-pill text-xs font-semibold border border-hairline-light text-shade-50 hover:bg-canvas-cream transition-colors"
          >
            Hủy
          </button>
          <Button
            type="submit"
            variant="primary"
            disabled={submitting || !activeWarehouse}
            loading={submitting}
          >
            {submitting ? 'Đang tạo...' : 'Tạo phiếu kiểm kê'}
          </Button>
        </div>
      </form>
    </div>
  );
};

export default StocktakeForm;
