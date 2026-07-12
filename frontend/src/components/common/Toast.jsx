import React from 'react';
import { CheckCircle2, AlertTriangle, AlertCircle, Info, X } from 'lucide-react';
import { useUiStore } from '../../stores/ui.store';

const ToastItem = ({ id, message, type }) => {
  const removeToast = useUiStore((state) => state.removeToast);

  const configs = {
    success: {
      bg: 'bg-canvas-light border-success-200',
      text: 'text-ink',
      icon: <CheckCircle2 className="w-5 h-5 text-success-600 flex-shrink-0" />
    },
    error: {
      bg: 'bg-canvas-light border-danger-200',
      text: 'text-ink',
      icon: <AlertCircle className="w-5 h-5 text-danger-500 flex-shrink-0" />
    },
    warning: {
      bg: 'bg-canvas-light border-warning-200',
      text: 'text-ink',
      icon: <AlertTriangle className="w-5 h-5 text-warning-500 flex-shrink-0" />
    },
    info: {
      bg: 'bg-canvas-light border-info-200',
      text: 'text-ink',
      icon: <Info className="w-5 h-5 text-info-500 flex-shrink-0" />
    }
  };

  const config = configs[type] || configs.info;

  return (
    <div
      className={`flex items-start gap-3 p-4 rounded-lg border shadow-level-4 max-w-sm w-80 animate-slide-in ${config.bg} ${config.text}`}
      role="alert"
    >
      {config.icon}
      <div className="flex-1 text-sm font-medium">
        {message}
      </div>
      <button
        onClick={() => removeToast(id)}
        className="text-shade-40 hover:text-ink transition-colors flex-shrink-0"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};

export const ToastContainer = () => {
  const toasts = useUiStore((state) => state.toasts);

  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-5 right-5 z-[9999] flex flex-col gap-3">
      {toasts.map((toast) => (
        <ToastItem key={toast.id} {...toast} />
      ))}
    </div>
  );
};

export default ToastItem;
