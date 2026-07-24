import React, { useState } from 'react';
import { Wrench } from 'lucide-react';
import { financeService } from '../../services/finance.service';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import { ROLES } from '../../utils/constants';
import Modal from './Modal';
import Button from './Button';
import Input from './Input';

// Contextual "Điều chỉnh" action for a single AR/AP document row whose own
// accounting period is already CLOSED (US-WMS-29). Deliberately not a separate
// page: it only ever makes sense in the context of one already-identified row,
// so referenceType/referenceId are supplied by the caller rather than picked
// inside a standalone form. Renders nothing unless the actor is ACCOUNTANT_MANAGER
// (or ADMIN) and the row's period is actually CLOSED - showing it otherwise would
// just let the user hit the backend's ORIGINAL_PERIOD_NOT_CLOSED rejection.
const CorrectionVoucherButton = ({
  referenceType,
  referenceId,
  documentLabel,
  isPeriodClosed,
  onSuccess
}) => {
  const { addToast } = useUiStore();
  const { hasRole } = useAuthStore();
  const canCorrect = hasRole(ROLES.ACCOUNTANT_MANAGER) || hasRole(ROLES.ADMIN);

  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({
    amountDelta: '',
    reason: '',
    documentDate: new Date().toISOString().slice(0, 10)
  });

  if (!canCorrect || !isPeriodClosed) {
    return null;
  }

  const openModal = () => {
    setForm({
      amountDelta: '',
      reason: '',
      documentDate: new Date().toISOString().slice(0, 10)
    });
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.amountDelta || Number(form.amountDelta) === 0) {
      addToast('Vui lòng nhập số tiền điều chỉnh khác 0', 'error');
      return;
    }
    if (!form.reason.trim()) {
      addToast('Vui lòng nhập lý do điều chỉnh', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await financeService.createCorrectionVoucher({
        referenceType,
        referenceId,
        amountDelta: Number(form.amountDelta),
        reason: form.reason.trim(),
        documentDate: form.documentDate
      });
      addToast('Đã tạo Bút toán Điều chỉnh và cập nhật công nợ.', 'success');
      setShowModal(false);
      onSuccess?.();
    } catch (err) {
      console.error('Create correction voucher failed:', err);
      addToast(err.message || 'Không thể tạo bút toán điều chỉnh', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <button
        onClick={openModal}
        title="Chứng từ thuộc kỳ đã chốt sổ - tạo bút toán điều chỉnh thay vì sửa trực tiếp"
        className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-canvas-cream text-ink text-[11px] font-semibold hover:bg-hairline-light"
      >
        <Wrench className="w-3 h-3" />
        Điều chỉnh
      </button>

      <Modal
        isOpen={showModal}
        onClose={() => !submitting && setShowModal(false)}
        title="Lập Bút toán Điều chỉnh"
        maxWidth="max-w-md"
      >
        <div className="flex flex-col gap-4 text-xs">
          <div className="p-3 bg-amber-50 border border-amber-200 rounded text-amber-800 leading-relaxed">
            Chứng từ {documentLabel ? <strong>{documentLabel}</strong> : 'này'} thuộc kỳ kế toán đã{' '}
            <strong>chốt sổ</strong>. Bút toán điều chỉnh sẽ được ghi vào kỳ hiện đang mở, không sửa
            trực tiếp chứng từ gốc.
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <Input
              id="amountDelta"
              label="Số tiền điều chỉnh (VND, dương = tăng nợ, âm = giảm nợ)"
              type="number"
              value={form.amountDelta}
              onChange={(e) => setForm((prev) => ({ ...prev, amountDelta: e.target.value }))}
              placeholder="ví dụ: -2000000"
              required
            />

            <Input
              id="documentDate"
              label="Ngày hạch toán (thuộc kỳ đang mở)"
              type="date"
              value={form.documentDate}
              onChange={(e) => setForm((prev) => ({ ...prev, documentDate: e.target.value }))}
              required
            />

            <div className="flex flex-col gap-1">
              <label className="font-semibold text-ink text-xs">
                Lý do điều chỉnh <span className="text-red-500">*</span>
              </label>
              <textarea
                value={form.reason}
                onChange={(e) => setForm((prev) => ({ ...prev, reason: e.target.value }))}
                className="bg-canvas-light border border-hairline-light rounded p-2 text-ink min-h-[70px] text-xs"
                placeholder="ví dụ: Hóa đơn ghi nhầm đơn giá, kỳ đã chốt sổ"
                required
              />
            </div>

            <div className="flex justify-end gap-3 mt-2 pt-3 border-t border-hairline-light">
              <Button type="button" variant="outline-light" onClick={() => setShowModal(false)} disabled={submitting}>
                Hủy bỏ
              </Button>
              <Button type="submit" variant="primary" loading={submitting}>
                Lưu Bút toán
              </Button>
            </div>
          </form>
        </div>
      </Modal>
    </>
  );
};

export default CorrectionVoucherButton;
