import React from 'react';
import { AlertCircle, CheckCircle2, AlertTriangle } from 'lucide-react';

const CreditCheckBanner = ({ status, remainingCredit }) => {
  if (!status) return null;

  const config = {
    OK: {
      bg: 'bg-success-50',
      border: 'border-success-200',
      text: 'text-success-800',
      icon: CheckCircle2,
      iconColor: 'text-success-500',
      message: `Công nợ hợp lệ. Hạn mức còn: ${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(remainingCredit || 0)}`
    },
    WARNING: {
      bg: 'bg-warning-50',
      border: 'border-warning-200',
      text: 'text-warning-800',
      icon: AlertTriangle,
      iconColor: 'text-warning-500',
      message: 'Cảnh báo: Đại lý sắp chạm hạn mức công nợ.'
    },
    BLOCKED: {
      bg: 'bg-danger-50',
      border: 'border-danger-200',
      text: 'text-danger-800',
      icon: AlertCircle,
      iconColor: 'text-danger-500',
      message: 'Đại lý đang bị khóa công nợ. Không thể tạo đơn xuất hàng.'
    }
  };

  const currentConfig = config[status];
  if (!currentConfig) return null;

  const Icon = currentConfig.icon;

  return (
    <div className={`flex items-start gap-3 p-3 rounded border ${currentConfig.bg} ${currentConfig.border}`}>
      <Icon className={`w-5 h-5 flex-shrink-0 mt-0.5 ${currentConfig.iconColor}`} />
      <p className={`text-sm font-medium ${currentConfig.text}`}>
        {currentConfig.message}
      </p>
    </div>
  );
};

export default CreditCheckBanner;
