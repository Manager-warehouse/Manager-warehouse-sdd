import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { authService } from '../../services/auth.service';
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
      navigate('/dashboard');
    } catch (err) {
      console.error(err);
      if (err.message === 'INVALID_CREDENTIALS') {
        setError('Email hoặc mật khẩu không chính xác');
        addToast('Đăng nhập thất bại', 'error');
      } else if (err.message === 'USER_INACTIVE') {
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
    <div className="min-h-screen bg-canvas-night text-onPrimary flex flex-col md:flex-row font-sans">
      {/* Left panel: Branding / Cinematic */}
      <div className="flex-1 flex flex-col justify-between p-8 md:p-16 bg-gradient-to-br from-canvas-nightElevated via-canvas-night to-shade-70 relative overflow-hidden border-b md:border-b-0 md:border-r border-hairline-dark">
        <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#c1fbd4_1px,transparent_1px)] [background-size:16px_16px]" />

        {/* Eyebrow / Logo */}
        <div className="relative flex items-center gap-3 z-10">
          <div className="p-2 bg-onPrimary rounded-lg text-canvas-night">
            <Shield className="w-6 h-6 stroke-[2.5]" />
          </div>
          <span className="font-display font-bold text-xl tracking-tight uppercase">
            Phúc Anh WMS
          </span>
        </div>

        {/* Cinematic Text */}
        <div className="relative my-auto py-12 md:py-0 z-10">
          <span className="text-[12px] font-semibold text-aloe-10 uppercase tracking-widest block mb-4">
            Hệ thống Quản lý Kho vận nội bộ
          </span>
          <h1 className="text-4xl md:text-6xl font-display font-light leading-tight tracking-tight max-w-xl text-onPrimary">
            Kiểm soát chính xác.
            <br />
            <span className="text-shade-40">Vận hành tối ưu.</span>
          </h1>
          <p className="text-sm md:text-base text-shade-40 font-light mt-6 max-w-md leading-relaxed">
            Hệ thống quản lý tồn kho tích hợp nghiệp vụ nhập, xuất, điều chuyển, kiểm định chất lượng (QC) và công nợ đại lý của Phúc Anh.
          </p>
        </div>

        {/* Version info */}
        <div className="relative text-xs text-shade-50 z-10">
          &copy; {new Date().getFullYear()} Phúc Anh Computer. Phiên bản 1.0.0 (Sprint 1)
        </div>
      </div>

      {/* Right panel: Login Form */}
      <div className="flex-1 flex flex-col justify-center items-center p-8 md:p-16 bg-canvas-night">
        <div className="w-full max-w-md flex flex-col gap-8">
          <div>
            <h2 className="text-2xl font-display font-semibold tracking-tight">
              Đăng nhập hệ thống
            </h2>
            <p className="text-sm text-shade-40 mt-2 font-light">
              Nhập địa chỉ email được phân quyền bởi System Admin để tiếp tục.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-5">
            {error && (
              <div className="p-4 bg-red-950/40 border border-red-800 rounded-lg text-red-400 text-xs font-medium">
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
          <div className="p-4 bg-canvas-nightElevated border border-hairline-dark rounded-lg">
            <span className="text-[10px] font-bold text-aloe-10 uppercase tracking-wider block mb-2">
              Tài khoản dùng thử (Mock Mode)
            </span>
            <div className="grid grid-cols-2 gap-x-4 gap-y-1.5 text-[11px] text-shade-40 font-mono">
              <div>
                <strong>Admin:</strong> admin@phucanh.vn
              </div>
              <div>
                <strong>CEO:</strong> ceo@phucanh.vn
              </div>
              <div>
                <strong>Planner:</strong> planner@phucanh.vn
              </div>
              <div>
                <strong>HP Manager:</strong> manager.hp@phucanh.vn
              </div>
              <div>
                <strong>HN Keeper:</strong> keeper.hn@phucanh.vn
              </div>
              <div>
                <strong>HP QC Staff:</strong> qc.hp@phucanh.vn
              </div>
              <div>
                <strong>Account Manager:</strong> accountmanager@gmail.com
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
