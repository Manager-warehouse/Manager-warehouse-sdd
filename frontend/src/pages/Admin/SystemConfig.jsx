import React, { useState, useEffect, useCallback } from 'react';
import { adminService } from '../../services/admin.service';
import { useUiStore } from '../../stores/ui.store';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { Save, Settings, CreditCard, CalendarDays, BarChart3 } from 'lucide-react';


const validate = (form) => {
  const errs = {};

  const creditLimit = Number(form.defaultCreditLimit);
  if (!Number.isFinite(creditLimit) || !Number.isInteger(creditLimit) || creditLimit <= 0) {
    errs.defaultCreditLimit = 'Hạn mức nợ phải là số nguyên dương (> 0)';
  }

  const paymentTerm = Number(form.defaultPaymentTermDays);
  if (!Number.isFinite(paymentTerm) || !Number.isInteger(paymentTerm) || paymentTerm <= 0) {
    errs.defaultPaymentTermDays = 'Thời hạn thanh toán phải là số nguyên dương (> 0)';
  }

  const overdueDays = Number(form.creditHoldOverdueDays);
  if (!Number.isFinite(overdueDays) || !Number.isInteger(overdueDays) || overdueDays <= 0) {
    errs.creditHoldOverdueDays = 'Số ngày trễ hạn phải là số nguyên dương (> 0)';
  }

  const bufferPct = Number(form.creditUnlockBufferPct);
  if (!Number.isFinite(bufferPct) || bufferPct <= 0 || bufferPct > 1) {
    errs.creditUnlockBufferPct = 'Hệ số đệm mở khóa phải là số thập phân trong khoảng (0, 1]';
  }

  const closingDay = Number(form.monthlyClosingDay);
  if (!Number.isFinite(closingDay) || !Number.isInteger(closingDay) || closingDay < 1 || closingDay > 31) {
    errs.monthlyClosingDay = 'Ngày khóa sổ phải là số nguyên từ 1 đến 31';
  }

  const threshold = Number(form.minInventoryWarningThreshold);
  if (!Number.isFinite(threshold) || !Number.isInteger(threshold) || threshold < 0) {
    errs.minInventoryWarningThreshold = 'Ngưỡng cảnh báo tồn kho phải là số nguyên >= 0';
  }

  return errs;
};


const Section = ({ icon: Icon, title, children }) => (
  <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 p-6 md:p-8 flex flex-col gap-5">
    <div className="flex items-center gap-3 pb-4 border-b border-hairline-light">
      <Icon className="w-5 h-5 text-shade-60" />
      <h2 className="text-base font-semibold uppercase tracking-wider text-ink">{title}</h2>
    </div>
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">{children}</div>
  </div>
);

const Hint = ({ children }) => (
  <span className="text-[11px] text-shade-40 italic mt-1">{children}</span>
);

const DEFAULTS = {
  defaultCreditLimit: 500000000,
  defaultPaymentTermDays: 30,
  creditHoldOverdueDays: 30,
  creditUnlockBufferPct: 0.8,
  monthlyClosingDay: 5,
  minInventoryWarningThreshold: 10
};

