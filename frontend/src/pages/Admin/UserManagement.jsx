import React, { useState, useEffect } from 'react';
import { adminService } from '../../services/admin.service';
import { useUiStore } from '../../stores/ui.store';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Table from '../../components/common/Table';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import { ROLES, ROLE_LABELS, WAREHOUSES } from '../../utils/constants';
import { formatDate } from '../../utils/format';
import { Plus, Search, Edit2, ToggleLeft, ToggleRight, History, Users } from 'lucide-react';

const UserManagement = () => {
  const { addToast } = useUiStore();

  // Tab State
  const [activeTab, setActiveTab] = useState('users'); // users, auditLogs

  // Users Data States
  const [users, setUsers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');

  // Form Modal States
  const [modalOpen, setModalOpen] = useState(false);
  const [modalType, setModalType] = useState('create'); // create, edit
  const [editingUserId, setEditingUserId] = useState(null);
  const [formError, setFormError] = useState('');
  const [formLoading, setFormLoading] = useState(false);

  // Form Fields
  const [code, setCode] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [selectedRole, setSelectedRole] = useState(ROLES.WAREHOUSE_STAFF);
  const [jobTitle, setJobTitle] = useState('');
  const [shift, setShift] = useState('');
  const [region, setRegion] = useState('');
  const [selectedWarehouses, setSelectedWarehouses] = useState([]);

  // Load Data
  const loadData = async () => {
    setLoading(true);
    try {
      const uList = await adminService.getUsers();
      const logs = await adminService.getAuditLogs();
      setUsers(uList);
      setAuditLogs(logs);
    } catch (err) {
      console.error(err);
      addToast('Không thể tải danh sách tài khoản', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Filtered Users
  const filteredUsers = users.filter((u) => {
    const searchLower = search.toLowerCase();
    const matchesSearch =
      u.fullName.toLowerCase().includes(searchLower) ||
      (u.email && u.email.toLowerCase().includes(searchLower)) ||
      (u.code && u.code.toLowerCase().includes(searchLower));
    return matchesSearch;
  });

  const handleOpenCreateModal = () => {
    setModalType('create');
    setEditingUserId(null);
    setCode('');
    setFullName('');
    setEmail('');
    setPhone('');
    setPassword('');
    setSelectedRole(ROLES.WAREHOUSE_STAFF);
    setJobTitle('');
    setShift('');
    setRegion('');
    setSelectedWarehouses([]);
    setFormError('');
    setModalOpen(true);
  };

  const handleOpenEditModal = (user) => {
    setModalType('edit');
    setEditingUserId(user.id);
    setCode(user.code || '');
    setFullName(user.fullName);
    setEmail(user.email || '');
    setPhone(user.phone || '');
    setPassword(''); // Leave empty, only change if entered
    setSelectedRole(user.role);
    setJobTitle(user.jobTitle || '');
    setShift(user.shift || '');
    setRegion(user.region || '');
    setSelectedWarehouses(user.warehouses || []);
    setFormError('');
    setModalOpen(true);
  };

  const handleSaveUser = async (e) => {
    e.preventDefault();
    setFormError('');

    if (!code || !fullName || !email) {
      setFormError('Vui lòng điền mã nhân viên, họ tên và email');
      return;
    }

    if (!selectedRole) {
      setFormError('Vui lòng gán vai trò (Role)');
      return;
    }

    // Role-based warehouse validation: Non-admin/non-CEO roles must have at least one warehouse assigned
    const needsWarehouse = selectedRole !== ROLES.ADMIN && selectedRole !== ROLES.CEO;
    if (needsWarehouse && selectedWarehouses.length === 0) {
      setFormError('Nhân viên nghiệp vụ phải được phân công ít nhất một Kho làm việc');
      return;
    }

    setFormLoading(true);
    try {
      if (modalType === 'create') {
        if (!password) {
          setFormError('Mật khẩu bắt buộc đối với tài khoản mới');
          setFormLoading(false);
          return;
        }

        // Pass strength check
        if (password.length < 8) {
          setFormError('Mật khẩu phải dài từ 8 ký tự trở lên');
          setFormLoading(false);
          return;
        }

        const hasLetter = /[a-zA-Z]/.test(password);
        const hasDigit = /[0-9]/.test(password);
        if (!hasLetter || !hasDigit) {
          setFormError('Mật khẩu mới phải chứa cả chữ và số');
          setFormLoading(false);
          return;
        }

        await adminService.createUser({
          code,
          fullName,
          email,
          phone,
          password,
          role: selectedRole,
          jobTitle,
          shift,
          region,
          warehouses: selectedWarehouses
        });

        addToast('Tạo tài khoản mới thành công', 'success');
      } else {
        await adminService.updateUser(editingUserId, {
          fullName,
          email,
          phone,
          role: selectedRole,
          jobTitle,
          shift,
          region,
          warehouses: selectedWarehouses
        });
        addToast('Cập nhật tài khoản thành công', 'success');
      }
      setModalOpen(false);
      loadData();
    } catch (err) {
      console.error(err);
      if (err.message === 'EMAIL_TAKEN') {
        setFormError('Địa chỉ email này đã tồn tại trên hệ thống');
      } else if (err.message === 'WEAK_PASSWORD') {
        setFormError('Mật khẩu không đạt yêu cầu bảo mật');
      } else {
        setFormError('Có lỗi xảy ra khi lưu tài khoản');
      }
    } finally {
      setFormLoading(false);
    }
  };

  const handleToggleUserStatus = async (user) => {
    const nextStatus = !user.isActive;
    try {
      await adminService.toggleUserStatus(user.id, nextStatus);
      addToast(
        `${nextStatus ? 'Kích hoạt' : 'Khóa'} tài khoản ${user.fullName} thành công`,
        'success'
      );
      loadData();
    } catch (err) {
      console.error(err);
      addToast('Không thể cập nhật trạng thái tài khoản', 'error');
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
    <div className="flex-1 flex flex-col gap-6 pb-12">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Hệ thống / Admin
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Quản trị tài khoản & Phân quyền
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Quản lý tài khoản nhân viên, gán vai trò RBAC và gán kho làm việc được phép truy cập.
          </p>
        </div>

        <div className="flex gap-2">
          <Button
            onClick={handleOpenCreateModal}
            variant="primary"
            icon={Plus}
          >
            Tạo tài khoản
          </Button>
        </div>
      </div>

      {/* Tabs Menu */}
      <div className="flex border-b border-hairline-light">
        <button
          onClick={() => setActiveTab('users')}
          className={`flex items-center gap-2 px-5 py-3 border-b-2 text-xs font-semibold uppercase tracking-wider transition-colors focus:outline-none ${
            activeTab === 'users'
              ? 'border-ink text-ink'
              : 'border-transparent text-shade-50 hover:text-ink'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>Danh sách tài khoản</span>
        </button>
        <button
          onClick={() => setActiveTab('auditLogs')}
          className={`flex items-center gap-2 px-5 py-3 border-b-2 text-xs font-semibold uppercase tracking-wider transition-colors focus:outline-none ${
            activeTab === 'auditLogs'
              ? 'border-ink text-ink'
              : 'border-transparent text-shade-50 hover:text-ink'
          }`}
        >
          <History className="w-4 h-4" />
          <span>Nhật ký hoạt động (Audit Trail)</span>
        </button>
      </div>

      {/* Tab Panels */}
      {activeTab === 'users' ? (
        <div className="flex flex-col gap-4">
          {/* Search bar */}
          <div className="relative w-full max-w-md">
            <Search className="absolute left-3 top-3.5 w-4 h-4 text-shade-50" />
            <input
              type="text"
              placeholder="Tìm theo tài khoản, tên hoặc email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full bg-canvas-light text-sm pl-9 pr-4 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all min-h-[44px]"
            />
          </div>

          {/* User Table */}
          <Table
            headers={['Mã NV / Họ tên', 'Tài khoản / SĐT', 'Vai trò (Role)', 'Kho phụ trách', 'Trạng thái', 'Thao tác']}
            data={filteredUsers}
            loading={loading}
            renderRow={(user, idx) => {
              const assignedWHNames = WAREHOUSES.filter((w) => user.warehouses?.includes(w.id))
                .map((w) => w.code)
                .join(', ');

              return (
                <tr key={user.id} className="hover:bg-canvas-cream/50 transition-colors">
                  <td className="px-6 py-4">
                    <div className="text-xs font-mono font-bold text-shade-60 mb-0.5">{user.code || 'NV-000'}</div>
                    <div className="text-sm font-semibold text-ink">{user.fullName}</div>
                  </td>
                  <td className="px-6 py-4 text-xs text-shade-60">
                    <div>{user.email || 'Không có email'}</div>
                    <div className="mt-0.5">{user.phone || 'Chưa có SĐT'}</div>
                  </td>
                  <td className="px-6 py-4">
                    {user.role && (
                      <span className="text-[10px] font-bold bg-canvas-cream text-ink border border-hairline-light px-2.5 py-1 rounded-pill uppercase tracking-wider">
                        {ROLE_LABELS[user.role] || user.role}
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 text-xs font-medium text-shade-70 font-mono">
                    {user.role === ROLES.ADMIN ? (
                      <span className="italic text-shade-40">Tất cả kho (Admin)</span>
                    ) : user.role === ROLES.CEO ? (
                      <span className="italic text-shade-40">Tất cả kho (CEO)</span>
                    ) : assignedWHNames ? (
                      assignedWHNames
                    ) : (
                      <span className="text-red-500 font-semibold italic">Chưa gán kho</span>
                    )}
                  </td>
                  <td className="px-6 py-4">
                    <Badge type={user.isActive ? 'success' : 'neutral'}>
                      {user.isActive ? 'Đang hoạt động' : 'Tạm khóa'}
                    </Badge>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => handleOpenEditModal(user)}
                        className="text-shade-60 hover:text-ink transition-colors"
                        title="Chỉnh sửa tài khoản"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleToggleUserStatus(user)}
                        className={`${user.isActive ? 'text-green-600 hover:text-green-800' : 'text-shade-40 hover:text-ink'} transition-colors`}
                        title={user.isActive ? 'Khóa tài khoản (Soft Delete)' : 'Kích hoạt tài khoản'}
                      >
                        {user.isActive ? (
                          <ToggleRight className="w-6 h-6" />
                        ) : (
                          <ToggleLeft className="w-6 h-6" />
                        )}
                      </button>
                    </div>
                  </td>
                </tr>
              );
            }}
          />
        </div>
      ) : (
        /* Audit logs Panel */
        <div className="flex flex-col gap-4">
          <Table
            headers={['Thời gian', 'Người thực hiện', 'Thao tác', 'Chi tiết đối tượng', 'Nội dung']}
            data={auditLogs}
            loading={loading}
            renderRow={(log, idx) => (
              <tr key={log.id} className="hover:bg-canvas-cream/50 transition-colors">
                <td className="px-6 py-4 text-xs font-mono text-shade-50">
                  {formatDate(log.createdAt)}
                </td>
                <td className="px-6 py-4 text-xs font-semibold text-ink">
                  {log.actorName}
                </td>
                <td className="px-6 py-4">
                  <Badge type={log.action.includes('CREATED') ? 'success' : log.action.includes('DELETED') || log.action.includes('DEACTIVATED') ? 'danger' : 'info'}>
                    {log.action}
                  </Badge>
                </td>
                <td className="px-6 py-4 text-xs text-shade-60">
                  {log.entityType} (ID: {log.entityId})
                </td>
                <td className="px-6 py-4 text-xs font-medium text-shade-70">
                  {log.details}
                </td>
              </tr>
            )}
          />
        </div>
      )}

      {/* Create / Edit Form Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={modalType === 'create' ? 'Tạo tài khoản mới' : 'Chỉnh sửa tài khoản'}
      >
        <form onSubmit={handleSaveUser} className="flex flex-col gap-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-md text-xs font-medium">
              {formError}
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

          {modalType === 'create' && (
            <Input
              label="Mật khẩu khởi tạo"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Mật khẩu tối thiểu 8 ký tự, chữ và số"
              required
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

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input
              label="Chức danh"
              type="text"
              value={jobTitle}
              onChange={(e) => setJobTitle(e.target.value)}
              placeholder="Thủ kho / Bảo vệ..."
            />
            <Input
              label="Ca làm việc"
              type="text"
              value={shift}
              onChange={(e) => setShift(e.target.value)}
              placeholder="Ca sáng / Ca chiều..."
            />
            <Input
              label="Khu vực phụ trách"
              type="text"
              value={region}
              onChange={(e) => setRegion(e.target.value)}
              placeholder="Hải Phòng / Hà Nội..."
            />
          </div>

          {/* Warehouses Checkbox group (only relevant if not Admin/CEO) */}
          {(selectedRole !== ROLES.ADMIN && selectedRole !== ROLES.CEO) && (
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
              onClick={() => setModalOpen(false)}
              variant="outline-light"
            >
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={formLoading}
            >
              Lưu lại
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default UserManagement;
