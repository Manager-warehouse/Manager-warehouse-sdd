import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { authService } from '../../services/auth.service';
import { ROLES } from '../../utils/constants';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import { Shield, User } from 'lucide-react';

const Login = () => {
  const navigate = useNavigate();
  const loginStore = useAuthStore((state) => state.login);
  const { addToast } = useUiStore();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Vui lòng nhập đầy đủ email và mật khẩu');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const data = await authService.login(email, password);
      loginStore(data.user, data.accessToken, data.refreshToken);
      addToast('Đăng nhập thành công', 'success');
      navigate('/admin/users');
    } catch (err) {
      const message = err.message || '';
      if (message.includes('INVALID_CREDENTIALS')) {
        setError('Email hoặc mật khẩu không chính xác');
        addToast('Đăng nhập thất bại', 'error');
      } else if (message.includes('USER_INACTIVE')) {
        setError('Tài khoản này đã bị khóa. Vui lòng liên hệ Admin.');
        addToast('Tài khoản bị khóa', 'error');
      } else {
        setError('Đã có lỗi hệ thống xảy ra. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-login-shell app-safe-public flex w-full max-w-[100vw] flex-col overflow-x-hidden bg-canvas-night font-sans text-onPrimary md:flex-row">
      {/* Left panel: Branding / Cinematic */}
      <div className="auth-login-brand relative flex min-w-0 flex-1 flex-col justify-between overflow-hidden border-b border-hairline-dark bg-gradient-to-br from-canvas-nightElevated via-canvas-night to-shade-70 md:border-b-0 md:border-r">
        <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#c1fbd4_1px,transparent_1px)] [background-size:16px_16px]" />

        {/* Eyebrow / Logo */}
        <div className="relative z-10 flex min-w-0 items-center gap-3">
          <div className="p-2 bg-onPrimary rounded-lg text-canvas-night">
            <Shield className="w-6 h-6 stroke-[2.5]" />
          </div>
          <span className="min-w-0 truncate font-display text-xl font-semibold uppercase tracking-tight">
            Phúc Anh WMS
          </span>
        </div>

        {/* Cinematic Text */}
        <div className="relative z-10 my-auto min-w-0 py-8 md:py-0">
          <span className="text-[12px] font-semibold text-aloe-10 uppercase tracking-widest block mb-4">
            Hệ thống Quản lý Kho vận nội bộ
          </span>
          <h1 className="max-w-xl break-words font-display text-4xl font-light leading-tight tracking-tight text-onPrimary md:text-6xl">
            Kiểm soát chính xác.
            <br />
            <span className="text-shade-40">Vận hành tối ưu.</span>
          </h1>
          <p className="mt-4 max-w-md text-sm font-light leading-relaxed text-shade-40 md:mt-6 md:text-base">
            Hệ thống quản lý tồn kho tích hợp nghiệp vụ nhập, xuất, điều chuyển, kiểm định chất lượng (QC) và công nợ đại lý của Phúc Anh.
          </p>
        </div>

        {/* Version info */}
        <div className="relative z-10 max-w-full truncate text-xs text-shade-50">
          &copy; {new Date().getFullYear()} Phúc Anh Computer. Phiên bản 1.0.0 (Sprint 1)
        </div>
      </div>

      {/* Right panel: Login Form */}
      <div className="auth-login-form flex min-w-0 flex-1 flex-col items-center justify-center bg-canvas-night p-8 md:p-16">
        <div className="flex w-full min-w-0 max-w-md flex-col gap-6 md:gap-8">
          <div>
            <h2 className="text-2xl font-display font-semibold tracking-tight">
              Đăng nhập hệ thống
            </h2>
            <p className="text-sm text-shade-40 mt-2 font-light">
              Nhập địa chỉ email được phân quyền bởi System Admin để tiếp tục.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4 md:gap-5">
            {error && (
              <div className="p-4 bg-danger-950/40 border border-danger-800 rounded-lg text-danger-400 text-xs font-medium">
                {error}
              </div>
            )}

            <div className="relative">
              <Input
                label="Địa chỉ Email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Ví dụ: admin@phucanh.vn"
                className="!bg-canvas-nightElevated !text-onPrimary !border-hairline-dark focus:!ring-onPrimary focus:!border-onPrimary placeholder-shade-60"
                required
              />
              <User className="absolute right-3 top-[38px] w-4 h-4 text-shade-50" />
            </div>

             <div>
              <Input
                label="Mật khẩu"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="!bg-canvas-nightElevated !text-onPrimary !border-hairline-dark focus:!ring-onPrimary focus:!border-onPrimary placeholder-shade-60"
                required
              />
            </div>

            <div className="flex justify-end -mt-3 mb-1">
              <Link to="/forgot-password" className="text-[11px] font-semibold text-shade-50 hover:text-onPrimary transition-colors">
                Quên mật khẩu?
              </Link>
            </div>

            <Button
              type="submit"
              variant="outline-dark"
              loading={loading}
              className="w-full mt-2 min-h-[44px]"
            >
              Tiếp tục
            </Button>
          </form>

          {/* Quick instructions / Help */}
          <div className="auth-login-trial p-4 bg-canvas-nightElevated border border-hairline-dark rounded-lg">
            <span className="text-[10px] font-bold text-aloe-10 uppercase tracking-wider block mb-2">
              Tài khoản dùng thử
            </span>
            <div className="mb-3 rounded-md border border-hairline-dark bg-canvas-night px-3 py-2 text-[11px] text-shade-40">
              <span className="font-semibold text-onPrimary">Mật khẩu:</span>{' '}
              <span className="font-mono">password123</span>
            </div>
            <div className="grid grid-cols-1 gap-2 text-[11px] text-shade-40 font-mono sm:grid-cols-2">
              {[
                ['Admin', 'admin@phucanh.vn'],
                ['CEO', 'ceo@phucanh.vn'],
                ['Planner', 'planer@gmail.com'],
                ['HP Manager', 'manager.hp@phucanh.vn'],
                ['HN Keeper', 'keeper.hn@phucanh.vn'],
                ['HP QC Staff', 'qc.hp@phucanh.vn'],
                ['Account Manager', 'accountmanager@gmail.com'],
                ['Accountant HP', 'accountantHP@phucanh.vn'],
                ['Acc Manager HP', 'acc_managerHP@phucanh.vn'],
              ].map(([role, mail]) => (
                <div key={role} className="min-w-0 rounded-md border border-hairline-dark bg-canvas-night px-3 py-2">
                  <strong className="block text-onPrimary">{role}</strong>
                  <span className="block truncate">{mail}</span>
                </div>
              ))}
            </div>

            <div className="mt-3 rounded-md border border-hairline-dark bg-canvas-night px-3 py-2 text-[11px] text-shade-40">
              <span className="font-semibold text-onPrimary">Luồng inbound HP:</span>{' '}
              Planner lập lệnh → Warehouse Staff kiểm đếm → Storekeeper QC/cất kệ → Warehouse Manager duyệt.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
