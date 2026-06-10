import React, { useEffect, useState } from 'react';
import { Plus, Search } from 'lucide-react';
import { adminService } from '../../services/admin.service';
import { useUiStore } from '../../stores/ui.store';
import Button from '../../components/common/Button';
import Pagination from '../../components/common/Pagination';
import UserTable from './UserTable';
import UserFormModal from './UserFormModal';

const UserManagement = () => {
  const { addToast } = useUiStore();

  // Users Data States
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');

  // Pagination States for Users
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Form Modal States
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [formLoading, setFormLoading] = useState(false);

  // Load All Data
  const loadData = async () => {
    setLoading(true);
    try {
      const uList = await adminService.getUsers();
      setUsers(uList || []);
    } catch (err) {
      console.error('Error loading admin user data:', err);
      addToast('Không thể tải dữ liệu quản trị', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Filter users
  const filteredUsers = users.filter((u) => {
    const searchLower = search.toLowerCase();
    return (
      u.fullName.toLowerCase().includes(searchLower) ||
      (u.email && u.email.toLowerCase().includes(searchLower)) ||
      (u.code && u.code.toLowerCase().includes(searchLower))
    );
  });

  // Reset page to 1 when search changes
  useEffect(() => {
    setCurrentPage(1);
  }, [search]);

  // Paginated Users
  const totalItems = filteredUsers.length;
  const totalPages = Math.ceil(totalItems / pageSize) || 1;
  const paginatedUsers = filteredUsers.slice((currentPage - 1) * pageSize, currentPage * pageSize);

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
        // Edit flow
        await adminService.updateUser(editingUser.id, {
          code: payload.code,
          fullName: payload.fullName,
          email: payload.email,
          phone: payload.phone,
          role: payload.role,
          jobTitle: payload.jobTitle,
          shift: payload.shift,
          region: payload.region,
          warehouses: payload.warehouses,
          password: payload.password
        });
        addToast('Cập nhật tài khoản thành công', 'success');
      } else {
        // Create flow
        await adminService.createUser(payload);
        addToast('Tạo tài khoản mới thành công', 'success');
      }
      setModalOpen(false);
      await loadData();
    } catch (err) {
      throw err; // Re-throw so UserFormModal can handle validation message
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
      await loadData();
    } catch {
      addToast('Không thể cập nhật trạng thái tài khoản', 'error');
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Hệ thống / Admin
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Quản trị tài khoản & Phân quyền
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Quản lý tài khoản nhân viên, vai trò RBAC và kho được phép truy cập.
          </p>
        </div>
        <Button onClick={handleOpenCreateModal} variant="primary" icon={Plus}>
          Tạo tài khoản
        </Button>
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
        <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden flex flex-col">
          <UserTable
            users={paginatedUsers}
            loading={loading}
            onEdit={handleOpenEditModal}
            onToggleStatus={handleToggleUserStatus}
          />
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            pageSize={pageSize}
            onPageChange={setCurrentPage}
            onPageSizeChange={setPageSize}
          />
        </div>
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
