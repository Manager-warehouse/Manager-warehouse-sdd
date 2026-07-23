import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useUiStore } from '../../stores/ui.store';
import { authService } from '../../services/auth.service';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import { Shield, Mail, KeyRound, ArrowLeft, CheckCircle2 } from 'lucide-react';

const ForgotPassword = () => {
  const navigate = useNavigate();
  const { addToast } = useUiStore();

  // step 1: email, step 2: OTP, step 3: new password, step 4: success
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleRequestOtp = async (e) => {
    e.preventDefault();
    if (!email) {
      setError('Vui lòng nhập địa chỉ email của bạn');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await authService.forgotPassword(email);
      addToast('Mã OTP đã được gửi đến email của bạn', 'success');
      setStep(2);
    } catch (err) {
      setError('Tài khoản không tồn tại hoặc đã bị khóa.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    if (!otp || otp.length < 6) {
      setError('Vui lòng nhập đủ mã OTP 6 số');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await authService.checkOtp(email, otp);
      setStep(3);
    } catch (err) {
      const code = err?.response?.data?.code;
      if (code === 'OTP_LOCKED') {
        setError('Bạn đã nhập sai mã OTP quá nhiều lần. Vui lòng yêu cầu gửi lại mã mới.');
      } else if (code === 'OTP_EXPIRED') {
        setError('Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.');
      } else {
        setError('Mã OTP không chính xác. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (!newPassword) {
      setError('Vui lòng nhập mật khẩu mới');
      return;
    }
    if (newPassword.length < 8) {
      setError('Mật khẩu mới phải có tối thiểu 8 ký tự');
      return;
    }
    // Must match backend VerifyOtpRequest.newPassword @Pattern rule
    const hasUpper = /[A-Z]/.test(newPassword);
    const hasLower = /[a-z]/.test(newPassword);
    const hasDigit = /[0-9]/.test(newPassword);
    if (!hasUpper || !hasLower || !hasDigit) {
      setError('Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 chữ số');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await authService.verifyOtp(email, otp, newPassword);
      addToast('Đặt lại mật khẩu thành công', 'success');
      setStep(4);
    } catch (err) {
      if (err.message === 'INVALID_OTP') {
        // OTP was wrong — send user back to re-enter it
        setOtp('');
        setStep(2);
        setError('Mã OTP không chính xác hoặc đã hết hạn. Vui lòng nhập lại.');
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
      <div className="flex-1 flex flex-col justify-between p-8 md:p-16 bg-gradient-to-br from-canvas-nightElevated via-canvas-night to-shade-70 relative overflow-hidden border-b md:border-b-0 md:border-r border-hairline-dark hidden md:flex">
        <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#c1fbd4_1px,transparent_1px)] [background-size:16px_16px]" />

        {/* Eyebrow / Logo */}
        <div className="relative flex items-center gap-3 z-10">
          <div className="p-2 bg-onPrimary rounded-lg text-canvas-night">
            <Shield className="w-6 h-6 stroke-[2.5]" />
          </div>
          <span className="font-display font-bold text-xl tracking-tight uppercase">
            Quản Lý Kho
          </span>
        </div>

        {/* Cinematic Text */}
        <div className="relative my-auto py-12 md:py-0 z-10">
          <span className="text-[12px] font-semibold text-aloe-10 uppercase tracking-widest block mb-4">
            Hệ thống Quản lý Kho vận nội bộ
          </span>
          <h1 className="text-4xl md:text-6xl font-display font-light leading-tight tracking-tight max-w-xl text-onPrimary">
            Bảo mật tối đa.
            <br />
            <span className="text-shade-40">Khôi phục dễ dàng.</span>
          </h1>
          <p className="text-sm md:text-base text-shade-40 font-light mt-6 max-w-md leading-relaxed">
            Sử dụng email đã đăng ký để khôi phục quyền truy cập vào hệ thống an toàn thông qua mã xác thực một lần (OTP).
          </p>
        </div>

        {/* Version info */}
        <div className="relative text-xs text-shade-50 z-10">
          &copy; {new Date().getFullYear()} Quản Lý Kho. Phiên bản 1.0.0 (Sprint 1)
        </div>
      </div>

      {/* Right panel: Form */}
      <div className="flex-1 flex flex-col justify-center items-center p-8 md:p-16 bg-canvas-night relative">
        <Link to="/login" className="absolute top-8 left-8 md:top-16 md:left-16 flex items-center gap-2 text-xs font-semibold text-shade-50 hover:text-onPrimary transition-colors uppercase tracking-wider">
          <ArrowLeft className="w-4 h-4" />
          Quay lại Đăng nhập
        </Link>

        <div className="w-full max-w-md flex flex-col gap-8 mt-12 md:mt-0">
          <div>
            <h2 className="text-2xl font-display font-semibold tracking-tight">
              Khôi phục mật khẩu
            </h2>
            <p className="text-sm text-shade-40 mt-2 font-light">
              {step === 1 && 'Nhập địa chỉ email của bạn để nhận mã xác thực khôi phục mật khẩu.'}
              {step === 2 && 'Kiểm tra email của bạn và nhập mã OTP 6 số.'}
              {step === 3 && 'Mã OTP hợp lệ. Hãy thiết lập mật khẩu mới cho tài khoản của bạn.'}
              {step === 4 && 'Mật khẩu của bạn đã được khôi phục thành công.'}
            </p>
          </div>

          {step === 1 && (
            <form onSubmit={handleRequestOtp} className="flex flex-col gap-5">
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
                  placeholder="Ví dụ: admin@quanlykho.vn"
                  className="!bg-canvas-nightElevated !text-onPrimary !border-hairline-dark focus:!ring-onPrimary focus:!border-onPrimary placeholder-shade-60"
                  required
                />
                <Mail className="absolute right-3 top-[38px] w-4 h-4 text-shade-50" />
              </div>

              <Button
                type="submit"
                variant="outline-dark"
                loading={loading}
                className="w-full mt-2 min-h-[44px]"
              >
                Gửi mã xác thực
              </Button>
            </form>
          )}

          {step === 2 && (
            <form onSubmit={handleVerifyOtp} className="flex flex-col gap-5">
              {error && (
                <div className="p-4 bg-danger-950/40 border border-danger-800 rounded-lg text-danger-400 text-xs font-medium">
                  {error}
                </div>
              )}

              <div className="relative">
                <Input
                  label="Mã xác thực (OTP)"
                  type="text"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  placeholder="Nhập mã 6 số"
                  className="!bg-canvas-nightElevated !text-onPrimary !border-hairline-dark focus:!ring-onPrimary focus:!border-onPrimary placeholder-shade-60 tracking-[0.5em] text-center font-mono text-lg"
                  required
                />
              </div>

              <Button
                type="submit"
                variant="outline-dark"
                loading={loading}
                className="w-full mt-2 min-h-[44px]"
              >
                Xác nhận mã OTP
              </Button>

              <div className="text-center mt-2">
                <span className="text-xs text-shade-50">Chưa nhận được mã? </span>
                <button
                  type="button"
                  onClick={handleRequestOtp}
                  disabled={loading}
                  className="text-xs font-semibold text-onPrimary hover:text-aloe-10 transition-colors"
                >
                  Gửi lại mã
                </button>
              </div>
            </form>
          )}

          {step === 3 && (
            <form onSubmit={handleResetPassword} className="flex flex-col gap-5">
              {error && (
                <div className="p-4 bg-danger-950/40 border border-danger-800 rounded-lg text-danger-400 text-xs font-medium">
                  {error}
                </div>
              )}

              <div>
                <Input
                  label="Mật khẩu mới"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Tối thiểu 8 ký tự"
                  className="!bg-canvas-nightElevated !text-onPrimary !border-hairline-dark focus:!ring-onPrimary focus:!border-onPrimary placeholder-shade-60"
                  required
                />
              </div>

              <div>
                <Input
                  label="Xác nhận mật khẩu mới"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Nhập lại mật khẩu mới"
                  className="!bg-canvas-nightElevated !text-onPrimary !border-hairline-dark focus:!ring-onPrimary focus:!border-onPrimary placeholder-shade-60"
                  required
                />
              </div>

              <Button
                type="submit"
                variant="outline-dark"
                loading={loading}
                className="w-full mt-2 min-h-[44px]"
              >
                Cập nhật mật khẩu
              </Button>
            </form>
          )}

          {step === 4 && (
            <div className="flex flex-col items-center justify-center py-8 gap-6">
              <div className="w-16 h-16 rounded-full bg-aloe-10/20 flex items-center justify-center text-aloe-10">
                <CheckCircle2 className="w-8 h-8" />
              </div>
              <p className="text-center text-sm text-shade-40">
                Tài khoản của bạn đã được bảo vệ với mật khẩu mới. Vui lòng đăng nhập lại để tiếp tục sử dụng hệ thống.
              </p>
              <Button
                onClick={() => navigate('/login')}
                variant="outline-dark"
                className="w-full mt-4 min-h-[44px]"
              >
                Chuyển đến Đăng nhập
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;
