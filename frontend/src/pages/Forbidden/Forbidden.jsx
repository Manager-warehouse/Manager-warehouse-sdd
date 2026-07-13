import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldOff, ArrowLeft } from 'lucide-react';
import Button from '../../components/common/Button';

const Forbidden = () => {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col min-h-screen bg-canvas-cream text-ink items-center justify-center px-6">
      <div className="flex flex-col items-center gap-6 max-w-md text-center">
        <div className="flex items-center justify-center w-20 h-20 rounded-full bg-danger-50 border border-danger-200">
          <ShieldOff className="w-9 h-9 text-danger-500" />
        </div>

        <span className="text-[11px] font-bold text-shade-60 uppercase tracking-widest">
          Lỗi 403 - Không có quyền truy cập
        </span>

        <h1 className="text-3xl font-display font-semibold tracking-tight leading-snug">
          Khu vực bị hạn chế
        </h1>

        <p className="text-sm text-shade-50 font-light leading-relaxed">
          Tài khoản của bạn không có quyền truy cập chức năng này.
          Nếu bạn cho rằng đây là nhầm lẫn, vui lòng liên hệ quản trị viên hệ thống.
        </p>

        <div className="flex gap-3 mt-2">
          <Button
            variant="outline-light"
            icon={ArrowLeft}
            onClick={() => navigate(-1)}
          >
            Quay lại
          </Button>
          <Button
            variant="primary"
            onClick={() => navigate('/dashboard', { replace: true })}
          >
            Về trang chủ
          </Button>
        </div>
      </div>
    </div>
  );
};

export default Forbidden;
