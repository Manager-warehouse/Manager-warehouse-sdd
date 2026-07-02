import React, { useState, useEffect } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { authService } from '../../services/auth.service';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import { WAREHOUSES } from '../../utils/constants';
import { getAvatarFallback, formatDate } from '../../utils/format';
import { User, KeyRound, Warehouse, ShieldAlert } from 'lucide-react';

const Profile = () => {
  const { user, login } = useAuthStore();
  const { addToast } = useUiStore();

  // Profile Form States
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [email, setEmail] = useState(user?.email || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [jobTitle, setJobTitle] = useState(user?.jobTitle || '');
  const [shift, setShift] = useState(user?.shift || '');
  const [region, setRegion] = useState(user?.region || '');
  const [profileLoading, setProfileLoading] = useState(false);

  useEffect(() => {
    if (user) {
      setFullName(user.fullName || '');
      setEmail(user.email || '');
      setPhone(user.phone || '');
      setJobTitle(user.jobTitle || '');
      setShift(user.shift || '');
      setRegion(user.region || '');
    }
  }, [user]);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const fullUser = await authService.getMe();
        if (fullUser) {
          login(fullUser, sessionStorage.getItem('wms_token'));
        }
      } catch (err) {
        console.error('Failed to fetch full profile:', err);
      }
    };
    fetchProfile();
  }, []);

  // Password Change States
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passLoading, setPassLoading] = useState(false);
  const [passError, setPassError] = useState('');

  // Find user's warehouse details
  const userWarehouses = WAREHOUSES.filter((w) => user?.warehouses?.includes(w.id));

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    if (!fullName) {
      addToast('Họ tên không được để trống', 'error');
      return;
    }

    setProfileLoading(true);
    try {
      const updatedUser = await authService.updateProfile(fullName, email, phone);
      // Refresh user details in store
      login(updatedUser, sessionStorage.getItem('wms_token'));
      addToast('Cập nhật thông tin cá nhân thành công', 'success');
    } catch (err) {
      console.error(err);
      addToast('Cập nhật thông tin thất bại', 'error');
    } finally {
      setProfileLoading(false);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPassError('');

    if (!currentPassword || !newPassword || !confirmPassword) {
      setPassError('Vui lòng điền đầy đủ thông tin mật khẩu');
      return;
    }

    if (newPassword !== confirmPassword) {
      setPassError('Mật khẩu mới và xác nhận mật khẩu không khớp');
      return;
    }

    // Password strength check (Spec 001)
    if (newPassword.length < 8) {
      setPassError('Mật khẩu mới phải có độ dài tối thiểu 8 ký tự');
      return;
    }

    // Event-driven validation rule: mixed case + digits
    const hasLetter = /[a-zA-Z]/.test(newPassword);
    const hasDigit = /[0-9]/.test(newPassword);
    if (!hasLetter || !hasDigit) {
      setPassError('Mật khẩu phải chứa cả chữ cái và chữ số');
      return;
    }

    setPassLoading(true);
    try {
      await authService.changePassword(currentPassword, newPassword);
      addToast('Thay đổi mật khẩu thành công', 'success');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      if (err.message === 'WEAK_PASSWORD') {
        setPassError('Mật khẩu không đủ mạnh (tối thiểu 8 ký tự, gồm cả chữ và số)');
      } else {
        setPassError('Thay đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu hiện tại.');
      }
    } finally {
      setPassLoading(false);
    }
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Page Header */}
      <div>
        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Hệ thống / Tài khoản
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Hồ sơ cá nhân
        </h1>
        <p className="text-xs text-shade-50 font-light mt-1">
          Xem thông tin phân quyền và cập nhật hồ sơ cá nhân của bạn.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left: Metadata / RBAC Info */}
        <div className="lg:col-span-1 flex flex-col gap-6">
          <div className="card-premium flex flex-col items-center text-center gap-4">
            <div className="w-20 h-20 rounded-full bg-ink text-onPrimary font-semibold text-2xl flex items-center justify-center shadow-lg">
              {getAvatarFallback(user?.fullName)}
            </div>
            <div>
              <h3 className="font-display font-semibold text-lg">{user?.fullName}</h3>
              <p className="text-xs text-shade-50 font-mono mt-1">
                {user?.code || 'NV-000'} &bull; {user?.email || ''}
              </p>
            </div>

            <div className="w-full border-t border-hairline-light pt-4 mt-2 flex flex-col gap-3 text-left">
              <div>
                <span className="text-[10px] font-bold text-shade-50 uppercase tracking-wider block mb-1">
                  Số điện thoại
                </span>
                <div className="text-sm font-semibold text-ink mb-1">{user?.phone || 'Chưa cập nhật'}</div>
              </div>
              <div>
                <span className="text-[10px] font-bold text-shade-50 uppercase tracking-wider block mb-1">
                  Trạng thái tài khoản
                </span>
                <Badge type={user?.isActive ? 'success' : 'danger'}>
                  {user?.isActive ? 'Đang hoạt động' : 'Bị khóa'}
                </Badge>
              </div>

              <div>
                <span className="text-[10px] font-bold text-shade-50 uppercase tracking-wider block mb-1">
                  Vai trò hệ thống (RBAC)
                </span>
                <div className="flex flex-wrap gap-1 mt-1">
                  {user?.role && (
                    <Badge type="premium">
                      {user.role}
                    </Badge>
                  )}
                </div>
              </div>

              <div>
                <span className="text-[10px] font-bold text-shade-50 uppercase tracking-wider block mb-1">
                  Ngày tham gia
                </span>
                <div className="text-xs font-semibold text-ink font-mono mb-1">
                  {user?.createdAt ? formatDate(user.createdAt) : formatDate(new Date(Date.now() - 3600000 * 24 * 30).toISOString())}
                </div>
              </div>
            </div>
          </div>

          {/* Warehouse scope info card */}
          <div className="card-premium">
            <div className="flex items-center gap-2 border-b border-hairline-light pb-3 mb-4">
              <Warehouse className="w-4 h-4 text-shade-60" />
              <h4 className="text-xs font-bold text-shade-70 uppercase tracking-wider">
                Phạm vi Kho được phân công
              </h4>
            </div>

            {user?.role === 'ADMIN' ? (
              <p className="text-xs text-shade-60 leading-relaxed font-light">
                Tài khoản <strong>ADMIN</strong> có toàn quyền thao tác trên tất cả các kho vật lý trong hệ thống.
              </p>
            ) : userWarehouses.length === 0 ? (
              <p className="text-xs text-shade-60 leading-relaxed font-light">
                Tài khoản chưa được gán kho cụ thể. Liên hệ System Admin để gán kho làm việc.
              </p>
            ) : (
              <div className="flex flex-col gap-3">
                {userWarehouses.map((wh) => (
                  <div key={wh.id} className="p-3 bg-canvas-cream rounded-md border border-hairline-light flex items-center justify-between">
                    <div>
                      <div className="text-xs font-semibold text-ink">{wh.name}</div>
                      <div className="text-[10px] text-shade-50 mt-0.5">{wh.address}</div>
                    </div>
                    <span className="text-[10px] font-bold text-shade-60 border border-shade-30 px-2 py-0.5 rounded-pill bg-canvas-light font-mono">
                      {wh.code}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right: Forms */}
        <div className="lg:col-span-2 flex flex-col gap-8">
          {/* General Information Form */}
          <form onSubmit={handleUpdateProfile} className="card-premium flex flex-col gap-6">
            <div className="flex items-center gap-2 border-b border-hairline-light pb-3">
              <User className="w-4 h-4 text-shade-60" />
              <h4 className="text-xs font-bold text-shade-70 uppercase tracking-wider">
                Thông tin cá nhân
              </h4>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Input
                label="Mã nhân viên"
                type="text"
                value={user?.code || 'NV-000'}
                disabled
              />
              <Input
                label="Họ và tên"
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
              />
              <Input
                label="Địa chỉ Email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <Input
                label="Số điện thoại"
                type="text"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
              <Input
                label="Chức danh công việc"
                type="text"
                value={jobTitle}
                onChange={(e) => setJobTitle(e.target.value)}
                disabled
              />
              <Input
                label="Ca làm việc"
                type="text"
                value={shift}
                onChange={(e) => setShift(e.target.value)}
                disabled
              />
              <Input
                label="Khu vực quản lý / Phụ trách"
                type="text"
                value={region}
                onChange={(e) => setRegion(e.target.value)}
                disabled
              />
            </div>

            <div className="flex justify-end">
              <Button type="submit" variant="primary" loading={profileLoading}>
                Lưu thông tin
              </Button>
            </div>
          </form>

          {/* Change Password Form */}
          <form onSubmit={handleChangePassword} className="card-premium flex flex-col gap-6">
            <div className="flex items-center gap-2 border-b border-hairline-light pb-3">
              <KeyRound className="w-4 h-4 text-shade-60" />
              <h4 className="text-xs font-bold text-shade-70 uppercase tracking-wider">
                Thay đổi mật khẩu
              </h4>
            </div>

            {passError && (
              <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-xs font-medium">
                {passError}
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Input
                label="Mật khẩu hiện tại"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
              />
              <Input
                label="Mật khẩu mới"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
              <Input
                label="Xác nhận mật khẩu mới"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </div>

            <div className="flex justify-between items-center gap-4">
              <div className="flex items-center gap-1.5 text-shade-50">
                <ShieldAlert className="w-4 h-4 flex-shrink-0" />
                <span className="text-[10px] font-light leading-snug">
                  Mật khẩu tối thiểu 8 ký tự, gồm cả chữ cái và chữ số.
                </span>
              </div>
              <Button type="submit" variant="primary" loading={passLoading}>
                Đổi mật khẩu
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Profile;
