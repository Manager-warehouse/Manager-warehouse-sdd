import React, { useState, useEffect } from 'react';
import { adminService } from '../../services/admin.service';
import { useUiStore } from '../../stores/ui.store';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { Save, Settings } from 'lucide-react';

const SystemConfig = () => {
  const { addToast } = useUiStore();
  const [loading, setLoading] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);

  // Form Fields
  const [defaultCreditLimit, setDefaultCreditLimit] = useState(500000000);
  const [minWarningStock, setMinWarningStock] = useState(10);
  const [shiftDurationHours, setShiftDurationHours] = useState(8);
  const [monthlyClosingDay, setMonthlyClosingDay] = useState(5);
  const [managerApprovalLimit, setManagerApprovalLimit] = useState(50000000);

  // Field Errors
  const [errors, setErrors] = useState({});

  // Load configuration
  const loadConfig = async () => {
    setLoading(true);
    try {
      const config = await adminService.getSystemConfig();
      setDefaultCreditLimit(config.defaultCreditLimit);
      setMinWarningStock(config.minWarningStock);
      setShiftDurationHours(config.shiftDurationHours);
      setMonthlyClosingDay(config.monthlyClosingDay);
      setManagerApprovalLimit(config.managerApprovalLimit);
    } catch (err) {
      console.error(err);
      addToast('Không thể tải cấu hình hệ thống', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadConfig();
  }, []);

  const validate = () => {
    const errs = {};

    // defaultCreditLimit validation
    const credit = Number(defaultCreditLimit);
    if (!Number.isInteger(credit) || credit <= 0) {
      errs.defaultCreditLimit = 'Hạn mức nợ mặc định phải là số nguyên dương';
    } else if (credit > 10000000000) {
      errs.defaultCreditLimit = 'Hạn mức nợ tối đa là 10 tỷ VND';
    }

    // minWarningStock validation
    const stock = Number(minWarningStock);
    if (!Number.isInteger(stock) || stock <= 0) {
      errs.minWarningStock = 'Ngưỡng cảnh báo tồn kho tối thiểu phải là số nguyên dương';
    } else if (stock > 1000) {
      errs.minWarningStock = 'Ngưỡng cảnh báo tồn kho tối đa là 1,000 sản phẩm';
    }

    // shiftDurationHours validation
    const hours = Number(shiftDurationHours);
    if (isNaN(hours) || hours < 4 || hours > 24) {
      errs.shiftDurationHours = 'Thời gian ca làm việc phải từ 4 đến 24 giờ';
    }

    // monthlyClosingDay validation
    const day = Number(monthlyClosingDay);
    if (!Number.isInteger(day) || day < 1 || day > 28) {
      errs.monthlyClosingDay = 'Ngày khóa sổ kế toán phải nằm trong khoảng từ ngày 1 đến ngày 28';
    }

    // managerApprovalLimit validation
    const approval = Number(managerApprovalLimit);
    if (!Number.isInteger(approval) || approval <= 0) {
      errs.managerApprovalLimit = 'Hạn mức phê duyệt tối đa của Trưởng kho phải là số nguyên dương';
    } else if (approval > 5000000000) {
      errs.managerApprovalLimit = 'Hạn mức phê duyệt tối đa của Trưởng kho là 5 tỷ VND';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!validate()) {
      addToast('Vui lòng kiểm tra lại các giá trị cấu hình', 'error');
      return;
    }

    setSaveLoading(true);
    try {
      await adminService.updateSystemConfig({
        defaultCreditLimit: Number(defaultCreditLimit),
        minWarningStock: Number(minWarningStock),
        shiftDurationHours: Number(shiftDurationHours),
        monthlyClosingDay: Number(monthlyClosingDay),
        managerApprovalLimit: Number(managerApprovalLimit)
      });
      addToast('Cập nhật cấu hình hệ thống thành công', 'success');
      setErrors({});
    } catch (err) {
      console.error(err);
      addToast('Không thể lưu cấu hình hệ thống', 'error');
    } finally {
      setSaveLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center py-20 text-shade-50">
        <svg className="animate-spin h-6 w-6 text-ink mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <span>Đang tải cấu hình hệ thống...</span>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col gap-6 pb-12 max-w-4xl">
      {/* Header */}
      <div>
        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Hệ thống / Admin
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Cấu hình Tham số Hệ thống
        </h1>
        <p className="text-xs text-shade-50 font-light mt-1">
          Thiết lập các định mức phê duyệt, quy chuẩn hoạt động và lịch khóa sổ kế toán tự động toàn hệ thống.
        </p>
      </div>

      {/* Main card */}
      <form onSubmit={handleSave} className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 p-6 md:p-8 flex flex-col gap-6">
        <div className="flex items-center gap-3 pb-4 border-b border-hairline-light">
          <Settings className="w-5 h-5 text-shade-60" />
          <h2 className="text-base font-semibold uppercase tracking-wider text-ink">Các tham số toàn cục</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* defaultCreditLimit */}
          <div className="flex flex-col gap-1">
            <Input
              label="Hạn mức nợ Đại lý mặc định (VND)"
              type="number"
              value={defaultCreditLimit}
              onChange={(e) => setDefaultCreditLimit(e.target.value)}
              error={errors.defaultCreditLimit}
              placeholder="Ví dụ: 500000000"
              required
            />
            <span className="text-[11px] text-shade-40 italic">
              Giá trị hiện tại: {Number(defaultCreditLimit).toLocaleString()} VND (Tối đa 10 tỷ)
            </span>
          </div>

          {/* managerApprovalLimit */}
          <div className="flex flex-col gap-1">
            <Input
              label="Hạn mức phê duyệt tối đa của Trưởng kho (VND)"
              type="number"
              value={managerApprovalLimit}
              onChange={(e) => setManagerApprovalLimit(e.target.value)}
              error={errors.managerApprovalLimit}
              placeholder="Ví dụ: 50000000"
              required
            />
            <span className="text-[11px] text-shade-40 italic">
              Giá trị hiện tại: {Number(managerApprovalLimit).toLocaleString()} VND (Tối đa 5 tỷ)
            </span>
          </div>

          {/* minWarningStock */}
          <Input
            label="Ngưỡng cảnh báo tồn kho tối thiểu (Sản phẩm)"
            type="number"
            value={minWarningStock}
            onChange={(e) => setMinWarningStock(e.target.value)}
            error={errors.minWarningStock}
            placeholder="Ví dụ: 10"
            required
          />

          {/* shiftDurationHours */}
          <Input
            label="Thời gian ca làm việc tiêu chuẩn (Giờ)"
            type="number"
            value={shiftDurationHours}
            onChange={(e) => setShiftDurationHours(e.target.value)}
            error={errors.shiftDurationHours}
            placeholder="Ví dụ: 8"
            required
          />

          {/* monthlyClosingDay */}
          <div className="flex flex-col gap-1">
            <Input
              label="Ngày khóa sổ kế toán hàng tháng"
              type="number"
              value={monthlyClosingDay}
              onChange={(e) => setMonthlyClosingDay(e.target.value)}
              error={errors.monthlyClosingDay}
              placeholder="Ví dụ: 5"
              required
            />
            <span className="text-[11px] text-shade-40">
              Giới hạn từ ngày 1 đến ngày 28 hàng tháng để tránh sai sót năm nhuận.
            </span>
          </div>
        </div>

        {/* Submit Actions */}
        <div className="flex justify-end gap-3 mt-4 pt-6 border-t border-hairline-light">
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
