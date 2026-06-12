import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { ROLES, ROLE_LABELS, WAREHOUSES } from '../../utils/constants';

const UserFormModal = ({ isOpen, onClose, onSave, user = null, loading = false }) => {
  const modalType = user ? 'edit' : 'create';

  // Form Fields State
  const [code, setCode] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [selectedRole, setSelectedRole] = useState(ROLES.WAREHOUSE_STAFF);
  const [shift, setShift] = useState('Ca sáng');
  const [region, setRegion] = useState('');
  const [selectedWarehouses, setSelectedWarehouses] = useState([]);
  const [error, setError] = useState('');

  // Reset or fill form when user or isOpen changes
  useEffect(() => {
    if (isOpen) {
      setError('');
      if (user) {
        setCode(user.code || '');
        setFullName(user.fullName || '');
        setEmail(user.email || '');
        setPhone(user.phone || '');
        setPassword('');
        setSelectedRole(user.role || ROLES.WAREHOUSE_STAFF);
        setShift(user.shift || 'Ca sáng');
        setRegion(user.region || '');
        setSelectedWarehouses(user.warehouses || []);
      } else {
        setCode('');
        setFullName('');
        setEmail('');
        setPhone('');
        setPassword('');
        setSelectedRole(ROLES.WAREHOUSE_STAFF);
        setShift('Ca sáng');
        setRegion('');
        setSelectedWarehouses([]);
      }
    }
  }, [isOpen, user]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!code || !fullName || !email) {
      setError('Vui lòng điền mã nhân viên, họ tên và email');
      return;
    }

    if (!selectedRole) {
      setError('Vui lòng gán vai trò (Role)');
      return;
    }

    // Role-based warehouse validation: Non-admin/non-CEO roles must have at least one warehouse assigned
    const needsWarehouse = selectedRole !== ROLES.ADMIN && selectedRole !== ROLES.CEO;
    if (needsWarehouse && selectedWarehouses.length === 0) {
      setError('Nhân viên nghiệp vụ phải được phân công ít nhất một Kho làm việc');
      return;
    }

    const getRegionFromWarehouses = (warehouseIds) => {
      if (!warehouseIds || warehouseIds.length === 0) return 'Toàn quốc';
      if (warehouseIds.length > 1) return 'Toàn quốc';
      const id = Number(warehouseIds[0]);
      if (id === 1) return 'Hải Phòng';
      if (id === 2) return 'Hà Nội';
      if (id === 3) return 'Hồ Chí Minh';
      return 'Toàn quốc';
    };

    const finalRegion = selectedRole === ROLES.DISPATCHER
      ? region
      : (needsWarehouse ? getRegionFromWarehouses(selectedWarehouses) : 'Toàn quốc');

    const payload = {
      code,
      fullName,
      email,
      phone,
      role: selectedRole,
      jobTitle: ROLE_LABELS[selectedRole],
      shift,
      region: finalRegion,
      warehouses: needsWarehouse ? selectedWarehouses : []
    };

    if (modalType === 'create') {
      if (!password) {
        setError('Mật khẩu bắt buộc đối với tài khoản mới');
        return;
      }

      // Password strength validation
      if (password.length < 8) {
        setError('Mật khẩu phải dài từ 8 ký tự trở lên');
        return;
      }

      const hasLetter = /[a-zA-Z]/.test(password);
      const hasDigit = /[0-9]/.test(password);
      if (!hasLetter || !hasDigit) {
        setError('Mật khẩu mới phải chứa cả chữ và số');
        return;
      }

      payload.password = password;
    } else if (modalType === 'edit' && password) {
      // Password strength validation for reset
      if (password.length < 8) {
        setError('Mật khẩu đặt lại phải dài từ 8 ký tự trở lên');
        return;
      }

      const hasLetter = /[a-zA-Z]/.test(password);
      const hasDigit = /[0-9]/.test(password);
      if (!hasLetter || !hasDigit) {
        setError('Mật khẩu đặt lại phải chứa cả chữ và số');
        return;
      }

      payload.password = password;
    }

    try {
      await onSave(payload);
    } catch (err) {
      if (err.message === 'EMAIL_TAKEN') {
        setError('Địa chỉ email này đã tồn tại trên hệ thống');
      } else if (err.message === 'WEAK_PASSWORD') {
        setError('Mật khẩu không đạt yêu cầu bảo mật');
      } else {
        setError('Có lỗi xảy ra khi lưu tài khoản');
      }
    }
  };

  // Options mapping for inputs
  const roleOptions = Object.keys(ROLES).map((key) => ({
    value: ROLES[key],
    label: ROLE_LABELS[ROLES[key]]
  }));

  const warehouseOptions = WAREHOUSES.map((w) => ({
    value: w.id,
    label: `${w.name} (${w.code})`
  }));

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      maxWidth="max-w-lg"
      title={modalType === 'create' ? 'Tạo tài khoản mới' : 'Chỉnh sửa tài khoản'}
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-md text-xs font-medium">
            {error}
          </div>
        )}

        <Input
          label="Mã nhân viên (Employee Code)"
          type="text"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          disabled={modalType === 'edit'}
          placeholder="Ví dụ: NV-005"
          required
        />

        <Input
          label="Họ và tên nhân viên"
          type="text"
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          placeholder="Ví dụ: Nguyễn Văn A"
          required
        />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input
            label="Địa chỉ Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="nhanvien@phucanh.vn"
            required
          />

          <Input
            label="Số điện thoại"
            type="text"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="Ví dụ: 0912345678"
          />
        </div>

        {modalType === 'create' ? (
          <Input
            label="Mật khẩu khởi tạo"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Mật khẩu tối thiểu 8 ký tự, chữ và số"
            required
          />
        ) : (
          <Input
            label="Đặt lại mật khẩu (Để trống nếu không đổi)"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Nhập mật khẩu mới để đặt lại mật khẩu cho tài khoản này"
          />
        )}

        {/* Role selection dropdown */}
        <Input
          label="Gán Vai trò (Role)"
          type="select"
          options={roleOptions}
          value={selectedRole}
          onChange={(e) => setSelectedRole(e.target.value)}
        />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input
            label="Ca làm việc"
            type="select"
            options={[
              { value: 'Ca sáng', label: 'Ca sáng' },
              { value: 'Ca chiều', label: 'Ca chiều' },
              { value: 'Cả ngày', label: 'Cả ngày' }
            ]}
            value={shift}
            onChange={(e) => setShift(e.target.value)}
          />
          {selectedRole === ROLES.DISPATCHER && (
            <Input
              label="Khu vực phụ trách"
              type="text"
              value={region}
              onChange={(e) => setRegion(e.target.value)}
              placeholder="Hải Phòng / Hà Nội..."
            />
          )}
        </div>

        {/* Warehouses Checkbox group (only relevant if not Admin/CEO) */}
        {selectedRole !== ROLES.ADMIN && selectedRole !== ROLES.CEO && (
          <Input
            label="Phân công Kho làm việc"
            type="checkbox-group"
            options={warehouseOptions}
            value={selectedWarehouses}
            onChange={setSelectedWarehouses}
          />
        )}

        <div className="flex justify-end gap-3 mt-4 pt-4 border-t border-hairline-light">
          <Button
            type="button"
            onClick={onClose}
            variant="outline-light"
          >
            Hủy
          </Button>
          <Button
            type="submit"
            variant="primary"
            loading={loading}
          >
            Lưu lại
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default UserFormModal;
