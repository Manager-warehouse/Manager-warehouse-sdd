import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { ROLES, ROLE_LABELS, WAREHOUSES } from '../../utils/constants';
import { masterDataService } from '../../services/masterData.service';

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
  const [selectedWarehouse, setSelectedWarehouse] = useState('1');
  const [error, setError] = useState('');
  const [warehousesList, setWarehousesList] = useState(WAREHOUSES);

  useEffect(() => {
    if (isOpen) {
      masterDataService.getWarehouses()
        .then((data) => {
          if (Array.isArray(data) && data.length > 0) {
            setWarehousesList(data);
          }
        })
        .catch(() => {});
    }
  }, [isOpen]);

  const physicalWarehouses = warehousesList.filter(
    (w) => w.type !== 'IN_TRANSIT' && w.code !== 'IN_TRANSIT' && w.is_active !== false
  );

  // Check if current role doesn't need warehouse selection (Only ADMIN and CEO access all warehouses)
  const noWarehouseSelection = selectedRole === ROLES.ADMIN || selectedRole === ROLES.CEO;

  // Reset or fill form when user or isOpen changes
  useEffect(() => {
    if (isOpen) {
      setError('');
      const defaultWhId = physicalWarehouses.length > 0 ? String(physicalWarehouses[0].id) : '1';
      if (user) {
        setCode(user.code || '');
        setFullName(user.fullName || '');
        setEmail(user.email || '');
        setPhone(user.phone || '');
        setPassword('');
        setSelectedRole(user.role || ROLES.WAREHOUSE_STAFF);
        setShift(user.shift || 'Ca sáng');
        setRegion(user.region || '');
        const firstWh = Array.isArray(user.warehouses) && user.warehouses.length > 0 ? String(user.warehouses[0]) : defaultWhId;
        setSelectedWarehouse(firstWh);
      } else {
        setCode('');
        setFullName('');
        setEmail('');
        setPhone('');
        setPassword('');
        setSelectedRole(ROLES.WAREHOUSE_STAFF);
        setShift('Ca sáng');
        setRegion('');
        setSelectedWarehouse(defaultWhId);
      }
    }
  }, [isOpen, user, physicalWarehouses.length]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!code || !fullName || !email) {
      setError('Vui lòng điền mã nhân viên, họ tên và email');
      return;
    }

    if (!selectedRole) {
      setError('Vui lòng gán vai trò');
      return;
    }

    if (!noWarehouseSelection && !selectedWarehouse) {
      setError('Vui lòng chọn Kho làm việc');
      return;
    }

    const getRegionFromWarehouse = (warehouseId) => {
      const id = Number(warehouseId);
      if (id === 1) return 'Hải Phòng';
      if (id === 2) return 'Hà Nội';
      if (id === 3) return 'Hồ Chí Minh';
      return 'Toàn quốc';
    };

    const finalRegion = selectedRole === ROLES.DISPATCHER
      ? region
      : (!noWarehouseSelection ? getRegionFromWarehouse(selectedWarehouse) : 'Toàn quốc');

    const payload = {
      code,
      fullName,
      email,
      phone,
      role: selectedRole,
      jobTitle: ROLE_LABELS[selectedRole],
      shift,
      region: finalRegion,
      warehouses: !noWarehouseSelection ? [Number(selectedWarehouse)] : []
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

  const warehouseOptions = physicalWarehouses.map((w) => ({
    value: String(w.id),
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
          <div className="p-3 bg-danger-50 border border-danger-200 text-danger-700 rounded-lg text-xs font-medium">
            {error}
          </div>
        )}

        <Input
          label="Mã nhân viên"
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
            placeholder="nhanvien@quanlykho.vn"
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
          label="Gán vai trò"
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

        {/* Single Warehouse Select dropdown (only for roles needing warehouse assignment) */}
        {!noWarehouseSelection && (
          <Input
            label="Phân công Kho làm việc"
            type="select"
            options={warehouseOptions}
            value={selectedWarehouse}
            onChange={(e) => setSelectedWarehouse(e.target.value)}
            required
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
