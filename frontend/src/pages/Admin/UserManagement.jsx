import React, { useState, useEffect } from 'react';
import { adminService } from '../../services/admin.service';
import { useUiStore } from '../../stores/ui.store';
import Button from '../../components/common/Button';
import UserTable from './UserTable';
import UserFormModal from './UserFormModal';
import { Plus, Search } from 'lucide-react';

const UserManagement = () => {
  const { addToast } = useUiStore();

  // Users Data States
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');

  // Form Modal States
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [formLoading, setFormLoading] = useState(false);

  // Load Data
  const loadData = async () => {
    setLoading(true);
    try {
      const uList = await adminService.getUsers();
      setUsers(uList);
    } catch {
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
    return (
      u.fullName.toLowerCase().includes(searchLower) ||
      (u.email && u.email.toLowerCase().includes(searchLower)) ||
      (u.code && u.code.toLowerCase().includes(searchLower))
    );
  });

  const handleOpenCreateModal = () => {
    setEditingUser(null);
    setModalOpen(true);
  };

  const handleOpenEditModal = (user) => {
    setEditingUser(user);
    setModalOpen(true);
  };

  const handleSaveUser = async (payload) => {
    setFormLoading(true);
    try {
      if (editingUser) {
        // Edit flow (Mã NV cannot be updated)
        await adminService.updateUser(editingUser.id, {
          fullName: payload.fullName,
          email: payload.email,
          phone: payload.phone,
          role: payload.role,
          jobTitle: payload.jobTitle,
          shift: payload.shift,
          region: payload.region,
          warehouses: payload.warehouses
        });
        addToast('Cập nhật tài khoản thành công', 'success');
      } else {
        // Create flow
        await adminService.createUser(payload);
        addToast('Tạo tài khoản mới thành công', 'success');
      }
      setModalOpen(false);
      loadData();
    } catch (err) {
      // Re-throw so UserFormModal can display field-specific error messages
      throw err;
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
    } catch {
      addToast('Không thể cập nhật trạng thái tài khoản', 'error');
    }
  };

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

        {/* User Table Component */}
        <UserTable
          users={filteredUsers}
          loading={loading}
          onEdit={handleOpenEditModal}
          onToggleStatus={handleToggleUserStatus}
        />
      </div>

      {/* User Form Modal Component */}
      <UserFormModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSave={handleSaveUser}
        user={editingUser}
        loading={formLoading}
      />
    </div>
  );
};

export default UserManagement;