const SystemConfig = () => {
  const { addToast } = useUiStore();
  const [loading, setLoading] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [form, setForm] = useState(DEFAULTS);
  const [errors, setErrors] = useState({});

  const loadConfig = useCallback(async () => {
    setLoading(true);
    try {
      const config = await adminService.getSystemConfig();
      setForm({
        defaultCreditLimit: config.defaultCreditLimit ?? DEFAULTS.defaultCreditLimit,
        defaultPaymentTermDays: config.defaultPaymentTermDays ?? DEFAULTS.defaultPaymentTermDays,
        creditHoldOverdueDays: config.creditHoldOverdueDays ?? DEFAULTS.creditHoldOverdueDays,
        creditUnlockBufferPct: config.creditUnlockBufferPct ?? DEFAULTS.creditUnlockBufferPct,
        monthlyClosingDay: config.monthlyClosingDay ?? DEFAULTS.monthlyClosingDay,
        minInventoryWarningThreshold: config.minInventoryWarningThreshold ?? DEFAULTS.minInventoryWarningThreshold
      });
      setErrors({});
    } catch (err) {
      console.error('Failed to load system config:', err);
      addToast('Không thể tải cấu hình hệ thống', 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => { loadConfig(); }, [loadConfig]);

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: undefined }));
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const errs = validate(form);
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      addToast('Vui lòng kiểm tra lại các giá trị cấu hình', 'error');
      return;
    }

    setSaveLoading(true);
    try {
      await adminService.updateSystemConfig({
        defaultCreditLimit: Number(form.defaultCreditLimit),
        defaultPaymentTermDays: Number(form.defaultPaymentTermDays),
        creditHoldOverdueDays: Number(form.creditHoldOverdueDays),
        creditUnlockBufferPct: Number(form.creditUnlockBufferPct),
        monthlyClosingDay: Number(form.monthlyClosingDay),
        minInventoryWarningThreshold: Number(form.minInventoryWarningThreshold)
      });
      addToast('Cập nhật cấu hình hệ thống thành công', 'success');
      setErrors({});
    } catch (err) {
      console.error('Failed to save system config:', err);
      addToast('Không thể lưu cấu hình hệ thống', 'error');
    } finally {
      setSaveLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center py-20 text-shade-50">
        <svg className="animate-spin h-6 w-6 text-ink mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
        </svg>
        <span>Đang tải cấu hình hệ thống...</span>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col gap-6 pb-12 max-w-4xl">
      <div>
        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Hệ thống / Admin
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Cấu hình Tham số Hệ thống
        </h1>
        <p className="text-xs text-shade-50 font-light mt-1">
          Thiết lập hạn mức tín dụng, thời hạn công nợ, chu kỳ khóa sổ và ngưỡng cảnh báo tồn kho toàn hệ thống.
        </p>
      </div>

      <form onSubmit={handleSave} className="flex flex-col gap-6">
        <Section icon={CreditCard} title="Kiểm soát tín dụng & Công nợ">
          <div className="flex flex-col gap-1">
            <Input
              id="defaultCreditLimit"
              label="Hạn mức nợ Đại lý mặc định (VND)"
              type="number"
              value={form.defaultCreditLimit}
              onChange={handleChange('defaultCreditLimit')}
              error={errors.defaultCreditLimit}
              placeholder="Ví dụ: 500000000"
              required
            />
            <Hint>Giá trị hiện tại: {Number(form.defaultCreditLimit).toLocaleString()} VND</Hint>
          </div>

          <div className="flex flex-col gap-1">
            <Input
              id="defaultPaymentTermDays"
              label="Thời hạn thanh toán mặc định (Ngày)"
              type="number"
              value={form.defaultPaymentTermDays}
              onChange={handleChange('defaultPaymentTermDays')}
              error={errors.defaultPaymentTermDays}
              placeholder="Ví dụ: 30"
              required
            />
            <Hint>Số ngày tối đa Đại lý được phép thanh toán sau khi xuất hàng.</Hint>
          </div>

          <div className="flex flex-col gap-1">
            <Input
              id="creditHoldOverdueDays"
              label="Số ngày quá hạn trước khi khóa tín dụng (Ngày)"
              type="number"
              value={form.creditHoldOverdueDays}
              onChange={handleChange('creditHoldOverdueDays')}
              error={errors.creditHoldOverdueDays}
              placeholder="Ví dụ: 30"
              required
            />
            <Hint>Sau số ngày này kể từ deadline thanh toán, tài khoản Đại lý sẽ bị tạm khóa.</Hint>
          </div>

          <div className="flex flex-col gap-1">
            <Input
              id="creditUnlockBufferPct"
              label="Hệ số đệm mở khóa tín dụng (0 – 1)"
              type="number"
              step="0.01"
              value={form.creditUnlockBufferPct}
              onChange={handleChange('creditUnlockBufferPct')}
              error={errors.creditUnlockBufferPct}
              placeholder="Ví dụ: 0.8"
              required
            />
            <Hint>Đại lý phải thanh toán ít nhất {Math.round(Number(form.creditUnlockBufferPct) * 100)}% dư nợ để mở khóa. Giá trị từ 0 đến 1.</Hint>
          </div>
        </Section>

        <Section icon={CalendarDays} title="Chu kỳ khóa sổ kế toán">
          <div className="flex flex-col gap-1">
            <Input
              id="monthlyClosingDay"
              label="Ngày khóa sổ kế toán hàng tháng"
              type="number"
              value={form.monthlyClosingDay}
              onChange={handleChange('monthlyClosingDay')}
              error={errors.monthlyClosingDay}
              placeholder="Ví dụ: 5"
              required
            />
            <Hint>Ngày trong tháng (1–31) để hệ thống tự động chốt sổ kế toán.</Hint>
          </div>
        </Section>

        <Section icon={BarChart3} title="Cảnh báo tồn kho">
          <div className="flex flex-col gap-1">
            <Input
              id="minInventoryWarningThreshold"
              label="Ngưỡng cảnh báo tồn kho tối thiểu (Sản phẩm)"
              type="number"
              value={form.minInventoryWarningThreshold}
              onChange={handleChange('minInventoryWarningThreshold')}
              error={errors.minInventoryWarningThreshold}
              placeholder="Ví dụ: 10"
              required
            />
            <Hint>Khi số lượng tồn kho xuống dưới mức này, hệ thống sẽ phát cảnh báo. Cho phép = 0 (không cảnh báo).</Hint>
          </div>
        </Section>

        <div className="flex justify-end gap-3 pt-2">
          <Button
            type="button"
            variant="outline-light"
            onClick={loadConfig}
            disabled={saveLoading}
          >
            Hủy thay đổi
          </Button>
          <Button
            type="submit"
            variant="primary"
            icon={Save}
            loading={saveLoading}
          >
            Lưu cấu hình
          </Button>
        </div>
      </form>
    </div>
  );
};

export default SystemConfig;
